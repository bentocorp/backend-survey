package com.bentonow.survey.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import org.safris.commons.util.TieredRangeFetcher;

import com.bentonow.survey.Config;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Meal extends Entity {
  private static final Logger logger = Logger.getLogger(Meal.class.getName());
  private static final Map<Integer,Meal> mealIdToMeal = new HashMap<Integer,Meal>();
  private static final ThreadLocal<SimpleDateFormat> dateFormatLocal = new ThreadLocal<SimpleDateFormat>() {
    protected SimpleDateFormat initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
  };

  public static Meal lookupMeal(final int mealId) throws IOException, SQLException {
    logger.info("" + mealId);
    Meal meal = mealIdToMeal.get(mealId);
    if (meal != null)
      return meal;

    synchronized (mealIdToMeal) {
      if ((meal = mealIdToMeal.get(mealId)) != null)
        return meal;

      try (
        final Connection connection = getConnection();
        final PreparedStatement statement = connection.prepareStatement("SELECT m.*, d.* FROM meal m, meal_dish md, dish d WHERE m.id = ? AND m.id = md.meal_id AND md.dish_id = d.id ORDER BY d.type");
      ) {
        statement.setInt(1, mealId);
        final ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          if (meal == null) {
            final int orderId = resultSet.getInt(2);
            final Date createdOn = resultSet.getTimestamp(3);
            final String email = resultSet.getString(4);
            final boolean sent = resultSet.getBoolean(5);
            final boolean skipped = resultSet.getBoolean(6);
            mealIdToMeal.put(mealId, meal = new Meal(mealId, orderId, createdOn, email, sent, skipped, new ArrayList<Dish>()));
          }

          final int dishId = resultSet.getInt(7);
          final String name = resultSet.getString(8);
          final String description = resultSet.getString(9);
          final Dish.Type type = Dish.Type.valueOf(resultSet.getString(10).toUpperCase());
          final String imageUrl = resultSet.getString(11);
          meal.dishes.add(Dish.create(dishId, name, description, type, imageUrl));
        }
      }

      return meal;
    }
  }

  public static List<Meal> fetchUnsent(long from, final long to) throws SQLException {
    try (
      final Connection connection = getConnection();
      final PreparedStatement minDateStatement = connection.prepareStatement("SELECT MIN(m.created_on) FROM meal m LEFT JOIN unsubscribed u ON u.email = m.email LEFT JOIN meal_survey ms ON ms.meal_id = m.id WHERE u.email IS NULL AND ms.meal_id IS NULL AND m.sent = 0 AND m.skipped = 0");
    ) {
      final ResultSet minDateResultSet = minDateStatement.executeQuery();

      Date date;
      if (minDateResultSet.next() && (date = minDateResultSet.getTimestamp(1)) != null) {
        from = date.getTime();
        logger.info("found earliest unsent survey as: " + dateFormatLocal.get().format(from));
      }
      else {
        try (final PreparedStatement maxDateStatement = connection.prepareStatement("SELECT MAX(m.created_on) FROM meal m LEFT JOIN unsubscribed u ON u.email = m.email LEFT JOIN meal_survey ms ON ms.meal_id = m.id WHERE u.email IS NULL AND ms.meal_id IS NULL")) {
          final ResultSet maxDateResultSet = maxDateStatement.executeQuery();
          if (maxDateResultSet.next() && (date = maxDateResultSet.getTimestamp(1)) != null && date.getTime() < from) {
            logger.info("all surveys sent .. found last sent survey as: " + dateFormatLocal.get().format(from));
            from = date.getTime() + 1000;

            // be thorough: based on the last meal order in the db, if it was before the desired from time, set it as the from time + 1 second (so as not to duplicate that last meal order in the db)
            try (final PreparedStatement statement = connection.prepareStatement("UPDATE meal_audit SET range_to = CASE WHEN range_to > ? THEN range_to ELSE ? END")) {
              statement.setTimestamp(1, new java.sql.Timestamp(date.getTime()));
              statement.setTimestamp(2, new java.sql.Timestamp(date.getTime()));
              statement.executeUpdate();
              logger.info("adjusted meal_audit range_to to: " + dateFormatLocal.get().format(date.getTime()));
            }
          }
        }
      }
    }

    // This means that there is no unsent data in the DB that would qualify to be fetched based on the "to" time
    if (from >= to) {
      logger.info("Earliest unsent, or latest sent surveys in the DB are prior to " + dateFormatLocal.get().format(from) + ". Thus, there is no unsent data in the DB that would qualify to be fetched based on the 'to' time of " + dateFormatLocal.get().format(to));
      return null;
    }

    final Collection<Meal> meals = fetchMeals(from, to);
    final List<Meal> unsentMeals = new ArrayList<Meal>();
    for (final Meal meal : meals)
      if (!meal.sent && !meal.skipped)
        unsentMeals.add(meal);

    return unsentMeals;
  }

  public static Collection<Meal> fetchMeals(final Long from, final Long to) throws SQLException {
    logger.info(dateFormatLocal.get().format(from) + ", " + dateFormatLocal.get().format(to));
    return cacheTier.fetch(from, to, null).values();
  }

  private static final String mealServiceUrl = Config.getConfig()._webApi(0)._protocol$().text() + "://" + Config.getConfig()._webApi(0)._host$().text() + "/extapi/reports/survey/range/${fromDate}/${endDate}?" + Config.getParameters(Config.getConfig()._webApi(0)._parameters(0));

  private static final TieredRangeFetcher<Long,Meal> webServiceTier = new TieredRangeFetcher<Long,Meal>(null) {
    private final Long[] range = new Long[] {Long.MIN_VALUE, Long.MAX_VALUE};

    protected Long[] range() {
      return range;
    }

    protected SortedMap<Long,Meal> select(final Long from, final Long to) {
      logger.info("webServiceTier.select(" + dateFormatLocal.get().format(from) + ", " + dateFormatLocal.get().format(to) + ")");

      try {
        final URL serviceUrl = new URL(mealServiceUrl.replace("${fromDate}", URLEncoder.encode(dateFormatLocal.get().format(from), "UTF-8")).replace("${endDate}", URLEncoder.encode(dateFormatLocal.get().format(to - 1000), "UTF-8"))); // remove 1 second from the to, becasue the web-service API is (from, to) spec
        logger.info(serviceUrl.toExternalForm());
        final HttpsURLConnection connection = (HttpsURLConnection)serviceUrl.openConnection();
        if (Config.getConfig()._webApi(0)._ignoreSecurityErrors$().text())
          connection.setHostnameVerifier(hostnameVerifier);

        final InputStream in = connection.getInputStream();
        final JsonElement json = new JsonParser().parse(new InputStreamReader(in));
        final JsonArray array = json.getAsJsonArray();
        final Iterator<JsonElement> iterator = array.iterator();

        final SortedMap<Long,Meal> meals = new TreeMap<Long,Meal>();
        while (iterator.hasNext()) {
          final JsonObject object = iterator.next().getAsJsonObject();
          final int id = object.get("pk_CustomerBentoBox").getAsInt();
          final int orderId = object.get("pk_Order").getAsInt();
          final Date createdOn = dateFormatLocal.get().parse(object.get("created_at").getAsString());
          final String email = object.get("email").getAsString();
          final List<Dish> dishes = new ArrayList<Dish>();
          dishes.add(Dish.fetch(object.get("main_id").getAsInt()));
          dishes.add(Dish.fetch(object.get("side1_id").getAsInt()));
          dishes.add(Dish.fetch(object.get("side2_id").getAsInt()));
          dishes.add(Dish.fetch(object.get("side3_id").getAsInt()));
          dishes.add(Dish.fetch(object.get("side4_id").getAsInt()));
          meals.put(createdOn.getTime(), new Meal(id, orderId, createdOn, email, false, meals.size() != 0 && meals.get(meals.lastKey()).orderId == orderId, dishes)); // FIXME: the last param marks subsequent orders for the same orderId as "skipped"
        }

        return meals;
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    protected void insert(final Long from, final Long to, final SortedMap<Long,Meal> data) {
    }
  };

  private static final TieredRangeFetcher<Long,Meal> dbTier = new TieredRangeFetcher<Long,Meal>(webServiceTier) {
    protected Long[] range() {
      try (
        final Connection connection = getConnection();
        final PreparedStatement statement = connection.prepareStatement("SELECT * FROM meal_audit");
      ) {
        final ResultSet resultSet = statement.executeQuery();
        return resultSet.next() ? new Long[] {resultSet.getTimestamp(1).getTime(), resultSet.getTimestamp(2).getTime()} : null;
      }
      catch (final SQLException e) {
        throw new RuntimeException(e);
      }
    }

    protected SortedMap<Long,Meal> select(final Long from, final Long to) {
      logger.info("dbTier.select(" + dateFormatLocal.get().format(from) + ", " + dateFormatLocal.get().format(to) + ")");
      try (
        final Connection connection = getConnection();
        final PreparedStatement statement = connection.prepareStatement("SELECT m.*, d.* FROM dish d, meal_dish md, meal m LEFT JOIN unsubscribed u ON u.email = m.email LEFT JOIN meal_survey ms ON ms.meal_id = m.id WHERE u.email IS NULL AND ms.meal_id IS NULL AND ? <= m.created_on AND m.created_on < ? AND m.id = md.meal_id AND md.dish_id = d.id ORDER BY m.created_on, m.order_id, d.type");
      ) {
        statement.setTimestamp(1, new java.sql.Timestamp(from));
        statement.setTimestamp(2, new java.sql.Timestamp(to));
        final ResultSet resultSet = statement.executeQuery();

        final SortedMap<Long,Meal> results = new TreeMap<Long,Meal>();
        Meal meal = null;
        while (resultSet.next()) {
          final int mealId = resultSet.getInt(1);
          if (meal == null || meal.id != mealId) {
            final int orderId = resultSet.getInt(2);
            final Date createdOn = resultSet.getTimestamp(3);
            final String email = resultSet.getString(4);
            final boolean sent = resultSet.getBoolean(5);
            final boolean skipped = resultSet.getBoolean(6);
            final List<Dish> dishes = new ArrayList<Dish>();
            results.put(createdOn.getTime(), meal = new Meal(mealId, orderId, createdOn, email, sent, skipped, dishes));
          }

          final int dishId = resultSet.getInt(7);
          final String name = resultSet.getString(8);
          final String description = resultSet.getString(9);
          final Dish.Type type = Dish.Type.valueOf(resultSet.getString(10).toUpperCase());
          final String imageUrl = resultSet.getString(11);
          meal.dishes.add(Dish.create(dishId, name, description, type, imageUrl));
        }

        return results;
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    protected void insert(final Long from, final Long to, final SortedMap<Long,Meal> data) {
      logger.info("dbTier.insert(" + dateFormatLocal.get().format(from) + ", " + dateFormatLocal.get().format(to) + ", " + data.size() + ")");
      try (final Connection connection = getConnection()) {
        try (final PreparedStatement mealStatement = connection.prepareStatement("INSERT INTO meal VALUES (?, ?, ?, ?, ?, ?)")) {
          for (final Meal meal : data.values()) {
            mealStatement.setInt(1, meal.id);
            mealStatement.setInt(2, meal.orderId);
            mealStatement.setTimestamp(3, new java.sql.Timestamp(meal.createdOn.getTime()));
            mealStatement.setString(4, meal.email);
            mealStatement.setBoolean(5, meal.sent);
            mealStatement.setBoolean(6, meal.skipped);
            mealStatement.addBatch();
          }

          mealStatement.executeBatch();
        }

        try (
          final PreparedStatement mealDishInsertStatement = connection.prepareStatement("INSERT INTO meal_dish VALUES (?, ?, ?)");
          final PreparedStatement mealDishUpdateStatement = connection.prepareStatement("UPDATE meal_dish SET quantity = quantity + 1 WHERE meal_id = ? AND dish_id = ?");
        ) {
          for (final Meal meal : data.values()) {
            for (final Dish dish : meal.dishes) {
              try {
                mealDishInsertStatement.setInt(1, meal.id);
                mealDishInsertStatement.setInt(2, dish.id);
                mealDishInsertStatement.setInt(3, 1);
                mealDishInsertStatement.executeUpdate();
              }
              catch (final SQLException e) {
                if (e.getErrorCode() == 1062) {
                  mealDishUpdateStatement.setInt(1, meal.id);
                  mealDishUpdateStatement.setInt(2, dish.id);
                  mealDishUpdateStatement.executeUpdate();
                }
              }
            }
          }
        }

        try (final PreparedStatement auditUpdateStatement = connection.prepareStatement("UPDATE meal_audit SET range_from = CASE WHEN range_from < ? THEN range_from ELSE ? END, range_to = CASE WHEN range_to > ? THEN range_to ELSE ? END")) {
          auditUpdateStatement.setTimestamp(1, new java.sql.Timestamp(from));
          auditUpdateStatement.setTimestamp(2, new java.sql.Timestamp(from));
          auditUpdateStatement.setTimestamp(3, new java.sql.Timestamp(to));
          auditUpdateStatement.setTimestamp(4, new java.sql.Timestamp(to));
          if (auditUpdateStatement.executeUpdate() == 0) { // this will only need to happen the 1st time after a fresh DB is created
            try (final PreparedStatement auditInsertStatement = connection.prepareStatement("INSERT INTO meal_audit VALUES (?, ?)")) {
              auditInsertStatement.setTimestamp(1, new java.sql.Timestamp(from));
              auditInsertStatement.setTimestamp(2, new java.sql.Timestamp(to));
              auditInsertStatement.executeUpdate();
            }
          }
        }
      }
      catch (final SQLException e) {
        throw new RuntimeException(e);
      }
    }
  };

  private static final TieredRangeFetcher<Long,Meal> cacheTier = new TieredRangeFetcher<Long,Meal>(dbTier) {
    private final SortedMap<Long,Meal> cache = new TreeMap<Long,Meal>();
    private final Long[] range = new Long[] {0L, 0L};

    protected Long[] range() {
      return range;
    }

    protected SortedMap<Long,Meal> select(final Long from, final Long to) {
      logger.info("cacheTier.select(" + dateFormatLocal.get().format(from) + ", " + dateFormatLocal.get().format(to) + ")");
      return cache.subMap(from, to);
    }

    protected void insert(final Long from, final Long to, final SortedMap<Long,Meal> data) {
      logger.info("cacheTier.insert(" + dateFormatLocal.get().format(from) + ", " + dateFormatLocal.get().format(to) + ", " + data.size() + ")");
      range[0] = from < range[0] ? from : range[0];
      range[1] = range[1] < to ? to : range[1];
      cache.putAll(data);

      for (final Meal meal : data.values())
        mealIdToMeal.put(meal.id, meal);
    }
  };

  public final int id;
  public final int orderId;
  public final Date createdOn;
  public final String email;
  private volatile boolean sent;
  public final boolean skipped;
  public final List<Dish> dishes;

  private Meal(final int id, final int orderId, final Date createdOn, final String email, final boolean sent, final boolean skipped, final List<Dish> dishes) {
    this.id = id;
    this.orderId = orderId;
    this.createdOn = createdOn;
    this.email = email;
    this.sent = sent;
    this.skipped = skipped;
    this.dishes = dishes;
  }

  public boolean sent() {
    return sent;
  }

  public void sent(final boolean value) {
    logger.info("" + value);
    if (this.sent == value)
      return;

    synchronized (this) {
      if (this.sent == value)
        return;

      this.sent = value;
      try (
        final Connection connection = getConnection();
        final PreparedStatement statement = connection.prepareStatement("UPDATE meal SET sent = 1 WHERE id = ?");
      ) {
        statement.setInt(1, id);
        statement.executeUpdate();
      }
      catch (final SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
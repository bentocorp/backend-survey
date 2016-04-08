package com.bentonow.survey.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import com.bentonow.survey.Config;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Dish extends Entity {
  public static enum Type {
    MAIN,
    SIDE
  }

  private static final Logger logger = Logger.getLogger(Dish.class.getName());
  private static final String dishServiceUrl = Config.getConfig()._webApi(0)._protocol$().text() + "://" + Config.getConfig()._webApi(0)._host$().text() + "/extapi/dish/${id}?" + Config.getParameters(Config.getConfig()._webApi(0)._parameters(0));
  private static final Map<Integer,Dish> idToDish = new HashMap<Integer,Dish>();

  public static Dish create(final int id, final String name, final String description, final Type type, final String imageUrl, final Date createdOn) {
    logger.finest(id + "...");
    Dish instance = idToDish.get(id);
    if (instance != null)
      return instance;

    synchronized (idToDish) {
      instance = idToDish.get(id);
      if (instance != null)
        return instance;

      idToDish.put(id, instance = new Dish(id, name, description, type, imageUrl, createdOn));
      return instance;
    }
  }

  private static Dish checkExpired(final int id) {
    Dish instance = idToDish.get(id);
    if (instance != null && instance.createdOn.getTime() > System.currentTimeMillis() - Config.getConfig()._db(0)._dishTTL$().text() * 60 * 60 * 1000)
      return instance;

    idToDish.remove(id);
    return null;
  }

  public static Dish fetch(final int id) throws IOException, SQLException {
    logger.finest("" + id);
    Dish instance = checkExpired(id);
    if (instance != null)
      return instance;

    synchronized (idToDish) {
      instance = checkExpired(id);
      if (instance != null)
        return instance;

      try (
        final Connection connection = getConnection();
        final PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM dish WHERE id = ?");
      ) {
        selectStatement.setInt(1, id);
        final ResultSet resultSet = selectStatement.executeQuery();
        String name = null;
        String description = null;
        Type type = null;
        String imageUrl = null;
        Date createdOn = null;
        if (resultSet.next()) {
          name = resultSet.getString(2);
          description = resultSet.getString(3);
          type = Type.valueOf(resultSet.getString(4).toUpperCase());
          imageUrl = resultSet.getString(5);
          createdOn = resultSet.getTimestamp(6);
        }

        final boolean insertDish;
        if ((insertDish = createdOn == null) || createdOn.getTime() <= System.currentTimeMillis() - Config.getConfig()._db(0)._dishTTL$().text() * 60 * 60 * 1000) {
          final URL serviceUrl = new URL(dishServiceUrl.replace("${id}", String.valueOf(id)));
          logger.info("serviceUrl: " + serviceUrl.toExternalForm());
          final HttpsURLConnection urlConnection = (HttpsURLConnection)serviceUrl.openConnection();
          if (Config.getConfig()._webApi(0)._ignoreSecurityErrors$().text())
            urlConnection.setHostnameVerifier(hostnameVerifier);

          try (final InputStream in = urlConnection.getInputStream()) {
            final JsonElement json = new JsonParser().parse(new InputStreamReader(in));
            final JsonObject object = json.getAsJsonObject();
            name = object.get("name").getAsString();
            description = object.get("description").getAsString();
            type = Type.valueOf(object.get("type").getAsString().toUpperCase());
            JsonElement image = object.get("email_image1");
            if (image == null || image.isJsonNull())
              image = object.get("image1");

            imageUrl = image == null || image.isJsonNull() ? null : image.getAsString();
            createdOn = new Date();
          }

          final int paramOffset = insertDish ? 0 : 1;
          try (final PreparedStatement insertStatement = insertDish ? connection.prepareStatement("INSERT INTO dish VALUES (?, ?, ?, ?, ?, ?)") : connection.prepareStatement("UPDATE dish SET name = ?, description = ?, type = ?, image_url = ?, created_on = ? WHERE id = ?")) {
            insertStatement.setInt(insertDish ? 1 : 6, id);
            insertStatement.setString(2 - paramOffset, name);
            insertStatement.setString(3 - paramOffset, description);
            insertStatement.setString(4 - paramOffset, type.toString());
            if (imageUrl != null)
              insertStatement.setString(5 - paramOffset, imageUrl);
            else
              insertStatement.setNull(5 - paramOffset, Types.VARCHAR);
            insertStatement.setTimestamp(6 - paramOffset, new Timestamp(createdOn.getTime()));
            insertStatement.executeUpdate();
          }
        }

        idToDish.put(id, instance = new Dish(id, name, description, type, imageUrl, createdOn));
        return instance;
      }
    }
  }

  public final int id;
  public final String name;
  public final String description;
  public final Type type;
  public final String imageUrl;
  public final Date createdOn;

  private Dish(final int id, final String name, final String description, final Type type, final String imageUrl, final Date createdOn) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.type = type;
    this.imageUrl = imageUrl;
    this.createdOn = createdOn;
  }
}
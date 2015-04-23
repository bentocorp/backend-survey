package com.bentonow.survey.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class MealSurvey extends Entity {
  private static final Logger logger = Logger.getLogger(MealSurvey.class.getName());
  private static final Map<Integer,MealSurvey> mealIdToMealSurvey = new HashMap<Integer,MealSurvey>();

  public static MealSurvey lookupMealSurvey(final int mealId) throws SQLException {
    logger.info("" + mealId);
    MealSurvey mealSurvey = mealIdToMealSurvey.get(mealId);
    if (mealSurvey != null)
      return mealSurvey;

    synchronized (mealIdToMealSurvey) {
      if ((mealSurvey = mealIdToMealSurvey.get(mealId)) != null)
        return mealSurvey;

      try (
        final Connection connection = getConnection();
        final PreparedStatement statement = connection.prepareStatement("SELECT ms.*, ds.* FROM meal_survey ms LEFT JOIN dish_survey ds ON ms.meal_id = ds.meal_id WHERE ms.meal_id = ?");
      ) {
        statement.setInt(1, mealId);
        final ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          if (mealSurvey == null) {
            Integer mealRating = resultSet.getInt(2);
            if (resultSet.wasNull())
              mealRating = null;

            final String mealComment = resultSet.getString(3);
            mealIdToMealSurvey.put(mealId, mealSurvey = new MealSurvey(mealId, mealRating, mealComment, new ArrayList<DishSurvey>()));
          }

          final int dishId = resultSet.getInt(5);
          if (resultSet.wasNull())
            continue;

          final int dishRating = resultSet.getInt(6);
          final String dishComment = resultSet.getString(7);
          mealSurvey.dishSurveys.add(new DishSurvey(mealId, dishId, dishRating, dishComment));
        }
      }

      return mealSurvey;
    }
  }

  public static MealSurvey insert(final int mealId, final Integer rating, String comment, final List<DishSurvey> dishSurveys) throws SQLException {
    comment = comment != null ? comment.trim() : "";

    logger.info(mealId + "...");
    MealSurvey mealSurvey = lookupMealSurvey(mealId);
    if (mealSurvey != null)
      throw new IllegalArgumentException("MealSurvey with id = " + mealId + " already exists");

    synchronized (mealIdToMealSurvey) {
      if ((mealSurvey = mealIdToMealSurvey.get(mealId)) != null)
        throw new IllegalArgumentException("MealSurvey with id = " + mealId + " already exists");

      try (
        final Connection connection = getConnection();
        final PreparedStatement mealSurveyStatement = connection.prepareStatement("INSERT INTO meal_survey VALUES (?, ?, ?)");
        final PreparedStatement dishSurveyStatement = connection.prepareStatement("INSERT INTO dish_survey VALUES (?, ?, ?, ?)");
      ) {
        mealSurveyStatement.setInt(1, mealId);
        if (rating != null)
          mealSurveyStatement.setInt(2, rating);
        else
          mealSurveyStatement.setNull(2, Types.INTEGER);
        mealSurveyStatement.setString(3, comment);
        try {
          if (mealSurveyStatement.executeUpdate() != 1)
            return null;
        }
        catch (final SQLException e) {
          if (e.getErrorCode() == 1062)
            return null;
        }

        mealIdToMealSurvey.put(mealId, mealSurvey = new MealSurvey(mealId, rating, comment, dishSurveys));
        for (final DishSurvey dishSurvey : mealSurvey.dishSurveys) {
          dishSurveyStatement.setInt(1, mealId);
          dishSurveyStatement.setInt(2, dishSurvey.dishId);
          dishSurveyStatement.setInt(3, dishSurvey.rating);
          dishSurveyStatement.setString(4, dishSurvey.comment);
          dishSurveyStatement.addBatch();
        }

        dishSurveyStatement.executeBatch();
      }

      return mealSurvey;
    }
  }

  public final int mealId;
  public final Integer rating;
  public final String comment;
  public final List<DishSurvey> dishSurveys;

  private MealSurvey(final int mealId, final Integer rating, final String comment, final List<DishSurvey> dishSurveys) {
    this.mealId = mealId;
    this.rating = rating;
    this.comment = comment != null && comment.length() > 0 ? comment : null;
    this.dishSurveys = dishSurveys;
  }
}
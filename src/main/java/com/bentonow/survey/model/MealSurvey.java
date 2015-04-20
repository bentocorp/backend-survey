package com.bentonow.survey.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class MealSurvey extends Entity {
  private static final Logger logger = Logger.getLogger(MealSurvey.class.getName());
  private static final Map<String,MealSurvey> mealIdToMealSurvey = new HashMap<String,MealSurvey>();

  public static MealSurvey lookupMealSurvey(final String mealId) throws SQLException {
    logger.info(mealId);
    MealSurvey mealSurvey = mealIdToMealSurvey.get(mealId);
    if (mealSurvey != null)
      return mealSurvey;

    synchronized (mealIdToMealSurvey) {
      if ((mealSurvey = mealIdToMealSurvey.get(mealId)) != null)
        return mealSurvey;

      try (
        final Connection connection = getConnection();
        final PreparedStatement statement = connection.prepareStatement("SELECT ms.*, ds.* FROM meal_survey ms, dish_survey ds WHERE ms.meal_id = ? AND ms.meal_id = ds.meal_id");
      ) {
        statement.setString(1, mealId);
        final ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
          if (mealSurvey == null) {
            final int mealRating = resultSet.getInt(2);
            final String mealComment = resultSet.getString(3);
            mealIdToMealSurvey.put(mealId, mealSurvey = new MealSurvey(mealId, mealRating, mealComment, new ArrayList<DishSurvey>()));
          }

          final int dishId = resultSet.getInt(5);
          final int dishRating = resultSet.getInt(6);
          final String dishComment = resultSet.getString(7);
          mealSurvey.dishSurveys.add(new DishSurvey(mealId, dishId, dishRating, dishComment));
        }
      }

      return mealSurvey;
    }
  }

  public static MealSurvey insert(final String mealId, final int rating, String comment, final List<DishSurvey> dishSurveys) throws SQLException {
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
        mealSurveyStatement.setString(1, mealId);
        mealSurveyStatement.setInt(2, rating);
        mealSurveyStatement.setString(3, comment);
        if (mealSurveyStatement.executeUpdate() != 1)
          return null;

        mealIdToMealSurvey.put(mealId, mealSurvey = new MealSurvey(mealId, rating, comment, dishSurveys));
        for (final DishSurvey dishSurvey : mealSurvey.dishSurveys) {
          dishSurveyStatement.setString(1, mealId);
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

  public final String mealId;
  public final int rating;
  public final String comment;
  public final List<DishSurvey> dishSurveys;

  private MealSurvey(final String mealId, final int rating, final String comment, final List<DishSurvey> dishSurveys) {
    this.mealId = mealId;
    this.rating = rating;
    this.comment = comment != null && comment.length() > 0 ? comment : null;
    this.dishSurveys = dishSurveys;
  }
}
package com.bentonow.survey.model;

public class DishSurvey extends Entity {
  public final int mealId;
  public final int dishId;
  public final Integer rating;
  public final String comment;

  public DishSurvey(final int mealId, final int dishId, final Integer rating, final String comment) {
    this.mealId = mealId;
    this.dishId = dishId;
    this.rating = rating;
    this.comment = comment != null ? comment.trim() : "";
  }
}
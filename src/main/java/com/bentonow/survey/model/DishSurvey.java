package com.bentonow.survey.model;

public class DishSurvey extends Entity {
  public final String mealId;
  public final int dishId;
  public final int rating;
  public final String comment;

  public DishSurvey(final String mealId, final int dishId, final int rating, final String comment) {
    this.mealId = mealId;
    this.dishId = dishId;
    this.rating = rating;
    this.comment = comment != null ? comment.trim() : "";
  }
}
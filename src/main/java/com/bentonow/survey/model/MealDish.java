package com.bentonow.survey.model;

public class MealDish extends Entity {
  public final String mealId;
  public final int dishId;
  public final int quantity;

  public MealDish(final String mealId, final int dishId, final int quantity) {
    this.mealId = mealId;
    this.dishId = dishId;
    this.quantity = quantity;
  }
}
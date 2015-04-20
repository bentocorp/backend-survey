package com.bentonow.survey.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class Subscription extends Entity {
  private static final Logger logger = Logger.getLogger(Subscription.class.getName());

  public static boolean resubscribe(final String email) throws SQLException {
    logger.info(email);
    try (
      final Connection connection = getConnection();
      final PreparedStatement statement = connection.prepareStatement("DELETE FROM unsubscribed WHERE email = ?");
    ) {
      statement.setString(1, email);
      return statement.executeUpdate() == 1;
    }
  }

  public static boolean unsubscribe(final String email) throws SQLException {
    logger.info(email);
    try (
      final Connection connection = getConnection();
      final PreparedStatement statement = connection.prepareStatement("INSERT INTO unsubscribed VALUES (?)");
    ) {
      statement.setString(1, email);
      return statement.executeUpdate() == 1;
    }
  }
}
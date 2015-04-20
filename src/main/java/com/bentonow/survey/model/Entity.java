package com.bentonow.survey.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.safris.commons.lang.Equals;
import org.safris.commons.lang.HashCodes;
import org.safris.commons.lang.ToStrings;
import org.safris.commons.sql.ConnectionProxy;

public abstract class Entity {
  protected static String webServiceAuthParams = "?api_username=seva_kjHgbmAq*7%40%235_%25KLbH&api_password=%245y%2410%24Ldvko.Fby1IhGHl16njuLOIbWlwz9TvMbBjqrph%2FlJuYIQxqTMyuG.86";
  private static Connection connection;

  protected static Connection getConnection() throws SQLException {
    return connection == null || connection.isClosed() ? ConnectionProxy.getInstance(connection = DriverManager.getConnection("jdbc:mysql://localhost/survey?user=survey&password=survey")) : connection;
  }

  public int hashCode() {
    return HashCodes.hashCode(this);
  }

  public boolean equals(Object obj) {
    return Equals.equals(this, obj);
  }

  public String toString() {
    return ToStrings.toString(this);
  }
}
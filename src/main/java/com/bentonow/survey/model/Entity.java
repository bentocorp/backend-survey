package com.bentonow.survey.model;

import java.sql.Connection;
import java.sql.SQLException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.sql.DataSource;

import org.safris.commons.dbcp.DataSources;
import org.safris.commons.lang.Equals;
import org.safris.commons.lang.HashCodes;
import org.safris.commons.lang.ToStrings;
import org.safris.commons.sql.ConnectionProxy;

import com.bentonow.survey.Config;

public abstract class Entity {
  protected static final HostnameVerifier hostnameVerifier = new HostnameVerifier() {
    @Override
    public boolean verify(final String arg0, final SSLSession arg1) {
      return true;
    }
  };

  private static final DataSource dataSource;

  static {
    try {
      dataSource = DataSources.createDataSource(Config.getConfig()._db(0).dbcp_dbcp(0));
    }
    catch (final SQLException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static Connection getConnection() throws SQLException {
    return new ConnectionProxy(dataSource.getConnection());
  }

  @Override
  public int hashCode() {
    return HashCodes.hashCode(this);
  }

  @Override
  public boolean equals(final Object obj) {
    return Equals.equals(this, obj);
  }

  @Override
  public String toString() {
    return ToStrings.toString(this);
  }
}
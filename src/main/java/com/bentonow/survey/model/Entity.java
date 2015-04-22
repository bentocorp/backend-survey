package com.bentonow.survey.model;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.safris.commons.dbcp.DataSources;
import org.safris.commons.dbcp.dbcp_dbcp;
import org.safris.commons.lang.Equals;
import org.safris.commons.lang.HashCodes;
import org.safris.commons.lang.Resources;
import org.safris.commons.lang.ToStrings;
import org.safris.commons.sql.ConnectionProxy;
import org.safris.xml.generator.compiler.runtime.Bindings;
import org.xml.sax.InputSource;

public abstract class Entity {
  private static final DataSource dataSource;

  static {
    try {
      final dbcp_dbcp dbcp = (dbcp_dbcp)Bindings.parse(new InputSource(Resources.getResource("dbcp.xml").getURL().openStream()));
      dataSource = DataSources.createDataSource(dbcp);
    }
    catch (final Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  protected static Connection getConnection() throws SQLException {
    return ConnectionProxy.getInstance(dataSource.getConnection());
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
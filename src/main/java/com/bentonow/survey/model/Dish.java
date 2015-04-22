package com.bentonow.survey.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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
  private static final String dishServiceUrl = "https://api.bentonow.com/extapi/dish/${id}?" + Config.getParameters(Config.getConfig()._webApi(0)._parameters(0));
  private static final Map<Integer,Dish> idToDish = new HashMap<Integer,Dish>();

  public static Dish create(final int id, final String name, final String description, final Type type, final String imageUrl) throws IOException, SQLException {
    logger.finest(id + "...");
    Dish instance = idToDish.get(id);
    if (instance != null)
      return instance;

    synchronized (idToDish) {
      instance = idToDish.get(id);
      if (instance != null)
        return instance;

      idToDish.put(id, instance = new Dish(id, name, description, type, imageUrl));
      return instance;
    }
  }

  public static Dish fetch(final int id) throws IOException, SQLException {
    logger.finest("" + id);
    Dish instance = idToDish.get(id);
    if (instance != null)
      return instance;

    synchronized (idToDish) {
      instance = idToDish.get(id);
      if (instance != null)
        return instance;

      try (
        final Connection connection = getConnection();
        final PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM dish WHERE id = ?");
      ) {
        selectStatement.setInt(1, id);
        final ResultSet resultSet = selectStatement.executeQuery();
        final String name;
        final String description;
        final Type type;
        final String imageUrl;
        if (resultSet.next()) {
          name = resultSet.getString(2);
          description = resultSet.getString(3);
          type = Type.valueOf(resultSet.getString(4).toUpperCase());
          imageUrl = resultSet.getString(5);
        }
        else {
          final URL serviceUrl = new URL(dishServiceUrl.replace("${id}", String.valueOf(id)));
          logger.info("serviceUrl: " + serviceUrl.toExternalForm());
          final URLConnection urlConnection = serviceUrl.openConnection();
          try (final InputStream in = urlConnection.getInputStream()) {
            final JsonElement json = new JsonParser().parse(new InputStreamReader(in));
            final JsonObject object = json.getAsJsonObject();
            name = object.get("name").getAsString();
            description = object.get("description").getAsString();
            type = Type.valueOf(object.get("type").getAsString().toUpperCase());
            imageUrl = object.get("email_image1").getAsString();
          }

          try (final PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO dish VALUES (?, ?, ?, ?, ?)")) {
            insertStatement.setInt(1, id);
            insertStatement.setString(2, name);
            insertStatement.setString(3, description);
            insertStatement.setString(4, type.toString());
            insertStatement.setString(5, imageUrl);
            insertStatement.executeUpdate();
          }
        }

        idToDish.put(id, instance = new Dish(id, name, description, type, imageUrl));
        return instance;
      }
    }
  }

  public final int id;
  public final String name;
  public final String description;
  public final Type type;
  public final String imageUrl;

  private Dish(final int id, final String name, final String description, final Type type, final String imageUrl) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.type = type;
    this.imageUrl = imageUrl;
  }
}
package com.bentonow.survey.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bentonow.resource.survey.config.cf_config;
import com.bentonow.survey.model.DishSurvey;
import com.bentonow.survey.model.MealSurvey;

@WebServlet(urlPatterns = {"/thankyou"})
public class SubmissionServlet extends TemplatedServlet {
  private static final long serialVersionUID = 6180095764961424067L;

  public SubmissionServlet(final cf_config config) throws IOException {
    super(config, "thankyou.html", "yelp.html");
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    log(request.getParameterMap().toString());
    String content = template[0];
    try {
      int mealId = -1;
      Integer mealRating = null;
      String mealComment = null;
      Enumeration<String> parameterNames = request.getParameterNames();
      while (parameterNames.hasMoreElements()) {
        final String parameter = parameterNames.nextElement();
        if (parameter.startsWith("c")) {
          mealId = Integer.parseInt(parameter.substring(1));
          mealComment = request.getParameter(parameter);
          final String ratingParam = request.getParameter("r" + mealId);
          mealRating = ratingParam == null ? null : Integer.parseInt(ratingParam);
          break;
        }
      }

      if (MealSurvey.lookupMealSurvey(mealId) == null) {
        int dishId;
        String dishRating;
        String dishComment;
        parameterNames = request.getParameterNames();
        final List<DishSurvey> dishSurveys = new ArrayList<DishSurvey>();
        while (parameterNames.hasMoreElements()) {
          final String parameter = parameterNames.nextElement();
          if (parameter.startsWith("d") && parameter.endsWith("c")) {
            dishId = Integer.parseInt(parameter.substring(1, parameter.length() - 1));
            dishRating = request.getParameter("d" + dishId + "r");
            dishComment = request.getParameter(parameter);
            dishSurveys.add(new DishSurvey(mealId, dishId, dishRating != null ? Integer.parseInt(dishRating) : null, dishComment));
          }
        }

        MealSurvey.insert(mealId, mealRating, mealComment, dishSurveys);
      }
      else {
        log("MealSurvey with id " + mealId + " already submitted... skipping..");
      }

      if (mealRating == null || mealRating > 8)
        content = content.replace("<!-- yelp -->", template[1]);
    }
    catch (final NumberFormatException e) {
      log(e.getMessage(), e);
    }
    catch (final SQLException e) {
      throw new ServletException(e);
    }

    response.getOutputStream().write(content.getBytes());
  }
}
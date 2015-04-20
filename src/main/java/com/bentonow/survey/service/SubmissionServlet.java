package com.bentonow.survey.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bentonow.resource.survey.config.$cf_server;
import com.bentonow.survey.model.DishSurvey;
import com.bentonow.survey.model.MealSurvey;

@WebServlet(urlPatterns = {"/thankyou"})
public class SubmissionServlet extends TemplatedServlet {
  private static final long serialVersionUID = 6180095764961424067L;

  public SubmissionServlet(final $cf_server config) throws IOException {
    super(config, "thankyou.html", "yelp.html");
  }

  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    response.sendError(404);
  }

  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    log(request.getParameterMap().toString());

    String content = template[0];
    final String mealId = request.getParameter("i");
    try {
      final int mealRating = Integer.parseInt(request.getParameter("r"));
      if (MealSurvey.lookupMealSurvey(mealId) == null) {
        // TODO: what do we do with the extra email the customer provides on the survey form???
        // String email = request.getParameter("e");
        // try {
        // new InternetAddress(email).validate();
        // }
        // catch (final AddressException e) {
        // email = null;
        // }

        final String mealComment = request.getParameter("c");
        final List<DishSurvey> dishSurveys = new ArrayList<DishSurvey>();

        String dishId;
        String dishRating;
        String dishComment;
        for (int i = 0; (dishId = request.getParameter("d" + i + "i")) != null; i++) {
          dishRating = request.getParameter("d" + i + "r");
          dishComment = request.getParameter("d" + i + "c");
          dishSurveys.add(new DishSurvey(mealId, Integer.parseInt(dishId), Integer.parseInt(dishRating), dishComment));
        }

        MealSurvey.insert(mealId, mealRating, mealComment, dishSurveys);
      }
      else {
        log("MealSurvey with id " + mealId + " already submitted... skipping..");
      }

      if (mealRating > 8)
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
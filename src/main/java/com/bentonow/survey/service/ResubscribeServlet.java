package com.bentonow.survey.service;

import java.io.IOException;

import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bentonow.resource.survey.config.xe.cf_config;
import com.bentonow.survey.model.Subscription;

@WebServlet(urlPatterns={"/r/*"})
public class ResubscribeServlet extends TemplatedServlet {
  private static final long serialVersionUID = -5032490249230923435L;

  public ResubscribeServlet(final cf_config config) throws IOException {
    super(config, "resubscribed.html");
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    log(request.getParameterMap().toString());

    final String email = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
    if (email == null) {
      response.sendError(400);
      return;
    }

    try {
      new InternetAddress(email).validate();
      Subscription.resubscribe(email);
    }
    catch (final Exception e) {
      log(e.getMessage(), e);
      response.sendError(409);
      return;
    }

    response.getOutputStream().write(template[0].replace("${email}", email).getBytes());
  }
}
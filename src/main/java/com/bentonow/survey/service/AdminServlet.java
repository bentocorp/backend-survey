package com.bentonow.survey.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns={"/admin"})
public class AdminServlet extends HttpServlet {
  private static final long serialVersionUID = 7390946258203407208L;

  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    String out = "";
    out += "<html>";
    out += "  <head>";
    out += "    <title>SCV Upload Form</title>";
    out += "  </head>";
    out += "  <body>";
    out += "    <h3>CSV File Upload:</h3>";
    out += "    <form action='/upload' method='POST' enctype='multipart/form-data'>";
    out += "      <input type='file' name='file' size='50'/>";
    out += "      <br/>";
    out += "      <br/>";
    out += "      <input type='submit' value='Upload File' disabled/>";
    out += "    </form>";
    out += "  </body>";
    out += "</html>";

    response.getOutputStream().write(out.getBytes());
  }
}
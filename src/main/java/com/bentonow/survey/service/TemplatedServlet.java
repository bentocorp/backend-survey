package com.bentonow.survey.service;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.safris.commons.io.Streams;
import org.safris.commons.lang.Resource;
import org.safris.commons.lang.Resources;

import com.bentonow.resource.survey.config.$cf_mail;
import com.bentonow.resource.survey.config.$cf_mail._serviceDestination._scheme$;
import com.bentonow.resource.survey.config.$cf_server;
import com.bentonow.resource.survey.config.cf_config;

@SuppressWarnings("serial")
public abstract class TemplatedServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(TemplatedServlet.class.getName());

  private static final Object mutex = new Object();
  private static String serverUrl = null;

  public static String filter(final $cf_mail config, final String string) throws IOException {
    if (serverUrl == null) {
      synchronized (mutex) {
        if (serverUrl == null) {
          final int port = config._serviceDestination(0)._port$().text();
          serverUrl = config._serviceDestination(0)._host$().text();
          if (_scheme$.https.text().equals(config._serviceDestination(0)._scheme$().text()))
            serverUrl = port == 443 ? "https://" + serverUrl : "https://" + serverUrl + ":" + port;
          else if (_scheme$.http.text().equals(config._serviceDestination(0)._scheme$().text()))
            serverUrl = port == 80 ? "http://" + serverUrl : "http://" + serverUrl + ":" + port;
          else
            throw new UnsupportedOperationException("Unsupported scheme: " + config._serviceDestination(0)._scheme$().text());
        }
      }
    }

    logger.info("setting ${serverUrl} to " + serverUrl);
    return string.replace("${serverUrl}", serverUrl);
  }

  protected final $cf_server config;
  protected final String[] template;

  protected TemplatedServlet(final cf_config config, final String ... template) throws IOException {
    this.config = config._server(0);
    this.template = new String[template.length];
    for (int i = 0; i < template.length; i++) {
      final Resource resubscribeResource = Resources.getResource(template[i]);
      this.template[i] = filter(config._mail(0), new String(Streams.getBytes(resubscribeResource.getURL().openStream())));
    }
  }

  @Override
  protected final void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    doPost(request, response);
  }
}
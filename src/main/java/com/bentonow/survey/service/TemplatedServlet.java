package com.bentonow.survey.service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.safris.commons.io.Streams;
import org.safris.commons.lang.Resource;
import org.safris.commons.lang.Resources;
import org.safris.commons.net.InetAddresses;

import com.bentonow.resource.survey.config.$cf_http;
import com.bentonow.resource.survey.config.$cf_https;
import com.bentonow.resource.survey.config.$cf_server;

@SuppressWarnings("serial")
public abstract class TemplatedServlet extends HttpServlet {
  private static final Logger logger = Logger.getLogger(TemplatedServlet.class.getName());

  private static final Object mutex = new Object();
  private static String serverUrl = null;

  public static String filter(final $cf_server config, final String string) throws IOException {
    if (serverUrl == null) {
      synchronized (mutex) {
        if (serverUrl == null) {
          final int port = config._port$().text();
          serverUrl = !config._host$().isNull() ? config._host$().text() : InetAddresses.toStringIP(InetAddress.getLocalHost());
          if (config instanceof $cf_http)
            serverUrl = port == 80 ? "http://" + serverUrl : "http://" + serverUrl + ":" + port;
          else if (config instanceof $cf_https)
            serverUrl = port == 443 ? "https://" + serverUrl : "https://" + serverUrl + ":" + port;
          else
            throw new Error("Unexpected server type: " + config.name());
        }
      }
    }

    logger.info("setting ${serverUrl} to " + serverUrl);
    return string.replace("${serverUrl}", serverUrl);
  }

  protected final $cf_server config;
  protected final String[] template;

  protected TemplatedServlet(final $cf_server config, final String ... template) throws IOException {
    this.config = config;
    this.template = new String[template.length];
    for (int i = 0; i < template.length; i++) {
      final Resource resubscribeResource = Resources.getResource(template[i]);
      this.template[i] = filter(config, new String(Streams.getBytes(resubscribeResource.getURL().openStream())));
    }
  }

  protected final void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    doPost(request, response);
  }
}
package com.bentonow.survey.service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;

import org.safris.commons.io.Streams;
import org.safris.commons.lang.Resource;
import org.safris.commons.lang.Resources;
import org.safris.commons.net.InetAddresses;

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
          if (port == 80)
            serverUrl = "http://" + serverUrl;
          else if (port == 443)
            serverUrl = "https://" + serverUrl;
          else
            serverUrl = "http://" + serverUrl + ":" + port;
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
}

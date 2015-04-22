package com.bentonow.survey;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import org.safris.commons.xml.XMLException;
import org.safris.commons.xml.dom.DOMStyle;
import org.safris.commons.xml.dom.DOMs;
import org.safris.xml.generator.compiler.runtime.Bindings;
import org.xml.sax.InputSource;

import com.bentonow.resource.survey.config.$cf_parameters;
import com.bentonow.resource.survey.config.cf_config;

public class Config {
  private static final Logger logger = Logger.getLogger(Config.class.getName());

  private static volatile boolean inited = false;
  private static final Object mutex = new Object();
  private static cf_config config;

  public static void load(final URL configURL) throws IOException, XMLException {
    if (inited)
      throw new IllegalStateException(Config.class.getName() + ".load() has already been called");

    synchronized (mutex) {
      if (inited)
        throw new IllegalStateException(Config.class.getName() + ".load() has already been called");

      config = (cf_config)Bindings.parse(new InputSource(configURL.openStream()));
      logger.info(DOMs.domToString(Bindings.marshal(Config.config), DOMStyle.INDENT));
      inited = true;
    }
  }

  public static cf_config getConfig() {
    return config;
  }

  public static String getParameters(final $cf_parameters parameters) {
    if (parameters.isNull())
      return "";

    final StringBuilder buffer = new StringBuilder();
    for (final $cf_parameters._parameter parameter : parameters._parameter())
      buffer.append("&").append(parameter._name$().text()).append("=").append(parameter._value$().text());

    return buffer.substring(1);
  }
}
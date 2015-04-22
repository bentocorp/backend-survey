package com.bentonow.survey;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.safris.commons.xml.XMLException;
import org.safris.commons.xml.dom.DOMStyle;
import org.safris.commons.xml.dom.DOMs;
import org.safris.xml.generator.compiler.runtime.Bindings;
import org.xml.sax.InputSource;

import com.bentonow.resource.survey.config.$cf_server;
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

  public static Map<String,String> getParameters(final List<$cf_server._webApi._parameter> parameters) {
    final Map<String,String> map = new HashMap<String,String>();
    for (final $cf_server._webApi._parameter parameter : parameters)
      map.put(parameter._name$().text(), parameter._value$().text());

    return map;
  }
}
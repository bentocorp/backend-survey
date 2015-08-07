package com.bentonow.survey;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.safris.commons.cli.Options;
import org.safris.commons.lang.Resources;

import com.bentonow.resource.survey.config.$cf_http;
import com.bentonow.resource.survey.config.$cf_https;
import com.bentonow.resource.survey.config.$cf_logging;
import com.bentonow.resource.survey.config.cf_config;
import com.bentonow.survey.service.AdminServlet;
import com.bentonow.survey.service.ReportServlet;
import com.bentonow.survey.service.ResubscribeServlet;
import com.bentonow.survey.service.SubmissionServlet;
import com.bentonow.survey.service.UnsubscribeServlet;

public class Server {
  private static final Logger logger = Logger.getLogger(Server.class.getName());

  public static void main(final String[] args) throws Exception {
    final Options options = Options.parse(Resources.getResource("cli.xml").getURL(), Server.class, args);
    logger.info(options.toString());
    final String configArg = options.getOption("config");
    Config.load(configArg != null ? new File(configArg).toURI().toURL() : Resources.getResource("config.xml").getURL());
    final cf_config config = Config.getConfig();

    if (config._debug(0)._logging() != null) {
      final Logger rootLogger = Logger.getLogger("");
      final Level globalLevel = Level.parse(config._debug(0)._logging(0)._global$().text());
      rootLogger.setLevel(globalLevel);
      for (final Handler handler : rootLogger.getHandlers())
        handler.setLevel(globalLevel);

      if (config._debug(0)._logging(0)._logger() != null)
        for (final $cf_logging logger : config._debug(0)._logging(0)._logger())
          Logger.getLogger(logger._name$().text()).setLevel(Level.parse(logger._level$().text()));
    }

    final org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();
    server.setConnectors(new Connector[] {makeConnector(server, config)});

    final ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    addAuthRealm(servletContextHandler, config._admin(0)._credentials(0)._username$().text(), config._admin(0)._credentials(0)._password$().text(), "Restricted", "/a");
    addServlet(servletContextHandler, new AdminServlet());
    addServlet(servletContextHandler, new ReportServlet());
    addServlet(servletContextHandler, new ResubscribeServlet(config));
    addServlet(servletContextHandler, new SubmissionServlet(config));
    addServlet(servletContextHandler, new UnsubscribeServlet(config));

    final HandlerList handlerList = new HandlerList();

    if (!config._debug(0)._externalResourcesAccess$().isNull() && config._debug(0)._externalResourcesAccess$().text()) {
      // FIXME: HACK: Why cannot I just get the "/" resource? In the IDE it works, but in the stand-alone jar, it does not
      final String configResourcePath = Resources.getResource("config.xml").getURL().toExternalForm();
      final URL rootResourceURL = new URL(configResourcePath.substring(0, configResourcePath.length() - "config.xml".length()));

      final ResourceHandler resourceHandler = new ResourceHandler();
      resourceHandler.setDirectoriesListed(true);
      resourceHandler.setBaseResource(Resource.newResource(rootResourceURL));

      handlerList.addHandler(resourceHandler);
    }

    handlerList.addHandler(servletContextHandler);

    server.setHandler(handlerList);

    server.start();

    final MailSender command = new MailSender(config._mail(0));
    final Worker worker = new Worker(config._worker(0), command);
    worker.start();

    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      public void uncaughtException(final Thread t, final Throwable e) {
        command.sendException(e);
        e.printStackTrace();
      }
    });

    server.join();
  }

  private static Connector makeConnector(final org.eclipse.jetty.server.Server server, final cf_config config) {
    if (config._server(0) instanceof $cf_http) {
      final ServerConnector connector = new ServerConnector(server);
      connector.setPort(config._server(0)._port$().text());
      return connector;
    }

    final $cf_https httpsConfig = ($cf_https)config._server(0);

    final HttpConfiguration https = new HttpConfiguration();
    https.addCustomizer(new SecureRequestCustomizer());

    final SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setKeyStorePath(Resources.getResource(httpsConfig._keystore(0)._path$().text()).getURL().toExternalForm());
    sslContextFactory.setKeyStorePassword(httpsConfig._keystore(0)._password$().text());
//    sslContextFactory.setKeyManagerPassword("123456");

    final ServerConnector connector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
    connector.setPort(config._server(0)._port$().text());
    return connector;
  }

  private static void addServlet(final ServletContextHandler handler, final HttpServlet servlet) {
    final WebServlet annotation = servlet.getClass().getAnnotation(WebServlet.class);
    if (annotation == null)
      throw new Error(servlet.getClass().getName() + " does not have a @" + WebServlet.class.getName() + " annotation.");

    if (annotation.urlPatterns() == null || annotation.urlPatterns().length == 0)
      throw new Error(servlet.getClass().getName() + " does not have urlPatterns parameter in the @" + WebServlet.class.getName() + " annotation.");

    logger.info(servlet.getClass().getName() + " " + Arrays.toString(annotation.urlPatterns()));
    for (final String urlPattern : annotation.urlPatterns())
      handler.addServlet(new ServletHolder(servlet), urlPattern);
  }

  private static final void addAuthRealm(final ServletContextHandler handler, final String username, final String password, final String realm, final String ... path) {
    logger.info(realm + ": " + Arrays.toString(path));
    HashLoginService loginService = new HashLoginService();
    loginService.putUser(username, Credential.getCredential(password), new String[] {
      "user"
    });
    loginService.setName(realm);

    final Constraint constraint = new Constraint();
    constraint.setName(Constraint.__BASIC_AUTH);
    constraint.setRoles(new String[] {
      "user"
    });
    constraint.setAuthenticate(true);

    final ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
    securityHandler.setAuthenticator(new BasicAuthenticator());
    securityHandler.setRealmName("myrealm");
    securityHandler.setLoginService(loginService);

    for (final String p : path) {
      final ConstraintMapping constraintMapping = new ConstraintMapping();
      constraintMapping.setConstraint(constraint);
      constraintMapping.setPathSpec(p);
      securityHandler.addConstraintMapping(constraintMapping);
    }

    handler.setSecurityHandler(securityHandler);
  }
}
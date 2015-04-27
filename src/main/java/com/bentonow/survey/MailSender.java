package com.bentonow.survey;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.mail.MessagingException;

import org.safris.commons.io.Streams;
import org.safris.commons.lang.Resource;
import org.safris.commons.lang.Resources;
import org.safris.commons.net.mail.Mail;
import org.safris.commons.net.mail.MimeContent;

import com.bentonow.resource.survey.config.$cf_mail;
import com.bentonow.resource.survey.config.$cf_server;
import com.bentonow.survey.model.Dish;
import com.bentonow.survey.model.Meal;
import com.bentonow.survey.service.TemplatedServlet;

public class MailSender {
  private static final Logger logger = Logger.getLogger(MailSender.class.getName());

  private final String container;
  private final String template;
  private final Mail.Server server;
  private final Mail.Credentials credentials;
  private final String from;
  private final String subject;
  private final String[] toOverride;

  public MailSender(final $cf_server serverConfig, final $cf_mail mailConfig) throws IOException {
    final Resource containerResource = Resources.getResource("container.html");
    container = TemplatedServlet.filter(serverConfig, new String(Streams.getBytes(containerResource.getURL().openStream())));

    final Resource templateResource = Resources.getResource("template.html");
    template = TemplatedServlet.filter(serverConfig, new String(Streams.getBytes(templateResource.getURL().openStream())));

    server = Mail.Server.instance(Mail.Protocol.valueOf(mailConfig._server(0)._protocol$().text().toUpperCase()), mailConfig._server(0)._host$().text(), mailConfig._server(0)._port$().text());
    credentials = new Mail.Credentials(mailConfig._server(0)._credentials(0)._username$().text(), mailConfig._server(0)._credentials(0)._password$().text());
    from = mailConfig._message(0)._from$().text();
    subject = mailConfig._message(0)._subject$().text();
    toOverride = !mailConfig._message(0)._override(0).isNull() ? mailConfig._message(0)._override(0)._to$().text().toArray(new String[mailConfig._message(0)._override(0)._to$().text().size()]) : null;
  }

  public void send(final List<Meal> meals) {
    logger.info("" + meals.size());
    try {
      if (meals.size() == 0)
        return;

      final Mail.Message[] messages = new Mail.Message[meals.size()];
      for (int i = 0; i < meals.size(); i++) {
        final Meal meal = meals.get(i);
        String questions = "";
        for (int j = 0; j < meal.dishes.size(); j++) {
          final Dish dish = meal.dishes.get(j);
          questions += "\n" + template.replace("${dishId}", "" + dish.id).replace("${imageUrl}", dish.imageUrl != null ? dish.imageUrl : "").replace("${name}", dish.name).replace("${itemNo}", "" + j);
        }

        final String[] to = toOverride != null ? toOverride : new String[] {meal.email};
        final String content = container.replace("${mealId}", "" + meal.id).replace("${email}", to[0]).replace("<!-- items-data -->", questions.substring(1)).replace(" id=\"items-container\"", "");
        messages[i] = new Mail.Message(subject, new MimeContent(content, "text/html"), from, to) {
          public void success() {
            meal.sent(true);
          }

          public void failure(final MessagingException e) {
          }
        };
      }

      server.send(credentials, messages);
    }
    catch (final MessagingException e) {
      logger.throwing(MailSender.class.getName(), "run", e);
    }
  }
}
package com.bentonow.survey;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.safris.commons.lang.DateUtil;
import org.safris.commons.lang.Numbers;
import org.safris.commons.xml.binding.Time;
import org.safris.xml.generator.compiler.runtime.Bindings;

import com.bentonow.resource.survey.config.$cf_days;
import com.bentonow.resource.survey.config.$cf_execute;
import com.bentonow.resource.survey.config.$cf_hours;
import com.bentonow.resource.survey.config.$cf_minutes;
import com.bentonow.resource.survey.config.$cf_seconds;
import com.bentonow.resource.survey.config.$cf_worker;
import com.bentonow.survey.model.Meal;

public class Worker implements Runnable {
  private static final Logger logger = Logger.getLogger(Worker.class.getName());

  private final $cf_worker config;
  private final MailSender sender;
  private final int startMinutesBeforeNow;
  private final int endMinutesBeforeNow;

  public Worker(final $cf_worker config, final MailSender sender) {
    this.config = config;
    this.sender = sender;
    startMinutesBeforeNow = config._match(0)._start(0)._minutesBeforeNow$().text();
    endMinutesBeforeNow = config._match(0)._end(0)._minutesBeforeNow$().text();
  }

  public void start() {
    final $cf_execute every = config._execute(0);
    final Calendar calendar = Calendar.getInstance();
    final long now = DateUtil.dropMilliseconds(System.currentTimeMillis());
    calendar.setTimeInMillis(now);

    final long period;
    if (every instanceof $cf_seconds) {
      final $cf_seconds schedule = ($cf_seconds)every;
      period = schedule._every$().text() * 1000;
    }
    else if (every instanceof $cf_minutes) {
      final $cf_minutes schedule = ($cf_minutes)every;
      period = schedule._every$().text() * 60 * 1000;

      final int begin = schedule._begin$().text();
      if (calendar.get(Calendar.SECOND) > begin)
        calendar.add(Calendar.MINUTE, 1);

      calendar.set(Calendar.SECOND, begin);
    }
    else if (every instanceof $cf_hours) {
      final $cf_hours schedule = ($cf_hours)every;
      calendar.setTimeZone(TimeZone.getTimeZone(schedule._timeZone$().text()));
      period = schedule._every$().text() * 60 * 60 * 1000;

      final int[] begin = Numbers.parseInteger(schedule._begin$().text().split(":"));
      if (calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND) > begin[0] * 60 + begin[1])
        calendar.add(Calendar.HOUR, 1);

      calendar.set(Calendar.MINUTE, begin[0]);
      calendar.set(Calendar.SECOND, begin[1]);
    }
    else if (every instanceof $cf_days) {
      final $cf_days schedule = ($cf_days)every;
      calendar.setTimeZone(TimeZone.getTimeZone(schedule._timeZone$().text()));
      period = schedule._every$().text() * 24 * 60 * 60 * 1000;

      final Time begin = schedule._begin$().text();
      if ((calendar.get(Calendar.HOUR) * 60 + calendar.get(Calendar.MINUTE)) * 60 + calendar.get(Calendar.SECOND) > (begin.getHour() * 60 + begin.getMinute()) * 60 + begin.getSecond())
        calendar.add(Calendar.DAY_OF_MONTH, 1);

      calendar.set(Calendar.HOUR, begin.getHour());
      calendar.set(Calendar.MINUTE, begin.getMinute());
      calendar.set(Calendar.SECOND, (int)begin.getSecond());
    }
    else {
      throw new UnsupportedOperationException("<cf:every> of type " + Bindings.getTypeName(every));
    }

    final long delay = calendar.getTimeInMillis() - now;
    logger.info("executing command with " + delay + " delay, and " + period + " period");

    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    executor.scheduleWithFixedDelay(this, delay, period, TimeUnit.MILLISECONDS);
  }

  public void run() {
    try {
      final long now = DateUtil.dropMilliseconds(System.currentTimeMillis());
      final List<Meal> meals = Meal.fetchUnsent(now - startMinutesBeforeNow * DateUtil.MILLISECONDS_IN_MINUTE, now - endMinutesBeforeNow * DateUtil.MILLISECONDS_IN_MINUTE);
      if (meals.size() > 0)
        sender.send(meals);
    }
    catch (final Exception e) {
      logger.throwing(MailSender.class.getName(), "run", e);
    }
  }
}
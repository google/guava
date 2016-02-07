package com.google.common.util.concurrent;

import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.Immutable;

/**
 * @author Ram Anvesh Reddy
 */
@Immutable
final class TimeDuration {

  private static final TimeDuration INFINITE_TIME = new TimeDuration(-1, TimeUnit.NANOSECONDS);

  private long duration;

  private TimeUnit unit;

  private TimeDuration(long duration, TimeUnit unit) {
    this.duration = duration;
    this.unit = unit;
  }

  static TimeDuration of(long duration, TimeUnit unit) {
    if (null == unit) {
      throw new NullPointerException("TimeUnit cannot be null");
    }
    if (duration < 0) {
      throw new IllegalArgumentException("duration cannot be negative");
    }
    return new TimeDuration(duration, unit);
  }

  static TimeDuration infinity() {
    return INFINITE_TIME;
  }

  public long getDuration() {
    return duration;
  }

  public TimeUnit getUnit() {
    return unit;
  }

  boolean isInfinity() {
    return this == INFINITE_TIME;
  }
}

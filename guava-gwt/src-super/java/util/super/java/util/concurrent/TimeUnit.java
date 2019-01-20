/*
 * This file is a modified version of
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/TimeUnit.java
 * which contained the following notice:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * GWT emulation of TimeUnit, created by removing unsupported operations from
 * Doug Lea's public domain version.
 */
public enum TimeUnit {
  NANOSECONDS {
    public long toNanos(long d)   { return d; }
    public long toMicros(long d)  { return d / C1_C0; }
    public long toMillis(long d)  { return d / C2_C0; }
    public long toSeconds(long d) { return d / C3_C0; }
    public long toMinutes(long d) { return d / C4_C0; }
    public long toHours(long d)   { return d / C5_C0; }
    public long toDays(long d)    { return d / C6_C0; }
    public long convert(long d, TimeUnit u) { return u.toNanos(d); }
  },
  MICROSECONDS {
    public long toNanos(long d)   { return x(d, C1_C0, MAX_C1_C0); }
    public long toMicros(long d)  { return d; }
    public long toMillis(long d)  { return d / C2_C1; }
    public long toSeconds(long d) { return d / C3_C1; }
    public long toMinutes(long d) { return d / C4_C1; }
    public long toHours(long d)   { return d / C5_C1; }
    public long toDays(long d)    { return d / C6_C1; }
    public long convert(long d, TimeUnit u) { return u.toMicros(d); }
  },
  MILLISECONDS {
    public long toNanos(long d)   { return x(d, C2_C0, MAX_C2_C0); }
    public long toMicros(long d)  { return x(d, C2_C1, MAX_C2_C1); }
    public long toMillis(long d)  { return d; }
    public long toSeconds(long d) { return d / C3_C2; }
    public long toMinutes(long d) { return d / C4_C2; }
    public long toHours(long d)   { return d / C5_C2; }
    public long toDays(long d)    { return d / C6_C2; }
    public long convert(long d, TimeUnit u) { return u.toMillis(d); }
  },
  SECONDS {
    public long toNanos(long d)   { return x(d, C3_C0, MAX_C3_C0); }
    public long toMicros(long d)  { return x(d, C3_C1, MAX_C3_C1); }
    public long toMillis(long d)  { return x(d, C3_C2, MAX_C3_C2); }
    public long toSeconds(long d) { return d; }
    public long toMinutes(long d) { return d / C4_C3; }
    public long toHours(long d)   { return d / C5_C3; }
    public long toDays(long d)    { return d / C6_C3; }
    public long convert(long d, TimeUnit u) { return u.toSeconds(d); }
  },
  MINUTES {
    public long toNanos(long d)   { return x(d, C4_C0, MAX_C4_C0); }
    public long toMicros(long d)  { return x(d, C4_C1, MAX_C4_C1); }
    public long toMillis(long d)  { return x(d, C4_C2, MAX_C4_C2); }
    public long toSeconds(long d) { return x(d, C4_C3, MAX_C4_C3); }
    public long toMinutes(long d) { return d; }
    public long toHours(long d)   { return d / C5_C4; }
    public long toDays(long d)    { return d / C6_C4; }
    public long convert(long d, TimeUnit u) { return u.toMinutes(d); }
  },
  HOURS {
    public long toNanos(long d)   { return x(d, C5_C0, MAX_C5_C0); }
    public long toMicros(long d)  { return x(d, C5_C1, MAX_C5_C1); }
    public long toMillis(long d)  { return x(d, C5_C2, MAX_C5_C2); }
    public long toSeconds(long d) { return x(d, C5_C3, MAX_C5_C3); }
    public long toMinutes(long d) { return x(d, C5_C4, MAX_C5_C4); }
    public long toHours(long d)   { return d; }
    public long toDays(long d)    { return d / C6_C5; }
    public long convert(long d, TimeUnit u) { return u.toHours(d); }
  },
  DAYS {
    public long toNanos(long d)   { return x(d, C6_C0, MAX_C6_C0); }
    public long toMicros(long d)  { return x(d, C6_C1, MAX_C6_C1); }
    public long toMillis(long d)  { return x(d, C6_C2, MAX_C6_C2); }
    public long toSeconds(long d) { return x(d, C6_C3, MAX_C6_C3); }
    public long toMinutes(long d) { return x(d, C6_C4, MAX_C6_C4); }
    public long toHours(long d)   { return x(d, C6_C5, MAX_C6_C5); }
    public long toDays(long d)    { return d; }
    public long convert(long d, TimeUnit u) { return u.toDays(d); }
  };

  // Handy constants for conversion methods
  private static final long C0 = 1L;
  private static final long C1 = C0 * 1000L;
  private static final long C2 = C1 * 1000L;
  private static final long C3 = C2 * 1000L;
  private static final long C4 = C3 * 60L;
  private static final long C5 = C4 * 60L;
  private static final long C6 = C5 * 24L;

  private static final long MAX = Long.MAX_VALUE;

  private static final long C6_C0 = C6 / C0;
  private static final long C6_C1 = C6 / C1;
  private static final long C6_C2 = C6 / C2;
  private static final long C6_C3 = C6 / C3;
  private static final long C6_C4 = C6 / C4;
  private static final long C6_C5 = C6 / C5;

  private static final long C5_C0 = C5 / C0;
  private static final long C5_C1 = C5 / C1;
  private static final long C5_C2 = C5 / C2;
  private static final long C5_C3 = C5 / C3;
  private static final long C5_C4 = C5 / C4;

  private static final long C4_C0 = C4 / C0;
  private static final long C4_C1 = C4 / C1;
  private static final long C4_C2 = C4 / C2;
  private static final long C4_C3 = C4 / C3;

  private static final long C3_C0 = C3 / C0;
  private static final long C3_C1 = C3 / C1;
  private static final long C3_C2 = C3 / C2;

  private static final long C2_C0 = C2 / C0;
  private static final long C2_C1 = C2 / C1;

  private static final long C1_C0 = C1 / C0;

  private static final long MAX_C6_C0 = MAX / C6_C0;
  private static final long MAX_C6_C1 = MAX / C6_C1;
  private static final long MAX_C6_C2 = MAX / C6_C2;
  private static final long MAX_C6_C3 = MAX / C6_C3;
  private static final long MAX_C6_C4 = MAX / C6_C4;
  private static final long MAX_C6_C5 = MAX / C6_C5;

  private static final long MAX_C5_C0 = MAX / C5_C0;
  private static final long MAX_C5_C1 = MAX / C5_C1;
  private static final long MAX_C5_C2 = MAX / C5_C2;
  private static final long MAX_C5_C3 = MAX / C5_C3;
  private static final long MAX_C5_C4 = MAX / C5_C4;

  private static final long MAX_C4_C0 = MAX / C4_C0;
  private static final long MAX_C4_C1 = MAX / C4_C1;
  private static final long MAX_C4_C2 = MAX / C4_C2;
  private static final long MAX_C4_C3 = MAX / C4_C3;

  private static final long MAX_C3_C0 = MAX / C3_C0;
  private static final long MAX_C3_C1 = MAX / C3_C1;
  private static final long MAX_C3_C2 = MAX / C3_C2;

  private static final long MAX_C2_C0 = MAX / C2_C0;
  private static final long MAX_C2_C1 = MAX / C2_C1;

  private static final long MAX_C1_C0 = MAX / C1_C0;

  static long x(long d, long m, long over) {
    if (d >  over) return Long.MAX_VALUE;
    if (d < -over) return Long.MIN_VALUE;
    return d * m;
  }

  public abstract long convert(long sourceDuration, TimeUnit sourceUnit);

  public abstract long toNanos(long duration);

  public abstract long toMicros(long duration);

  public abstract long toMillis(long duration);

  public abstract long toSeconds(long duration);

  public abstract long toMinutes(long duration);

  public abstract long toHours(long duration);

  public abstract long toDays(long duration);
}

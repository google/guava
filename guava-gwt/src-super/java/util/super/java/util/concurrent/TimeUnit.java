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
    int excessNanos(long d, long m) { return (int)(d - (m*C2)); }
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
    int excessNanos(long d, long m) { return (int)((d*C1) - (m*C2)); }
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
    int excessNanos(long d, long m) { return 0; }
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
    int excessNanos(long d, long m) { return 0; }
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
    int excessNanos(long d, long m) { return 0; }
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
    int excessNanos(long d, long m) { return 0; }
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
    int excessNanos(long d, long m) { return 0; }
  };

  // Handy constants for conversion methods
  static final long C0 = 1L;
  static final long C1 = C0 * 1000L;
  static final long C2 = C1 * 1000L;
  static final long C3 = C2 * 1000L;
  static final long C4 = C3 * 60L;
  static final long C5 = C4 * 60L;
  static final long C6 = C5 * 24L;

  static final long MAX = Long.MAX_VALUE;
  
  static final long C6_C0 = C6 / C0;
  static final long C6_C1 = C6 / C1;
  static final long C6_C2 = C6 / C2;
  static final long C6_C3 = C6 / C3;
  static final long C6_C4 = C6 / C4;
  static final long C6_C5 = C6 / C5;

  static final long C5_C0 = C5 / C0;
  static final long C5_C1 = C5 / C1;
  static final long C5_C2 = C5 / C2;
  static final long C5_C3 = C5 / C3;
  static final long C5_C4 = C5 / C4;

  static final long C4_C0 = C4 / C0;
  static final long C4_C1 = C4 / C1;
  static final long C4_C2 = C4 / C2;
  static final long C4_C3 = C4 / C3;
  
  static final long C3_C0 = C3 / C0;
  static final long C3_C1 = C3 / C1;
  static final long C3_C2 = C3 / C2;
  
  static final long C2_C0 = C2 / C0;
  static final long C2_C1 = C2 / C1;
  
  static final long C1_C0 = C1 / C0;

  static final long MAX_C6_C0 = MAX / C6_C0;
  static final long MAX_C6_C1 = MAX / C6_C1;
  static final long MAX_C6_C2 = MAX / C6_C2;
  static final long MAX_C6_C3 = MAX / C6_C3;
  static final long MAX_C6_C4 = MAX / C6_C4;
  static final long MAX_C6_C5 = MAX / C6_C5;

  static final long MAX_C5_C0 = MAX / C5_C0;
  static final long MAX_C5_C1 = MAX / C5_C1;
  static final long MAX_C5_C2 = MAX / C5_C2;
  static final long MAX_C5_C3 = MAX / C5_C3;
  static final long MAX_C5_C4 = MAX / C5_C4;

  static final long MAX_C4_C0 = MAX / C4_C0;
  static final long MAX_C4_C1 = MAX / C4_C1;
  static final long MAX_C4_C2 = MAX / C4_C2;
  static final long MAX_C4_C3 = MAX / C4_C3;

  static final long MAX_C3_C0 = MAX / C3_C0;
  static final long MAX_C3_C1 = MAX / C3_C1;
  static final long MAX_C3_C2 = MAX / C3_C2;

  static final long MAX_C2_C0 = MAX / C2_C0;
  static final long MAX_C2_C1 = MAX / C2_C1;

  static final long MAX_C1_C0 = MAX / C1_C0;
  
  static long x(long d, long m, long over) {
    if (d >  over) return Long.MAX_VALUE;
    if (d < -over) return Long.MIN_VALUE;
    return d * m;
  }

  // exceptions below changed from AbstractMethodError for GWT

  public long convert(long sourceDuration, TimeUnit sourceUnit) {
    throw new AssertionError();
  }

  public long toNanos(long duration) {
    throw new AssertionError();
  }

  public long toMicros(long duration) {
    throw new AssertionError();
  }

  public long toMillis(long duration) {
    throw new AssertionError();
  }

  public long toSeconds(long duration) {
    throw new AssertionError();
  }

  public long toMinutes(long duration) {
    throw new AssertionError();
  }

  public long toHours(long duration) {
    throw new AssertionError();
  }

  public long toDays(long duration) {
    throw new AssertionError();
  }

  abstract int excessNanos(long d, long m);
}

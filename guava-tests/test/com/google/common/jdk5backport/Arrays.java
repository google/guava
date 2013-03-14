package com.google.common.jdk5backport;

/**
 * An Arrays alternative containing JDK 1.6 method equivalents used
 * to support JDK 1.5 with a few pass-through methods to reduce import
 * conflicts.
 */
public final class Arrays {
  public static byte[] copyOf(byte[] original, int newLength) {
    return copyOfRange(original, 0, newLength);
  }

  public static byte[] copyOfRange(byte[] original, int from, int to) {
    int newLength = to - from;
    if (newLength >= 0) {
      byte[] copy = new byte[newLength];
      System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
      return copy;
    }
    throw new IllegalArgumentException();
  }

  public static void fill(byte[] array, byte val) {
    java.util.Arrays.fill(array, val);
  }

  public static boolean equals(byte[] a, byte[] a2) {
     return java.util.Arrays.equals(a, a2);
  }
}

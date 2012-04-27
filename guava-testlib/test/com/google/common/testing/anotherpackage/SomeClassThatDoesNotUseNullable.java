// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.testing.anotherpackage;

/** Does not check null, but should not matter since it's in a different package. */
@SuppressWarnings("unused") // For use by NullPointerTester
public class SomeClassThatDoesNotUseNullable {

  void packagePrivateButDoesNotCheckNull(String s) {}

  protected void protectedButDoesNotCheckNull(String s) {}

  public void publicButDoesNotCheckNull(String s) {}

  public static void staticButDoesNotCheckNull(String s) {}
}

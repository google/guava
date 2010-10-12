/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.common.testing;

import static java.lang.String.format;

import com.google.common.annotations.Beta;


/**
 * A simple collection of assertions used in testing - rewritten to
 * avoid test framework dependencies.
 *
 * @author Christian Edward Gruber
 * @since 8
 */
@Beta
public class Assert {
  /**
   * Fail with an RuntimeException.
   *
   * @throws RuntimeException always
   */
  public static void fail() {
    throw new RuntimeException();
  }
	
  /**
   * Fail with an RuntimeException and a message.
   *
   * @throws RuntimeException always
   */
  public static void fail(String message) {
    throw new RuntimeException(message);
  }
  
  /**
   * Test the condition and throw a failure exception if false with 
   * a stock message.
   *
   * @throws RuntimeException
   */
  public static void assertTrue(boolean condition) {
    if (!condition) fail("Condition expected to be true but was false.");
  }
  
  /**
   * Test the condition and throw a failure exception if false with 
   * a stock message.
   *
   * @throws RuntimeException
   */
  public static void assertTrue(String message, boolean condition) {
    if (!condition) fail(message);
  }
  
  /**
   * Assert the equality of two objects
   */
  public static void assertEquals(Object expected, Object actual) {
    assertEquals(format("Expected '%s' but got '%s'", expected, actual), expected, actual);
  }
  
  /**
   * Assert the equality of two objects
   */
  public static void assertEquals(String message, Object o1, Object o2) {
    if (o1 == o2) {
      return;
    } else {
      assertTrue(message, o1.equals(o2));
    }
  }
}

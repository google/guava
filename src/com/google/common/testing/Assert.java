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
 * A simple collection of assertions used in testing - re-written to
 * avoid junit or testNG dependencies.
 *
 * @author cgruber@google.com (Christian Edward Gruber)
 * @since r08
 */
@Beta
public class Assert {

  /**
   * Fail with an AssertionFailedError();
   * @throws AssertionFailedError
   */
  public static void fail() {
    throw new AssertionFailedError();
  }
	
  /**
   * Fail with an AssertionFailedError and a message which may use String.format if any
   * subsequent parameters are provided.
   * 
   * @throws AssertionFailedError
   */
  public static void fail(String message) {
    throw new AssertionFailedError(message);
  }
  
  /**
   * Test the condition and throw a failure exception if false with 
   * a stock message.
   * @throws AssertionFailedError
   */
  public static void assertTrue(boolean condition) {
    if (!condition) fail("Condition exepected to be true but was false.");
  }
  
  /**
   * Test the condition and throw a failure exception with a 
   * provided message.
   * @throws AssertionFailedError
   */
  public static void assertTrue(String message, boolean condition) {
    if (!condition) fail(message);
  }
  
  /**
   * Test the negation of the condition and throw a failure exception with a 
   * canned message.
   * @throws AssertionFailedError
   */
  public static void assertFalse(boolean condition) {
    assertTrue("Condition expected to be false but was true.", !condition);
  }
  
  /**
   * Test the negation of the condition and throw a failure exception with a 
   * provided message.
   * @throws AssertionFailedError
   */
  public static void assertFalse(String message, boolean condition) {
    assertTrue(message, !condition);
  }
  
  /**
   * Assert the equality of two objects
   */
  public static void assertEquals(Object o1, Object o2) {
    assertEquals(format("Objects '%s' and '%s' are not equal", o1, o2), o1, o2);
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

  /**
   * Assert the equality of two objects
   */
  public static void assertNotEquals(Object o1, Object o2) {
    assertNotEquals(format("Objects '%s' and '%s' are equal but should not be", o1, o2), o1, o2);
  }
  
  /**
   * Assert the equality of two objects
   */
  public static void assertNotEquals(String message, Object o1, Object o2) {
    if (o1 == o2) {
      fail(format("Objects '%s' and '%s' should not be equal but are identical", o1, o2));
    } else {
      assertFalse(message, o1.equals(o2));
    }
  }

  /**
   * Assert the identity of the provided objects, else fail with a 
   * canned message.
   */
  public static void assertSame(Object o1, Object o2) {
    assertSame(format("Objects '%s' and '%s' are not the same.", o1, o2), o1, o2);
  }

  /**
   * Assert the identity of the provided objects, else fail with the 
   * provided message.
   */
  public static void assertSame(String message, Object o1, Object o2) {
    if (o1 != o2) {
      fail(message);
    }
  }
  
  /**
   * Assert the identity of the provided objects, else fail with a 
   * canned message.
   */
  public static void assertNotSame(Object o1, Object o2) {
    assertNotSame(format("Objects '%s' and '%s' are not the same.", o1, o2), o1, o2);
  }

  /**
   * Assert the identity of the provided objects, else fail with the 
   * provided message.
   */
  public static void assertNotSame(String message, Object o1, Object o2) {
    if (o1 == o2) {
      fail(message);
    }
  }

  /**
   * Assert that the provided reference is null else throw an AssertionFailedError
   * with a canned error message.
   */
  public static void assertNull(Object object) {
    assertNull(format("Expected null but found '%s'.", object), object);
  }

  /**
   * Assert that the provided reference is null else throw an AssertionFailedError
   * with a canned error message.
   */
  public static void assertNull(String message, Object object) {
    if (object != null) {
      fail(message);
    }
  }

  /**
   * Assert that the provided reference is null else throw an AssertionFailedError
   * with a canned error message.
   */
  public static void assertNotNull(Object object) {
    assertNotNull(format("Null returned where expected non null result."), object);
  }

  /**
   * Assert that the provided reference is null else throw an AssertionFailedError
   * with a canned error message.
   */
  public static void assertNotNull(String message, Object object) {
    if (object == null) {
      fail(message);
    }
  }
}
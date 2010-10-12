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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;

/**
 * Contains additional assertion methods not found in JUnit.
 *
 * @author kevinb@google.com (Kevin Bourillion)
 */
@Beta
@GwtCompatible
public final class GuavaAsserts {

  private GuavaAsserts() { }

  /**
   * Utility for testing equals() and hashCode() results at once.
   * Tests that lhs.equals(rhs) matches expectedResult, as well as
   * rhs.equals(lhs).  Also tests that hashCode() return values are
   * equal if expectedResult is true.  (hashCode() is not tested if
   * expectedResult is false, as unequal objects can have equal hashCodes.)
   *
   * @param lhs An Object for which equals() and hashCode() are to be tested.
   * @param rhs As lhs.
   * @param expectedResult True if the objects should compare equal,
   *   false if not.
   */
  public static void checkEqualsAndHashCodeMethods(
      String message, Object lhs, Object rhs, boolean expectedResult) {

    if ((lhs == null) && (rhs == null)) {
      Assert.assertTrue(
          "Your check is dubious...why would you expect null != null?",
          expectedResult);
      return;
    }

    if ((lhs == null) || (rhs == null)) {
      Assert.assertTrue(
          "Your check is dubious...why would you expect an object "
          + "to be equal to null?", !expectedResult);
    }

    if (lhs != null) {
      assertEqualsImpl(message, expectedResult, lhs.equals(rhs));
    }
    if (rhs != null) {
      assertEqualsImpl(message, expectedResult, rhs.equals(lhs));
    }

    if (expectedResult) {
      String hashMessage =
          "hashCode() values for equal objects should be the same";
      if (message != null) {
        hashMessage += ": " + message;
      }
      Assert.assertTrue(hashMessage, lhs.hashCode() == rhs.hashCode());
    }
  }

  /**
   * Replacement of {@link Assert#assertEquals} which provides the same error
   * message in GWT and java.
   */
  private static void assertEqualsImpl(
      String message, Object expected, Object actual) {
    if (!Objects.equal(expected, actual)) {
      failWithMessage(
          message, "expected:<" + expected + "> but was:<" + actual + ">");
    }
  }

  private static void failWithMessage(String userMessage, String ourMessage) {
    Assert.fail((userMessage == null)
        ? ourMessage
        : userMessage + ' ' + ourMessage);
  }

}

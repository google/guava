/*
 * Copyright (C) 2006 The Guava Authors
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

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.NullPointerTester;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Unit test for {@link Preconditions}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class PreconditionsTest extends TestCase {
  public void testCheckArgument_simple_success() {
    Preconditions.checkArgument(true);
  }

  public void testCheckArgument_simple_failure() {
    try {
      Preconditions.checkArgument(false);
      fail("no exception thrown");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckArgument_simpleMessage_success() {
    Preconditions.checkArgument(true, IGNORE_ME);
  }

  public void testCheckArgument_simpleMessage_failure() {
    try {
      Preconditions.checkArgument(false, new Message());
      fail("no exception thrown");
    } catch (IllegalArgumentException expected) {
      verifySimpleMessage(expected);
    }
  }

  public void testCheckArgument_nullMessage_failure() {
    try {
      Preconditions.checkArgument(false, null);
      fail("no exception thrown");
    } catch (IllegalArgumentException expected) {
      assertEquals("null", expected.getMessage());
    }
  }

  public void testCheckArgument_complexMessage_success() {
    Preconditions.checkArgument(true, "%s", IGNORE_ME);
  }

  public void testCheckArgument_complexMessage_failure() {
    try {
      Preconditions.checkArgument(false, FORMAT, 5);
      fail("no exception thrown");
    } catch (IllegalArgumentException expected) {
      verifyComplexMessage(expected);
    }
  }

  public void testCheckState_simple_success() {
    Preconditions.checkState(true);
  }

  public void testCheckState_simple_failure() {
    try {
      Preconditions.checkState(false);
      fail("no exception thrown");
    } catch (IllegalStateException expected) {
    }
  }

  public void testCheckState_simpleMessage_success() {
    Preconditions.checkState(true, IGNORE_ME);
  }

  public void testCheckState_simpleMessage_failure() {
    try {
      Preconditions.checkState(false, new Message());
      fail("no exception thrown");
    } catch (IllegalStateException expected) {
      verifySimpleMessage(expected);
    }
  }

  public void testCheckState_nullMessage_failure() {
    try {
      Preconditions.checkState(false, null);
      fail("no exception thrown");
    } catch (IllegalStateException expected) {
      assertEquals("null", expected.getMessage());
    }
  }

  public void testCheckState_complexMessage_success() {
    Preconditions.checkState(true, "%s", IGNORE_ME);
  }

  public void testCheckState_complexMessage_failure() {
    try {
      Preconditions.checkState(false, FORMAT, 5);
      fail("no exception thrown");
    } catch (IllegalStateException expected) {
      verifyComplexMessage(expected);
    }
  }

  private static final String NON_NULL_STRING = "foo";

  public void testCheckNotNull_simple_success() {
    String result = Preconditions.checkNotNull(NON_NULL_STRING);
    assertSame(NON_NULL_STRING, result);
  }

  public void testCheckNotNull_simple_failure() {
    try {
      Preconditions.checkNotNull(null);
      fail("no exception thrown");
    } catch (NullPointerException expected) {
    }
  }

  public void testCheckNotNull_simpleMessage_success() {
    String result = Preconditions.checkNotNull(NON_NULL_STRING, IGNORE_ME);
    assertSame(NON_NULL_STRING, result);
  }

  public void testCheckNotNull_simpleMessage_failure() {
    try {
      Preconditions.checkNotNull(null, new Message());
      fail("no exception thrown");
    } catch (NullPointerException expected) {
      verifySimpleMessage(expected);
    }
  }

  public void testCheckNotNull_complexMessage_success() {
    String result = Preconditions.checkNotNull(
        NON_NULL_STRING, "%s", IGNORE_ME);
    assertSame(NON_NULL_STRING, result);
  }

  public void testCheckNotNull_complexMessage_failure() {
    try {
      Preconditions.checkNotNull(null, FORMAT, 5);
      fail("no exception thrown");
    } catch (NullPointerException expected) {
      verifyComplexMessage(expected);
    }
  }

  public void testCheckElementIndex_ok() {
    assertEquals(0, Preconditions.checkElementIndex(0, 1));
    assertEquals(0, Preconditions.checkElementIndex(0, 2));
    assertEquals(1, Preconditions.checkElementIndex(1, 2));
  }

  public void testCheckElementIndex_badSize() {
    try {
      Preconditions.checkElementIndex(1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
      // don't care what the message text is, as this is an invalid usage of
      // the Preconditions class, unlike all the other exceptions it throws
    }
  }

  public void testCheckElementIndex_negative() {
    try {
      Preconditions.checkElementIndex(-1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertEquals("index (-1) must not be negative", expected.getMessage());
    }
  }

  public void testCheckElementIndex_tooHigh() {
    try {
      Preconditions.checkElementIndex(1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertEquals("index (1) must be less than size (1)",
          expected.getMessage());
    }
  }

  public void testCheckElementIndex_withDesc_negative() {
    try {
      Preconditions.checkElementIndex(-1, 1, "foo");
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertEquals("foo (-1) must not be negative", expected.getMessage());
    }
  }

  public void testCheckElementIndex_withDesc_tooHigh() {
    try {
      Preconditions.checkElementIndex(1, 1, "foo");
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertEquals("foo (1) must be less than size (1)",
          expected.getMessage());
    }
  }

  public void testCheckPositionIndex_ok() {
    assertEquals(0, Preconditions.checkPositionIndex(0, 0));
    assertEquals(0, Preconditions.checkPositionIndex(0, 1));
    assertEquals(1, Preconditions.checkPositionIndex(1, 1));
  }

  public void testCheckPositionIndex_badSize() {
    try {
      Preconditions.checkPositionIndex(1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
      // don't care what the message text is, as this is an invalid usage of
      // the Preconditions class, unlike all the other exceptions it throws
    }
  }

  public void testCheckPositionIndex_negative() {
    try {
      Preconditions.checkPositionIndex(-1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertEquals("index (-1) must not be negative", expected.getMessage());
    }
  }

  public void testCheckPositionIndex_tooHigh() {
    try {
      Preconditions.checkPositionIndex(2, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertEquals("index (2) must not be greater than size (1)",
          expected.getMessage());
    }
  }

  public void testCheckPositionIndex_withDesc_negative() {
    try {
      Preconditions.checkPositionIndex(-1, 1, "foo");
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertEquals("foo (-1) must not be negative", expected.getMessage());
    }
  }

  public void testCheckPositionIndex_withDesc_tooHigh() {
    try {
      Preconditions.checkPositionIndex(2, 1, "foo");
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertEquals("foo (2) must not be greater than size (1)",
          expected.getMessage());
    }
  }

  public void testCheckPositionIndexes_ok() {
    Preconditions.checkPositionIndexes(0, 0, 0);
    Preconditions.checkPositionIndexes(0, 0, 1);
    Preconditions.checkPositionIndexes(0, 1, 1);
    Preconditions.checkPositionIndexes(1, 1, 1);
  }

  public void testCheckPositionIndexes_badSize() {
    try {
      Preconditions.checkPositionIndexes(1, 1, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCheckPositionIndex_startNegative() {
    try {
      Preconditions.checkPositionIndexes(-1, 1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertEquals("start index (-1) must not be negative",
          expected.getMessage());
    }
  }

  public void testCheckPositionIndexes_endTooHigh() {
    try {
      Preconditions.checkPositionIndexes(0, 2, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertEquals("end index (2) must not be greater than size (1)",
          expected.getMessage());
    }
  }

  public void testCheckPositionIndexes_reversed() {
    try {
      Preconditions.checkPositionIndexes(1, 0, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
      assertEquals("end index (0) must not be less than start index (1)",
          expected.getMessage());
    }
  }

  public void testFormat() {
    assertEquals("%s", Preconditions.format("%s"));
    assertEquals("5", Preconditions.format("%s", 5));
    assertEquals("foo [5]", Preconditions.format("foo", 5));
    assertEquals("foo [5, 6, 7]", Preconditions.format("foo", 5, 6, 7));
    assertEquals("%s 1 2", Preconditions.format("%s %s %s", "%s", 1, 2));
    assertEquals(" [5, 6]", Preconditions.format("", 5, 6));
    assertEquals("123", Preconditions.format("%s%s%s", 1, 2, 3));
    assertEquals("1%s%s", Preconditions.format("%s%s%s", 1));
    assertEquals("5 + 6 = 11", Preconditions.format("%s + 6 = 11", 5));
    assertEquals("5 + 6 = 11", Preconditions.format("5 + %s = 11", 6));
    assertEquals("5 + 6 = 11", Preconditions.format("5 + 6 = %s", 11));
    assertEquals("5 + 6 = 11", Preconditions.format("%s + %s = %s", 5, 6, 11));
    assertEquals("null [null, null]",
        Preconditions.format("%s", null, null, null));
    assertEquals("null [5, 6]", Preconditions.format(null, 5, 6));
  }

  @GwtIncompatible("NullPointerTester")
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Preconditions.class);
  }

  private static final Object IGNORE_ME = new Object() {
    @Override public String toString() {
      throw new AssertionFailedError();
    }
  };

  private static class Message {
    boolean invoked;
    @Override public String toString() {
      assertFalse(invoked);
      invoked = true;
      return "A message";
    }
  }

  private static final String FORMAT = "I ate %s pies.";

  private static void verifySimpleMessage(Exception e) {
    assertEquals("A message", e.getMessage());
  }

  private static void verifyComplexMessage(Exception e) {
    assertEquals("I ate 5 pies.", e.getMessage());
  }
}

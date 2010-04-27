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

package com.google.common.base;

import junit.framework.TestCase;

/**
 * Unit test for {@link Strings}.
 *
 * @author Kevin Bourrillion
 */
public class StringsTest extends TestCase {
  public void testNullToEmpty() {
    assertEquals("", Strings.nullToEmpty(null));
    assertEquals("", Strings.nullToEmpty(""));
    assertEquals("a", Strings.nullToEmpty("a"));
  }

  public void testEmptyToNull() {
    assertNull(Strings.emptyToNull(null));
    assertNull(Strings.emptyToNull(""));
    assertEquals("a", Strings.emptyToNull("a"));
  }

  public void testIsNullOrEmpty() {
    assertTrue(Strings.isNullOrEmpty(null));
    assertTrue(Strings.isNullOrEmpty(""));
    assertFalse(Strings.isNullOrEmpty("a"));
  }

  public void testPadStart_noPadding() {
    assertSame("", Strings.padStart("", 0, '-'));
    assertSame("x", Strings.padStart("x", 0, '-'));
    assertSame("x", Strings.padStart("x", 1, '-'));
    assertSame("xx", Strings.padStart("xx", 0, '-'));
    assertSame("xx", Strings.padStart("xx", 2, '-'));
  }

  public void testPadStart_somePadding() {
    assertEquals("-", Strings.padStart("", 1, '-'));
    assertEquals("--", Strings.padStart("", 2, '-'));
    assertEquals("-x", Strings.padStart("x", 2, '-'));
    assertEquals("--x", Strings.padStart("x", 3, '-'));
    assertEquals("-xx", Strings.padStart("xx", 3, '-'));
  }

  public void testPadStart_negativeMinLength() {
    assertSame("x", Strings.padStart("x", -1, '-'));
  }

  // TODO: could remove if we got NPT working in GWT somehow
  public void testPadStart_null() {
    try {
      Strings.padStart(null, 5, '0');
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testPadEnd_noPadding() {
    assertSame("", Strings.padEnd("", 0, '-'));
    assertSame("x", Strings.padEnd("x", 0, '-'));
    assertSame("x", Strings.padEnd("x", 1, '-'));
    assertSame("xx", Strings.padEnd("xx", 0, '-'));
    assertSame("xx", Strings.padEnd("xx", 2, '-'));
  }

  public void testPadEnd_somePadding() {
    assertEquals("-", Strings.padEnd("", 1, '-'));
    assertEquals("--", Strings.padEnd("", 2, '-'));
    assertEquals("x-", Strings.padEnd("x", 2, '-'));
    assertEquals("x--", Strings.padEnd("x", 3, '-'));
    assertEquals("xx-", Strings.padEnd("xx", 3, '-'));
  }

  public void testPadEnd_negativeMinLength() {
    assertSame("x", Strings.padEnd("x", -1, '-'));
  }

  // TODO: could remove if we got NPT working in GWT somehow
  public void testPadEnd_null() {
    try {
      Strings.padEnd(null, 5, '0');
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testRepeat() {
    String input = "20";
    assertEquals("", Strings.repeat(input, 0));
    assertEquals("20", Strings.repeat(input, 1));
    assertEquals("202020", Strings.repeat(input, 3));

    assertEquals("", Strings.repeat("", 4));

    try {
      Strings.repeat("x", -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  // TODO: could remove if we got NPT working in GWT somehow
  public void testRepeat_null() {
    try {
      Strings.repeat(null, 5);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  // TODO: salvage the nullpointer testing in a gwt-safe way
  // public void testNullPointers() throws Exception {
  //   NullPointerTester tester = new NullPointerTester();
  //   tester.testAllPublicStaticMethods(Strings.class);
  // }
}

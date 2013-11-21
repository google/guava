/*
 * Copyright (C) 2010 The Guava Authors
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

import junit.framework.TestCase;

/**
 * Unit test for {@link Ascii}.
 *
 * @author Craig Berry
 */
@GwtCompatible
public class AsciiTest extends TestCase {

  /**
   * The Unicode points {@code 00c1} and {@code 00e1} are the upper- and
   * lowercase forms of A-with-acute-accent, {@code Á} and {@code á}.
   */
  private static final String IGNORED =
      "`10-=~!@#$%^&*()_+[]\\{}|;':\",./<>?'\u00c1\u00e1\n";
  private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
  private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  public void testToLowerCase() {
    assertEquals(LOWER, Ascii.toLowerCase(UPPER));
    assertSame(LOWER, Ascii.toLowerCase(LOWER));
    assertEquals(IGNORED, Ascii.toLowerCase(IGNORED));
    assertEquals("foobar", Ascii.toLowerCase("fOobaR"));
  }

  public void testToUpperCase() {
    assertEquals(UPPER, Ascii.toUpperCase(LOWER));
    assertSame(UPPER, Ascii.toUpperCase(UPPER));
    assertEquals(IGNORED, Ascii.toUpperCase(IGNORED));
    assertEquals("FOOBAR", Ascii.toUpperCase("FoOBAr"));
  }

  public void testCharsIgnored() {
    for (char c : IGNORED.toCharArray()) {
      String str = String.valueOf(c);
      assertTrue(str, c == Ascii.toLowerCase(c));
      assertTrue(str, c == Ascii.toUpperCase(c));
      assertFalse(str, Ascii.isLowerCase(c));
      assertFalse(str, Ascii.isUpperCase(c));
    }
  }

  public void testCharsLower() {
    for (char c : LOWER.toCharArray()) {
      String str = String.valueOf(c);
      assertTrue(str, c == Ascii.toLowerCase(c));
      assertFalse(str, c == Ascii.toUpperCase(c));
      assertTrue(str, Ascii.isLowerCase(c));
      assertFalse(str, Ascii.isUpperCase(c));
    }
  }

  public void testCharsUpper() {
    for (char c : UPPER.toCharArray()) {
      String str = String.valueOf(c);
      assertFalse(str, c == Ascii.toLowerCase(c));
      assertTrue(str, c == Ascii.toUpperCase(c));
      assertFalse(str, Ascii.isLowerCase(c));
      assertTrue(str, Ascii.isUpperCase(c));
    }
  }

  public void testTruncate() {
    assertEquals("foobar", Ascii.truncate("foobar", 10));
    assertEquals("fo...", Ascii.truncate("foobar", 5));
    assertEquals("foobar", Ascii.truncate("foobar", 6));
    assertEquals("", Ascii.truncate("", 6));
  }

  public void testTruncateWithCustomTruncationIndicator() {
    assertEquals("foobar", Ascii.truncate("foobar", 10, "…"));
    assertEquals("foo…", Ascii.truncate("foobar", 4, "…"));
    assertEquals("fo--", Ascii.truncate("foobar", 4, "--"));
    assertEquals("foobar", Ascii.truncate("foobar", 6, "…"));
    assertEquals("foob…", Ascii.truncate("foobar", 5, "…"));
    assertEquals("foo", Ascii.truncate("foobar", 3, ""));
    assertEquals("", Ascii.truncate("", 5, ""));
    assertEquals("", Ascii.truncate("", 5, "..."));
    assertEquals("", Ascii.truncate("", 0, ""));
  }

  public void testTruncateIllegalArguments() {
    try {
      Ascii.truncate("foobar", 2);
      fail();
    } catch (IllegalArgumentException expected) {}

    try {
      Ascii.truncate("foobar", 8, "1234567890");
      fail();
    } catch (IllegalArgumentException expected) {}

    try {
      Ascii.truncate("foobar", -1);
      fail();
    } catch (IllegalArgumentException expected) {}

    try {
      Ascii.truncate("foobar", -1, "");
      fail();
    } catch (IllegalArgumentException expected) {}
  }
}

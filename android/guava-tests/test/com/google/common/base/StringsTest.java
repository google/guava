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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.NullPointerTester;
import junit.framework.TestCase;

/**
 * Unit test for {@link Strings}.
 *
 * @author Kevin Bourrillion
 */
@ElementTypesAreNonnullByDefault
@GwtCompatible(emulated = true)
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
    assertEquals("2020", Strings.repeat(input, 2));
    assertEquals("202020", Strings.repeat(input, 3));

    assertEquals("", Strings.repeat("", 4));

    for (int i = 0; i < 100; ++i) {
      assertEquals(2 * i, Strings.repeat(input, i).length());
    }

    try {
      Strings.repeat("x", -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      // Massive string
      Strings.repeat("12345678", (1 << 30) + 3);
      fail();
    } catch (ArrayIndexOutOfBoundsException expected) {
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

  public void testCommonPrefix() {
    assertEquals("", Strings.commonPrefix("", ""));
    assertEquals("", Strings.commonPrefix("abc", ""));
    assertEquals("", Strings.commonPrefix("", "abc"));
    assertEquals("", Strings.commonPrefix("abcde", "xyz"));
    assertEquals("", Strings.commonPrefix("xyz", "abcde"));
    assertEquals("", Strings.commonPrefix("xyz", "abcxyz"));
    assertEquals("a", Strings.commonPrefix("abc", "aaaaa"));
    assertEquals("aa", Strings.commonPrefix("aa", "aaaaa"));
    assertEquals("abc", Strings.commonPrefix(new StringBuffer("abcdef"), "abcxyz"));

    // Identical valid surrogate pairs.
    assertEquals(
        "abc\uD8AB\uDCAB", Strings.commonPrefix("abc\uD8AB\uDCABdef", "abc\uD8AB\uDCABxyz"));
    // Differing valid surrogate pairs.
    assertEquals("abc", Strings.commonPrefix("abc\uD8AB\uDCABdef", "abc\uD8AB\uDCACxyz"));
    // One invalid pair.
    assertEquals("abc", Strings.commonPrefix("abc\uD8AB\uDCABdef", "abc\uD8AB\uD8ABxyz"));
    // Two identical invalid pairs.
    assertEquals(
        "abc\uD8AB\uD8AC", Strings.commonPrefix("abc\uD8AB\uD8ACdef", "abc\uD8AB\uD8ACxyz"));
    // Two differing invalid pairs.
    assertEquals("abc\uD8AB", Strings.commonPrefix("abc\uD8AB\uD8ABdef", "abc\uD8AB\uD8ACxyz"));
    // One orphan high surrogate.
    assertEquals("", Strings.commonPrefix("\uD8AB\uDCAB", "\uD8AB"));
    // Two orphan high surrogates.
    assertEquals("\uD8AB", Strings.commonPrefix("\uD8AB", "\uD8AB"));
  }

  public void testCommonSuffix() {
    assertEquals("", Strings.commonSuffix("", ""));
    assertEquals("", Strings.commonSuffix("abc", ""));
    assertEquals("", Strings.commonSuffix("", "abc"));
    assertEquals("", Strings.commonSuffix("abcde", "xyz"));
    assertEquals("", Strings.commonSuffix("xyz", "abcde"));
    assertEquals("", Strings.commonSuffix("xyz", "xyzabc"));
    assertEquals("c", Strings.commonSuffix("abc", "ccccc"));
    assertEquals("aa", Strings.commonSuffix("aa", "aaaaa"));
    assertEquals("abc", Strings.commonSuffix(new StringBuffer("xyzabc"), "xxxabc"));

    // Identical valid surrogate pairs.
    assertEquals(
        "\uD8AB\uDCABdef", Strings.commonSuffix("abc\uD8AB\uDCABdef", "xyz\uD8AB\uDCABdef"));
    // Differing valid surrogate pairs.
    assertEquals("def", Strings.commonSuffix("abc\uD8AB\uDCABdef", "abc\uD8AC\uDCABdef"));
    // One invalid pair.
    assertEquals("def", Strings.commonSuffix("abc\uD8AB\uDCABdef", "xyz\uDCAB\uDCABdef"));
    // Two identical invalid pairs.
    assertEquals(
        "\uD8AB\uD8ABdef", Strings.commonSuffix("abc\uD8AB\uD8ABdef", "xyz\uD8AB\uD8ABdef"));
    // Two differing invalid pairs.
    assertEquals("\uDCABdef", Strings.commonSuffix("abc\uDCAB\uDCABdef", "abc\uDCAC\uDCABdef"));
    // One orphan low surrogate.
    assertEquals("", Strings.commonSuffix("x\uD8AB\uDCAB", "\uDCAB"));
    // Two orphan low surrogates.
    assertEquals("\uDCAB", Strings.commonSuffix("\uDCAB", "\uDCAB"));
  }

  public void testValidSurrogatePairAt() {
    assertTrue(Strings.validSurrogatePairAt("\uD8AB\uDCAB", 0));
    assertTrue(Strings.validSurrogatePairAt("abc\uD8AB\uDCAB", 3));
    assertTrue(Strings.validSurrogatePairAt("abc\uD8AB\uDCABxyz", 3));
    assertFalse(Strings.validSurrogatePairAt("\uD8AB\uD8AB", 0));
    assertFalse(Strings.validSurrogatePairAt("\uDCAB\uDCAB", 0));
    assertFalse(Strings.validSurrogatePairAt("\uD8AB\uDCAB", -1));
    assertFalse(Strings.validSurrogatePairAt("\uD8AB\uDCAB", 1));
    assertFalse(Strings.validSurrogatePairAt("\uD8AB\uDCAB", -2));
    assertFalse(Strings.validSurrogatePairAt("\uD8AB\uDCAB", 2));
    assertFalse(Strings.validSurrogatePairAt("x\uDCAB", 0));
    assertFalse(Strings.validSurrogatePairAt("\uD8ABx", 0));
  }

  @SuppressWarnings("LenientFormatStringValidation") // Intentional for testing.
  public void testLenientFormat() {
    assertEquals("%s", Strings.lenientFormat("%s"));
    assertEquals("5", Strings.lenientFormat("%s", 5));
    assertEquals("foo [5]", Strings.lenientFormat("foo", 5));
    assertEquals("foo [5, 6, 7]", Strings.lenientFormat("foo", 5, 6, 7));
    assertEquals("%s 1 2", Strings.lenientFormat("%s %s %s", "%s", 1, 2));
    assertEquals(" [5, 6]", Strings.lenientFormat("", 5, 6));
    assertEquals("123", Strings.lenientFormat("%s%s%s", 1, 2, 3));
    assertEquals("1%s%s", Strings.lenientFormat("%s%s%s", 1));
    assertEquals("5 + 6 = 11", Strings.lenientFormat("%s + 6 = 11", 5));
    assertEquals("5 + 6 = 11", Strings.lenientFormat("5 + %s = 11", 6));
    assertEquals("5 + 6 = 11", Strings.lenientFormat("5 + 6 = %s", 11));
    assertEquals("5 + 6 = 11", Strings.lenientFormat("%s + %s = %s", 5, 6, 11));
    assertEquals("null [null, null]", Strings.lenientFormat("%s", null, null, null));
    assertEquals("null [5, 6]", Strings.lenientFormat(null, 5, 6));
    assertEquals("null", Strings.lenientFormat("%s", (Object) null));
    assertEquals("(Object[])null", Strings.lenientFormat("%s", (Object[]) null));
  }

  @J2ktIncompatible
  @GwtIncompatible // GWT reflection includes less data
  public void testLenientFormat_badArgumentToString() {
    assertThat(Strings.lenientFormat("boiler %s plate", new ThrowsOnToString()))
        .matches(
            "boiler <com\\.google\\.common\\.base\\.StringsTest\\$ThrowsOnToString@[0-9a-f]+ "
                + "threw java\\.lang\\.UnsupportedOperationException> plate");
  }

  public void testLenientFormat_badArgumentToString_gwtFriendly() {
    assertThat(Strings.lenientFormat("boiler %s plate", new ThrowsOnToString()))
        .matches("boiler <.*> plate");
  }

  private static class ThrowsOnToString {
    @Override
    public String toString() {
      throw new UnsupportedOperationException();
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Strings.class);
  }
}

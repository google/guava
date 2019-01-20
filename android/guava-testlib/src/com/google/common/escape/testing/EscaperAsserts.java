/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.escape.testing;

import static com.google.common.escape.Escapers.computeReplacement;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.escape.CharEscaper;
import com.google.common.escape.Escaper;
import com.google.common.escape.UnicodeEscaper;
import java.io.IOException;
import junit.framework.Assert;

/**
 * Extra assert methods for testing Escaper implementations.
 *
 * @author David Beaumont
 * @since 15.0
 */
@Beta
@GwtCompatible
public final class EscaperAsserts {
  private EscaperAsserts() {}

  /**
   * Asserts that an escaper behaves correctly with respect to null inputs.
   *
   * @param escaper the non-null escaper to test
   */
  public static void assertBasic(Escaper escaper) throws IOException {
    // Escapers operate on characters: no characters, no escaping.
    Assert.assertEquals("", escaper.escape(""));
    // Assert that escapers throw null pointer exceptions.
    try {
      escaper.escape((String) null);
      Assert.fail("exception not thrown when escaping a null string");
    } catch (NullPointerException e) {
      // pass
    }
  }

  /**
   * Asserts that an escaper escapes the given character into the expected string.
   *
   * @param escaper the non-null escaper to test
   * @param expected the expected output string
   * @param c the character to escape
   */
  public static void assertEscaping(CharEscaper escaper, String expected, char c) {

    String escaped = computeReplacement(escaper, c);
    Assert.assertNotNull(escaped);
    Assert.assertEquals(expected, escaped);
  }

  /**
   * Asserts that a Unicode escaper escapes the given code point into the expected string.
   *
   * @param escaper the non-null escaper to test
   * @param expected the expected output string
   * @param cp the Unicode code point to escape
   */
  public static void assertEscaping(UnicodeEscaper escaper, String expected, int cp) {

    String escaped = computeReplacement(escaper, cp);
    Assert.assertNotNull(escaped);
    Assert.assertEquals(expected, escaped);
  }

  /**
   * Asserts that an escaper does not escape the given character.
   *
   * @param escaper the non-null escaper to test
   * @param c the character to test
   */
  public static void assertUnescaped(CharEscaper escaper, char c) {
    Assert.assertNull(computeReplacement(escaper, c));
  }

  /**
   * Asserts that a Unicode escaper does not escape the given character.
   *
   * @param escaper the non-null escaper to test
   * @param cp the Unicode code point to test
   */
  public static void assertUnescaped(UnicodeEscaper escaper, int cp) {
    Assert.assertNull(computeReplacement(escaper, cp));
  }

  /**
   * Asserts that a Unicode escaper escapes the given hi/lo surrogate pair into the expected string.
   *
   * @param escaper the non-null escaper to test
   * @param expected the expected output string
   * @param hi the high surrogate pair character
   * @param lo the low surrogate pair character
   */
  public static void assertUnicodeEscaping(
      UnicodeEscaper escaper, String expected, char hi, char lo) {

    int cp = Character.toCodePoint(hi, lo);
    String escaped = computeReplacement(escaper, cp);
    Assert.assertNotNull(escaped);
    Assert.assertEquals(expected, escaped);
  }
}

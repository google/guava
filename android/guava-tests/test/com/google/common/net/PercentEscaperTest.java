/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.net;

import static com.google.common.escape.testing.EscaperAsserts.assertEscaping;
import static com.google.common.escape.testing.EscaperAsserts.assertUnescaped;
import static com.google.common.escape.testing.EscaperAsserts.assertUnicodeEscaping;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import com.google.common.escape.UnicodeEscaper;
import junit.framework.TestCase;

/**
 * Tests for {@link PercentEscaper}.
 *
 * @author David Beaumont
 */
@GwtCompatible
public class PercentEscaperTest extends TestCase {

  /** Tests that the simple escaper treats 0-9, a-z and A-Z as safe */
  public void testSimpleEscaper() {
    UnicodeEscaper e = new PercentEscaper("", false);
    for (char c = 0; c < 128; c++) {
      if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
        assertUnescaped(e, c);
      } else {
        assertEscaping(e, escapeAscii(c), c);
      }
    }

    // Testing mutlibyte escape sequences
    assertEscaping(e, "%00", '\u0000'); // nul
    assertEscaping(e, "%7F", '\u007f'); // del
    assertEscaping(e, "%C2%80", '\u0080'); // xx-00010,x-000000
    assertEscaping(e, "%DF%BF", '\u07ff'); // xx-11111,x-111111
    assertEscaping(e, "%E0%A0%80", '\u0800'); // xxx-0000,x-100000,x-00,0000
    assertEscaping(e, "%EF%BF%BF", '\uffff'); // xxx-1111,x-111111,x-11,1111
    assertUnicodeEscaping(e, "%F0%90%80%80", '\uD800', '\uDC00');
    assertUnicodeEscaping(e, "%F4%8F%BF%BF", '\uDBFF', '\uDFFF');

    // simple string tests
    assertEquals("", e.escape(""));
    assertEquals("safestring", e.escape("safestring"));
    assertEquals("embedded%00null", e.escape("embedded\0null"));
    assertEquals("max%EF%BF%BFchar", e.escape("max\uffffchar"));
  }

  /** Tests the various ways that the space character can be handled */
  public void testPlusForSpace() {
    UnicodeEscaper basicEscaper = new PercentEscaper("", false);
    UnicodeEscaper plusForSpaceEscaper = new PercentEscaper("", true);
    UnicodeEscaper spaceEscaper = new PercentEscaper(" ", false);

    assertEquals("string%20with%20spaces", basicEscaper.escape("string with spaces"));
    assertEquals("string+with+spaces", plusForSpaceEscaper.escape("string with spaces"));
    assertEquals("string with spaces", spaceEscaper.escape("string with spaces"));
  }

  /** Tests that if we add extra 'safe' characters they remain unescaped */
  public void testCustomEscaper() {
    UnicodeEscaper e = new PercentEscaper("+*/-", false);
    for (char c = 0; c < 128; c++) {
      if ((c >= '0' && c <= '9')
          || (c >= 'a' && c <= 'z')
          || (c >= 'A' && c <= 'Z')
          || "+*/-".indexOf(c) >= 0) {
        assertUnescaped(e, c);
      } else {
        assertEscaping(e, escapeAscii(c), c);
      }
    }
  }

  /** Tests that if specify '%' as safe the result is an idempotent escaper. */
  public void testCustomEscaper_withpercent() {
    UnicodeEscaper e = new PercentEscaper("%", false);
    assertEquals("foo%7Cbar", e.escape("foo|bar"));
    assertEquals("foo%7Cbar", e.escape("foo%7Cbar")); // idempotent
  }

  /** Test that giving a null 'safeChars' string causes a {@link NullPointerException}. */
  public void testBadArguments_null() {
    try {
      new PercentEscaper(null, false);
      fail("Expected null pointer exception for null parameter");
    } catch (NullPointerException expected) {
      // pass
    }
  }

  /**
   * Tests that specifying any alphanumeric characters as 'safe' causes an {@link
   * IllegalArgumentException}.
   */
  public void testBadArguments_badchars() {
    String msg =
        "Alphanumeric characters are always 'safe' " + "and should not be explicitly specified";
    try {
      new PercentEscaper("-+#abc.!", false);
      fail(msg);
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo(msg);
    }
  }

  /**
   * Tests that if space is a safe character you cannot also specify 'plusForSpace' (throws {@link
   * IllegalArgumentException}).
   */
  public void testBadArguments_plusforspace() {
    try {
      new PercentEscaper(" ", false);
    } catch (IllegalArgumentException e) {
      fail("Space can be a 'safe' character if plusForSpace is false");
    }
    String msg = "plusForSpace cannot be specified when space is a 'safe' character";
    try {
      new PercentEscaper(" ", true);
      fail(msg);
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessageThat().isEqualTo(msg);
    }
  }

  /** Helper to manually escape a 7-bit ascii character */
  private String escapeAscii(char c) {
    Preconditions.checkArgument(c < 128);
    String hex = "0123456789ABCDEF";
    return "%" + hex.charAt((c >> 4) & 0xf) + hex.charAt(c & 0xf);
  }
}

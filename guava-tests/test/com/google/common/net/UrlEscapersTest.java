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

package com.google.common.net;

import static com.google.common.escape.testing.EscaperAsserts.assertEscaping;
import static com.google.common.escape.testing.EscaperAsserts.assertUnescaped;
import static com.google.common.escape.testing.EscaperAsserts.assertUnicodeEscaping;
import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static com.google.common.net.UrlEscapers.urlFragmentEscaper;
import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;

import com.google.common.annotations.GwtCompatible;
import com.google.common.escape.UnicodeEscaper;
import junit.framework.TestCase;

/**
 * Tests for the {@link UrlEscapers} class.
 *
 * @author David Beaumont
 */
@GwtCompatible
public class UrlEscapersTest extends TestCase {
  /**
   * Helper to assert common expected behaviour of uri escapers. You should call
   * assertBasicUrlEscaper() unless the escaper explicitly does not escape '%'.
   */
  static void assertBasicUrlEscaperExceptPercent(UnicodeEscaper e) {
    // URL escapers should throw null pointer exceptions for null input
    try {
      e.escape((String) null);
      fail("Escaping null string should throw exception");
    } catch (NullPointerException x) {
      // pass
    }

    // All URL escapers should leave 0-9, A-Z, a-z unescaped
    assertUnescaped(e, 'a');
    assertUnescaped(e, 'z');
    assertUnescaped(e, 'A');
    assertUnescaped(e, 'Z');
    assertUnescaped(e, '0');
    assertUnescaped(e, '9');

    // Unreserved characters used in java.net.URLEncoder
    assertUnescaped(e, '-');
    assertUnescaped(e, '_');
    assertUnescaped(e, '.');
    assertUnescaped(e, '*');

    assertEscaping(e, "%00", '\u0000'); // nul
    assertEscaping(e, "%7F", '\u007f'); // del
    assertEscaping(e, "%C2%80", '\u0080'); // xx-00010,x-000000
    assertEscaping(e, "%DF%BF", '\u07ff'); // xx-11111,x-111111
    assertEscaping(e, "%E0%A0%80", '\u0800'); // xxx-0000,x-100000,x-00,0000
    assertEscaping(e, "%EF%BF%BF", '\uffff'); // xxx-1111,x-111111,x-11,1111
    assertUnicodeEscaping(e, "%F0%90%80%80", '\uD800', '\uDC00');
    assertUnicodeEscaping(e, "%F4%8F%BF%BF", '\uDBFF', '\uDFFF');

    assertEquals("", e.escape(""));
    assertEquals("safestring", e.escape("safestring"));
    assertEquals("embedded%00null", e.escape("embedded\0null"));
    assertEquals("max%EF%BF%BFchar", e.escape("max\uffffchar"));
  }

  // Helper to assert common expected behaviour of uri escapers.
  static void assertBasicUrlEscaper(UnicodeEscaper e) {
    assertBasicUrlEscaperExceptPercent(e);
    // The escape character must always be escaped
    assertEscaping(e, "%25", '%');
  }

  public void testUrlFormParameterEscaper() {
    UnicodeEscaper e = (UnicodeEscaper) urlFormParameterEscaper();
    // Verify that these are the same escaper (as documented)
    assertSame(e, urlFormParameterEscaper());
    assertBasicUrlEscaper(e);

    /*
     * Specified as safe by RFC 2396 but not by java.net.URLEncoder. These tests will start failing
     * when the escaper is made compliant with RFC 2396, but that's a good thing (just change them
     * to assertUnescaped).
     */
    assertEscaping(e, "%21", '!');
    assertEscaping(e, "%28", '(');
    assertEscaping(e, "%29", ')');
    assertEscaping(e, "%7E", '~');
    assertEscaping(e, "%27", '\'');

    // Plus for spaces
    assertEscaping(e, "+", ' ');
    assertEscaping(e, "%2B", '+');

    assertEquals("safe+with+spaces", e.escape("safe with spaces"));
    assertEquals("foo%40bar.com", e.escape("foo@bar.com"));
  }

  public void testUrlPathSegmentEscaper() {
    UnicodeEscaper e = (UnicodeEscaper) urlPathSegmentEscaper();
    assertPathEscaper(e);
    assertUnescaped(e, '+');
  }

  static void assertPathEscaper(UnicodeEscaper e) {
    assertBasicUrlEscaper(e);

    assertUnescaped(e, '!');
    assertUnescaped(e, '\'');
    assertUnescaped(e, '(');
    assertUnescaped(e, ')');
    assertUnescaped(e, '~');
    assertUnescaped(e, ':');
    assertUnescaped(e, '@');

    // Don't use plus for spaces
    assertEscaping(e, "%20", ' ');

    assertEquals("safe%20with%20spaces", e.escape("safe with spaces"));
    assertEquals("foo@bar.com", e.escape("foo@bar.com"));
  }

  public void testUrlFragmentEscaper() {
    UnicodeEscaper e = (UnicodeEscaper) urlFragmentEscaper();
    assertUnescaped(e, '+');
    assertUnescaped(e, '/');
    assertUnescaped(e, '?');

    assertPathEscaper(e);
  }
}

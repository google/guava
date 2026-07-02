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

package com.google.common.xml;

import static com.google.common.escape.testing.EscaperAsserts.assertEscaping;
import static com.google.common.escape.testing.EscaperAsserts.assertUnescaped;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.annotations.GwtCompatible;
import com.google.common.escape.CharEscaper;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for the {@link XmlEscapers} class.
 *
 * @author Alex Matevossian
 * @author David Beaumont
 */
@GwtCompatible
@NullUnmarked
public class XmlEscapersTest extends TestCase {

  public void testXmlContentEscaper() {
    CharEscaper xmlContentEscaper = (CharEscaper) XmlEscapers.xmlContentEscaper();
    assertBasicXmlEscaper(xmlContentEscaper, false, false);
    // Test quotes are not escaped.
    assertThat(xmlContentEscaper.escape("\"test\"")).isEqualTo("\"test\"");
    assertThat(xmlContentEscaper.escape("'test'")).isEqualTo("'test'");
  }

  public void testXmlAttributeEscaper() {
    CharEscaper xmlAttributeEscaper = (CharEscaper) XmlEscapers.xmlAttributeEscaper();
    assertBasicXmlEscaper(xmlAttributeEscaper, true, true);
    // Test quotes are escaped.
    assertThat(xmlAttributeEscaper.escape("\"test\"")).isEqualTo("&quot;test&quot;");
    assertThat(xmlAttributeEscaper.escape("'test'")).isEqualTo("&apos;test&apos;");
    // Test all escapes
    assertThat(xmlAttributeEscaper.escape("a\"b<c>d&e\"f'"))
        .isEqualTo("a&quot;b&lt;c&gt;d&amp;e&quot;f&apos;");
    // Test '\t', '\n' and '\r' are escaped.
    assertThat(xmlAttributeEscaper.escape("a\tb\nc\rd")).isEqualTo("a&#x9;b&#xA;c&#xD;d");
  }

  // Helper to assert common properties of xml escapers.
  static void assertBasicXmlEscaper(
      CharEscaper xmlEscaper, boolean shouldEscapeQuotes, boolean shouldEscapeWhitespaceChars) {
    // Simple examples (smoke tests)
    assertThat(xmlEscaper.escape("xxx")).isEqualTo("xxx");
    assertThat(xmlEscaper.escape("test & test & test")).isEqualTo("test &amp; test &amp; test");
    assertThat(xmlEscaper.escape("test << 1")).isEqualTo("test &lt;&lt; 1");
    assertThat(xmlEscaper.escape("test >> 1")).isEqualTo("test &gt;&gt; 1");
    assertThat(xmlEscaper.escape("<tab>")).isEqualTo("&lt;tab&gt;");

    // Test all non-escaped ASCII characters.
    String s =
        "!@#$%^*()_+=-/?\\|]}[{,.;:"
            + "abcdefghijklmnopqrstuvwxyz"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "1234567890";
    assertThat(xmlEscaper.escape(s)).isEqualTo(s);

    // Test ASCII control characters.
    for (char ch = 0; ch < 0x20; ch++) {
      if (ch == '\t' || ch == '\n' || ch == '\r') {
        // Only these whitespace chars are permitted in XML,
        if (shouldEscapeWhitespaceChars) {
          assertEscaping(xmlEscaper, "&#x" + Integer.toHexString(ch).toUpperCase() + ";", ch);
        } else {
          assertUnescaped(xmlEscaper, ch);
        }
      } else {
        // and everything else is replaced with FFFD.
        assertEscaping(xmlEscaper, "\uFFFD", ch);
      }
    }

    // Test _all_ allowed characters (including surrogate values).
    for (char ch = 0x20; ch <= 0xFFFD; ch++) {
      // There are a small number of cases to consider, so just do it manually.
      if (ch == '&') {
        assertEscaping(xmlEscaper, "&amp;", ch);
      } else if (ch == '<') {
        assertEscaping(xmlEscaper, "&lt;", ch);
      } else if (ch == '>') {
        assertEscaping(xmlEscaper, "&gt;", ch);
      } else if (shouldEscapeQuotes && ch == '\'') {
        assertEscaping(xmlEscaper, "&apos;", ch);
      } else if (shouldEscapeQuotes && ch == '"') {
        assertEscaping(xmlEscaper, "&quot;", ch);
      } else {
        String input = String.valueOf(ch);
        String escaped = xmlEscaper.escape(input);
        assertWithMessage("char 0x" + Integer.toString(ch, 16) + " should not be escaped")
            .that(escaped)
            .isEqualTo(input);
      }
    }

    // Test that 0xFFFE and 0xFFFF are replaced with 0xFFFD
    assertEscaping(xmlEscaper, "\uFFFD", '\uFFFE');
    assertEscaping(xmlEscaper, "\uFFFD", '\uFFFF');

    assertWithMessage("0xFFFE is forbidden and should be replaced during escaping")
        .that(xmlEscaper.escape("[\ufffe]"))
        .isEqualTo("[\uFFFD]");
    assertWithMessage("0xFFFF is forbidden and should be replaced during escaping")
        .that(xmlEscaper.escape("[\uffff]"))
        .isEqualTo("[\uFFFD]");
  }
}

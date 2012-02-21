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

package com.google.common.html;

import static com.google.common.escape.testing.EscaperAsserts.assertUnescaped;
import static com.google.common.escape.testing.EscaperAsserts.assertUnicodeEscaping;
import static com.google.common.escape.testing.EscaperAsserts.assertEscaping;

import com.google.common.annotations.GwtCompatible;
import com.google.common.escape.UnicodeEscaper;

import junit.framework.TestCase;

/**
 * Tests for the {@link HtmlEscapers} class.
 *
 * @author David Beaumont
 */
@GwtCompatible
public class HtmlEscapersTest extends TestCase {

  public void testHtmlEscaper() throws Exception {
    UnicodeEscaper e = HtmlEscapers.htmlEscaper();

    // Test a sampling of 'safe' chars.
    assertUnescaped(e, ' ');
    assertUnescaped(e, 'a');
    assertUnescaped(e, 'A');
    assertUnescaped(e, '1');
    assertUnescaped(e, '~');

    // Test basic special chars.
    assertEscaping(e, "&quot;", '"');
    assertEscaping(e, "&#39;",  '\'');
    assertEscaping(e, "&amp;",  '&');
    assertEscaping(e, "&lt;",   '<');
    assertEscaping(e, "&gt;",   '>');

    // Extended special chars (a sampling).
    assertEscaping(e, "&nbsp;",  '\u00A0');
    assertEscaping(e, "&pound;", '\u00A3');
    assertEscaping(e, "&raquo;", '\u00BB');
    assertEscaping(e, "&laquo;", '\u00AB');
    assertEscaping(e, "&diams;", '\u2666');

    // Test decimal escaping conversion (3, 4 & 5 length decimals).
    assertEscaping(e, "&#127;",   '\u007F');
    assertEscaping(e, "&#256;",   '\u0100');
    assertEscaping(e, "&#9831;",  '\u2667');
    assertEscaping(e, "&#65535;", '\uFFFF');

    // Test escaping for surrogate characters (5, 6 & 7 length decimals).
    assertUnicodeEscaping(e, "&#65536;",    // 0x10000
        Character.MIN_HIGH_SURROGATE, Character.MIN_LOW_SURROGATE);
    assertUnicodeEscaping(e, "&#100000;", '\uD821', '\uDEA0');
    assertUnicodeEscaping(e, "&#1114111;",  // U+10FFFF
        Character.MAX_HIGH_SURROGATE, Character.MAX_LOW_SURROGATE);
  }

  public void testHtmlContentEscaper() throws Exception {
    assertEquals("xxx", HtmlEscapers.htmlContentEscaper().escape("xxx"));
    assertEquals("&quot;test&quot;",
        HtmlEscapers.htmlContentEscaper().escape("\"test\""));
    assertEquals("&#39;test&#39;",
        HtmlEscapers.htmlContentEscaper().escape("\'test'"));
    assertEquals("test &amp; test &amp; test",
        HtmlEscapers.htmlContentEscaper().escape("test & test & test"));
    assertEquals("test &lt;&lt; 1",
        HtmlEscapers.htmlContentEscaper().escape("test << 1"));
    assertEquals("test &gt;&gt; 1",
        HtmlEscapers.htmlContentEscaper().escape("test >> 1"));
    assertEquals("&lt;tab&gt;",
        HtmlEscapers.htmlContentEscaper().escape("<tab>"));

    // Test simple escape of '&'.
    assertEquals("foo&amp;bar",
        HtmlEscapers.htmlContentEscaper().escape("foo&bar"));

    // If the string contains no escapes, it should return the arg.
    // Note: assert<b>Same</b> for this implementation.
    String s = "blah blah farhvergnugen";
    assertSame(s, HtmlEscapers.htmlContentEscaper().escape(s));

    // Tests escapes at begin and end of string.
    assertEquals("&lt;p&gt;", HtmlEscapers.htmlContentEscaper().escape("<p>"));

    // Test all escapes.
    assertEquals("a&quot;b&lt;c&gt;d&amp;",
        HtmlEscapers.htmlContentEscaper().escape("a\"b<c>d&"));

    // Test two escapes in a row.
    assertEquals("foo&amp;&amp;bar",
        HtmlEscapers.htmlContentEscaper().escape("foo&&bar"));

    // Test many non-escaped characters.
    s = "!@#$%^*()_+=-/?\\|]}[{,.;:"
      + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
      + "1234567890";
    assertSame(s, HtmlEscapers.htmlContentEscaper().escape(s));
  }
}

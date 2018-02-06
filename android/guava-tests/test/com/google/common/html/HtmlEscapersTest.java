/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.html;

import static com.google.common.html.HtmlEscapers.htmlEscaper;

import com.google.common.annotations.GwtCompatible;
import junit.framework.TestCase;

/**
 * Tests for the {@link HtmlEscapers} class.
 *
 * @author David Beaumont
 */
@GwtCompatible
public class HtmlEscapersTest extends TestCase {

  public void testHtmlEscaper() throws Exception {
    assertEquals("xxx", htmlEscaper().escape("xxx"));
    assertEquals("&quot;test&quot;", htmlEscaper().escape("\"test\""));
    assertEquals("&#39;test&#39;", htmlEscaper().escape("\'test'"));
    assertEquals("test &amp; test &amp; test", htmlEscaper().escape("test & test & test"));
    assertEquals("test &lt;&lt; 1", htmlEscaper().escape("test << 1"));
    assertEquals("test &gt;&gt; 1", htmlEscaper().escape("test >> 1"));
    assertEquals("&lt;tab&gt;", htmlEscaper().escape("<tab>"));

    // Test simple escape of '&'.
    assertEquals("foo&amp;bar", htmlEscaper().escape("foo&bar"));

    // If the string contains no escapes, it should return the arg.
    // Note: assert<b>Same</b> for this implementation.
    String s = "blah blah farhvergnugen";
    assertSame(s, htmlEscaper().escape(s));

    // Tests escapes at begin and end of string.
    assertEquals("&lt;p&gt;", htmlEscaper().escape("<p>"));

    // Test all escapes.
    assertEquals("a&quot;b&lt;c&gt;d&amp;", htmlEscaper().escape("a\"b<c>d&"));

    // Test two escapes in a row.
    assertEquals("foo&amp;&amp;bar", htmlEscaper().escape("foo&&bar"));

    // Test many non-escaped characters.
    s =
        "!@#$%^*()_+=-/?\\|]}[{,.;:"
            + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "1234567890";
    assertSame(s, htmlEscaper().escape(s));
  }
}

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
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for the {@link HtmlEscapers} class.
 *
 * @author David Beaumont
 */
@GwtCompatible
@NullUnmarked
public class HtmlEscapersTest extends TestCase {

  public void testHtmlEscaper() {
    assertThat(htmlEscaper().escape("xxx")).isEqualTo("xxx");
    assertThat(htmlEscaper().escape("\"test\"")).isEqualTo("&quot;test&quot;");
    assertThat(htmlEscaper().escape("'test'")).isEqualTo("&#39;test&#39;");
    assertThat(htmlEscaper().escape("test & test & test")).isEqualTo("test &amp; test &amp; test");
    assertThat(htmlEscaper().escape("test << 1")).isEqualTo("test &lt;&lt; 1");
    assertThat(htmlEscaper().escape("test >> 1")).isEqualTo("test &gt;&gt; 1");
    assertThat(htmlEscaper().escape("<tab>")).isEqualTo("&lt;tab&gt;");

    // Test simple escape of '&'.
    assertThat(htmlEscaper().escape("foo&bar")).isEqualTo("foo&amp;bar");

    // If the string contains no escapes, it should return the arg.
    // Note: assert<b>Same</b> for this implementation.
    String s = "blah blah farhvergnugen";
    assertThat(htmlEscaper().escape(s)).isSameInstanceAs(s);

    // Tests escapes at begin and end of string.
    assertThat(htmlEscaper().escape("<p>")).isEqualTo("&lt;p&gt;");

    // Test all escapes.
    assertThat(htmlEscaper().escape("a\"b<c>d&")).isEqualTo("a&quot;b&lt;c&gt;d&amp;");

    // Test two escapes in a row.
    assertThat(htmlEscaper().escape("foo&&bar")).isEqualTo("foo&amp;&amp;bar");

    // Test many non-escaped characters.
    s =
        "!@#$%^*()_+=-/?\\|]}[{,.;:"
            + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "1234567890";
    assertThat(htmlEscaper().escape(s)).isSameInstanceAs(s);
  }
}

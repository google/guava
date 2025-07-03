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
import static com.google.common.net.UrlEscaperTesting.assertBasicUrlEscaper;
import static com.google.common.net.UrlEscaperTesting.assertPathEscaper;
import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static com.google.common.net.UrlEscapers.urlFragmentEscaper;
import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;

import com.google.common.annotations.GwtCompatible;
import com.google.common.escape.UnicodeEscaper;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for the {@link UrlEscapers} class.
 *
 * @author David Beaumont
 */
@GwtCompatible
@NullUnmarked
public class UrlEscapersTest extends TestCase {
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

  public void testUrlFragmentEscaper() {
    UnicodeEscaper e = (UnicodeEscaper) urlFragmentEscaper();
    assertUnescaped(e, '+');
    assertUnescaped(e, '/');
    assertUnescaped(e, '?');

    assertPathEscaper(e);
  }
}

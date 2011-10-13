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

package com.google.common.escape;

import static com.google.common.escape.testing.EscaperAsserts.assertEscaping;
import static com.google.common.escape.testing.EscaperAsserts.assertUnescaped;

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

/**
 * @author David Beaumont
 */
@GwtCompatible
public class SourceCodeEscapersTest extends TestCase {

  // ASCII control characters.
  private static final String ASCII_CONTROL_UNESCAPED =
      "\000\001\002\003\004\005\006\007" +
      "\010\011\012\013\014\015\016\017" +
      "\020\021\022\023\024\025\026\027" +
      "\030\031\032\033\034\035\036\037";
  private static final String ASCII_CONTROL_ESCAPED =
      "\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007" +
      "\\b\\t\\n\\u000b\\f\\r\\u000e\\u000f" +
      "\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017" +
      "\\u0018\\u0019\\u001a\\u001b\\u001c\\u001d\\u001e\\u001f";
  private static final String ASCII_CONTROL_ESCAPED_WITH_OCTAL =
      "\\000\\001\\002\\003\\004\\005\\006\\007" +
      "\\b\\t\\n\\013\\f\\r\\016\\017" +
      "\\020\\021\\022\\023\\024\\025\\026\\027" +
      "\\030\\031\\032\\033\\034\\035\\036\\037";

  // This does not include single quotes, double quotes or backslash.
  private static final String SAFE_ASCII =
      " !#$%&()*+,-./0123456789:;<=>?@" +
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`" +
      "abcdefghijklmnopqrstuvwxyz{|}~";

  private static final String ABOVE_ASCII_UNESCAPED =
      "\200\377\u0100\u0800\u1000\u89AB\uCDEF\uFFFF";
  private static final String ABOVE_ASCII_ESCAPED =
      "\\u0080\\u00ff\\u0100\\u0800\\u1000\\u89ab\\ucdef\\uffff";
  private static final String ABOVE_ASCII_ESCAPED_WITH_OCTAL =
      "\\200\\377\\u0100\\u0800\\u1000\\u89ab\\ucdef\\uffff";

  public void testJavaCharEscaper() {
    CharEscaper escaper = SourceCodeEscapers.javaCharEscaper();
    assertEquals(ASCII_CONTROL_ESCAPED, escaper.escape(ASCII_CONTROL_UNESCAPED));
    assertEquals(SAFE_ASCII, escaper.escape(SAFE_ASCII));
    assertEquals(ABOVE_ASCII_ESCAPED, escaper.escape(ABOVE_ASCII_UNESCAPED));
    // Single quotes, double quotes and backslash are escaped.
    assertEquals("\\'\\\"\\\\", escaper.escape("'\"\\"));
  }

  public void testJavaStringUnicodeEscaper() {
    CharEscaper escaper = SourceCodeEscapers.javaStringUnicodeEscaper();

    // Test that 7-bit ASCII is never escaped.
    assertUnescaped(escaper, '\u0000');
    assertUnescaped(escaper, 'a');
    assertUnescaped(escaper, '\'');
    assertUnescaped(escaper, '\"');
    assertUnescaped(escaper, '~');
    assertUnescaped(escaper, '\u007f');

    // Test hex escaping for UTF-16.
    assertEscaping(escaper, "\\u0080", '\u0080');
    assertEscaping(escaper, "\\u00bb", '\u00bb');
    assertEscaping(escaper, "\\u0100", '\u0100');
    assertEscaping(escaper, "\\u1000", '\u1000');
    assertEscaping(escaper, "\\uffff", '\uffff');

    // Make sure HEX_DIGITS are all correct.
    assertEscaping(escaper, "\\u0123", '\u0123');
    assertEscaping(escaper, "\\u4567", '\u4567');
    assertEscaping(escaper, "\\u89ab", '\u89ab');
    assertEscaping(escaper, "\\ucdef", '\ucdef');

    // Verify that input is treated as UTF-16 and _not_ Unicode.
    assertEquals("\\udbff\\udfff", escaper.escape("\uDBFF\uDFFF"));

    // Verify the escaper does _not_ double escape (it is idempotent).
    assertEquals("\\uabcd", escaper.escape("\uABCD"));
    assertEquals("\\uabcd", escaper.escape(escaper.escape("\uABCD")));

    // Existing escape sequences are left completely unchanged (including case).
    assertEquals("\\uAbCd", escaper.escape("\\uAbCd"));
  }

  public void testJavascriptEscaper() {
    CharEscaper e = SourceCodeEscapers.javascriptEscaper();

    // Test chars that are specially escaped.
    assertEscaping(e, "\\x27", '\'');
    assertEscaping(e, "\\x22", '"');
    assertEscaping(e, "\\x3c", '<');
    assertEscaping(e, "\\x3d", '=');
    assertEscaping(e, "\\x3e", '>');
    assertEscaping(e, "\\x26", '&');
    assertEscaping(e, "\\b",   '\b');
    assertEscaping(e, "\\t",   '\t');
    assertEscaping(e, "\\n",   '\n');
    assertEscaping(e, "\\f",   '\f');
    assertEscaping(e, "\\r",   '\r');
    assertEscaping(e, "\\\\",  '\\');

    // A sampling of chars that do not need to be escaped.
    assertUnescaped(e, ' ');
    assertUnescaped(e, '1');
    assertUnescaped(e, 'A');
    assertUnescaped(e, 'a');
    assertUnescaped(e, '~');

    // Test chars that can be 2 digit hex escaped.
    // Note that some sources will tell you that \u000b can be escaped to \v but
    // some versions of IE will handle this incorrectly
    assertEscaping(e, "\\x00", '\u0000');
    assertEscaping(e, "\\x0b", '\u000b');
    assertEscaping(e, "\\x19", '\u0019');
    assertEscaping(e, "\\x7f", '\u007f');
    assertEscaping(e, "\\xff", '\u00ff');

    // Test chars that must be 4 digit hex escaped.
    // Specifically test \u200d as a Unicode Format-Control Character which we
    // *always* want to escape.
    assertEscaping(e, "\\u0100", '\u0100');
    assertEscaping(e, "\\u200d", '\u200d');
    assertEscaping(e, "\\uffff", '\uffff');

    // Javascript is UTF-16 so surrogate pairs are not converted to code points.
    assertEquals("\\udbff\\udfff", e.escape("\uDBFF\uDFFF"));
  }

  public void testPythonEscaper() throws Exception {
    UnicodeEscaper e = SourceCodeEscapers.pythonEscaper();

    assertEquals("a normal ascii string", e.escape("a normal ascii string"));
    assertEquals("\\\\", e.escape("\\"));
    assertEquals("\\'\\\"\\n\\r\\t", e.escape("'\"\n\r\t"));
    assertEquals("\\x08\\x0c", e.escape("\b\f"));
    // Test highest and lowest surrogate pairs
    assertEquals("\\U00010000", e.escape("\uD800\uDC00"));
    assertEquals("\\U0010ffff", e.escape("\uDBFF\uDFFF"));
  }
}

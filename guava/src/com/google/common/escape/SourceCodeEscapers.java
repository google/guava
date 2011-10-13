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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory for Escaper instances used to escape strings for safe use in
 * various common programming languages.
 *
 * @author Alex Matevossian
 * @author David Beaumont
 * @since 11.0
 */
@Beta
@GwtCompatible
public final class SourceCodeEscapers {
  private SourceCodeEscapers() { }

  // For each xxxEscaper() method, please add links to external reference pages
  // that are considered authoritative for the behavior of that escaper.

  // From: http://en.wikipedia.org/wiki/ASCII#ASCII_printable_characters
  private static final char PRINTABLE_ASCII_MIN = 0x20;  // ' '
  private static final char PRINTABLE_ASCII_MAX = 0x7E;  // '~'

  private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

  /**
   * Returns an {@link Escaper} instance that escapes special characters in a
   * string so it can safely be included in either a Java character literal or
   * string literal. This is the preferred way to escape Java characters for
   * use in String or character literals.
   *
   * <p>See: <a href=
   * "http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#101089"
   * >The Java Language Specification</a> for more details.
   */
  public static CharEscaper javaCharEscaper() {
    return JAVA_CHAR_ESCAPER;
  }

  private static final CharEscaper JAVA_CHAR_ESCAPER;
  private static final CharEscaper JAVA_CHAR_ESCAPER_WITH_OCTAL;
  private static final CharEscaper JAVA_STRING_ESCAPER_WITH_OCTAL;
  static {
    Map<Character, String> javaMap = new HashMap<Character, String>();
    javaMap.put('\b', "\\b");
    javaMap.put('\f', "\\f");
    javaMap.put('\n', "\\n");
    javaMap.put('\r', "\\r");
    javaMap.put('\t', "\\t");
    javaMap.put('\"', "\\\"");
    javaMap.put('\\', "\\\\");
    JAVA_STRING_ESCAPER_WITH_OCTAL = new JavaCharEscaperWithOctal(javaMap);
    // The only difference is that the char escaper also escapes single quotes.
    javaMap.put('\'', "\\'");
    JAVA_CHAR_ESCAPER = new JavaCharEscaper(javaMap);
    JAVA_CHAR_ESCAPER_WITH_OCTAL = new JavaCharEscaperWithOctal(javaMap);
  }

  // This escaper does not produce octal escape sequences. See:
  // http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#101089
  //  "Octal escapes are provided for compatibility with C, but can express
  //   only Unicode values \u0000 through \u00FF, so Unicode escapes are
  //   usually preferred."
  private static class JavaCharEscaper extends ArrayBasedCharEscaper {
    JavaCharEscaper(Map<Character, String> replacements) {
      super(replacements, PRINTABLE_ASCII_MIN, PRINTABLE_ASCII_MAX);
    }

    @Override protected char[] escapeUnsafe(char c) {
      return asUnicodeHexEscape(c);
    }
  }

  private static class JavaCharEscaperWithOctal extends ArrayBasedCharEscaper {
    JavaCharEscaperWithOctal(Map<Character, String> replacements) {
      super(replacements, PRINTABLE_ASCII_MIN, PRINTABLE_ASCII_MAX);
    }

    @Override protected char[] escapeUnsafe(char c) {
      if (c < 0x100) {
        return asOctalEscape(c);
      } else {
        return asUnicodeHexEscape(c);
      }
    }
  }

  /**
   * Returns a {@link CharEscaper} instance that replaces non-ASCII characters
   * in a string with their Unicode escape sequences ({@code \\uxxxx} where
   * {@code xxxx} is a hex number). Existing escape sequences won't be affected.
   *
   * <p>As existing escape sequences are not re-escaped, this escaper is
   * idempotent. However this means that there can be no well defined inverse
   * function for this escaper.
   *
   * <p><b>Note</b></p>: the returned escaper is still a {@code CharEscaper} and
   * will not combine surrogate pairs into a single code point before escaping.
   */
  public static CharEscaper javaStringUnicodeEscaper() {
    return JAVA_STRING_UNICODE_ESCAPER;
  }

  private static final CharEscaper JAVA_STRING_UNICODE_ESCAPER
      = new CharEscaper() {
          @Override protected char[] escape(char c) {
            if (c < 0x80) {
              return null;
            }
            return asUnicodeHexEscape(c);
          }
        };

  /**
   * Returns an {@link Escaper} instance that replaces non-ASCII characters
   * in a string with their equivalent Javascript UTF-16 escape sequences
   * "{@literal \}unnnn", "\xnn" or special replacement sequences "\b", "\t",
   * "\n", "\f", "\r" or "\\".
   */
  public static CharEscaper javascriptEscaper() {
    return JAVASCRIPT_ESCAPER;
  }

  /**
   * A CharEscaper for javascript strings. Turns all non-ASCII characters into
   * ASCII javascript escape sequences.
   */
  private static final CharEscaper JAVASCRIPT_ESCAPER;
  static {
    Map<Character, String> jsMap = new HashMap<Character, String>();
    jsMap.put('\'', "\\x27");
    jsMap.put('"',  "\\x22");
    jsMap.put('<',  "\\x3c");
    jsMap.put('=',  "\\x3d");
    jsMap.put('>',  "\\x3e");
    jsMap.put('&',  "\\x26");
    jsMap.put('\b', "\\b");
    jsMap.put('\t', "\\t");
    jsMap.put('\n', "\\n");
    jsMap.put('\f', "\\f");
    jsMap.put('\r', "\\r");
    jsMap.put('\\', "\\\\");
    JAVASCRIPT_ESCAPER = new ArrayBasedCharEscaper(
        jsMap, PRINTABLE_ASCII_MIN, PRINTABLE_ASCII_MAX) {
          @Override
          protected char[] escapeUnsafe(char c) {
            // Do two digit hex escape for value less than 0x100.
            if (c < 0x100) {
              char[] r = new char[4];
              r[3] = HEX_DIGITS[c & 0xF];
              c >>>= 4;
              r[2] = HEX_DIGITS[c & 0xF];
              r[1] = 'x';
              r[0] = '\\';
              return r;
            }
            return asUnicodeHexEscape(c);
          }
    };
  }

  /**
   * Returns an {@link Escaper} instance that escapes special characters
   * from a string so it can safely be included in a Python Unicode string
   * literal.
   *
   * <p>The escaper returned by this method will correctly deal with all Unicode
   * code point values and generate escape sequences of the form
   * "{@literal \}unnnn" or "\Unnnnnnnn".
   *
   * <p><b>Note</b>: According to the
   * <a href=
   * "http://docs.python.org/reference/lexical_analysis.html#string-literals">
   * Python reference documentation</a> the following escape sequences are
   * recognized in string literals:
   * <ul>
   * <li>'\a'  ASCII Bell (BEL)</li>
   * <li>'\b'  ASCII Backspace (BS)</li>
   * <li>'\f'  ASCII Formfeed (FF)</li>
   * <li>'\n'  ASCII Linefeed (LF)</li>
   * <li>'\r'  ASCII Carriage Return (CR)</li>
   * <li>'\t'  ASCII Horizontal Tab (TAB)</li>
   * <li>'\\'  Backslash (\)</li>
   * <li>'\''  Single quote (')</li>
   * <li>'\"'  Double quote (")</li>
   * </ul>
   * <p>However in order to match the output of the Python {@code repr()}
   * function this escaper only escapes '\n', '\r', '\t', '\\', '\'' and '\"'.
   * The remaining ASCII control characters are escaped in the form "\xnn".
   */
  public static UnicodeEscaper pythonEscaper() {
    return PYTHON_ESCAPER;
  }

  private static final UnicodeEscaper PYTHON_ESCAPER;
  static {
    Map<Character, String> pythonMap = new HashMap<Character, String>();
    pythonMap.put('\n', "\\n");
    pythonMap.put('\r', "\\r");
    pythonMap.put('\t', "\\t");
    pythonMap.put('\\', "\\\\");
    pythonMap.put('\'', "\\\'");
    pythonMap.put('\"', "\\\"");

    PYTHON_ESCAPER = new ArrayBasedUnicodeEscaper(
        pythonMap, PRINTABLE_ASCII_MIN, PRINTABLE_ASCII_MAX, null) {
          @Override
          protected char[] escapeUnsafe(int cp) {
            if (cp < 0x100) {
              // Format as \xnn
              char[] r = new char[4];
              r[3] = HEX_DIGITS[cp & 0xF];
              cp >>>= 4;
              r[2] = HEX_DIGITS[cp & 0xF];
              r[1] = 'x';
              r[0] = '\\';
              return r;
            } else if (cp < 0x10000) {
              return asUnicodeHexEscape((char) cp);
            } else {
              // Format as \Unnnnnnnn
              char[] r = new char[10];
              r[9] = HEX_DIGITS[cp & 0xF];
              cp >>>= 4;
              r[8] = HEX_DIGITS[cp & 0xF];
              cp >>>= 4;
              r[7] = HEX_DIGITS[cp & 0xF];
              cp >>>= 4;
              r[6] = HEX_DIGITS[cp & 0xF];
              cp >>>= 4;
              r[5] = HEX_DIGITS[cp & 0xF];
              cp >>>= 4;
              r[4] = HEX_DIGITS[cp & 0xF];
              cp >>>= 4;
              r[3] = HEX_DIGITS[cp & 0xF];
              cp >>>= 4;
              r[2] = HEX_DIGITS[cp & 0xF];
              r[1] = 'U';
              r[0] = '\\';
              return r;
            }
          }
        };
  }

  // Helper for common case of escaping a single char.
  private static char[] asUnicodeHexEscape(char c) {
    // Equivalent to String.format("\\u%04x", (int)c);
    char[] r = new char[6];
    r[0] = '\\';
    r[1] = 'u';
    r[5] = HEX_DIGITS[c & 0xF];
    c >>>= 4;
    r[4] = HEX_DIGITS[c & 0xF];
    c >>>= 4;
    r[3] = HEX_DIGITS[c & 0xF];
    c >>>= 4;
    r[2] = HEX_DIGITS[c & 0xF];
    return r;
  }

  // Helper for backward compatible octal escape sequences (c < 256)
  private static char[] asOctalEscape(char c) {
    char[] r = new char[4];
    r[0] = '\\';
    r[3] = HEX_DIGITS[c & 0x7];
    c >>>= 3;
    r[2] = HEX_DIGITS[c & 0x7];
    c >>>= 3;
    r[1] = HEX_DIGITS[c & 0x3];
    return r;
  }
}

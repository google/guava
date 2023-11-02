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

package com.google.common.escape;

import com.google.common.annotations.GwtCompatible;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@link UnicodeEscaper}.
 *
 * @author David Beaumont
 */
@GwtCompatible
public class UnicodeEscaperTest extends TestCase {

  private static final String SMALLEST_SURROGATE =
      "" + Character.MIN_HIGH_SURROGATE + Character.MIN_LOW_SURROGATE;
  private static final String LARGEST_SURROGATE =
      "" + Character.MAX_HIGH_SURROGATE + Character.MAX_LOW_SURROGATE;

  private static final String TEST_STRING =
      "\0abyz\u0080\u0100\u0800\u1000ABYZ\uffff" + SMALLEST_SURROGATE + "0189" + LARGEST_SURROGATE;

  // Escapes nothing
  private static final UnicodeEscaper NOP_ESCAPER =
      new UnicodeEscaper() {
        @Override
        protected char @Nullable [] escape(int c) {
          return null;
        }
      };

  // Escapes everything except [a-zA-Z0-9]
  private static final UnicodeEscaper SIMPLE_ESCAPER =
      new UnicodeEscaper() {
        @Override
        protected char @Nullable [] escape(int cp) {
          return ('a' <= cp && cp <= 'z') || ('A' <= cp && cp <= 'Z') || ('0' <= cp && cp <= '9')
              ? null
              : ("[" + String.valueOf(cp) + "]").toCharArray();
        }
      };

  public void testNopEscaper() {
    UnicodeEscaper e = NOP_ESCAPER;
    assertEquals(TEST_STRING, escapeAsString(e, TEST_STRING));
  }

  public void testSimpleEscaper() {
    UnicodeEscaper e = SIMPLE_ESCAPER;
    String expected =
        "[0]abyz[128][256][2048][4096]ABYZ[65535]"
            + "["
            + Character.MIN_SUPPLEMENTARY_CODE_POINT
            + "]"
            + "0189["
            + Character.MAX_CODE_POINT
            + "]";
    assertEquals(expected, escapeAsString(e, TEST_STRING));
  }

  public void testGrowBuffer() { // need to grow past an initial 1024 byte buffer
    StringBuilder input = new StringBuilder();
    StringBuilder expected = new StringBuilder();
    for (int i = 256; i < 1024; i++) {
      input.append((char) i);
      expected.append("[" + i + "]");
    }
    assertEquals(expected.toString(), SIMPLE_ESCAPER.escape(input.toString()));
  }

  public void testSurrogatePairs() {
    UnicodeEscaper e = SIMPLE_ESCAPER;

    // Build up a range of surrogate pair characters to test
    final int min = Character.MIN_SUPPLEMENTARY_CODE_POINT;
    final int max = Character.MAX_CODE_POINT;
    final int range = max - min;
    final int s1 = min + (1 * range) / 4;
    final int s2 = min + (2 * range) / 4;
    final int s3 = min + (3 * range) / 4;
    final char[] dst = new char[12];

    // Put surrogate pairs at odd indices so they can be split easily
    dst[0] = 'x';
    Character.toChars(min, dst, 1);
    Character.toChars(s1, dst, 3);
    Character.toChars(s2, dst, 5);
    Character.toChars(s3, dst, 7);
    Character.toChars(max, dst, 9);
    dst[11] = 'x';
    String test = new String(dst);

    // Get the expected result string
    String expected = "x[" + min + "][" + s1 + "][" + s2 + "][" + s3 + "][" + max + "]x";
    assertEquals(expected, escapeAsString(e, test));
  }

  public void testTrailingHighSurrogate() {
    String test = "abc" + Character.MIN_HIGH_SURROGATE;
    try {
      escapeAsString(NOP_ESCAPER, test);
      fail("Trailing high surrogate should cause exception");
    } catch (IllegalArgumentException expected) {
      // Pass
    }
    try {
      escapeAsString(SIMPLE_ESCAPER, test);
      fail("Trailing high surrogate should cause exception");
    } catch (IllegalArgumentException expected) {
      // Pass
    }
  }

  public void testNullInput() {
    UnicodeEscaper e = SIMPLE_ESCAPER;
    try {
      e.escape((String) null);
      fail("Null string should cause exception");
    } catch (NullPointerException expected) {
      // Pass
    }
  }

  public void testBadStrings() {
    UnicodeEscaper e = SIMPLE_ESCAPER;
    String[] BAD_STRINGS = {
      String.valueOf(Character.MIN_LOW_SURROGATE),
      Character.MIN_LOW_SURROGATE + "xyz",
      "abc" + Character.MIN_LOW_SURROGATE,
      "abc" + Character.MIN_LOW_SURROGATE + "xyz",
      String.valueOf(Character.MAX_LOW_SURROGATE),
      Character.MAX_LOW_SURROGATE + "xyz",
      "abc" + Character.MAX_LOW_SURROGATE,
      "abc" + Character.MAX_LOW_SURROGATE + "xyz",
    };
    for (String s : BAD_STRINGS) {
      try {
        escapeAsString(e, s);
        fail("Isolated low surrogate should cause exception [" + s + "]");
      } catch (IllegalArgumentException expected) {
        // Pass
      }
    }
  }

  public void testFalsePositivesForNextEscapedIndex() {
    UnicodeEscaper e =
        new UnicodeEscaper() {
          // Canonical escaper method that only escapes lower case ASCII letters.
          @Override
          protected char @Nullable [] escape(int cp) {
            return ('a' <= cp && cp <= 'z') ? new char[] {Character.toUpperCase((char) cp)} : null;
          }
          // Inefficient implementation that defines all letters as escapable.
          @Override
          protected int nextEscapeIndex(CharSequence csq, int index, int end) {
            while (index < end && !Character.isLetter(csq.charAt(index))) {
              index++;
            }
            return index;
          }
        };
    assertEquals("\0HELLO \uD800\uDC00 WORLD!\n", e.escape("\0HeLLo \uD800\uDC00 WorlD!\n"));
  }

  public void testCodePointAt_IndexOutOfBoundsException() {
    try {
      UnicodeEscaper.codePointAt("Testing...", 4, 2);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  private static String escapeAsString(Escaper e, String s) {
    return e.escape(s);
  }
}

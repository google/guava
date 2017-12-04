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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap;
import com.google.common.escape.testing.EscaperAsserts;
import java.io.IOException;
import junit.framework.TestCase;

/** @author David Beaumont */
@GwtCompatible
public class ArrayBasedUnicodeEscaperTest extends TestCase {
  private static final ImmutableMap<Character, String> NO_REPLACEMENTS = ImmutableMap.of();
  private static final ImmutableMap<Character, String> SIMPLE_REPLACEMENTS =
      ImmutableMap.of(
          '\n', "<newline>",
          '\t', "<tab>",
          '&', "<and>");
  private static final char[] NO_CHARS = new char[0];

  public void testReplacements() throws IOException {
    // In reality this is not a very sensible escaper to have (if you are only
    // escaping elements from a map you would use a ArrayBasedCharEscaper).
    UnicodeEscaper escaper =
        new ArrayBasedUnicodeEscaper(
            SIMPLE_REPLACEMENTS, Character.MIN_VALUE, Character.MAX_CODE_POINT, null) {
          @Override
          protected char[] escapeUnsafe(int c) {
            return NO_CHARS;
          }
        };
    EscaperAsserts.assertBasic(escaper);
    assertEquals("<tab>Fish <and> Chips<newline>", escaper.escape("\tFish & Chips\n"));

    // Verify that everything else is left unescaped.
    String safeChars = "\0\u0100\uD800\uDC00\uFFFF";
    assertEquals(safeChars, escaper.escape(safeChars));

    // Ensure that Unicode escapers behave correctly wrt badly formed input.
    String badUnicode = "\uDC00\uD800";
    try {
      escaper.escape(badUnicode);
      fail("should fail for bad Unicode");
    } catch (IllegalArgumentException e) {
      // Pass
    }
  }

  public void testSafeRange() throws IOException {
    // Basic escaping of unsafe chars (wrap them in {,}'s)
    UnicodeEscaper wrappingEscaper =
        new ArrayBasedUnicodeEscaper(NO_REPLACEMENTS, 'A', 'Z', null) {
          @Override
          protected char[] escapeUnsafe(int c) {
            return ("{" + (char) c + "}").toCharArray();
          }
        };
    EscaperAsserts.assertBasic(wrappingEscaper);
    // '[' and '@' lie either side of [A-Z].
    assertEquals("{[}FOO{@}BAR{]}", wrappingEscaper.escape("[FOO@BAR]"));
  }

  public void testDeleteUnsafeChars() throws IOException {
    UnicodeEscaper deletingEscaper =
        new ArrayBasedUnicodeEscaper(NO_REPLACEMENTS, ' ', '~', null) {
          @Override
          protected char[] escapeUnsafe(int c) {
            return NO_CHARS;
          }
        };
    EscaperAsserts.assertBasic(deletingEscaper);
    assertEquals(
        "Everything outside the printable ASCII range is deleted.",
        deletingEscaper.escape(
            "\tEverything\0 outside the\uD800\uDC00 "
                + "printable ASCII \uFFFFrange is \u007Fdeleted.\n"));
  }

  public void testReplacementPriority() throws IOException {
    UnicodeEscaper replacingEscaper =
        new ArrayBasedUnicodeEscaper(SIMPLE_REPLACEMENTS, ' ', '~', null) {
          private final char[] unknown = new char[] {'?'};

          @Override
          protected char[] escapeUnsafe(int c) {
            return unknown;
          }
        };
    EscaperAsserts.assertBasic(replacingEscaper);

    // Replacements are applied first regardless of whether the character is in
    // the safe range or not ('&' is a safe char while '\t' and '\n' are not).
    assertEquals(
        "<tab>Fish <and>? Chips?<newline>", replacingEscaper.escape("\tFish &\0 Chips\r\n"));
  }

  public void testCodePointsFromSurrogatePairs() throws IOException {
    UnicodeEscaper surrogateEscaper =
        new ArrayBasedUnicodeEscaper(NO_REPLACEMENTS, 0, 0x20000, null) {
          private final char[] escaped = new char[] {'X'};

          @Override
          protected char[] escapeUnsafe(int c) {
            return escaped;
          }
        };
    EscaperAsserts.assertBasic(surrogateEscaper);

    // A surrogate pair defining a code point within the safe range.
    String safeInput = "\uD800\uDC00"; // 0x10000
    assertEquals(safeInput, surrogateEscaper.escape(safeInput));

    // A surrogate pair defining a code point outside the safe range (but both
    // of the surrogate characters lie within the safe range). It is important
    // not to accidentally treat this as a sequence of safe characters.
    String unsafeInput = "\uDBFF\uDFFF"; // 0x10FFFF
    assertEquals("X", surrogateEscaper.escape(unsafeInput));
  }
}

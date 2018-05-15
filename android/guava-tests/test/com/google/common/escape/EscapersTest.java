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
public class EscapersTest extends TestCase {
  public void testNullEscaper() throws IOException {
    Escaper escaper = Escapers.nullEscaper();
    EscaperAsserts.assertBasic(escaper);
    String s = "\0\n\t\\az09~\uD800\uDC00\uFFFF";
    assertEquals("null escaper should have no effect", s, escaper.escape(s));
  }

  public void testBuilderInitialStateNoReplacement() {
    // Unsafe characters aren't modified by default (unsafeReplacement == null).
    Escaper escaper = Escapers.builder().setSafeRange('a', 'z').build();
    assertEquals("The Quick Brown Fox", escaper.escape("The Quick Brown Fox"));
  }

  public void testBuilderInitialStateNoneUnsafe() {
    // No characters are unsafe by default (safeMin == 0, safeMax == 0xFFFF).
    Escaper escaper = Escapers.builder().setUnsafeReplacement("X").build();
    assertEquals("\0\uFFFF", escaper.escape("\0\uFFFF"));
  }

  public void testBuilderRetainsState() {
    // Setting a safe range and unsafe replacement works as expected.
    Escapers.Builder builder = Escapers.builder();
    builder.setSafeRange('a', 'z');
    builder.setUnsafeReplacement("X");
    assertEquals("XheXXuickXXrownXXoxX", builder.build().escape("The Quick Brown Fox!"));
    // Explicit replacements take priority over unsafe characters.
    builder.addEscape(' ', "_");
    builder.addEscape('!', "_");
    assertEquals("Xhe_Xuick_Xrown_Xox_", builder.build().escape("The Quick Brown Fox!"));
    // Explicit replacements take priority over safe characters.
    builder.setSafeRange(' ', '~');
    assertEquals("The_Quick_Brown_Fox_", builder.build().escape("The Quick Brown Fox!"));
  }

  public void testBuilderCreatesIndependentEscapers() {
    // Setup a simple builder and create the first escaper.
    Escapers.Builder builder = Escapers.builder();
    builder.setSafeRange('a', 'z');
    builder.setUnsafeReplacement("X");
    builder.addEscape(' ', "_");
    Escaper first = builder.build();
    // Modify one of the existing mappings before creating a new escaper.
    builder.addEscape(' ', "-");
    builder.addEscape('!', "$");
    Escaper second = builder.build();
    // This should have no effect on existing escapers.
    builder.addEscape(' ', "*");

    // Test both escapers after modifying the builder.
    assertEquals("Xhe_Xuick_Xrown_XoxX", first.escape("The Quick Brown Fox!"));
    assertEquals("Xhe-Xuick-Xrown-Xox$", second.escape("The Quick Brown Fox!"));
  }

  public void testAsUnicodeEscaper() throws IOException {
    CharEscaper charEscaper =
        createSimpleCharEscaper(
            ImmutableMap.<Character, char[]>builder()
                .put('x', "<hello>".toCharArray())
                .put('\uD800', "<hi>".toCharArray())
                .put('\uDC00', "<lo>".toCharArray())
                .build());
    UnicodeEscaper unicodeEscaper = Escapers.asUnicodeEscaper(charEscaper);
    EscaperAsserts.assertBasic(unicodeEscaper);
    assertEquals("<hello><hi><lo>", charEscaper.escape("x\uD800\uDC00"));
    assertEquals("<hello><hi><lo>", unicodeEscaper.escape("x\uD800\uDC00"));

    // Test that wrapped escapers acquire good Unicode semantics.
    assertEquals("<hi><hello><lo>", charEscaper.escape("\uD800x\uDC00"));
    try {
      unicodeEscaper.escape("\uD800x\uDC00");
      fail("should have failed for bad Unicode input");
    } catch (IllegalArgumentException e) {
      // pass
    }
    assertEquals("<lo><hi>", charEscaper.escape("\uDC00\uD800"));
    try {
      unicodeEscaper.escape("\uDC00\uD800");
      fail("should have failed for bad Unicode input");
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  // A trival non-optimized escaper for testing.
  static CharEscaper createSimpleCharEscaper(final ImmutableMap<Character, char[]> replacementMap) {
    return new CharEscaper() {
      @Override
      protected char[] escape(char c) {
        return replacementMap.get(c);
      }
    };
  }

  // A trival non-optimized escaper for testing.
  static UnicodeEscaper createSimpleUnicodeEscaper(
      final ImmutableMap<Integer, char[]> replacementMap) {
    return new UnicodeEscaper() {
      @Override
      protected char[] escape(int cp) {
        return replacementMap.get(cp);
      }
    };
  }
}

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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap;
import com.google.common.escape.testing.EscaperAsserts;
import java.io.IOException;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * @author David Beaumont
 */
@GwtCompatible
@NullUnmarked
public class EscapersTest extends TestCase {
  public void testNullEscaper() throws IOException {
    Escaper escaper = Escapers.nullEscaper();
    EscaperAsserts.assertBasic(escaper);
    String s = "\0\n\t\\az09~\uD800\uDC00\uFFFF";
    assertWithMessage("null escaper should have no effect").that(escaper.escape(s)).isEqualTo(s);
  }

  public void testBuilderInitialStateNoReplacement() {
    // Unsafe characters aren't modified by default (unsafeReplacement == null).
    Escaper escaper = Escapers.builder().setSafeRange('a', 'z').build();
    assertThat(escaper.escape("The Quick Brown Fox")).isEqualTo("The Quick Brown Fox");
  }

  public void testBuilderInitialStateNoneUnsafe() {
    // No characters are unsafe by default (safeMin == 0, safeMax == 0xFFFF).
    Escaper escaper = Escapers.builder().setUnsafeReplacement("X").build();
    assertThat(escaper.escape("\0\uFFFF")).isEqualTo("\0\uFFFF");
  }

  public void testBuilderRetainsState() {
    // Setting a safe range and unsafe replacement works as expected.
    Escapers.Builder builder = Escapers.builder();
    builder.setSafeRange('a', 'z');
    builder.setUnsafeReplacement("X");
    assertThat(builder.build().escape("The Quick Brown Fox!")).isEqualTo("XheXXuickXXrownXXoxX");
    // Explicit replacements take priority over unsafe characters.
    builder.addEscape(' ', "_");
    builder.addEscape('!', "_");
    assertThat(builder.build().escape("The Quick Brown Fox!")).isEqualTo("Xhe_Xuick_Xrown_Xox_");
    // Explicit replacements take priority over safe characters.
    builder.setSafeRange(' ', '~');
    assertThat(builder.build().escape("The Quick Brown Fox!")).isEqualTo("The_Quick_Brown_Fox_");
  }

  public void testBuilderCreatesIndependentEscapers() {
    // Set up a simple builder and create the first escaper.
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
    assertThat(first.escape("The Quick Brown Fox!")).isEqualTo("Xhe_Xuick_Xrown_XoxX");
    assertThat(second.escape("The Quick Brown Fox!")).isEqualTo("Xhe-Xuick-Xrown-Xox$");
  }

  // A trivial non-optimized escaper for testing.
  static CharEscaper createSimpleCharEscaper(ImmutableMap<Character, char[]> replacementMap) {
    return new CharEscaper() {
      @Override
      protected char[] escape(char c) {
        return replacementMap.get(c);
      }
    };
  }

  // A trivial non-optimized escaper for testing.
  static UnicodeEscaper createSimpleUnicodeEscaper(ImmutableMap<Integer, char[]> replacementMap) {
    return new UnicodeEscaper() {
      @Override
      protected char[] escape(int cp) {
        return replacementMap.get(cp);
      }
    };
  }
}

/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.base;

/**
 * Common benchmarking utilities.
 *
 * @author Christopher Swenson
 * @author Louis Wasserman
 */
class BenchmarkHelpers {
  private static final String WHITESPACE_CHARACTERS =
      "\u00a0\u180e\u202f\t\n\013\f\r \u0085"
          + "\u1680\u2028\u2029\u205f\u3000\u2000\u2001\u2002\u2003\u2004\u2005"
          + "\u2006\u2007\u2008\u2009\u200a";
  private static final String ASCII_CHARACTERS;

  static {
    int spaceInAscii = 32;
    int sevenBitAsciiMax = 128;
    StringBuilder sb = new StringBuilder(sevenBitAsciiMax - spaceInAscii);
    for (int ch = spaceInAscii; ch < sevenBitAsciiMax; ch++) {
      sb.append((char) ch);
    }
    ASCII_CHARACTERS = sb.toString();
  }

  private static final String ALL_DIGITS;

  static {
    StringBuilder sb = new StringBuilder();
    String zeros =
        "0\u0660\u06f0\u07c0\u0966\u09e6\u0a66\u0ae6\u0b66\u0be6\u0c66"
            + "\u0ce6\u0d66\u0e50\u0ed0\u0f20\u1040\u1090\u17e0\u1810\u1946"
            + "\u19d0\u1b50\u1bb0\u1c40\u1c50\ua620\ua8d0\ua900\uaa50\uff10";
    for (char base : zeros.toCharArray()) {
      for (int offset = 0; offset < 10; offset++) {
        sb.append((char) (base + offset));
      }
    }
    ALL_DIGITS = sb.toString();
  }

  /** Sample CharMatcher instances for benchmarking. */
  public enum SampleMatcherConfig {
    WHITESPACE(CharMatcher.whitespace(), WHITESPACE_CHARACTERS),
    HASH(CharMatcher.is('#'), "#"),
    ASCII(CharMatcher.ascii(), ASCII_CHARACTERS),
    WESTERN_DIGIT("0123456789"),
    ALL_DIGIT(CharMatcher.digit(), ALL_DIGITS),
    OPS_5("+-*/%"),
    HEX_16(CharMatcher.inRange('0', '9').or(CharMatcher.inRange('A', 'F')), "0123456789ABCDEF"),
    HEX_22(
        CharMatcher.inRange('0', '9')
            .or(CharMatcher.inRange('A', 'F'))
            .or(CharMatcher.inRange('a', 'f')),
        "0123456789ABCDEFabcdef"),
    GERMAN_59(
        CharMatcher.inRange('a', 'z')
            .or(CharMatcher.inRange('A', 'Z'))
            .or(CharMatcher.anyOf("äöüßÄÖÜ")),
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZäöüßÄÖÜ");

    public final CharMatcher matcher;
    public final String matchingChars;

    SampleMatcherConfig(String matchingChars) {
      this(CharMatcher.anyOf(matchingChars), matchingChars);
    }

    SampleMatcherConfig(CharMatcher matcher, String matchingChars) {
      this.matcher = matcher;
      this.matchingChars = matchingChars;
    }
  }
}

/*
 * Copyright (C) 2006 The Guava Authors
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

package com.google.common.base;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;

/**
 * Utility class for converting between various ASCII case formats.
 *
 * @author Mike Bostock
 * @since 1.0
 */
@GwtCompatible
public enum CaseFormat {
  /**
   * Hyphenated variable naming convention, e.g., "lower-hyphen".
   */
  LOWER_HYPHEN(CharMatcher.is('-'), "-") {
    @Override String normalizeWord(String word) {
      return Ascii.toLowerCase(word);
    }
  },

  /**
   * C++ variable naming convention, e.g., "lower_underscore".
   */
  LOWER_UNDERSCORE(CharMatcher.is('_'), "_") {
    @Override String normalizeWord(String word) {
      return Ascii.toLowerCase(word);
    }
  },

  /**
   * Java variable naming convention, e.g., "lowerCamel".
   */
  LOWER_CAMEL(CharMatcher.inRange('A', 'Z'), "") {
    @Override String normalizeWord(String word) {
      return firstCharOnlyToUpper(word);
    }
  },

  /**
   * Java and C++ class naming convention, e.g., "UpperCamel".
   */
  UPPER_CAMEL(CharMatcher.inRange('A', 'Z'), "") {
    @Override String normalizeWord(String word) {
      return firstCharOnlyToUpper(word);
    }
  },

  /**
   * Java and C++ constant naming convention, e.g., "UPPER_UNDERSCORE".
   */
  UPPER_UNDERSCORE(CharMatcher.is('_'), "_") {
    @Override String normalizeWord(String word) {
      return Ascii.toUpperCase(word);
    }
  };

  private final CharMatcher wordBoundary;
  private final String wordSeparator;

  CaseFormat(CharMatcher wordBoundary, String wordSeparator) {
    this.wordBoundary = wordBoundary;
    this.wordSeparator = wordSeparator;
  }

  /**
   * Converts the specified {@code String s} from this format to the specified {@code format}. A
   * "best effort" approach is taken; if {@code s} does not conform to the assumed format, then the
   * behavior of this method is undefined but we make a reasonable effort at converting anyway.
   */
  public String to(CaseFormat format, String s) {
    checkNotNull(format);
    checkNotNull(s);

    if (format == this) {
      return s;
    }

    // TODO(user): Get rid of this switch and use enum methods instead?

    // optimize cases where no camel conversion is required
    switch (this) {
      case LOWER_HYPHEN:
        switch (format) {
          case LOWER_UNDERSCORE:
            return s.replace('-', '_');
          case UPPER_UNDERSCORE:
            return Ascii.toUpperCase(s.replace('-', '_'));
        }
        break;
      case LOWER_UNDERSCORE:
        switch (format) {
          case LOWER_HYPHEN:
            return s.replace('_', '-');
          case UPPER_UNDERSCORE:
            return Ascii.toUpperCase(s);
        }
        break;
      case UPPER_UNDERSCORE:
        switch (format) {
          case LOWER_HYPHEN:
            return Ascii.toLowerCase(s.replace('_', '-'));
          case LOWER_UNDERSCORE:
            return Ascii.toLowerCase(s);
        }
        break;
    }

    // otherwise, deal with camel conversion
    StringBuilder out = null;
    int i = 0;
    int j = -1;
    while ((j = wordBoundary.indexIn(s, ++j)) != -1) {
      if (i == 0) {
        // include some extra space for separators
        out = new StringBuilder(s.length() + 4 * wordSeparator.length());
        out.append(format.normalizeFirstWord(s.substring(i, j)));
      } else {
        out.append(format.normalizeWord(s.substring(i, j)));
      }
      out.append(format.wordSeparator);
      i = j + wordSeparator.length();
    }
    if (i == 0) {
      return format.normalizeFirstWord(s);
    }
    out.append(format.normalizeWord(s.substring(i)));
    return out.toString();
  }

  abstract String normalizeWord(String word);

  private String normalizeFirstWord(String word) {
    return (this == LOWER_CAMEL) ? Ascii.toLowerCase(word) : normalizeWord(word);
  }

  private static String firstCharOnlyToUpper(String word) {
    return (word.isEmpty())
        ? word
        : new StringBuilder(word.length())
            .append(Ascii.toUpperCase(word.charAt(0)))
            .append(Ascii.toLowerCase(word.substring(1)))
            .toString();
  }
}

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

package com.google.common.escape;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.util.Map;
import javax.annotation.CheckForNull;

/**
 * A {@link CharEscaper} that uses an array to quickly look up replacement characters for a given
 * {@code char} value. An additional safe range is provided that determines whether {@code char}
 * values without specific replacements are to be considered safe and left unescaped or should be
 * escaped in a general way.
 *
 * <p>A good example of usage of this class is for Java source code escaping where the replacement
 * array contains information about special ASCII characters such as {@code \\t} and {@code \\n}
 * while {@link #escapeUnsafe} is overridden to handle general escaping of the form {@code \\uxxxx}.
 *
 * <p>The size of the data structure used by {@link ArrayBasedCharEscaper} is proportional to the
 * highest valued character that requires escaping. For example a replacement map containing the
 * single character '{@code \}{@code u1000}' will require approximately 16K of memory. If you need
 * to create multiple escaper instances that have the same character replacement mapping consider
 * using {@link ArrayBasedEscaperMap}.
 *
 * @author Sven Mawson
 * @author David Beaumont
 * @since 15.0
 */
@Beta
@GwtCompatible
@ElementTypesAreNonnullByDefault
public abstract class ArrayBasedCharEscaper extends CharEscaper {
  // The replacement array (see ArrayBasedEscaperMap).
  private final char[][] replacements;
  // The number of elements in the replacement array.
  private final int replacementsLength;
  // The first character in the safe range.
  private final char safeMin;
  // The last character in the safe range.
  private final char safeMax;

  /**
   * Creates a new ArrayBasedCharEscaper instance with the given replacement map and specified safe
   * range. If {@code safeMax < safeMin} then no characters are considered safe.
   *
   * <p>If a character has no mapped replacement then it is checked against the safe range. If it
   * lies outside that, then {@link #escapeUnsafe} is called, otherwise no escaping is performed.
   *
   * @param replacementMap a map of characters to their escaped representations
   * @param safeMin the lowest character value in the safe range
   * @param safeMax the highest character value in the safe range
   */
  protected ArrayBasedCharEscaper(
      Map<Character, String> replacementMap, char safeMin, char safeMax) {

    this(ArrayBasedEscaperMap.create(replacementMap), safeMin, safeMax);
  }

  /**
   * Creates a new ArrayBasedCharEscaper instance with the given replacement map and specified safe
   * range. If {@code safeMax < safeMin} then no characters are considered safe. This initializer is
   * useful when explicit instances of ArrayBasedEscaperMap are used to allow the sharing of large
   * replacement mappings.
   *
   * <p>If a character has no mapped replacement then it is checked against the safe range. If it
   * lies outside that, then {@link #escapeUnsafe} is called, otherwise no escaping is performed.
   *
   * @param escaperMap the mapping of characters to be escaped
   * @param safeMin the lowest character value in the safe range
   * @param safeMax the highest character value in the safe range
   */
  protected ArrayBasedCharEscaper(ArrayBasedEscaperMap escaperMap, char safeMin, char safeMax) {

    checkNotNull(escaperMap); // GWT specific check (do not optimize)
    this.replacements = escaperMap.getReplacementArray();
    this.replacementsLength = replacements.length;
    if (safeMax < safeMin) {
      // If the safe range is empty, set the range limits to opposite extremes
      // to ensure the first test of either value will (almost certainly) fail.
      safeMax = Character.MIN_VALUE;
      safeMin = Character.MAX_VALUE;
    }
    this.safeMin = safeMin;
    this.safeMax = safeMax;
  }

  /*
   * This is overridden to improve performance. Rough benchmarking shows that this almost doubles
   * the speed when processing strings that do not require any escaping.
   */
  @Override
  public final String escape(String s) {
    checkNotNull(s); // GWT specific check (do not optimize).
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if ((c < replacementsLength && replacements[c] != null) || c > safeMax || c < safeMin) {
        return escapeSlow(s, i);
      }
    }
    return s;
  }

  /**
   * Escapes a single character using the replacement array and safe range values. If the given
   * character does not have an explicit replacement and lies outside the safe range then {@link
   * #escapeUnsafe} is called.
   *
   * @return the replacement characters, or {@code null} if no escaping was required
   */
  @Override
  @CheckForNull
  protected final char[] escape(char c) {
    if (c < replacementsLength) {
      char[] chars = replacements[c];
      if (chars != null) {
        return chars;
      }
    }
    if (c >= safeMin && c <= safeMax) {
      return null;
    }
    return escapeUnsafe(c);
  }

  /**
   * Escapes a {@code char} value that has no direct explicit value in the replacement array and
   * lies outside the stated safe range. Subclasses should override this method to provide
   * generalized escaping for characters.
   *
   * <p>Note that arrays returned by this method must not be modified once they have been returned.
   * However it is acceptable to return the same array multiple times (even for different input
   * characters).
   *
   * @param c the character to escape
   * @return the replacement characters, or {@code null} if no escaping was required
   */
  // TODO(dbeaumont,cpovirk): Rename this something better once refactoring done
  @CheckForNull
  protected abstract char[] escapeUnsafe(char c);
}

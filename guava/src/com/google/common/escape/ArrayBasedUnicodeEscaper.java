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

import com.google.common.annotations.GwtCompatible;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link UnicodeEscaper} that uses an array to quickly look up replacement characters for a given
 * code point. An additional safe range is provided that determines whether code points without
 * specific replacements are to be considered safe and left unescaped or should be escaped in a
 * general way.
 *
 * <p>A good example of usage of this class is for HTML escaping where the replacement array
 * contains information about the named HTML entities such as {@code &amp;} and {@code &quot;} while
 * {@link #escapeUnsafe} is overridden to handle general escaping of the form {@code &#NNNNN;}.
 *
 * <p>The size of the data structure used by {@link ArrayBasedUnicodeEscaper} is proportional to the
 * highest valued code point that requires escaping. For example a replacement map containing the
 * single character '{@code \}{@code u1000}' will require approximately 16K of memory. If you need
 * to create multiple escaper instances that have the same character replacement mapping consider
 * using {@link ArrayBasedEscaperMap}.
 *
 * @author David Beaumont
 * @since 15.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public abstract class ArrayBasedUnicodeEscaper extends UnicodeEscaper {
  // The replacement array (see ArrayBasedEscaperMap).
  private final char[][] replacements;
  // The number of elements in the replacement array.
  private final int replacementsLength;
  // The first code point in the safe range.
  private final int safeMin;
  // The last code point in the safe range.
  private final int safeMax;

  // Cropped values used in the fast path range checks.
  private final char safeMinChar;
  private final char safeMaxChar;

  /**
   * Creates a new ArrayBasedUnicodeEscaper instance with the given replacement map and specified
   * safe range. If {@code safeMax < safeMin} then no code points are considered safe.
   *
   * <p>If a code point has no mapped replacement then it is checked against the safe range. If it
   * lies outside that, then {@link #escapeUnsafe} is called, otherwise no escaping is performed.
   *
   * @param replacementMap a map of characters to their escaped representations
   * @param safeMin the lowest character value in the safe range
   * @param safeMax the highest character value in the safe range
   * @param unsafeReplacement the default replacement for unsafe characters or null if no default
   *     replacement is required
   */
  protected ArrayBasedUnicodeEscaper(
      Map<Character, String> replacementMap,
      int safeMin,
      int safeMax,
      @Nullable String unsafeReplacement) {
    this(ArrayBasedEscaperMap.create(replacementMap), safeMin, safeMax, unsafeReplacement);
  }

  /**
   * Creates a new ArrayBasedUnicodeEscaper instance with the given replacement map and specified
   * safe range. If {@code safeMax < safeMin} then no code points are considered safe. This
   * initializer is useful when explicit instances of ArrayBasedEscaperMap are used to allow the
   * sharing of large replacement mappings.
   *
   * <p>If a code point has no mapped replacement then it is checked against the safe range. If it
   * lies outside that, then {@link #escapeUnsafe} is called, otherwise no escaping is performed.
   *
   * @param escaperMap the map of replacements
   * @param safeMin the lowest character value in the safe range
   * @param safeMax the highest character value in the safe range
   * @param unsafeReplacement the default replacement for unsafe characters or null if no default
   *     replacement is required
   */
  protected ArrayBasedUnicodeEscaper(
      ArrayBasedEscaperMap escaperMap,
      int safeMin,
      int safeMax,
      @Nullable String unsafeReplacement) {
    checkNotNull(escaperMap); // GWT specific check (do not optimize)
    this.replacements = escaperMap.getReplacementArray();
    this.replacementsLength = replacements.length;
    if (safeMax < safeMin) {
      // If the safe range is empty, set the range limits to opposite extremes
      // to ensure the first test of either value will fail.
      safeMax = -1;
      safeMin = Integer.MAX_VALUE;
    }
    this.safeMin = safeMin;
    this.safeMax = safeMax;

    // This is a bit of a hack but lets us do quicker per-character checks in
    // the fast path code. The safe min/max values are very unlikely to extend
    // into the range of surrogate characters, but if they do we must not test
    // any values in that range. To see why, consider the case where:
    // safeMin <= {hi,lo} <= safeMax
    // where {hi,lo} are characters forming a surrogate pair such that:
    // codePointOf(hi, lo) > safeMax
    // which would result in the surrogate pair being (wrongly) considered safe.
    // If we clip the safe range used during the per-character tests so it is
    // below the values of characters in surrogate pairs, this cannot occur.
    // This approach does mean that we break out of the fast path code in cases
    // where we don't strictly need to, but this situation will almost never
    // occur in practice.
    if (safeMin >= Character.MIN_HIGH_SURROGATE) {
      // The safe range is empty or the all safe code points lie in or above the
      // surrogate range. Either way the character range is empty.
      this.safeMinChar = Character.MAX_VALUE;
      this.safeMaxChar = 0;
    } else {
      // The safe range is non-empty and contains values below the surrogate
      // range but may extend above it. We may need to clip the maximum value.
      this.safeMinChar = (char) safeMin;
      this.safeMaxChar = (char) Math.min(safeMax, Character.MIN_HIGH_SURROGATE - 1);
    }
  }

  /*
   * This is overridden to improve performance. Rough benchmarking shows that this almost doubles
   * the speed when processing strings that do not require any escaping.
   */
  @Override
  public final String escape(String s) {
    checkNotNull(s); // GWT specific check (do not optimize)
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if ((c < replacementsLength && replacements[c] != null)
          || c > safeMaxChar
          || c < safeMinChar) {
        return escapeSlow(s, i);
      }
    }
    return s;
  }

  /**
   * Escapes a single Unicode code point using the replacement array and safe range values. If the
   * given character does not have an explicit replacement and lies outside the safe range then
   * {@link #escapeUnsafe} is called.
   *
   * @return the replacement characters, or {@code null} if no escaping was required
   */
  @Override
  @CheckForNull
  protected final char[] escape(int cp) {
    if (cp < replacementsLength) {
      char[] chars = replacements[cp];
      if (chars != null) {
        return chars;
      }
    }
    if (cp >= safeMin && cp <= safeMax) {
      return null;
    }
    return escapeUnsafe(cp);
  }

  /* Overridden for performance. */
  @Override
  protected final int nextEscapeIndex(CharSequence csq, int index, int end) {
    while (index < end) {
      char c = csq.charAt(index);
      if ((c < replacementsLength && replacements[c] != null)
          || c > safeMaxChar
          || c < safeMinChar) {
        break;
      }
      index++;
    }
    return index;
  }

  /**
   * Escapes a code point that has no direct explicit value in the replacement array and lies
   * outside the stated safe range. Subclasses should override this method to provide generalized
   * escaping for code points if required.
   *
   * <p>Note that arrays returned by this method must not be modified once they have been returned.
   * However it is acceptable to return the same array multiple times (even for different input
   * characters).
   *
   * @param cp the Unicode code point to escape
   * @return the replacement characters, or {@code null} if no escaping was required
   */
  @CheckForNull
  protected abstract char[] escapeUnsafe(int cp);
}

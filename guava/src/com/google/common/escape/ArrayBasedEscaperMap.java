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
import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.Map;

/**
 * An implementation-specific parameter class suitable for initializing {@link
 * ArrayBasedCharEscaper} or {@link ArrayBasedUnicodeEscaper} instances. This class should be used
 * when more than one escaper is created using the same character replacement mapping to allow the
 * underlying (implementation specific) data structures to be shared.
 *
 * <p>The size of the data structure used by ArrayBasedCharEscaper and ArrayBasedUnicodeEscaper is
 * proportional to the highest valued character that has a replacement. For example a replacement
 * map containing the single character '{@literal \}u1000' will require approximately 16K of memory.
 * As such sharing this data structure between escaper instances is the primary goal of this class.
 *
 * @author David Beaumont
 * @since 15.0
 */
@Beta
@GwtCompatible
@ElementTypesAreNonnullByDefault
public final class ArrayBasedEscaperMap {
  /**
   * Returns a new ArrayBasedEscaperMap for creating ArrayBasedCharEscaper or
   * ArrayBasedUnicodeEscaper instances.
   *
   * @param replacements a map of characters to their escaped representations
   */
  public static ArrayBasedEscaperMap create(Map<Character, String> replacements) {
    return new ArrayBasedEscaperMap(createReplacementArray(replacements));
  }

  // The underlying replacement array we can share between multiple escaper
  // instances.
  private final char[][] replacementArray;

  private ArrayBasedEscaperMap(char[][] replacementArray) {
    this.replacementArray = replacementArray;
  }

  // Returns the non-null array of replacements for fast lookup.
  char[][] getReplacementArray() {
    return replacementArray;
  }

  // Creates a replacement array from the given map. The returned array is a
  // linear lookup table of replacement character sequences indexed by the
  // original character value.
  @VisibleForTesting
  static char[][] createReplacementArray(Map<Character, String> map) {
    checkNotNull(map); // GWT specific check (do not optimize)
    if (map.isEmpty()) {
      return EMPTY_REPLACEMENT_ARRAY;
    }
    char max = Collections.max(map.keySet());
    char[][] replacements = new char[max + 1][];
    for (Character c : map.keySet()) {
      replacements[c] = map.get(c).toCharArray();
    }
    return replacements;
  }

  // Immutable empty array for when there are no replacements.
  private static final char[][] EMPTY_REPLACEMENT_ARRAY = new char[0][0];
}

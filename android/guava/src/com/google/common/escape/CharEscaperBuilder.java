/*
 * Copyright (C) 2006 The Guava Authors
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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Simple helper class to build a "sparse" array of objects based on the indexes that were added to
 * it. The array will be from 0 to the maximum index given. All non-set indexes will contain null
 * (so it's not really a sparse array, just a pseudo sparse array). The builder can also return a
 * CharEscaper based on the generated array.
 *
 * @author Sven Mawson
 * @since 15.0
 */
@Beta
@GwtCompatible
public final class CharEscaperBuilder {
  /**
   * Simple decorator that turns an array of replacement char[]s into a CharEscaper, this results in
   * a very fast escape method.
   */
  private static class CharArrayDecorator extends CharEscaper {
    private final char[][] replacements;
    private final int replaceLength;

    CharArrayDecorator(char[][] replacements) {
      this.replacements = replacements;
      this.replaceLength = replacements.length;
    }

    /*
     * Overriding escape method to be slightly faster for this decorator. We test the replacements
     * array directly, saving a method call.
     */
    @Override
    public String escape(String s) {
      int slen = s.length();
      for (int index = 0; index < slen; index++) {
        char c = s.charAt(index);
        if (c < replacements.length && replacements[c] != null) {
          return escapeSlow(s, index);
        }
      }
      return s;
    }

    @Override
    protected char[] escape(char c) {
      return c < replaceLength ? replacements[c] : null;
    }
  }

  // Replacement mappings.
  private final Map<Character, String> map;

  // The highest index we've seen so far.
  private int max = -1;

  /** Construct a new sparse array builder. */
  public CharEscaperBuilder() {
    this.map = new HashMap<>();
  }

  /** Add a new mapping from an index to an object to the escaping. */
  @CanIgnoreReturnValue
  public CharEscaperBuilder addEscape(char c, String r) {
    map.put(c, checkNotNull(r));
    if (c > max) {
      max = c;
    }
    return this;
  }

  /** Add multiple mappings at once for a particular index. */
  @CanIgnoreReturnValue
  public CharEscaperBuilder addEscapes(char[] cs, String r) {
    checkNotNull(r);
    for (char c : cs) {
      addEscape(c, r);
    }
    return this;
  }

  /**
   * Convert this builder into an array of char[]s where the maximum index is the value of the
   * highest character that has been seen. The array will be sparse in the sense that any unseen
   * index will default to null.
   *
   * @return a "sparse" array that holds the replacement mappings.
   */
  public char[][] toArray() {
    char[][] result = new char[max + 1][];
    for (Entry<Character, String> entry : map.entrySet()) {
      result[entry.getKey()] = entry.getValue().toCharArray();
    }
    return result;
  }

  /**
   * Convert this builder into a char escaper which is just a decorator around the underlying array
   * of replacement char[]s.
   *
   * @return an escaper that escapes based on the underlying array.
   */
  public Escaper toEscaper() {
    return new CharArrayDecorator(toArray());
  }
}

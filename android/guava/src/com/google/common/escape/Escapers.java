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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Static utility methods pertaining to {@link Escaper} instances.
 *
 * @author Sven Mawson
 * @author David Beaumont
 * @since 15.0
 */
@GwtCompatible
public final class Escapers {
  private Escapers() {}

  /**
   * Returns an {@link Escaper} that does no escaping, passing all character data through unchanged.
   */
  public static Escaper nullEscaper() {
    return NULL_ESCAPER;
  }

  // An Escaper that efficiently performs no escaping.
  // Extending CharEscaper (instead of Escaper) makes Escapers.compose() easier.
  private static final Escaper NULL_ESCAPER =
      new CharEscaper() {
        @Override
        public String escape(String string) {
          return checkNotNull(string);
        }

        @Override
        protected char @Nullable [] escape(char c) {
          // TODO: Fix tests not to call this directly and make it throw an error.
          return null;
        }
      };

  /**
   * Returns a builder for creating simple, fast escapers. A builder instance can be reused and each
   * escaper that is created will be a snapshot of the current builder state. Builders are not
   * thread safe.
   *
   * <p>The initial state of the builder is such that:
   *
   * <ul>
   *   <li>There are no replacement mappings
   *   <li>{@code safeMin == Character.MIN_VALUE}
   *   <li>{@code safeMax == Character.MAX_VALUE}
   *   <li>{@code unsafeReplacement == null}
   * </ul>
   *
   * <p>For performance reasons escapers created by this builder are not Unicode aware and will not
   * validate the well-formedness of their input.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for simple, fast escapers.
   *
   * <p>Typically an escaper needs to deal with the escaping of high valued characters or code
   * points. In these cases it is necessary to extend either {@link ArrayBasedCharEscaper} or {@link
   * ArrayBasedUnicodeEscaper} to provide the desired behavior. However this builder is suitable for
   * creating escapers that replace a relative small set of characters.
   *
   * @author David Beaumont
   * @since 15.0
   */
  public static final class Builder {
    private final Map<Character, String> replacementMap = new HashMap<>();
    private char safeMin = Character.MIN_VALUE;
    private char safeMax = Character.MAX_VALUE;
    private @Nullable String unsafeReplacement = null;

    // The constructor is exposed via the builder() method above.
    private Builder() {}

    /**
     * Sets the safe range of characters for the escaper. Characters in this range that have no
     * explicit replacement are considered 'safe' and remain unescaped in the output. If {@code
     * safeMax < safeMin} then the safe range is empty.
     *
     * @param safeMin the lowest 'safe' character
     * @param safeMax the highest 'safe' character
     * @return the builder instance
     */
    @CanIgnoreReturnValue
    public Builder setSafeRange(char safeMin, char safeMax) {
      this.safeMin = safeMin;
      this.safeMax = safeMax;
      return this;
    }

    /**
     * Sets the replacement string for any characters outside the 'safe' range that have no explicit
     * replacement. If {@code unsafeReplacement} is {@code null} then no replacement will occur, if
     * it is {@code ""} then the unsafe characters are removed from the output.
     *
     * @param unsafeReplacement the string to replace unsafe characters
     * @return the builder instance
     */
    @CanIgnoreReturnValue
    public Builder setUnsafeReplacement(@Nullable String unsafeReplacement) {
      this.unsafeReplacement = unsafeReplacement;
      return this;
    }

    /**
     * Adds a replacement string for the given input character. The specified character will be
     * replaced by the given string whenever it occurs in the input, irrespective of whether it lies
     * inside or outside the 'safe' range.
     *
     * @param c the character to be replaced
     * @param replacement the string to replace the given character
     * @return the builder instance
     * @throws NullPointerException if {@code replacement} is null
     */
    @CanIgnoreReturnValue
    public Builder addEscape(char c, String replacement) {
      checkNotNull(replacement);
      // This can replace an existing character (the builder is re-usable).
      replacementMap.put(c, replacement);
      return this;
    }

    /** Returns a new escaper based on the current state of the builder. */
    public Escaper build() {
      return new ArrayBasedCharEscaper(replacementMap, safeMin, safeMax) {
        private final char @Nullable [] replacementChars =
            unsafeReplacement != null ? unsafeReplacement.toCharArray() : null;

        @Override
        protected char @Nullable [] escapeUnsafe(char c) {
          return replacementChars;
        }
      };
    }
  }

  /**
   * Returns a string that would replace the given character in the specified escaper, or {@code
   * null} if no replacement should be made. This method is intended for use in tests through the
   * {@code EscaperAsserts} class; production users of {@link CharEscaper} should limit themselves
   * to its public interface.
   *
   * @param c the character to escape if necessary
   * @return the replacement string, or {@code null} if no escaping was needed
   */
  public static @Nullable String computeReplacement(CharEscaper escaper, char c) {
    return stringOrNull(escaper.escape(c));
  }

  /**
   * Returns a string that would replace the given character in the specified escaper, or {@code
   * null} if no replacement should be made. This method is intended for use in tests through the
   * {@code EscaperAsserts} class; production users of {@link UnicodeEscaper} should limit
   * themselves to its public interface.
   *
   * @param cp the Unicode code point to escape if necessary
   * @return the replacement string, or {@code null} if no escaping was needed
   */
  public static @Nullable String computeReplacement(UnicodeEscaper escaper, int cp) {
    return stringOrNull(escaper.escape(cp));
  }

  private static @Nullable String stringOrNull(char @Nullable [] in) {
    return (in == null) ? null : new String(in);
  }
}

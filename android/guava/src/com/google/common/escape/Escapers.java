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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Static utility methods pertaining to {@link Escaper} instances.
 *
 * @author Sven Mawson
 * @author David Beaumont
 * @since 15.0
 */
@Beta
@GwtCompatible
@ElementTypesAreNonnullByDefault
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
        @CheckForNull
        protected char[] escape(char c) {
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
  @Beta
  public static final class Builder {
    private final Map<Character, String> replacementMap = new HashMap<>();
    private char safeMin = Character.MIN_VALUE;
    private char safeMax = Character.MAX_VALUE;
    @CheckForNull private String unsafeReplacement = null;

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
        @CheckForNull
        private final char[] replacementChars =
            unsafeReplacement != null ? unsafeReplacement.toCharArray() : null;

        @Override
        @CheckForNull
        protected char[] escapeUnsafe(char c) {
          return replacementChars;
        }
      };
    }
  }

  /**
   * Returns a {@link UnicodeEscaper} equivalent to the given escaper instance. If the escaper is
   * already a UnicodeEscaper then it is simply returned, otherwise it is wrapped in a
   * UnicodeEscaper.
   *
   * <p>When a {@link CharEscaper} escaper is wrapped by this method it acquires extra behavior with
   * respect to the well-formedness of Unicode character sequences and will throw {@link
   * IllegalArgumentException} when given bad input.
   *
   * @param escaper the instance to be wrapped
   * @return a UnicodeEscaper with the same behavior as the given instance
   * @throws NullPointerException if escaper is null
   * @throws IllegalArgumentException if escaper is not a UnicodeEscaper or a CharEscaper
   */
  static UnicodeEscaper asUnicodeEscaper(Escaper escaper) {
    checkNotNull(escaper);
    if (escaper instanceof UnicodeEscaper) {
      return (UnicodeEscaper) escaper;
    } else if (escaper instanceof CharEscaper) {
      return wrap((CharEscaper) escaper);
    }
    // In practice this shouldn't happen because it would be very odd not to
    // extend either CharEscaper or UnicodeEscaper for non trivial cases.
    throw new IllegalArgumentException(
        "Cannot create a UnicodeEscaper from: " + escaper.getClass().getName());
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
  @CheckForNull
  public static String computeReplacement(CharEscaper escaper, char c) {
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
  @CheckForNull
  public static String computeReplacement(UnicodeEscaper escaper, int cp) {
    return stringOrNull(escaper.escape(cp));
  }

  @CheckForNull
  private static String stringOrNull(@CheckForNull char[] in) {
    return (in == null) ? null : new String(in);
  }

  /** Private helper to wrap a CharEscaper as a UnicodeEscaper. */
  private static UnicodeEscaper wrap(final CharEscaper escaper) {
    return new UnicodeEscaper() {
      @Override
      @CheckForNull
      protected char[] escape(int cp) {
        // If a code point maps to a single character, just escape that.
        if (cp < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
          return escaper.escape((char) cp);
        }
        // Convert the code point to a surrogate pair and escape them both.
        // Note: This code path is horribly slow and typically allocates 4 new
        // char[] each time it is invoked. However this avoids any
        // synchronization issues and makes the escaper thread safe.
        char[] surrogateChars = new char[2];
        Character.toChars(cp, surrogateChars, 0);
        char[] hiChars = escaper.escape(surrogateChars[0]);
        char[] loChars = escaper.escape(surrogateChars[1]);

        // If either hiChars or lowChars are non-null, the CharEscaper is trying
        // to escape the characters of a surrogate pair separately. This is
        // uncommon and applies only to escapers that assume UCS-2 rather than
        // UTF-16. See: http://en.wikipedia.org/wiki/UTF-16/UCS-2
        if (hiChars == null && loChars == null) {
          // We expect this to be the common code path for most escapers.
          return null;
        }
        // Combine the characters and/or escaped sequences into a single array.
        int hiCount = hiChars != null ? hiChars.length : 1;
        int loCount = loChars != null ? loChars.length : 1;
        char[] output = new char[hiCount + loCount];
        if (hiChars != null) {
          // TODO: Is this faster than System.arraycopy() for small arrays?
          for (int n = 0; n < hiChars.length; ++n) {
            output[n] = hiChars[n];
          }
        } else {
          output[0] = surrogateChars[0];
        }
        if (loChars != null) {
          for (int n = 0; n < loChars.length; ++n) {
            output[hiCount + n] = loChars[n];
          }
        } else {
          output[hiCount] = surrogateChars[1];
        }
        return output;
      }
    };
  }
}

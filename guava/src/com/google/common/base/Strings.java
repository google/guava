/*
 * Copyright (C) 2010 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;

import java.util.Formatter;

import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to {@code String} or {@code CharSequence}
 * instances.
 *
 * @author Kevin Bourrillion
 * @since 3.0
 */
@GwtCompatible
public final class Strings {
  private Strings() {}

  /**
   * Returns the given string if it is non-null; the empty string otherwise.
   *
   * @param string the string to test and possibly return
   * @return {@code string} itself if it is non-null; {@code ""} if it is null
   */
  public static String nullToEmpty(@Nullable String string) {
    return (string == null) ? "" : string;
  }

  /**
   * Returns the given string if it is nonempty; {@code null} otherwise.
   *
   * @param string the string to test and possibly return
   * @return {@code string} itself if it is nonempty; {@code null} if it is
   *     empty or null
   */
  public static @Nullable String emptyToNull(@Nullable String string) {
    return isNullOrEmpty(string) ? null : string;
  }

  /**
   * Returns {@code true} if the given string is null or is the empty string.
   *
   * <p>Consider normalizing your string references with {@link #nullToEmpty}.
   * If you do, you can use {@link String#isEmpty()} instead of this
   * method, and you won't need special null-safe forms of methods like {@link
   * String#toUpperCase} either. Or, if you'd like to normalize "in the other
   * direction," converting empty strings to {@code null}, you can use {@link
   * #emptyToNull}.
   *
   * @param string a string reference to check
   * @return {@code true} if the string is null or is the empty string
   */
  public static boolean isNullOrEmpty(@Nullable String string) {
    return string == null || string.length() == 0; // string.isEmpty() in Java 6
  }

  /**
   * Returns a string, of length at least {@code minLength}, consisting of
   * {@code string} prepended with as many copies of {@code padChar} as are
   * necessary to reach that length. For example,
   *
   * <ul>
   * <li>{@code padStart("7", 3, '0')} returns {@code "007"}
   * <li>{@code padStart("2010", 3, '0')} returns {@code "2010"}
   * </ul>
   *
   * <p>See {@link Formatter} for a richer set of formatting capabilities.
   *
   * @param string the string which should appear at the end of the result
   * @param minLength the minimum length the resulting string must have. Can be
   *     zero or negative, in which case the input string is always returned.
   * @param padChar the character to insert at the beginning of the result until
   *     the minimum length is reached
   * @return the padded string
   */
  public static String padStart(String string, int minLength, char padChar) {
    checkNotNull(string);  // eager for GWT.
    if (string.length() >= minLength) {
      return string;
    }
    StringBuilder sb = new StringBuilder(minLength);
    for (int i = string.length(); i < minLength; i++) {
      sb.append(padChar);
    }
    sb.append(string);
    return sb.toString();
  }

  /**
   * Returns a string, of length at least {@code minLength}, consisting of
   * {@code string} appended with as many copies of {@code padChar} as are
   * necessary to reach that length. For example,
   *
   * <ul>
   * <li>{@code padEnd("4.", 5, '0')} returns {@code "4.000"}
   * <li>{@code padEnd("2010", 3, '!')} returns {@code "2010"}
   * </ul>
   *
   * <p>See {@link Formatter} for a richer set of formatting capabilities.
   *
   * @param string the string which should appear at the beginning of the result
   * @param minLength the minimum length the resulting string must have. Can be
   *     zero or negative, in which case the input string is always returned.
   * @param padChar the character to append to the end of the result until the
   *     minimum length is reached
   * @return the padded string
   */
  public static String padEnd(String string, int minLength, char padChar) {
    checkNotNull(string);  // eager for GWT.
    if (string.length() >= minLength) {
      return string;
    }
    StringBuilder sb = new StringBuilder(minLength);
    sb.append(string);
    for (int i = string.length(); i < minLength; i++) {
      sb.append(padChar);
    }
    return sb.toString();
  }

  /**
   * Returns a string consisting of a specific number of concatenated copies of
   * an input string. For example, {@code repeat("hey", 3)} returns the string
   * {@code "heyheyhey"}.
   *
   * @param string any non-null string
   * @param count the number of times to repeat it; a nonnegative integer
   * @return a string containing {@code string} repeated {@code count} times
   *     (the empty string if {@code count} is zero)
   * @throws IllegalArgumentException if {@code count} is negative
   */
  public static String repeat(String string, int count) {
    checkNotNull(string);  // eager for GWT.
    checkArgument(count >= 0, "invalid count: %s", count);

    // If this multiplication overflows, a NegativeArraySizeException or
    // OutOfMemoryError is not far behind
    StringBuilder builder = new StringBuilder(string.length() * count);
    for (int i = 0; i < count; i++) {
      builder.append(string);
    }
    return builder.toString();
  }
}

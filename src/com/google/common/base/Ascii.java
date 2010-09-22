/*
 * Copyright (C) 2010 Google Inc.
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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/**
 * Static methods pertaining ASCII characters (those in the range of values {@code 0x00} through
 * {@code 0x7F}), and to strings containing such characters.
 *
 * @author Craig Berry
 * @author Gregory Kick
 * @since 7
 */
@Beta
@GwtCompatible
public final class Ascii {

  private Ascii() {}

  /**
   * Returns a copy of the input string in which all {@linkplain #isUpperCase(char) uppercase ASCII
   * characters} have been converted to lowercase. All other characters are copied without
   * modification.
   */
  public static String toLowerCase(String string) {
    int length = string.length();
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(toLowerCase(string.charAt(i)));
    }
    return builder.toString();
  }

  /**
   * If the argument is an {@linkplain #isUpperCase(char) uppercase ASCII character} returns the
   * lowercase equivalent. Otherwise returns the argument.
   */
  public static char toLowerCase(char c) {
    return isUpperCase(c) ? (char) (c ^ 0x20) : c;
  }

  /**
   * Returns a copy of the input string in which all {@linkplain #isLowerCase(char) lowercase ASCII
   * characters} have been converted to uppercase. All other characters are copied without
   * modification.
   */
  public static String toUpperCase(String string) {
    int length = string.length();
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(toUpperCase(string.charAt(i)));
    }
    return builder.toString();
  }

  /**
   * If the argument is a {@linkplain #isLowerCase(char) lowercase ASCII character} returns the
   * uppercase equivalent. Otherwise returns the argument.
   */
  public static char toUpperCase(char c) {
    return isLowerCase(c) ? (char) (c & 0x5f) : c;
  }

  /**
   * Indicates whether {@code c} is one of the twenty-six lowercase ASCII alphabetic characters
   * between {@code 'a'} and {@code 'z'} inclusive. All others (including non-ASCII characters)
   * return {@code false}.
   */
  public static boolean isLowerCase(char c) {
    return (c >= 'a') && (c <= 'z');
  }

  /**
   * Indicates whether {@code c} is one of the twenty-six uppercase ASCII alphabetic characters
   * between {@code 'A'} and {@code 'Z'} inclusive. All others (including non-ASCII characters)
   * return {@code false}.
   */
  public static boolean isUpperCase(char c) {
    return (c >= 'A') && (c <= 'Z');
  }
}

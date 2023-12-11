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

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;

/**
 * Methods factored out so that they can be emulated differently in GWT.
 *
 * @author Jesse Wilson
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
final class Platform {
  private static final PatternCompiler patternCompiler = loadPatternCompiler();

  private Platform() {}

  static CharMatcher precomputeCharMatcher(CharMatcher matcher) {
    return matcher.precomputedInternal();
  }

  static <T extends Enum<T>> Optional<T> getEnumIfPresent(Class<T> enumClass, String value) {
    WeakReference<? extends Enum<?>> ref = Enums.getEnumConstants(enumClass).get(value);
    return ref == null ? Optional.<T>absent() : Optional.of(enumClass.cast(ref.get()));
  }

  static String formatCompact4Digits(double value) {
    return String.format(Locale.ROOT, "%.4g", value);
  }

  static boolean stringIsNullOrEmpty(@CheckForNull String string) {
    return string == null || string.isEmpty();
  }

  /**
   * Returns the string if it is not null, or an empty string otherwise.
   *
   * @param string the string to test and possibly return
   * @return {@code string} if it is not null; {@code ""} otherwise
   */
  static String nullToEmpty(@CheckForNull String string) {
    return (string == null) ? "" : string;
  }

  /**
   * Returns the string if it is not empty, or a null string otherwise.
   *
   * @param string the string to test and possibly return
   * @return {@code string} if it is not empty; {@code null} otherwise
   */
  @CheckForNull
  static String emptyToNull(@CheckForNull String string) {
    return stringIsNullOrEmpty(string) ? null : string;
  }

  static CommonPattern compilePattern(String pattern) {
    Preconditions.checkNotNull(pattern);
    return patternCompiler.compile(pattern);
  }

  static boolean patternCompilerIsPcreLike() {
    return patternCompiler.isPcreLike();
  }

  private static PatternCompiler loadPatternCompiler() {
    /*
     * We'd normally use ServiceLoader here, but it hurts Android startup performance. To avoid
     * that, we hardcode the JDK Pattern compiler on Android (and, inadvertently, on App Engine and
     * in Guava, at least for now).
     */
    return new JdkPatternCompiler();
  }

  private static final class JdkPatternCompiler implements PatternCompiler {
    @Override
    public CommonPattern compile(String pattern) {
      return new JdkPattern(Pattern.compile(pattern));
    }

    @Override
    public boolean isPcreLike() {
      return true;
    }
  }
}

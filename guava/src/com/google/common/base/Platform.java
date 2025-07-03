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
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Methods factored out so that they can be emulated differently in GWT.
 *
 * @author Jesse Wilson
 */
@GwtCompatible(emulated = true)
final class Platform {
  private static final Logger logger = Logger.getLogger(Platform.class.getName());
  private static final PatternCompiler patternCompiler = loadPatternCompiler();

  private Platform() {}

  static CharMatcher precomputeCharMatcher(CharMatcher matcher) {
    return matcher.precomputedInternal();
  }

  static <T extends Enum<T>> Optional<T> getEnumIfPresent(Class<T> enumClass, String value) {
    WeakReference<? extends Enum<?>> ref = Enums.getEnumConstants(enumClass).get(value);
    /*
     * We use `fromNullable` instead of `of` because `WeakReference.get()` has a nullable return
     * type.
     *
     * In practice, we are very unlikely to see `null`: The `WeakReference` to the enum constant
     * won't be cleared as long as the enum constant is referenced somewhere, and the enum constant
     * is referenced somewhere for as long as the enum class is loaded. *Maybe in theory* the enum
     * class could be unloaded after the above call to `getEnumConstants` but before we call
     * `get()`, but that is vanishingly unlikely.
     */
    return ref == null ? Optional.absent() : Optional.fromNullable(enumClass.cast(ref.get()));
  }

  static String formatCompact4Digits(double value) {
    return String.format(Locale.ROOT, "%.4g", value);
  }

  static boolean stringIsNullOrEmpty(@Nullable String string) {
    return string == null || string.isEmpty();
  }

  /**
   * Returns the string if it is not null, or an empty string otherwise.
   *
   * @param string the string to test and possibly return
   * @return {@code string} if it is not null; {@code ""} otherwise
   */
  static String nullToEmpty(@Nullable String string) {
    return (string == null) ? "" : string;
  }

  /**
   * Returns the string if it is not empty, or a null string otherwise.
   *
   * @param string the string to test and possibly return
   * @return {@code string} if it is not empty; {@code null} otherwise
   */
  static @Nullable String emptyToNull(@Nullable String string) {
    return stringIsNullOrEmpty(string) ? null : string;
  }

  static String lenientFormat(@Nullable String template, @Nullable Object @Nullable ... args) {
    return Strings.lenientFormat(template, args);
  }

  static String stringValueOf(@Nullable Object o) {
    return String.valueOf(o);
  }

  static CommonPattern compilePattern(String pattern) {
    Preconditions.checkNotNull(pattern);
    return patternCompiler.compile(pattern);
  }

  static boolean patternCompilerIsPcreLike() {
    return patternCompiler.isPcreLike();
  }

  private static PatternCompiler loadPatternCompiler() {
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

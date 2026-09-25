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
import org.jspecify.annotations.Nullable;

/**
 * Methods factored out so that they can be emulated differently in GWT.
 *
 * @author Jesse Wilson
 */
@GwtCompatible
final class Platform {
  private static final PatternCompiler patternCompiler = loadPatternCompiler();

  private Platform() {}

  static CharMatcher precomputeCharMatcher(CharMatcher matcher) {
    return matcher.precomputedInternal();
  }

  static <T extends Enum<T>> Optional<T> getEnumIfPresent(Class<T> enumClass, String value) {
    WeakReference<? extends Enum<?>> ref = Enums.getEnumConstants(enumClass).get(value);
    return ref == null ? Optional.absent() : Optional.fromNullable(enumClass.cast(ref.get()));
  }

  static String formatCompact4Digits(double value) {
    return String.format(Locale.ROOT, "%.4g", value);
  }

  static boolean stringIsNullOrEmpty(@Nullable String string) {
    return string == null || string.isEmpty();
  }

  static String nullToEmpty(@Nullable String string) {
    return (string == null) ? "" : string;
  }

  static @Nullable String emptyToNull(@Nullable String string) {
    return stringIsNullOrEmpty(string) ? null : string;
  }

  static String lenientFormat(
      @Nullable String template, @Nullable Object @Nullable ... args) {
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
    // We want the JDK Pattern compiler:
    // - under Android (where it hurts startup performance)
    // - even for the JVM in our open-source release (https://github.com/google/guava/issues/3147)
    // If anyone in our monorepo uses the Android copy of Guava on a JVM, that would be unfortunate.
    // But that is only likely to happen in Robolectric tests, where the risks of JDK regex are low.
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

  private static @Nullable ThreadLocal<char @Nullable []> destTl;

  /** Acquires a thread-local 1024-char buffer if available, or returns null if busy. */
  static char @Nullable [] acquireCharBuffer() {
    ThreadLocal<char @Nullable []> tl = destTl;
    if (tl == null) {
      destTl = tl = new ThreadLocal<char @Nullable []>();
    }
    char[] buffer = tl.get();
    if (buffer == null) {
      return new char[1024];
    }
    tl.set(null);
    return buffer;
  }

  /** Releases the acquired thread-local buffer. */
  static void releaseCharBuffer(char[] buffer) {
    if (buffer.length == 1024) {
      ThreadLocal<char @Nullable []> tl = destTl;
      if (tl != null) {
        tl.set(buffer);
      }
    }
  }
}

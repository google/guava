/*
 * Copyright (C) 2009 The Guava Authors
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

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import org.jspecify.annotations.Nullable;

/**
 * @author Jesse Wilson
 */
final class Platform {
  static CharMatcher precomputeCharMatcher(CharMatcher matcher) {
    // CharMatcher.precomputed() produces CharMatchers that are maybe a little
    // faster (and that's debatable), but definitely more memory-hungry. We're
    // choosing to turn .precomputed() into a no-op in GWT, because it doesn't
    // seem to be a worthwhile tradeoff in a browser.
    return matcher;
  }

  static String formatCompact4Digits(double value) {
    return toPrecision(value, 4);
  }

  @JsMethod(name = "Number.prototype.toPrecision.call", namespace = JsPackage.GLOBAL)
  private static native String toPrecision(double value, int precision);

  @JsMethod
  static native boolean stringIsNullOrEmpty(@Nullable String string) /*-{
    return !string;
  }-*/;

  @JsMethod
  static native String nullToEmpty(@Nullable String string) /*-{
    return string || "";
  }-*/;

  @JsMethod
  static native String emptyToNull(@Nullable String string) /*-{
    return string || null;
  }-*/;

  static CommonPattern compilePattern(String pattern) {
    throw new UnsupportedOperationException();
  }

  static boolean patternCompilerIsPcreLike() {
    throw new UnsupportedOperationException();
  }

  static String lenientFormat(@Nullable String template, @Nullable Object @Nullable ... args) {
    return Strings.lenientFormat(template, args);
  }

  static String stringValueOf(@Nullable Object o) {
    return String.valueOf(o);
  }

  private Platform() {}
}

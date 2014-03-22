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

import java.util.concurrent.TimeUnit;

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

  static long systemNanoTime() {
    // System.nanoTime() is not available in GWT, so we get milliseconds
    // and convert to nanos.
    return TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
  }

  static <T extends Enum<T>> Optional<T> getEnumIfPresent(Class<T> enumClass, String value) {
    try {
      return Optional.of(Enum.valueOf(enumClass, value));
    } catch (IllegalArgumentException iae) {
      return Optional.absent();
    }
  }

  private Platform() {}
}

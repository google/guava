/*
 * Copyright (C) 2016 The Guava Authors
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

/**
 * The subset of the {@link java.util.regex.Matcher} API which is used by this package, and also
 * shared with the {@code re2j} library. For internal use only. Please refer to the {@code Matcher}
 * javadoc for details.
 */
@GwtCompatible
abstract class CommonMatcher {
  /** Returns {@code true} if the entire input region matches the pattern. */
  public abstract boolean matches();

  /** Attempts to find the next subsequence of the input sequence that matches the pattern. */
  public abstract boolean find();

  /**
   * Resets this matcher and then attempts to find the next subsequence of the input sequence that
   * matches the pattern, starting at the specified index.
   */
  public abstract boolean find(int index);

  /**
   * Replaces every subsequence of the input sequence that matches the pattern with the given
   * replacement string.
   */
  public abstract String replaceAll(String replacement);

  /** Returns the offset after the last character matched. */
  public abstract int end();

  /** Returns the start index of the previous match. */
  public abstract int start();
}

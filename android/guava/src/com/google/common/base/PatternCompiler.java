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

import com.google.common.annotations.GwtIncompatible;

/**
 * Pluggable interface for compiling a regex pattern. By default this package uses the {@code
 * java.util.regex} library, but an alternate implementation can be supplied using the {@link
 * java.util.ServiceLoader} mechanism.
 */
@GwtIncompatible
interface PatternCompiler {
  /**
   * Compiles the given pattern.
   *
   * @throws IllegalArgumentException if the pattern is invalid
   */
  CommonPattern compile(String pattern);

  /**
   * Returns {@code true} if the regex implementation behaves like Perl -- notably, by supporting
   * possessive quantifiers but also being susceptible to catastrophic backtracking.
   */
  boolean isPcreLike();
}

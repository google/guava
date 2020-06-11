/*
 * Copyright (C) 2014 The Guava Authors
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

package com.google.common.cache;

import static com.google.common.truth.Truth.assertThat;

import junit.framework.TestCase;

/** Unit tests for {@link LongAdder}. */
public class LongAdderTest extends TestCase {

  /**
   * No-op null-pointer test for {@link LongAdder} to override the {@link PackageSanityTests}
   * version, which checks package-private methods that we don't want to have to annotate as {@code
   * Nullable} because we don't want diffs from jsr166e.
   */
  public void testNulls() {}

  public void testOverflows() {
    LongAdder longAdder = new LongAdder();
    longAdder.add(Long.MAX_VALUE);
    assertThat(longAdder.sum()).isEqualTo(Long.MAX_VALUE);
    longAdder.add(1);
    // silently overflows; is this a bug?
    // See https://github.com/google/guava/issues/3503
    assertThat(longAdder.sum()).isEqualTo(-9223372036854775808L);
  }
}

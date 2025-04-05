/*
 * Copyright (C) 2015 The Guava Authors
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

package com.google.common.util.concurrent;

import static com.google.common.truth.Truth.assertThat;

import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests that {@link AggregateFutureState} uses the expected {@code AtomicHelper} implementation.
 *
 * <p>We have more thorough testing of {@code AtomicHelper} implementations in {@link
 * AggregateFutureStateFallbackAtomicHelperTest}. The advantage to this test is that it can run
 * under Android.
 */
@NullUnmarked
public class AggregateFutureStateDefaultAtomicHelperTest extends TestCase {
  public void testUsingExpectedAtomicHelper() throws Exception {
    assertThat(AggregateFutureState.atomicHelperTypeForTest()).isEqualTo("SafeAtomicHelper");
  }
}

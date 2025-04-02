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

import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;
import static com.google.common.truth.Truth.assertThat;

import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests that {@link AbstractFutureState} uses the expected {@code AtomicHelper} implementation.
 *
 * <p>We have more thorough testing of {@code AtomicHelper} implementations in {@link
 * AbstractFutureFallbackAtomicHelperTest}. The advantage to this test is that it can run under
 * Android.
 */
@NullUnmarked
public class AbstractFutureDefaultAtomicHelperTest extends TestCase {
  public void testUsingExpectedAtomicHelper() throws Exception {
    if (isJava8() || isAndroid()) {
      assertThat(AbstractFutureState.atomicHelperTypeForTest()).isEqualTo("UnsafeAtomicHelper");
    } else {
      assertThat(AbstractFutureState.atomicHelperTypeForTest()).isEqualTo("VarHandleAtomicHelper");
    }
  }

  private static boolean isJava8() {
    return JAVA_SPECIFICATION_VERSION.value().equals("1.8");
  }

  private static boolean isAndroid() {
    return System.getProperty("java.runtime.name", "").contains("Android");
  }
}

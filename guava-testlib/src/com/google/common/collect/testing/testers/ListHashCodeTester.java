/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.collect.testing.testers;

import java.lang.reflect.Method;

/**
 * Tests {@link java.util.List#hashCode}.
 *
 * <p>This class is GWT compatible.
 *
 * @author George van den Driessche
 */
public class ListHashCodeTester<E> extends AbstractListTester<E> {
  public void testHashCode() {
    int expectedHashCode = 1;
    for (E element : getSampleElements()) {
      expectedHashCode = 31 * expectedHashCode +
          ((element == null) ? 0 : element.hashCode());
    }
    assertEquals(
        "A List's hashCode() should be computed from those of its elements.",
        expectedHashCode, getList().hashCode());
  }

  /**
   * Returns the {@link Method} instance for {@link #testHashCode()} so that
   * list tests on unhashable objects can suppress it with
   * {@code FeatureSpecificTestSuiteBuilder.suppressing()}.
   */
  public static Method getHashCodeMethod() {
    return Platform.getMethod(ListHashCodeTester.class, "testHashCode");
  }
}

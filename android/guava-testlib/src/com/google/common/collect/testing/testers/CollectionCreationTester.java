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

import static com.google.common.collect.testing.Helpers.getMethod;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.testers.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.lang.reflect.Method;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests creation (typically through a constructor or static factory
 * method) of a collection. Can't be invoked directly; please see {@link
 * com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@GwtCompatible(emulated = true)
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class CollectionCreationTester<E> extends AbstractCollectionTester<E> {
  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testCreateWithNull_supported() {
    E[] array = createArrayWithNullElement();
    collection = getSubjectGenerator().create(array);
    expectContents(array);
  }

  @CollectionFeature.Require(absent = ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testCreateWithNull_unsupported() {
    E[] array = createArrayWithNullElement();

    assertThrows(
        NullPointerException.class,
        () -> {
          Object unused = getSubjectGenerator().create(array);
        });
  }

  /**
   * Returns the {@link Method} instance for {@link #testCreateWithNull_unsupported()} so that tests
   * can suppress it with {@code FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="https://bugs.openjdk.org/browse/JDK-5045147">JDK-5045147</a> is fixed.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getCreateWithNullUnsupportedMethod() {
    return getMethod(CollectionCreationTester.class, "testCreateWithNull_unsupported");
  }
}

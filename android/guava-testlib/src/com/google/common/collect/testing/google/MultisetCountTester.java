/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_QUERIES;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.WrongType;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.Ignore;

/**
 * Tests for {@code Multiset#count}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultisetCountTester<E> extends AbstractMultisetTester<E> {

  public void testCount_0() {
    assertEquals("multiset.count(missing) didn't return 0", 0, getMultiset().count(e3()));
  }

  @CollectionSize.Require(absent = ZERO)
  public void testCount_1() {
    assertEquals("multiset.count(present) didn't return 1", 1, getMultiset().count(e0()));
  }

  @CollectionSize.Require(SEVERAL)
  public void testCount_3() {
    initThreeCopies();
    assertEquals("multiset.count(thriceContained) didn't return 3", 3, getMultiset().count(e0()));
  }

  @CollectionFeature.Require(ALLOWS_NULL_QUERIES)
  public void testCount_nullAbsent() {
    assertEquals("multiset.count(null) didn't return 0", 0, getMultiset().count(null));
  }

  @CollectionFeature.Require(absent = ALLOWS_NULL_QUERIES)
  public void testCount_null_forbidden() {
    try {
      getMultiset().count(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  public void testCount_nullPresent() {
    initCollectionWithNullElement();
    assertEquals(1, getMultiset().count(null));
  }

  public void testCount_wrongType() {
    assertEquals(
        "multiset.count(wrongType) didn't return 0", 0, getMultiset().count(WrongType.VALUE));
  }

  /**
   * Returns {@link Method} instances for the read tests that assume multisets support duplicates so
   * that the test of {@code Multisets.forSet()} can suppress them.
   */
  @GwtIncompatible // reflection
  public static List<Method> getCountDuplicateInitializingMethods() {
    return Arrays.asList(Helpers.getMethod(MultisetCountTester.class, "testCount_3"));
  }
}

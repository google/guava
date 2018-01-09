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

import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_SET;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import java.lang.reflect.Method;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code set()} operations on a list. Can't be invoked directly;
 * please see {@link com.google.common.collect.testing.ListTestSuiteBuilder}.
 *
 * @author George van den Driessche
 */
@GwtCompatible(emulated = true)
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class ListSetTester<E> extends AbstractListTester<E> {
  @ListFeature.Require(SUPPORTS_SET)
  @CollectionSize.Require(absent = ZERO)
  public void testSet() {
    doTestSet(e3());
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  @ListFeature.Require(SUPPORTS_SET)
  public void testSet_null() {
    doTestSet(null);
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  @ListFeature.Require(SUPPORTS_SET)
  public void testSet_replacingNull() {
    E[] elements = createSamplesArray();
    int i = aValidIndex();
    elements[i] = null;
    collection = getSubjectGenerator().create(elements);

    doTestSet(e3());
  }

  private void doTestSet(E newValue) {
    int index = aValidIndex();
    E initialValue = getList().get(index);
    assertEquals(
        "set(i, x) should return the old element at position i.",
        initialValue,
        getList().set(index, newValue));
    assertEquals("After set(i, x), get(i) should return x", newValue, getList().get(index));
    assertEquals("set() should not change the size of a list.", getNumElements(), getList().size());
  }

  @ListFeature.Require(SUPPORTS_SET)
  public void testSet_indexTooLow() {
    try {
      getList().set(-1, e3());
      fail("set(-1) should throw IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
    }
    expectUnchanged();
  }

  @ListFeature.Require(SUPPORTS_SET)
  public void testSet_indexTooHigh() {
    int index = getNumElements();
    try {
      getList().set(index, e3());
      fail("set(size) should throw IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException expected) {
    }
    expectUnchanged();
  }

  @CollectionSize.Require(absent = ZERO)
  @ListFeature.Require(absent = SUPPORTS_SET)
  public void testSet_unsupported() {
    try {
      getList().set(aValidIndex(), e3());
      fail("set() should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
  }

  @CollectionSize.Require(ZERO)
  @ListFeature.Require(absent = SUPPORTS_SET)
  public void testSet_unsupportedByEmptyList() {
    try {
      getList().set(0, e3());
      fail("set() should throw UnsupportedOperationException or IndexOutOfBoundsException");
    } catch (UnsupportedOperationException | IndexOutOfBoundsException tolerated) {
    }
    expectUnchanged();
  }

  @CollectionSize.Require(absent = ZERO)
  @ListFeature.Require(SUPPORTS_SET)
  @CollectionFeature.Require(absent = ALLOWS_NULL_VALUES)
  public void testSet_nullUnsupported() {
    try {
      getList().set(aValidIndex(), null);
      fail("set(null) should throw NullPointerException");
    } catch (NullPointerException expected) {
    }
    expectUnchanged();
  }

  private int aValidIndex() {
    return getList().size() / 2;
  }

  /**
   * Returns the {@link java.lang.reflect.Method} instance for {@link #testSet_null()} so that tests
   * of {@link java.util.Collections#checkedCollection(java.util.Collection, Class)} can suppress it
   * with {@code FeatureSpecificTestSuiteBuilder.suppressing()} until <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6409434">Sun bug 6409434</a> is fixed.
   * It's unclear whether nulls were to be permitted or forbidden, but presumably the eventual fix
   * will be to permit them, as it seems more likely that code would depend on that behavior than on
   * the other. Thus, we say the bug is in set(), which fails to support null.
   */
  @GwtIncompatible // reflection
  public static Method getSetNullSupportedMethod() {
    return Helpers.getMethod(ListSetTester.class, "testSet_null");
  }
}

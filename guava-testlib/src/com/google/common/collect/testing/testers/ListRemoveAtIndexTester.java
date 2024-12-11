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

import static com.google.common.collect.testing.Helpers.copyToList;
import static com.google.common.collect.testing.features.CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_REMOVE_WITH_INDEX;
import static com.google.common.collect.testing.testers.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code remove(int)} operations on a list. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.ListTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class ListRemoveAtIndexTester<E> extends AbstractListTester<E> {
  @ListFeature.Require(absent = SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testRemoveAtIndex_unsupported() {
    assertThrows(UnsupportedOperationException.class, () -> getList().remove(0));
    expectUnchanged();
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  public void testRemoveAtIndex_negative() {
    assertThrows(IndexOutOfBoundsException.class, () -> getList().remove(-1));
    expectUnchanged();
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  public void testRemoveAtIndex_tooLarge() {
    assertThrows(IndexOutOfBoundsException.class, () -> getList().remove(getNumElements()));
    expectUnchanged();
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testRemoveAtIndex_first() {
    runRemoveTest(0);
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testRemoveAtIndex_middle() {
    runRemoveTest(getNumElements() / 2);
  }

  @CollectionFeature.Require(FAILS_FAST_ON_CONCURRENT_MODIFICATION)
  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testRemoveAtIndexConcurrentWithIteration() {
    assertThrows(
        ConcurrentModificationException.class,
        () -> {
          Iterator<E> iterator = collection.iterator();
          getList().remove(getNumElements() / 2);
          iterator.next();
        });
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testRemoveAtIndex_last() {
    runRemoveTest(getNumElements() - 1);
  }

  private void runRemoveTest(int index) {
    assertEquals(
        Platform.format("remove(%d) should return the element at index %d", index, index),
        getList().get(index),
        getList().remove(index));
    List<E> expected = copyToList(createSamplesArray());
    expected.remove(index);
    expectContents(expected);
  }
}

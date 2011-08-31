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

import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_REMOVE_WITH_INDEX;

import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;

import java.util.List;

/**
 * A generic JUnit test which tests {@code remove(int)} operations on a list.
 * Can't be invoked directly; please see
 * {@link com.google.common.collect.testing.ListTestSuiteBuilder}.
 *
 * <p>This class is GWT compatible.
 *
 * @author Chris Povirk
 */
public class ListRemoveAtIndexTester<E> extends AbstractListTester<E> {
  @ListFeature.Require(absent = SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testRemoveAtIndex_unsupported() {
    try {
      getList().remove(0);
      fail("remove(i) should throw");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  public void testRemoveAtIndex_negative() {
    try {
      getList().remove(-1);
      fail("remove(-1) should throw");
    } catch (IndexOutOfBoundsException expected) {
    }
    expectUnchanged();
  }

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  public void testRemoveAtIndex_tooLarge() {
    try {
      getList().remove(getNumElements());
      fail("remove(size) should throw");
    } catch (IndexOutOfBoundsException expected) {
    }
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

  @ListFeature.Require(SUPPORTS_REMOVE_WITH_INDEX)
  @CollectionSize.Require(absent = ZERO)
  public void testRemoveAtIndex_last() {
    runRemoveTest(getNumElements() - 1);
  }

  private void runRemoveTest(int index) {
    assertEquals(Platform.format(
        "remove(%d) should return the element at index %d", index, index),
        getList().get(index), getList().remove(index));
    List<E> expected = Helpers.copyToList(createSamplesArray());
    expected.remove(index);
    expectContents(expected);
  }
}

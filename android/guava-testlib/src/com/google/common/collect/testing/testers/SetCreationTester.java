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
import static com.google.common.collect.testing.features.CollectionFeature.REJECTS_DUPLICATES_AT_CREATION;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.testers.ReflectionFreeAssertThrows.assertThrows;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.List;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests creation (typically through a constructor or static factory
 * method) of a set. Can't be invoked directly; please see {@link
 * com.google.common.collect.testing.SetTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class SetCreationTester<E> extends AbstractSetTester<E> {
  @CollectionFeature.Require(value = ALLOWS_NULL_VALUES, absent = REJECTS_DUPLICATES_AT_CREATION)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testCreateWithDuplicates_nullDuplicatesNotRejected() {
    E[] array = createArrayWithNullElement();
    array[0] = null;
    collection = getSubjectGenerator().create(array);

    List<E> expectedWithDuplicateRemoved = asList(array).subList(1, getNumElements());
    expectContents(expectedWithDuplicateRemoved);
  }

  @CollectionFeature.Require(absent = REJECTS_DUPLICATES_AT_CREATION)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testCreateWithDuplicates_nonNullDuplicatesNotRejected() {
    E[] array = createSamplesArray();
    array[1] = e0();
    collection = getSubjectGenerator().create(array);

    List<E> expectedWithDuplicateRemoved = asList(array).subList(1, getNumElements());
    expectContents(expectedWithDuplicateRemoved);
  }

  @CollectionFeature.Require({ALLOWS_NULL_VALUES, REJECTS_DUPLICATES_AT_CREATION})
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testCreateWithDuplicates_nullDuplicatesRejected() {
    E[] array = createArrayWithNullElement();
    array[0] = null;
    assertThrows(
        IllegalArgumentException.class, () -> collection = getSubjectGenerator().create(array));
  }

  @CollectionFeature.Require(REJECTS_DUPLICATES_AT_CREATION)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testCreateWithDuplicates_nonNullDuplicatesRejected() {
    E[] array = createSamplesArray();
    array[1] = e0();
    assertThrows(
        IllegalArgumentException.class, () -> collection = getSubjectGenerator().create(array));
  }
}

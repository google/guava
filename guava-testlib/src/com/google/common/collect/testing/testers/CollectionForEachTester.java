/*
 * Copyright (C) 2015 The Guava Authors
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

import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code forEach} operations on a collection. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class CollectionForEachTester<E> extends AbstractCollectionTester<E> {
  @CollectionFeature.Require(absent = KNOWN_ORDER)
  public void testForEachUnknownOrder() {
    List<E> elements = new ArrayList<>();
    collection.forEach(elements::add);
    Helpers.assertEqualIgnoringOrder(Arrays.asList(createSamplesArray()), elements);
  }

  @CollectionFeature.Require(KNOWN_ORDER)
  public void testForEachKnownOrder() {
    List<E> elements = new ArrayList<>();
    collection.forEach(elements::add);
    List<E> expected = Helpers.copyToList(getOrderedElements());
    assertEquals("Different ordered iteration", expected, elements);
  }
}

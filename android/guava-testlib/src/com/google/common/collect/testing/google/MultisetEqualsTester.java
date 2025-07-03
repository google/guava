/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.EqualsTester;
import org.junit.Ignore;

/**
 * Tests for {@code Multiset.equals} and {@code Multiset.hashCode}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class MultisetEqualsTester<E> extends AbstractMultisetTester<E> {
  public void testEqualsSameContents() {
    new EqualsTester()
        .addEqualityGroup(
            getMultiset(), getSubjectGenerator().create(getSampleElements().toArray()))
        .testEquals();
  }

  @CollectionSize.Require(absent = ZERO)
  public void testNotEqualsEmpty() {
    new EqualsTester()
        .addEqualityGroup(getMultiset())
        .addEqualityGroup(getSubjectGenerator().create())
        .testEquals();
  }

  public void testHashCodeMatchesEntrySet() {
    assertEquals(getMultiset().entrySet().hashCode(), getMultiset().hashCode());
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  public void testEqualsMultisetWithNullValue() {
    new EqualsTester()
        .addEqualityGroup(getMultiset())
        .addEqualityGroup(
            getSubjectGenerator().create(createArrayWithNullElement()),
            getSubjectGenerator().create(createArrayWithNullElement()))
        .testEquals();
  }
}

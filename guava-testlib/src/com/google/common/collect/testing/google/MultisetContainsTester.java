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

import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.Arrays;
import org.junit.Ignore;

/**
 * Tests for {@code Multiset.containsAll} not already addressed by {@code CollectionContainsTester}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultisetContainsTester<E> extends AbstractMultisetTester<E> {
  @CollectionSize.Require(absent = ZERO)
  public void testContainsAllMultisetIgnoresFrequency() {
    assertTrue(getMultiset().containsAll(getSubjectGenerator().create(e0(), e0(), e0())));
  }

  @CollectionSize.Require(absent = ZERO)
  public void testContainsAllListIgnoresFrequency() {
    assertTrue(getMultiset().containsAll(Arrays.asList(e0(), e0(), e0())));
  }
}

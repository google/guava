/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.collect;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Math.max;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for CompactHashSet.
 *
 * @author Dimitris Andreou
 */
@GwtIncompatible // java.util.Arrays#copyOf(Object[], int), java.lang.reflect.Array
public class CompactHashSetTest extends TestCase {
  public static Test suite() {
    List<Feature<?>> allFeatures =
        Arrays.<Feature<?>>asList(
            CollectionSize.ANY,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.REMOVE_OPERATIONS,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.SUPPORTS_ADD,
            CollectionFeature.SUPPORTS_REMOVE);

    TestSuite suite = new TestSuite();
    suite.addTestSuite(CompactHashSetTest.class);
    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    return CompactHashSet.create(asList(elements));
                  }
                })
            .named("CompactHashSet")
            .withFeatures(allFeatures)
            .createTestSuite());
    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    CompactHashSet<String> set = CompactHashSet.create();
                    set.convertToHashFloodingResistantImplementation();
                    Collections.addAll(set, elements);
                    return set;
                  }
                })
            .named("CompactHashSet with flooding protection")
            .withFeatures(allFeatures)
            .createTestSuite());
    suite.addTest(
        SetTestSuiteBuilder.using(
                new TestStringSetGenerator() {
                  @Override
                  protected Set<String> create(String[] elements) {
                    CompactHashSet<String> set = CompactHashSet.create(asList(elements));
                    for (int i = 0; i < 100; i++) {
                      set.add("extra" + i);
                    }
                    for (int i = 0; i < 100; i++) {
                      set.remove("extra" + i);
                    }
                    set.trimToSize();
                    return set;
                  }
                })
            .named("CompactHashSet#TrimToSize")
            .withFeatures(allFeatures)
            .createTestSuite());
    return suite;
  }

  public void testAllocArraysDefault() {
    CompactHashSet<Integer> set = CompactHashSet.create();
    assertThat(set.needsAllocArrays()).isTrue();
    assertThat(set.elements).isNull();

    set.add(1);
    assertThat(set.needsAllocArrays()).isFalse();
    assertThat(set.elements).hasLength(CompactHashing.DEFAULT_SIZE);
  }

  public void testAllocArraysExpectedSize() {
    for (int i = 0; i <= CompactHashing.DEFAULT_SIZE; i++) {
      CompactHashSet<Integer> set = CompactHashSet.createWithExpectedSize(i);
      assertThat(set.needsAllocArrays()).isTrue();
      assertThat(set.elements).isNull();

      set.add(1);
      assertThat(set.needsAllocArrays()).isFalse();
      int expectedSize = max(1, i);
      assertThat(set.elements).hasLength(expectedSize);
    }
  }

}

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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Set;

/**
 * Collection tests for bimaps.
 *
 * @author Jared Levy
 */
@GwtCompatible
public class BiMapCollectionTest extends TestCase {

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            BiMap<String, Integer> bimap = HashBiMap.create();
            for (int i = 0; i < elements.length; i++) {
              bimap.put(elements[i], i);
            }
            return bimap.keySet();
          }
        })
        .named("HashBiMap.keySet")
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.REMOVE_OPERATIONS)
        .createTestSuite());

    suite.addTest(SetTestSuiteBuilder.using(new TestStringSetGenerator() {
          @Override protected Set<String> create(String[] elements) {
            BiMap<Integer, String> bimap = HashBiMap.create();
            for (int i = 0; i < elements.length; i++) {
              bimap.put(i, elements[i]);
            }
            return bimap.values();
          }
        })
        .named("HashBiMap.values")
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.REMOVE_OPERATIONS,
            CollectionFeature.REJECTS_DUPLICATES_AT_CREATION)
        .createTestSuite());

    return suite;
  }
}

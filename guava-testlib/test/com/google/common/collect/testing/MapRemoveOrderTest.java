/*
 * Copyright (C) 2026 The Guava Authors
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

package com.google.common.collect.testing;

import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.testers.MapRemoveTester;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.jspecify.annotations.Nullable;

/** Tests that generated removal tests check the order of the remaining entries. */
@AndroidIncompatible // test-suite builders
public final class MapRemoveOrderTest extends TestCase {
  public void testReorderingOnRemovalIsDetected() {
    TestResult result = runRemovalTests(false, true, true);
    assertEquals(0, result.errorCount());
    assertEquals(3, result.failureCount());
  }

  public void testInsertionOrderIsPreserved() {
    assertSuccessful(runRemovalTests(false, false, true));
  }

  public void testSortedOrderIsPreserved() {
    assertSuccessful(runRemovalTests(true, false, true));
  }

  public void testUnorderedMapMayReorderOnRemoval() {
    assertSuccessful(runRemovalTests(false, true, false));
  }

  private static void assertSuccessful(TestResult result) {
    assertTrue(result.runCount() > 0);
    assertEquals(0, result.errorCount());
    assertEquals(0, result.failureCount());
  }

  private static TestResult runRemovalTests(
      boolean sorted, boolean reorderOnRemoval, boolean knownOrder) {
    TestStringMapGenerator generator =
        new TestStringMapGenerator() {
          @Override
          protected Map<String, String> create(Entry<String, String>[] entries) {
            Map<String, String> map = sorted ? new TreeMap<>() : new LinkedHashMap<>();
            for (Entry<String, String> entry : entries) {
              map.put(entry.getKey(), entry.getValue());
            }
            if (!reorderOnRemoval) {
              return map;
            }
            return new LinkedHashMap<String, String>(map) {
              @Override
              public @Nullable String remove(Object key) {
                String value = super.remove(key);
                if (value != null) {
                  List<Entry<String, String>> remaining = new ArrayList<>(entrySet());
                  Collections.reverse(remaining);
                  clear();
                  for (Entry<String, String> entry : remaining) {
                    put(entry.getKey(), entry.getValue());
                  }
                }
                return value;
              }
            };
          }

          @Override
          public Iterable<Entry<String, String>> order(List<Entry<String, String>> entries) {
            List<Entry<String, String>> ordered = new ArrayList<>(entries);
            if (sorted) {
              ordered.sort(Comparator.comparing(Entry::getKey));
            }
            return ordered;
          }
        };
    MapTestSuiteBuilder<String, String> builder =
        MapTestSuiteBuilder.using(generator)
            .named("removal order")
            .withFeatures(MapFeature.GENERAL_PURPOSE, CollectionSize.SEVERAL);
    if (knownOrder) {
      builder.withFeatures(CollectionFeature.KNOWN_ORDER);
    }
    TestSuite removalTests = new TestSuite();
    collectRemovalTests(builder.createTestSuite(), removalTests);
    TestResult result = new TestResult();
    removalTests.run(result);
    return result;
  }

  private static void collectRemovalTests(Test test, TestSuite result) {
    if (test instanceof TestSuite) {
      TestSuite suite = (TestSuite) test;
      for (int i = 0; i < suite.testCount(); i++) {
        collectRemovalTests(suite.testAt(i), result);
      }
    } else if (test instanceof MapRemoveTester) {
      result.addTest(test);
    }
  }
}

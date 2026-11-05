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

import com.google.common.collect.ForwardingConcurrentMap;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;

/** Tests for {@link ConcurrentMapTestSuiteBuilder}. */
@AndroidIncompatible // test-suite builders
public final class ConcurrentMapTestSuiteBuilderTest extends TestCase {
  public void testConcurrentHashMapDerivedSetSpliteratorsPass() {
    TestResult result = runSuite(new ConcurrentHashMapGenerator(), "ConcurrentHashMap");

    assertEquals(0, result.errorCount());
    assertEquals(0, result.failureCount());
  }

  public void testConcurrentMapDerivedSetSpliteratorTestsCatchWrongCharacteristics() {
    TestResult result = runSuite(new BadSpliteratorConcurrentMapGenerator(), "BadConcurrentMap");

    assertEquals(0, result.errorCount());

    List<String> failedTests = failedTests(result);
    assertTrue(failedTests.toString(), failedTests.size() >= 4);
    assertContainsFailure(
        failedTests, "testKeySetSpliteratorHasConcurrentCharacteristic");
    assertContainsFailure(
        failedTests, "testKeySetSpliteratorDoesNotHaveSizedCharacteristic");
    assertContainsFailure(
        failedTests, "testEntrySetSpliteratorHasConcurrentCharacteristic");
    assertContainsFailure(
        failedTests, "testEntrySetSpliteratorDoesNotHaveSizedCharacteristic");
  }

  private static TestResult runSuite(TestStringMapGenerator generator, String name) {
    Test suite =
        ConcurrentMapTestSuiteBuilder.using(generator)
            .named(name)
            .withFeatures(
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionSize.ONE)
            .suppressing(new OpenJdk6MapTests().suppressForConcurrentHashMap())
            .createTestSuite();
    TestResult result = new TestResult();
    suite.run(result);
    return result;
  }

  private static List<String> failedTests(TestResult result) {
    List<String> failedTests = new ArrayList<>();
    for (java.util.Enumeration<TestFailure> failures = result.failures();
        failures.hasMoreElements(); ) {
      failedTests.add(failures.nextElement().failedTest().toString());
    }
    return failedTests;
  }

  private static void assertContainsFailure(List<String> failedTests, String testName) {
    for (String failedTest : failedTests) {
      if (failedTest.contains(testName)) {
        return;
      }
    }
    fail("Expected failure for " + testName + " in " + failedTests);
  }

  private static final class ConcurrentHashMapGenerator extends TestStringMapGenerator {
    @Override
    protected Map<String, String> create(Entry<String, String>[] entries) {
      ConcurrentMap<String, String> map = new ConcurrentHashMap<>();
      for (Entry<String, String> entry : entries) {
        map.put(entry.getKey(), entry.getValue());
      }
      return map;
    }
  }

  private static final class BadSpliteratorConcurrentMapGenerator extends TestStringMapGenerator {
    @Override
    protected Map<String, String> create(Entry<String, String>[] entries) {
      BadSpliteratorConcurrentMap map = new BadSpliteratorConcurrentMap();
      for (Entry<String, String> entry : entries) {
        map.put(entry.getKey(), entry.getValue());
      }
      return map;
    }
  }

  private static final class BadSpliteratorConcurrentMap
      extends ForwardingConcurrentMap<String, String> {
    private final ConcurrentMap<String, String> delegate = new ConcurrentHashMap<>();

    @Override
    protected ConcurrentMap<String, String> delegate() {
      return delegate;
    }

    @Override
    public Set<String> keySet() {
      return new BadSpliteratorSet<>(delegate.keySet());
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
      return new BadSpliteratorSet<>(delegate.entrySet());
    }
  }

  private static final class BadSpliteratorSet<E> extends AbstractSet<E> {
    private final Set<E> delegate;

    BadSpliteratorSet(Set<E> delegate) {
      this.delegate = delegate;
    }

    @Override
    public Iterator<E> iterator() {
      return delegate.iterator();
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public boolean contains(Object o) {
      return delegate.contains(o);
    }

    @Override
    public boolean remove(Object o) {
      return delegate.remove(o);
    }

    @Override
    public void clear() {
      delegate.clear();
    }

    @Override
    public Spliterator<E> spliterator() {
      return Spliterators.spliterator(iterator(), size(), Spliterator.DISTINCT);
    }
  }
}

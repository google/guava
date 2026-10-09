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

import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;

import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Regression test for {@link testers.CollectionRemoveIfTester} feature requirements.
 */
@AndroidIncompatible // test-suite builders
public class CollectionRemoveIfFeatureTest extends TestCase {

  /**
   * A collection that supports {@link Collection#removeIf} and {@link Collection#remove} but whose
   * iterator does not support {@link Iterator#remove()}.
   */
  private static final class RemoveIfOnlyList extends AbstractList<String> {
    private final List<String> elements = Lists.newArrayList("a", "b", "c");

    void resetTo(String[] newElements) {
      elements.clear();
      elements.addAll(Lists.newArrayList(newElements));
    }

    @Override
    public String get(int index) {
      return elements.get(index);
    }

    @Override
    public int size() {
      return elements.size();
    }

    @Override
    public boolean remove(Object o) {
      return elements.remove(o);
    }

    @Override
    public boolean removeIf(Predicate<? super String> filter) {
      return elements.removeIf(filter);
    }

    @Override
    public Iterator<String> iterator() {
      return new Iterator<String>() {
        private int index;

        @Override
        public boolean hasNext() {
          return index < size();
        }

        @Override
        public String next() {
          return get(index++);
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  public void testRemoveIfTestsRunWhenOnlySupportsRemove() {
    TestSuite suite =
        CollectionTestSuiteBuilder.using(
                new TestStringCollectionGenerator() {
                  @Override
                  protected Collection<String> create(String[] elements) {
                    RemoveIfOnlyList list = new RemoveIfOnlyList();
                    list.resetTo(elements);
                    return list;
                  }
                })
            .named("RemoveIfOnlyList")
            .withFeatures(SUPPORTS_REMOVE, KNOWN_ORDER, SEVERAL)
            .createTestSuite();

    int removeIfTestCount = countTestsWithNameContaining(suite, "testRemoveIf");
    assertTrue(
        "removeIf tests should run for collections with SUPPORTS_REMOVE, even without"
            + " SUPPORTS_ITERATOR_REMOVE",
        removeIfTestCount > 0);
  }

  private static int countTestsWithNameContaining(Test test, String substring) {
    if (test instanceof TestSuite) {
      TestSuite testSuite = (TestSuite) test;
      int count = 0;
      for (int i = 0; i < testSuite.testCount(); i++) {
        count += countTestsWithNameContaining(testSuite.testAt(i), substring);
      }
      return count;
    }
    return test.toString().contains(substring) ? 1 : 0;
  }
}

/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.DiscreteDomains.integers;

import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.testers.SetHashCodeTester;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Gregory Kick
 */
public class ContiguousSetNonGwtTest extends TestCase {
  public static class BuiltTests extends TestCase {
    public static Test suite() {
      TestSuite suite = new TestSuite();

      suite.addTest(
          SetTestSuiteBuilder
              .using(
                  new TestIntegerSetGenerator() {
                    @Override
                    protected Set<Integer> create(Integer[] elements) {
                      // reject duplicates at creation, just so that I can use
                      // that SetFeature below, which stops a test from running
                      // that doesn't work. hack!
                      SortedSet<Integer> set = new TreeSet<Integer>();
                      Collections.addAll(set, elements);
                      checkArgument(set.size() == elements.length);
                      return Ranges.closed(set.first(), set.last()).asSet(integers());
                    }
                  })
              .withFeatures(
                  CollectionSize.ONE,
                  CollectionSize.SEVERAL,
                  CollectionFeature.KNOWN_ORDER,
                  CollectionFeature.ALLOWS_NULL_QUERIES,
                  CollectionFeature.NON_STANDARD_TOSTRING,
                  CollectionFeature.RESTRICTS_ELEMENTS,
                  CollectionFeature.REJECTS_DUPLICATES_AT_CREATION)
              .suppressing(SetHashCodeTester.getHashCodeMethods())
              .named("DiscreteRange.asSet, closed")
              .createTestSuite());

      return suite;
    }
  }

  abstract static class TestIntegerSetGenerator implements TestSetGenerator<Integer> {
    @Override public SampleElements<Integer> samples() {
      return new SampleElements<Integer>(1, 2, 3, 4, 5);
    }

    @Override public Set<Integer> create(Object... elements) {
      Integer[] array = new Integer[elements.length];
      int i = 0;
      for (Object e : elements) {
        array[i++] = (Integer) e;
      }
      return create(array);
    }

    protected abstract Set<Integer> create(Integer[] elements);

    @Override public Integer[] createArray(int length) {
      return new Integer[length];
    }

    @Override public List<Integer> order(List<Integer> insertionOrder) {
      return Ordering.natural().sortedCopy(insertionOrder);
    }
  }

  public void testNothing() {
    /*
     * It's a warning if a TestCase subclass contains no tests, so we add one.
     * Alternatively, we could stop extending TestCase, but I worry that someone
     * will add a test in the future and not realize that it's being ignored.
     */
  }
}

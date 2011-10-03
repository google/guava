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

import static com.google.common.collect.testing.google.AbstractMultisetSetCountTester.getSetCountDuplicateInitializingMethods;
import static com.google.common.collect.testing.google.MultisetIteratorTester.getIteratorDuplicateInitializingMethods;
import static com.google.common.collect.testing.google.MultisetReadsTester.getReadsDuplicateInitializingMethods;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.AnEnum;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.SortedMultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestEnumMultisetGenerator;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Collection tests for {@link Multiset} implementations.
 *
 * @author Jared Levy
 */
@GwtIncompatible("suite") // TODO(cpovirk): set up collect/gwt/suites version
public class MultisetCollectionTest extends TestCase {

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(MultisetTestSuiteBuilder.using(hashMultisetGenerator())
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.GENERAL_PURPOSE)
        .named("HashMultiset")
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(
        unmodifiableMultisetGenerator())
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .named("UnmodifiableTreeMultiset")
        .createTestSuite());

    suite.addTest(SortedMultisetTestSuiteBuilder
        .using(new TestStringMultisetGenerator() {
          @Override
          protected Multiset<String> create(String[] elements) {
            return TreeMultiset.create(Arrays.asList(elements));
          }

          @Override
          public List<String> order(List<String> insertionOrder) {
            return Ordering.natural().sortedCopy(insertionOrder);
          }
        })
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .named("TreeMultiset, Ordering.natural")
        .createTestSuite());
    

    suite.addTest(SortedMultisetTestSuiteBuilder
        .using(new TestStringMultisetGenerator() {
          @Override
          protected Multiset<String> create(String[] elements) {
            Multiset<String> result = TreeMultiset.create(Ordering.natural().nullsFirst());
            result.addAll(Arrays.asList(elements));
            return result;
          }

          @Override
          public List<String> order(List<String> insertionOrder) {
            return Ordering.natural().nullsFirst().sortedCopy(insertionOrder);
          }
        })
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_VALUES)
        .named("TreeMultiset, Ordering.natural.nullsFirst")
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(forSetGenerator())
        .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.REMOVE_OPERATIONS)
        .suppressing(getReadsDuplicateInitializingMethods())
        .suppressing(getSetCountDuplicateInitializingMethods())
        .suppressing(getIteratorDuplicateInitializingMethods())
        .named("ForSetMultiset")
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(
        concurrentMultisetGenerator())
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .named("ConcurrentHashMultiset")
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(enumMultisetGenerator())
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .named("EnumMultiset")
        .createTestSuite());

    suite.addTest(MultisetTestSuiteBuilder.using(intersectionGenerator())
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER)
        .named("IntersectionMultiset")
        .createTestSuite());

    suite.addTest(SortedMultisetTestSuiteBuilder.using(unmodifiableSortedMultisetGenerator())
        .withFeatures(CollectionSize.ANY, CollectionFeature.KNOWN_ORDER,
            CollectionFeature.ALLOWS_NULL_QUERIES)
        .named("UnmodifiableSortedTreeMultiset")
        .createTestSuite());

    return suite;
  }

  private static TestStringMultisetGenerator hashMultisetGenerator() {
    return new TestStringMultisetGenerator() {
      @Override protected Multiset<String> create(String[] elements) {
        return HashMultiset.create(asList(elements));
      }
    };
  }

  private static TestStringMultisetGenerator unmodifiableMultisetGenerator() {
    return new TestStringMultisetGenerator() {
      @Override protected Multiset<String> create(String[] elements) {
        return Multisets.unmodifiableMultiset(
            TreeMultiset.create(asList(elements)));
      }
      @Override public List<String> order(List<String> insertionOrder) {
        Collections.sort(insertionOrder);
        return insertionOrder;
      }
    };
  }

  private static TestStringMultisetGenerator unmodifiableSortedMultisetGenerator() {
    return new TestStringMultisetGenerator() {
      @Override protected Multiset<String> create(String[] elements) {
        return Multisets.unmodifiableSortedMultiset(
            TreeMultiset.create(asList(elements)));
      }
      @Override public List<String> order(List<String> insertionOrder) {
        Collections.sort(insertionOrder);
        return insertionOrder;
      }
    };
  }

  private static TestStringMultisetGenerator forSetGenerator() {
    return new TestStringMultisetGenerator() {
      @Override protected Multiset<String> create(String[] elements) {
        return Multisets.forSet(Sets.newHashSet(elements));
      }
    };
  }

  private static TestStringMultisetGenerator concurrentMultisetGenerator() {
    return new TestStringMultisetGenerator() {
      @Override protected Multiset<String> create(String[] elements) {
        return ConcurrentHashMultiset.create(asList(elements));
      }
    };
  }

  private static TestEnumMultisetGenerator enumMultisetGenerator() {
    return new TestEnumMultisetGenerator() {
      @Override protected Multiset<AnEnum> create(AnEnum[] elements) {
        return (elements.length == 0)
            ? EnumMultiset.create(AnEnum.class)
            : EnumMultiset.create(asList(elements));
      }
    };
  }

  private static TestStringMultisetGenerator intersectionGenerator() {
    return new TestStringMultisetGenerator() {
      @Override protected Multiset<String> create(String[] elements) {
        Multiset<String> multiset1 = LinkedHashMultiset.create();
        Multiset<String> multiset2 = LinkedHashMultiset.create();
        multiset1.add("only1");
        multiset2.add("only2");
        for (int i = 0; i < elements.length; i++) {
          multiset1.add(elements[i]);
          multiset2.add(elements[elements.length - 1 - i]);
        }
        if (elements.length > 0) {
          multiset1.add(elements[0]);
        }
        if (elements.length > 1) {
          /*
           * When a test requests a multiset with duplicates, our plan of
           * "add an extra item 0 to A and an extra item 1 to B" really means
           * "add an extra item 0 to A and B," which isn't what we want.
           */
          if (!elements[0].equals(elements[1])) {
            multiset2.add(elements[1], 2);
          }
        }
        return Multisets.intersection(multiset1, multiset2);
      }
    };
  }
}

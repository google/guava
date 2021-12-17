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

import static java.util.Arrays.asList;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.SortedMultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Collection tests on wrappers from {@link Multisets}.
 *
 * @author Jared Levy
 */
@GwtIncompatible // suite // TODO(cpovirk): set up collect/gwt/suites version
public class MultisetsCollectionTest extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(
        MultisetTestSuiteBuilder.using(unmodifiableMultisetGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .named("Multisets.unmodifiableMultiset[LinkedHashMultiset]")
            .createTestSuite());

    suite.addTest(
        SortedMultisetTestSuiteBuilder.using(unmodifiableSortedMultisetGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.ALLOWS_NULL_QUERIES)
            .named("Multisets.unmodifiableMultiset[TreeMultiset]")
            .createTestSuite());

    suite.addTest(
        MultisetTestSuiteBuilder.using(unionGenerator())
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES)
            .named("Multisets.union")
            .createTestSuite());

    suite.addTest(
        MultisetTestSuiteBuilder.using(intersectionGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.KNOWN_ORDER)
            .named("Multisets.intersection")
            .createTestSuite());

    suite.addTest(
        MultisetTestSuiteBuilder.using(sumGenerator())
            .withFeatures(CollectionSize.ANY, CollectionFeature.ALLOWS_NULL_VALUES)
            .named("Multisets.sum")
            .createTestSuite());

    suite.addTest(
        MultisetTestSuiteBuilder.using(differenceGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.KNOWN_ORDER)
            .named("Multisets.difference")
            .createTestSuite());

    suite.addTest(
        MultisetTestSuiteBuilder.using(filteredGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.SUPPORTS_ADD,
                CollectionFeature.SUPPORTS_REMOVE)
            .named("Multiset.filter[Multiset, Predicate]")
            .createTestSuite());

    return suite;
  }

  private static TestStringMultisetGenerator unmodifiableMultisetGenerator() {
    return new TestStringMultisetGenerator() {
      @Override
      protected Multiset<String> create(String[] elements) {
        return Multisets.unmodifiableMultiset(LinkedHashMultiset.create(asList(elements)));
      }

      @Override
      public List<String> order(List<String> insertionOrder) {
        List<String> order = new ArrayList<>();
        for (String s : insertionOrder) {
          int index = order.indexOf(s);
          if (index == -1) {
            order.add(s);
          } else {
            order.add(index, s);
          }
        }
        return order;
      }
    };
  }

  private static TestStringMultisetGenerator unmodifiableSortedMultisetGenerator() {
    return new TestStringMultisetGenerator() {
      @Override
      protected Multiset<String> create(String[] elements) {
        return Multisets.unmodifiableSortedMultiset(TreeMultiset.create(asList(elements)));
      }

      @Override
      public List<String> order(List<String> insertionOrder) {
        Collections.sort(insertionOrder);
        return insertionOrder;
      }
    };
  }

  private static TestStringMultisetGenerator unionGenerator() {
    return new TestStringMultisetGenerator() {
      @Override
      protected Multiset<String> create(String[] elements) {
        Multiset<String> multiset1 = LinkedHashMultiset.create();
        Multiset<String> multiset2 = LinkedHashMultiset.create();
        for (int i = 0; i < elements.length; i++) {
          String element = elements[i];
          if (multiset1.contains(element) || multiset2.contains(element)) {
            // add to both; the one already containing it will have more
            multiset1.add(element);
            multiset2.add(element);
          } else if (i % 2 == 0) {
            multiset1.add(elements[i]);
          } else {
            multiset2.add(elements[i]);
          }
        }
        return Multisets.union(multiset1, multiset2);
      }
    };
  }

  private static TestStringMultisetGenerator intersectionGenerator() {
    return new TestStringMultisetGenerator() {
      @Override
      protected Multiset<String> create(String[] elements) {
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
          if (!Objects.equal(elements[0], elements[1])) {
            multiset2.add(elements[1], 2);
          }
        }
        return Multisets.intersection(multiset1, multiset2);
      }
    };
  }

  private static TestStringMultisetGenerator sumGenerator() {
    return new TestStringMultisetGenerator() {
      @Override
      protected Multiset<String> create(String[] elements) {
        Multiset<String> multiset1 = LinkedHashMultiset.create();
        Multiset<String> multiset2 = LinkedHashMultiset.create();
        for (int i = 0; i < elements.length; i++) {
          // add to either; sum should contain all
          if (i % 2 == 0) {
            multiset1.add(elements[i]);
          } else {
            multiset2.add(elements[i]);
          }
        }
        return Multisets.sum(multiset1, multiset2);
      }
    };
  }

  private static TestStringMultisetGenerator differenceGenerator() {
    return new TestStringMultisetGenerator() {
      @Override
      protected Multiset<String> create(String[] elements) {
        Multiset<String> multiset1 = LinkedHashMultiset.create();
        Multiset<String> multiset2 = LinkedHashMultiset.create();
        multiset1.add("equalIn1");
        multiset1.add("fewerIn1");
        multiset2.add("equalIn1");
        multiset2.add("fewerIn1", 3);
        multiset2.add("onlyIn2", 2);
        for (int i = 0; i < elements.length; i++) {
          // add 1 more copy of each element to multiset1 than multiset2
          multiset1.add(elements[i], i + 2);
          multiset2.add(elements[i], i + 1);
        }
        return Multisets.difference(multiset1, multiset2);
      }
    };
  }

  private static final ImmutableMultiset<String> ELEMENTS_TO_FILTER_OUT =
      ImmutableMultiset.of("foobar", "bazfoo", "foobar", "foobar");

  private static final Predicate<String> PREDICATE =
      Predicates.not(Predicates.in(ELEMENTS_TO_FILTER_OUT));

  private static TestStringMultisetGenerator filteredGenerator() {
    return new TestStringMultisetGenerator() {
      @Override
      protected Multiset<String> create(String[] elements) {
        Multiset<String> multiset = LinkedHashMultiset.create();
        Collections.addAll(multiset, elements);
        multiset.addAll(ELEMENTS_TO_FILTER_OUT);
        return Multisets.filter(multiset, PREDICATE);
      }

      @Override
      public List<String> order(List<String> insertionOrder) {
        return Lists.newArrayList(LinkedHashMultiset.create(insertionOrder));
      }
    };
  }
}

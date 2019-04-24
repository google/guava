/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.collect.testing.testers.CollectionSpliteratorTester.getSpliteratorNotImmutableCollectionAllowsAddMethod;
import static com.google.common.collect.testing.testers.CollectionSpliteratorTester.getSpliteratorNotImmutableCollectionAllowsRemoveMethod;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.SetFeature;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Generates a test suite covering the {@link Set} implementations in the {@link java.util} package.
 * Can be subclassed to specify tests that should be suppressed.
 *
 * @author Kevin Bourrillion
 */
@GwtIncompatible
public class TestsForSetsInJavaUtil {
  public static Test suite() {
    return new TestsForSetsInJavaUtil().allTests();
  }

  public Test allTests() {
    TestSuite suite = new TestSuite("java.util Sets");
    suite.addTest(testsForCheckedNavigableSet());
    suite.addTest(testsForEmptySet());
    suite.addTest(testsForEmptyNavigableSet());
    suite.addTest(testsForEmptySortedSet());
    suite.addTest(testsForSingletonSet());
    suite.addTest(testsForHashSet());
    suite.addTest(testsForLinkedHashSet());
    suite.addTest(testsForEnumSet());
    suite.addTest(testsForSynchronizedNavigableSet());
    suite.addTest(testsForTreeSetNatural());
    suite.addTest(testsForTreeSetWithComparator());
    suite.addTest(testsForCopyOnWriteArraySet());
    suite.addTest(testsForUnmodifiableSet());
    suite.addTest(testsForUnmodifiableNavigableSet());
    suite.addTest(testsForCheckedSet());
    suite.addTest(testsForCheckedSortedSet());
    suite.addTest(testsForAbstractSet());
    suite.addTest(testsForBadlyCollidingHashSet());
    suite.addTest(testsForConcurrentSkipListSetNatural());
    suite.addTest(testsForConcurrentSkipListSetWithComparator());

    return suite;
  }

  protected Collection<Method> suppressForCheckedNavigableSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForEmptySet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForEmptyNavigableSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForEmptySortedSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForSingletonSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForHashSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForLinkedHashSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForEnumSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForSynchronizedNavigableSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForTreeSetNatural() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForTreeSetWithComparator() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForCopyOnWriteArraySet() {
    return asList(
        getSpliteratorNotImmutableCollectionAllowsAddMethod(),
        getSpliteratorNotImmutableCollectionAllowsRemoveMethod());
  }

  protected Collection<Method> suppressForUnmodifiableSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForUnmodifiableNavigableSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForCheckedSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForCheckedSortedSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForAbstractSet() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForConcurrentSkipListSetNatural() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForConcurrentSkipListSetWithComparator() {
    return Collections.emptySet();
  }

  public Test testsForCheckedNavigableSet() {
    return SortedSetTestSuiteBuilder.using(
            new TestStringSortedSetGenerator() {
              @Override
              public NavigableSet<String> create(String[] elements) {
                NavigableSet<String> innerSet = new TreeSet<>();
                Collections.addAll(innerSet, elements);
                return Collections.checkedNavigableSet(innerSet, String.class);
              }
            })
        .named("checkedNavigableSet/TreeSet, natural")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.RESTRICTS_ELEMENTS,
            CollectionSize.ANY)
        .suppressing(suppressForCheckedNavigableSet())
        .createTestSuite();
  }

  public Test testsForEmptySet() {
    return SetTestSuiteBuilder.using(
            new TestStringSetGenerator() {
              @Override
              public Set<String> create(String[] elements) {
                return Collections.emptySet();
              }
            })
        .named("emptySet")
        .withFeatures(CollectionFeature.SERIALIZABLE, CollectionSize.ZERO)
        .suppressing(suppressForEmptySet())
        .createTestSuite();
  }

  public Test testsForEmptyNavigableSet() {
    return SetTestSuiteBuilder.using(
            new TestStringSortedSetGenerator() {
              @Override
              public NavigableSet<String> create(String[] elements) {
                return Collections.emptyNavigableSet();
              }
            })
        .named("emptyNavigableSet")
        .withFeatures(CollectionFeature.SERIALIZABLE, CollectionSize.ZERO)
        .suppressing(suppressForEmptyNavigableSet())
        .createTestSuite();
  }

  public Test testsForEmptySortedSet() {
    return SetTestSuiteBuilder.using(
            new TestStringSortedSetGenerator() {
              @Override
              public SortedSet<String> create(String[] elements) {
                return Collections.emptySortedSet();
              }
            })
        .named("emptySortedSet")
        .withFeatures(CollectionFeature.SERIALIZABLE, CollectionSize.ZERO)
        .suppressing(suppressForEmptySortedSet())
        .createTestSuite();
  }

  public Test testsForSingletonSet() {
    return SetTestSuiteBuilder.using(
            new TestStringSetGenerator() {
              @Override
              public Set<String> create(String[] elements) {
                return Collections.singleton(elements[0]);
              }
            })
        .named("singleton")
        .withFeatures(
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionSize.ONE)
        .suppressing(suppressForSingletonSet())
        .createTestSuite();
  }

  public Test testsForHashSet() {
    return SetTestSuiteBuilder.using(
            new TestStringSetGenerator() {
              @Override
              public Set<String> create(String[] elements) {
                return new HashSet<>(MinimalCollection.of(elements));
              }
            })
        .named("HashSet")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionSize.ANY)
        .suppressing(suppressForHashSet())
        .createTestSuite();
  }

  public Test testsForLinkedHashSet() {
    return SetTestSuiteBuilder.using(
            new TestStringSetGenerator() {
              @Override
              public Set<String> create(String[] elements) {
                return new LinkedHashSet<>(MinimalCollection.of(elements));
              }
            })
        .named("LinkedHashSet")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionSize.ANY)
        .suppressing(suppressForLinkedHashSet())
        .createTestSuite();
  }

  public Test testsForEnumSet() {
    return SetTestSuiteBuilder.using(
            new TestEnumSetGenerator() {
              @Override
              public Set<AnEnum> create(AnEnum[] elements) {
                return (elements.length == 0)
                    ? EnumSet.noneOf(AnEnum.class)
                    : EnumSet.copyOf(MinimalCollection.of(elements));
              }
            })
        .named("EnumSet")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.RESTRICTS_ELEMENTS,
            CollectionSize.ANY)
        .suppressing(suppressForEnumSet())
        .createTestSuite();
  }

  /**
   * Tests regular NavigableSet behavior of synchronizedNavigableSet(treeSet); does not test the
   * fact that it's synchronized.
   */
  public Test testsForSynchronizedNavigableSet() {
    return NavigableSetTestSuiteBuilder.using(
            new TestStringSortedSetGenerator() {
              @Override
              public SortedSet<String> create(String[] elements) {
                NavigableSet<String> delegate = new TreeSet<>(MinimalCollection.of(elements));
                return Collections.synchronizedNavigableSet(delegate);
              }
            })
        .named("synchronizedNavigableSet/TreeSet, natural")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionSize.ANY)
        .suppressing(suppressForSynchronizedNavigableSet())
        .createTestSuite();
  }

  public Test testsForTreeSetNatural() {
    return NavigableSetTestSuiteBuilder.using(
            new TestStringSortedSetGenerator() {
              @Override
              public SortedSet<String> create(String[] elements) {
                return new TreeSet<>(MinimalCollection.of(elements));
              }
            })
        .named("TreeSet, natural")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionSize.ANY)
        .suppressing(suppressForTreeSetNatural())
        .createTestSuite();
  }

  public Test testsForTreeSetWithComparator() {
    return NavigableSetTestSuiteBuilder.using(
            new TestStringSortedSetGenerator() {
              @Override
              public SortedSet<String> create(String[] elements) {
                SortedSet<String> set = new TreeSet<>(arbitraryNullFriendlyComparator());
                Collections.addAll(set, elements);
                return set;
              }
            })
        .named("TreeSet, with comparator")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionSize.ANY)
        .suppressing(suppressForTreeSetWithComparator())
        .createTestSuite();
  }

  public Test testsForCopyOnWriteArraySet() {
    return SetTestSuiteBuilder.using(
            new TestStringSetGenerator() {
              @Override
              public Set<String> create(String[] elements) {
                return new CopyOnWriteArraySet<>(MinimalCollection.of(elements));
              }
            })
        .named("CopyOnWriteArraySet")
        .withFeatures(
            CollectionFeature.SUPPORTS_ADD,
            CollectionFeature.SUPPORTS_REMOVE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .suppressing(suppressForCopyOnWriteArraySet())
        .createTestSuite();
  }

  public Test testsForUnmodifiableSet() {
    return SetTestSuiteBuilder.using(
            new TestStringSetGenerator() {
              @Override
              public Set<String> create(String[] elements) {
                Set<String> innerSet = new HashSet<>();
                Collections.addAll(innerSet, elements);
                return Collections.unmodifiableSet(innerSet);
              }
            })
        .named("unmodifiableSet/HashSet")
        .withFeatures(
            CollectionFeature.NONE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionSize.ANY)
        .suppressing(suppressForUnmodifiableSet())
        .createTestSuite();
  }

  public Test testsForUnmodifiableNavigableSet() {
    return SetTestSuiteBuilder.using(
            new TestStringSortedSetGenerator() {
              @Override
              public NavigableSet<String> create(String[] elements) {
                NavigableSet<String> innerSet = new TreeSet<>();
                Collections.addAll(innerSet, elements);
                return Collections.unmodifiableNavigableSet(innerSet);
              }
            })
        .named("unmodifiableNavigableSet/TreeSet, natural")
        .withFeatures(
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.RESTRICTS_ELEMENTS,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .suppressing(suppressForUnmodifiableNavigableSet())
        .createTestSuite();
  }

  public Test testsForCheckedSet() {
    return SetTestSuiteBuilder.using(
            new TestStringSetGenerator() {
              @Override
              public Set<String> create(String[] elements) {
                Set<String> innerSet = new HashSet<>();
                Collections.addAll(innerSet, elements);
                return Collections.checkedSet(innerSet, String.class);
              }
            })
        .named("checkedSet/HashSet")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.RESTRICTS_ELEMENTS,
            CollectionSize.ANY)
        .suppressing(suppressForCheckedSet())
        .createTestSuite();
  }

  public Test testsForCheckedSortedSet() {
    return SortedSetTestSuiteBuilder.using(
            new TestStringSortedSetGenerator() {
              @Override
              public SortedSet<String> create(String[] elements) {
                SortedSet<String> innerSet = new TreeSet<>();
                Collections.addAll(innerSet, elements);
                return Collections.checkedSortedSet(innerSet, String.class);
              }
            })
        .named("checkedSortedSet/TreeSet, natural")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.RESTRICTS_ELEMENTS,
            CollectionSize.ANY)
        .suppressing(suppressForCheckedSortedSet())
        .createTestSuite();
  }

  public Test testsForAbstractSet() {
    return SetTestSuiteBuilder.using(
            new TestStringSetGenerator() {
              @Override
              protected Set<String> create(String[] elements) {
                final String[] deduped = dedupe(elements);
                return new AbstractSet<String>() {
                  @Override
                  public int size() {
                    return deduped.length;
                  }

                  @Override
                  public Iterator<String> iterator() {
                    return MinimalCollection.of(deduped).iterator();
                  }
                };
              }
            })
        .named("AbstractSet")
        .withFeatures(
            CollectionFeature.NONE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.KNOWN_ORDER, // in this case, anyway
            CollectionSize.ANY)
        .suppressing(suppressForAbstractSet())
        .createTestSuite();
  }

  public Test testsForBadlyCollidingHashSet() {
    return SetTestSuiteBuilder.using(
            new TestCollidingSetGenerator() {
              @Override
              public Set<Object> create(Object... elements) {
                return new HashSet<>(MinimalCollection.of(elements));
              }
            })
        .named("badly colliding HashSet")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionSize.SEVERAL)
        .suppressing(suppressForHashSet())
        .createTestSuite();
  }

  public Test testsForConcurrentSkipListSetNatural() {
    return SetTestSuiteBuilder.using(
            new TestStringSortedSetGenerator() {
              @Override
              public SortedSet<String> create(String[] elements) {
                return new ConcurrentSkipListSet<>(MinimalCollection.of(elements));
              }
            })
        .named("ConcurrentSkipListSet, natural")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .suppressing(suppressForConcurrentSkipListSetNatural())
        .createTestSuite();
  }

  public Test testsForConcurrentSkipListSetWithComparator() {
    return SetTestSuiteBuilder.using(
            new TestStringSortedSetGenerator() {
              @Override
              public SortedSet<String> create(String[] elements) {
                SortedSet<String> set =
                    new ConcurrentSkipListSet<>(arbitraryNullFriendlyComparator());
                Collections.addAll(set, elements);
                return set;
              }
            })
        .named("ConcurrentSkipListSet, with comparator")
        .withFeatures(
            SetFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.KNOWN_ORDER,
            CollectionSize.ANY)
        .suppressing(suppressForConcurrentSkipListSetWithComparator())
        .createTestSuite();
  }

  private static String[] dedupe(String[] elements) {
    Set<String> tmp = new LinkedHashSet<>();
    Collections.addAll(tmp, elements);
    return tmp.toArray(new String[0]);
  }

  static <T> Comparator<T> arbitraryNullFriendlyComparator() {
    return new NullFriendlyComparator<T>();
  }

  private static final class NullFriendlyComparator<T> implements Comparator<T>, Serializable {
    @Override
    public int compare(T left, T right) {
      return String.valueOf(left).compareTo(String.valueOf(right));
    }
  }
}

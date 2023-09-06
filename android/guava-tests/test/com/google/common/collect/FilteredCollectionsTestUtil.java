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
import static org.junit.Assert.assertThrows;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.testing.EqualsTester;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import junit.framework.TestCase;

/**
 * Class that contains nested abstract tests for filtered collection views, along with their
 * implementation helpers.
 *
 * @author Louis Wasserman
 */
/*
 * TODO(cpovirk): Should all the tests for filtered collections run under GWT, too? Currently, they
 * don't.
 */
public final class FilteredCollectionsTestUtil {
  private static final Predicate<Integer> EVEN =
      new Predicate<Integer>() {
        @Override
        public boolean apply(Integer input) {
          return input % 2 == 0;
        }
      };

  private static final Predicate<Integer> PRIME_DIGIT = Predicates.in(ImmutableSet.of(2, 3, 5, 7));

  private static final ImmutableList<? extends List<Integer>> SAMPLE_INPUTS =
      ImmutableList.of(
          ImmutableList.<Integer>of(),
          ImmutableList.of(1),
          ImmutableList.of(2),
          ImmutableList.of(2, 3),
          ImmutableList.of(1, 2),
          ImmutableList.of(3, 5),
          ImmutableList.of(2, 4),
          ImmutableList.of(1, 2, 3, 5, 6, 8, 9));

  /*
   * We have a whole series of abstract test classes that "stack", so e.g. the tests for filtered
   * NavigableSets inherit the tests for filtered Iterables, Collections, Sets, and SortedSets. The
   * actual implementation tests are further down.
   */

  public abstract static class AbstractFilteredIterableTest<C extends Iterable<Integer>>
      extends TestCase {
    abstract C createUnfiltered(Iterable<Integer> contents);

    abstract C filter(C elements, Predicate<? super Integer> predicate);

    public void testIterationOrderPreserved() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        C unfiltered = createUnfiltered(contents);
        C filtered = filter(unfiltered, EVEN);

        Iterator<Integer> filteredItr = filtered.iterator();
        for (Integer i : unfiltered) {
          if (EVEN.apply(i)) {
            assertTrue(filteredItr.hasNext());
            assertEquals(i, filteredItr.next());
          }
        }
        assertFalse(filteredItr.hasNext());
      }
    }
  }

  public abstract static class AbstractFilteredCollectionTest<C extends Collection<Integer>>
      extends AbstractFilteredIterableTest<C> {

    public void testReadsThroughAdd() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        C unfiltered = createUnfiltered(contents);
        C filterThenAdd = filter(unfiltered, EVEN);
        unfiltered.add(4);

        List<Integer> target = Lists.newArrayList(contents);
        target.add(4);
        C addThenFilter = filter(createUnfiltered(target), EVEN);

        assertThat(filterThenAdd).containsExactlyElementsIn(addThenFilter);
      }
    }

    public void testAdd() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        for (int toAdd = 0; toAdd < 10; toAdd++) {
          boolean expectedResult = createUnfiltered(contents).add(toAdd);

          C filtered = filter(createUnfiltered(contents), EVEN);
          try {
            assertEquals(expectedResult, filtered.add(toAdd));
            assertTrue(EVEN.apply(toAdd));
          } catch (IllegalArgumentException e) {
            assertFalse(EVEN.apply(toAdd));
          }
        }
      }
    }

    public void testRemove() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        for (int toRemove = 0; toRemove < 10; toRemove++) {
          assertEquals(
              contents.contains(toRemove) && EVEN.apply(toRemove),
              filter(createUnfiltered(contents), EVEN).remove(toRemove));
        }
      }
    }

    public void testContains() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        for (int i = 0; i < 10; i++) {
          assertEquals(
              EVEN.apply(i) && contents.contains(i),
              filter(createUnfiltered(contents), EVEN).contains(i));
        }
      }
    }

    public void testContainsOnDifferentType() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        assertFalse(filter(createUnfiltered(contents), EVEN).contains(new Object()));
      }
    }

    public void testAddAllFailsAtomically() {
      ImmutableList<Integer> toAdd = ImmutableList.of(2, 4, 3);
      for (List<Integer> contents : SAMPLE_INPUTS) {
        C filtered = filter(createUnfiltered(contents), EVEN);
        C filteredToModify = filter(createUnfiltered(contents), EVEN);

        assertThrows(IllegalArgumentException.class, () -> filteredToModify.addAll(toAdd));

        assertThat(filteredToModify).containsExactlyElementsIn(filtered);
      }
    }

    public void testAddToFilterFiltered() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        C unfiltered = createUnfiltered(contents);
        C filtered1 = filter(unfiltered, EVEN);
        C filtered2 = filter(filtered1, PRIME_DIGIT);

        assertThrows(IllegalArgumentException.class, () -> filtered2.add(4));

        assertThrows(IllegalArgumentException.class, () -> filtered2.add(3));

        filtered2.add(2);
      }
    }

    public void testClearFilterFiltered() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        C unfiltered = createUnfiltered(contents);
        C filtered1 = filter(unfiltered, EVEN);
        C filtered2 = filter(filtered1, PRIME_DIGIT);

        C inverseFiltered =
            filter(createUnfiltered(contents), Predicates.not(Predicates.and(EVEN, PRIME_DIGIT)));

        filtered2.clear();
        assertThat(unfiltered).containsExactlyElementsIn(inverseFiltered);
      }
    }
  }

  public abstract static class AbstractFilteredSetTest<C extends Set<Integer>>
      extends AbstractFilteredCollectionTest<C> {
    public void testEqualsAndHashCode() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        Set<Integer> expected = Sets.newHashSet();
        for (Integer i : contents) {
          if (EVEN.apply(i)) {
            expected.add(i);
          }
        }
        new EqualsTester()
            .addEqualityGroup(expected, filter(createUnfiltered(contents), EVEN))
            .testEquals();
      }
    }
  }

  public abstract static class AbstractFilteredSortedSetTest<C extends SortedSet<Integer>>
      extends AbstractFilteredSetTest<C> {
    public void testFirst() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        C filtered = filter(createUnfiltered(contents), EVEN);

        try {
          Integer first = filtered.first();
          assertFalse(filtered.isEmpty());
          assertEquals(Ordering.natural().min(filtered), first);
        } catch (NoSuchElementException e) {
          assertTrue(filtered.isEmpty());
        }
      }
    }

    public void testLast() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        C filtered = filter(createUnfiltered(contents), EVEN);

        try {
          Integer first = filtered.last();
          assertFalse(filtered.isEmpty());
          assertEquals(Ordering.natural().max(filtered), first);
        } catch (NoSuchElementException e) {
          assertTrue(filtered.isEmpty());
        }
      }
    }

    @SuppressWarnings("unchecked")
    public void testHeadSet() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        for (int i = 0; i < 10; i++) {
          assertEquals(
              filter((C) createUnfiltered(contents).headSet(i), EVEN),
              filter(createUnfiltered(contents), EVEN).headSet(i));
        }
      }
    }

    @SuppressWarnings("unchecked")
    public void testTailSet() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        for (int i = 0; i < 10; i++) {
          assertEquals(
              filter((C) createUnfiltered(contents).tailSet(i), EVEN),
              filter(createUnfiltered(contents), EVEN).tailSet(i));
        }
      }
    }

    @SuppressWarnings("unchecked")
    public void testSubSet() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        for (int i = 0; i < 10; i++) {
          for (int j = i; j < 10; j++) {
            assertEquals(
                filter((C) createUnfiltered(contents).subSet(i, j), EVEN),
                filter(createUnfiltered(contents), EVEN).subSet(i, j));
          }
        }
      }
    }
  }

  public abstract static class AbstractFilteredNavigableSetTest
      extends AbstractFilteredSortedSetTest<NavigableSet<Integer>> {

    public void testNavigableHeadSet() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        for (int i = 0; i < 10; i++) {
          for (boolean inclusive : ImmutableList.of(true, false)) {
            assertEquals(
                filter(createUnfiltered(contents).headSet(i, inclusive), EVEN),
                filter(createUnfiltered(contents), EVEN).headSet(i, inclusive));
          }
        }
      }
    }

    public void testNavigableTailSet() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        for (int i = 0; i < 10; i++) {
          for (boolean inclusive : ImmutableList.of(true, false)) {
            assertEquals(
                filter(createUnfiltered(contents).tailSet(i, inclusive), EVEN),
                filter(createUnfiltered(contents), EVEN).tailSet(i, inclusive));
          }
        }
      }
    }

    public void testNavigableSubSet() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        for (int i = 0; i < 10; i++) {
          for (int j = i + 1; j < 10; j++) {
            for (boolean fromInclusive : ImmutableList.of(true, false)) {
              for (boolean toInclusive : ImmutableList.of(true, false)) {
                NavigableSet<Integer> filterSubset =
                    filter(
                        createUnfiltered(contents).subSet(i, fromInclusive, j, toInclusive), EVEN);
                NavigableSet<Integer> subsetFilter =
                    filter(createUnfiltered(contents), EVEN)
                        .subSet(i, fromInclusive, j, toInclusive);
                assertEquals(filterSubset, subsetFilter);
              }
            }
          }
        }
      }
    }

    public void testDescendingSet() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        NavigableSet<Integer> filtered = filter(createUnfiltered(contents), EVEN);
        NavigableSet<Integer> unfiltered = createUnfiltered(filtered);

        assertThat(filtered.descendingSet())
            .containsExactlyElementsIn(unfiltered.descendingSet())
            .inOrder();
      }
    }

    public void testPollFirst() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        NavigableSet<Integer> filtered = filter(createUnfiltered(contents), EVEN);
        NavigableSet<Integer> unfiltered = createUnfiltered(filtered);

        assertEquals(unfiltered.pollFirst(), filtered.pollFirst());
        assertEquals(unfiltered, filtered);
      }
    }

    public void testPollLast() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        NavigableSet<Integer> filtered = filter(createUnfiltered(contents), EVEN);
        NavigableSet<Integer> unfiltered = createUnfiltered(filtered);

        assertEquals(unfiltered.pollLast(), filtered.pollLast());
        assertEquals(unfiltered, filtered);
      }
    }

    public void testNavigation() {
      for (List<Integer> contents : SAMPLE_INPUTS) {
        NavigableSet<Integer> filtered = filter(createUnfiltered(contents), EVEN);
        NavigableSet<Integer> unfiltered = createUnfiltered(filtered);
        for (int i = 0; i < 10; i++) {
          assertEquals(unfiltered.lower(i), filtered.lower(i));
          assertEquals(unfiltered.floor(i), filtered.floor(i));
          assertEquals(unfiltered.ceiling(i), filtered.ceiling(i));
          assertEquals(unfiltered.higher(i), filtered.higher(i));
        }
      }
    }
  }

  private FilteredCollectionsTestUtil() {}
}

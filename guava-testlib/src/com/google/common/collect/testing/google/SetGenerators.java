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

package com.google.common.collect.testing.google;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newTreeSet;
import static com.google.common.collect.testing.SampleElements.Strings.AFTER_LAST;
import static com.google.common.collect.testing.SampleElements.Strings.AFTER_LAST_2;
import static com.google.common.collect.testing.SampleElements.Strings.BEFORE_FIRST;
import static com.google.common.collect.testing.SampleElements.Strings.BEFORE_FIRST_2;
import static junit.framework.Assert.assertEquals;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.testing.TestCollectionGenerator;
import com.google.common.collect.testing.TestCollidingSetGenerator;
import com.google.common.collect.testing.TestIntegerSortedSetGenerator;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.TestStringSortedSetGenerator;
import com.google.common.collect.testing.TestUnhashableCollectionGenerator;
import com.google.common.collect.testing.UnhashableObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Generators of different types of sets and derived collections from sets.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @author Hayward Chan
 */
@GwtCompatible(emulated = true)
public class SetGenerators {

  public static class ImmutableSetCopyOfGenerator extends TestStringSetGenerator {
    @Override
    protected Set<String> create(String[] elements) {
      return ImmutableSet.copyOf(elements);
    }
  }

  public static class ImmutableSetUnsizedBuilderGenerator extends TestStringSetGenerator {
    @Override
    protected Set<String> create(String[] elements) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (String e : elements) {
        builder.add(e);
      }
      return builder.build();
    }
  }

  public static class ImmutableSetSizedBuilderGenerator extends TestStringSetGenerator {
    @Override
    protected Set<String> create(String[] elements) {
      ImmutableSet.Builder<String> builder =
          ImmutableSet.builderWithExpectedSize(Sets.newHashSet(elements).size());
      for (String e : elements) {
        builder.add(e);
      }
      return builder.build();
    }
  }

  public static class ImmutableSetTooBigBuilderGenerator extends TestStringSetGenerator {
    @Override
    protected Set<String> create(String[] elements) {
      ImmutableSet.Builder<String> builder =
          ImmutableSet.builderWithExpectedSize(Sets.newHashSet(elements).size() + 1);
      for (String e : elements) {
        builder.add(e);
      }
      return builder.build();
    }
  }

  public static class ImmutableSetTooSmallBuilderGenerator extends TestStringSetGenerator {
    @Override
    protected Set<String> create(String[] elements) {
      ImmutableSet.Builder<String> builder =
          ImmutableSet.builderWithExpectedSize(Math.max(0, Sets.newHashSet(elements).size() - 1));
      for (String e : elements) {
        builder.add(e);
      }
      return builder.build();
    }
  }

  public static class ImmutableSetWithBadHashesGenerator extends TestCollidingSetGenerator
      // Work around a GWT compiler bug.  Not explicitly listing this will
      // cause the createArray() method missing in the generated javascript.
      // TODO: Remove this once the GWT bug is fixed.
      implements TestCollectionGenerator<Object> {
    @Override
    public Set<Object> create(Object... elements) {
      return ImmutableSet.copyOf(elements);
    }
  }

  public static class DegeneratedImmutableSetGenerator extends TestStringSetGenerator {
    // Make sure we get what we think we're getting, or else this test
    // is pointless
    @SuppressWarnings("cast")
    @Override
    protected Set<String> create(String[] elements) {
      return (ImmutableSet<String>) ImmutableSet.of(elements[0], elements[0]);
    }
  }

  public static class ImmutableSortedSetCopyOfGenerator extends TestStringSortedSetGenerator {
    @Override
    protected SortedSet<String> create(String[] elements) {
      return ImmutableSortedSet.copyOf(elements);
    }
  }

  public static class ImmutableSortedSetHeadsetGenerator extends TestStringSortedSetGenerator {
    @Override
    protected SortedSet<String> create(String[] elements) {
      List<String> list = Lists.newArrayList(elements);
      list.add("zzz");
      return ImmutableSortedSet.copyOf(list).headSet("zzy");
    }
  }

  public static class ImmutableSortedSetTailsetGenerator extends TestStringSortedSetGenerator {
    @Override
    protected SortedSet<String> create(String[] elements) {
      List<String> list = Lists.newArrayList(elements);
      list.add("\0");
      return ImmutableSortedSet.copyOf(list).tailSet("\0\0");
    }
  }

  public static class ImmutableSortedSetSubsetGenerator extends TestStringSortedSetGenerator {
    @Override
    protected SortedSet<String> create(String[] elements) {
      List<String> list = Lists.newArrayList(elements);
      list.add("\0");
      list.add("zzz");
      return ImmutableSortedSet.copyOf(list).subSet("\0\0", "zzy");
    }
  }

  @GwtIncompatible // NavigableSet
  public static class ImmutableSortedSetDescendingGenerator extends TestStringSortedSetGenerator {
    @Override
    protected SortedSet<String> create(String[] elements) {
      return ImmutableSortedSet.<String>reverseOrder().add(elements).build().descendingSet();
    }
  }

  public static class ImmutableSortedSetExplicitComparator extends TestStringSetGenerator {

    private static final Comparator<String> STRING_REVERSED = Collections.reverseOrder();

    @Override
    protected SortedSet<String> create(String[] elements) {
      return ImmutableSortedSet.orderedBy(STRING_REVERSED).add(elements).build();
    }

    @Override
    public List<String> order(List<String> insertionOrder) {
      Collections.sort(insertionOrder, Collections.reverseOrder());
      return insertionOrder;
    }
  }

  public static class ImmutableSortedSetExplicitSuperclassComparatorGenerator
      extends TestStringSetGenerator {

    private static final Comparator<Comparable<?>> COMPARABLE_REVERSED = Collections.reverseOrder();

    @Override
    protected SortedSet<String> create(String[] elements) {
      return new ImmutableSortedSet.Builder<String>(COMPARABLE_REVERSED).add(elements).build();
    }

    @Override
    public List<String> order(List<String> insertionOrder) {
      Collections.sort(insertionOrder, Collections.reverseOrder());
      return insertionOrder;
    }
  }

  public static class ImmutableSortedSetReversedOrderGenerator extends TestStringSetGenerator {

    @Override
    protected SortedSet<String> create(String[] elements) {
      return ImmutableSortedSet.<String>reverseOrder()
          .addAll(Arrays.asList(elements).iterator())
          .build();
    }

    @Override
    public List<String> order(List<String> insertionOrder) {
      Collections.sort(insertionOrder, Collections.reverseOrder());
      return insertionOrder;
    }
  }

  public static class ImmutableSortedSetUnhashableGenerator extends TestUnhashableSetGenerator {
    @Override
    public Set<UnhashableObject> create(UnhashableObject[] elements) {
      return ImmutableSortedSet.copyOf(elements);
    }
  }

  public static class ImmutableSetAsListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      return ImmutableSet.copyOf(elements).asList();
    }
  }

  public static class ImmutableSortedSetAsListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      Comparator<String> comparator = createExplicitComparator(elements);
      ImmutableSet<String> set = ImmutableSortedSet.copyOf(comparator, Arrays.asList(elements));
      return set.asList();
    }
  }

  public static class ImmutableSortedSetSubsetAsListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      Comparator<String> comparator = createExplicitComparator(elements);
      ImmutableSortedSet.Builder<String> builder = ImmutableSortedSet.orderedBy(comparator);
      builder.add(BEFORE_FIRST);
      builder.add(elements);
      builder.add(AFTER_LAST);
      return builder.build().subSet(BEFORE_FIRST_2, AFTER_LAST).asList();
    }
  }

  @GwtIncompatible // NavigableSet
  public static class ImmutableSortedSetDescendingAsListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      Comparator<String> comparator = createExplicitComparator(elements).reverse();
      return ImmutableSortedSet.orderedBy(comparator)
          .add(elements)
          .build()
          .descendingSet()
          .asList();
    }
  }

  public static class ImmutableSortedSetAsListSubListGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      Comparator<String> comparator = createExplicitComparator(elements);
      ImmutableSortedSet.Builder<String> builder = ImmutableSortedSet.orderedBy(comparator);
      builder.add(BEFORE_FIRST);
      builder.add(elements);
      builder.add(AFTER_LAST);
      return builder.build().asList().subList(1, elements.length + 1);
    }
  }

  public static class ImmutableSortedSetSubsetAsListSubListGenerator
      extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      Comparator<String> comparator = createExplicitComparator(elements);
      ImmutableSortedSet.Builder<String> builder = ImmutableSortedSet.orderedBy(comparator);
      builder.add(BEFORE_FIRST);
      builder.add(BEFORE_FIRST_2);
      builder.add(elements);
      builder.add(AFTER_LAST);
      builder.add(AFTER_LAST_2);
      return builder
          .build()
          .subSet(BEFORE_FIRST_2, AFTER_LAST_2)
          .asList()
          .subList(1, elements.length + 1);
    }
  }

  public abstract static class TestUnhashableSetGenerator
      extends TestUnhashableCollectionGenerator<Set<UnhashableObject>>
      implements TestSetGenerator<UnhashableObject> {}

  private static Ordering<String> createExplicitComparator(String[] elements) {
    // Collapse equal elements, which Ordering.explicit() doesn't support, while
    // maintaining the ordering by first occurrence.
    Set<String> elementsPlus = Sets.newLinkedHashSet();
    elementsPlus.add(BEFORE_FIRST);
    elementsPlus.add(BEFORE_FIRST_2);
    elementsPlus.addAll(Arrays.asList(elements));
    elementsPlus.add(AFTER_LAST);
    elementsPlus.add(AFTER_LAST_2);
    return Ordering.explicit(Lists.newArrayList(elementsPlus));
  }

  /*
   * All the ContiguousSet generators below manually reject nulls here. In principle, we'd like to
   * defer that to Range, since it's ContiguousSet.create() that's used to create the sets. However,
   * that gets messy here, and we already have null tests for Range.
   */

  /*
   * These generators also rely on consecutive integer inputs (not necessarily in order, but no
   * holes).
   */

  // SetCreationTester has some tests that pass in duplicates. Dedup them.
  private static <E extends Comparable<? super E>> SortedSet<E> nullCheckedTreeSet(E[] elements) {
    SortedSet<E> set = newTreeSet();
    for (E element : elements) {
      // Explicit null check because TreeSet wrongly accepts add(null) when empty.
      set.add(checkNotNull(element));
    }
    return set;
  }

  public static class ContiguousSetGenerator extends AbstractContiguousSetGenerator {
    @Override
    protected SortedSet<Integer> create(Integer[] elements) {
      return checkedCreate(nullCheckedTreeSet(elements));
    }
  }

  public static class ContiguousSetHeadsetGenerator extends AbstractContiguousSetGenerator {
    @Override
    protected SortedSet<Integer> create(Integer[] elements) {
      SortedSet<Integer> set = nullCheckedTreeSet(elements);
      int tooHigh = set.isEmpty() ? 0 : set.last() + 1;
      set.add(tooHigh);
      return checkedCreate(set).headSet(tooHigh);
    }
  }

  public static class ContiguousSetTailsetGenerator extends AbstractContiguousSetGenerator {
    @Override
    protected SortedSet<Integer> create(Integer[] elements) {
      SortedSet<Integer> set = nullCheckedTreeSet(elements);
      int tooLow = set.isEmpty() ? 0 : set.first() - 1;
      set.add(tooLow);
      return checkedCreate(set).tailSet(tooLow + 1);
    }
  }

  public static class ContiguousSetSubsetGenerator extends AbstractContiguousSetGenerator {
    @Override
    protected SortedSet<Integer> create(Integer[] elements) {
      SortedSet<Integer> set = nullCheckedTreeSet(elements);
      if (set.isEmpty()) {
        /*
         * The (tooLow + 1, tooHigh) arguments below would be invalid because tooLow would be
         * greater than tooHigh.
         */
        return ContiguousSet.create(Range.openClosed(0, 1), DiscreteDomain.integers()).subSet(0, 1);
      }
      int tooHigh = set.last() + 1;
      int tooLow = set.first() - 1;
      set.add(tooHigh);
      set.add(tooLow);
      return checkedCreate(set).subSet(tooLow + 1, tooHigh);
    }
  }

  @GwtIncompatible // NavigableSet
  public static class ContiguousSetDescendingGenerator extends AbstractContiguousSetGenerator {
    @Override
    protected SortedSet<Integer> create(Integer[] elements) {
      return checkedCreate(nullCheckedTreeSet(elements)).descendingSet();
    }

    /** Sorts the elements in reverse natural order. */
    @Override
    public List<Integer> order(List<Integer> insertionOrder) {
      Collections.sort(insertionOrder, Ordering.natural().reverse());
      return insertionOrder;
    }
  }

  private abstract static class AbstractContiguousSetGenerator
      extends TestIntegerSortedSetGenerator {
    protected final ContiguousSet<Integer> checkedCreate(SortedSet<Integer> elementsSet) {
      List<Integer> elements = newArrayList(elementsSet);
      /*
       * A ContiguousSet can't have holes. If a test demands a hole, it should be changed so that it
       * doesn't need one, or it should be suppressed for ContiguousSet.
       */
      for (int i = 0; i < elements.size() - 1; i++) {
        assertEquals(elements.get(i) + 1, (int) elements.get(i + 1));
      }
      Range<Integer> range =
          elements.isEmpty() ? Range.closedOpen(0, 0) : Range.encloseAll(elements);
      return ContiguousSet.create(range, DiscreteDomain.integers());
    }
  }
}

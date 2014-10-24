/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.NavigableSetTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.SerializableTester;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link ImmutableRangeSet}.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible("ImmutableRangeSet")
public class ImmutableRangeSetTest extends AbstractRangeSetTest {
  @SuppressWarnings("unchecked") // varargs
  private static final ImmutableSet<Range<Integer>> RANGES = ImmutableSet.of(
      Range.<Integer>all(),
      Range.closedOpen(3, 5),
      Range.singleton(1),
      Range.lessThan(2),
      Range.greaterThan(10),
      Range.atMost(4),
      Range.atLeast(3),
      Range.closed(4, 6),
      Range.closedOpen(1, 3),
      Range.openClosed(5, 7),
      Range.open(3, 4));
  
  static final class ImmutableRangeSetIntegerAsSetGenerator implements TestSetGenerator<Integer> {
    @Override
    public SampleElements<Integer> samples() {
      return new SampleElements<Integer>(1, 4, 3, 2, 5);
    }

    @Override
    public Integer[] createArray(int length) {
      return new Integer[length];
    }

    @Override
    public Iterable<Integer> order(List<Integer> insertionOrder) {
      return Ordering.natural().sortedCopy(insertionOrder);
    }

    @Override
    public Set<Integer> create(Object... elements) {
      ImmutableRangeSet.Builder<Integer> builder = ImmutableRangeSet.builder();
      for (Object o : elements) {
        Integer i = (Integer) o;
        builder.add(Range.singleton(i));
      }
      return builder.build().asSet(DiscreteDomain.integers());
    }
  }

  static final class ImmutableRangeSetBigIntegerAsSetGenerator
      implements TestSetGenerator<BigInteger> {
    @Override
    public SampleElements<BigInteger> samples() {
      return new SampleElements<BigInteger>(
          BigInteger.valueOf(1),
          BigInteger.valueOf(4),
          BigInteger.valueOf(3),
          BigInteger.valueOf(2),
          BigInteger.valueOf(5));
    }

    @Override
    public BigInteger[] createArray(int length) {
      return new BigInteger[length];
    }

    @Override
    public Iterable<BigInteger> order(List<BigInteger> insertionOrder) {
      return Ordering.natural().sortedCopy(insertionOrder);
    }

    @Override
    public Set<BigInteger> create(Object... elements) {
      ImmutableRangeSet.Builder<BigInteger> builder = ImmutableRangeSet.builder();
      for (Object o : elements) {
        BigInteger i = (BigInteger) o;
        builder.add(Range.closedOpen(i, i.add(BigInteger.ONE)));
      }
      return builder.build().asSet(DiscreteDomain.bigIntegers());
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(ImmutableRangeSetTest.class);
    suite.addTest(NavigableSetTestSuiteBuilder.using(new ImmutableRangeSetIntegerAsSetGenerator())
        .named("ImmutableRangeSet.asSet[DiscreteDomain.integers[]]")
        .withFeatures(
            CollectionSize.ANY,
            CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
            CollectionFeature.ALLOWS_NULL_QUERIES,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.NON_STANDARD_TOSTRING,
            CollectionFeature.SERIALIZABLE)
        .createTestSuite());

    suite.addTest(NavigableSetTestSuiteBuilder.using(
          new ImmutableRangeSetBigIntegerAsSetGenerator())
        .named("ImmutableRangeSet.asSet[DiscreteDomain.bigIntegers[]]")
        .withFeatures(
            CollectionSize.ANY,
            CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
            CollectionFeature.ALLOWS_NULL_QUERIES,
            CollectionFeature.KNOWN_ORDER,
            CollectionFeature.NON_STANDARD_TOSTRING,
            CollectionFeature.SERIALIZABLE)
        .createTestSuite());
    return suite;
  }

  public void testEmpty() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.of();

    assertThat(rangeSet.asRanges()).isEmpty();
    assertEquals(ImmutableRangeSet.<Integer>all(), rangeSet.complement());
    assertFalse(rangeSet.contains(0));
    assertFalse(rangeSet.encloses(Range.singleton(0)));
    assertTrue(rangeSet.enclosesAll(rangeSet));
    assertTrue(rangeSet.isEmpty());
  }

  public void testAll() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.all();

    assertThat(rangeSet.asRanges()).contains(Range.<Integer>all());
    assertTrue(rangeSet.contains(0));
    assertTrue(rangeSet.encloses(Range.<Integer>all()));
    assertTrue(rangeSet.enclosesAll(rangeSet));
    assertEquals(ImmutableRangeSet.<Integer>of(), rangeSet.complement());
  }

  public void testSingleBoundedRange() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.of(Range.closedOpen(1, 5));

    assertThat(rangeSet.asRanges()).contains(Range.closedOpen(1, 5));

    assertTrue(rangeSet.encloses(Range.closed(3, 4)));
    assertTrue(rangeSet.encloses(Range.closedOpen(1, 4)));
    assertTrue(rangeSet.encloses(Range.closedOpen(1, 5)));
    assertFalse(rangeSet.encloses(Range.greaterThan(2)));

    assertTrue(rangeSet.contains(3));
    assertFalse(rangeSet.contains(5));
    assertFalse(rangeSet.contains(0));

    RangeSet<Integer> expectedComplement = TreeRangeSet.create();
    expectedComplement.add(Range.lessThan(1));
    expectedComplement.add(Range.atLeast(5));

    assertEquals(expectedComplement, rangeSet.complement());
  }

  public void testSingleBoundedBelowRange() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.of(Range.greaterThan(2));

    assertThat(rangeSet.asRanges()).contains(Range.greaterThan(2));

    assertTrue(rangeSet.encloses(Range.closed(3, 4)));
    assertTrue(rangeSet.encloses(Range.greaterThan(3)));
    assertFalse(rangeSet.encloses(Range.closedOpen(1, 5)));

    assertTrue(rangeSet.contains(3));
    assertTrue(rangeSet.contains(5));
    assertFalse(rangeSet.contains(0));
    assertFalse(rangeSet.contains(2));

    assertEquals(ImmutableRangeSet.of(Range.atMost(2)), rangeSet.complement());
  }

  public void testSingleBoundedAboveRange() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.of(Range.atMost(3));

    assertThat(rangeSet.asRanges()).contains(Range.atMost(3));

    assertTrue(rangeSet.encloses(Range.closed(2, 3)));
    assertTrue(rangeSet.encloses(Range.lessThan(1)));
    assertFalse(rangeSet.encloses(Range.closedOpen(1, 5)));

    assertTrue(rangeSet.contains(3));
    assertTrue(rangeSet.contains(0));
    assertFalse(rangeSet.contains(4));
    assertFalse(rangeSet.contains(5));

    assertEquals(ImmutableRangeSet.of(Range.greaterThan(3)), rangeSet.complement());
  }

  public void testMultipleBoundedRanges() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.<Integer>builder()
        .add(Range.closed(5, 8)).add(Range.closedOpen(1, 3)).build();

    assertThat(rangeSet.asRanges())
        .containsExactly(Range.closedOpen(1, 3), Range.closed(5, 8)).inOrder();

    assertTrue(rangeSet.encloses(Range.closed(1, 2)));
    assertTrue(rangeSet.encloses(Range.open(5, 8)));
    assertFalse(rangeSet.encloses(Range.closed(1, 8)));
    assertFalse(rangeSet.encloses(Range.greaterThan(5)));

    RangeSet<Integer> expectedComplement = ImmutableRangeSet.<Integer>builder()
        .add(Range.lessThan(1))
        .add(Range.closedOpen(3, 5))
        .add(Range.greaterThan(8))
        .build();

    assertEquals(expectedComplement, rangeSet.complement());
  }

  public void testMultipleBoundedBelowRanges() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.<Integer>builder()
        .add(Range.greaterThan(6)).add(Range.closedOpen(1, 3)).build();

    assertThat(rangeSet.asRanges())
        .containsExactly(Range.closedOpen(1, 3), Range.greaterThan(6)).inOrder();

    assertTrue(rangeSet.encloses(Range.closed(1, 2)));
    assertTrue(rangeSet.encloses(Range.open(6, 8)));
    assertFalse(rangeSet.encloses(Range.closed(1, 8)));
    assertFalse(rangeSet.encloses(Range.greaterThan(5)));

    RangeSet<Integer> expectedComplement = ImmutableRangeSet.<Integer>builder()
        .add(Range.lessThan(1))
        .add(Range.closed(3, 6))
        .build();

    assertEquals(expectedComplement, rangeSet.complement());
  }

  public void testMultipleBoundedAboveRanges() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.<Integer>builder()
        .add(Range.atMost(0)).add(Range.closedOpen(2, 5)).build();

    assertThat(rangeSet.asRanges())
        .containsExactly(Range.atMost(0), Range.closedOpen(2, 5)).inOrder();

    assertTrue(rangeSet.encloses(Range.closed(2, 4)));
    assertTrue(rangeSet.encloses(Range.open(-5, -2)));
    assertFalse(rangeSet.encloses(Range.closed(1, 8)));
    assertFalse(rangeSet.encloses(Range.greaterThan(5)));

    RangeSet<Integer> expectedComplement = ImmutableRangeSet.<Integer>builder()
        .add(Range.open(0, 2))
        .add(Range.atLeast(5))
        .build();

    assertEquals(expectedComplement, rangeSet.complement());
  }

  public void testAddUnsupported() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.<Integer>builder()
        .add(Range.closed(5, 8)).add(Range.closedOpen(1, 3)).build();

    try {
      rangeSet.add(Range.open(3, 4));
      fail();
    } catch (UnsupportedOperationException expected) {
      // success
    }
  }

  public void testAddAllUnsupported() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.<Integer>builder()
        .add(Range.closed(5, 8)).add(Range.closedOpen(1, 3)).build();

    try {
      rangeSet.addAll(ImmutableRangeSet.<Integer>of());
      fail();
    } catch (UnsupportedOperationException expected) {
      // success
    }
  }

  public void testRemoveUnsupported() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.<Integer>builder()
        .add(Range.closed(5, 8)).add(Range.closedOpen(1, 3)).build();

    try {
      rangeSet.remove(Range.closed(6, 7));
      fail();
    } catch (UnsupportedOperationException expected) {
      // success
    }
  }

  public void testRemoveAllUnsupported() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.<Integer>builder()
        .add(Range.closed(5, 8)).add(Range.closedOpen(1, 3)).build();

    try {
      rangeSet.removeAll(ImmutableRangeSet.<Integer>of());
      fail();
    } catch (UnsupportedOperationException expected) {
      // success
    }

    try {
      rangeSet.removeAll(ImmutableRangeSet.of(Range.closed(6, 8)));
      fail();
    } catch (UnsupportedOperationException expected) {
      // success
    }
  }

  public void testExhaustive() {
    @SuppressWarnings("unchecked")
    ImmutableSet<Range<Integer>> ranges = ImmutableSet.of(
        Range.<Integer>all(),
        Range.<Integer>closedOpen(3, 5),
        Range.singleton(1),
        Range.lessThan(2),
        Range.greaterThan(10),
        Range.atMost(4),
        Range.atLeast(3),
        Range.closed(4, 6),
        Range.closedOpen(1, 3),
        Range.openClosed(5, 7),
        Range.open(3, 4));
    for (Set<Range<Integer>> subset : Sets.powerSet(ranges)) {
      RangeSet<Integer> mutable = TreeRangeSet.create();
      ImmutableRangeSet.Builder<Integer> builder = ImmutableRangeSet.builder();

      int expectedRanges = 0;
      for (Range<Integer> range : subset) {
        boolean overlaps = false;
        for (Range<Integer> other : mutable.asRanges()) {
          if (other.isConnected(range) && !other.intersection(range).isEmpty()) {
            overlaps = true;
          }
        }

        try {
          builder.add(range);
          assertFalse(overlaps);
          mutable.add(range);
        } catch (IllegalArgumentException e) {
          assertTrue(overlaps);
        }
      }

      ImmutableRangeSet<Integer> built = builder.build();
      assertEquals(mutable, built);
      assertEquals(ImmutableRangeSet.copyOf(mutable), built);
      assertEquals(mutable.complement(), built.complement());

      for (int i = 0; i <= 11; i++) {
        assertEquals(mutable.contains(i), built.contains(i));
      }

      SerializableTester.reserializeAndAssert(built);
    }
  }

  public void testAsSet() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.<Integer>builder()
        .add(Range.closed(2, 4))
        .add(Range.open(6, 7))
        .add(Range.closedOpen(8, 10))
        .add(Range.openClosed(15, 17))
        .build();
    ImmutableSortedSet<Integer> expectedSet = ImmutableSortedSet.of(2, 3, 4, 8, 9, 16, 17);
    ImmutableSortedSet<Integer> asSet = rangeSet.asSet(DiscreteDomain.integers());
    assertEquals(expectedSet, asSet);
    assertThat(asSet).containsExactlyElementsIn(expectedSet).inOrder();
    assertTrue(asSet.containsAll(expectedSet));
    SerializableTester.reserializeAndAssert(asSet);
  }

  public void testAsSetHeadSet() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.<Integer>builder()
        .add(Range.closed(2, 4))
        .add(Range.open(6, 7))
        .add(Range.closedOpen(8, 10))
        .add(Range.openClosed(15, 17))
        .build();

    ImmutableSortedSet<Integer> expectedSet = ImmutableSortedSet.of(2, 3, 4, 8, 9, 16, 17);
    ImmutableSortedSet<Integer> asSet = rangeSet.asSet(DiscreteDomain.integers());

    for (int i = 0; i <= 20; i++) {
      assertEquals(asSet.headSet(i, false), expectedSet.headSet(i, false));
      assertEquals(asSet.headSet(i, true), expectedSet.headSet(i, true));
    }
  }

  public void testAsSetTailSet() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.<Integer>builder()
        .add(Range.closed(2, 4))
        .add(Range.open(6, 7))
        .add(Range.closedOpen(8, 10))
        .add(Range.openClosed(15, 17))
        .build();

    ImmutableSortedSet<Integer> expectedSet = ImmutableSortedSet.of(2, 3, 4, 8, 9, 16, 17);
    ImmutableSortedSet<Integer> asSet = rangeSet.asSet(DiscreteDomain.integers());

    for (int i = 0; i <= 20; i++) {
      assertEquals(asSet.tailSet(i, false), expectedSet.tailSet(i, false));
      assertEquals(asSet.tailSet(i, true), expectedSet.tailSet(i, true));
    }
  }

  public void testAsSetSubSet() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.<Integer>builder()
        .add(Range.closed(2, 4))
        .add(Range.open(6, 7))
        .add(Range.closedOpen(8, 10))
        .add(Range.openClosed(15, 17))
        .build();

    ImmutableSortedSet<Integer> expectedSet = ImmutableSortedSet.of(2, 3, 4, 8, 9, 16, 17);
    ImmutableSortedSet<Integer> asSet = rangeSet.asSet(DiscreteDomain.integers());

    for (int i = 0; i <= 20; i++) {
      for (int j = i + 1; j <= 20; j++) {
        assertEquals(expectedSet.subSet(i, false, j, false),
            asSet.subSet(i, false, j, false));
        assertEquals(expectedSet.subSet(i, true, j, false),
            asSet.subSet(i, true, j, false));
        assertEquals(expectedSet.subSet(i, false, j, true),
            asSet.subSet(i, false, j, true));
        assertEquals(expectedSet.subSet(i, true, j, true),
            asSet.subSet(i, true, j, true));
      }
    }
  }
  
  public void testSubRangeSet() {
    ImmutableList.Builder<Range<Integer>> rangesBuilder = ImmutableList.builder();
    rangesBuilder.add(Range.<Integer>all());
    for (int i = -2; i <= 2; i++) {
      for (BoundType boundType : BoundType.values()) {
        rangesBuilder.add(Range.upTo(i, boundType));
        rangesBuilder.add(Range.downTo(i, boundType));
      }
      for (int j = i + 1; j <= 2; j++) {
        for (BoundType lbType : BoundType.values()) {
          for (BoundType ubType : BoundType.values()) {
            rangesBuilder.add(Range.range(i, lbType, j, ubType));
          }
        }
      }
    }
    ImmutableList<Range<Integer>> ranges = rangesBuilder.build();
    for (int i = -2; i <= 2; i++) {
      rangesBuilder.add(Range.closedOpen(i, i));
      rangesBuilder.add(Range.openClosed(i, i));
    }
    ImmutableList<Range<Integer>> subRanges = rangesBuilder.build();
    for (Range<Integer> range1 : ranges) {
      for (Range<Integer> range2 : ranges) {
        if (!range1.isConnected(range2) || range1.intersection(range2).isEmpty()) {
          ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.<Integer>builder()
              .add(range1)
              .add(range2)
              .build();
          for (Range<Integer> subRange : subRanges) {
            RangeSet<Integer> expected = TreeRangeSet.create();
            for (Range<Integer> range : rangeSet.asRanges()) {
              if (range.isConnected(subRange)) {
                expected.add(range.intersection(subRange));
              }
            }
            ImmutableRangeSet<Integer> subRangeSet = rangeSet.subRangeSet(subRange);
            assertEquals(expected, subRangeSet);
            assertEquals(expected.asRanges(), subRangeSet.asRanges());
            if (!expected.isEmpty()) {
              assertEquals(expected.span(), subRangeSet.span());
            }
            for (int i = -3; i <= 3; i++) {
              assertEquals(expected.contains(i), subRangeSet.contains(i));
            }
          }
        }
      }
    }
  }
}

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

import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.SerializableTester;

import java.util.Set;

/**
 * Tests for {@link ImmutableRangeSet}.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible("ImmutableRangeSet")
public class ImmutableRangeSetTest extends AbstractRangeSetTest {
  public void testEmpty() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.of();

    ASSERT.that(rangeSet.asRanges()).isEmpty();
    assertEquals(ImmutableRangeSet.<Integer>all(), rangeSet.complement());
    assertFalse(rangeSet.contains(0));
    assertFalse(rangeSet.encloses(Range.singleton(0)));
    assertTrue(rangeSet.enclosesAll(rangeSet));
    assertTrue(rangeSet.isEmpty());
  }

  public void testAll() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.all();

    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.all());
    assertTrue(rangeSet.contains(0));
    assertTrue(rangeSet.encloses(Range.<Integer>all()));
    assertTrue(rangeSet.enclosesAll(rangeSet));
    assertEquals(ImmutableRangeSet.<Integer>of(), rangeSet.complement());
  }

  public void testSingleBoundedRange() {
    ImmutableRangeSet<Integer> rangeSet = ImmutableRangeSet.of(Range.closedOpen(1, 5));

    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closedOpen(1, 5));

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

    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.greaterThan(2));

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

    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.atMost(3));

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

    ASSERT.that(rangeSet.asRanges()).hasContentsInOrder(Range.closedOpen(1, 3), Range.closed(5, 8));

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

    ASSERT.that(rangeSet.asRanges())
        .hasContentsInOrder(Range.closedOpen(1, 3), Range.greaterThan(6));

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

    ASSERT.that(rangeSet.asRanges())
        .hasContentsInOrder(Range.atMost(0), Range.closedOpen(2, 5));

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
}

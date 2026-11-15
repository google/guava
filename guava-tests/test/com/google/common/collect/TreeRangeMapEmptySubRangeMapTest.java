/*
 * Copyright (C) 2026 The Guava Authors
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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.EqualsTester;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/** Tests equality of empty nested {@link TreeRangeMap} views. */
@GwtIncompatible // NavigableMap
@NullUnmarked
public class TreeRangeMapEmptySubRangeMapTest extends TestCase {
  public void testDisconnectedSubRangeMapEquals() {
    TreeRangeMap<Integer, String> rangeMap = createRangeMap();
    RangeMap<Integer, String> disconnected =
        rangeMap.subRangeMap(Range.closed(1, 2)).subRangeMap(Range.closed(3, 4));

    new EqualsTester()
        .addEqualityGroup(
            disconnected,
            disconnected.subRangeMap(Range.all()),
            TreeRangeMap.<Integer, String>create(),
            ImmutableRangeMap.<Integer, String>of(),
            rangeMap.subRangeMap(Range.closed(20, 30)),
            rangeMap.subRangeMap(Range.closedOpen(1, 2)).subRangeMap(Range.closed(2, 3)))
        .addEqualityGroup(rangeMap)
        .testEquals();
  }

  public void testDisconnectedSubRangeMapHashCode() {
    RangeMap<Integer, String> disconnected =
        createRangeMap().subRangeMap(Range.closed(1, 2)).subRangeMap(Range.closed(3, 4));

    assertEquals(0, disconnected.asMapOfRanges().hashCode());
    assertEquals(disconnected.asMapOfRanges().hashCode(), disconnected.hashCode());
  }

  public void testOrdinaryEmptySubRangeMapEquals() {
    TreeRangeMap<Integer, String> rangeMap = createRangeMap();
    RangeMap<Integer, String> empty = rangeMap.subRangeMap(Range.closed(20, 30));

    new EqualsTester()
        .addEqualityGroup(
            empty, TreeRangeMap.<Integer, String>create(), ImmutableRangeMap.<Integer, String>of())
        .addEqualityGroup(rangeMap)
        .testEquals();
    assertEquals(empty.asMapOfRanges().hashCode(), empty.hashCode());
  }

  public void testNonemptySubRangeMapEquals() {
    TreeRangeMap<Integer, String> rangeMap = createRangeMap();
    RangeMap<Integer, String> subRangeMap = rangeMap.subRangeMap(Range.closed(1, 2));

    new EqualsTester()
        .addEqualityGroup(subRangeMap, ImmutableRangeMap.of(Range.closed(1, 2), "a"))
        .addEqualityGroup(rangeMap)
        .addEqualityGroup(ImmutableRangeMap.of(Range.closed(1, 2), "b"))
        .addEqualityGroup(ImmutableRangeMap.<Integer, String>of())
        .testEquals();
    assertEquals(subRangeMap.asMapOfRanges().hashCode(), subRangeMap.hashCode());
  }

  private static TreeRangeMap<Integer, String> createRangeMap() {
    TreeRangeMap<Integer, String> rangeMap = TreeRangeMap.create();
    rangeMap.put(Range.closed(0, 10), "a");
    return rangeMap;
  }
}

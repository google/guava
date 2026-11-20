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

import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import java.util.NoSuchElementException;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/** Tests the span contract for empty restrictions of a range map. */
@GwtIncompatible // NavigableMap
@NullUnmarked
public class TreeRangeMapEmptySpanTest extends TestCase {
  public void testEmptyClosedOpenRestrictionInsideMapping() {
    assertEmptySpanThrows(mapWithMapping().subRangeMap(Range.closedOpen(5, 5)));
  }

  public void testEmptyOpenClosedRestrictionInsideMapping() {
    assertEmptySpanThrows(mapWithMapping().subRangeMap(Range.openClosed(5, 5)));
  }

  public void testNestedEmptyRestrictionInsideMapping() {
    assertEmptySpanThrows(
        mapWithMapping().subRangeMap(Range.closed(1, 9)).subRangeMap(Range.closedOpen(5, 5)));
  }

  public void testEmptyRestrictionAtIncludedUpperEndpoint() {
    assertEmptySpanThrows(mapWithMapping().subRangeMap(Range.closedOpen(10, 10)));
  }

  public void testEmptyParent() {
    TreeRangeMap<Integer, String> map = TreeRangeMap.create();
    assertEmptySpanThrows(map);
    assertEmptySpanThrows(map.subRangeMap(Range.closedOpen(5, 5)));
  }

  public void testEmptyRestrictionOutsideMapping() {
    assertEmptySpanThrows(mapWithMapping().subRangeMap(Range.closedOpen(20, 20)));
  }

  public void testNonemptyRestrictionAndParentUnchanged() {
    TreeRangeMap<Integer, String> map = mapWithMapping();
    assertEquals(Range.closed(3, 7), map.subRangeMap(Range.closed(3, 7)).span());
    assertEquals(Range.singleton(5), map.subRangeMap(Range.singleton(5)).span());
    assertEquals(Range.closed(0, 10), map.span());
    assertEquals(ImmutableMap.of(Range.closed(0, 10), "a"), map.asMapOfRanges());
  }

  public void testRetainedNonemptyViewBecomesEmptyAfterRemoval() {
    TreeRangeMap<Integer, String> map = mapWithMapping();
    RangeMap<Integer, String> view = map.subRangeMap(Range.closed(3, 7));
    map.remove(Range.closed(3, 7));
    assertEmptySpanThrows(view);
    assertEquals("a", map.get(2));
    assertEquals("a", map.get(8));
  }

  private static TreeRangeMap<Integer, String> mapWithMapping() {
    TreeRangeMap<Integer, String> map = TreeRangeMap.create();
    map.put(Range.closed(0, 10), "a");
    return map;
  }

  private static void assertEmptySpanThrows(RangeMap<Integer, String> view) {
    assertTrue(view.asMapOfRanges().isEmpty());
    assertTrue(view.asDescendingMapOfRanges().isEmpty());
    assertNull(view.get(5));
    assertThrows(NoSuchElementException.class, view::span);
  }
}

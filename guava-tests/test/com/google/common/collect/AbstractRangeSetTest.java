/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtIncompatible;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import junit.framework.TestCase;

/**
 * Base class for {@link RangeSet} tests.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible // TreeRangeSet
public abstract class AbstractRangeSetTest extends TestCase {
  public static void testInvariants(RangeSet<?> rangeSet) {
    testInvariantsInternal(rangeSet);
    testInvariantsInternal(rangeSet.complement());
  }

  private static <C extends Comparable> void testInvariantsInternal(RangeSet<C> rangeSet) {
    assertEquals(rangeSet.asRanges().isEmpty(), rangeSet.isEmpty());
    assertEquals(rangeSet.asDescendingSetOfRanges().isEmpty(), rangeSet.isEmpty());
    assertEquals(!rangeSet.asRanges().iterator().hasNext(), rangeSet.isEmpty());
    assertEquals(!rangeSet.asDescendingSetOfRanges().iterator().hasNext(), rangeSet.isEmpty());

    List<Range<C>> asRanges = ImmutableList.copyOf(rangeSet.asRanges());

    // test that connected ranges are coalesced
    for (int i = 0; i + 1 < asRanges.size(); i++) {
      Range<C> range1 = asRanges.get(i);
      Range<C> range2 = asRanges.get(i + 1);
      assertFalse(range1.isConnected(range2));
    }

    // test that there are no empty ranges
    for (Range<C> range : asRanges) {
      assertFalse(range.isEmpty());
    }

    // test that the RangeSet's span is the span of all the ranges
    Iterator<Range<C>> itr = rangeSet.asRanges().iterator();
    Range<C> expectedSpan = null;
    if (itr.hasNext()) {
      expectedSpan = itr.next();
      while (itr.hasNext()) {
        expectedSpan = expectedSpan.span(itr.next());
      }
    }

    try {
      Range<C> span = rangeSet.span();
      assertEquals(expectedSpan, span);
    } catch (NoSuchElementException e) {
      assertNull(expectedSpan);
    }

    // test that asDescendingSetOfRanges is the reverse of asRanges
    assertEquals(Lists.reverse(asRanges), ImmutableList.copyOf(rangeSet.asDescendingSetOfRanges()));
  }
}

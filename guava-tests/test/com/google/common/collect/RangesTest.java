/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.collect.BoundType.CLOSED;
import static com.google.common.collect.BoundType.OPEN;

import com.google.common.annotations.GwtCompatible;
import com.google.common.testing.EqualsTester;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Gregory Kick
 */
@GwtCompatible
public class RangesTest extends TestCase {
  public void testSingleton() {
    assertEquals(Ranges.closed(0, 0), Ranges.singleton(0));
    assertEquals(Ranges.closed(9, 9), Ranges.singleton(9));
  }

  public void testEncloseAll() {
    assertEquals(Ranges.closed(0, 0), Ranges.encloseAll(Arrays.asList(0)));
    assertEquals(Ranges.closed(-3, 5), Ranges.encloseAll(Arrays.asList(5, -3)));
    assertEquals(Ranges.closed(-3, 5), Ranges.encloseAll(Arrays.asList(1, 2, 2, 2, 5, -3, 0, -1)));
  }

  public void testEncloseAll_empty() {
    try {
      Ranges.encloseAll(ImmutableSet.<Integer>of());
      fail();
    } catch (NoSuchElementException expected) {}
  }

  public void testEncloseAll_nullValue() {
    List<Integer> nullFirst = Lists.newArrayList(null, 0);
    try {
      Ranges.encloseAll(nullFirst);
      fail();
    } catch (NullPointerException expected) {}
    List<Integer> nullNotFirst = Lists.newArrayList(0, null);
    try {
      Ranges.encloseAll(nullNotFirst);
      fail();
    } catch (NullPointerException expected) {}
  }

  public void testEquivalentFactories() {
    new EqualsTester()
        .addEqualityGroup(Ranges.all())
        .addEqualityGroup(
            Ranges.atLeast(1),
            Ranges.downTo(1, CLOSED))
        .addEqualityGroup(
            Ranges.greaterThan(1),
            Ranges.downTo(1, OPEN))
        .addEqualityGroup(
            Ranges.atMost(7),
            Ranges.upTo(7, CLOSED))
        .addEqualityGroup(
            Ranges.lessThan(7),
            Ranges.upTo(7, OPEN))
        .addEqualityGroup(
            Ranges.open(1, 7),
            Ranges.range(1, OPEN, 7, OPEN))
        .addEqualityGroup(
            Ranges.openClosed(1, 7),
            Ranges.range(1, OPEN, 7, CLOSED))
        .addEqualityGroup(
            Ranges.closed(1, 7),
            Ranges.range(1, CLOSED, 7, CLOSED))
        .addEqualityGroup(
            Ranges.closedOpen(1, 7),
            Ranges.range(1, CLOSED, 7, OPEN))
        .testEquals();
  }
}

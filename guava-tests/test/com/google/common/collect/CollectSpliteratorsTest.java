/*
 * Copyright (C) 2015 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Ascii;
import com.google.common.collect.testing.SpliteratorTester;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import junit.framework.TestCase;

/** Tests for {@code CollectSpliterators}. */
@GwtCompatible
public class CollectSpliteratorsTest extends TestCase {
  public void testMap() {
    SpliteratorTester.of(
            () ->
                CollectSpliterators.map(
                    Arrays.spliterator(new String[] {"a", "b", "c", "d", "e"}), Ascii::toUpperCase))
        .expect("A", "B", "C", "D", "E");
  }

  public void testFlatMap() {
    SpliteratorTester.of(
            () ->
                CollectSpliterators.flatMap(
                    Arrays.spliterator(new String[] {"abc", "", "de", "f", "g", ""}),
                    (String str) -> Lists.charactersOf(str).spliterator(),
                    Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.NONNULL,
                    7))
        .expect('a', 'b', 'c', 'd', 'e', 'f', 'g');
  }

  public void testFlatMap_nullStream() {
    SpliteratorTester.of(
            () ->
                CollectSpliterators.flatMap(
                    Arrays.spliterator(new String[] {"abc", "", "de", "f", "g", ""}),
                    (String str) -> str.isEmpty() ? null : Lists.charactersOf(str).spliterator(),
                    Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.NONNULL,
                    7))
        .expect('a', 'b', 'c', 'd', 'e', 'f', 'g');
  }

  public void testFlatMapToInt_nullStream() {
    SpliteratorTester.ofInt(
            () ->
                CollectSpliterators.flatMapToInt(
                    Arrays.spliterator(new Integer[] {1, 0, 1, 2, 3}),
                    (Integer i) -> i == 0 ? null : IntStream.of(i).spliterator(),
                    Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.NONNULL,
                    4))
        .expect(1, 1, 2, 3);
  }

  public void testFlatMapToLong_nullStream() {
    SpliteratorTester.ofLong(
            () ->
                CollectSpliterators.flatMapToLong(
                    Arrays.spliterator(new Long[] {1L, 0L, 1L, 2L, 3L}),
                    (Long i) -> i == 0L ? null : LongStream.of(i).spliterator(),
                    Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.NONNULL,
                    4))
        .expect(1L, 1L, 2L, 3L);
  }

  public void testFlatMapToDouble_nullStream() {
    SpliteratorTester.ofDouble(
            () ->
                CollectSpliterators.flatMapToDouble(
                    Arrays.spliterator(new Double[] {1.0, 0.0, 1.0, 2.0, 3.0}),
                    (Double i) -> i == 0.0 ? null : DoubleStream.of(i).spliterator(),
                    Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.NONNULL,
                    4))
        .expect(1.0, 1.0, 2.0, 3.0);
  }

  public void testMultisetsSpliterator() {
    Multiset<String> multiset = TreeMultiset.create();
    multiset.add("a", 3);
    multiset.add("b", 1);
    multiset.add("c", 2);

    List<String> actualValues = Lists.newArrayList();
    multiset.spliterator().forEachRemaining(actualValues::add);
    assertThat(multiset).containsExactly("a", "a", "a", "b", "c", "c").inOrder();
  }
}

/*
 * Copyright (C) 2016 The Guava Authors
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

import static com.google.common.collect.Streams.findLast;
import static com.google.common.collect.Streams.stream;

import com.google.common.collect.testing.SpliteratorTester;
import com.google.common.primitives.Doubles;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.Truth;
import com.google.common.truth.Truth8;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import junit.framework.TestCase;

/**
 * Unit test for {@link Streams}.
 */
public class StreamsTest extends TestCase {
  /*
   * Full and proper black-box testing of a Stream-returning method is extremely involved, and is
   * overkill when nearly all Streams are produced using well-tested JDK calls. So, we cheat and
   * just test that the toArray() contents are as expected.
   */
  public void testStream_nonCollection() {
    assertThat(stream(FluentIterable.of())).isEmpty();
    assertThat(stream(FluentIterable.of("a"))).containsExactly("a");
    assertThat(stream(FluentIterable.of(1, 2, 3)).filter(n -> n > 1)).containsExactly(2, 3);
  }

  @SuppressWarnings("deprecation")
  public void testStream_collection() {
    assertThat(stream(Arrays.asList())).isEmpty();
    assertThat(stream(Arrays.asList("a"))).containsExactly("a");
    assertThat(stream(Arrays.asList(1, 2, 3)).filter(n -> n > 1)).containsExactly(2, 3);
  }

  public void testStream_iterator() {
    assertThat(stream(Arrays.asList().iterator())).isEmpty();
    assertThat(stream(Arrays.asList("a").iterator())).containsExactly("a");
    assertThat(stream(Arrays.asList(1, 2, 3).iterator()).filter(n -> n > 1)).containsExactly(2, 3);
  }

  public void testStream_googleOptional() {
    assertThat(stream(com.google.common.base.Optional.absent())).isEmpty();
    assertThat(stream(com.google.common.base.Optional.of("a"))).containsExactly("a");
  }

  public void testStream_javaOptional() {
    assertThat(stream(java.util.Optional.empty())).isEmpty();
    assertThat(stream(java.util.Optional.of("a"))).containsExactly("a");
  }
  
  public void testFindLast_refStream() {
    Truth8.assertThat(findLast(Stream.of())).isEmpty();
    Truth8.assertThat(findLast(Stream.of("a", "b", "c", "d"))).hasValue("d");

    // test with a large, not-subsized Spliterator
    List<Integer> list =
        IntStream.rangeClosed(0, 10000).boxed().collect(Collectors.toCollection(LinkedList::new));
    Truth8.assertThat(findLast(list.stream())).hasValue(10000);

    // no way to find out the stream is empty without walking its spliterator
    Truth8.assertThat(findLast(list.stream().filter(i -> i < 0))).isEmpty();
  }

  public void testFindLast_intStream() {
    Truth.assertThat(findLast(IntStream.of())).isEqualTo(OptionalInt.empty());
    Truth.assertThat(findLast(IntStream.of(1, 2, 3, 4, 5))).isEqualTo(OptionalInt.of(5));

    // test with a large, not-subsized Spliterator
    List<Integer> list =
        IntStream.rangeClosed(0, 10000).boxed().collect(Collectors.toCollection(LinkedList::new));
    Truth.assertThat(findLast(list.stream().mapToInt(i -> i))).isEqualTo(OptionalInt.of(10000));

    // no way to find out the stream is empty without walking its spliterator
    Truth.assertThat(findLast(list.stream().mapToInt(i -> i).filter(i -> i < 0)))
        .isEqualTo(OptionalInt.empty());
  }

  public void testFindLast_longStream() {
    Truth.assertThat(findLast(LongStream.of())).isEqualTo(OptionalLong.empty());
    Truth.assertThat(findLast(LongStream.of(1, 2, 3, 4, 5))).isEqualTo(OptionalLong.of(5));

    // test with a large, not-subsized Spliterator
    List<Long> list =
        LongStream.rangeClosed(0, 10000).boxed().collect(Collectors.toCollection(LinkedList::new));
    Truth.assertThat(findLast(list.stream().mapToLong(i -> i))).isEqualTo(OptionalLong.of(10000));

    // no way to find out the stream is empty without walking its spliterator
    Truth.assertThat(findLast(list.stream().mapToLong(i -> i).filter(i -> i < 0)))
        .isEqualTo(OptionalLong.empty());
  }

  public void testFindLast_doubleStream() {
    Truth.assertThat(findLast(DoubleStream.of())).isEqualTo(OptionalDouble.empty());
    Truth.assertThat(findLast(DoubleStream.of(1, 2, 3, 4, 5))).isEqualTo(OptionalDouble.of(5));

    // test with a large, not-subsized Spliterator
    List<Long> list =
        LongStream.rangeClosed(0, 10000).boxed().collect(Collectors.toCollection(LinkedList::new));
    Truth.assertThat(findLast(list.stream().mapToDouble(i -> i)))
        .isEqualTo(OptionalDouble.of(10000));

    // no way to find out the stream is empty without walking its spliterator
    Truth.assertThat(findLast(list.stream().mapToDouble(i -> i).filter(i -> i < 0)))
        .isEqualTo(OptionalDouble.empty());
  }

  public void testConcat_refStream() {
    assertThat(Streams.concat(Stream.of("a"), Stream.of("b"), Stream.empty(), Stream.of("c", "d")))
        .containsExactly("a", "b", "c", "d")
        .inOrder();
  }

  public void testConcat_intStream() {
    assertThat(
            Streams.concat(IntStream.of(1), IntStream.of(2), IntStream.empty(), IntStream.of(3, 4)))
        .containsExactly(1, 2, 3, 4)
        .inOrder();
  }

  public void testConcat_longStream() {
    assertThat(
            Streams.concat(
                LongStream.of(1), LongStream.of(2), LongStream.empty(), LongStream.of(3, 4)))
        .containsExactly(1L, 2L, 3L, 4L)
        .inOrder();
  }

  public void testConcat_doubleStream() {
    assertThat(
            Streams.concat(
                DoubleStream.of(1),
                DoubleStream.of(2),
                DoubleStream.empty(),
                DoubleStream.of(3, 4)))
        .containsExactly(1.0, 2.0, 3.0, 4.0)
        .inOrder();
  }

  public void testStream_optionalInt() {
    assertThat(stream(java.util.OptionalInt.empty())).isEmpty();
    assertThat(stream(java.util.OptionalInt.of(5))).containsExactly(5);
  }

  public void testStream_optionalLong() {
    assertThat(stream(java.util.OptionalLong.empty())).isEmpty();
    assertThat(stream(java.util.OptionalLong.of(5L))).containsExactly(5L);
  }

  public void testStream_optionalDouble() {
    assertThat(stream(java.util.OptionalDouble.empty())).isEmpty();
    assertThat(stream(java.util.OptionalDouble.of(5.0))).containsExactly(5.0);
  }
  
  private void testMapWithIndex(Function<Collection<String>, Stream<String>> collectionImpl) {
    SpliteratorTester.of(
            () ->
                Streams.mapWithIndex(
                        collectionImpl.apply(ImmutableList.of()), (str, i) -> str + ":" + i)
                    .spliterator())
        .expect(ImmutableList.of());
    SpliteratorTester.of(
            () ->
                Streams.mapWithIndex(
                        collectionImpl.apply(ImmutableList.of("a", "b", "c", "d", "e")),
                        (str, i) -> str + ":" + i)
                    .spliterator())
        .expect("a:0", "b:1", "c:2", "d:3", "e:4");
  }

  public void testMapWithIndex_arrayListSource() {
    testMapWithIndex(elems -> new ArrayList<>(elems).stream());
  }

  public void testMapWithIndex_linkedHashSetSource() {
    testMapWithIndex(elems -> new LinkedHashSet<>(elems).stream());
  }

  public void testMapWithIndex_unsizedSource() {
    testMapWithIndex(
        elems -> Stream.of((Object) null).flatMap(unused -> ImmutableList.copyOf(elems).stream()));
  }

  public void testZip() {
    assertThat(Streams.zip(Stream.of("a", "b", "c"), Stream.of(1, 2, 3), (a, b) -> a + ":" + b))
        .containsExactly("a:1", "b:2", "c:3")
        .inOrder();
  }

  public void testZipFiniteWithInfinite() {
    assertThat(
            Streams.zip(
                Stream.of("a", "b", "c"), Stream.iterate(1, i -> i + 1), (a, b) -> a + ":" + b))
        .containsExactly("a:1", "b:2", "c:3")
        .inOrder();
  }

  public void testZipInfiniteWithInfinite() {
    // zip is doing an infinite zip, but we truncate the result so we can actually test it
    // but we want the zip itself to work
    assertThat(
            Streams.zip(
                    Stream.iterate(1, i -> i + 1).map(String::valueOf),
                    Stream.iterate(1, i -> i + 1),
                    (String str, Integer i) -> str.equals(Integer.toString(i)))
                .limit(100))
        .doesNotContain(false);
  }

  public void testZipDifferingLengths() {
    assertThat(
            Streams.zip(Stream.of("a", "b", "c", "d"), Stream.of(1, 2, 3), (a, b) -> a + ":" + b))
        .containsExactly("a:1", "b:2", "c:3")
        .inOrder();
    assertThat(Streams.zip(Stream.of("a", "b", "c"), Stream.of(1, 2, 3, 4), (a, b) -> a + ":" + b))
        .containsExactly("a:1", "b:2", "c:3")
        .inOrder();
  }

  // TODO(kevinb): switch to importing Truth's assertThat(Stream) if we get that added
  private static IterableSubject assertThat(Stream<?> stream) {
    return Truth.assertThat(stream.toArray()).asList();
  }
  
  private static IterableSubject assertThat(IntStream stream) {
    return Truth.assertThat(stream.toArray()).asList();
  }

  private static IterableSubject assertThat(LongStream stream) {
    return Truth.assertThat(stream.toArray()).asList();
  }

  private static IterableSubject assertThat(DoubleStream stream) {
    return Truth.assertThat(Doubles.asList(stream.toArray()));
  }
}

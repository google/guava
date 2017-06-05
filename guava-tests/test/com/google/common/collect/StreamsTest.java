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

import static com.google.common.collect.Streams.stream;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.SpliteratorTester;
import com.google.common.primitives.Doubles;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.Truth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import junit.framework.TestCase;

/** Unit test for {@link Streams}. */
@GwtCompatible(emulated = true)
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

  public void testConcat_refStream() {
    assertThat(Streams.concat(Stream.of("a"), Stream.of("b"), Stream.empty(), Stream.of("c", "d")))
        .containsExactly("a", "b", "c", "d")
        .inOrder();
    SpliteratorTester.of(
            () ->
                Streams.concat(Stream.of("a"), Stream.of("b"), Stream.empty(), Stream.of("c", "d"))
                    .spliterator())
        .expect("a", "b", "c", "d");
  }

  public void testConcat_refStream_parallel() {
    Truth.assertThat(
            Streams.concat(Stream.of("a"), Stream.of("b"), Stream.empty(), Stream.of("c", "d"))
                .parallel()
                .toArray())
        .asList()
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

  public void testMapWithIndex_intStream() {
    SpliteratorTester.of(
            () -> Streams.mapWithIndex(IntStream.of(0, 1, 2), (x, i) -> x + ":" + i).spliterator())
        .expect("0:0", "1:1", "2:2");
  }

  public void testMapWithIndex_longStream() {
    SpliteratorTester.of(
            () -> Streams.mapWithIndex(LongStream.of(0, 1, 2), (x, i) -> x + ":" + i).spliterator())
        .expect("0:0", "1:1", "2:2");
  }

  @GwtIncompatible // TODO(b/38490623): reenable after GWT double-to-string conversion is fixed
  public void testMapWithIndex_doubleStream() {
    SpliteratorTester.of(
            () ->
                Streams.mapWithIndex(DoubleStream.of(0, 1, 2), (x, i) -> x + ":" + i).spliterator())
        .expect("0.0:0", "1.0:1", "2.0:2");
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

  public void testForEachPair() {
    List<String> list = new ArrayList<>();
    Streams.forEachPair(
        Stream.of("a", "b", "c"), Stream.of(1, 2, 3), (a, b) -> list.add(a + ":" + b));
    Truth.assertThat(list).containsExactly("a:1", "b:2", "c:3");
  }

  public void testForEachPair_differingLengths1() {
    List<String> list = new ArrayList<>();
    Streams.forEachPair(
        Stream.of("a", "b", "c", "d"), Stream.of(1, 2, 3), (a, b) -> list.add(a + ":" + b));
    Truth.assertThat(list).containsExactly("a:1", "b:2", "c:3");
  }

  public void testForEachPair_differingLengths2() {
    List<String> list = new ArrayList<>();
    Streams.forEachPair(
        Stream.of("a", "b", "c"), Stream.of(1, 2, 3, 4), (a, b) -> list.add(a + ":" + b));
    Truth.assertThat(list).containsExactly("a:1", "b:2", "c:3");
  }

  public void testForEachPair_oneEmpty() {
    Streams.forEachPair(Stream.of("a"), Stream.empty(), (a, b) -> fail());
  }

  public void testForEachPair_finiteWithInfinite() {
    List<String> list = new ArrayList<>();
    Streams.forEachPair(
        Stream.of("a", "b", "c"), Stream.iterate(1, i -> i + 1), (a, b) -> list.add(a + ":" + b));
    Truth.assertThat(list).containsExactly("a:1", "b:2", "c:3");
  }

  public void testForEachPair_parallel() {
    Stream<String> streamA = IntStream.range(0, 100000).mapToObj(String::valueOf).parallel();
    Stream<Integer> streamB = IntStream.range(0, 100000).mapToObj(i -> i).parallel();

    AtomicInteger count = new AtomicInteger(0);
    Streams.forEachPair(
        streamA,
        streamB,
        (a, b) -> {
          count.incrementAndGet();
          Truth.assertThat(a.equals(String.valueOf(b))).isTrue();
        });
    Truth.assertThat(count.get()).isEqualTo(100000);
    // of course, this test doesn't prove that anything actually happened in parallel...
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

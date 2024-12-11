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
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.SpliteratorTester;
import com.google.common.primitives.Doubles;
import com.google.common.truth.IterableSubject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Unit test for {@link Streams}. */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
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
    assertThat(stream(asList())).isEmpty();
    assertThat(stream(asList("a"))).containsExactly("a");
    assertThat(stream(asList(1, 2, 3)).filter(n -> n > 1)).containsExactly(2, 3);
  }

  public void testStream_iterator() {
    assertThat(stream(asList().iterator())).isEmpty();
    assertThat(stream(asList("a").iterator())).containsExactly("a");
    assertThat(stream(asList(1, 2, 3).iterator()).filter(n -> n > 1)).containsExactly(2, 3);
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
    assertThat(findLast(Stream.of())).isEmpty();
    assertThat(findLast(Stream.of("a", "b", "c", "d"))).hasValue("d");

    // test with a large, not-subsized Spliterator
    List<Integer> list =
        IntStream.rangeClosed(0, 10000).boxed().collect(Collectors.toCollection(LinkedList::new));
    assertThat(findLast(list.stream())).hasValue(10000);

    // no way to find out the stream is empty without walking its spliterator
    assertThat(findLast(list.stream().filter(i -> i < 0))).isEmpty();
  }

  public void testFindLast_intStream() {
    assertThat(findLast(IntStream.of())).isEqualTo(OptionalInt.empty());
    assertThat(findLast(IntStream.of(1, 2, 3, 4, 5))).isEqualTo(OptionalInt.of(5));

    // test with a large, not-subsized Spliterator
    List<Integer> list =
        IntStream.rangeClosed(0, 10000).boxed().collect(Collectors.toCollection(LinkedList::new));
    assertThat(findLast(list.stream().mapToInt(i -> i))).isEqualTo(OptionalInt.of(10000));

    // no way to find out the stream is empty without walking its spliterator
    assertThat(findLast(list.stream().mapToInt(i -> i).filter(i -> i < 0)))
        .isEqualTo(OptionalInt.empty());
  }

  public void testFindLast_longStream() {
    assertThat(findLast(LongStream.of())).isEqualTo(OptionalLong.empty());
    assertThat(findLast(LongStream.of(1, 2, 3, 4, 5))).isEqualTo(OptionalLong.of(5));

    // test with a large, not-subsized Spliterator
    List<Long> list =
        LongStream.rangeClosed(0, 10000).boxed().collect(Collectors.toCollection(LinkedList::new));
    assertThat(findLast(list.stream().mapToLong(i -> i))).isEqualTo(OptionalLong.of(10000));

    // no way to find out the stream is empty without walking its spliterator
    assertThat(findLast(list.stream().mapToLong(i -> i).filter(i -> i < 0)))
        .isEqualTo(OptionalLong.empty());
  }

  public void testFindLast_doubleStream() {
    assertThat(findLast(DoubleStream.of())).isEqualTo(OptionalDouble.empty());
    assertThat(findLast(DoubleStream.of(1, 2, 3, 4, 5))).isEqualTo(OptionalDouble.of(5));

    // test with a large, not-subsized Spliterator
    List<Long> list =
        LongStream.rangeClosed(0, 10000).boxed().collect(Collectors.toCollection(LinkedList::new));
    assertThat(findLast(list.stream().mapToDouble(i -> i))).isEqualTo(OptionalDouble.of(10000));

    // no way to find out the stream is empty without walking its spliterator
    assertThat(findLast(list.stream().mapToDouble(i -> i).filter(i -> i < 0)))
        .isEqualTo(OptionalDouble.empty());
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

  public void testConcat_refStream_closeIsPropagated() {
    AtomicInteger closeCountB = new AtomicInteger(0);
    Stream<String> streamB = Stream.of("b").onClose(closeCountB::incrementAndGet);
    Stream<String> concatenated =
        Streams.concat(Stream.of("a"), streamB, Stream.empty(), Stream.of("c", "d"));
    assertThat(concatenated).containsExactly("a", "b", "c", "d").inOrder();
    concatenated.close();
    assertThat(closeCountB.get()).isEqualTo(1);
  }

  public void testConcat_refStream_closeIsPropagated_stream_concat() {
    // Just to demonstrate behavior of Stream::concat in the standard library
    AtomicInteger closeCountB = new AtomicInteger(0);
    Stream<String> streamB = Stream.of("b").onClose(closeCountB::incrementAndGet);
    Stream<String> concatenated =
        Stream.<Stream<String>>of(Stream.of("a"), streamB, Stream.empty(), Stream.of("c", "d"))
            .reduce(Stream.empty(), Stream::concat);
    assertThat(concatenated).containsExactly("a", "b", "c", "d").inOrder();
    concatenated.close();
    assertThat(closeCountB.get()).isEqualTo(1);
  }

  public void testConcat_refStream_closeIsPropagated_stream_flatMap() {
    // Just to demonstrate behavior of Stream::flatMap in the standard library
    AtomicInteger closeCountB = new AtomicInteger(0);
    Stream<String> streamB = Stream.of("b").onClose(closeCountB::incrementAndGet);
    Stream<String> concatenated =
        Stream.<Stream<String>>of(Stream.of("a"), streamB, Stream.empty(), Stream.of("c", "d"))
            .flatMap(x -> x);
    assertThat(concatenated).containsExactly("a", "b", "c", "d").inOrder();
    concatenated.close();
    // even without close, see doc for flatMap
    assertThat(closeCountB.get()).isEqualTo(1);
  }

  public void testConcat_refStream_closeIsPropagated_exceptionsChained() {
    RuntimeException exception1 = new IllegalArgumentException("exception from stream 1");
    RuntimeException exception2 = new IllegalStateException("exception from stream 2");
    RuntimeException exception3 = new ArithmeticException("exception from stream 3");
    Stream<String> stream1 = Stream.of("foo", "bar").onClose(doThrow(exception1));
    Stream<String> stream2 = Stream.of("baz", "buh").onClose(doThrow(exception2));
    Stream<String> stream3 = Stream.of("quux").onClose(doThrow(exception3));
    RuntimeException exception = null;
    try (Stream<String> concatenated = Streams.concat(stream1, stream2, stream3)) {
    } catch (RuntimeException e) {
      exception = e;
    }
    assertThat(exception).isEqualTo(exception1);
    assertThat(exception.getSuppressed())
        .asList()
        .containsExactly(exception2, exception3)
        .inOrder();
  }

  private static Runnable doThrow(RuntimeException exception) {
    return () -> {
      throw exception;
    };
  }

  public void testConcat_refStream_parallel() {
    assertThat(
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
    assertThatDoubleStream(
            Streams.concat(
                DoubleStream.of(1),
                DoubleStream.of(2),
                DoubleStream.empty(),
                DoubleStream.of(3, 4)))
        .containsExactly(1.0, 2.0, 3.0, 4.0)
        .inOrder();
  }

  public void testStream_optionalInt() {
    assertThat(stream(OptionalInt.empty())).isEmpty();
    assertThat(stream(OptionalInt.of(5))).containsExactly(5);
  }

  public void testStream_optionalLong() {
    assertThat(stream(OptionalLong.empty())).isEmpty();
    assertThat(stream(OptionalLong.of(5L))).containsExactly(5L);
  }

  public void testStream_optionalDouble() {
    assertThatDoubleStream(stream(OptionalDouble.empty())).isEmpty();
    assertThatDoubleStream(stream(OptionalDouble.of(5.0))).containsExactly(5.0);
  }

  public void testConcatInfiniteStream() {
    assertThat(Streams.concat(Stream.of(1, 2, 3), Stream.generate(() -> 5)).limit(5))
        .containsExactly(1, 2, 3, 5, 5)
        .inOrder();
  }

  public void testConcatInfiniteStream_int() {
    assertThat(Streams.concat(IntStream.of(1, 2, 3), IntStream.generate(() -> 5)).limit(5))
        .containsExactly(1, 2, 3, 5, 5)
        .inOrder();
  }

  public void testConcatInfiniteStream_long() {
    assertThat(Streams.concat(LongStream.of(1, 2, 3), LongStream.generate(() -> 5)).limit(5))
        .containsExactly(1L, 2L, 3L, 5L, 5L)
        .inOrder();
  }

  public void testConcatInfiniteStream_double() {
    assertThatDoubleStream(
            Streams.concat(DoubleStream.of(1, 2, 3), DoubleStream.generate(() -> 5)).limit(5))
        .containsExactly(1., 2., 3., 5., 5.)
        .inOrder();
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
        elems ->
            Stream.<@Nullable Object>of((Object) null)
                .flatMap(unused -> ImmutableList.copyOf(elems).stream()));
  }

  public void testMapWithIndex_closeIsPropagated_sizedSource() {
    testMapWithIndex_closeIsPropagated(Stream.of("a", "b", "c"));
  }

  public void testMapWithIndex_closeIsPropagated_unsizedSource() {
    testMapWithIndex_closeIsPropagated(
        Stream.<@Nullable Object>of((Object) null).flatMap(unused -> Stream.of("a", "b", "c")));
  }

  private void testMapWithIndex_closeIsPropagated(Stream<String> source) {
    AtomicInteger stringsCloseCount = new AtomicInteger();
    Stream<String> strings = source.onClose(stringsCloseCount::incrementAndGet);
    Stream<String> withIndex = Streams.mapWithIndex(strings, (str, i) -> str + ":" + i);

    withIndex.close();

    assertThat(stringsCloseCount.get()).isEqualTo(1);
  }

  public void testMapWithIndex_intStream() {
    SpliteratorTester.of(
            () -> Streams.mapWithIndex(IntStream.of(0, 1, 2), (x, i) -> x + ":" + i).spliterator())
        .expect("0:0", "1:1", "2:2");
  }

  public void testMapWithIndex_intStream_closeIsPropagated_sized() {
    testMapWithIndex_intStream_closeIsPropagated(IntStream.of(1, 2, 3));
  }

  public void testMapWithIndex_intStream_closeIsPropagated_unsized() {
    testMapWithIndex_intStream_closeIsPropagated(
        IntStream.of(0).flatMap(unused -> IntStream.of(1, 2, 3)));
  }

  private void testMapWithIndex_intStream_closeIsPropagated(IntStream source) {
    AtomicInteger intStreamCloseCount = new AtomicInteger();
    IntStream intStream = source.onClose(intStreamCloseCount::incrementAndGet);
    Stream<String> withIndex = Streams.mapWithIndex(intStream, (str, i) -> str + ":" + i);

    withIndex.close();

    assertThat(intStreamCloseCount.get()).isEqualTo(1);
  }

  public void testMapWithIndex_longStream() {
    SpliteratorTester.of(
            () -> Streams.mapWithIndex(LongStream.of(0, 1, 2), (x, i) -> x + ":" + i).spliterator())
        .expect("0:0", "1:1", "2:2");
  }

  public void testMapWithIndex_longStream_closeIsPropagated_sized() {
    testMapWithIndex_longStream_closeIsPropagated(LongStream.of(1, 2, 3));
  }

  public void testMapWithIndex_longStream_closeIsPropagated_unsized() {
    testMapWithIndex_longStream_closeIsPropagated(
        LongStream.of(0).flatMap(unused -> LongStream.of(1, 2, 3)));
  }

  private void testMapWithIndex_longStream_closeIsPropagated(LongStream source) {
    AtomicInteger longStreamCloseCount = new AtomicInteger();
    LongStream longStream = source.onClose(longStreamCloseCount::incrementAndGet);
    Stream<String> withIndex = Streams.mapWithIndex(longStream, (str, i) -> str + ":" + i);

    withIndex.close();

    assertThat(longStreamCloseCount.get()).isEqualTo(1);
  }

  @GwtIncompatible // TODO(b/38490623): reenable after GWT double-to-string conversion is fixed
  public void testMapWithIndex_doubleStream() {
    SpliteratorTester.of(
            () ->
                Streams.mapWithIndex(DoubleStream.of(0, 1, 2), (x, i) -> x + ":" + i).spliterator())
        .expect("0.0:0", "1.0:1", "2.0:2");
  }

  public void testMapWithIndex_doubleStream_closeIsPropagated_sized() {
    testMapWithIndex_doubleStream_closeIsPropagated(DoubleStream.of(1, 2, 3));
  }

  public void testMapWithIndex_doubleStream_closeIsPropagated_unsized() {
    testMapWithIndex_doubleStream_closeIsPropagated(
        DoubleStream.of(0).flatMap(unused -> DoubleStream.of(1, 2, 3)));
  }

  private void testMapWithIndex_doubleStream_closeIsPropagated(DoubleStream source) {
    AtomicInteger doubleStreamCloseCount = new AtomicInteger();
    DoubleStream doubleStream = source.onClose(doubleStreamCloseCount::incrementAndGet);
    Stream<String> withIndex = Streams.mapWithIndex(doubleStream, (str, i) -> str + ":" + i);

    withIndex.close();

    assertThat(doubleStreamCloseCount.get()).isEqualTo(1);
  }

  public void testZip() {
    assertThat(Streams.zip(Stream.of("a", "b", "c"), Stream.of(1, 2, 3), (a, b) -> a + ":" + b))
        .containsExactly("a:1", "b:2", "c:3")
        .inOrder();
  }

  public void testZip_closeIsPropagated() {
    AtomicInteger lettersCloseCount = new AtomicInteger();
    Stream<String> letters = Stream.of("a", "b", "c").onClose(lettersCloseCount::incrementAndGet);
    AtomicInteger numbersCloseCount = new AtomicInteger();
    Stream<Integer> numbers = Stream.of(1, 2, 3).onClose(numbersCloseCount::incrementAndGet);

    Stream<String> zipped = Streams.zip(letters, numbers, (a, b) -> a + ":" + b);

    zipped.close();

    assertThat(lettersCloseCount.get()).isEqualTo(1);
    assertThat(numbersCloseCount.get()).isEqualTo(1);
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
    assertThat(list).containsExactly("a:1", "b:2", "c:3");
  }

  public void testForEachPair_differingLengths1() {
    List<String> list = new ArrayList<>();
    Streams.forEachPair(
        Stream.of("a", "b", "c", "d"), Stream.of(1, 2, 3), (a, b) -> list.add(a + ":" + b));
    assertThat(list).containsExactly("a:1", "b:2", "c:3");
  }

  public void testForEachPair_differingLengths2() {
    List<String> list = new ArrayList<>();
    Streams.forEachPair(
        Stream.of("a", "b", "c"), Stream.of(1, 2, 3, 4), (a, b) -> list.add(a + ":" + b));
    assertThat(list).containsExactly("a:1", "b:2", "c:3");
  }

  public void testForEachPair_oneEmpty() {
    Streams.forEachPair(Stream.of("a"), Stream.empty(), (a, b) -> fail());
  }

  public void testForEachPair_finiteWithInfinite() {
    List<String> list = new ArrayList<>();
    Streams.forEachPair(
        Stream.of("a", "b", "c"), Stream.iterate(1, i -> i + 1), (a, b) -> list.add(a + ":" + b));
    assertThat(list).containsExactly("a:1", "b:2", "c:3");
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
          assertThat(a.equals(String.valueOf(b))).isTrue();
        });
    assertThat(count.get()).isEqualTo(100000);
    // of course, this test doesn't prove that anything actually happened in parallel...
  }

  // TODO(kevinb): switch to importing Truth's assertThat(DoubleStream) if we get that added
  private static IterableSubject assertThatDoubleStream(DoubleStream stream) {
    return assertThat(Doubles.asList(stream.toArray()));
  }
}

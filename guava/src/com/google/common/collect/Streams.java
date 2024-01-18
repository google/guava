/*
 * Copyright (C) 2015 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;
import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.math.LongMath;
import com.google.errorprone.annotations.InlineMe;
import com.google.errorprone.annotations.InlineMeValidationDisabled;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Static utility methods related to {@code Stream} instances.
 *
 * @since 21.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public final class Streams {
  /**
   * Returns a sequential {@link Stream} of the contents of {@code iterable}, delegating to {@link
   * Collection#stream} if possible.
   */
  public static <T extends @Nullable Object> Stream<T> stream(Iterable<T> iterable) {
    return (iterable instanceof Collection)
        ? ((Collection<T>) iterable).stream()
        : StreamSupport.stream(iterable.spliterator(), false);
  }

  /**
   * Returns {@link Collection#stream}.
   *
   * @deprecated There is no reason to use this; just invoke {@code collection.stream()} directly.
   */
  @Deprecated
  @InlineMe(replacement = "collection.stream()")
  public static <T extends @Nullable Object> Stream<T> stream(Collection<T> collection) {
    return collection.stream();
  }

  /**
   * Returns a sequential {@link Stream} of the remaining contents of {@code iterator}. Do not use
   * {@code iterator} directly after passing it to this method.
   */
  public static <T extends @Nullable Object> Stream<T> stream(Iterator<T> iterator) {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
  }

  /**
   * If a value is present in {@code optional}, returns a stream containing only that element,
   * otherwise returns an empty stream.
   */
  public static <T> Stream<T> stream(com.google.common.base.Optional<T> optional) {
    return optional.isPresent() ? Stream.of(optional.get()) : Stream.empty();
  }

  /**
   * If a value is present in {@code optional}, returns a stream containing only that element,
   * otherwise returns an empty stream.
   *
   * <p><b>Java 9 users:</b> use {@code optional.stream()} instead.
   */
  @Beta
  @InlineMe(replacement = "optional.stream()")
  @InlineMeValidationDisabled("Java 9+ API only")
  public static <T> Stream<T> stream(java.util.Optional<T> optional) {
    return optional.isPresent() ? Stream.of(optional.get()) : Stream.empty();
  }

  /**
   * If a value is present in {@code optional}, returns a stream containing only that element,
   * otherwise returns an empty stream.
   *
   * <p><b>Java 9 users:</b> use {@code optional.stream()} instead.
   */
  @Beta
  @InlineMe(replacement = "optional.stream()")
  @InlineMeValidationDisabled("Java 9+ API only")
  public static IntStream stream(OptionalInt optional) {
    return optional.isPresent() ? IntStream.of(optional.getAsInt()) : IntStream.empty();
  }

  /**
   * If a value is present in {@code optional}, returns a stream containing only that element,
   * otherwise returns an empty stream.
   *
   * <p><b>Java 9 users:</b> use {@code optional.stream()} instead.
   */
  @Beta
  @InlineMe(replacement = "optional.stream()")
  @InlineMeValidationDisabled("Java 9+ API only")
  public static LongStream stream(OptionalLong optional) {
    return optional.isPresent() ? LongStream.of(optional.getAsLong()) : LongStream.empty();
  }

  /**
   * If a value is present in {@code optional}, returns a stream containing only that element,
   * otherwise returns an empty stream.
   *
   * <p><b>Java 9 users:</b> use {@code optional.stream()} instead.
   */
  @Beta
  @InlineMe(replacement = "optional.stream()")
  @InlineMeValidationDisabled("Java 9+ API only")
  public static DoubleStream stream(OptionalDouble optional) {
    return optional.isPresent() ? DoubleStream.of(optional.getAsDouble()) : DoubleStream.empty();
  }

  @SuppressWarnings("CatchingUnchecked") // sneaky checked exception
  private static void closeAll(BaseStream<?, ?>[] toClose) {
    // If one of the streams throws an exception, continue closing the others, then throw the
    // exception later. If more than one stream throws an exception, the later ones are added to the
    // first as suppressed exceptions. We don't catch Error on the grounds that it should be allowed
    // to propagate immediately.
    Exception exception = null;
    for (BaseStream<?, ?> stream : toClose) {
      try {
        stream.close();
      } catch (Exception e) { // sneaky checked exception
        if (exception == null) {
          exception = e;
        } else {
          exception.addSuppressed(e);
        }
      }
    }
    if (exception != null) {
      // Normally this is a RuntimeException that doesn't need sneakyThrow.
      // But theoretically we could see sneaky checked exception
      sneakyThrow(exception);
    }
  }

  /** Throws an undeclared checked exception. */
  private static void sneakyThrow(Throwable t) {
    class SneakyThrower<T extends Throwable> {
      @SuppressWarnings("unchecked") // not really safe, but that's the point
      void throwIt(Throwable t) throws T {
        throw (T) t;
      }
    }
    new SneakyThrower<Error>().throwIt(t);
  }

  /**
   * Returns a {@link Stream} containing the elements of the first stream, followed by the elements
   * of the second stream, and so on.
   *
   * <p>This is equivalent to {@code Stream.of(streams).flatMap(stream -> stream)}, but the returned
   * stream may perform better.
   *
   * @see Stream#concat(Stream, Stream)
   */
  @SafeVarargs
  public static <T extends @Nullable Object> Stream<T> concat(Stream<? extends T>... streams) {
    // TODO(lowasser): consider an implementation that can support SUBSIZED
    boolean isParallel = false;
    int characteristics = Spliterator.ORDERED | Spliterator.SIZED | Spliterator.NONNULL;
    long estimatedSize = 0L;
    ImmutableList.Builder<Spliterator<? extends T>> splitrsBuilder =
        new ImmutableList.Builder<>(streams.length);
    for (Stream<? extends T> stream : streams) {
      isParallel |= stream.isParallel();
      Spliterator<? extends T> splitr = stream.spliterator();
      splitrsBuilder.add(splitr);
      characteristics &= splitr.characteristics();
      estimatedSize = LongMath.saturatedAdd(estimatedSize, splitr.estimateSize());
    }
    return StreamSupport.stream(
            CollectSpliterators.flatMap(
                splitrsBuilder.build().spliterator(),
                splitr -> (Spliterator<T>) splitr,
                characteristics,
                estimatedSize),
            isParallel)
        .onClose(() -> closeAll(streams));
  }

  /**
   * Returns an {@link IntStream} containing the elements of the first stream, followed by the
   * elements of the second stream, and so on.
   *
   * <p>This is equivalent to {@code Stream.of(streams).flatMapToInt(stream -> stream)}, but the
   * returned stream may perform better.
   *
   * @see IntStream#concat(IntStream, IntStream)
   */
  public static IntStream concat(IntStream... streams) {
    boolean isParallel = false;
    int characteristics = Spliterator.ORDERED | Spliterator.SIZED | Spliterator.NONNULL;
    long estimatedSize = 0L;
    ImmutableList.Builder<Spliterator.OfInt> splitrsBuilder =
        new ImmutableList.Builder<>(streams.length);
    for (IntStream stream : streams) {
      isParallel |= stream.isParallel();
      Spliterator.OfInt splitr = stream.spliterator();
      splitrsBuilder.add(splitr);
      characteristics &= splitr.characteristics();
      estimatedSize = LongMath.saturatedAdd(estimatedSize, splitr.estimateSize());
    }
    return StreamSupport.intStream(
            CollectSpliterators.flatMapToInt(
                splitrsBuilder.build().spliterator(),
                splitr -> splitr,
                characteristics,
                estimatedSize),
            isParallel)
        .onClose(() -> closeAll(streams));
  }

  /**
   * Returns a {@link LongStream} containing the elements of the first stream, followed by the
   * elements of the second stream, and so on.
   *
   * <p>This is equivalent to {@code Stream.of(streams).flatMapToLong(stream -> stream)}, but the
   * returned stream may perform better.
   *
   * @see LongStream#concat(LongStream, LongStream)
   */
  public static LongStream concat(LongStream... streams) {
    boolean isParallel = false;
    int characteristics = Spliterator.ORDERED | Spliterator.SIZED | Spliterator.NONNULL;
    long estimatedSize = 0L;
    ImmutableList.Builder<Spliterator.OfLong> splitrsBuilder =
        new ImmutableList.Builder<>(streams.length);
    for (LongStream stream : streams) {
      isParallel |= stream.isParallel();
      Spliterator.OfLong splitr = stream.spliterator();
      splitrsBuilder.add(splitr);
      characteristics &= splitr.characteristics();
      estimatedSize = LongMath.saturatedAdd(estimatedSize, splitr.estimateSize());
    }
    return StreamSupport.longStream(
            CollectSpliterators.flatMapToLong(
                splitrsBuilder.build().spliterator(),
                splitr -> splitr,
                characteristics,
                estimatedSize),
            isParallel)
        .onClose(() -> closeAll(streams));
  }

  /**
   * Returns a {@link DoubleStream} containing the elements of the first stream, followed by the
   * elements of the second stream, and so on.
   *
   * <p>This is equivalent to {@code Stream.of(streams).flatMapToDouble(stream -> stream)}, but the
   * returned stream may perform better.
   *
   * @see DoubleStream#concat(DoubleStream, DoubleStream)
   */
  public static DoubleStream concat(DoubleStream... streams) {
    boolean isParallel = false;
    int characteristics = Spliterator.ORDERED | Spliterator.SIZED | Spliterator.NONNULL;
    long estimatedSize = 0L;
    ImmutableList.Builder<Spliterator.OfDouble> splitrsBuilder =
        new ImmutableList.Builder<>(streams.length);
    for (DoubleStream stream : streams) {
      isParallel |= stream.isParallel();
      Spliterator.OfDouble splitr = stream.spliterator();
      splitrsBuilder.add(splitr);
      characteristics &= splitr.characteristics();
      estimatedSize = LongMath.saturatedAdd(estimatedSize, splitr.estimateSize());
    }
    return StreamSupport.doubleStream(
            CollectSpliterators.flatMapToDouble(
                splitrsBuilder.build().spliterator(),
                splitr -> splitr,
                characteristics,
                estimatedSize),
            isParallel)
        .onClose(() -> closeAll(streams));
  }

  /**
   * Returns a stream in which each element is the result of passing the corresponding element of
   * each of {@code streamA} and {@code streamB} to {@code function}.
   *
   * <p>For example:
   *
   * <pre>{@code
   * Streams.zip(
   *   Stream.of("foo1", "foo2", "foo3"),
   *   Stream.of("bar1", "bar2"),
   *   (arg1, arg2) -> arg1 + ":" + arg2)
   * }</pre>
   *
   * <p>will return {@code Stream.of("foo1:bar1", "foo2:bar2")}.
   *
   * <p>The resulting stream will only be as long as the shorter of the two input streams; if one
   * stream is longer, its extra elements will be ignored.
   *
   * <p>Note that if you are calling {@link Stream#forEach} on the resulting stream, you might want
   * to consider using {@link #forEachPair} instead of this method.
   *
   * <p><b>Performance note:</b> The resulting stream is not <a
   * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>.
   * This may harm parallel performance.
   */
  @Beta
  public static <A extends @Nullable Object, B extends @Nullable Object, R extends @Nullable Object>
      Stream<R> zip(
          Stream<A> streamA, Stream<B> streamB, BiFunction<? super A, ? super B, R> function) {
    checkNotNull(streamA);
    checkNotNull(streamB);
    checkNotNull(function);
    boolean isParallel = streamA.isParallel() || streamB.isParallel(); // same as Stream.concat
    Spliterator<A> splitrA = streamA.spliterator();
    Spliterator<B> splitrB = streamB.spliterator();
    int characteristics =
        splitrA.characteristics()
            & splitrB.characteristics()
            & (Spliterator.SIZED | Spliterator.ORDERED);
    Iterator<A> itrA = Spliterators.iterator(splitrA);
    Iterator<B> itrB = Spliterators.iterator(splitrB);
    return StreamSupport.stream(
            new AbstractSpliterator<R>(
                min(splitrA.estimateSize(), splitrB.estimateSize()), characteristics) {
              @Override
              public boolean tryAdvance(Consumer<? super R> action) {
                if (itrA.hasNext() && itrB.hasNext()) {
                  action.accept(function.apply(itrA.next(), itrB.next()));
                  return true;
                }
                return false;
              }
            },
            isParallel)
        .onClose(streamA::close)
        .onClose(streamB::close);
  }

  /**
   * Invokes {@code consumer} once for each pair of <i>corresponding</i> elements in {@code streamA}
   * and {@code streamB}. If one stream is longer than the other, the extra elements are silently
   * ignored. Elements passed to the consumer are guaranteed to come from the same position in their
   * respective source streams. For example:
   *
   * <pre>{@code
   * Streams.forEachPair(
   *   Stream.of("foo1", "foo2", "foo3"),
   *   Stream.of("bar1", "bar2"),
   *   (arg1, arg2) -> System.out.println(arg1 + ":" + arg2)
   * }</pre>
   *
   * <p>will print:
   *
   * <pre>{@code
   * foo1:bar1
   * foo2:bar2
   * }</pre>
   *
   * <p><b>Warning:</b> If either supplied stream is a parallel stream, the same correspondence
   * between elements will be made, but the order in which those pairs of elements are passed to the
   * consumer is <i>not</i> defined.
   *
   * <p>Note that many usages of this method can be replaced with simpler calls to {@link #zip}.
   * This method behaves equivalently to {@linkplain #zip zipping} the stream elements into
   * temporary pair objects and then using {@link Stream#forEach} on that stream.
   *
   * @since 22.0
   */
  @Beta
  public static <A extends @Nullable Object, B extends @Nullable Object> void forEachPair(
      Stream<A> streamA, Stream<B> streamB, BiConsumer<? super A, ? super B> consumer) {
    checkNotNull(consumer);

    if (streamA.isParallel() || streamB.isParallel()) {
      zip(streamA, streamB, TemporaryPair::new).forEach(pair -> consumer.accept(pair.a, pair.b));
    } else {
      Iterator<A> iterA = streamA.iterator();
      Iterator<B> iterB = streamB.iterator();
      while (iterA.hasNext() && iterB.hasNext()) {
        consumer.accept(iterA.next(), iterB.next());
      }
    }
  }

  // Use this carefully - it doesn't implement value semantics
  private static class TemporaryPair<A extends @Nullable Object, B extends @Nullable Object> {
    @ParametricNullness final A a;
    @ParametricNullness final B b;

    TemporaryPair(@ParametricNullness A a, @ParametricNullness B b) {
      this.a = a;
      this.b = b;
    }
  }

  /**
   * Returns a stream consisting of the results of applying the given function to the elements of
   * {@code stream} and their indices in the stream. For example,
   *
   * <pre>{@code
   * mapWithIndex(
   *     Stream.of("a", "b", "c"),
   *     (e, index) -> index + ":" + e)
   * }</pre>
   *
   * <p>would return {@code Stream.of("0:a", "1:b", "2:c")}.
   *
   * <p>The resulting stream is <a
   * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
   * if and only if {@code stream} was efficiently splittable and its underlying spliterator
   * reported {@link Spliterator#SUBSIZED}. This is generally the case if the underlying stream
   * comes from a data structure supporting efficient indexed random access, typically an array or
   * list.
   *
   * <p>The order of the resulting stream is defined if and only if the order of the original stream
   * was defined.
   */
  public static <T extends @Nullable Object, R extends @Nullable Object> Stream<R> mapWithIndex(
      Stream<T> stream, FunctionWithIndex<? super T, ? extends R> function) {
    checkNotNull(stream);
    checkNotNull(function);
    boolean isParallel = stream.isParallel();
    Spliterator<T> fromSpliterator = stream.spliterator();

    if (!fromSpliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
      Iterator<T> fromIterator = Spliterators.iterator(fromSpliterator);
      return StreamSupport.stream(
              new AbstractSpliterator<R>(
                  fromSpliterator.estimateSize(),
                  fromSpliterator.characteristics() & (Spliterator.ORDERED | Spliterator.SIZED)) {
                long index = 0;

                @Override
                public boolean tryAdvance(Consumer<? super R> action) {
                  if (fromIterator.hasNext()) {
                    action.accept(function.apply(fromIterator.next(), index++));
                    return true;
                  }
                  return false;
                }
              },
              isParallel)
          .onClose(stream::close);
    }
    class Splitr extends MapWithIndexSpliterator<Spliterator<T>, R, Splitr> implements Consumer<T> {
      @CheckForNull T holder;

      Splitr(Spliterator<T> splitr, long index) {
        super(splitr, index);
      }

      @Override
      public void accept(@ParametricNullness T t) {
        this.holder = t;
      }

      @Override
      public boolean tryAdvance(Consumer<? super R> action) {
        if (fromSpliterator.tryAdvance(this)) {
          try {
            // The cast is safe because tryAdvance puts a T into `holder`.
            action.accept(function.apply(uncheckedCastNullableTToT(holder), index++));
            return true;
          } finally {
            holder = null;
          }
        }
        return false;
      }

      @Override
      Splitr createSplit(Spliterator<T> from, long i) {
        return new Splitr(from, i);
      }
    }
    return StreamSupport.stream(new Splitr(fromSpliterator, 0), isParallel).onClose(stream::close);
  }

  /**
   * Returns a stream consisting of the results of applying the given function to the elements of
   * {@code stream} and their indexes in the stream. For example,
   *
   * <pre>{@code
   * mapWithIndex(
   *     IntStream.of(10, 11, 12),
   *     (e, index) -> index + ":" + e)
   * }</pre>
   *
   * <p>...would return {@code Stream.of("0:10", "1:11", "2:12")}.
   *
   * <p>The resulting stream is <a
   * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
   * if and only if {@code stream} was efficiently splittable and its underlying spliterator
   * reported {@link Spliterator#SUBSIZED}. This is generally the case if the underlying stream
   * comes from a data structure supporting efficient indexed random access, typically an array or
   * list.
   *
   * <p>The order of the resulting stream is defined if and only if the order of the original stream
   * was defined.
   */
  public static <R extends @Nullable Object> Stream<R> mapWithIndex(
      IntStream stream, IntFunctionWithIndex<R> function) {
    checkNotNull(stream);
    checkNotNull(function);
    boolean isParallel = stream.isParallel();
    Spliterator.OfInt fromSpliterator = stream.spliterator();

    if (!fromSpliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
      PrimitiveIterator.OfInt fromIterator = Spliterators.iterator(fromSpliterator);
      return StreamSupport.stream(
              new AbstractSpliterator<R>(
                  fromSpliterator.estimateSize(),
                  fromSpliterator.characteristics() & (Spliterator.ORDERED | Spliterator.SIZED)) {
                long index = 0;

                @Override
                public boolean tryAdvance(Consumer<? super R> action) {
                  if (fromIterator.hasNext()) {
                    action.accept(function.apply(fromIterator.nextInt(), index++));
                    return true;
                  }
                  return false;
                }
              },
              isParallel)
          .onClose(stream::close);
    }
    class Splitr extends MapWithIndexSpliterator<Spliterator.OfInt, R, Splitr>
        implements IntConsumer, Spliterator<R> {
      int holder;

      Splitr(Spliterator.OfInt splitr, long index) {
        super(splitr, index);
      }

      @Override
      public void accept(int t) {
        this.holder = t;
      }

      @Override
      public boolean tryAdvance(Consumer<? super R> action) {
        if (fromSpliterator.tryAdvance(this)) {
          action.accept(function.apply(holder, index++));
          return true;
        }
        return false;
      }

      @Override
      Splitr createSplit(Spliterator.OfInt from, long i) {
        return new Splitr(from, i);
      }
    }
    return StreamSupport.stream(new Splitr(fromSpliterator, 0), isParallel).onClose(stream::close);
  }

  /**
   * Returns a stream consisting of the results of applying the given function to the elements of
   * {@code stream} and their indexes in the stream. For example,
   *
   * <pre>{@code
   * mapWithIndex(
   *     LongStream.of(10, 11, 12),
   *     (e, index) -> index + ":" + e)
   * }</pre>
   *
   * <p>...would return {@code Stream.of("0:10", "1:11", "2:12")}.
   *
   * <p>The resulting stream is <a
   * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
   * if and only if {@code stream} was efficiently splittable and its underlying spliterator
   * reported {@link Spliterator#SUBSIZED}. This is generally the case if the underlying stream
   * comes from a data structure supporting efficient indexed random access, typically an array or
   * list.
   *
   * <p>The order of the resulting stream is defined if and only if the order of the original stream
   * was defined.
   */
  public static <R extends @Nullable Object> Stream<R> mapWithIndex(
      LongStream stream, LongFunctionWithIndex<R> function) {
    checkNotNull(stream);
    checkNotNull(function);
    boolean isParallel = stream.isParallel();
    Spliterator.OfLong fromSpliterator = stream.spliterator();

    if (!fromSpliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
      PrimitiveIterator.OfLong fromIterator = Spliterators.iterator(fromSpliterator);
      return StreamSupport.stream(
              new AbstractSpliterator<R>(
                  fromSpliterator.estimateSize(),
                  fromSpliterator.characteristics() & (Spliterator.ORDERED | Spliterator.SIZED)) {
                long index = 0;

                @Override
                public boolean tryAdvance(Consumer<? super R> action) {
                  if (fromIterator.hasNext()) {
                    action.accept(function.apply(fromIterator.nextLong(), index++));
                    return true;
                  }
                  return false;
                }
              },
              isParallel)
          .onClose(stream::close);
    }
    class Splitr extends MapWithIndexSpliterator<Spliterator.OfLong, R, Splitr>
        implements LongConsumer, Spliterator<R> {
      long holder;

      Splitr(Spliterator.OfLong splitr, long index) {
        super(splitr, index);
      }

      @Override
      public void accept(long t) {
        this.holder = t;
      }

      @Override
      public boolean tryAdvance(Consumer<? super R> action) {
        if (fromSpliterator.tryAdvance(this)) {
          action.accept(function.apply(holder, index++));
          return true;
        }
        return false;
      }

      @Override
      Splitr createSplit(Spliterator.OfLong from, long i) {
        return new Splitr(from, i);
      }
    }
    return StreamSupport.stream(new Splitr(fromSpliterator, 0), isParallel).onClose(stream::close);
  }

  /**
   * Returns a stream consisting of the results of applying the given function to the elements of
   * {@code stream} and their indexes in the stream. For example,
   *
   * <pre>{@code
   * mapWithIndex(
   *     DoubleStream.of(0.0, 1.0, 2.0)
   *     (e, index) -> index + ":" + e)
   * }</pre>
   *
   * <p>...would return {@code Stream.of("0:0.0", "1:1.0", "2:2.0")}.
   *
   * <p>The resulting stream is <a
   * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
   * if and only if {@code stream} was efficiently splittable and its underlying spliterator
   * reported {@link Spliterator#SUBSIZED}. This is generally the case if the underlying stream
   * comes from a data structure supporting efficient indexed random access, typically an array or
   * list.
   *
   * <p>The order of the resulting stream is defined if and only if the order of the original stream
   * was defined.
   */
  public static <R extends @Nullable Object> Stream<R> mapWithIndex(
      DoubleStream stream, DoubleFunctionWithIndex<R> function) {
    checkNotNull(stream);
    checkNotNull(function);
    boolean isParallel = stream.isParallel();
    Spliterator.OfDouble fromSpliterator = stream.spliterator();

    if (!fromSpliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
      PrimitiveIterator.OfDouble fromIterator = Spliterators.iterator(fromSpliterator);
      return StreamSupport.stream(
              new AbstractSpliterator<R>(
                  fromSpliterator.estimateSize(),
                  fromSpliterator.characteristics() & (Spliterator.ORDERED | Spliterator.SIZED)) {
                long index = 0;

                @Override
                public boolean tryAdvance(Consumer<? super R> action) {
                  if (fromIterator.hasNext()) {
                    action.accept(function.apply(fromIterator.nextDouble(), index++));
                    return true;
                  }
                  return false;
                }
              },
              isParallel)
          .onClose(stream::close);
    }
    class Splitr extends MapWithIndexSpliterator<Spliterator.OfDouble, R, Splitr>
        implements DoubleConsumer, Spliterator<R> {
      double holder;

      Splitr(Spliterator.OfDouble splitr, long index) {
        super(splitr, index);
      }

      @Override
      public void accept(double t) {
        this.holder = t;
      }

      @Override
      public boolean tryAdvance(Consumer<? super R> action) {
        if (fromSpliterator.tryAdvance(this)) {
          action.accept(function.apply(holder, index++));
          return true;
        }
        return false;
      }

      @Override
      Splitr createSplit(Spliterator.OfDouble from, long i) {
        return new Splitr(from, i);
      }
    }
    return StreamSupport.stream(new Splitr(fromSpliterator, 0), isParallel).onClose(stream::close);
  }

  /**
   * An analogue of {@link java.util.function.Function} also accepting an index.
   *
   * <p>This interface is only intended for use by callers of {@link #mapWithIndex(Stream,
   * FunctionWithIndex)}.
   *
   * @since 21.0
   */
  public interface FunctionWithIndex<T extends @Nullable Object, R extends @Nullable Object> {
    /** Applies this function to the given argument and its index within a stream. */
    @ParametricNullness
    R apply(@ParametricNullness T from, long index);
  }

  private abstract static class MapWithIndexSpliterator<
          F extends Spliterator<?>,
          R extends @Nullable Object,
          S extends MapWithIndexSpliterator<F, R, S>>
      implements Spliterator<R> {
    final F fromSpliterator;
    long index;

    MapWithIndexSpliterator(F fromSpliterator, long index) {
      this.fromSpliterator = fromSpliterator;
      this.index = index;
    }

    abstract S createSplit(F from, long i);

    @Override
    @CheckForNull
    public S trySplit() {
      Spliterator<?> splitOrNull = fromSpliterator.trySplit();
      if (splitOrNull == null) {
        return null;
      }
      @SuppressWarnings("unchecked")
      F split = (F) splitOrNull;
      S result = createSplit(split, index);
      this.index += split.getExactSizeIfKnown();
      return result;
    }

    @Override
    public long estimateSize() {
      return fromSpliterator.estimateSize();
    }

    @Override
    public int characteristics() {
      return fromSpliterator.characteristics()
          & (Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED);
    }
  }

  /**
   * An analogue of {@link java.util.function.IntFunction} also accepting an index.
   *
   * <p>This interface is only intended for use by callers of {@link #mapWithIndex(IntStream,
   * IntFunctionWithIndex)}.
   *
   * @since 21.0
   */
  public interface IntFunctionWithIndex<R extends @Nullable Object> {
    /** Applies this function to the given argument and its index within a stream. */
    @ParametricNullness
    R apply(int from, long index);
  }

  /**
   * An analogue of {@link java.util.function.LongFunction} also accepting an index.
   *
   * <p>This interface is only intended for use by callers of {@link #mapWithIndex(LongStream,
   * LongFunctionWithIndex)}.
   *
   * @since 21.0
   */
  public interface LongFunctionWithIndex<R extends @Nullable Object> {
    /** Applies this function to the given argument and its index within a stream. */
    @ParametricNullness
    R apply(long from, long index);
  }

  /**
   * An analogue of {@link java.util.function.DoubleFunction} also accepting an index.
   *
   * <p>This interface is only intended for use by callers of {@link #mapWithIndex(DoubleStream,
   * DoubleFunctionWithIndex)}.
   *
   * @since 21.0
   */
  public interface DoubleFunctionWithIndex<R extends @Nullable Object> {
    /** Applies this function to the given argument and its index within a stream. */
    @ParametricNullness
    R apply(double from, long index);
  }

  /**
   * Returns the last element of the specified stream, or {@link java.util.Optional#empty} if the
   * stream is empty.
   *
   * <p>Equivalent to {@code stream.reduce((a, b) -> b)}, but may perform significantly better. This
   * method's runtime will be between O(log n) and O(n), performing better on <a
   * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
   * streams.
   *
   * <p>If the stream has nondeterministic order, this has equivalent semantics to {@link
   * Stream#findAny} (which you might as well use).
   *
   * @see Stream#findFirst()
   * @throws NullPointerException if the last element of the stream is null
   */
  /*
   * By declaring <T> instead of <T extends @Nullable Object>, we declare this method as requiring a
   * stream whose elements are non-null. However, the method goes out of its way to still handle
   * nulls in the stream. This means that the method can safely be used with a stream that contains
   * nulls as long as the *last* element is *not* null.
   *
   * (To "go out of its way," the method tracks a `set` bit so that it can distinguish "the final
   * split has a last element of null, so throw NPE" from "the final split was empty, so look for an
   * element in the prior one.")
   */
  public static <T> java.util.Optional<T> findLast(Stream<T> stream) {
    class OptionalState {
      boolean set = false;
      @CheckForNull T value = null;

      void set(T value) {
        this.set = true;
        this.value = value;
      }

      T get() {
        /*
         * requireNonNull is safe because we call get() only if we've previously called set().
         *
         * (For further discussion of nullness, see the comment above the method.)
         */
        return requireNonNull(value);
      }
    }
    OptionalState state = new OptionalState();

    Deque<Spliterator<T>> splits = new ArrayDeque<>();
    splits.addLast(stream.spliterator());

    while (!splits.isEmpty()) {
      Spliterator<T> spliterator = splits.removeLast();

      if (spliterator.getExactSizeIfKnown() == 0) {
        continue; // drop this split
      }

      // Many spliterators will have trySplits that are SUBSIZED even if they are not themselves
      // SUBSIZED.
      if (spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
        // we can drill down to exactly the smallest nonempty spliterator
        while (true) {
          Spliterator<T> prefix = spliterator.trySplit();
          if (prefix == null || prefix.getExactSizeIfKnown() == 0) {
            break;
          } else if (spliterator.getExactSizeIfKnown() == 0) {
            spliterator = prefix;
            break;
          }
        }

        // spliterator is known to be nonempty now
        spliterator.forEachRemaining(state::set);
        return java.util.Optional.of(state.get());
      }

      Spliterator<T> prefix = spliterator.trySplit();
      if (prefix == null || prefix.getExactSizeIfKnown() == 0) {
        // we can't split this any further
        spliterator.forEachRemaining(state::set);
        if (state.set) {
          return java.util.Optional.of(state.get());
        }
        // fall back to the last split
        continue;
      }
      splits.addLast(prefix);
      splits.addLast(spliterator);
    }
    return java.util.Optional.empty();
  }

  /**
   * Returns the last element of the specified stream, or {@link OptionalInt#empty} if the stream is
   * empty.
   *
   * <p>Equivalent to {@code stream.reduce((a, b) -> b)}, but may perform significantly better. This
   * method's runtime will be between O(log n) and O(n), performing better on <a
   * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
   * streams.
   *
   * @see IntStream#findFirst()
   * @throws NullPointerException if the last element of the stream is null
   */
  public static OptionalInt findLast(IntStream stream) {
    // findLast(Stream) does some allocation, so we might as well box some more
    java.util.Optional<Integer> boxedLast = findLast(stream.boxed());
    return boxedLast.map(OptionalInt::of).orElse(OptionalInt.empty());
  }

  /**
   * Returns the last element of the specified stream, or {@link OptionalLong#empty} if the stream
   * is empty.
   *
   * <p>Equivalent to {@code stream.reduce((a, b) -> b)}, but may perform significantly better. This
   * method's runtime will be between O(log n) and O(n), performing better on <a
   * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
   * streams.
   *
   * @see LongStream#findFirst()
   * @throws NullPointerException if the last element of the stream is null
   */
  public static OptionalLong findLast(LongStream stream) {
    // findLast(Stream) does some allocation, so we might as well box some more
    java.util.Optional<Long> boxedLast = findLast(stream.boxed());
    return boxedLast.map(OptionalLong::of).orElse(OptionalLong.empty());
  }

  /**
   * Returns the last element of the specified stream, or {@link OptionalDouble#empty} if the stream
   * is empty.
   *
   * <p>Equivalent to {@code stream.reduce((a, b) -> b)}, but may perform significantly better. This
   * method's runtime will be between O(log n) and O(n), performing better on <a
   * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
   * streams.
   *
   * @see DoubleStream#findFirst()
   * @throws NullPointerException if the last element of the stream is null
   */
  public static OptionalDouble findLast(DoubleStream stream) {
    // findLast(Stream) does some allocation, so we might as well box some more
    java.util.Optional<Double> boxedLast = findLast(stream.boxed());
    return boxedLast.map(OptionalDouble::of).orElse(OptionalDouble.empty());
  }

  private Streams() {}
}

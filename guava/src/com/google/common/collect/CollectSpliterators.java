/*
 * Copyright (C) 2015 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;
import static java.lang.Math.max;

import com.google.common.annotations.GwtCompatible;
import com.google.j2objc.annotations.Weak;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Spliterator utilities for {@code common.collect} internals. */
@GwtCompatible
@ElementTypesAreNonnullByDefault
final class CollectSpliterators {
  private CollectSpliterators() {}

  static <T extends @Nullable Object> Spliterator<T> indexed(
      int size, int extraCharacteristics, IntFunction<T> function) {
    return indexed(size, extraCharacteristics, function, null);
  }

  static <T extends @Nullable Object> Spliterator<T> indexed(
      int size,
      int extraCharacteristics,
      IntFunction<T> function,
      @CheckForNull Comparator<? super T> comparator) {
    if (comparator != null) {
      checkArgument((extraCharacteristics & Spliterator.SORTED) != 0);
    }
    class WithCharacteristics implements Spliterator<T> {
      private final Spliterator.OfInt delegate;

      WithCharacteristics(Spliterator.OfInt delegate) {
        this.delegate = delegate;
      }

      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
        return delegate.tryAdvance((IntConsumer) i -> action.accept(function.apply(i)));
      }

      @Override
      public void forEachRemaining(Consumer<? super T> action) {
        delegate.forEachRemaining((IntConsumer) i -> action.accept(function.apply(i)));
      }

      @Override
      @CheckForNull
      public Spliterator<T> trySplit() {
        Spliterator.OfInt split = delegate.trySplit();
        return (split == null) ? null : new WithCharacteristics(split);
      }

      @Override
      public long estimateSize() {
        return delegate.estimateSize();
      }

      @Override
      public int characteristics() {
        return Spliterator.ORDERED
            | Spliterator.SIZED
            | Spliterator.SUBSIZED
            | extraCharacteristics;
      }

      @Override
      @CheckForNull
      public Comparator<? super T> getComparator() {
        if (hasCharacteristics(Spliterator.SORTED)) {
          return comparator;
        } else {
          throw new IllegalStateException();
        }
      }
    }
    return new WithCharacteristics(IntStream.range(0, size).spliterator());
  }

  /**
   * Returns a {@code Spliterator} over the elements of {@code fromSpliterator} mapped by {@code
   * function}.
   */
  static <InElementT extends @Nullable Object, OutElementT extends @Nullable Object>
      Spliterator<OutElementT> map(
          Spliterator<InElementT> fromSpliterator,
          Function<? super InElementT, ? extends OutElementT> function) {
    checkNotNull(fromSpliterator);
    checkNotNull(function);
    return new Spliterator<OutElementT>() {

      @Override
      public boolean tryAdvance(Consumer<? super OutElementT> action) {
        return fromSpliterator.tryAdvance(
            fromElement -> action.accept(function.apply(fromElement)));
      }

      @Override
      public void forEachRemaining(Consumer<? super OutElementT> action) {
        fromSpliterator.forEachRemaining(fromElement -> action.accept(function.apply(fromElement)));
      }

      @Override
      @CheckForNull
      public Spliterator<OutElementT> trySplit() {
        Spliterator<InElementT> fromSplit = fromSpliterator.trySplit();
        return (fromSplit != null) ? map(fromSplit, function) : null;
      }

      @Override
      public long estimateSize() {
        return fromSpliterator.estimateSize();
      }

      @Override
      public int characteristics() {
        return fromSpliterator.characteristics()
            & ~(Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.SORTED);
      }
    };
  }

  /** Returns a {@code Spliterator} filtered by the specified predicate. */
  static <T extends @Nullable Object> Spliterator<T> filter(
      Spliterator<T> fromSpliterator, Predicate<? super T> predicate) {
    checkNotNull(fromSpliterator);
    checkNotNull(predicate);
    class Splitr implements Spliterator<T>, Consumer<T> {
      @CheckForNull T holder = null;

      @Override
      public void accept(@ParametricNullness T t) {
        this.holder = t;
      }

      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
        while (fromSpliterator.tryAdvance(this)) {
          try {
            // The cast is safe because tryAdvance puts a T into `holder`.
            T next = uncheckedCastNullableTToT(holder);
            if (predicate.test(next)) {
              action.accept(next);
              return true;
            }
          } finally {
            holder = null;
          }
        }
        return false;
      }

      @Override
      @CheckForNull
      public Spliterator<T> trySplit() {
        Spliterator<T> fromSplit = fromSpliterator.trySplit();
        return (fromSplit == null) ? null : filter(fromSplit, predicate);
      }

      @Override
      public long estimateSize() {
        return fromSpliterator.estimateSize() / 2;
      }

      @Override
      @CheckForNull
      public Comparator<? super T> getComparator() {
        return fromSpliterator.getComparator();
      }

      @Override
      public int characteristics() {
        return fromSpliterator.characteristics()
            & (Spliterator.DISTINCT
                | Spliterator.NONNULL
                | Spliterator.ORDERED
                | Spliterator.SORTED);
      }
    }
    return new Splitr();
  }

  /**
   * Returns a {@code Spliterator} that iterates over the elements of the spliterators generated by
   * applying {@code function} to the elements of {@code fromSpliterator}.
   */
  static <InElementT extends @Nullable Object, OutElementT extends @Nullable Object>
      Spliterator<OutElementT> flatMap(
          Spliterator<InElementT> fromSpliterator,
          Function<? super InElementT, Spliterator<OutElementT>> function,
          int topCharacteristics,
          long topSize) {
    checkArgument(
        (topCharacteristics & Spliterator.SUBSIZED) == 0,
        "flatMap does not support SUBSIZED characteristic");
    checkArgument(
        (topCharacteristics & Spliterator.SORTED) == 0,
        "flatMap does not support SORTED characteristic");
    checkNotNull(fromSpliterator);
    checkNotNull(function);
    return new FlatMapSpliteratorOfObject<>(
        null, fromSpliterator, function, topCharacteristics, topSize);
  }

  /**
   * Returns a {@code Spliterator.OfInt} that iterates over the elements of the spliterators
   * generated by applying {@code function} to the elements of {@code fromSpliterator}. (If {@code
   * function} returns {@code null} for an input, it is replaced with an empty stream.)
   */
  static <InElementT extends @Nullable Object> Spliterator.OfInt flatMapToInt(
      Spliterator<InElementT> fromSpliterator,
      Function<? super InElementT, Spliterator.OfInt> function,
      int topCharacteristics,
      long topSize) {
    checkArgument(
        (topCharacteristics & Spliterator.SUBSIZED) == 0,
        "flatMap does not support SUBSIZED characteristic");
    checkArgument(
        (topCharacteristics & Spliterator.SORTED) == 0,
        "flatMap does not support SORTED characteristic");
    checkNotNull(fromSpliterator);
    checkNotNull(function);
    return new FlatMapSpliteratorOfInt<>(
        null, fromSpliterator, function, topCharacteristics, topSize);
  }

  /**
   * Returns a {@code Spliterator.OfLong} that iterates over the elements of the spliterators
   * generated by applying {@code function} to the elements of {@code fromSpliterator}. (If {@code
   * function} returns {@code null} for an input, it is replaced with an empty stream.)
   */
  static <InElementT extends @Nullable Object> Spliterator.OfLong flatMapToLong(
      Spliterator<InElementT> fromSpliterator,
      Function<? super InElementT, Spliterator.OfLong> function,
      int topCharacteristics,
      long topSize) {
    checkArgument(
        (topCharacteristics & Spliterator.SUBSIZED) == 0,
        "flatMap does not support SUBSIZED characteristic");
    checkArgument(
        (topCharacteristics & Spliterator.SORTED) == 0,
        "flatMap does not support SORTED characteristic");
    checkNotNull(fromSpliterator);
    checkNotNull(function);
    return new FlatMapSpliteratorOfLong<>(
        null, fromSpliterator, function, topCharacteristics, topSize);
  }

  /**
   * Returns a {@code Spliterator.OfDouble} that iterates over the elements of the spliterators
   * generated by applying {@code function} to the elements of {@code fromSpliterator}. (If {@code
   * function} returns {@code null} for an input, it is replaced with an empty stream.)
   */
  static <InElementT extends @Nullable Object> Spliterator.OfDouble flatMapToDouble(
      Spliterator<InElementT> fromSpliterator,
      Function<? super InElementT, Spliterator.OfDouble> function,
      int topCharacteristics,
      long topSize) {
    checkArgument(
        (topCharacteristics & Spliterator.SUBSIZED) == 0,
        "flatMap does not support SUBSIZED characteristic");
    checkArgument(
        (topCharacteristics & Spliterator.SORTED) == 0,
        "flatMap does not support SORTED characteristic");
    checkNotNull(fromSpliterator);
    checkNotNull(function);
    return new FlatMapSpliteratorOfDouble<>(
        null, fromSpliterator, function, topCharacteristics, topSize);
  }

  /**
   * Implements the {@link Stream#flatMap} operation on spliterators.
   *
   * @param <InElementT> the element type of the input spliterator
   * @param <OutElementT> the element type of the output spliterators
   * @param <OutSpliteratorT> the type of the output spliterators
   */
  abstract static class FlatMapSpliterator<
          InElementT extends @Nullable Object,
          OutElementT extends @Nullable Object,
          OutSpliteratorT extends Spliterator<OutElementT>>
      implements Spliterator<OutElementT> {
    /** Factory for constructing {@link FlatMapSpliterator} instances. */
    @FunctionalInterface
    interface Factory<InElementT extends @Nullable Object, OutSpliteratorT extends Spliterator<?>> {
      OutSpliteratorT newFlatMapSpliterator(
          @CheckForNull OutSpliteratorT prefix,
          Spliterator<InElementT> fromSplit,
          Function<? super InElementT, OutSpliteratorT> function,
          int splitCharacteristics,
          long estSplitSize);
    }

    @Weak @CheckForNull OutSpliteratorT prefix;
    final Spliterator<InElementT> from;
    final Function<? super InElementT, OutSpliteratorT> function;
    final Factory<InElementT, OutSpliteratorT> factory;
    int characteristics;
    long estimatedSize;

    FlatMapSpliterator(
        @CheckForNull OutSpliteratorT prefix,
        Spliterator<InElementT> from,
        Function<? super InElementT, OutSpliteratorT> function,
        Factory<InElementT, OutSpliteratorT> factory,
        int characteristics,
        long estimatedSize) {
      this.prefix = prefix;
      this.from = from;
      this.function = function;
      this.factory = factory;
      this.characteristics = characteristics;
      this.estimatedSize = estimatedSize;
    }

    /*
     * The tryAdvance and forEachRemaining in FlatMapSpliteratorOfPrimitive are overloads of these
     * methods, not overrides. They are annotated @Override because they implement methods from
     * Spliterator.OfPrimitive (and override default implementations from Spliterator.OfPrimitive or
     * a subtype like Spliterator.OfInt).
     */

    @Override
    public final boolean tryAdvance(Consumer<? super OutElementT> action) {
      while (true) {
        if (prefix != null && prefix.tryAdvance(action)) {
          if (estimatedSize != Long.MAX_VALUE) {
            estimatedSize--;
          }
          return true;
        } else {
          prefix = null;
        }
        if (!from.tryAdvance(fromElement -> prefix = function.apply(fromElement))) {
          return false;
        }
      }
    }

    @Override
    public final void forEachRemaining(Consumer<? super OutElementT> action) {
      if (prefix != null) {
        prefix.forEachRemaining(action);
        prefix = null;
      }
      from.forEachRemaining(
          fromElement -> {
            Spliterator<OutElementT> elements = function.apply(fromElement);
            if (elements != null) {
              elements.forEachRemaining(action);
            }
          });
      estimatedSize = 0;
    }

    @Override
    @CheckForNull
    public final OutSpliteratorT trySplit() {
      Spliterator<InElementT> fromSplit = from.trySplit();
      if (fromSplit != null) {
        int splitCharacteristics = characteristics & ~Spliterator.SIZED;
        long estSplitSize = estimateSize();
        if (estSplitSize < Long.MAX_VALUE) {
          estSplitSize /= 2;
          this.estimatedSize -= estSplitSize;
          this.characteristics = splitCharacteristics;
        }
        OutSpliteratorT result =
            factory.newFlatMapSpliterator(
                this.prefix, fromSplit, function, splitCharacteristics, estSplitSize);
        this.prefix = null;
        return result;
      } else if (prefix != null) {
        OutSpliteratorT result = prefix;
        this.prefix = null;
        return result;
      } else {
        return null;
      }
    }

    @Override
    public final long estimateSize() {
      if (prefix != null) {
        estimatedSize = max(estimatedSize, prefix.estimateSize());
      }
      return max(estimatedSize, 0);
    }

    @Override
    public final int characteristics() {
      return characteristics;
    }
  }

  /**
   * Implementation of {@link Stream#flatMap} with an object spliterator output type.
   *
   * <p>To avoid having this type, we could use {@code FlatMapSpliterator} directly. The main
   * advantages to having the type are the ability to use its constructor reference below and the
   * parallelism with the primitive version. In short, it makes its caller ({@code flatMap})
   * simpler.
   *
   * @param <InElementT> the element type of the input spliterator
   * @param <OutElementT> the element type of the output spliterators
   */
  static final class FlatMapSpliteratorOfObject<
          InElementT extends @Nullable Object, OutElementT extends @Nullable Object>
      extends FlatMapSpliterator<InElementT, OutElementT, Spliterator<OutElementT>> {
    FlatMapSpliteratorOfObject(
        @CheckForNull Spliterator<OutElementT> prefix,
        Spliterator<InElementT> from,
        Function<? super InElementT, Spliterator<OutElementT>> function,
        int characteristics,
        long estimatedSize) {
      super(
          prefix, from, function, FlatMapSpliteratorOfObject::new, characteristics, estimatedSize);
    }
  }

  /**
   * Implementation of {@link Stream#flatMap} with a primitive spliterator output type.
   *
   * @param <InElementT> the element type of the input spliterator
   * @param <OutElementT> the (boxed) element type of the output spliterators
   * @param <OutConsumerT> the specialized consumer type for the primitive output type
   * @param <OutSpliteratorT> the primitive spliterator type associated with {@code OutElementT}
   */
  abstract static class FlatMapSpliteratorOfPrimitive<
          InElementT extends @Nullable Object,
          OutElementT extends @Nullable Object,
          OutConsumerT,
          OutSpliteratorT extends
              Spliterator.OfPrimitive<OutElementT, OutConsumerT, OutSpliteratorT>>
      extends FlatMapSpliterator<InElementT, OutElementT, OutSpliteratorT>
      implements Spliterator.OfPrimitive<OutElementT, OutConsumerT, OutSpliteratorT> {

    FlatMapSpliteratorOfPrimitive(
        @CheckForNull OutSpliteratorT prefix,
        Spliterator<InElementT> from,
        Function<? super InElementT, OutSpliteratorT> function,
        Factory<InElementT, OutSpliteratorT> factory,
        int characteristics,
        long estimatedSize) {
      super(prefix, from, function, factory, characteristics, estimatedSize);
    }

    @Override
    public final boolean tryAdvance(OutConsumerT action) {
      while (true) {
        if (prefix != null && prefix.tryAdvance(action)) {
          if (estimatedSize != Long.MAX_VALUE) {
            estimatedSize--;
          }
          return true;
        } else {
          prefix = null;
        }
        if (!from.tryAdvance(fromElement -> prefix = function.apply(fromElement))) {
          return false;
        }
      }
    }

    @Override
    public final void forEachRemaining(OutConsumerT action) {
      if (prefix != null) {
        prefix.forEachRemaining(action);
        prefix = null;
      }
      from.forEachRemaining(
          fromElement -> {
            OutSpliteratorT elements = function.apply(fromElement);
            if (elements != null) {
              elements.forEachRemaining(action);
            }
          });
      estimatedSize = 0;
    }
  }

  /** Implementation of {@link #flatMapToInt}. */
  static final class FlatMapSpliteratorOfInt<InElementT extends @Nullable Object>
      extends FlatMapSpliteratorOfPrimitive<InElementT, Integer, IntConsumer, Spliterator.OfInt>
      implements Spliterator.OfInt {
    FlatMapSpliteratorOfInt(
        @CheckForNull Spliterator.OfInt prefix,
        Spliterator<InElementT> from,
        Function<? super InElementT, Spliterator.OfInt> function,
        int characteristics,
        long estimatedSize) {
      super(prefix, from, function, FlatMapSpliteratorOfInt::new, characteristics, estimatedSize);
    }
  }

  /** Implementation of {@link #flatMapToLong}. */
  static final class FlatMapSpliteratorOfLong<InElementT extends @Nullable Object>
      extends FlatMapSpliteratorOfPrimitive<InElementT, Long, LongConsumer, Spliterator.OfLong>
      implements Spliterator.OfLong {
    FlatMapSpliteratorOfLong(
        @CheckForNull Spliterator.OfLong prefix,
        Spliterator<InElementT> from,
        Function<? super InElementT, Spliterator.OfLong> function,
        int characteristics,
        long estimatedSize) {
      super(prefix, from, function, FlatMapSpliteratorOfLong::new, characteristics, estimatedSize);
    }
  }

  /** Implementation of {@link #flatMapToDouble}. */
  static final class FlatMapSpliteratorOfDouble<InElementT extends @Nullable Object>
      extends FlatMapSpliteratorOfPrimitive<
          InElementT, Double, DoubleConsumer, Spliterator.OfDouble>
      implements Spliterator.OfDouble {
    FlatMapSpliteratorOfDouble(
        @CheckForNull Spliterator.OfDouble prefix,
        Spliterator<InElementT> from,
        Function<? super InElementT, Spliterator.OfDouble> function,
        int characteristics,
        long estimatedSize) {
      super(
          prefix, from, function, FlatMapSpliteratorOfDouble::new, characteristics, estimatedSize);
    }
  }
}

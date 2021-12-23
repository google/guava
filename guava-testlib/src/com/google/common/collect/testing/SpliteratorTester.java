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

package com.google.common.collect.testing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.testing.Helpers.assertEqualIgnoringOrder;
import static com.google.common.collect.testing.Helpers.assertEqualInOrder;
import static com.google.common.collect.testing.Platform.format;
import static java.util.Comparator.naturalOrder;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Tester for {@code Spliterator} implementations. */
@GwtCompatible
public final class SpliteratorTester<E> {
  /** Return type from "contains the following elements" assertions. */
  public interface Ordered {
    /**
     * Attests that the expected values must not just be present but must be present in the order
     * they were given.
     */
    void inOrder();
  }

  private abstract static class GeneralSpliterator<E> {
    final Spliterator<E> spliterator;

    GeneralSpliterator(Spliterator<E> spliterator) {
      this.spliterator = checkNotNull(spliterator);
    }

    abstract void forEachRemaining(Consumer<? super E> action);

    abstract boolean tryAdvance(Consumer<? super E> action);

    abstract GeneralSpliterator<E> trySplit();

    final int characteristics() {
      return spliterator.characteristics();
    }

    final long estimateSize() {
      return spliterator.estimateSize();
    }

    final Comparator<? super E> getComparator() {
      return spliterator.getComparator();
    }

    final long getExactSizeIfKnown() {
      return spliterator.getExactSizeIfKnown();
    }

    final boolean hasCharacteristics(int characteristics) {
      return spliterator.hasCharacteristics(characteristics);
    }
  }

  private static final class GeneralSpliteratorOfObject<E> extends GeneralSpliterator<E> {
    GeneralSpliteratorOfObject(Spliterator<E> spliterator) {
      super(spliterator);
    }

    @Override
    void forEachRemaining(Consumer<? super E> action) {
      spliterator.forEachRemaining(action);
    }

    @Override
    boolean tryAdvance(Consumer<? super E> action) {
      return spliterator.tryAdvance(action);
    }

    @Override
    GeneralSpliterator<E> trySplit() {
      Spliterator<E> split = spliterator.trySplit();
      return split == null ? null : new GeneralSpliteratorOfObject<>(split);
    }
  }

  /*
   * The AndroidJdkLibsChecker violation is informing us that this method isn't usable under
   * Desugar. But we want to include it here for Nougat+ users -- and, mainly, for non-Android
   * users. Fortunately, anyone who tries to use it under Desugar will presumably already see errors
   * from creating the Spliterator.OfInt in the first place. So it's probably OK for us to suppress
   * this particular violation.
   */
  @SuppressWarnings("AndroidJdkLibsChecker")
  private static final class GeneralSpliteratorOfPrimitive<E, C> extends GeneralSpliterator<E> {
    final Spliterator.OfPrimitive<E, C, ?> spliterator;
    final Function<Consumer<? super E>, C> consumerizer;

    GeneralSpliteratorOfPrimitive(
        Spliterator.OfPrimitive<E, C, ?> spliterator,
        Function<Consumer<? super E>, C> consumerizer) {
      super(spliterator);
      this.spliterator = spliterator;
      this.consumerizer = consumerizer;
    }

    @Override
    void forEachRemaining(Consumer<? super E> action) {
      spliterator.forEachRemaining(consumerizer.apply(action));
    }

    @Override
    boolean tryAdvance(Consumer<? super E> action) {
      return spliterator.tryAdvance(consumerizer.apply(action));
    }

    @Override
    GeneralSpliterator<E> trySplit() {
      Spliterator.OfPrimitive<E, C, ?> split = spliterator.trySplit();
      return split == null ? null : new GeneralSpliteratorOfPrimitive<>(split, consumerizer);
    }
  }

  /**
   * Different ways of decomposing a Spliterator, all of which must produce the same elements (up to
   * ordering, if Spliterator.ORDERED is not present).
   */
  enum SpliteratorDecompositionStrategy {
    NO_SPLIT_FOR_EACH_REMAINING {
      @Override
      <E> void forEach(GeneralSpliterator<E> spliterator, Consumer<? super E> consumer) {
        spliterator.forEachRemaining(consumer);
      }
    },
    NO_SPLIT_TRY_ADVANCE {
      @Override
      <E> void forEach(GeneralSpliterator<E> spliterator, Consumer<? super E> consumer) {
        while (spliterator.tryAdvance(consumer)) {
          // do nothing
        }
      }
    },
    MAXIMUM_SPLIT {
      @Override
      <E> void forEach(GeneralSpliterator<E> spliterator, Consumer<? super E> consumer) {
        for (GeneralSpliterator<E> prefix = trySplitTestingSize(spliterator);
            prefix != null;
            prefix = trySplitTestingSize(spliterator)) {
          forEach(prefix, consumer);
        }
        long size = spliterator.getExactSizeIfKnown();
        long[] counter = {0};
        spliterator.forEachRemaining(
            e -> {
              consumer.accept(e);
              counter[0]++;
            });
        if (size >= 0) {
          assertEquals(size, counter[0]);
        }
      }
    },
    ALTERNATE_ADVANCE_AND_SPLIT {
      @Override
      <E> void forEach(GeneralSpliterator<E> spliterator, Consumer<? super E> consumer) {
        while (spliterator.tryAdvance(consumer)) {
          GeneralSpliterator<E> prefix = trySplitTestingSize(spliterator);
          if (prefix != null) {
            forEach(prefix, consumer);
          }
        }
      }
    };

    abstract <E> void forEach(GeneralSpliterator<E> spliterator, Consumer<? super E> consumer);
  }

  private static <E> @Nullable GeneralSpliterator<E> trySplitTestingSize(
      GeneralSpliterator<E> spliterator) {
    boolean subsized = spliterator.hasCharacteristics(Spliterator.SUBSIZED);
    long originalSize = spliterator.estimateSize();
    GeneralSpliterator<E> trySplit = spliterator.trySplit();
    if (spliterator.estimateSize() > originalSize) {
      fail(
          format(
              "estimated size of spliterator after trySplit (%s) is larger than original size (%s)",
              spliterator.estimateSize(), originalSize));
    }
    if (trySplit != null) {
      if (trySplit.estimateSize() > originalSize) {
        fail(
            format(
                "estimated size of trySplit result (%s) is larger than original size (%s)",
                trySplit.estimateSize(), originalSize));
      }
    }
    if (subsized) {
      if (trySplit != null) {
        assertEquals(
            "sum of estimated sizes of trySplit and original spliterator after trySplit",
            originalSize,
            trySplit.estimateSize() + spliterator.estimateSize());
      } else {
        assertEquals(
            "estimated size of spliterator after failed trySplit",
            originalSize,
            spliterator.estimateSize());
      }
    }
    return trySplit;
  }

  public static <E> SpliteratorTester<E> of(Supplier<Spliterator<E>> spliteratorSupplier) {
    return new SpliteratorTester<>(
        ImmutableSet.of(() -> new GeneralSpliteratorOfObject<>(spliteratorSupplier.get())));
  }

  /** @since 28.1 */
  @SuppressWarnings("AndroidJdkLibsChecker") // see comment on GeneralSpliteratorOfPrimitive
  public static SpliteratorTester<Integer> ofInt(Supplier<Spliterator.OfInt> spliteratorSupplier) {
    return new SpliteratorTester<>(
        ImmutableSet.of(
            () -> new GeneralSpliteratorOfObject<>(spliteratorSupplier.get()),
            () -> new GeneralSpliteratorOfPrimitive<>(spliteratorSupplier.get(), c -> c::accept)));
  }

  /** @since 28.1 */
  @SuppressWarnings("AndroidJdkLibsChecker") // see comment on GeneralSpliteratorOfPrimitive
  public static SpliteratorTester<Long> ofLong(Supplier<Spliterator.OfLong> spliteratorSupplier) {
    return new SpliteratorTester<>(
        ImmutableSet.of(
            () -> new GeneralSpliteratorOfObject<>(spliteratorSupplier.get()),
            () -> new GeneralSpliteratorOfPrimitive<>(spliteratorSupplier.get(), c -> c::accept)));
  }

  /** @since 28.1 */
  @SuppressWarnings("AndroidJdkLibsChecker") // see comment on GeneralSpliteratorOfPrimitive
  public static SpliteratorTester<Double> ofDouble(
      Supplier<Spliterator.OfDouble> spliteratorSupplier) {
    return new SpliteratorTester<>(
        ImmutableSet.of(
            () -> new GeneralSpliteratorOfObject<>(spliteratorSupplier.get()),
            () -> new GeneralSpliteratorOfPrimitive<>(spliteratorSupplier.get(), c -> c::accept)));
  }

  private final ImmutableSet<Supplier<GeneralSpliterator<E>>> spliteratorSuppliers;

  private SpliteratorTester(ImmutableSet<Supplier<GeneralSpliterator<E>>> spliteratorSuppliers) {
    this.spliteratorSuppliers = checkNotNull(spliteratorSuppliers);
  }

  @SafeVarargs
  public final Ordered expect(Object... elements) {
    return expect(Arrays.asList(elements));
  }

  public final Ordered expect(Iterable<?> elements) {
    List<List<E>> resultsForAllStrategies = new ArrayList<>();
    for (Supplier<GeneralSpliterator<E>> spliteratorSupplier : spliteratorSuppliers) {
      GeneralSpliterator<E> spliterator = spliteratorSupplier.get();
      int characteristics = spliterator.characteristics();
      long estimatedSize = spliterator.estimateSize();
      for (SpliteratorDecompositionStrategy strategy :
          EnumSet.allOf(SpliteratorDecompositionStrategy.class)) {
        List<E> resultsForStrategy = new ArrayList<>();
        strategy.forEach(spliteratorSupplier.get(), resultsForStrategy::add);

        // TODO(cpovirk): better failure messages
        if ((characteristics & Spliterator.NONNULL) != 0) {
          assertFalse(resultsForStrategy.contains(null));
        }
        if ((characteristics & Spliterator.SORTED) != 0) {
          Comparator<? super E> comparator = spliterator.getComparator();
          if (comparator == null) {
            comparator = (Comparator) naturalOrder();
          }
          assertTrue(Ordering.from(comparator).isOrdered(resultsForStrategy));
        }
        if ((characteristics & Spliterator.SIZED) != 0) {
          assertEquals(Ints.checkedCast(estimatedSize), resultsForStrategy.size());
        }

        assertEqualIgnoringOrder(elements, resultsForStrategy);
        resultsForAllStrategies.add(resultsForStrategy);
      }
    }
    return new Ordered() {
      @Override
      public void inOrder() {
        for (List<E> resultsForStrategy : resultsForAllStrategies) {
          assertEqualInOrder(elements, resultsForStrategy);
        }
      }
    };
  }
}

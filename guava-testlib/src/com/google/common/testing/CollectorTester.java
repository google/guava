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

package com.google.common.testing;

import static com.google.common.base.Preconditions.checkNotNull;
import static junit.framework.Assert.assertTrue;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tester for {@code Collector} implementations.
 *
 * <p>Example usage:
 *
 * <pre>
 * CollectorTester.of(Collectors.summingInt(Integer::parseInt))
 *     .expectCollects(3, "1", "2")
 *     .expectCollects(10, "1", "4", "3", "2")
 *     .expectCollects(5, "-3", "0", "8");
 * </pre>
 *
 * @author Louis Wasserman
 * @since 21.0
 */
@Beta
@GwtCompatible
public final class CollectorTester<T, A, R> {
  /**
   * Creates a {@code CollectorTester} for the specified {@code Collector}. The result of the {@code
   * Collector} will be compared to the expected value using {@link Object.equals}.
   */
  public static <T, A, R> CollectorTester<T, A, R> of(Collector<T, A, R> collector) {
    return of(collector, Objects::equals);
  }

  /**
   * Creates a {@code CollectorTester} for the specified {@code Collector}. The result of the {@code
   * Collector} will be compared to the expected value using the specified {@code equivalence}.
   */
  public static <T, A, R> CollectorTester<T, A, R> of(
      Collector<T, A, R> collector, BiPredicate<? super R, ? super R> equivalence) {
    return new CollectorTester<>(collector, equivalence);
  }

  private final Collector<T, A, R> collector;
  private final BiPredicate<? super R, ? super R> equivalence;

  private CollectorTester(
      Collector<T, A, R> collector, BiPredicate<? super R, ? super R> equivalence) {
    this.collector = checkNotNull(collector);
    this.equivalence = checkNotNull(equivalence);
  }

  /**
   * Different orderings for combining the elements of an input array, which must all produce the
   * same result.
   */
  enum CollectStrategy {
    /** Get one accumulator and accumulate the elements into it sequentially. */
    SEQUENTIAL {
      @Override
      final <T, A, R> A result(Collector<T, A, R> collector, Iterable<T> inputs) {
        A accum = collector.supplier().get();
        for (T input : inputs) {
          collector.accumulator().accept(accum, input);
        }
        return accum;
      }
    },
    /** Get one accumulator for each element and merge the accumulators left-to-right. */
    MERGE_LEFT_ASSOCIATIVE {
      @Override
      final <T, A, R> A result(Collector<T, A, R> collector, Iterable<T> inputs) {
        A accum = collector.supplier().get();
        for (T input : inputs) {
          A newAccum = collector.supplier().get();
          collector.accumulator().accept(newAccum, input);
          accum = collector.combiner().apply(accum, newAccum);
        }
        return accum;
      }
    },
    /** Get one accumulator for each element and merge the accumulators right-to-left. */
    MERGE_RIGHT_ASSOCIATIVE {
      @Override
      final <T, A, R> A result(Collector<T, A, R> collector, Iterable<T> inputs) {
        List<A> stack = new ArrayList<>();
        for (T input : inputs) {
          A newAccum = collector.supplier().get();
          collector.accumulator().accept(newAccum, input);
          push(stack, newAccum);
        }
        push(stack, collector.supplier().get());
        while (stack.size() > 1) {
          A right = pop(stack);
          A left = pop(stack);
          push(stack, collector.combiner().apply(left, right));
        }
        return pop(stack);
      }

      <E> void push(List<E> stack, E value) {
        stack.add(value);
      }

      <E> E pop(List<E> stack) {
        return stack.remove(stack.size() - 1);
      }
    };

    abstract <T, A, R> A result(Collector<T, A, R> collector, Iterable<T> inputs);
  }

  /**
   * Verifies that the specified expected result is always produced by collecting the specified
   * inputs, regardless of how the elements are divided.
   */
  @SafeVarargs
  public final CollectorTester<T, A, R> expectCollects(@Nullable R expectedResult, T... inputs) {
    List<T> list = Arrays.asList(inputs);
    doExpectCollects(expectedResult, list);
    if (collector.characteristics().contains(Collector.Characteristics.UNORDERED)) {
      Collections.reverse(list);
      doExpectCollects(expectedResult, list);
    }
    return this;
  }

  private void doExpectCollects(@Nullable R expectedResult, List<T> inputs) {
    for (CollectStrategy scheme : EnumSet.allOf(CollectStrategy.class)) {
      A finalAccum = scheme.result(collector, inputs);
      if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
        assertEquivalent(expectedResult, (R) finalAccum);
      }
      assertEquivalent(expectedResult, collector.finisher().apply(finalAccum));
    }
  }

  private void assertEquivalent(@Nullable R expected, @Nullable R actual) {
    assertTrue(
        "Expected " + expected + " got " + actual + " modulo equivalence " + equivalence,
        equivalence.test(expected, actual));
  }
}

/*
 * Copyright (C) 2019 The Guava Authors
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

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntToDoubleFunction;
import java.util.function.Supplier;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract superclass for tests that hash flooding a collection has controlled worst-case
 * performance.
 */
@GwtCompatible
public abstract class AbstractHashFloodingTest<T> extends TestCase {
  private final List<Construction<T>> constructions;
  private final IntToDoubleFunction constructionAsymptotics;
  private final List<QueryOp<T>> queries;

  AbstractHashFloodingTest(
      List<Construction<T>> constructions,
      IntToDoubleFunction constructionAsymptotics,
      List<QueryOp<T>> queries) {
    this.constructions = constructions;
    this.constructionAsymptotics = constructionAsymptotics;
    this.queries = queries;
  }

  /**
   * A Comparable wrapper around a String which executes callbacks on calls to hashCode, equals, and
   * compareTo.
   */
  private static class CountsHashCodeAndEquals implements Comparable<CountsHashCodeAndEquals> {
    private final String delegateString;
    private final Runnable onHashCode;
    private final Runnable onEquals;
    private final Runnable onCompareTo;

    CountsHashCodeAndEquals(
        String delegateString, Runnable onHashCode, Runnable onEquals, Runnable onCompareTo) {
      this.delegateString = delegateString;
      this.onHashCode = onHashCode;
      this.onEquals = onEquals;
      this.onCompareTo = onCompareTo;
    }

    @Override
    public int hashCode() {
      onHashCode.run();
      return delegateString.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
      onEquals.run();
      return other instanceof CountsHashCodeAndEquals
          && delegateString.equals(((CountsHashCodeAndEquals) other).delegateString);
    }

    @Override
    public int compareTo(CountsHashCodeAndEquals o) {
      onCompareTo.run();
      return delegateString.compareTo(o.delegateString);
    }
  }

  /** A holder of counters for calls to hashCode, equals, and compareTo. */
  private static final class CallsCounter {
    long hashCode;
    long equals;
    long compareTo;

    long total() {
      return hashCode + equals + compareTo;
    }

    void zero() {
      hashCode = 0;
      equals = 0;
      compareTo = 0;
    }
  }

  @FunctionalInterface
  interface Construction<T> {
    @CanIgnoreReturnValue
    abstract T create(List<?> keys);

    static Construction<Map<Object, Object>> mapFromKeys(
        Supplier<Map<Object, Object>> mutableSupplier) {
      return keys -> {
        Map<Object, Object> map = mutableSupplier.get();
        for (Object key : keys) {
          map.put(key, new Object());
        }
        return map;
      };
    }

    static Construction<Set<Object>> setFromElements(Supplier<Set<Object>> mutableSupplier) {
      return elements -> {
        Set<Object> set = mutableSupplier.get();
        set.addAll(elements);
        return set;
      };
    }
  }

  abstract static class QueryOp<T> {
    static <T> QueryOp<T> create(
        String name, BiConsumer<T, Object> queryLambda, IntToDoubleFunction asymptotic) {
      return new QueryOp<T>() {
        @Override
        void apply(T collection, Object query) {
          queryLambda.accept(collection, query);
        }

        @Override
        double expectedAsymptotic(int n) {
          return asymptotic.applyAsDouble(n);
        }

        @Override
        public String toString() {
          return name;
        }
      };
    }

    static final QueryOp<Map<Object, Object>> MAP_GET =
        QueryOp.create("Map.get", Map::get, Math::log);

    @SuppressWarnings("ReturnValueIgnored")
    static final QueryOp<Set<Object>> SET_CONTAINS =
        QueryOp.create("Set.contains", Set::contains, Math::log);

    abstract void apply(T collection, Object query);

    abstract double expectedAsymptotic(int n);
  }

  /**
   * Returns a list of objects with the same hash code, of size 2^power, counting calls to equals,
   * hashCode, and compareTo in counter.
   */
  static List<CountsHashCodeAndEquals> createAdversarialInput(int power, CallsCounter counter) {
    String str1 = "Aa";
    String str2 = "BB";
    assertEquals(str1.hashCode(), str2.hashCode());
    List<String> haveSameHashes2 = Arrays.asList(str1, str2);
    List<CountsHashCodeAndEquals> result =
        Lists.newArrayList(
            Lists.transform(
                Lists.cartesianProduct(Collections.nCopies(power, haveSameHashes2)),
                strs ->
                    new CountsHashCodeAndEquals(
                        String.join("", strs),
                        () -> counter.hashCode++,
                        () -> counter.equals++,
                        () -> counter.compareTo++)));
    assertEquals(
        result.get(0).delegateString.hashCode(),
        result.get(result.size() - 1).delegateString.hashCode());
    return result;
  }

  @GwtIncompatible
  public void testResistsHashFloodingInConstruction() {
    CallsCounter smallCounter = new CallsCounter();
    List<CountsHashCodeAndEquals> haveSameHashesSmall = createAdversarialInput(10, smallCounter);
    int smallSize = haveSameHashesSmall.size();

    CallsCounter largeCounter = new CallsCounter();
    List<CountsHashCodeAndEquals> haveSameHashesLarge = createAdversarialInput(15, largeCounter);
    int largeSize = haveSameHashesLarge.size();

    for (Construction<T> pathway : constructions) {
      smallCounter.zero();
      pathway.create(haveSameHashesSmall);
      long smallOps = smallCounter.total();

      largeCounter.zero();
      pathway.create(haveSameHashesLarge);
      long largeOps = largeCounter.total();

      double ratio = (double) largeOps / smallOps;
      assertWithMessage(
              "ratio of equals/hashCode/compareTo operations to build with %s entries versus %s"
                  + " entries",
              largeSize, smallSize)
          .that(ratio)
          .isAtMost(
              2
                  * constructionAsymptotics.applyAsDouble(largeSize)
                  / constructionAsymptotics.applyAsDouble(smallSize));
      // allow up to 2x wobble in the constant factors
    }
  }

  @GwtIncompatible
  public void testResistsHashFloodingOnQuery() {
    CallsCounter smallCounter = new CallsCounter();
    List<CountsHashCodeAndEquals> haveSameHashesSmall = createAdversarialInput(10, smallCounter);
    int smallSize = haveSameHashesSmall.size();

    CallsCounter largeCounter = new CallsCounter();
    List<CountsHashCodeAndEquals> haveSameHashesLarge = createAdversarialInput(15, largeCounter);
    int largeSize = haveSameHashesLarge.size();

    for (QueryOp<T> query : queries) {
      for (Construction<T> pathway : constructions) {
        long worstSmallOps = getWorstCaseOps(smallCounter, haveSameHashesSmall, query, pathway);
        long worstLargeOps = getWorstCaseOps(largeCounter, haveSameHashesLarge, query, pathway);

        double ratio = (double) worstLargeOps / worstSmallOps;
        assertWithMessage(
                "ratio of equals/hashCode/compareTo operations to query %s with %s entries versus"
                    + " %s entries",
                query, largeSize, smallSize)
            .that(ratio)
            .isAtMost(
                2 * query.expectedAsymptotic(largeSize) / query.expectedAsymptotic(smallSize));
        // allow up to 2x wobble in the constant factors
      }
    }
  }

  private long getWorstCaseOps(
      CallsCounter counter,
      List<CountsHashCodeAndEquals> haveSameHashes,
      QueryOp<T> query,
      Construction<T> pathway) {
    T collection = pathway.create(haveSameHashes);
    long worstOps = 0;
    for (Object o : haveSameHashes) {
      counter.zero();
      query.apply(collection, o);
      worstOps = Math.max(worstOps, counter.total());
    }
    return worstOps;
  }
}

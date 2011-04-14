/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * Contains static factory methods for creating {@code Equivalence} instances.
 *
 * <p>All methods return serializable instances.
 *
 * @author Bob Lee
 * @author Kurt Alfred Kluever
 * @author Gregory Kick
 * @since Guava release 04
 */
@Beta
@GwtCompatible
public final class Equivalences {
  private Equivalences() {}

  /**
   * Returns an equivalence that delegates to {@link Object#equals} and {@link Object#hashCode}.
   * {@link Equivalence#equivalent} returns {@code true} if both values are null, or if neither
   * value is null and {@link Object#equals} returns {@code true}. {@link Equivalence#hash} returns
   * {@code 0} if passed a null value.
   *
   * @since Guava release 08 (present null-friendly behavior)
   * @since Guava release 04 (otherwise)
   */
  public static Equivalence<Object> equals() {
    return Impl.EQUALS;
  }

  /**
   * Returns an equivalence that uses {@code ==} to compare values and {@link
   * System#identityHashCode(Object)} to compute the hash code.  {@link Equivalence#equivalent}
   * returns {@code true} if {@code a == b}, including in the case that a and b are both null.
   */
  public static Equivalence<Object> identity() {
    return Impl.IDENTITY;
  }

  private enum Impl implements Equivalence<Object> {
    EQUALS {
      @Override
      public boolean equivalent(@Nullable Object a, @Nullable Object b) {
        // TODO(kevinb): use Objects.equal() after testing issue is worked out.
        return (a == null) ? (b == null) : a.equals(b);
      }

      @Override
      public int hash(@Nullable Object o) {
        return (o == null) ? 0 : o.hashCode();
      }
    },
    IDENTITY {
      @Override
      public boolean equivalent(@Nullable Object a, @Nullable Object b) {
        return a == b;
      }

      @Override
      public int hash(@Nullable Object o) {
        return System.identityHashCode(o);
      }
    },
  }

  /**
   * Returns an equivalence over iterables based on the equivalence of their elements.  More
   * specifically, two iterables are considered equivalent if they both contain the same number of
   * elements, and each pair of corresponding elements is equivalent according to
   * {@code elementEquivalence}.  Null iterables are equivalent to one another.
   *
   * @since Guava release 09
   */
  @GwtCompatible(serializable = true)
  public static <T> Equivalence<Iterable<T>> pairwise(Equivalence<? super T> elementEquivalence) {
    /*
     * Ideally, the returned equivalence would support {@code Iterable<? extends T>}.  However, the
     * need for this is so rare that it's not worth making callers deal with the ugly wildcard.
     */
    return new PairwiseEquivalence<T>(elementEquivalence);
  }
}

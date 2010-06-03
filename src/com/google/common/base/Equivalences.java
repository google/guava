/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.common.base;

import com.google.common.annotations.Beta;

/**
 * Contains static factory methods for creating {@code Equivalence} instances.
 *
 * <p>All methods returns serializable instances.
 *
 * @author Bob Lee
 * @since 4
 */
@Beta
public final class Equivalences {
  private Equivalences() {}

  /**
   * Returns an equivalence that delegates to {@link Object#equals} and {@link Object#hashCode}.
   * Does not support null values.
   */
  public static Equivalence<Object> equals() {
    return Impl.EQUALS;
  }

  /**
   * Returns an equivalence that delegates to {@link Object#equals} and {@link Object#hashCode}.
   * {@link Equivalence#equivalent} returns {@code true} if both values are null, or if neither
   * value is null and {@link Object#equals} returns {@code true}. {@link Equivalence#hash} throws a
   * {@link NullPointerException} if passed a null value.
   */
  public static Equivalence<Object> nullAwareEquals() {
    return Impl.NULL_AWARE_EQUALS;
  }

  /**
   * Returns an equivalence that uses {@code ==} to compare values and {@link
   * System#identityHashCode(Object)} to compute the hash code.  {@link Equivalence#equivalent}
   * returns {@code true} if both values are null, or if neither value is null and {@code ==}
   * returns {@code true}. {@link Equivalence#hash} throws a {@link NullPointerException} if passed
   * a null value.
   */
  public static Equivalence<Object> identity() {
    return Impl.IDENTITY;
  }

  private enum Impl implements Equivalence<Object> {
    EQUALS {
      public boolean equivalent(Object a, Object b) {
        return a.equals(b);
      }

      public int hash(Object o) {
        return o.hashCode();
      }
    },
    IDENTITY {
      public boolean equivalent(Object a, Object b) {
        return a == b;
      }

      public int hash(Object o) {
        return System.identityHashCode(o);
      }
    },
    NULL_AWARE_EQUALS {
      public boolean equivalent(Object a, Object b) {
        return Objects.equal(a, b);
      }

      public int hash(Object o) {
        return o.hashCode(); // TODO: why NPE? counter-intuitive.
      }
    },
  }
}

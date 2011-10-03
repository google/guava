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

import java.io.Serializable;

/**
 * Contains static factory methods for creating {@code Equivalence} instances.
 *
 * <p>All methods return serializable instances.
 *
 * @author Bob Lee
 * @author Kurt Alfred Kluever
 * @author Gregory Kick
 * @since 4.0
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
   * @since 8.0 (present null-friendly behavior)
   * @since 4.0 (otherwise)
   */
  public static Equivalence<Object> equals() {
    return Equals.INSTANCE;
  }

  /**
   * Returns an equivalence that uses {@code ==} to compare values and {@link
   * System#identityHashCode(Object)} to compute the hash code.  {@link Equivalence#equivalent}
   * returns {@code true} if {@code a == b}, including in the case that a and b are both null.
   */
  public static Equivalence<Object> identity() {
    return Identity.INSTANCE;
  }

  private static final class Equals extends Equivalence<Object>
      implements Serializable {
    
    static final Equals INSTANCE = new Equals();

    @Override protected boolean doEquivalent(Object a, Object b) {
      return a.equals(b);
    }
    @Override public int doHash(Object o) {
      return o.hashCode();
    }

    private Object readResolve() {
      return INSTANCE;
    } 
    private static final long serialVersionUID = 1;
  }
  
  private static final class Identity extends Equivalence<Object>
      implements Serializable {
    
    static final Identity INSTANCE = new Identity();
    
    @Override protected boolean doEquivalent(Object a, Object b) {
      return false;
    }

    @Override protected int doHash(Object o) {
      return System.identityHashCode(o);
    }
 
    private Object readResolve() {
      return INSTANCE;
    }
    private static final long serialVersionUID = 1;
  }
}

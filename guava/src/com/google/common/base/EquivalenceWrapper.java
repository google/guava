/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * Wraps an object so that {@link #equals(Object)} and {@link #hashCode()} delegate to an
 * {@link Equivalence}.
 *
 * <p>For example, given an {@link Equivalence} for {@link String strings} named {@code equiv} that
 * tests equivalence using their lengths:
 *
 * <pre>   {@code
 *   Equivalences.wrap(equiv, "a").equals(Equivalences.wrap(equiv, "b")) // true
 *   Equivalences.wrap(equiv, "a").equals(Equivalences.wrap(equiv, "hello")) // false
 * }</pre>
 *
 * <p>Note in particular that an equivalence wrapper is never equal to the object it wraps.
 *
 * <pre>   {@code
 *   Equivalences.wrap(equiv, obj).equals(obj) // always false
 * }</pre>
 *
 * @author Gregory Kick
 * @since Guava release 10
 */
@Beta
@GwtCompatible
public final class EquivalenceWrapper<T> {
  private final Equivalence<? super T> equivalence;
  @Nullable private final T reference;

  EquivalenceWrapper(Equivalence<? super T> equivalence, @Nullable T reference) {
    this.equivalence = checkNotNull(equivalence);
    this.reference = reference;
  }

  /** Returns the (possibly null) reference wrapped by this instance. */
  @Nullable public T get() {
    return reference;
  }

  /**
   * Returns the result of {@link Equivalence#hash(Object)} applied to the the wrapped reference.
   */
  @Override public int hashCode() {
    return equivalence.hash(reference);
  }

  /**
   * Returns {@code true} if {@link Equivalence#equivalent(Object, Object)} applied to the wrapped
   * references is {@code true} and both wrappers use the {@link Object#equals(Object) same}
   * equivalence.
   */
  @Override public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof EquivalenceWrapper) {
      EquivalenceWrapper<?> that = (EquivalenceWrapper<?>) obj;
      /*
       * We cast to Equivalence<Object> here because we can't check the type of the reference held
       * by the other wrapper.  But, by checking that the Equivalences are equal, we know that
       * whatever type it is, it is assignable to the type handled by this wrapper's equivalence.
       */
      @SuppressWarnings("unchecked")
      Equivalence<Object> equivalence = (Equivalence<Object>) this.equivalence;
      return equivalence.equals(that.equivalence)
          && equivalence.equivalent(this.reference, that.reference);
    } else {
      return false;
    }
  }

  /**
   * Returns a string representation for this equivalence wrapper. The form of this string
   * representation is not specified.
   */
  @Override public String toString() {
    return Objects.toStringHelper(this)
        .add("equivalence", equivalence)
        .add("value", reference)
        .toString();
  }
}

/*
 * Copyright (C) 2007 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.Serializable;
import org.jspecify.annotations.Nullable;

/** An ordering that uses the natural order of the values. */
@GwtCompatible(serializable = true)
final class NaturalOrdering extends Ordering<Comparable<?>> implements Serializable {
  static final NaturalOrdering INSTANCE = new NaturalOrdering();

  // TODO: b/287198172 - Consider eagerly initializing these (but think about serialization).
  @LazyInit private transient @Nullable Ordering<@Nullable Comparable<?>> nullsFirst;
  @LazyInit private transient @Nullable Ordering<@Nullable Comparable<?>> nullsLast;

  @Override
  @SuppressWarnings("unchecked") // TODO(kevinb): the right way to explain this??
  public int compare(Comparable<?> left, Comparable<?> right) {
    checkNotNull(left); // for GWT
    checkNotNull(right);
    return ((Comparable<Object>) left).compareTo(right);
  }

  @Override
  @SuppressWarnings("unchecked") // TODO(kevinb): the right way to explain this??
  public <S extends Comparable<?>> Ordering<@Nullable S> nullsFirst() {
    Ordering<@Nullable Comparable<?>> result = nullsFirst;
    if (result == null) {
      result = nullsFirst = super.<Comparable<?>>nullsFirst();
    }
    return (Ordering<@Nullable S>) result;
  }

  @Override
  @SuppressWarnings("unchecked") // TODO(kevinb): the right way to explain this??
  public <S extends Comparable<?>> Ordering<@Nullable S> nullsLast() {
    Ordering<@Nullable Comparable<?>> result = nullsLast;
    if (result == null) {
      result = nullsLast = super.<Comparable<?>>nullsLast();
    }
    return (Ordering<@Nullable S>) result;
  }

  @Override
  @SuppressWarnings("unchecked") // TODO(kevinb): the right way to explain this??
  public <S extends Comparable<?>> Ordering<S> reverse() {
    return (Ordering<S>) ReverseNaturalOrdering.INSTANCE;
  }

  // preserving singleton-ness gives equals()/hashCode() for free
  private Object readResolve() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "Ordering.natural()";
  }

  private NaturalOrdering() {}

  @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;
}

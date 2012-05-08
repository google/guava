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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.util.Iterator;

/**
 * An {@code ImmutableSet} whose elements are derived by transforming another collection's elements,
 * useful for {@code ImmutableMap.keySet()}.
 *
 * @author Jesse Wilson
 */
@GwtCompatible(emulated = true)
abstract class TransformedImmutableSet<D, E> extends ImmutableSet<E> {
  /*
   * TODO(cpovirk): using an abstract source() method instead of a field could simplify
   * ImmutableMapKeySet, which currently has to pass in entrySet() manually
   */
  final ImmutableCollection<D> source;
  final int hashCode;

  TransformedImmutableSet(ImmutableCollection<D> source) {
    this.source = source;
    this.hashCode = Sets.hashCodeImpl(this);
  }

  TransformedImmutableSet(ImmutableCollection<D> source, int hashCode) {
    this.source = source;
    this.hashCode = hashCode;
  }

  abstract E transform(D element);

  @Override
  public int size() {
    return source.size();
  }

  @Override public boolean isEmpty() {
    return false;
  }

  @Override public UnmodifiableIterator<E> iterator() {
    final Iterator<D> backingIterator = source.iterator();
    return new UnmodifiableIterator<E>() {
      @Override
      public boolean hasNext() {
        return backingIterator.hasNext();
      }

      @Override
      public E next() {
        return transform(backingIterator.next());
      }
    };
  }

  @Override public Object[] toArray() {
    return toArray(new Object[size()]);
  }

  @Override public <T> T[] toArray(T[] array) {
    return ObjectArrays.toArrayImpl(this, array);
  }

  @Override public final int hashCode() {
    return hashCode;
  }

  @GwtIncompatible("unused")
  @Override boolean isHashCodeFast() {
    return true;
  }
}

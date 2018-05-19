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

import static com.google.common.collect.Multisets.setCountImpl;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2objc.annotations.WeakOuter;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class provides a skeletal implementation of the {@link Multiset} interface. A new multiset
 * implementation can be created easily by extending this class and implementing the {@link
 * Multiset#entrySet()} method, plus optionally overriding {@link #add(Object, int)} and {@link
 * #remove(Object, int)} to enable modifications to the multiset.
 *
 * <p>The {@link #count} and {@link #size} implementations all iterate across the set returned by
 * {@link Multiset#entrySet()}, as do many methods acting on the set returned by {@link
 * #elementSet()}. Override those methods for better performance.
 *
 * @author Kevin Bourrillion
 * @author Louis Wasserman
 */
@GwtCompatible
abstract class AbstractMultiset<E> extends AbstractCollection<E> implements Multiset<E> {
  // Query Operations

  @Override
  public boolean isEmpty() {
    return entrySet().isEmpty();
  }

  @Override
  public boolean contains(@Nullable Object element) {
    return count(element) > 0;
  }

  // Modification Operations
  @CanIgnoreReturnValue
  @Override
  public final boolean add(@Nullable E element) {
    add(element, 1);
    return true;
  }

  @CanIgnoreReturnValue
  @Override
  public int add(@Nullable E element, int occurrences) {
    throw new UnsupportedOperationException();
  }

  @CanIgnoreReturnValue
  @Override
  public final boolean remove(@Nullable Object element) {
    return remove(element, 1) > 0;
  }

  @CanIgnoreReturnValue
  @Override
  public int remove(@Nullable Object element, int occurrences) {
    throw new UnsupportedOperationException();
  }

  @CanIgnoreReturnValue
  @Override
  public int setCount(@Nullable E element, int count) {
    return setCountImpl(this, element, count);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean setCount(@Nullable E element, int oldCount, int newCount) {
    return setCountImpl(this, element, oldCount, newCount);
  }

  // Bulk Operations

  /**
   * {@inheritDoc}
   *
   * <p>This implementation is highly efficient when {@code elementsToAdd} is itself a {@link
   * Multiset}.
   */
  @CanIgnoreReturnValue
  @Override
  public final boolean addAll(Collection<? extends E> elementsToAdd) {
    return Multisets.addAllImpl(this, elementsToAdd);
  }

  @CanIgnoreReturnValue
  @Override
  public final boolean removeAll(Collection<?> elementsToRemove) {
    return Multisets.removeAllImpl(this, elementsToRemove);
  }

  @CanIgnoreReturnValue
  @Override
  public final boolean retainAll(Collection<?> elementsToRetain) {
    return Multisets.retainAllImpl(this, elementsToRetain);
  }

  @Override
  public abstract void clear();

  // Views

  private transient @MonotonicNonNull Set<E> elementSet;

  @Override
  public Set<E> elementSet() {
    Set<E> result = elementSet;
    if (result == null) {
      elementSet = result = createElementSet();
    }
    return result;
  }

  /**
   * Creates a new instance of this multiset's element set, which will be returned by {@link
   * #elementSet()}.
   */
  Set<E> createElementSet() {
    return new ElementSet();
  }

  @WeakOuter
  class ElementSet extends Multisets.ElementSet<E> {
    @Override
    Multiset<E> multiset() {
      return AbstractMultiset.this;
    }

    @Override
    public Iterator<E> iterator() {
      return elementIterator();
    }
  }

  abstract Iterator<E> elementIterator();

  private transient @MonotonicNonNull Set<Entry<E>> entrySet;

  @Override
  public Set<Entry<E>> entrySet() {
    Set<Entry<E>> result = entrySet;
    if (result == null) {
      entrySet = result = createEntrySet();
    }
    return result;
  }

  @WeakOuter
  class EntrySet extends Multisets.EntrySet<E> {
    @Override
    Multiset<E> multiset() {
      return AbstractMultiset.this;
    }

    @Override
    public Iterator<Entry<E>> iterator() {
      return entryIterator();
    }

    @Override
    public int size() {
      return distinctElements();
    }
  }

  Set<Entry<E>> createEntrySet() {
    return new EntrySet();
  }

  abstract Iterator<Entry<E>> entryIterator();

  abstract int distinctElements();

  // Object methods

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns {@code true} if {@code object} is a multiset of the same size
   * and if, for each element, the two multisets have the same count.
   */
  @Override
  public final boolean equals(@Nullable Object object) {
    return Multisets.equalsImpl(this, object);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns the hash code of {@link Multiset#entrySet()}.
   */
  @Override
  public final int hashCode() {
    return entrySet().hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns the result of invoking {@code toString} on {@link
   * Multiset#entrySet()}.
   */
  @Override
  public final String toString() {
    return entrySet().toString();
  }
}

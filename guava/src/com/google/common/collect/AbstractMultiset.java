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

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.AnnotatedFor;

import static com.google.common.collect.Multisets.setCountImpl;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.j2objc.annotations.WeakOuter;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * This class provides a skeletal implementation of the {@link Multiset}
 * interface. A new multiset implementation can be created easily by extending
 * this class and implementing the {@link Multiset#entrySet()} method, plus
 * optionally overriding {@link #add(Object, int)} and
 * {@link #remove(Object, int)} to enable modifications to the multiset.
 *
 * <p>The {@link #count} and {@link #size} implementations all iterate across
 * the set returned by {@link Multiset#entrySet()}, as do many methods acting on
 * the set returned by {@link #elementSet()}. Override those methods for better
 * performance.
 *
 * @author Kevin Bourrillion
 * @author Louis Wasserman
 */
@AnnotatedFor({"nullness"})
@GwtCompatible
abstract class AbstractMultiset<E extends /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object> extends AbstractCollection<E> implements Multiset<E> {
  // Query Operations

  @Pure
  @Override
  public int size() {
    return Multisets.sizeImpl(this);
  }

  @Pure
  @Override
  public boolean isEmpty() {
    return entrySet().isEmpty();
  }

  @Pure
  @Override
  public boolean contains(/*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object element) {
    return count(element) > 0;
  }

  @Override
  public Iterator<E> iterator() {
    return Multisets.iteratorImpl(this);
  }

  @Override
  public int count(/*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object element) {
    for (Entry<E> entry : entrySet()) {
      if (Objects.equal(entry.getElement(), element)) {
        return entry.getCount();
      }
    }
    return 0;
  }

  // Modification Operations

  @Override
  public boolean add(/*@Nullable*/ E element) {
    add(element, 1);
    return true;
  }

  @Override
  public int add(/*@Nullable*/ E element, int occurrences) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(/*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object element) {
    return remove(element, 1) > 0;
  }

  @Override
  public int remove(/*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object element, int occurrences) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int setCount(/*@Nullable*/ E element, int count) {
    return setCountImpl(this, element, count);
  }

  @Override
  public boolean setCount(/*@Nullable*/ E element, int oldCount, int newCount) {
    return setCountImpl(this, element, oldCount, newCount);
  }

  // Bulk Operations

  /**
   * {@inheritDoc}
   *
   * <p>This implementation is highly efficient when {@code elementsToAdd}
   * is itself a {@link Multiset}.
   */
  @Override
  public boolean addAll(Collection<? extends E> elementsToAdd) {
    return Multisets.addAllImpl(this, elementsToAdd);
  }

  @Override
  public boolean removeAll(Collection<? extends /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object> elementsToRemove) {
    return Multisets.removeAllImpl(this, elementsToRemove);
  }

  @Override
  public boolean retainAll(Collection<? extends /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object> elementsToRetain) {
    return Multisets.retainAllImpl(this, elementsToRetain);
  }

  @Override
  public void clear() {
    Iterators.clear(entryIterator());
  }

  // Views

  private transient Set<E> elementSet;

  @SideEffectFree
  @Override
  public Set<E> elementSet() {
    Set<E> result = elementSet;
    if (result == null) {
      elementSet = result = createElementSet();
    }
    return result;
  }

  /**
   * Creates a new instance of this multiset's element set, which will be
   * returned by {@link #elementSet()}.
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
  }

  abstract Iterator<Entry<E>> entryIterator();

  abstract int distinctElements();

  private transient Set<Entry<E>> entrySet;

  @SideEffectFree
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

  // Object methods

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns {@code true} if {@code object} is a multiset
   * of the same size and if, for each element, the two multisets have the same
   * count.
   */
  @Pure
  @Override
  public boolean equals(/*@Nullable*/ /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object object) {
    return Multisets.equalsImpl(this, object);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns the hash code of {@link
   * Multiset#entrySet()}.
   */
  @Pure
  @Override
  public int hashCode() {
    return entrySet().hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns the result of invoking {@code toString} on
   * {@link Multiset#entrySet()}.
   */
  @Pure
  @Override
  public String toString() {
    return entrySet().toString();
  }

  @Override
  public boolean containsAll(Collection<? extends /*@org.checkerframework.checker.nullness.qual.Nullable*/ Object> arg0) { return super.containsAll(arg0); }
}

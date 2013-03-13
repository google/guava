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

package com.google.common.collect.testing;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A wrapper around {@code TreeSet} that aggressively checks to see if elements
 * are mutually comparable. This implementation passes the navigable set test
 * suites.
 *
 * @author Louis Wasserman
 */
public final class SafeTreeSet<E> implements Serializable, SortedSet<E> {
  @SuppressWarnings("unchecked")
  private static final Comparator<Object> NATURAL_ORDER = new Comparator<Object>() {
    @Override public int compare(Object o1, Object o2) {
      return ((Comparable<Object>) o1).compareTo(o2);
    }
  };
  private final SortedSet<E> delegate;

  public SafeTreeSet() {
    this(new TreeSet<E>());
  }

  public SafeTreeSet(Collection<? extends E> collection) {
    this(new TreeSet<E>(collection));
  }

  public SafeTreeSet(Comparator<? super E> comparator) {
    this(new TreeSet<E>(comparator));
  }

  private SafeTreeSet(SortedSet<E> delegate) {
    this.delegate = delegate;
    for (E e : this) {
      checkValid(e);
    }
  }

  @Override public boolean add(E element) {
    return delegate.add(checkValid(element));
  }

  @Override public boolean addAll(Collection<? extends E> collection) {
    for (E e : collection) {
      checkValid(e);
    }
    return delegate.addAll(collection);
  }

  @Override public void clear() {
    delegate.clear();
  }

  @SuppressWarnings("unchecked")
  @Override public Comparator<? super E> comparator() {
    Comparator<? super E> comparator = delegate.comparator();
    if (comparator == null) {
      comparator = (Comparator<? super E>) NATURAL_ORDER;
    }
    return comparator;
  }

  @Override public boolean contains(Object object) {
    return delegate.contains(checkValid(object));
  }

  @Override public boolean containsAll(Collection<?> c) {
    return delegate.containsAll(c);
  }

  @Override public E first() {
    return delegate.first();
  }

  @Override public SortedSet<E> headSet(E toElement) {
    return new SafeTreeSet<E>(delegate.headSet(checkValid(toElement)));
  }

  @Override public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override public Iterator<E> iterator() {
    return delegate.iterator();
  }

  @Override public E last() {
    return delegate.last();
  }

  @Override public boolean remove(Object object) {
    return delegate.remove(checkValid(object));
  }

  @Override public boolean removeAll(Collection<?> c) {
    return delegate.removeAll(c);
  }

  @Override public boolean retainAll(Collection<?> c) {
    return delegate.retainAll(c);
  }

  @Override public int size() {
    return delegate.size();
  }


  @Override public SortedSet<E> subSet(E fromElement, E toElement) {
    return new SafeTreeSet<E>(delegate.subSet(checkValid(fromElement), checkValid(toElement)));
  }

  @Override public SortedSet<E> tailSet(E fromElement) {
    return new SafeTreeSet<E>(delegate.tailSet(checkValid(fromElement)));
  }

  @Override public Object[] toArray() {
    return delegate.toArray();
  }

  @Override public <T> T[] toArray(T[] a) {
    return delegate.toArray(a);
  }

  private <T> T checkValid(T t) {
    // a ClassCastException is what's supposed to happen!
    @SuppressWarnings("unchecked")
    E e = (E) t;
    comparator().compare(e, e);
    return t;
  }

  @Override public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  @Override public int hashCode() {
    return delegate.hashCode();
  }

  @Override public String toString() {
    return delegate.toString();
  }

  private static final long serialVersionUID = 0L;
}

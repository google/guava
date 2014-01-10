/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.collect.BoundType.CLOSED;
import static com.google.common.collect.BoundType.OPEN;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Multiset.Entry;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

import javax.annotation.Nullable;

/**
 * Provides static utility methods for creating and working with
 * {@link SortedMultiset} instances.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
final class SortedMultisets {
  private SortedMultisets() {
  }

  /**
   * A skeleton implementation for {@link SortedMultiset#elementSet}.
   */
  static class ElementSet<E> extends Multisets.ElementSet<E> implements
      SortedSet<E> {
    private final SortedMultiset<E> multiset;

    ElementSet(SortedMultiset<E> multiset) {
      this.multiset = multiset;
    }

    @Override final SortedMultiset<E> multiset() {
      return multiset;
    }

    @Override public Comparator<? super E> comparator() {
      return multiset().comparator();
    }

    @Override public SortedSet<E> subSet(E fromElement, E toElement) {
      return multiset().subMultiset(fromElement, CLOSED, toElement, OPEN).elementSet();
    }

    @Override public SortedSet<E> headSet(E toElement) {
      return multiset().headMultiset(toElement, OPEN).elementSet();
    }

    @Override public SortedSet<E> tailSet(E fromElement) {
      return multiset().tailMultiset(fromElement, CLOSED).elementSet();
    }

    @Override public E first() {
      return getElementOrThrow(multiset().firstEntry());
    }

    @Override public E last() {
      return getElementOrThrow(multiset().lastEntry());
    }
  }

  private static <E> E getElementOrThrow(Entry<E> entry) {
    if (entry == null) {
      throw new NoSuchElementException();
    }
    return entry.getElement();
  }

  private static <E> E getElementOrNull(@Nullable Entry<E> entry) {
    return (entry == null) ? null : entry.getElement();
  }
}

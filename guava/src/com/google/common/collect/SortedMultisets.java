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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multiset.Entry;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

/**
 * Provides static utility methods for creating and working with
 * {@link SortedMultiset} instances.
 * 
 * @author Louis Wasserman
 */
@GwtCompatible
final class SortedMultisets {
  private SortedMultisets() {
  }

  /**
   * A skeleton implementation for {@link SortedMultiset#elementSet}.
   */
  static abstract class ElementSet<E> extends Multisets.ElementSet<E> implements
      SortedSet<E> {
    @Override abstract SortedMultiset<E> multiset();

    @Override public Comparator<? super E> comparator() {
      return multiset().comparator();
    }

    @Override public SortedSet<E> subSet(E fromElement, E toElement) {
      return multiset().subMultiset(fromElement, BoundType.CLOSED, toElement,
          BoundType.OPEN).elementSet();
    }

    @Override public SortedSet<E> headSet(E toElement) {
      return multiset().headMultiset(toElement, BoundType.OPEN).elementSet();
    }

    @Override public SortedSet<E> tailSet(E fromElement) {
      return multiset().tailMultiset(fromElement, BoundType.CLOSED)
          .elementSet();
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
  
  /**
   * A skeleton implementation of a descending multiset.  Only needs
   * {@code forwardMultiset()} and {@code entryIterator()}.
   */
  static abstract class DescendingMultiset<E> extends ForwardingMultiset<E>
      implements SortedMultiset<E> {
    abstract SortedMultiset<E> forwardMultiset();

    private transient Comparator<? super E> comparator;

    @Override public Comparator<? super E> comparator() {
      Comparator<? super E> result = comparator;
      if (result == null) {
        return comparator =
            Ordering.from(forwardMultiset().comparator()).<E>reverse();
      }
      return result;
    }

    private transient SortedSet<E> elementSet;

    @Override public SortedSet<E> elementSet() {
      SortedSet<E> result = elementSet;
      if (result == null) {
        return elementSet = new SortedMultisets.ElementSet<E>() {
          @Override SortedMultiset<E> multiset() {
            return DescendingMultiset.this;
          }
        };
      }
      return result;
    }

    @Override public Entry<E> pollFirstEntry() {
      return forwardMultiset().pollLastEntry();
    }

    @Override public Entry<E> pollLastEntry() {
      return forwardMultiset().pollFirstEntry();
    }

    @Override public SortedMultiset<E> headMultiset(E toElement,
        BoundType boundType) {
      return forwardMultiset().tailMultiset(toElement, boundType)
          .descendingMultiset();
    }

    @Override public SortedMultiset<E> subMultiset(E fromElement,
        BoundType fromBoundType, E toElement, BoundType toBoundType) {
      return forwardMultiset().subMultiset(toElement, toBoundType, fromElement,
          fromBoundType).descendingMultiset();
    }

    @Override public SortedMultiset<E> tailMultiset(E fromElement,
        BoundType boundType) {
      return forwardMultiset().headMultiset(fromElement, boundType)
          .descendingMultiset();
    }

    @Override protected Multiset<E> delegate() {
      return forwardMultiset();
    }

    @Override public SortedMultiset<E> descendingMultiset() {
      return forwardMultiset();
    }

    @Override public Entry<E> firstEntry() {
      return forwardMultiset().lastEntry();
    }

    @Override public Entry<E> lastEntry() {
      return forwardMultiset().firstEntry();
    }

    abstract Iterator<Entry<E>> entryIterator();

    private transient Set<Entry<E>> entrySet;

    @Override public Set<Entry<E>> entrySet() {
      Set<Entry<E>> result = entrySet;
      return (result == null) ? entrySet = createEntrySet() : result;
    }

    Set<Entry<E>> createEntrySet() {
      return new Multisets.EntrySet<E>() {
        @Override Multiset<E> multiset() {
          return DescendingMultiset.this;
        }

        @Override public Iterator<Entry<E>> iterator() {
          return entryIterator();
        }

        @Override public int size() {
          return forwardMultiset().entrySet().size();
        }
      };
    }

    @Override public Iterator<E> iterator() {
      return Multisets.iteratorImpl(this);
    }

    @Override public Object[] toArray() {
      return standardToArray();
    }

    @Override public <T> T[] toArray(T[] array) {
      return standardToArray(array);
    }

    @Override public String toString() {
      return entrySet().toString();
    }
  }
}

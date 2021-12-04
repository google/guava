/*
 * Copyright (C) 2012 The Guava Authors
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
import com.google.j2objc.annotations.WeakOuter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A skeleton implementation of a descending multiset. Only needs {@code forwardMultiset()} and
 * {@code entryIterator()}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
abstract class DescendingMultiset<E extends @Nullable Object> extends ForwardingMultiset<E>
    implements SortedMultiset<E> {
  abstract SortedMultiset<E> forwardMultiset();

  @CheckForNull private transient Comparator<? super E> comparator;

  @Override
  public Comparator<? super E> comparator() {
    Comparator<? super E> result = comparator;
    if (result == null) {
      return comparator = Ordering.from(forwardMultiset().comparator()).<E>reverse();
    }
    return result;
  }

  @CheckForNull private transient NavigableSet<E> elementSet;

  @Override
  public NavigableSet<E> elementSet() {
    NavigableSet<E> result = elementSet;
    if (result == null) {
      return elementSet = new SortedMultisets.NavigableElementSet<>(this);
    }
    return result;
  }

  @Override
  @CheckForNull
  public Entry<E> pollFirstEntry() {
    return forwardMultiset().pollLastEntry();
  }

  @Override
  @CheckForNull
  public Entry<E> pollLastEntry() {
    return forwardMultiset().pollFirstEntry();
  }

  @Override
  public SortedMultiset<E> headMultiset(@ParametricNullness E toElement, BoundType boundType) {
    return forwardMultiset().tailMultiset(toElement, boundType).descendingMultiset();
  }

  @Override
  public SortedMultiset<E> subMultiset(
      @ParametricNullness E fromElement,
      BoundType fromBoundType,
      @ParametricNullness E toElement,
      BoundType toBoundType) {
    return forwardMultiset()
        .subMultiset(toElement, toBoundType, fromElement, fromBoundType)
        .descendingMultiset();
  }

  @Override
  public SortedMultiset<E> tailMultiset(@ParametricNullness E fromElement, BoundType boundType) {
    return forwardMultiset().headMultiset(fromElement, boundType).descendingMultiset();
  }

  @Override
  protected Multiset<E> delegate() {
    return forwardMultiset();
  }

  @Override
  public SortedMultiset<E> descendingMultiset() {
    return forwardMultiset();
  }

  @Override
  @CheckForNull
  public Entry<E> firstEntry() {
    return forwardMultiset().lastEntry();
  }

  @Override
  @CheckForNull
  public Entry<E> lastEntry() {
    return forwardMultiset().firstEntry();
  }

  abstract Iterator<Entry<E>> entryIterator();

  @CheckForNull private transient Set<Entry<E>> entrySet;

  @Override
  public Set<Entry<E>> entrySet() {
    Set<Entry<E>> result = entrySet;
    return (result == null) ? entrySet = createEntrySet() : result;
  }

  Set<Entry<E>> createEntrySet() {
    @WeakOuter
    class EntrySetImpl extends Multisets.EntrySet<E> {
      @Override
      Multiset<E> multiset() {
        return DescendingMultiset.this;
      }

      @Override
      public Iterator<Entry<E>> iterator() {
        return entryIterator();
      }

      @Override
      public int size() {
        return forwardMultiset().entrySet().size();
      }
    }
    return new EntrySetImpl();
  }

  @Override
  public Iterator<E> iterator() {
    return Multisets.iteratorImpl(this);
  }

  @Override
  public @Nullable Object[] toArray() {
    return standardToArray();
  }

  @Override
  @SuppressWarnings("nullness") // b/192354773 in our checker affects toArray declarations
  public <T extends @Nullable Object> T[] toArray(T[] array) {
    return standardToArray(array);
  }

  @Override
  public String toString() {
    return entrySet().toString();
  }
}

/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class provides a skeletal implementation of the {@link SortedMultiset} interface.
 *
 * <p>The {@link #count} and {@link #size} implementations all iterate across the set returned by
 * {@link Multiset#entrySet()}, as do many methods acting on the set returned by {@link
 * #elementSet()}. Override those methods for better performance.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
abstract class AbstractSortedMultiset<E extends @Nullable Object> extends AbstractMultiset<E>
    implements SortedMultiset<E> {
  @GwtTransient final Comparator<? super E> comparator;

  // needed for serialization
  @SuppressWarnings("unchecked")
  AbstractSortedMultiset() {
    this((Comparator) Ordering.natural());
  }

  AbstractSortedMultiset(Comparator<? super E> comparator) {
    this.comparator = checkNotNull(comparator);
  }

  @Override
  public SortedSet<E> elementSet() {
    return (SortedSet<E>) super.elementSet();
  }

  @Override
  SortedSet<E> createElementSet() {
    return new SortedMultisets.ElementSet<E>(this);
  }

  @Override
  public Comparator<? super E> comparator() {
    return comparator;
  }

  @Override
  @CheckForNull
  public Entry<E> firstEntry() {
    Iterator<Entry<E>> entryIterator = entryIterator();
    return entryIterator.hasNext() ? entryIterator.next() : null;
  }

  @Override
  @CheckForNull
  public Entry<E> lastEntry() {
    Iterator<Entry<E>> entryIterator = descendingEntryIterator();
    return entryIterator.hasNext() ? entryIterator.next() : null;
  }

  @Override
  @CheckForNull
  public Entry<E> pollFirstEntry() {
    Iterator<Entry<E>> entryIterator = entryIterator();
    if (entryIterator.hasNext()) {
      Entry<E> result = entryIterator.next();
      result = Multisets.immutableEntry(result.getElement(), result.getCount());
      entryIterator.remove();
      return result;
    }
    return null;
  }

  @Override
  @CheckForNull
  public Entry<E> pollLastEntry() {
    Iterator<Entry<E>> entryIterator = descendingEntryIterator();
    if (entryIterator.hasNext()) {
      Entry<E> result = entryIterator.next();
      result = Multisets.immutableEntry(result.getElement(), result.getCount());
      entryIterator.remove();
      return result;
    }
    return null;
  }

  @Override
  public SortedMultiset<E> subMultiset(
      E fromElement, BoundType fromBoundType, E toElement, BoundType toBoundType) {
    // These are checked elsewhere, but NullPointerTester wants them checked eagerly.
    checkNotNull(fromBoundType);
    checkNotNull(toBoundType);
    return tailMultiset(fromElement, fromBoundType).headMultiset(toElement, toBoundType);
  }

  abstract Iterator<Entry<E>> descendingEntryIterator();

  Iterator<E> descendingIterator() {
    return Multisets.iteratorImpl(descendingMultiset());
  }

  @Nullable private transient SortedMultiset<E> descendingMultiset;

  @Override
  public SortedMultiset<E> descendingMultiset() {
    SortedMultiset<E> result = descendingMultiset;
    return (result == null) ? descendingMultiset = createDescendingMultiset() : result;
  }

  SortedMultiset<E> createDescendingMultiset() {
    return new DescendingMultiset<E>() {
      @Override
      SortedMultiset<E> forwardMultiset() {
        return AbstractSortedMultiset.this;
      }

      @Override
      Iterator<Entry<E>> entryIterator() {
        return descendingEntryIterator();
      }

      @Override
      public Iterator<E> iterator() {
        return descendingIterator();
      }
    };
  }
}

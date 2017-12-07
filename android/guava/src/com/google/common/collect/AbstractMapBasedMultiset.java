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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Basic implementation of {@code Multiset<E>} backed by an instance of {@code
 * AbstractObjectCountMap<E>}.
 *
 * <p>For serialization to work, the subclass must specify explicit {@code readObject} and {@code
 * writeObject} methods.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
abstract class AbstractMapBasedMultiset<E> extends AbstractMultiset<E> implements Serializable {
  transient AbstractObjectCountMap<E> backingMap;

  /*
   * Cache the size for efficiency. Using a long lets us avoid the need for
   * overflow checking and ensures that size() will function correctly even if
   * the multiset had once been larger than Integer.MAX_VALUE.
   */
  private transient long size;

  /** Standard constructor. */
  protected AbstractMapBasedMultiset(AbstractObjectCountMap<E> backingMap) {
    this.backingMap = checkNotNull(backingMap);
    this.size = super.size();
  }

  /** Used during deserialization only. The backing map must be empty. */
  void setBackingMap(AbstractObjectCountMap<E> backingMap) {
    this.backingMap = backingMap;
  }

  // Required Implementations
  @Override
  Set<E> createElementSet() {
    return backingMap.keySet();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Invoking {@link Multiset.Entry#getCount} on an entry in the returned set always returns the
   * current count of that element in the multiset, as opposed to the count at the time the entry
   * was retrieved.
   */
  @Override
  public Set<Multiset.Entry<E>> createEntrySet() {
    return new EntrySet();
  }

  @Override
  Iterator<Entry<E>> entryIterator() {
    final Iterator<Entry<E>> backingEntries = backingMap.entrySet().iterator();
    return new Iterator<Multiset.Entry<E>>() {
      Entry<E> toRemove;
      boolean canRemove;

      @Override
      public boolean hasNext() {
        return backingEntries.hasNext();
      }

      @Override
      public Multiset.Entry<E> next() {
        final Entry<E> mapEntry = backingEntries.next();
        toRemove = mapEntry;
        canRemove = true;
        return mapEntry;
      }

      @Override
      public void remove() {
        checkRemove(canRemove);
        size -= toRemove.getCount();
        backingEntries.remove();
        canRemove = false;
        toRemove = null;
      }
    };
  }

  @Override
  public void clear() {
    backingMap.clear();
    size = 0L;
  }

  @Override
  int distinctElements() {
    return backingMap.size();
  }

  // Optimizations - Query Operations

  @Override
  public int size() {
    return Ints.saturatedCast(size);
  }

  @Override
  public Iterator<E> iterator() {
    return new MapBasedMultisetIterator();
  }

  /*
   * Not subclassing AbstractMultiset$MultisetIterator because next() needs to
   * retrieve the Map.Entry<E, Count> entry, which can then be used for
   * a more efficient remove() call.
   */
  private class MapBasedMultisetIterator implements Iterator<E> {
    final Iterator<Entry<E>> entryIterator;
    Entry<E> currentEntry;
    int occurrencesLeft = 0;
    boolean canRemove = false;

    MapBasedMultisetIterator() {
      this.entryIterator = backingMap.entrySet().iterator();
    }

    @Override
    public boolean hasNext() {
      return occurrencesLeft > 0 || entryIterator.hasNext();
    }

    @Override
    public E next() {
      if (occurrencesLeft == 0) {
        currentEntry = entryIterator.next();
        occurrencesLeft = currentEntry.getCount();
      }
      occurrencesLeft--;
      canRemove = true;
      return currentEntry.getElement();
    }

    @Override
    public void remove() {
      checkRemove(canRemove);
      int frequency = currentEntry.getCount();
      if (frequency <= 0) {
        throw new ConcurrentModificationException();
      }
      if (frequency == 1) {
        entryIterator.remove();
      } else {
        ((ObjectCountHashMap.MapEntry) currentEntry).setCount(frequency - 1);
      }
      size--;
      canRemove = false;
    }
  }

  @Override
  public int count(@NullableDecl Object element) {
    return backingMap.get(element);
  }

  // Optional Operations - Modification Operations

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the call would result in more than {@link
   *     Integer#MAX_VALUE} occurrences of {@code element} in this multiset.
   */
  @CanIgnoreReturnValue
  @Override
  public int add(@NullableDecl E element, int occurrences) {
    if (occurrences == 0) {
      return count(element);
    }
    checkArgument(occurrences > 0, "occurrences cannot be negative: %s", occurrences);
    int oldCount = backingMap.get(element);
    long newCount = (long) oldCount + (long) occurrences;
    checkArgument(newCount <= Integer.MAX_VALUE, "too many occurrences: %s", newCount);
    backingMap.put(element, (int) newCount);
    size += occurrences;
    return oldCount;
  }

  @CanIgnoreReturnValue
  @Override
  public int remove(@NullableDecl Object element, int occurrences) {
    if (occurrences == 0) {
      return count(element);
    }
    checkArgument(occurrences > 0, "occurrences cannot be negative: %s", occurrences);
    int oldCount = backingMap.get(element);
    int numberRemoved;
    if (oldCount > occurrences) {
      numberRemoved = occurrences;
      backingMap.put((E) element, oldCount - occurrences);
    } else {
      numberRemoved = oldCount;
      backingMap.remove(element);
    }
    size -= numberRemoved;
    return oldCount;
  }

  // Roughly a 33% performance improvement over AbstractMultiset.setCount().
  @CanIgnoreReturnValue
  @Override
  public int setCount(@NullableDecl E element, int count) {
    checkNonnegative(count, "count");
    int oldCount = (count == 0) ? backingMap.remove(element) : backingMap.put(element, count);
    size += (count - oldCount);
    return oldCount;
  }

  // Don't allow default serialization.
  @GwtIncompatible // java.io.ObjectStreamException
  private void readObjectNoData() throws ObjectStreamException {
    throw new InvalidObjectException("Stream data required");
  }

  @GwtIncompatible // not needed in emulated source.
  private static final long serialVersionUID = -2250766705698539974L;
}

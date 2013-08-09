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

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Basic implementation of {@code Multiset<E>} backed by an instance of {@code
 * Map<E, Count>}.
 *
 * <p>For serialization to work, the subclass must specify explicit {@code
 * readObject} and {@code writeObject} methods.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
abstract class AbstractMapBasedMultiset<E> extends AbstractMultiset<E>
    implements Serializable {

  private transient Map<E, Count> backingMap;

  /*
   * Cache the size for efficiency. Using a long lets us avoid the need for
   * overflow checking and ensures that size() will function correctly even if
   * the multiset had once been larger than Integer.MAX_VALUE.
   */
  private transient long size;

  /** Standard constructor. */
  protected AbstractMapBasedMultiset(Map<E, Count> backingMap) {
    this.backingMap = checkNotNull(backingMap);
    this.size = super.size();
  }

  /** Used during deserialization only. The backing map must be empty. */
  void setBackingMap(Map<E, Count> backingMap) {
    this.backingMap = backingMap;
  }

  // Required Implementations

  /**
   * {@inheritDoc}
   *
   * <p>Invoking {@link Multiset.Entry#getCount} on an entry in the returned
   * set always returns the current count of that element in the multiset, as
   * opposed to the count at the time the entry was retrieved.
   */
  @Override
  public Set<Multiset.Entry<E>> entrySet() {
    return super.entrySet();
  }

  @Override
  Iterator<Entry<E>> entryIterator() {
    final Iterator<Map.Entry<E, Count>> backingEntries =
        backingMap.entrySet().iterator();
    return new Iterator<Multiset.Entry<E>>() {
      Map.Entry<E, Count> toRemove;

      @Override
      public boolean hasNext() {
        return backingEntries.hasNext();
      }

      @Override
      public Multiset.Entry<E> next() {
        final Map.Entry<E, Count> mapEntry = backingEntries.next();
        toRemove = mapEntry;
        return new Multisets.AbstractEntry<E>() {
          @Override
          public E getElement() {
            return mapEntry.getKey();
          }
          @Override
          public int getCount() {
            Count count = mapEntry.getValue();
            if (count == null || count.get() == 0) {
              Count frequency = backingMap.get(getElement());
              if (frequency != null) {
                return frequency.get();
              }
            }
            return (count == null) ? 0 : count.get();
          }
        };
      }

      @Override
      public void remove() {
        checkRemove(toRemove != null);
        size -= toRemove.getValue().getAndSet(0);
        backingEntries.remove();
        toRemove = null;
      }
    };
  }

  @Override
  public void clear() {
    for (Count frequency : backingMap.values()) {
      frequency.set(0);
    }
    backingMap.clear();
    size = 0L;
  }

  @Override
  int distinctElements() {
    return backingMap.size();
  }

  // Optimizations - Query Operations

  @Override public int size() {
    return Ints.saturatedCast(size);
  }

  @Override public Iterator<E> iterator() {
    return new MapBasedMultisetIterator();
  }

  /*
   * Not subclassing AbstractMultiset$MultisetIterator because next() needs to
   * retrieve the Map.Entry<E, Count> entry, which can then be used for
   * a more efficient remove() call.
   */
  private class MapBasedMultisetIterator implements Iterator<E> {
    final Iterator<Map.Entry<E, Count>> entryIterator;
    Map.Entry<E, Count> currentEntry;
    int occurrencesLeft;
    boolean canRemove;

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
        occurrencesLeft = currentEntry.getValue().get();
      }
      occurrencesLeft--;
      canRemove = true;
      return currentEntry.getKey();
    }

    @Override
    public void remove() {
      checkRemove(canRemove);
      int frequency = currentEntry.getValue().get();
      if (frequency <= 0) {
        throw new ConcurrentModificationException();
      }
      if (currentEntry.getValue().addAndGet(-1) == 0) {
        entryIterator.remove();
      }
      size--;
      canRemove = false;
    }
  }

  @Override public int count(@Nullable Object element) {
    Count frequency = Maps.safeGet(backingMap, element);
    return (frequency == null) ? 0 : frequency.get();
  }

  // Optional Operations - Modification Operations

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if the call would result in more than
   *     {@link Integer#MAX_VALUE} occurrences of {@code element} in this
   *     multiset.
   */
  @Override public int add(@Nullable E element, int occurrences) {
    if (occurrences == 0) {
      return count(element);
    }
    checkArgument(
        occurrences > 0, "occurrences cannot be negative: %s", occurrences);
    Count frequency = backingMap.get(element);
    int oldCount;
    if (frequency == null) {
      oldCount = 0;
      backingMap.put(element, new Count(occurrences));
    } else {
      oldCount = frequency.get();
      long newCount = (long) oldCount + (long) occurrences;
      checkArgument(newCount <= Integer.MAX_VALUE,
          "too many occurrences: %s", newCount);
      frequency.getAndAdd(occurrences);
    }
    size += occurrences;
    return oldCount;
  }

  @Override public int remove(@Nullable Object element, int occurrences) {
    if (occurrences == 0) {
      return count(element);
    }
    checkArgument(
        occurrences > 0, "occurrences cannot be negative: %s", occurrences);
    Count frequency = backingMap.get(element);
    if (frequency == null) {
      return 0;
    }

    int oldCount = frequency.get();

    int numberRemoved;
    if (oldCount > occurrences) {
      numberRemoved = occurrences;
    } else {
      numberRemoved = oldCount;
      backingMap.remove(element);
    }

    frequency.addAndGet(-numberRemoved);
    size -= numberRemoved;
    return oldCount;
  }

  // Roughly a 33% performance improvement over AbstractMultiset.setCount().
  @Override public int setCount(@Nullable E element, int count) {
    checkNonnegative(count, "count");

    Count existingCounter;
    int oldCount;
    if (count == 0) {
      existingCounter = backingMap.remove(element);
      oldCount = getAndSet(existingCounter, count);
    } else {
      existingCounter = backingMap.get(element);
      oldCount = getAndSet(existingCounter, count);

      if (existingCounter == null) {
        backingMap.put(element, new Count(count));
      }
    }

    size += (count - oldCount);
    return oldCount;
  }

  private static int getAndSet(Count i, int count) {
    if (i == null) {
      return 0;
    }

    return i.getAndSet(count);
  }

  // Don't allow default serialization.
  @GwtIncompatible("java.io.ObjectStreamException")
  @SuppressWarnings("unused") // actually used during deserialization
  private void readObjectNoData() throws ObjectStreamException {
    throw new InvalidObjectException("Stream data required");
  }

  @GwtIncompatible("not needed in emulated source.")
  private static final long serialVersionUID = -2250766705698539974L;
}

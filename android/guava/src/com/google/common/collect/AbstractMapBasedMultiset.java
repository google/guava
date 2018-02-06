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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
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

  transient ObjectCountHashMap<E> backingMap;
  transient long size;

  AbstractMapBasedMultiset(int distinctElements) {
    init(distinctElements);
  }

  abstract void init(int distinctElements);

  @Override
  public final int count(@NullableDecl Object element) {
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
  public final int add(@NullableDecl E element, int occurrences) {
    if (occurrences == 0) {
      return count(element);
    }
    checkArgument(occurrences > 0, "occurrences cannot be negative: %s", occurrences);
    int entryIndex = backingMap.indexOf(element);
    if (entryIndex == -1) {
      backingMap.put(element, occurrences);
      size += occurrences;
      return 0;
    }
    int oldCount = backingMap.getValue(entryIndex);
    long newCount = (long) oldCount + (long) occurrences;
    checkArgument(newCount <= Integer.MAX_VALUE, "too many occurrences: %s", newCount);
    backingMap.setValue(entryIndex, (int) newCount);
    size += occurrences;
    return oldCount;
  }

  @CanIgnoreReturnValue
  @Override
  public final int remove(@NullableDecl Object element, int occurrences) {
    if (occurrences == 0) {
      return count(element);
    }
    checkArgument(occurrences > 0, "occurrences cannot be negative: %s", occurrences);
    int entryIndex = backingMap.indexOf(element);
    if (entryIndex == -1) {
      return 0;
    }
    int oldCount = backingMap.getValue(entryIndex);
    int numberRemoved;
    if (oldCount > occurrences) {
      numberRemoved = occurrences;
      backingMap.setValue(entryIndex, oldCount - occurrences);
    } else {
      numberRemoved = oldCount;
      backingMap.removeEntry(entryIndex);
    }
    size -= numberRemoved;
    return oldCount;
  }

  @CanIgnoreReturnValue
  @Override
  public final int setCount(@NullableDecl E element, int count) {
    checkNonnegative(count, "count");
    int oldCount = (count == 0) ? backingMap.remove(element) : backingMap.put(element, count);
    size += (count - oldCount);
    return oldCount;
  }

  @Override
  public final boolean setCount(@NullableDecl E element, int oldCount, int newCount) {
    checkNonnegative(oldCount, "oldCount");
    checkNonnegative(newCount, "newCount");
    int entryIndex = backingMap.indexOf(element);
    if (entryIndex == -1) {
      if (oldCount != 0) {
        return false;
      }
      if (newCount > 0) {
        backingMap.put(element, newCount);
        size += newCount;
      }
      return true;
    }
    int actualOldCount = backingMap.getValue(entryIndex);
    if (actualOldCount != oldCount) {
      return false;
    }
    if (newCount == 0) {
      backingMap.removeEntry(entryIndex);
      size -= oldCount;
    } else {
      backingMap.setValue(entryIndex, newCount);
      size += newCount - oldCount;
    }
    return true;
  }

  @Override
  public final void clear() {
    backingMap.clear();
    size = 0;
  }

  /**
   * Skeleton of per-entry iterators. We could push this down and win a few bytes, but it's complex
   * enough it's not especially worth it.
   */
  abstract class Itr<T> implements Iterator<T> {
    int entryIndex = backingMap.firstIndex();
    int toRemove = -1;
    int expectedModCount = backingMap.modCount;

    abstract T result(int entryIndex);

    private void checkForConcurrentModification() {
      if (backingMap.modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
    }

    @Override
    public boolean hasNext() {
      checkForConcurrentModification();
      return entryIndex >= 0;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      T result = result(entryIndex);
      toRemove = entryIndex;
      entryIndex = backingMap.nextIndex(entryIndex);
      return result;
    }

    @Override
    public void remove() {
      checkForConcurrentModification();
      CollectPreconditions.checkRemove(toRemove != -1);
      size -= backingMap.removeEntry(toRemove);
      entryIndex = backingMap.nextIndexAfterRemove(entryIndex, toRemove);
      toRemove = -1;
      expectedModCount = backingMap.modCount;
    }
  }

  @Override
  final Iterator<E> elementIterator() {
    return new Itr<E>() {
      @Override
      E result(int entryIndex) {
        return backingMap.getKey(entryIndex);
      }
    };
  }

  @Override
  final Iterator<Entry<E>> entryIterator() {
    return new Itr<Entry<E>>() {
      @Override
      Entry<E> result(int entryIndex) {
        return backingMap.getEntry(entryIndex);
      }
    };
  }

  /** Allocation-free implementation of {@code target.addAll(this)}. */
  void addTo(Multiset<? super E> target) {
    checkNotNull(target);
    for (int i = backingMap.firstIndex(); i >= 0; i = backingMap.nextIndex(i)) {
      target.add(backingMap.getKey(i), backingMap.getValue(i));
    }
  }

  @Override
  final int distinctElements() {
    return backingMap.size();
  }

  @Override
  public final Iterator<E> iterator() {
    return Multisets.iteratorImpl(this);
  }

  @Override
  public final int size() {
    return Ints.saturatedCast(size);
  }

  /**
   * @serialData the number of distinct elements, the first element, its count, the second element,
   *     its count, and so on
   */
  @GwtIncompatible // java.io.ObjectOutputStream
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    Serialization.writeMultiset(this, stream);
  }

  @GwtIncompatible // java.io.ObjectInputStream
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    int distinctElements = Serialization.readCount(stream);
    init(ObjectCountHashMap.DEFAULT_SIZE);
    Serialization.populateMultiset(this, stream, distinctElements);
  }

  @GwtIncompatible // Not needed in emulated source.
  private static final long serialVersionUID = 0;
}

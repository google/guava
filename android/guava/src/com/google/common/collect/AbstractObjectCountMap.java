/*
 * Copyright (C) 2017 The Guava Authors
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

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.collect.CollectPreconditions.checkRemove;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets.AbstractEntry;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.j2objc.annotations.WeakOuter;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Basic implementation of a primitive map of Object keys to counting number values, to be used for
 * backing store for {@link Multiset} implementations.
 */
@GwtCompatible(serializable = true, emulated = true)
abstract class AbstractObjectCountMap<K> {

  /** The keys of the entries in the map. */
  transient Object[] keys;

  /** The values of the entries in the map. */
  transient int[] values;

  /** Sentinel value that denotes an unset entry. */
  static final int UNSET = -1;

  /** The number of elements contained in the set. */
  transient int size;

  transient int modCount;

  private transient Set<K> keySetView;

  /**
   * Associates the specified value with the specified key in this map. If the map previously
   * contained a mapping for the key, the old value is replaced by the specified value. (A map m is
   * said to contain a mapping for a key k if and only if m.containsKey(k) would return true.)
   *
   * @param key key with which the specified value is to be associated
   * @param value a positive int value to be associated with the specified key
   * @return the previous value associated with key, or 0 if there was no mapping for key.
   */
  @CanIgnoreReturnValue
  abstract int put(@NullableDecl K key, int value);

  /**
   * Returns the value to which the specified key is mapped, or 0 if this map contains no mapping
   * for the key.
   *
   * @param key the key whose associated value is to be returned
   * @return the int value to which the specified key is mapped, or 0 if this map contains no
   *     mapping for the key
   */
  abstract int get(@NullableDecl Object key);

  /**
   * Removes the mapping for a key from this map if it is present. More formally, if this map
   * contains a mapping from key k to value v such that (key==null ? k==null : key.equals(k)), that
   * mapping is removed. (The map can contain at most one such mapping.)
   *
   * <p>Returns the value to which this map previously associated the key, or 0 if the map contained
   * no mapping for the key.
   *
   * @param key key whose mapping is to be removed from the map
   * @return the previous int value associated with key, or 0 if there was no mapping for key.
   */
  @CanIgnoreReturnValue
  abstract int remove(@NullableDecl Object key);

  /** Removes all of the mappings from this map. The map will be empty after this call returns. */
  abstract void clear();

  /**
   * Returns true if this map contains a mapping for the specified key. More formally, returns true
   * if and only if this map contains a mapping for a key k such that (key==null ? k==null :
   * key.equals(k)). (There can be at most one such mapping.)
   *
   * @param key key whose presence in this map is to be tested
   * @return true if this map contains a mapping for the specified key
   */
  abstract boolean containsKey(@NullableDecl Object key);

  /**
   * Returns a Set view of the keys contained in this map. The set is backed by the map, so changes
   * to the map are reflected in the set, and vice-versa.
   *
   * @return a set view of the keys contained in this map
   */
  Set<K> keySet() {
    return (keySetView == null) ? keySetView = createKeySet() : keySetView;
  }

  /** Returns the number of key-value mappings in this map. */
  int size() {
    return size;
  }

  /** Returns true if this map contains no key-value mappings. */
  boolean isEmpty() {
    return size == 0;
  }

  /**
   * Returns index of the specified key in the underlying key array. Implementation specific and
   * does not denote ordering of the keys.
   */
  abstract int indexOf(@NullableDecl Object key);

  /** Given the index of a key, remove the corresponding entry in the map. */
  @CanIgnoreReturnValue
  abstract int removeEntry(int entryIndex);

  Set<K> createKeySet() {
    return new KeySetView();
  }

  K getKey(int index) {
    checkElementIndex(index, size);
    return (K) keys[index];
  }

  int getValue(int index) {
    checkElementIndex(index, size);
    return values[index];
  }

  Entry<K> getEntry(int index) {
    checkElementIndex(index, size);
    return new MapEntry(index);
  }

  @WeakOuter
  class KeySetView extends Sets.ImprovedAbstractSet<K> {
    @Override
    public Object[] toArray() {
      return ObjectArrays.copyAsObjectArray(keys, 0, size);
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return ObjectArrays.toArrayImpl(keys, 0, size, a);
    }

    @Override
    public Iterator<K> iterator() {
      return new Itr<K>() {
        @SuppressWarnings("unchecked") // keys only contains Ks
        @Override
        K getOutput(int entry) {
          return (K) keys[entry];
        }
      };
    }

    @Override
    public int size() {
      return size;
    }
  }

  int firstIndex() {
    return 0;
  }

  int nextIndex(int index) {
    return (index + 1 < size) ? index + 1 : -1;
  }

  abstract class Itr<T> implements Iterator<T> {
    int expectedModCount = modCount;
    boolean nextCalled = false;
    int index = 0;

    @Override
    public boolean hasNext() {
      return index < size;
    }

    abstract T getOutput(int entry);

    @Override
    public T next() {
      checkForConcurrentModification();
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      nextCalled = true;
      return getOutput(index++);
    }

    @Override
    public void remove() {
      checkForConcurrentModification();
      checkRemove(nextCalled);
      expectedModCount++;
      index--;
      removeEntry(index);
      nextCalled = false;
    }

    void checkForConcurrentModification() {
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
    }
  }

  private transient Set<Entry<K>> entrySetView;

  /**
   * Returns a Set view of the entries contained in this map. The set is backed by the map, so
   * changes to the map are reflected in the set, and vice-versa.
   *
   * @return a set view of the entries contained in this map
   */
  Set<Entry<K>> entrySet() {
    return (entrySetView == null) ? entrySetView = createEntrySet() : entrySetView;
  }

  abstract Set<Entry<K>> createEntrySet();

  @WeakOuter
  abstract class EntrySetView extends Sets.ImprovedAbstractSet<Entry<K>> {

    @Override
    public boolean contains(@NullableDecl Object o) {
      if (o instanceof Entry) {
        Entry<?> entry = (Entry<?>) o;
        int index = indexOf(entry.getElement());
        return index != -1 && values[index] == entry.getCount();
      }
      return false;
    }

    @Override
    public boolean remove(@NullableDecl Object o) {
      if (o instanceof Entry) {
        Entry<?> entry = (Entry<?>) o;
        int index = indexOf(entry.getElement());
        if (index != -1 && values[index] == entry.getCount()) {
          removeEntry(index);
          return true;
        }
      }
      return false;
    }

    @Override
    public int size() {
      return size;
    }
  }

  class MapEntry extends AbstractEntry<K> {
    @NullableDecl final K key;

    int lastKnownIndex;

    @SuppressWarnings("unchecked") // keys only contains Ks
    MapEntry(int index) {
      this.key = (K) keys[index];
      this.lastKnownIndex = index;
    }

    @Override
    public K getElement() {
      return key;
    }

    void updateLastKnownIndex() {
      if (lastKnownIndex == -1
          || lastKnownIndex >= size()
          || !Objects.equal(key, keys[lastKnownIndex])) {
        lastKnownIndex = indexOf(key);
      }
    }

    @SuppressWarnings("unchecked") // values only contains Vs
    @Override
    public int getCount() {
      updateLastKnownIndex();
      return (lastKnownIndex == -1) ? 0 : values[lastKnownIndex];
    }

    @SuppressWarnings("unchecked") // values only contains Vs
    @CanIgnoreReturnValue
    public int setCount(int count) {
      updateLastKnownIndex();
      if (lastKnownIndex == -1) {
        put(key, count);
        return 0;
      } else {
        int old = values[lastKnownIndex];
        values[lastKnownIndex] = count;
        return old;
      }
    }
  }
}

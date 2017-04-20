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
import static com.google.common.collect.CollectPreconditions.checkPositive;
import static com.google.common.collect.CollectPreconditions.checkRemove;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multiset.Entry;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;

/** EnumCountHashMap is an implementation of {@code AbstractObjectCountMap} with enum type keys. */
@GwtCompatible(serializable = true, emulated = true)
public class EnumCountHashMap<K extends Enum<K>> extends AbstractObjectCountMap<K> {

  /** Creates an empty {@code EnumCountHashMap} instance. */
  public static <K extends Enum<K>> EnumCountHashMap<K> create(Class<K> type) {
    return new EnumCountHashMap<K>(type);
  }

  private final Class<K> keyType;

  /** Constructs a new empty instance of {@code EnumCountHashMap}. */
  EnumCountHashMap(Class<K> keyType) {
    this.keyType = keyType;
    this.keys = keyType.getEnumConstants();
    if (this.keys == null) {
      throw new IllegalStateException("Expected Enum class type, but got " + keyType.getName());
    }
    this.values = new int[this.keys.length];
    Arrays.fill(values, 0, this.keys.length, UNSET);
  }

  @Override
  int firstIndex() {
    for (int i = 0; i < this.keys.length; i++) {
      if (values[i] > 0) {
        return i;
      }
    }
    return -1;
  }

  @Override
  int nextIndex(int index) {
    for (int i = index + 1; i < this.keys.length; i++) {
      if (values[i] > 0) {
        return i;
      }
    }
    return -1;
  }

  private abstract class EnumIterator<T> extends Itr<T> {
    int nextIndex = UNSET;

    @Override
    public boolean hasNext() {
      while (index < values.length && values[index] <= 0) {
        index++;
      }
      return index != values.length;
    }

    @Override
    public T next() {
      checkForConcurrentModification();
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      nextCalled = true;
      nextIndex = index;
      return getOutput(index++);
    }

    @Override
    public void remove() {
      checkForConcurrentModification();
      checkRemove(nextCalled);
      expectedModCount++;
      removeEntry(nextIndex);
      nextCalled = false;
      nextIndex = UNSET;
      index--;
    }
  }

  @Override
  Set<K> createKeySet() {
    return new KeySetView() {
      private Object[] getFilteredKeyArray() {
        Object[] filteredKeys = new Object[size];
        for (int i = 0, j = 0; i < keys.length; i++) {
          if (values[i] != UNSET) {
            filteredKeys[j++] = keys[i];
          }
        }
        return filteredKeys;
      }

      @Override
      public Object[] toArray() {
        return getFilteredKeyArray();
      }

      @Override
      public <T> T[] toArray(T[] a) {
        return ObjectArrays.toArrayImpl(getFilteredKeyArray(), 0, size, a);
      }

      @Override
      public Iterator<K> iterator() {
        return new EnumIterator<K>() {
          @SuppressWarnings("unchecked")
          @Override
          K getOutput(int entry) {
            return (K) keys[entry];
          }
        };
      }
    };
  }

  @Override
  Multiset.Entry<K> getEntry(int index) {
    checkElementIndex(index, size);
    return new EnumMapEntry(index);
  }

  class EnumMapEntry extends MapEntry {
    EnumMapEntry(int index) {
      super(index);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int getCount() {
      return values[lastKnownIndex] == UNSET ? 0 : values[lastKnownIndex];
    }

    @SuppressWarnings("unchecked")
    @Override
    public int setCount(int count) {
      if (values[lastKnownIndex] == UNSET) {
        put(key, count);
        return 0;
      } else {
        int old = values[lastKnownIndex];
        values[lastKnownIndex] = count;
        return old == UNSET ? 0 : old;
      }
    }
  }

  @Override
  Set<Entry<K>> createEntrySet() {
    return new EntrySetView() {
      @Override
      public Iterator<Entry<K>> iterator() {
        return new EnumIterator<Entry<K>>() {
          @Override
          Entry<K> getOutput(int entry) {
            return new EnumMapEntry(entry);
          }
        };
      }
    };
  }

  @Override
  public void clear() {
    modCount++;
    if (keys != null) {
      Arrays.fill(values, 0, values.length, UNSET);
      this.size = 0;
    }
  }

  /** Returns true if key is of the proper type to be a key in this enum map. */
  private boolean isValidKey(Object key) {
    if (key == null) return false;

    // Cheaper than instanceof Enum followed by getDeclaringClass
    Class<?> keyClass = key.getClass();
    return keyClass == keyType || keyClass.getSuperclass() == keyType;
  }

  @Override
  public boolean containsKey(@Nullable Object key) {
    return isValidKey(key) && values[((Enum<?>) key).ordinal()] != UNSET;
  }

  @Override
  public int get(@Nullable Object key) {
    return containsKey(key) ? values[((Enum<?>) key).ordinal()] : 0;
  }

  @Override
  int indexOf(@Nullable Object key) {
    if (!isValidKey(key)) {
      return -1;
    }
    return ((Enum<?>) key).ordinal();
  }

  @CanIgnoreReturnValue
  @Override
  int removeEntry(int entryIndex) {
    return remove(keys[entryIndex]);
  }

  @CanIgnoreReturnValue
  @Override
  public int put(@Nullable K key, int value) {
    checkPositive(value, "count");
    typeCheck(key);
    int index = key.ordinal();
    int oldValue = values[index];
    values[index] = value;
    modCount++;
    if (oldValue == UNSET) {
      size++;
      return 0;
    }
    return oldValue;
  }

  @CanIgnoreReturnValue
  @Override
  public int remove(@Nullable Object key) {
    if (!isValidKey(key)) {
      return 0;
    }
    int index = ((Enum<?>) key).ordinal();
    int oldValue = values[index];
    if (oldValue == UNSET) {
      return 0;
    } else {
      values[index] = UNSET;
      size--;
      modCount++;
      return oldValue;
    }
  }

  /** Throws an exception if key is not of the correct type for this enum set. */
  private void typeCheck(K key) {
    Class<?> keyClass = key.getClass();
    if (keyClass != keyType && keyClass.getSuperclass() != keyType)
      throw new ClassCastException(keyClass + " != " + keyType);
  }

  @Override
  public int hashCode() {
    int h = 0;
    for (int i = 0; i < keys.length; i++) {
      h += keys[i].hashCode() ^ values[i];
    }
    return h;
  }
}

/*
 * Copyright (C) 2007 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Multiset implementation specialized for enum elements, supporting all single-element operations
 * in O(1).
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#multiset"> {@code
 * Multiset}</a>.
 *
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class EnumMultiset<E extends Enum<E>> extends AbstractMultiset<E>
    implements Serializable {
  /** Creates an empty {@code EnumMultiset}. */
  public static <E extends Enum<E>> EnumMultiset<E> create(Class<E> type) {
    return new EnumMultiset<E>(type);
  }

  /**
   * Creates a new {@code EnumMultiset} containing the specified elements.
   *
   * <p>This implementation is highly efficient when {@code elements} is itself a {@link Multiset}.
   *
   * @param elements the elements that the multiset should contain
   * @throws IllegalArgumentException if {@code elements} is empty
   */
  public static <E extends Enum<E>> EnumMultiset<E> create(Iterable<E> elements) {
    Iterator<E> iterator = elements.iterator();
    checkArgument(iterator.hasNext(), "EnumMultiset constructor passed empty Iterable");
    EnumMultiset<E> multiset = new EnumMultiset<>(iterator.next().getDeclaringClass());
    Iterables.addAll(multiset, elements);
    return multiset;
  }

  /**
   * Returns a new {@code EnumMultiset} instance containing the given elements. Unlike {@link
   * EnumMultiset#create(Iterable)}, this method does not produce an exception on an empty iterable.
   *
   * @since 14.0
   */
  public static <E extends Enum<E>> EnumMultiset<E> create(Iterable<E> elements, Class<E> type) {
    EnumMultiset<E> result = create(type);
    Iterables.addAll(result, elements);
    return result;
  }

  private transient Class<E> type;
  private transient E[] enumConstants;
  private transient int[] counts;
  private transient int distinctElements;
  private transient long size;

  /** Creates an empty {@code EnumMultiset}. */
  private EnumMultiset(Class<E> type) {
    this.type = type;
    checkArgument(type.isEnum());
    this.enumConstants = type.getEnumConstants();
    this.counts = new int[enumConstants.length];
  }

  private boolean isActuallyE(@NullableDecl Object o) {
    if (o instanceof Enum) {
      Enum<?> e = (Enum<?>) o;
      int index = e.ordinal();
      return index < enumConstants.length && enumConstants[index] == e;
    }
    return false;
  }

  /**
   * Returns {@code element} cast to {@code E}, if it actually is a nonnull E. Otherwise, throws
   * either a NullPointerException or a ClassCastException as appropriate.
   */
  void checkIsE(@NullableDecl Object element) {
    checkNotNull(element);
    if (!isActuallyE(element)) {
      throw new ClassCastException("Expected an " + type + " but got " + element);
    }
  }

  @Override
  int distinctElements() {
    return distinctElements;
  }

  @Override
  public int size() {
    return Ints.saturatedCast(size);
  }

  @Override
  public int count(@NullableDecl Object element) {
    if (!isActuallyE(element)) {
      return 0;
    }
    Enum<?> e = (Enum<?>) element;
    return counts[e.ordinal()];
  }

  // Modification Operations
  @CanIgnoreReturnValue
  @Override
  public int add(E element, int occurrences) {
    checkIsE(element);
    checkNonnegative(occurrences, "occurrences");
    if (occurrences == 0) {
      return count(element);
    }
    int index = element.ordinal();
    int oldCount = counts[index];
    long newCount = (long) oldCount + occurrences;
    checkArgument(newCount <= Integer.MAX_VALUE, "too many occurrences: %s", newCount);
    counts[index] = (int) newCount;
    if (oldCount == 0) {
      distinctElements++;
    }
    size += occurrences;
    return oldCount;
  }

  // Modification Operations
  @CanIgnoreReturnValue
  @Override
  public int remove(@NullableDecl Object element, int occurrences) {
    if (!isActuallyE(element)) {
      return 0;
    }
    Enum<?> e = (Enum<?>) element;
    checkNonnegative(occurrences, "occurrences");
    if (occurrences == 0) {
      return count(element);
    }
    int index = e.ordinal();
    int oldCount = counts[index];
    if (oldCount == 0) {
      return 0;
    } else if (oldCount <= occurrences) {
      counts[index] = 0;
      distinctElements--;
      size -= oldCount;
    } else {
      counts[index] = oldCount - occurrences;
      size -= occurrences;
    }
    return oldCount;
  }

  // Modification Operations
  @CanIgnoreReturnValue
  @Override
  public int setCount(E element, int count) {
    checkIsE(element);
    checkNonnegative(count, "count");
    int index = element.ordinal();
    int oldCount = counts[index];
    counts[index] = count;
    size += count - oldCount;
    if (oldCount == 0 && count > 0) {
      distinctElements++;
    } else if (oldCount > 0 && count == 0) {
      distinctElements--;
    }
    return oldCount;
  }

  @Override
  public void clear() {
    Arrays.fill(counts, 0);
    size = 0;
    distinctElements = 0;
  }

  abstract class Itr<T> implements Iterator<T> {
    int index = 0;
    int toRemove = -1;

    abstract T output(int index);

    @Override
    public boolean hasNext() {
      for (; index < enumConstants.length; index++) {
        if (counts[index] > 0) {
          return true;
        }
      }
      return false;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      T result = output(index);
      toRemove = index;
      index++;
      return result;
    }

    @Override
    public void remove() {
      checkRemove(toRemove >= 0);
      if (counts[toRemove] > 0) {
        distinctElements--;
        size -= counts[toRemove];
        counts[toRemove] = 0;
      }
      toRemove = -1;
    }
  }

  @Override
  Iterator<E> elementIterator() {
    return new Itr<E>() {
      @Override
      E output(int index) {
        return enumConstants[index];
      }
    };
  }

  @Override
  Iterator<Entry<E>> entryIterator() {
    return new Itr<Entry<E>>() {
      @Override
      Entry<E> output(final int index) {
        return new Multisets.AbstractEntry<E>() {
          @Override
          public E getElement() {
            return enumConstants[index];
          }

          @Override
          public int getCount() {
            return counts[index];
          }
        };
      }
    };
  }

  @Override
  public Iterator<E> iterator() {
    return Multisets.iteratorImpl(this);
  }

  @GwtIncompatible // java.io.ObjectOutputStream
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(type);
    Serialization.writeMultiset(this, stream);
  }

  /**
   * @serialData the {@code Class<E>} for the enum type, the number of distinct elements, the first
   *     element, its count, the second element, its count, and so on
   */
  @GwtIncompatible // java.io.ObjectInputStream
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    @SuppressWarnings("unchecked") // reading data stored by writeObject
    Class<E> localType = (Class<E>) stream.readObject();
    type = localType;
    enumConstants = type.getEnumConstants();
    counts = new int[enumConstants.length];
    Serialization.populateMultiset(this, stream);
  }

  @GwtIncompatible // Not needed in emulated source
  private static final long serialVersionUID = 0;
}

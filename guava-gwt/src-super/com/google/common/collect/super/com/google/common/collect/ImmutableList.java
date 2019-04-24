/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ObjectArrays.checkElementsNotNull;

import com.google.common.annotations.Beta;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.stream.Collector;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * GWT emulated version of {@link com.google.common.collect.ImmutableList}. TODO(cpovirk): more doc
 *
 * @author Hayward Chan
 */
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableList<E> extends ImmutableCollection<E>
    implements List<E>, RandomAccess {
  static final ImmutableList<Object> EMPTY =
      new RegularImmutableList<Object>(Collections.emptyList());

  ImmutableList() {}

  @Beta
  public static <E> Collector<E, ?, ImmutableList<E>> toImmutableList() {
    return CollectCollectors.toImmutableList();
  }

  // Casting to any type is safe because the list will never hold any elements.
  @SuppressWarnings("unchecked")
  public static <E> ImmutableList<E> of() {
    return (ImmutableList<E>) EMPTY;
  }

  public static <E> ImmutableList<E> of(E element) {
    return new SingletonImmutableList<E>(checkNotNull(element));
  }

  public static <E> ImmutableList<E> of(E e1, E e2) {
    return new RegularImmutableList<E>(ImmutableList.<E>nullCheckedList(e1, e2));
  }

  public static <E> ImmutableList<E> of(E e1, E e2, E e3) {
    return new RegularImmutableList<E>(ImmutableList.<E>nullCheckedList(e1, e2, e3));
  }

  public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4) {
    return new RegularImmutableList<E>(ImmutableList.<E>nullCheckedList(e1, e2, e3, e4));
  }

  public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5) {
    return new RegularImmutableList<E>(ImmutableList.<E>nullCheckedList(e1, e2, e3, e4, e5));
  }

  public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
    return new RegularImmutableList<E>(ImmutableList.<E>nullCheckedList(e1, e2, e3, e4, e5, e6));
  }

  public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
    return new RegularImmutableList<E>(
        ImmutableList.<E>nullCheckedList(e1, e2, e3, e4, e5, e6, e7));
  }

  public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
    return new RegularImmutableList<E>(
        ImmutableList.<E>nullCheckedList(e1, e2, e3, e4, e5, e6, e7, e8));
  }

  public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
    return new RegularImmutableList<E>(
        ImmutableList.<E>nullCheckedList(e1, e2, e3, e4, e5, e6, e7, e8, e9));
  }

  public static <E> ImmutableList<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
    return new RegularImmutableList<E>(
        ImmutableList.<E>nullCheckedList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
  }

  public static <E> ImmutableList<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11) {
    return new RegularImmutableList<E>(
        ImmutableList.<E>nullCheckedList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11));
  }

  public static <E> ImmutableList<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12, E... others) {
    final int paramCount = 12;
    Object[] array = new Object[paramCount + others.length];
    arrayCopy(array, 0, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12);
    arrayCopy(array, paramCount, others);
    return new RegularImmutableList<E>(ImmutableList.<E>nullCheckedList(array));
  }

  private static void arrayCopy(Object[] dest, int pos, Object... source) {
    System.arraycopy(source, 0, dest, pos, source.length);
  }

  public static <E> ImmutableList<E> copyOf(Iterable<? extends E> elements) {
    checkNotNull(elements); // for GWT
    return (elements instanceof Collection)
        ? copyOf((Collection<? extends E>) elements)
        : copyOf(elements.iterator());
  }

  public static <E> ImmutableList<E> copyOf(Iterator<? extends E> elements) {
    return copyFromCollection(Lists.newArrayList(elements));
  }

  public static <E> ImmutableList<E> copyOf(Collection<? extends E> elements) {
    if (elements instanceof ImmutableCollection) {
      /*
       * TODO: When given an ImmutableList that's a sublist, copy the referenced
       * portion of the array into a new array to save space?
       */
      @SuppressWarnings("unchecked") // all supported methods are covariant
      ImmutableCollection<E> list = (ImmutableCollection<E>) elements;
      return list.asList();
    }
    return copyFromCollection(elements);
  }

  public static <E> ImmutableList<E> copyOf(E[] elements) {
    checkNotNull(elements); // eager for GWT
    return copyOf(Arrays.asList(elements));
  }

  private static <E> ImmutableList<E> copyFromCollection(Collection<? extends E> collection) {
    Object[] elements = collection.toArray();
    switch (elements.length) {
      case 0:
        return of();
      case 1:
        return of((E) elements[0]);
      default:
        return new RegularImmutableList<E>(ImmutableList.<E>nullCheckedList(elements));
    }
  }

  // Factory method that skips the null checks.  Used only when the elements
  // are guaranteed to be non-null.
  static <E> ImmutableList<E> unsafeDelegateList(List<? extends E> list) {
    switch (list.size()) {
      case 0:
        return of();
      case 1:
        return of(list.get(0));
      default:
        @SuppressWarnings("unchecked")
        List<E> castedList = (List<E>) list;
        return new RegularImmutableList<E>(castedList);
    }
  }

  /**
   * Views the array as an immutable list. The array must have only {@code E} elements.
   *
   * <p>The array must be internally created.
   */
  @SuppressWarnings("unchecked") // caller is reponsible for getting this right
  static <E> ImmutableList<E> asImmutableList(Object[] elements) {
    return unsafeDelegateList((List) Arrays.asList(elements));
  }

  public static <E extends Comparable<? super E>> ImmutableList<E> sortedCopyOf(
      Iterable<? extends E> elements) {
    Comparable[] array = Iterables.toArray(elements, new Comparable[0]);
    checkElementsNotNull(array);
    Arrays.sort(array);
    return asImmutableList(array);
  }

  public static <E> ImmutableList<E> sortedCopyOf(
      Comparator<? super E> comparator, Iterable<? extends E> elements) {
    checkNotNull(comparator);
    @SuppressWarnings("unchecked") // all supported methods are covariant
    E[] array = (E[]) Iterables.toArray(elements);
    checkElementsNotNull(array);
    Arrays.sort(array, comparator);
    return asImmutableList(array);
  }

  private static <E> List<E> nullCheckedList(Object... array) {
    for (int i = 0, len = array.length; i < len; i++) {
      if (array[i] == null) {
        throw new NullPointerException("at index " + i);
      }
    }
    @SuppressWarnings("unchecked")
    E[] castedArray = (E[]) array;
    return Arrays.asList(castedArray);
  }

  @Override
  public int indexOf(@Nullable Object object) {
    return (object == null) ? -1 : Lists.indexOfImpl(this, object);
  }

  @Override
  public int lastIndexOf(@Nullable Object object) {
    return (object == null) ? -1 : Lists.lastIndexOfImpl(this, object);
  }

  public final boolean addAll(int index, Collection<? extends E> newElements) {
    throw new UnsupportedOperationException();
  }

  public final E set(int index, E element) {
    throw new UnsupportedOperationException();
  }

  public final void add(int index, E element) {
    throw new UnsupportedOperationException();
  }

  public final E remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public UnmodifiableIterator<E> iterator() {
    return listIterator();
  }

  @Override
  public ImmutableList<E> subList(int fromIndex, int toIndex) {
    return unsafeDelegateList(Lists.subListImpl(this, fromIndex, toIndex));
  }

  @Override
  public UnmodifiableListIterator<E> listIterator() {
    return listIterator(0);
  }

  @Override
  public UnmodifiableListIterator<E> listIterator(int index) {
    return new AbstractIndexedListIterator<E>(size(), index) {
      @Override
      protected E get(int index) {
        return ImmutableList.this.get(index);
      }
    };
  }

  @Override
  public ImmutableList<E> asList() {
    return this;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return Lists.equalsImpl(this, obj);
  }

  @Override
  public int hashCode() {
    return Lists.hashCodeImpl(this);
  }

  public ImmutableList<E> reverse() {
    List<E> list = Lists.newArrayList(this);
    Collections.reverse(list);
    return unsafeDelegateList(list);
  }

  public static <E> Builder<E> builder() {
    return new Builder<E>();
  }

  public static <E> Builder<E> builderWithExpectedSize(int expectedSize) {
    return new Builder<E>(expectedSize);
  }

  public static final class Builder<E> extends ImmutableCollection.Builder<E> {
    private final ArrayList<E> contents;

    public Builder() {
      contents = Lists.newArrayList();
    }

    Builder(int capacity) {
      contents = Lists.newArrayListWithCapacity(capacity);
    }

    @CanIgnoreReturnValue
    @Override
    public Builder<E> add(E element) {
      contents.add(checkNotNull(element));
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Builder<E> addAll(Iterable<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Builder<E> add(E... elements) {
      checkNotNull(elements); // for GWT
      super.add(elements);
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public Builder<E> addAll(Iterator<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    @CanIgnoreReturnValue
    Builder<E> combine(Builder<E> builder) {
      checkNotNull(builder);
      contents.addAll(builder.contents);
      return this;
    }

    @Override
    public ImmutableList<E> build() {
      return copyOf(contents);
    }
  }
}

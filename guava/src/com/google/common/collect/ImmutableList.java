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

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.collect.ObjectArrays.arraysCopyOf;
import static com.google.common.collect.ObjectArrays.checkElementsNotNull;
import static com.google.common.collect.RegularImmutableList.EMPTY;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import javax.annotation.Nullable;

/**
 * A {@link List} whose contents will never change, with many other important properties detailed at
 * {@link ImmutableCollection}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained">
 * immutable collections</a>.
 *
 * @see ImmutableMap
 * @see ImmutableSet
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableList<E> extends ImmutableCollection<E>
    implements List<E>, RandomAccess {
  /**
   * Returns the empty immutable list. This list behaves and performs comparably
   * to {@link Collections#emptyList}, and is preferable mainly for consistency
   * and maintainability of your code.
   */
  // Casting to any type is safe because the list will never hold any elements.
  @SuppressWarnings("unchecked")
  public static <E> ImmutableList<E> of() {
    return (ImmutableList<E>) EMPTY;
  }

  /**
   * Returns an immutable list containing a single element. This list behaves
   * and performs comparably to {@link Collections#singleton}, but will not
   * accept a null element. It is preferable mainly for consistency and
   * maintainability of your code.
   *
   * @throws NullPointerException if {@code element} is null
   */
  public static <E> ImmutableList<E> of(E element) {
    return new SingletonImmutableList<E>(element);
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableList<E> of(E e1, E e2) {
    return construct(e1, e2);
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableList<E> of(E e1, E e2, E e3) {
    return construct(e1, e2, e3);
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4) {
    return construct(e1, e2, e3, e4);
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5) {
    return construct(e1, e2, e3, e4, e5);
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
    return construct(e1, e2, e3, e4, e5, e6);
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
    return construct(e1, e2, e3, e4, e5, e6, e7);
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
    return construct(e1, e2, e3, e4, e5, e6, e7, e8);
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
    return construct(e1, e2, e3, e4, e5, e6, e7, e8, e9);
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableList<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
    return construct(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableList<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11) {
    return construct(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11);
  }

  // These go up to eleven. After that, you just get the varargs form, and
  // whatever warnings might come along with it. :(

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   * @since 3.0 (source-compatible since 2.0)
   */
  @SafeVarargs // For Eclipse. For internal javac we have disabled this pointless type of warning.
  public static <E> ImmutableList<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12, E... others) {
    Object[] array = new Object[12 + others.length];
    array[0] = e1;
    array[1] = e2;
    array[2] = e3;
    array[3] = e4;
    array[4] = e5;
    array[5] = e6;
    array[6] = e7;
    array[7] = e8;
    array[8] = e9;
    array[9] = e10;
    array[10] = e11;
    array[11] = e12;
    System.arraycopy(others, 0, array, 12, others.length);
    return construct(array);
  }

  /**
   * Returns an immutable list containing the given elements, in order. If
   * {@code elements} is a {@link Collection}, this method behaves exactly as
   * {@link #copyOf(Collection)}; otherwise, it behaves exactly as {@code
   * copyOf(elements.iterator()}.
   *
   * @throws NullPointerException if any of {@code elements} is null
   */
  public static <E> ImmutableList<E> copyOf(Iterable<? extends E> elements) {
    checkNotNull(elements); // TODO(kevinb): is this here only for GWT?
    return (elements instanceof Collection)
        ? copyOf((Collection<? extends E>) elements)
        : copyOf(elements.iterator());
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying
   * the data when it is safe to do so. The exact circumstances under which a
   * copy will or will not be performed are undocumented and subject to change.
   *
   * <p>Note that if {@code list} is a {@code List<String>}, then {@code
   * ImmutableList.copyOf(list)} returns an {@code ImmutableList<String>}
   * containing each of the strings in {@code list}, while
   * ImmutableList.of(list)} returns an {@code ImmutableList<List<String>>}
   * containing one element (the given list itself).
   *
   * <p>This method is safe to use even when {@code elements} is a synchronized
   * or concurrent collection that is currently being modified by another
   * thread.
   *
   * @throws NullPointerException if any of {@code elements} is null
   */
  public static <E> ImmutableList<E> copyOf(Collection<? extends E> elements) {
    if (elements instanceof ImmutableCollection) {
      @SuppressWarnings("unchecked") // all supported methods are covariant
      ImmutableList<E> list = ((ImmutableCollection<E>) elements).asList();
      return list.isPartialView() ? ImmutableList.<E>asImmutableList(list.toArray()) : list;
    }
    return construct(elements.toArray());
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any of {@code elements} is null
   */
  public static <E> ImmutableList<E> copyOf(Iterator<? extends E> elements) {
    // We special-case for 0 or 1 elements, but going further is madness.
    if (!elements.hasNext()) {
      return of();
    }
    E first = elements.next();
    if (!elements.hasNext()) {
      return of(first);
    } else {
      return new ImmutableList.Builder<E>().add(first).addAll(elements).build();
    }
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any of {@code elements} is null
   * @since 3.0
   */
  public static <E> ImmutableList<E> copyOf(E[] elements) {
    switch (elements.length) {
      case 0:
        return ImmutableList.of();
      case 1:
        return new SingletonImmutableList<E>(elements[0]);
      default:
        return new RegularImmutableList<E>(checkElementsNotNull(elements.clone()));
    }
  }

  /**
   * Views the array as an immutable list.  Checks for nulls; does not copy.
   */
  private static <E> ImmutableList<E> construct(Object... elements) {
    return asImmutableList(checkElementsNotNull(elements));
  }

  /**
   * Views the array as an immutable list.  Does not check for nulls; does not copy.
   *
   * <p>The array must be internally created.
   */
  static <E> ImmutableList<E> asImmutableList(Object[] elements) {
    return asImmutableList(elements, elements.length);
  }

  /**
   * Views the array as an immutable list. Copies if the specified range does not cover the complete
   * array. Does not check for nulls.
   */
  static <E> ImmutableList<E> asImmutableList(Object[] elements, int length) {
    switch (length) {
      case 0:
        return of();
      case 1:
        @SuppressWarnings("unchecked") // collection had only Es in it
        ImmutableList<E> list = new SingletonImmutableList<E>((E) elements[0]);
        return list;
      default:
        if (length < elements.length) {
          elements = arraysCopyOf(elements, length);
        }
        return new RegularImmutableList<E>(elements);
    }
  }

  ImmutableList() {}

  // This declaration is needed to make List.iterator() and
  // ImmutableCollection.iterator() consistent.
  @Override
  public UnmodifiableIterator<E> iterator() {
    return listIterator();
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
  public int indexOf(@Nullable Object object) {
    return (object == null) ? -1 : Lists.indexOfImpl(this, object);
  }

  @Override
  public int lastIndexOf(@Nullable Object object) {
    return (object == null) ? -1 : Lists.lastIndexOfImpl(this, object);
  }

  @Override
  public boolean contains(@Nullable Object object) {
    return indexOf(object) >= 0;
  }

  // constrain the return type to ImmutableList<E>

  /**
   * Returns an immutable list of the elements between the specified {@code
   * fromIndex}, inclusive, and {@code toIndex}, exclusive. (If {@code
   * fromIndex} and {@code toIndex} are equal, the empty immutable list is
   * returned.)
   */
  @Override
  public ImmutableList<E> subList(int fromIndex, int toIndex) {
    checkPositionIndexes(fromIndex, toIndex, size());
    int length = toIndex - fromIndex;
    if (length == size()) {
      return this;
    }
    switch (length) {
      case 0:
        return of();
      case 1:
        return of(get(fromIndex));
      default:
        return subListUnchecked(fromIndex, toIndex);
    }
  }

  /**
   * Called by the default implementation of {@link #subList} when {@code
   * toIndex - fromIndex > 1}, after index validation has already been
   * performed.
   */
  ImmutableList<E> subListUnchecked(int fromIndex, int toIndex) {
    return new SubList(fromIndex, toIndex - fromIndex);
  }

  class SubList extends ImmutableList<E> {
    final transient int offset;
    final transient int length;

    SubList(int offset, int length) {
      this.offset = offset;
      this.length = length;
    }

    @Override
    public int size() {
      return length;
    }

    @Override
    public E get(int index) {
      checkElementIndex(index, length);
      return ImmutableList.this.get(index + offset);
    }

    @Override
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
      checkPositionIndexes(fromIndex, toIndex, length);
      return ImmutableList.this.subList(fromIndex + offset, toIndex + offset);
    }

    @Override
    boolean isPartialView() {
      return true;
    }
  }

  /**
   * Guaranteed to throw an exception and leave the list unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  public final boolean addAll(int index, Collection<? extends E> newElements) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the list unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  public final E set(int index, E element) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the list unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public final void add(int index, E element) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the list unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  public final E remove(int index) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns this list instance.
   *
   * @since 2.0
   */
  @Override
  public final ImmutableList<E> asList() {
    return this;
  }

  @Override
  int copyIntoArray(Object[] dst, int offset) {
    // this loop is faster for RandomAccess instances, which ImmutableLists are
    int size = size();
    for (int i = 0; i < size; i++) {
      dst[offset + i] = get(i);
    }
    return offset + size;
  }

  /**
   * Returns a view of this immutable list in reverse order. For example, {@code
   * ImmutableList.of(1, 2, 3).reverse()} is equivalent to {@code
   * ImmutableList.of(3, 2, 1)}.
   *
   * @return a view of this immutable list in reverse order
   * @since 7.0
   */
  public ImmutableList<E> reverse() {
    return (size() <= 1) ? this : new ReverseImmutableList<E>(this);
  }

  private static class ReverseImmutableList<E> extends ImmutableList<E> {
    private final transient ImmutableList<E> forwardList;

    ReverseImmutableList(ImmutableList<E> backingList) {
      this.forwardList = backingList;
    }

    private int reverseIndex(int index) {
      return (size() - 1) - index;
    }

    private int reversePosition(int index) {
      return size() - index;
    }

    @Override
    public ImmutableList<E> reverse() {
      return forwardList;
    }

    @Override
    public boolean contains(@Nullable Object object) {
      return forwardList.contains(object);
    }

    @Override
    public int indexOf(@Nullable Object object) {
      int index = forwardList.lastIndexOf(object);
      return (index >= 0) ? reverseIndex(index) : -1;
    }

    @Override
    public int lastIndexOf(@Nullable Object object) {
      int index = forwardList.indexOf(object);
      return (index >= 0) ? reverseIndex(index) : -1;
    }

    @Override
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
      checkPositionIndexes(fromIndex, toIndex, size());
      return forwardList.subList(reversePosition(toIndex), reversePosition(fromIndex)).reverse();
    }

    @Override
    public E get(int index) {
      checkElementIndex(index, size());
      return forwardList.get(reverseIndex(index));
    }

    @Override
    public int size() {
      return forwardList.size();
    }

    @Override
    boolean isPartialView() {
      return forwardList.isPartialView();
    }
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return Lists.equalsImpl(this, obj);
  }

  @Override
  public int hashCode() {
    int hashCode = 1;
    int n = size();
    for (int i = 0; i < n; i++) {
      hashCode = 31 * hashCode + get(i).hashCode();

      hashCode = ~~hashCode;
      // needed to deal with GWT integer overflow
    }
    return hashCode;
  }

  /*
   * Serializes ImmutableLists as their logical contents. This ensures that
   * implementation types do not leak into the serialized representation.
   */
  static class SerializedForm implements Serializable {
    final Object[] elements;

    SerializedForm(Object[] elements) {
      this.elements = elements;
    }

    Object readResolve() {
      return copyOf(elements);
    }

    private static final long serialVersionUID = 0;
  }

  private void readObject(ObjectInputStream stream) throws InvalidObjectException {
    throw new InvalidObjectException("Use SerializedForm");
  }

  @Override
  Object writeReplace() {
    return new SerializedForm(toArray());
  }

  /**
   * Returns a new builder. The generated builder is equivalent to the builder
   * created by the {@link Builder} constructor.
   */
  public static <E> Builder<E> builder() {
    return new Builder<E>();
  }

  /**
   * A builder for creating immutable list instances, especially {@code public
   * static final} lists ("constant lists"). Example: <pre>   {@code
   *
   *   public static final ImmutableList<Color> GOOGLE_COLORS
   *       = new ImmutableList.Builder<Color>()
   *           .addAll(WEBSAFE_COLORS)
   *           .add(new Color(0, 191, 255))
   *           .build();}</pre>
   *
   * <p>Builder instances can be reused; it is safe to call {@link #build} multiple
   * times to build multiple lists in series. Each new list contains all the
   * elements of the ones created before it.
   *
   * @since 2.0
   */
  public static final class Builder<E> extends ImmutableCollection.ArrayBasedBuilder<E> {
    /**
     * Creates a new builder. The returned builder is equivalent to the builder
     * generated by {@link ImmutableList#builder}.
     */
    public Builder() {
      this(DEFAULT_INITIAL_CAPACITY);
    }

    // TODO(lowasser): consider exposing this
    Builder(int capacity) {
      super(capacity);
    }

    /**
     * Adds {@code element} to the {@code ImmutableList}.
     *
     * @param element the element to add
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code element} is null
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<E> add(E element) {
      super.add(element);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableList}.
     *
     * @param elements the {@code Iterable} to add to the {@code ImmutableList}
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a
     *     null element
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<E> addAll(Iterable<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableList}.
     *
     * @param elements the {@code Iterable} to add to the {@code ImmutableList}
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a
     *     null element
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<E> add(E... elements) {
      super.add(elements);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableList}.
     *
     * @param elements the {@code Iterable} to add to the {@code ImmutableList}
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a
     *     null element
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<E> addAll(Iterator<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    /**
     * Returns a newly-created {@code ImmutableList} based on the contents of
     * the {@code Builder}.
     */
    @Override
    public ImmutableList<E> build() {
      return asImmutableList(contents, size);
    }
  }
}

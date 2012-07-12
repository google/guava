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
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.collect.ObjectArrays.checkElementNotNull;

import com.google.common.annotations.GwtCompatible;

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
 * A high-performance, immutable, random-access {@code List} implementation.
 * Does not permit null elements.
 *
 * <p>Unlike {@link Collections#unmodifiableList}, which is a <i>view</i> of a
 * separate collection that can still change, an instance of {@code
 * ImmutableList} contains its own private data and will <i>never</i> change.
 * {@code ImmutableList} is convenient for {@code public static final} lists
 * ("constant lists") and also lets you easily make a "defensive copy" of a list
 * provided to your class by a caller.
 *
 * <p><b>Note:</b> Although this class is not final, it cannot be subclassed as
 * it has no public or protected constructors. Thus, instances of this type are
 * guaranteed to be immutable.
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/ImmutableCollectionsExplained">
 * immutable collections</a>.
 *
 * @see ImmutableMap
 * @see ImmutableSet
 * @author Kevin Bourrillion
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableList<E> extends ImmutableCollection<E>
    implements List<E>, RandomAccess {
  /**
   * Returns the empty immutable list. This set behaves and performs comparably
   * to {@link Collections#emptyList}, and is preferable mainly for consistency
   * and maintainability of your code.
   */
  // Casting to any type is safe because the list will never hold any elements.
  @SuppressWarnings("unchecked")
  public static <E> ImmutableList<E> of() {
    return (ImmutableList<E>) EmptyImmutableList.INSTANCE;
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
  public static <E> ImmutableList<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
    return construct(e1, e2, e3, e4, e5, e6, e7);
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableList<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
    return construct(e1, e2, e3, e4, e5, e6, e7, e8);
  }

  /**
   * Returns an immutable list containing the given elements, in order.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableList<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
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
  public static <E> ImmutableList<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12,
      E... others) {
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
      ? copyOf(Collections2.cast(elements))
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
      return list.isPartialView() ? copyFromCollection(list) : list;
    }
    return copyFromCollection(elements);
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
      return new ImmutableList.Builder<E>()
          .add(first)
          .addAll(elements)
          .build();
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
        return construct(elements.clone());
    }
  }

  /**
   * Views the array as an immutable list.  The array must have only non-null {@code E} elements.
   *
   * <p>The array must be internally created.
   */
  static <E> ImmutableList<E> asImmutableList(Object[] elements) {
    switch (elements.length) {
      case 0:
        return of();
      case 1:
        @SuppressWarnings("unchecked") // collection had only Es in it
        ImmutableList<E> list = new SingletonImmutableList<E>((E) elements[0]);
        return list;
      default:
        return construct(elements);
    }
  }

  private static <E> ImmutableList<E> copyFromCollection(
      Collection<? extends E> collection) {
    return asImmutableList(collection.toArray());
  }

  /** {@code elements} has to be internally created array. */
  private static <E> ImmutableList<E> construct(Object... elements) {
    for (int i = 0; i < elements.length; i++) {
      ObjectArrays.checkElementNotNull(elements[i], i);
    }
    return new RegularImmutableList<E>(elements);
  }

  ImmutableList() {}

  // This declaration is needed to make List.iterator() and
  // ImmutableCollection.iterator() consistent.
  @Override public UnmodifiableIterator<E> iterator() {
    return listIterator();
  }

  @Override public UnmodifiableListIterator<E> listIterator() {
    return listIterator(0);
  }

  @Override public UnmodifiableListIterator<E> listIterator(int index) {
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
    transient final int offset;
    transient final int length;

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
   */
  @Override
  public final boolean addAll(int index, Collection<? extends E> newElements) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the list unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public final E set(int index, E element) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the list unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public final void add(int index, E element) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the list unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public final E remove(int index) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns this list instance.
   *
   * @since 2.0
   */
  @Override public ImmutableList<E> asList() {
    return this;
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
    return new ReverseImmutableList<E>(this);
  }

  private static class ReverseImmutableList<E> extends ImmutableList<E> {
    private final transient ImmutableList<E> forwardList;
    private final transient int size;

    ReverseImmutableList(ImmutableList<E> backingList) {
      this.forwardList = backingList;
      this.size = backingList.size();
    }

    private int reverseIndex(int index) {
      return (size - 1) - index;
    }

    private int reversePosition(int index) {
      return size - index;
    }

    @Override public ImmutableList<E> reverse() {
      return forwardList;
    }

    @Override public boolean contains(@Nullable Object object) {
      return forwardList.contains(object);
    }

    @Override public boolean containsAll(Collection<?> targets) {
      return forwardList.containsAll(targets);
    }

    @Override public int indexOf(@Nullable Object object) {
      int index = forwardList.lastIndexOf(object);
      return (index >= 0) ? reverseIndex(index) : -1;
    }

    @Override public int lastIndexOf(@Nullable Object object) {
      int index = forwardList.indexOf(object);
      return (index >= 0) ? reverseIndex(index) : -1;
    }

    @Override public ImmutableList<E> subList(int fromIndex, int toIndex) {
      checkPositionIndexes(fromIndex, toIndex, size);
      return forwardList.subList(
          reversePosition(toIndex), reversePosition(fromIndex)).reverse();
    }

    @Override public E get(int index) {
      checkElementIndex(index, size);
      return forwardList.get(reverseIndex(index));
    }

    @Override public UnmodifiableListIterator<E> listIterator(int index) {
      checkPositionIndex(index, size);
      final UnmodifiableListIterator<E> forward =
          forwardList.listIterator(reversePosition(index));
      return new UnmodifiableListIterator<E>() {
        @Override public boolean hasNext() {
          return forward.hasPrevious();
        }

        @Override public boolean hasPrevious() {
          return forward.hasNext();
        }

        @Override public E next() {
          return forward.previous();
        }

        @Override public int nextIndex() {
          return reverseIndex(forward.previousIndex());
        }

        @Override public E previous() {
          return forward.next();
        }

        @Override public int previousIndex() {
          return reverseIndex(forward.nextIndex());
        }
      };
    }

    @Override public int size() {
      return size;
    }

    @Override public boolean isEmpty() {
      return forwardList.isEmpty();
    }

    @Override boolean isPartialView() {
      return forwardList.isPartialView();
    }
  }

  @Override public boolean equals(Object obj) {
    return Lists.equalsImpl(this, obj);
  }

  @Override public int hashCode() {
    return Lists.hashCodeImpl(this);
  }

  /*
   * Serializes ImmutableLists as their logical contents. This ensures that
   * implementation types do not leak into the serialized representation.
   */
  private static class SerializedForm implements Serializable {
    final Object[] elements;
    SerializedForm(Object[] elements) {
      this.elements = elements;
    }
    Object readResolve() {
      return copyOf(elements);
    }
    private static final long serialVersionUID = 0;
  }

  private void readObject(ObjectInputStream stream)
      throws InvalidObjectException {
    throw new InvalidObjectException("Use SerializedForm");
  }

  @Override Object writeReplace() {
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
   * Builder instances can be reused; it is safe to call {@link #build} multiple
   * times to build multiple lists in series. Each new list contains all the
   * elements of the ones created before it.
   *
   * @since 2.0 (imported from Google Collections Library)
   */
  public static final class Builder<E> extends ImmutableCollection.Builder<E> {
    private Object[] contents;
    private int size;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder
     * generated by {@link ImmutableList#builder}.
     */
    public Builder() {
      this(DEFAULT_INITIAL_CAPACITY);
    }

    // TODO(user): consider exposing this
    Builder(int capacity) {
      this.contents = new Object[capacity];
      this.size = 0;
    }

    /**
     * Expand capacity to allow the specified number of elements to be added.
     */
    Builder<E> expandFor(int count) {
      int minCapacity = size + count;
      if (contents.length < minCapacity) {
        this.contents = ObjectArrays.arraysCopyOf(
            this.contents, expandedCapacity(contents.length, minCapacity));
      }
      return this;
    }

    /**
     * Adds {@code element} to the {@code ImmutableList}.
     *
     * @param element the element to add
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code element} is null
     */
    @Override public Builder<E> add(E element) {
      checkNotNull(element);
      expandFor(1);
      contents[size++] = element;
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
    @Override public Builder<E> addAll(Iterable<? extends E> elements) {
      if (elements instanceof Collection) {
        Collection<?> collection = (Collection<?>) elements;
        expandFor(collection.size());
      }
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
    @Override public Builder<E> add(E... elements) {
      for (int i = 0; i < elements.length; i++) {
        checkElementNotNull(elements[i], i);
      }
      expandFor(elements.length);
      System.arraycopy(elements, 0, contents, size, elements.length);
      size += elements.length;
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
    @Override public Builder<E> addAll(Iterator<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    /**
     * Returns a newly-created {@code ImmutableList} based on the contents of
     * the {@code Builder}.
     */
    @Override public ImmutableList<E> build() {
      switch (size) {
        case 0:
          return of();
        case 1:
          @SuppressWarnings("unchecked") // guaranteed to be an E
          E singleElement = (E) contents[0];
          return of(singleElement);
        default:
          if (size == contents.length) {
            // no need to copy; any further add operations on the builder will copy the buffer
            return new RegularImmutableList<E>(contents);
          } else {
            return new RegularImmutableList<E>(ObjectArrays.arraysCopyOf(contents, size));
          }
      }
    }
  }
}

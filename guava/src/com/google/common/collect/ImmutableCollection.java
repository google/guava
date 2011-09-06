/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nullable;

/**
 * An immutable collection. Does not permit null elements.
 *
 * <p>In addition to the {@link Collection} methods, this class has an {@link
 * #asList()} method, which returns a list view of the collection's elements.
 *
 * <p><b>Note:</b> Although this class is not final, it cannot be subclassed
 * outside of this package as it has no public or protected constructors. Thus,
 * instances of this type are guaranteed to be immutable.
 *
 * @author Jesse Wilson
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableCollection<E>
    implements Collection<E>, Serializable {
  static final ImmutableCollection<Object> EMPTY_IMMUTABLE_COLLECTION
      = new EmptyImmutableCollection();

  ImmutableCollection() {}

  /**
   * Returns an unmodifiable iterator across the elements in this collection.
   */
  @Override
  public abstract UnmodifiableIterator<E> iterator();

  @Override
  public Object[] toArray() {
    return ObjectArrays.toArrayImpl(this);
  }

  @Override
  public <T> T[] toArray(T[] other) {
    return ObjectArrays.toArrayImpl(this, other);
  }

  @Override
  public boolean contains(@Nullable Object object) {
    return object != null && Iterators.contains(iterator(), object);
  }

  @Override
  public boolean containsAll(Collection<?> targets) {
    return Collections2.containsAllImpl(this, targets);
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override public String toString() {
    return Collections2.toStringImpl(this);
  }

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public final boolean add(E e) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public final boolean remove(Object object) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public final boolean addAll(Collection<? extends E> newElements) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public final boolean removeAll(Collection<?> oldElements) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public final boolean retainAll(Collection<?> elementsToKeep) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   */
  @Override
  public final void clear() {
    throw new UnsupportedOperationException();
  }

  /*
   * TODO(kevinb): Restructure code so ImmutableList doesn't contain this
   * variable, which it doesn't use.
   */
  private transient ImmutableList<E> asList;

  /**
   * Returns a list view of the collection.
   *
   * @since 2.0
   */
  public ImmutableList<E> asList() {
    ImmutableList<E> list = asList;
    return (list == null) ? (asList = createAsList()) : list;
  }

  ImmutableList<E> createAsList() {
    switch (size()) {
      case 0:
        return ImmutableList.of();
      case 1:
        return ImmutableList.of(iterator().next());
      default:
        return new ImmutableAsList<E>(toArray(), this);
    }
  }

  abstract boolean isPartialView();

  private static class EmptyImmutableCollection
      extends ImmutableCollection<Object> {
    @Override
    public int size() {
      return 0;
    }

    @Override public boolean isEmpty() {
      return true;
    }

    @Override public boolean contains(@Nullable Object object) {
      return false;
    }

    @Override public UnmodifiableIterator<Object> iterator() {
      return Iterators.EMPTY_ITERATOR;
    }

    private static final Object[] EMPTY_ARRAY = new Object[0];

    @Override public Object[] toArray() {
      return EMPTY_ARRAY;
    }

    @Override public <T> T[] toArray(T[] array) {
      if (array.length > 0) {
        array[0] = null;
      }
      return array;
    }

    @Override ImmutableList<Object> createAsList() {
      return ImmutableList.of();
    }

    @Override boolean isPartialView() {
      return false;
    }
  }

  /**
   * Nonempty collection stored in an array.
   */
  private static class ArrayImmutableCollection<E>
      extends ImmutableCollection<E> {
    private final E[] elements;

    ArrayImmutableCollection(E[] elements) {
      this.elements = elements;
    }

    @Override
    public int size() {
      return elements.length;
    }

    @Override public boolean isEmpty() {
      return false;
    }

    @Override public UnmodifiableIterator<E> iterator() {
      return Iterators.forArray(elements);
    }

    @Override ImmutableList<E> createAsList() {
      return elements.length == 1 ? new SingletonImmutableList<E>(elements[0])
          : new RegularImmutableList<E>(elements);
    }

    @Override boolean isPartialView() {
      return false;
    }
  }

  /*
   * Serializes ImmutableCollections as their logical contents. This ensures
   * that implementation types do not leak into the serialized representation.
   */
  private static class SerializedForm implements Serializable {
    final Object[] elements;
    SerializedForm(Object[] elements) {
      this.elements = elements;
    }
    Object readResolve() {
      return elements.length == 0
          ? EMPTY_IMMUTABLE_COLLECTION
          : new ArrayImmutableCollection<Object>(Platform.clone(elements));
    }
    private static final long serialVersionUID = 0;
  }

  Object writeReplace() {
    return new SerializedForm(toArray());
  }

  /**
   * Abstract base class for builders of {@link ImmutableCollection} types.
   *
   * @since 10.0
   */
  public abstract static class Builder<E> {

    Builder() {
    }

    /**
     * Adds {@code element} to the {@code ImmutableCollection} being built.
     *
     * <p>Note that each builder class covariantly returns its own type from
     * this method.
     *
     * @param element the element to add
     * @return this {@code Builder} instance
     * @throws NullPointerException if {@code element} is null
     */
    public abstract Builder<E> add(E element);

    /**
     * Adds each element of {@code elements} to the {@code ImmutableCollection}
     * being built.
     *
     * <p>Note that each builder class overrides this method in order to
     * covariantly return its own type.
     *
     * @param elements the elements to add
     * @return this {@code Builder} instance
     * @throws NullPointerException if {@code elements} is null or contains a
     *     null element
     */
    public Builder<E> add(E... elements) {
      for (E element : elements) {
        add(element);
      }
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableCollection}
     * being built.
     *
     * <p>Note that each builder class overrides this method in order to
     * covariantly return its own type.
     *
     * @param elements the elements to add
     * @return this {@code Builder} instance
     * @throws NullPointerException if {@code elements} is null or contains a
     *     null element
     */
    public Builder<E> addAll(Iterable<? extends E> elements) {
      for (E element : elements) {
        add(element);
      }
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableCollection}
     * being built.
     *
     * <p>Note that each builder class overrides this method in order to
     * covariantly return its own type.
     *
     * @param elements the elements to add
     * @return this {@code Builder} instance
     * @throws NullPointerException if {@code elements} is null or contains a
     *     null element
     */
    public Builder<E> addAll(Iterator<? extends E> elements) {
      while (elements.hasNext()) {
        add(elements.next());
      }
      return this;
    }

    /**
     * Returns a newly-created {@code ImmutableCollection} of the appropriate
     * type, containing the elements provided to this builder.
     *
     * <p>Note that each builder class covariantly returns the appropriate type
     * of {@code ImmutableCollection} from this method.
     */
    public abstract ImmutableCollection<E> build();
  }
}

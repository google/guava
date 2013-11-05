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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.ObjectArrays.checkElementsNotNull;

import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;
import java.util.AbstractCollection;
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
public abstract class ImmutableCollection<E> extends AbstractCollection<E>
    implements Serializable {

  ImmutableCollection() {}

  /**
   * Returns an unmodifiable iterator across the elements in this collection.
   */
  @Override
  public abstract UnmodifiableIterator<E> iterator();

  @Override
  public final Object[] toArray() {
    int size = size();
    if (size == 0) {
      return ObjectArrays.EMPTY_ARRAY;
    }
    Object[] result = new Object[size()];
    copyIntoArray(result, 0);
    return result;
  }

  @Override
  public final <T> T[] toArray(T[] other) {
    checkNotNull(other);
    int size = size();
    if (other.length < size) {
      other = ObjectArrays.newArray(other, size);
    } else if (other.length > size) {
      other[size] = null;
    }
    copyIntoArray(other, 0);
    return other;
  }

  @Override
  public boolean contains(@Nullable Object object) {
    return object != null && super.contains(object);
  }

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public final boolean add(E e) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public final boolean remove(Object object) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public final boolean addAll(Collection<? extends E> newElements) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public final boolean removeAll(Collection<?> oldElements) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  public final boolean retainAll(Collection<?> elementsToKeep) {
    throw new UnsupportedOperationException();
  }

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @Deprecated
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
        return new RegularImmutableAsList<E>(this, toArray());
    }
  }

  /**
   * Returns {@code true} if this immutable collection's implementation contains references to
   * user-created objects that aren't accessible via this collection's methods. This is generally
   * used to determine whether {@code copyOf} implementations should make an explicit copy to avoid
   * memory leaks.
   */
  abstract boolean isPartialView();
  
  /**
   * Copies the contents of this immutable collection into the specified array at the specified
   * offset.  Returns {@code offset + size()}.
   */
  int copyIntoArray(Object[] dst, int offset) {
    for (E e : this) {
      dst[offset++] = e;
    }
    return offset;
  }

  Object writeReplace() {
    // We serialize by default to ImmutableList, the simplest thing that works.
    return new ImmutableList.SerializedForm(toArray());
  }

  /**
   * Abstract base class for builders of {@link ImmutableCollection} types.
   *
   * @since 10.0
   */
  public abstract static class Builder<E> {
    static final int DEFAULT_INITIAL_CAPACITY = 4;

    static int expandedCapacity(int oldCapacity, int minCapacity) {
      if (minCapacity < 0) {
        throw new AssertionError("cannot store more than MAX_VALUE elements");
      }
      // careful of overflow!
      int newCapacity = oldCapacity + (oldCapacity >> 1) + 1;
      if (newCapacity < minCapacity) {
        newCapacity = Integer.highestOneBit(minCapacity - 1) << 1;
      }
      if (newCapacity < 0) {
        newCapacity = Integer.MAX_VALUE;
        // guaranteed to be >= newCapacity
      }
      return newCapacity;
    }

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
  
  abstract static class ArrayBasedBuilder<E> extends ImmutableCollection.Builder<E> {
    Object[] contents;
    int size;
    
    ArrayBasedBuilder(int initialCapacity) {
      checkNonnegative(initialCapacity, "initialCapacity");
      this.contents = new Object[initialCapacity];
      this.size = 0;
    }
    
    /**
     * Expand the absolute capacity of the builder so it can accept at least
     * the specified number of elements without being resized.
     */
    private void ensureCapacity(int minCapacity) {
      if (contents.length < minCapacity) {
        this.contents = ObjectArrays.arraysCopyOf(
            this.contents, expandedCapacity(contents.length, minCapacity));
      }
    }

    @Override
    public ArrayBasedBuilder<E> add(E element) {
      checkNotNull(element);
      ensureCapacity(size + 1);
      contents[size++] = element;
      return this;
    }

    @Override
    public Builder<E> add(E... elements) {
      checkElementsNotNull(elements);
      ensureCapacity(size + elements.length);
      System.arraycopy(elements, 0, contents, size, elements.length);
      size += elements.length;
      return this;
    }

    @Override
    public Builder<E> addAll(Iterable<? extends E> elements) {
      if (elements instanceof Collection) {
        Collection<?> collection = (Collection<?>) elements;
        ensureCapacity(size + collection.size());
      }
      super.addAll(elements);
      return this;
    }
  }
}

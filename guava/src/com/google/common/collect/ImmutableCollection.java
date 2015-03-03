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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A subtype of {@link Collection} making additional guarantees: its contents will never change, it
 * will never contain {@code null}, and its iteration order is deterministic.
 *
 * <p><b>Note:</b> {@code ImmutableCollection} itself exists primarily as a common supertype for
 * more useful types like {@link ImmutableSet} and {@link ImmutableList}. Like {@code Collection},
 * it has no defined {@link #equals} behavior, which can lead to surprises and bugs, so (like {@code
 * Collection}) it should not be used directly.
 *
 * <p>Example usage: <pre>   {@code
 *
 *   class Foo {
 *     private static final ImmutableSet<String> RESERVED_CODES =
 *         ImmutableSet.of("AZ", "CQ", "ZX");
 *
 *     private final ImmutableSet<String> codes;
 *
 *     public Foo(Iterable<String> codes) {
 *       this.codes = ImmutableSet.copyOf(codes);
 *       checkArgument(Collections.disjoint(this.codes, RESERVED_CODES));
 *     }
 *   }}</pre>
 *
 * <h3>About <i>all</i> public {@code Immutable-} types in this package</h3>
 *
 * <h4>Guarantees</h4>
 *
 * <p>Each makes the following guarantees:
 *
 * <ul>
 * <li>Its contents can never change. Any attempt to add, remove or replace an element results in an
 *     {@link UnsupportedOperationException}. Note that this guarantee of <i>immutability</i> is
 *     stronger than that of {@link Collections#unmodifiableCollection}, which only prevents
 *     modification operations from being invoked on the reference it returns, while any other code
 *     having a reference to the inner collection can still modify it at will.
 * <li>It can never contain {@code null} as an element, key or value. An attempt to do so results in
 *     a {@link NullPointerException}.
 * <li>Its iteration order is deterministic. What that order is, specifically, depends on how the
 *     collection was created. See the appropriate factory method for details.
 * <li>It cannot be subclassed outside this package (which would permit these guarantees to be
 *     violated).
 * <li>It is thread-safe.
 * </ul>
 *
 * <h4>Types, not implementations</h4>
 *
 * <p>Each of these public classes, such as {@code ImmutableList}, is a <i>type</i>, not a
 * specific <i>implementation</i> (unlike the case of, say, {@link ArrayList}). That is, they should
 * be thought of as interfaces in virtually every important sense, just ones that classes outside
 * this package can't implement.
 *
 * <p>For your field types and method return types, use the immutable type (like {@code
 * ImmutableList}) instead of the corresponding basic collection interface type (like {@link List})
 * unless the semantic guarantees listed above are not considered relevant. On the other hand, a
 * <i>parameter</i> type of {@code ImmutableList} can be a nuisance to callers; instead, accept
 * {@link List} (or even {@link Iterable}) and pass it to {@link ImmutableList#copyOf(Collection)}
 * yourself.
 *
 * <h4>Creation</h4>
 *
 * <p>With the exception of {@code ImmutableCollection} itself, each {@code Immutable} type provides
 * the static operations you need to obtain instances of that type:
 *
 * <ul>
 * <li>Static methods named {@code of} accepting an explicit list of elements or entries
 * <li>Static methods named {@code copyOf} accepting an existing collection (or similar) whose
 *     contents should be copied
 * <li>A static nested {@code Builder} class which can be used to progressively populate a new
 *     immutable instance
 * </ul>
 *
 * <h4>Other common properties</h4>
 *
 * <ul>
 * <li>View collections, such as {@link ImmutableMap#keySet} or {@link ImmutableList#subList},
 *     return the appropriate {@code Immutable} type. This is true even when the language does not
 *     permit the method's return type to express it (for example in the case of {@link
 *     ImmutableListMultimap#asMap}).
 *
 * <h4>Performance notes</h4>
 *
 * <ul>
 * <li>When a {@code copyOf} method is passed a collection that is already immutable, in most cases
 *     it can return quickly without actually copying anything. This means that making defensive
 *     copies at API boundaries as a habit is not necessarily expensive in the long run.
 * <li>Implementations can be generally assumed to prioritize memory efficiency and speed of access
 *     over speed of creation.
 * <li>The performance of using the associated {@code Builder} class can generally be assumed to be
 *     no worse, and possibly better, than creating a mutable collection and copying it.
 * <li>Implementations generally do not cache hash codes. If your key type has a slow {@code
 *     hashCode} implementation, it should cache it itself.
 * </ul>
 *
 * <h4>Notable subtypes (not exhaustive)</h4>
 *
 * <ul>
 * <li>{@code ImmutableCollection}
 *     <ul>
 *     <li>{@link ImmutableSet}
 *         <ul>
 *         <li>{@link ImmutableSortedSet}
 *         </ul>
 *     <li>{@link ImmutableList}
 *     <li>{@link ImmutableMultiset}
 *     </ul>
 * <li>{@link ImmutableMap}
 *     <ul>
 *     <li>{@link ImmutableSortedMap}
 *     <li>{@link ImmutableBiMap}
 *     </ul>
 * <li>{@link ImmutableMultimap}
 *     <ul>
 *     <li>{@link ImmutableListMultimap}
 *     <li>{@link ImmutableSetMultimap}
 *     </ul>
 * <li>{@link ImmutableTable}
 * <li>{@link ImmutableRangeSet}
 * <li>{@link ImmutableRangeMap}
 * </ul>
 *
 * <h3>See also</h3>
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/ImmutableCollectionsExplained">
 * immutable collections</a>.
 *
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
// TODO(kevinb): I think we should push everything down to "BaseImmutableCollection" or something,
// just to do everything we can to emphasize the "practically an interface" nature of this class.
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
    Object[] result = new Object[size];
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
  public abstract boolean contains(@Nullable Object object);

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
   * Returns an {@code ImmutableList} containing the same elements, in the same order, as this
   * collection.
   *
   * <p><b>Performance note:</b> in most cases this method can return quickly without actually
   * copying anything. The exact circumstances under which the copy is performed are undefined and
   * subject to change.
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

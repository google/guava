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

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotMock;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link Collection} whose contents will never change, and which offers a few additional
 * guarantees detailed below.
 *
 * <p><b>Warning:</b> avoid <i>direct</i> usage of {@link ImmutableCollection} as a type (just as
 * with {@link Collection} itself). Prefer subtypes such as {@link ImmutableSet} or {@link
 * ImmutableList}, which have well-defined {@link #equals} semantics, thus avoiding a common source
 * of bugs and confusion.
 *
 * <h3>About <i>all</i> {@code Immutable-} collections</h3>
 *
 * <p>The remainder of this documentation applies to every public {@code Immutable-} type in this
 * package, whether it is a subtype of {@code ImmutableCollection} or not.
 *
 * <h4>Guarantees</h4>
 *
 * <p>Each makes the following guarantees:
 *
 * <ul>
 *   <li><b>Shallow immutability.</b> Elements can never be added, removed or replaced in this
 *       collection. This is a stronger guarantee than that of {@link
 *       Collections#unmodifiableCollection}, whose contents change whenever the wrapped collection
 *       is modified.
 *   <li><b>Null-hostility.</b> This collection will never contain a null element.
 *   <li><b>Deterministic iteration.</b> The iteration order is always well-defined, depending on
 *       how the collection was created. Typically this is insertion order unless an explicit
 *       ordering is otherwise specified (e.g. {@link ImmutableSortedSet#naturalOrder}). See the
 *       appropriate factory method for details. View collections such as {@link
 *       ImmutableMultiset#elementSet} iterate in the same order as the parent, except as noted.
 *   <li><b>Thread safety.</b> It is safe to access this collection concurrently from multiple
 *       threads.
 *   <li><b>Integrity.</b> This type cannot be subclassed outside this package (which would allow
 *       these guarantees to be violated).
 * </ul>
 *
 * <h4>"Interfaces", not implementations</h4>
 *
 * <p>These are classes instead of interfaces to prevent external subtyping, but should be thought
 * of as interfaces in every important sense. Each public class such as {@link ImmutableSet} is a
 * <i>type</i> offering meaningful behavioral guarantees. This is substantially different from the
 * case of (say) {@link HashSet}, which is an <i>implementation</i>, with semantics that were
 * largely defined by its supertype.
 *
 * <p>For field types and method return types, you should generally use the immutable type (such as
 * {@link ImmutableList}) instead of the general collection interface type (such as {@link List}).
 * This communicates to your callers all of the semantic guarantees listed above, which is almost
 * always very useful information.
 *
 * <p>On the other hand, a <i>parameter</i> type of {@link ImmutableList} is generally a nuisance to
 * callers. Instead, accept {@link Iterable} and have your method or constructor body pass it to the
 * appropriate {@code copyOf} method itself.
 *
 * <p>Expressing the immutability guarantee directly in the type that user code references is a
 * powerful advantage. Although Java offers certain immutable collection factory methods, such as
 * {@link Collections#singleton(Object)} and <a
 * href="https://docs.oracle.com/javase/9/docs/api/java/util/Set.html#immutable">{@code Set.of}</a>,
 * we recommend using <i>these</i> classes instead for this reason (as well as for consistency).
 *
 * <h4>Creation</h4>
 *
 * <p>Except for logically "abstract" types like {@code ImmutableCollection} itself, each {@code
 * Immutable} type provides the static operations you need to obtain instances of that type. These
 * usually include:
 *
 * <ul>
 *   <li>Static methods named {@code of}, accepting an explicit list of elements or entries.
 *   <li>Static methods named {@code copyOf} (or {@code copyOfSorted}), accepting an existing
 *       collection whose contents should be copied.
 *   <li>A static nested {@code Builder} class which can be used to populate a new immutable
 *       instance.
 * </ul>
 *
 * <h4>Warnings</h4>
 *
 * <ul>
 *   <li><b>Warning:</b> as with any collection, it is almost always a bad idea to modify an element
 *       (in a way that affects its {@link Object#equals} behavior) while it is contained in a
 *       collection. Undefined behavior and bugs will result. It's generally best to avoid using
 *       mutable objects as elements at all, as many users may expect your "immutable" object to be
 *       <i>deeply</i> immutable.
 * </ul>
 *
 * <h4>Performance notes</h4>
 *
 * <ul>
 *   <li>Implementations can be generally assumed to prioritize memory efficiency, then speed of
 *       access, and lastly speed of creation.
 *   <li>The {@code copyOf} methods will sometimes recognize that the actual copy operation is
 *       unnecessary; for example, {@code copyOf(copyOf(anArrayList))} should copy the data only
 *       once. This reduces the expense of habitually making defensive copies at API boundaries.
 *       However, the precise conditions for skipping the copy operation are undefined.
 *   <li><b>Warning:</b> a view collection such as {@link ImmutableMap#keySet} or {@link
 *       ImmutableList#subList} may retain a reference to the entire data set, preventing it from
 *       being garbage collected. If some of the data is no longer reachable through other means,
 *       this constitutes a memory leak. Pass the view collection to the appropriate {@code copyOf}
 *       method to obtain a correctly-sized copy.
 *   <li>The performance of using the associated {@code Builder} class can be assumed to be no
 *       worse, and possibly better, than creating a mutable collection and copying it.
 *   <li>Implementations generally do not cache hash codes. If your element or key type has a slow
 *       {@code hashCode} implementation, it should cache it itself.
 * </ul>
 *
 * <h4>Example usage</h4>
 *
 * <pre>{@code
 * class Foo {
 *   private static final ImmutableSet<String> RESERVED_CODES =
 *       ImmutableSet.of("AZ", "CQ", "ZX");
 *
 *   private final ImmutableSet<String> codes;
 *
 *   public Foo(Iterable<String> codes) {
 *     this.codes = ImmutableSet.copyOf(codes);
 *     checkArgument(Collections.disjoint(this.codes, RESERVED_CODES));
 *   }
 * }
 * }</pre>
 *
 * <h3>See also</h3>
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained"> immutable collections</a>.
 *
 * @since 2.0
 */
@DoNotMock("Use ImmutableList.of or another implementation")
@GwtCompatible(emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
// TODO(kevinb): I think we should push everything down to "BaseImmutableCollection" or something,
// just to do everything we can to emphasize the "practically an interface" nature of this class.
public abstract class ImmutableCollection<E> extends AbstractCollection<E> implements Serializable {
  /*
   * We expect SIZED (and SUBSIZED, if applicable) to be added by the spliterator factory methods.
   * These are properties of the collection as a whole; SIZED and SUBSIZED are more properties of
   * the spliterator implementation.
   */
  static final int SPLITERATOR_CHARACTERISTICS =
      Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED;

  ImmutableCollection() {}

  /** Returns an unmodifiable iterator across the elements in this collection. */
  @Override
  public abstract UnmodifiableIterator<E> iterator();

  @Override
  public Spliterator<E> spliterator() {
    return Spliterators.spliterator(this, SPLITERATOR_CHARACTERISTICS);
  }

  private static final Object[] EMPTY_ARRAY = {};

  @Override
  public final Object[] toArray() {
    return toArray(EMPTY_ARRAY);
  }

  @CanIgnoreReturnValue
  @Override
  public final <T> T[] toArray(T[] other) {
    checkNotNull(other);
    int size = size();

    if (other.length < size) {
      Object[] internal = internalArray();
      if (internal != null) {
        return Platform.copy(internal, internalArrayStart(), internalArrayEnd(), other);
      }
      other = ObjectArrays.newArray(other, size);
    } else if (other.length > size) {
      other[size] = null;
    }
    copyIntoArray(other, 0);
    return other;
  }

  /** If this collection is backed by an array of its elements in insertion order, returns it. */
  @Nullable
  Object[] internalArray() {
    return null;
  }

  /**
   * If this collection is backed by an array of its elements in insertion order, returns the offset
   * where this collection's elements start.
   */
  int internalArrayStart() {
    throw new UnsupportedOperationException();
  }

  /**
   * If this collection is backed by an array of its elements in insertion order, returns the offset
   * where this collection's elements end.
   */
  int internalArrayEnd() {
    throw new UnsupportedOperationException();
  }

  @Override
  public abstract boolean contains(@Nullable Object object);

  /**
   * Guaranteed to throw an exception and leave the collection unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
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
  @CanIgnoreReturnValue
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
  @CanIgnoreReturnValue
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
  @CanIgnoreReturnValue
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
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  public final boolean removeIf(Predicate<? super E> filter) {
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
   * offset. Returns {@code offset + size()}.
   */
  @CanIgnoreReturnValue
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
  @DoNotMock
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

    Builder() {}

    /**
     * Adds {@code element} to the {@code ImmutableCollection} being built.
     *
     * <p>Note that each builder class covariantly returns its own type from this method.
     *
     * @param element the element to add
     * @return this {@code Builder} instance
     * @throws NullPointerException if {@code element} is null
     */
    @CanIgnoreReturnValue
    public abstract Builder<E> add(E element);

    /**
     * Adds each element of {@code elements} to the {@code ImmutableCollection} being built.
     *
     * <p>Note that each builder class overrides this method in order to covariantly return its own
     * type.
     *
     * @param elements the elements to add
     * @return this {@code Builder} instance
     * @throws NullPointerException if {@code elements} is null or contains a null element
     */
    @CanIgnoreReturnValue
    public Builder<E> add(E... elements) {
      for (E element : elements) {
        add(element);
      }
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableCollection} being built.
     *
     * <p>Note that each builder class overrides this method in order to covariantly return its own
     * type.
     *
     * @param elements the elements to add
     * @return this {@code Builder} instance
     * @throws NullPointerException if {@code elements} is null or contains a null element
     */
    @CanIgnoreReturnValue
    public Builder<E> addAll(Iterable<? extends E> elements) {
      for (E element : elements) {
        add(element);
      }
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableCollection} being built.
     *
     * <p>Note that each builder class overrides this method in order to covariantly return its own
     * type.
     *
     * @param elements the elements to add
     * @return this {@code Builder} instance
     * @throws NullPointerException if {@code elements} is null or contains a null element
     */
    @CanIgnoreReturnValue
    public Builder<E> addAll(Iterator<? extends E> elements) {
      while (elements.hasNext()) {
        add(elements.next());
      }
      return this;
    }

    /**
     * Returns a newly-created {@code ImmutableCollection} of the appropriate type, containing the
     * elements provided to this builder.
     *
     * <p>Note that each builder class covariantly returns the appropriate type of {@code
     * ImmutableCollection} from this method.
     */
    public abstract ImmutableCollection<E> build();
  }
}

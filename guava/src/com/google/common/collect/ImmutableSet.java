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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ObjectArrays.checkElementNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A high-performance, immutable {@code Set} with reliable, user-specified
 * iteration order. Does not permit null elements.
 *
 * <p>Unlike {@link Collections#unmodifiableSet}, which is a <i>view</i> of a
 * separate collection that can still change, an instance of this class contains
 * its own private data and will <i>never</i> change. This class is convenient
 * for {@code public static final} sets ("constant sets") and also lets you
 * easily make a "defensive copy" of a set provided to your class by a caller.
 *
 * <p><b>Warning:</b> Like most sets, an {@code ImmutableSet} will not function
 * correctly if an element is modified after being placed in the set. For this
 * reason, and to avoid general confusion, it is strongly recommended to place
 * only immutable objects into this collection.
 *
 * <p>This class has been observed to perform significantly better than {@link
 * HashSet} for objects with very fast {@link Object#hashCode} implementations
 * (as a well-behaved immutable object should). While this class's factory
 * methods create hash-based instances, the {@link ImmutableSortedSet} subclass
 * performs binary searches instead.
 *
 * <p><b>Note:</b> Although this class is not final, it cannot be subclassed
 * outside its package as it has no public or protected constructors. Thus,
 * instances of this type are guaranteed to be immutable.
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/ImmutableCollectionsExplained">
 * immutable collections</a>.
 *
 * @see ImmutableList
 * @see ImmutableMap
 * @author Kevin Bourrillion
 * @author Nick Kralevich
 * @since 2.0 (imported from Google Collections Library)
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableSet<E> extends ImmutableCollection<E>
    implements Set<E> {
  /**
   * Returns the empty immutable set. This set behaves and performs comparably
   * to {@link Collections#emptySet}, and is preferable mainly for consistency
   * and maintainability of your code.
   */
  // Casting to any type is safe because the set will never hold any elements.
  @SuppressWarnings({"unchecked"})
  public static <E> ImmutableSet<E> of() {
    return (ImmutableSet<E>) EmptyImmutableSet.INSTANCE;
  }

  /**
   * Returns an immutable set containing a single element. This set behaves and
   * performs comparably to {@link Collections#singleton}, but will not accept
   * a null element. It is preferable mainly for consistency and
   * maintainability of your code.
   */
  public static <E> ImmutableSet<E> of(E element) {
    return new SingletonImmutableSet<E>(element);
  }

  /**
   * Returns an immutable set containing the given elements, in order. Repeated
   * occurrences of an element (according to {@link Object#equals}) after the
   * first are ignored.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableSet<E> of(E e1, E e2) {
    return construct(2, e1, e2);
  }

  /**
   * Returns an immutable set containing the given elements, in order. Repeated
   * occurrences of an element (according to {@link Object#equals}) after the
   * first are ignored.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3) {
    return construct(3, e1, e2, e3);
  }

  /**
   * Returns an immutable set containing the given elements, in order. Repeated
   * occurrences of an element (according to {@link Object#equals}) after the
   * first are ignored.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4) {
    return construct(4, e1, e2, e3, e4);
  }

  /**
   * Returns an immutable set containing the given elements, in order. Repeated
   * occurrences of an element (according to {@link Object#equals}) after the
   * first are ignored.
   *
   * @throws NullPointerException if any element is null
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5) {
    return construct(5, e1, e2, e3, e4, e5);
  }

  /**
   * Returns an immutable set containing the given elements, in order. Repeated
   * occurrences of an element (according to {@link Object#equals}) after the
   * first are ignored.
   *
   * @throws NullPointerException if any element is null
   * @since 3.0 (source-compatible since 2.0)
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6,
      E... others) {
    final int paramCount = 6;
    Object[] elements = new Object[paramCount + others.length];
    elements[0] = e1;
    elements[1] = e2;
    elements[2] = e3;
    elements[3] = e4;
    elements[4] = e5;
    elements[5] = e6;
    System.arraycopy(others, 0, elements, paramCount, others.length);
    return construct(elements.length, elements);
  }

  /**
   * Constructs an {@code ImmutableSet} from the first {@code n} elements of the specified array.
   * If {@code k} is the size of the returned {@code ImmutableSet}, then the unique elements of
   * {@code elements} will be in the first {@code k} positions, and {@code elements[i] == null} for
   * {@code k <= i < n}.
   *
   * <p>This may modify {@code elements}.  Additionally, if {@code n == elements.length} and
   * {@code elements} contains no duplicates, {@code elements} may be used without copying in the
   * returned {@code ImmutableSet}, in which case it may no longer be modified.
   *
   * <p>{@code elements} may contain only values of type {@code E}.
   *
   * @throws NullPointerException if any of the first {@code n} elements of {@code elements} is
   *          null
   */
  private static <E> ImmutableSet<E> construct(int n, Object... elements) {
    switch (n) {
      case 0:
        return of();
      case 1:
        @SuppressWarnings("unchecked") // safe; elements contains only E's
        E elem = (E) elements[0];
        return of(elem);
      default:
        // continue below to handle the general case
    }
    int tableSize = chooseTableSize(n);
    Object[] table = new Object[tableSize];
    int mask = tableSize - 1;
    int hashCode = 0;
    int uniques = 0;
    for (int i = 0; i < n; i++) {
      Object element = checkElementNotNull(elements[i], i);
      int hash = element.hashCode();
      for (int j = Hashing.smear(hash); ; j++) {
        int index = j & mask;
        Object value = table[index];
        if (value == null) {
          // Came to an empty slot. Put the element here.
          elements[uniques++] = element;
          table[index] = element;
          hashCode += hash;
          break;
        } else if (value.equals(element)) {
          break;
        }
      }
    }
    Arrays.fill(elements, uniques, n, null);
    if (uniques == 1) {
      // There is only one element or elements are all duplicates
      @SuppressWarnings("unchecked") // we are careful to only pass in E
      E element = (E) elements[0];
      return new SingletonImmutableSet<E>(element, hashCode);
    } else if (tableSize != chooseTableSize(uniques)) {
      // Resize the table when the array includes too many duplicates.
      // when this happens, we have already made a copy
      return construct(uniques, elements);
    } else {
      Object[] uniqueElements = (uniques < elements.length)
          ? ObjectArrays.arraysCopyOf(elements, uniques)
          : elements;
      return new RegularImmutableSet<E>(uniqueElements, hashCode, table, mask);
    }
  }

  // We use power-of-2 tables, and this is the highest int that's a power of 2
  static final int MAX_TABLE_SIZE = Ints.MAX_POWER_OF_TWO;

  // Represents how tightly we can pack things, as a maximum.
  private static final double DESIRED_LOAD_FACTOR = 0.7;

  // If the set has this many elements, it will "max out" the table size
  private static final int CUTOFF =
      (int) (MAX_TABLE_SIZE * DESIRED_LOAD_FACTOR);

  /**
   * Returns an array size suitable for the backing array of a hash table that
   * uses open addressing with linear probing in its implementation.  The
   * returned size is the smallest power of two that can hold setSize elements
   * with the desired load factor.
   *
   * <p>Do not call this method with setSize < 2.
   */
  @VisibleForTesting static int chooseTableSize(int setSize) {
    // Correct the size for open addressing to match desired load factor.
    if (setSize < CUTOFF) {
      // Round up to the next highest power of 2.
      int tableSize = Integer.highestOneBit(setSize - 1) << 1;
      while (tableSize * DESIRED_LOAD_FACTOR < setSize) {
        tableSize <<= 1;
      }
      return tableSize;
    }

    // The table can't be completely full or we'll get infinite reprobes
    checkArgument(setSize < MAX_TABLE_SIZE, "collection too large");
    return MAX_TABLE_SIZE;
  }

  /**
   * Returns an immutable set containing the given elements, in order. Repeated
   * occurrences of an element (according to {@link Object#equals}) after the
   * first are ignored.
   *
   * @throws NullPointerException if any of {@code elements} is null
   * @since 3.0
   */
  public static <E> ImmutableSet<E> copyOf(E[] elements) {
    switch (elements.length) {
      case 0:
        return of();
      case 1:
        return of(elements[0]);
      default:
        return construct(elements.length, elements.clone());
    }
  }

  /**
   * Returns an immutable set containing the given elements, in order. Repeated
   * occurrences of an element (according to {@link Object#equals}) after the
   * first are ignored. This method iterates over {@code elements} at most once.
   *
   * <p>Note that if {@code s} is a {@code Set<String>}, then {@code
   * ImmutableSet.copyOf(s)} returns an {@code ImmutableSet<String>} containing
   * each of the strings in {@code s}, while {@code ImmutableSet.of(s)} returns
   * a {@code ImmutableSet<Set<String>>} containing one element (the given set
   * itself).
   *
   * <p>Despite the method name, this method attempts to avoid actually copying
   * the data when it is safe to do so. The exact circumstances under which a
   * copy will or will not be performed are undocumented and subject to change.
   *
   * @throws NullPointerException if any of {@code elements} is null
   */
  public static <E> ImmutableSet<E> copyOf(Iterable<? extends E> elements) {
    return (elements instanceof Collection)
        ? copyOf((Collection<? extends E>) elements)
        : copyOf(elements.iterator());
  }

  /**
   * Returns an immutable set containing the given elements, in order. Repeated
   * occurrences of an element (according to {@link Object#equals}) after the
   * first are ignored.
   *
   * @throws NullPointerException if any of {@code elements} is null
   */
  public static <E> ImmutableSet<E> copyOf(Iterator<? extends E> elements) {
    // We special-case for 0 or 1 elements, but anything further is madness.
    if (!elements.hasNext()) {
      return of();
    }
    E first = elements.next();
    if (!elements.hasNext()) {
      return of(first);
    } else {
      return new ImmutableSet.Builder<E>()
          .add(first)
          .addAll(elements)
          .build();
    }
  }

  /**
   * Returns an immutable set containing the given elements, in order. Repeated
   * occurrences of an element (according to {@link Object#equals}) after the
   * first are ignored. This method iterates over {@code elements} at most
   * once.
   *
   * <p>Note that if {@code s} is a {@code Set<String>}, then {@code
   * ImmutableSet.copyOf(s)} returns an {@code ImmutableSet<String>} containing
   * each of the strings in {@code s}, while {@code ImmutableSet.of(s)} returns
   * a {@code ImmutableSet<Set<String>>} containing one element (the given set
   * itself).
   *
   * <p><b>Note:</b> Despite what the method name suggests, {@code copyOf} will
   * return constant-space views, rather than linear-space copies, of some
   * inputs known to be immutable. For some other immutable inputs, such as key
   * sets of an {@code ImmutableMap}, it still performs a copy in order to avoid
   * holding references to the values of the map. The heuristics used in this
   * decision are undocumented and subject to change except that:
   * <ul>
   * <li>A full copy will be done of any {@code ImmutableSortedSet}.</li>
   * <li>{@code ImmutableSet.copyOf()} is idempotent with respect to pointer
   * equality.</li>
   * </ul>
   *
   * <p>This method is safe to use even when {@code elements} is a synchronized
   * or concurrent collection that is currently being modified by another
   * thread.
   *
   * @throws NullPointerException if any of {@code elements} is null
   * @since 7.0 (source-compatible since 2.0)
   */
  public static <E> ImmutableSet<E> copyOf(Collection<? extends E> elements) {
    /*
     * TODO(user): consider checking for ImmutableAsList here
     * TODO(user): consider checking for Multiset here
     */
    if (elements instanceof ImmutableSet
        && !(elements instanceof ImmutableSortedSet)) {
      @SuppressWarnings("unchecked") // all supported methods are covariant
      ImmutableSet<E> set = (ImmutableSet<E>) elements;
      if (!set.isPartialView()) {
        return set;
      }
    } else if (elements instanceof EnumSet) {
      return copyOfEnumSet((EnumSet) elements);
    }
    Object[] array = elements.toArray();
    return construct(array.length, array);
  }

  private static <E extends Enum<E>> ImmutableSet<E> copyOfEnumSet(
      EnumSet<E> enumSet) {
    return ImmutableEnumSet.asImmutable(EnumSet.copyOf(enumSet));
  }

  ImmutableSet() {}

  /** Returns {@code true} if the {@code hashCode()} method runs quickly. */
  boolean isHashCodeFast() {
    return false;
  }

  @Override public boolean equals(@Nullable Object object) {
    if (object == this) {
      return true;
    } else if (object instanceof ImmutableSet
        && isHashCodeFast()
        && ((ImmutableSet<?>) object).isHashCodeFast()
        && hashCode() != object.hashCode()) {
      return false;
    }
    return Sets.equalsImpl(this, object);
  }

  @Override public int hashCode() {
    return Sets.hashCodeImpl(this);
  }

  // This declaration is needed to make Set.iterator() and
  // ImmutableCollection.iterator() consistent.
  @Override public abstract UnmodifiableIterator<E> iterator();

  /*
   * This class is used to serialize all ImmutableSet instances, except for
   * ImmutableEnumSet/ImmutableSortedSet, regardless of implementation type. It
   * captures their "logical contents" and they are reconstructed using public
   * static factories. This is necessary to ensure that the existence of a
   * particular implementation type is an implementation detail.
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
   * A builder for creating immutable set instances, especially {@code public
   * static final} sets ("constant sets"). Example: <pre>   {@code
   *
   *   public static final ImmutableSet<Color> GOOGLE_COLORS =
   *       new ImmutableSet.Builder<Color>()
   *           .addAll(WEBSAFE_COLORS)
   *           .add(new Color(0, 191, 255))
   *           .build();}</pre>
   *
   * <p>Builder instances can be reused; it is safe to call {@link #build} multiple
   * times to build multiple sets in series. Each set is a superset of the set
   * created before it.
   *
   * @since 2.0 (imported from Google Collections Library)
   */
  public static class Builder<E> extends ImmutableCollection.ArrayBasedBuilder<E> {

    /**
     * Creates a new builder. The returned builder is equivalent to the builder
     * generated by {@link ImmutableSet#builder}.
     */
    public Builder() {
      this(DEFAULT_INITIAL_CAPACITY);
    }

    Builder(int capacity) {
      super(capacity);
    }

    /**
     * Adds {@code element} to the {@code ImmutableSet}.  If the {@code
     * ImmutableSet} already contains {@code element}, then {@code add} has no
     * effect (only the previously added element is retained).
     *
     * @param element the element to add
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code element} is null
     */
    @Override public Builder<E> add(E element) {
      super.add(element);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSet},
     * ignoring duplicate elements (only the first duplicate element is added).
     *
     * @param elements the elements to add
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a
     *     null element
     */
    @Override public Builder<E> add(E... elements) {
      super.add(elements);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSet},
     * ignoring duplicate elements (only the first duplicate element is added).
     *
     * @param elements the {@code Iterable} to add to the {@code ImmutableSet}
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a
     *     null element
     */
    @Override public Builder<E> addAll(Iterable<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSet},
     * ignoring duplicate elements (only the first duplicate element is added).
     *
     * @param elements the elements to add to the {@code ImmutableSet}
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a
     *     null element
     */
    @Override public Builder<E> addAll(Iterator<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    /**
     * Returns a newly-created {@code ImmutableSet} based on the contents of
     * the {@code Builder}.
     */
    @Override public ImmutableSet<E> build() {
      ImmutableSet<E> result = construct(size, contents);
      // construct has the side effect of deduping contents, so we update size
      // accordingly.
      size = result.size();
      return result;
    }
  }
}

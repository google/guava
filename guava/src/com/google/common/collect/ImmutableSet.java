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
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A {@link Set} whose contents will never change, with many other important properties detailed at
 * {@link ImmutableCollection}.
 *
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {
  /**
   * Returns the empty immutable set. Preferred over {@link Collections#emptySet} for code
   * consistency, and because the return type conveys the immutability guarantee.
   */
  @SuppressWarnings({"unchecked"}) // fully variant implementation (never actually produces any Es)
  public static <E> ImmutableSet<E> of() {
    return (ImmutableSet<E>) RegularImmutableSet.EMPTY;
  }

  /**
   * Returns an immutable set containing {@code element}. Preferred over {@link
   * Collections#singleton} for code consistency, {@code null} rejection, and because the return
   * type conveys the immutability guarantee.
   */
  public static <E> ImmutableSet<E> of(E element) {
    return new SingletonImmutableSet<E>(element);
  }

  /**
   * Returns an immutable set containing the given elements, minus duplicates, in the order each was
   * first specified. That is, if multiple elements are {@linkplain Object#equals equal}, all except
   * the first are ignored.
   */
  public static <E> ImmutableSet<E> of(E e1, E e2) {
    return construct(2, e1, e2);
  }

  /**
   * Returns an immutable set containing the given elements, minus duplicates, in the order each was
   * first specified. That is, if multiple elements are {@linkplain Object#equals equal}, all except
   * the first are ignored.
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3) {
    return construct(3, e1, e2, e3);
  }

  /**
   * Returns an immutable set containing the given elements, minus duplicates, in the order each was
   * first specified. That is, if multiple elements are {@linkplain Object#equals equal}, all except
   * the first are ignored.
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4) {
    return construct(4, e1, e2, e3, e4);
  }

  /**
   * Returns an immutable set containing the given elements, minus duplicates, in the order each was
   * first specified. That is, if multiple elements are {@linkplain Object#equals equal}, all except
   * the first are ignored.
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5) {
    return construct(5, e1, e2, e3, e4, e5);
  }

  /**
   * Returns an immutable set containing the given elements, minus duplicates, in the order each was
   * first specified. That is, if multiple elements are {@linkplain Object#equals equal}, all except
   * the first are ignored.
   *
   * @since 3.0 (source-compatible since 2.0)
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E... others) {
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
      Object[] uniqueElements =
          (uniques < elements.length)
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
  private static final int CUTOFF = (int) (MAX_TABLE_SIZE * DESIRED_LOAD_FACTOR);

  /**
   * Returns an array size suitable for the backing array of a hash table that
   * uses open addressing with linear probing in its implementation.  The
   * returned size is the smallest power of two that can hold setSize elements
   * with the desired load factor.
   *
   * <p>Do not call this method with setSize < 2.
   */
  @VisibleForTesting
  static int chooseTableSize(int setSize) {
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
   * Returns an immutable set containing each of {@code elements}, minus duplicates, in the order
   * each appears first in the source collection.
   *
   * <p><b>Performance note:</b> This method will sometimes recognize that the actual copy operation
   * is unnecessary; for example, {@code copyOf(copyOf(anArrayList))} will copy the data only once.
   * This reduces the expense of habitually making defensive copies at API boundaries. However, the
   * the precise conditions for skipping the copy operation are undefined.
   *
   * @throws NullPointerException if any of {@code elements} is null
   * @since 7.0 (source-compatible since 2.0)
   */
  public static <E> ImmutableSet<E> copyOf(Collection<? extends E> elements) {
    /*
     * TODO(lowasser): consider checking for ImmutableAsList here
     * TODO(lowasser): consider checking for Multiset here
     */
    if (elements instanceof ImmutableSet && !(elements instanceof ImmutableSortedSet)) {
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

  /**
   * Returns an immutable set containing each of {@code elements}, minus duplicates, in the order
   * each appears first in the source iterable. This method iterates over {@code elements} only
   * once.
   *
   * <p><b>Performance note:</b> This method will sometimes recognize that the actual copy operation
   * is unnecessary; for example, {@code copyOf(copyOf(anArrayList))} should copy the data only
   * once. This reduces the expense of habitually making defensive copies at API boundaries.
   * However, the precise conditions for skipping the copy operation are undefined.
   *
   * @throws NullPointerException if any of {@code elements} is null
   */
  public static <E> ImmutableSet<E> copyOf(Iterable<? extends E> elements) {
    return (elements instanceof Collection)
        ? copyOf((Collection<? extends E>) elements)
        : copyOf(elements.iterator());
  }

  /**
   * Returns an immutable set containing each of {@code elements}, minus duplicates, in the order
   * each appears first in the source iterator.
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
   * Returns an immutable set containing each of {@code elements}, minus duplicates, in the order
   * each appears first in the source array.
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

  @SuppressWarnings("rawtypes") // necessary to compile against Java 8
  private static ImmutableSet copyOfEnumSet(EnumSet enumSet) {
    return ImmutableEnumSet.asImmutable(EnumSet.copyOf(enumSet));
  }

  ImmutableSet() {}

  /** Returns {@code true} if the {@code hashCode()} method runs quickly. */
  boolean isHashCodeFast() {
    return false;
  }

  @Override
  public boolean equals(@Nullable Object object) {
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

  @Override
  public int hashCode() {
    return Sets.hashCodeImpl(this);
  }

  // This declaration is needed to make Set.iterator() and
  // ImmutableCollection.iterator() consistent.
  @Override
  public abstract UnmodifiableIterator<E> iterator();

  abstract static class Indexed<E> extends ImmutableSet<E> {
    abstract E get(int index);

    @Override
    public UnmodifiableIterator<E> iterator() {
      return asList().iterator();
    }

    @Override
    ImmutableList<E> createAsList() {
      return new ImmutableAsList<E>() {
        @Override
        public E get(int index) {
          return Indexed.this.get(index);
        }

        @Override
        Indexed<E> delegateCollection() {
          return Indexed.this;
        }
      };
    }
  }

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
   * A builder for creating {@code ImmutableSet} instances. Example: <pre>   {@code
   *
   *   static final ImmutableSet<Color> GOOGLE_COLORS =
   *       ImmutableSet.<Color>builder()
   *           .addAll(WEBSAFE_COLORS)
   *           .add(new Color(0, 191, 255))
   *           .build();}</pre>
   *
   * <p>Building does not change the state of the builder, so it is still possible to add more
   * elements and to build again.
   *
   * @since 2.0
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
    @Override
    public Builder<E> add(E element) {
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
    @Override
    public Builder<E> add(E... elements) {
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
    @Override
    public Builder<E> addAll(Iterable<? extends E> elements) {
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
    @Override
    public Builder<E> addAll(Iterator<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    /**
     * Returns a newly-created {@code ImmutableSet} based on the contents of
     * the {@code Builder}.
     */
    @Override
    public ImmutableSet<E> build() {
      ImmutableSet<E> result = construct(size, contents);
      // construct has the side effect of deduping contents, so we update size
      // accordingly.
      size = result.size();
      return result;
    }
  }
}

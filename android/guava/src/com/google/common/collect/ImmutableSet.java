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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.ObjectArrays.checkElementNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedWith;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

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
   * <p>The array {@code others} must not be longer than {@code Integer.MAX_VALUE - 6}.
   *
   * @since 3.0 (source-compatible since 2.0)
   */
  @SafeVarargs // For Eclipse. For internal javac we have disabled this pointless type of warning.
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E... others) {
    checkArgument(
        others.length <= Integer.MAX_VALUE - 6, "the total number of elements must fit in an int");
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
   * Constructs an {@code ImmutableSet} from the first {@code n} elements of the specified array. If
   * {@code k} is the size of the returned {@code ImmutableSet}, then the unique elements of {@code
   * elements} will be in the first {@code k} positions, and {@code elements[i] == null} for {@code
   * k <= i < n}.
   *
   * <p>After this method returns, {@code elements} will contain no duplicates, but {@code elements}
   * may be the real array backing the returned set, so do not modify it further.
   *
   * <p>{@code elements} may contain only values of type {@code E}.
   *
   * @throws NullPointerException if any of the first {@code n} elements of {@code elements} is null
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
    } else if (chooseTableSize(uniques) < tableSize / 2) {
      // Resize the table when the array includes too many duplicates.
      return construct(uniques, elements);
    } else {
      Object[] uniqueElements =
          shouldTrim(uniques, elements.length) ? Arrays.copyOf(elements, uniques) : elements;
      return new RegularImmutableSet<E>(uniqueElements, hashCode, table, mask, uniques);
    }
  }

  private static boolean shouldTrim(int actualUnique, int expectedUnique) {
    return actualUnique < (expectedUnique >> 1) + (expectedUnique >> 2);
  }

  // We use power-of-2 tables, and this is the highest int that's a power of 2
  static final int MAX_TABLE_SIZE = Ints.MAX_POWER_OF_TWO;

  // Represents how tightly we can pack things, as a maximum.
  private static final double DESIRED_LOAD_FACTOR = 0.7;

  // If the set has this many elements, it will "max out" the table size
  private static final int CUTOFF = (int) (MAX_TABLE_SIZE * DESIRED_LOAD_FACTOR);

  /**
   * Returns an array size suitable for the backing array of a hash table that uses open addressing
   * with linear probing in its implementation. The returned size is the smallest power of two that
   * can hold setSize elements with the desired load factor. Always returns at least setSize + 2.
   */
  @VisibleForTesting
  static int chooseTableSize(int setSize) {
    setSize = Math.max(setSize, 2);
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
   * precise conditions for skipping the copy operation are undefined.
   *
   * @throws NullPointerException if any of {@code elements} is null
   * @since 7.0 (source-compatible since 2.0)
   */
  public static <E> ImmutableSet<E> copyOf(Collection<? extends E> elements) {
    /*
     * TODO(lowasser): consider checking for ImmutableAsList here
     * TODO(lowasser): consider checking for Multiset here
     */
    // Don't refer to ImmutableSortedSet by name so it won't pull in all that code
    if (elements instanceof ImmutableSet && !(elements instanceof SortedSet)) {
      @SuppressWarnings("unchecked") // all supported methods are covariant
      ImmutableSet<E> set = (ImmutableSet<E>) elements;
      if (!set.isPartialView()) {
        return set;
      }
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
      return new ImmutableSet.Builder<E>().add(first).addAll(elements).build();
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

  ImmutableSet() {}

  /** Returns {@code true} if the {@code hashCode()} method runs quickly. */
  boolean isHashCodeFast() {
    return false;
  }

  @Override
  public boolean equals(@NullableDecl Object object) {
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

  @LazyInit @RetainedWith @NullableDecl private transient ImmutableList<E> asList;

  @Override
  public ImmutableList<E> asList() {
    ImmutableList<E> result = asList;
    return (result == null) ? asList = createAsList() : result;
  }

  ImmutableList<E> createAsList() {
    return ImmutableList.asImmutableList(toArray());
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
   * Returns a new builder. The generated builder is equivalent to the builder created by the {@link
   * Builder} constructor.
   */
  public static <E> Builder<E> builder() {
    return new Builder<E>();
  }

  /**
   * Returns a new builder, expecting the specified number of distinct elements to be added.
   *
   * <p>If {@code expectedSize} is exactly the number of distinct elements added to the builder
   * before {@link Builder#build} is called, the builder is likely to perform better than an unsized
   * {@link #builder()} would have.
   *
   * <p>It is not specified if any performance benefits apply if {@code expectedSize} is close to,
   * but not exactly, the number of distinct elements added to the builder.
   *
   * @since 23.1
   */
  @Beta
  public static <E> Builder<E> builderWithExpectedSize(int expectedSize) {
    checkNonnegative(expectedSize, "expectedSize");
    return new Builder<E>(expectedSize);
  }

  /**
   * A builder for creating {@code ImmutableSet} instances. Example:
   *
   * <pre>{@code
   * static final ImmutableSet<Color> GOOGLE_COLORS =
   *     ImmutableSet.<Color>builder()
   *         .addAll(WEBSAFE_COLORS)
   *         .add(new Color(0, 191, 255))
   *         .build();
   * }</pre>
   *
   * <p>Elements appear in the resulting set in the same order they were first added to the builder.
   *
   * <p>Building does not change the state of the builder, so it is still possible to add more
   * elements and to build again.
   *
   * @since 2.0
   */
  public static class Builder<E> extends ImmutableCollection.ArrayBasedBuilder<E> {
    @NullableDecl @VisibleForTesting Object[] hashTable;
    private int hashCode;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder generated by {@link
     * ImmutableSet#builder}.
     */
    public Builder() {
      super(DEFAULT_INITIAL_CAPACITY);
    }

    Builder(int capacity) {
      super(capacity);
      this.hashTable = new Object[chooseTableSize(capacity)];
    }

    /**
     * Adds {@code element} to the {@code ImmutableSet}. If the {@code ImmutableSet} already
     * contains {@code element}, then {@code add} has no effect (only the previously added element
     * is retained).
     *
     * @param element the element to add
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code element} is null
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<E> add(E element) {
      checkNotNull(element);
      if (hashTable != null && chooseTableSize(size) <= hashTable.length) {
        addDeduping(element);
        return this;
      } else {
        hashTable = null;
        super.add(element);
        return this;
      }
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSet}, ignoring duplicate
     * elements (only the first duplicate element is added).
     *
     * @param elements the elements to add
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a null element
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<E> add(E... elements) {
      if (hashTable != null) {
        for (E e : elements) {
          add(e);
        }
      } else {
        super.add(elements);
      }
      return this;
    }

    private void addDeduping(E element) {
      int mask = hashTable.length - 1;
      int hash = element.hashCode();
      for (int i = Hashing.smear(hash); ; i++) {
        i &= mask;
        Object previous = hashTable[i];
        if (previous == null) {
          hashTable[i] = element;
          hashCode += hash;
          super.add(element);
          return;
        } else if (previous.equals(element)) {
          return;
        }
      }
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSet}, ignoring duplicate
     * elements (only the first duplicate element is added).
     *
     * @param elements the {@code Iterable} to add to the {@code ImmutableSet}
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a null element
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<E> addAll(Iterable<? extends E> elements) {
      checkNotNull(elements);
      if (hashTable != null) {
        for (E e : elements) {
          add(e);
        }
      } else {
        super.addAll(elements);
      }
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSet}, ignoring duplicate
     * elements (only the first duplicate element is added).
     *
     * @param elements the elements to add to the {@code ImmutableSet}
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a null element
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<E> addAll(Iterator<? extends E> elements) {
      checkNotNull(elements);
      while (elements.hasNext()) {
        add(elements.next());
      }
      return this;
    }

    /**
     * Returns a newly-created {@code ImmutableSet} based on the contents of the {@code Builder}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public ImmutableSet<E> build() {
      switch (size) {
        case 0:
          return of();
        case 1:
          return (ImmutableSet<E>) of(contents[0]);
        default:
          ImmutableSet<E> result;
          if (hashTable != null && chooseTableSize(size) == hashTable.length) {
            Object[] uniqueElements =
                shouldTrim(size, contents.length) ? Arrays.copyOf(contents, size) : contents;
            result =
                new RegularImmutableSet<E>(
                    uniqueElements, hashCode, hashTable, hashTable.length - 1, size);
          } else {
            result = construct(size, contents);
            // construct has the side effect of deduping contents, so we update size
            // accordingly.
            size = result.size();
          }
          forceCopy = true;
          hashTable = null;
          return result;
      }
    }
  }
}

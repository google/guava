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
import static com.google.common.collect.Sets.equalsImpl;
import static com.google.common.collect.Sets.hashCodeImpl;
import static com.google.common.collect.Sets.newHashSetWithExpectedSize;
import static com.google.common.math.IntMath.sqrt;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedWith;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Set} whose contents will never change, with many other important properties detailed at
 * {@link ImmutableCollection}.
 *
 * @since 2.0
 */
@GwtCompatible
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {

  /**
   * Returns the empty immutable set. Preferred over {@link Collections#emptySet} for code
   * consistency, and because the return type conveys the immutability guarantee.
   *
   * <p><b>Performance note:</b> the instance returned is a singleton.
   */
  @SuppressWarnings({"unchecked"}) // fully variant implementation (never actually produces any Es)
  public static <E> ImmutableSet<E> of() {
    return (ImmutableSet<E>) RegularImmutableSet.EMPTY;
  }

  /**
   * Returns an immutable set containing the given element. Preferred over {@link
   * Collections#singleton} for code consistency, {@code null} rejection, and because the return
   * type conveys the immutability guarantee.
   */
  public static <E> ImmutableSet<E> of(E e1) {
    return new SingletonImmutableSet<>(e1);
  }

  /*
   * TODO(b/315526394): Skip the Builder entirely for the of(...) methods, since we don't need to
   * worry that we might trigger the fallback to the JDK-backed implementation? (The varargs one
   * _could_, so we could keep it as it is. Or we could convince ourselves that hash flooding is
   * unlikely in practice there, too.)
   */

  /**
   * Returns an immutable set containing the given elements, minus duplicates, in the order each was
   * first specified. That is, if multiple elements are {@linkplain Object#equals equal}, all except
   * the first are ignored.
   */
  public static <E> ImmutableSet<E> of(E e1, E e2) {
    return new Builder<E>(2).add(e1).add(e2).build();
  }

  /**
   * Returns an immutable set containing the given elements, minus duplicates, in the order each was
   * first specified. That is, if multiple elements are {@linkplain Object#equals equal}, all except
   * the first are ignored.
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3) {
    return new Builder<E>(3).add(e1).add(e2).add(e3).build();
  }

  /**
   * Returns an immutable set containing the given elements, minus duplicates, in the order each was
   * first specified. That is, if multiple elements are {@linkplain Object#equals equal}, all except
   * the first are ignored.
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4) {
    return new Builder<E>(4).add(e1).add(e2).add(e3).add(e4).build();
  }

  /**
   * Returns an immutable set containing the given elements, minus duplicates, in the order each was
   * first specified. That is, if multiple elements are {@linkplain Object#equals equal}, all except
   * the first are ignored.
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5) {
    return new Builder<E>(5).add(e1).add(e2).add(e3).add(e4).add(e5).build();
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
    Builder<E> builder = new Builder<>(6 + others.length);
    builder.add(e1).add(e2).add(e3).add(e4).add(e5).add(e6);
    for (int i = 0; i < others.length; i++) {
      builder.add(others[i]);
    }
    return builder.build();
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
     * TODO(lowasser): consider checking for Multiset here
     */
    // Don't refer to ImmutableSortedSet by name so it won't pull in all that code
    if (elements instanceof ImmutableSet && !(elements instanceof SortedSet)) {
      @SuppressWarnings("unchecked") // all supported methods are covariant
      ImmutableSet<E> set = (ImmutableSet<E>) elements;
      if (!set.isPartialView()) {
        return set;
      }
    } else if (elements instanceof ImmutableAsList) {
      ImmutableCollection<? extends E> delegate =
          ((ImmutableAsList<? extends E>) elements).delegateCollection();
      if (delegate instanceof ImmutableSet && !(delegate instanceof SortedSet)) {
        @SuppressWarnings("unchecked") // all supported methods are covariant
        ImmutableSet<E> set = (ImmutableSet<E>) delegate;
        if (!set.isPartialView()) {
          return set;
        }
      }
    } else if (elements instanceof EnumSet) {
      EnumSet<?> clone = ((EnumSet<?>) elements).clone();
      ImmutableSet<?> untypedResult = ImmutableEnumSet.asImmutable(clone);
      /*
       * The result has the same type argument we started with. We just couldn't express EnumSet<E>
       * or ImmutableEnumSet<E> along the way because our own <E> isn't <E extends Enum<E>>.
       *
       * We are also performing a safe covariant cast to change <? extends E> to <E>.
       */
      @SuppressWarnings("unchecked")
      ImmutableSet<E> result = (ImmutableSet<E>) untypedResult;
      return result;
    }

    if (elements.isEmpty()) {
      // We avoid allocating anything.
      return of();
    }
    @Nullable Object[] array = elements.toArray();
    /*
     * For a Set, we guess that it contains no duplicates. That's just a guess for purpose of
     * sizing; if the Set uses different equality semantics, it might contain duplicates according
     * to equals(), and we will deduplicate those properly, albeit at some cost in allocations.
     */
    int expectedSize =
        elements instanceof Set ? array.length : estimatedSizeForUnknownDuplication(array.length);
    return fromUntrustedArray(array, expectedSize);
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
        Builder<E> builder = new Builder<>(estimatedSizeForUnknownDuplication(elements.length));
        for (int i = 0; i < elements.length; i++) {
          builder.add(elements[i]);
        }
        return builder.build();
    }
  }

  /**
   * Builds an {@code ImmutableSet} from a {@code Collection.toArray()} result, copying into a fresh
   * exact {@code Object[]} for length {@code >= 2} so neither in-place deduplication nor the
   * returned set shares an array with a buggy or hostile collection, while avoiding intermediate
   * array resizes.
   */
  private static <E> ImmutableSet<E> fromUntrustedArray(
      @Nullable Object[] array, int expectedSize) {
    switch (array.length) {
      case 0:
        return of();
      case 1:
        @SuppressWarnings("unchecked") // elements came from Collection<? extends E>.toArray()
        E onlyElement = (E) checkNotNull(array[0]);
        return of(onlyElement);
      default:
        // Copy defensively before hashing or retaining the buffer: a buggy or hostile toArray() may
        // alias a retained or internal array (enabling in-flight or post-return mutation) or return
        // a subtype array such as String[].
        @Nullable Object[] copy = new Object[array.length];
        arraycopy(array, 0, copy, 0, array.length);
        // Safe to pass as the owned buffer: `copy` is an unaliased exact Object[], and each
        // copy[i] is checked non-null before it can be written to dedupedElements[distinct <= i].
        @SuppressWarnings("nullness")
        Object[] nonNullBuffer = copy;
        Builder<E> builder = new Builder<>(nonNullBuffer, expectedSize);
        for (int i = 0; i < copy.length; i++) {
          @SuppressWarnings("unchecked") // elements came from Collection<? extends E>.toArray()
          E elem = (E) checkNotNull(copy[i]);
          builder.add(elem);
        }
        return builder.build();
    }
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
    }
    if (object instanceof ImmutableSet
        && isHashCodeFast()
        && ((ImmutableSet<?>) object).isHashCodeFast()
        && hashCode() != object.hashCode()) {
      return false;
    }
    return equalsImpl(this, object);
  }

  @Override
  public int hashCode() {
    return hashCodeImpl(this);
  }

  // This declaration is needed to make Set.iterator() and
  // ImmutableCollection.iterator() consistent.
  @Override
  public abstract UnmodifiableIterator<E> iterator();

  abstract static class CachingAsList<E> extends ImmutableSet<E> {
    @LazyInit @RetainedWith private transient @Nullable ImmutableList<E> asList;

    @Override
    public ImmutableList<E> asList() {
      ImmutableList<E> result = asList;
      if (result == null) {
        result = asList = createAsList();
      }
      return result;
    }

    ImmutableList<E> createAsList() {
      return new RegularImmutableAsList<>(this, toArray());
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible
    @GwtIncompatible
        Object writeReplace() {
      return super.writeReplace();
    }
  }

  @LazyInit @RetainedWith private transient @Nullable ImmutableList<E> asList;

  @Override
  public ImmutableList<E> asList() {
    ImmutableList<E> result = asList;
    if (result == null) {
      result = asList = createAsList();
    }
    return result;
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
  @J2ktIncompatible // serialization
  private static final class SerializedForm implements Serializable {
    final Object[] elements;

    SerializedForm(Object[] elements) {
      this.elements = elements;
    }

    Object readResolve() {
      return copyOf(elements);
    }

    @GwtIncompatible private static final long serialVersionUID = 0;
  }

  @Override
  @J2ktIncompatible
    Object writeReplace() {
    return new SerializedForm(toArray());
  }

  @J2ktIncompatible
    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
    throw new InvalidObjectException("Use SerializedForm");
  }

  /**
   * Returns a new builder. The generated builder is equivalent to the builder created by the {@link
   * Builder} constructor.
   */
  public static <E> Builder<E> builder() {
    return new Builder<>();
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
  public static <E> Builder<E> builderWithExpectedSize(int expectedSize) {
    checkNonnegative(expectedSize, "expectedSize");
    return new Builder<>(expectedSize);
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
  // TODO(cpovirk): Move to Hashing or something, since it's used elsewhere in the Android version.
  static int chooseTableSize(int setSize) {
    setSize = max(setSize, 2);
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
   * A builder for creating {@code ImmutableSet} instances. Example:
   *
   * {@snippet :
   * static final ImmutableSet<Color> GOOGLE_COLORS =
   *     ImmutableSet.<Color>builder()
   *         .addAll(WEBSAFE_COLORS)
   *         .add(new Color(0, 191, 255))
   *         .build();
   * }
   *
   * <p>Elements appear in the resulting set in the same order they were first added to the builder.
   *
   * <p>Building does not change the state of the builder, so it is still possible to add more
   * elements and to build again.
   *
   * @since 2.0
   */
  public static class Builder<E> extends ImmutableCollection.Builder<E> {
    /*
     * State progression (stored directly in Builder to avoid wrapper-object allocations and
     * virtual dispatch on every add()):
     *   1. Empty / Singleton (distinct <= 1, dedupedElements == null, jdkDelegate == null):
     *      `firstElement` holds the sole element when distinct == 1 (allocating zero Object[]
     *      arrays for empty or 1-element builders), and `expandTableThreshold` remembers the
     *      initial capacity hint.
     *   2. Inline SWAR (2 <= distinct <= 8, table == null, jdkDelegate == null):
     *      `dedupedElements` holds distinct elements in insertion order, `inlineCtrl` packs up to 8
     *      occupied h2 control bytes, and `expandTableThreshold` retains the initial capacity hint.
     *   3. Tiered Swiss table (distinct >= 9, table != null, jdkDelegate == null):
     *      `table` stores 8-slot groups (strides 2..5 for Tiers 1..4), and `expandTableThreshold`
     *      equals min(numGroups * 7, TIER_MAX_SIZE).
     *   4. Hash-flooding fallback (jdkDelegate != null):
     *      `jdkDelegate` deduplicates via j.u.HashSet while `dedupedElements` preserves order.
     *
     * Note: ImmutableSortedSet.Builder subclasses this class via `Builder(boolean)` and overrides
     * all public/package-private methods, leaving these fields unused.
     */
    private static final int MAX_NUM_GROUPS = 1 << 28;
    private static final int MAX_SWISS_CAPACITY = 7 * MAX_NUM_GROUPS;
    private static final int MAX_GROUP_PROBES_MULTIPLIER = 4;

    private @Nullable E firstElement;
    private Object @Nullable [] dedupedElements;
    private int distinct;
    private long @Nullable [] table;
    private long inlineCtrl;
    private int expandTableThreshold;
    private int hashCode;
    private @Nullable Set<Object> jdkDelegate;
    boolean forceCopy;
    @Nullable Object @Nullable [] contents;
    int size;
    private boolean isArrayBased;

    /**
     * Creates a new builder. The returned builder is equivalent to the builder generated by {@link
     * ImmutableSet#builder}.
     */
    public Builder() {
      this(DEFAULT_INITIAL_CAPACITY);
    }

    Builder(int capacity) {
      this.expandTableThreshold = (capacity == 0) ? DEFAULT_INITIAL_CAPACITY : capacity;
    }

    Builder(int capacity, boolean isArrayBased) {
      this.isArrayBased = isArrayBased;
      this.contents = new @Nullable Object[capacity];
      this.size = 0;
    }

    /** Takes ownership of {@code ownedBuffer} for in-place deduplication in {@code copyOf}. */
    Builder(Object[] ownedBuffer, int expectedCapacity) {
      this.dedupedElements = ownedBuffer;
      this.expandTableThreshold = max(DEFAULT_INITIAL_CAPACITY, expectedCapacity);
    }

    Builder(@SuppressWarnings("unused") boolean subclass) {}

    @SuppressWarnings("nullness") // only indices < distinct are ever read, after being populated
    private static Object[] newElementBuffer(int capacity) {
      return new Object[capacity];
    }

    @VisibleForTesting
    void forceJdk() {
      if (jdkDelegate != null) {
        return;
      }
      copyIfNecessary();
      switchToJdk();
    }

    private void switchToJdk() {
      Set<Object> delegate = newHashSetWithExpectedSize(distinct);
      if (distinct == 1 && dedupedElements == null) {
        Object e0 = requireNonNull(firstElement);
        delegate.add(e0);
        dedupedElements = new Object[] {e0};
        firstElement = null;
      } else if (dedupedElements != null) {
        for (int i = 0; i < distinct; i++) {
          delegate.add(dedupedElements[i]);
        }
      } else {
        dedupedElements = newElementBuffer(max(DEFAULT_INITIAL_CAPACITY, expandTableThreshold));
      }
      this.jdkDelegate = delegate;
      this.table = null;
      this.inlineCtrl = 0L;
      this.expandTableThreshold = 0;
    }

    final void copyIfNecessary() {
      if (forceCopy) {
        copy();
        forceCopy = false;
      }
    }

    void copy() {
      if (dedupedElements != null) {
        dedupedElements = Arrays.copyOf(dedupedElements, dedupedElements.length);
      }
      if (table != null) {
        table = table.clone();
      }
      if (jdkDelegate != null) {
        Set<Object> copy = newHashSetWithExpectedSize(distinct);
        copy.addAll(jdkDelegate);
        jdkDelegate = copy;
      }
    }

    private void ensureCapacity(int minCapacity) {
      Object[] elements = requireNonNull(dedupedElements);
      if (minCapacity > elements.length) {
        dedupedElements = Arrays.copyOf(elements, expandedCapacity(elements.length, minCapacity));
      }
    }

    private void addDedupedElement(E e) {
      ensureCapacity(distinct + 1);
      requireNonNull(dedupedElements)[distinct++] = e;
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<E> add(E element) {
      checkNotNull(element);
      if (isArrayBased) {
        Object[] contents = requireNonNull(this.contents);
        if (contents.length < size + 1) {
          this.contents =
              contents = Arrays.copyOf(contents, expandedCapacity(contents.length, size + 1));
        } else if (forceCopy) {
          this.contents = contents = contents.clone();
          forceCopy = false;
        }
        contents[size++] = element;
        return this;
      }
      copyIfNecessary();
      if (jdkDelegate != null) {
        if (jdkDelegate.add(element)) {
          addDedupedElement(element);
        }
        return this;
      }
      long[] table = this.table;
      if (table == null) {
        addInline(element);
      } else if (expandTableThreshold <= SwarTables.TIER1_MAX_SIZE) {
        insertTier1(element, table);
      } else {
        insertLargeTable(element, table);
      }
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<E> add(E... elements) {
      super.add(elements);
      return this;
    }

    /**
     * Adds each element of {@code elements} to the {@code ImmutableSet}, ignoring duplicate
     * elements (only the first duplicate element is added).
     *
     * @param elements the elements to add
     * @return this {@code Builder} object
     * @throws NullPointerException if {@code elements} is null or contains a null element
     */
    @Override
    @CanIgnoreReturnValue
    public Builder<E> addAll(Iterable<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<E> addAll(Iterator<? extends E> elements) {
      super.addAll(elements);
      return this;
    }

    @CanIgnoreReturnValue
    Builder<E> combine(Builder<E> other) {
      copyIfNecessary();
      int otherDistinct = other.distinct;
      if (otherDistinct == 0) {
        return this;
      }
      if (otherDistinct == 1 && other.dedupedElements == null) {
        add(requireNonNull(other.firstElement));
        return this;
      }
      Object[] otherElements = requireNonNull(other.dedupedElements);
      for (int i = 0; i < otherDistinct; i++) {
        @SuppressWarnings("unchecked") // only E instances were added to other
        E elem = (E) otherElements[i];
        add(elem);
      }
      return this;
    }

    /**
     * Inserts {@code e} while {@code distinct <= 8}, deferring {@code Object[]} allocation until a
     * 2nd distinct element arrives and deferring Swiss-table allocation until a 9th arrives.
     */
    private void addInline(E e) {
      int n = distinct;
      if (n == 0) {
        if (dedupedElements == null) {
          firstElement = e;
        } else {
          dedupedElements[0] = e;
        }
        distinct = 1;
        return;
      }
      if (n == 1) {
        Object e0 = (firstElement != null) ? firstElement : requireNonNull(dedupedElements)[0];
        if (e0.equals(e)) {
          return;
        }
        Object[] elements = dedupedElements;
        if (elements == null) {
          elements = newElementBuffer(max(2, expandTableThreshold));
          elements[0] = e0;
          dedupedElements = elements;
          firstElement = null;
        }
        elements[1] = e;
        distinct = 2;
        int h0 = e0.hashCode();
        int h1 = e.hashCode();
        hashCode = h0 + h1;
        inlineCtrl =
            ((long) SwarTables.h2Top7(Hashing.smear(h0)))
                | (((long) SwarTables.h2Top7(Hashing.smear(h1))) << 8);
        return;
      }
      int eHash = e.hashCode();
      int h2 = SwarTables.h2Top7(Hashing.smear(eHash));
      long ctrl = this.inlineCtrl;
      long matches = SwarTables.matchTags(ctrl, SwarTables.broadcastTag(h2));
      Object[] elements = requireNonNull(this.dedupedElements);
      while (matches != 0L) {
        if (elements[SwarTables.firstLane(matches)].equals(e)) {
          return;
        }
        matches &= matches - 1;
      }
      if (n < 8) {
        this.inlineCtrl = ctrl | (((long) h2) << (n << 3));
        this.hashCode += eHash;
        addDedupedElement(e);
        return;
      }
      promoteToTable(e, eHash);
    }

    /**
     * Transitions from inline mode ({@code distinct == 8}) to a tiered Swiss table on the 9th item.
     */
    private void promoteToTable(E e, int eHash) {
      addDedupedElement(e);
      hashCode += eHash;
      int targetCap = min(max(9, expandTableThreshold), MAX_SWISS_CAPACITY);
      int numGroups = chooseNumGroups(targetCap);
      int tier = SwarTables.tierForSize(targetCap);
      long[] newTable =
          rebuildSwissTable(numGroups, tier, requireNonNull(dedupedElements), distinct);
      if (newTable == null) {
        switchToJdk();
        return;
      }
      this.table = newTable;
      this.inlineCtrl = 0L;
      this.expandTableThreshold = computeExpandThreshold(numGroups, tier);
    }

    private void insertTier1(E e, long[] table) {
      int eHash = e.hashCode();
      int h = Hashing.smear(eHash);
      int h2 = SwarTables.h2Top7(h);
      long h2Mask = SwarTables.broadcastTag(h2);
      int groupMask = (table.length >>> 1) - 1;
      int g = h & groupMask;
      Object[] elements = requireNonNull(this.dedupedElements);
      int step = 0;
      while (true) {
        int base = g << 1;
        long ctrl = table[base];
        long matches = SwarTables.matchTags(ctrl, h2Mask);
        if (matches != 0L) {
          long indices = table[base + 1];
          do {
            if (elements[SwarTables.read8BitIndex(indices, SwarTables.firstLane(matches))].equals(
                e)) {
              return;
            }
            matches &= matches - 1;
          } while (matches != 0L);
        }
        long emptyMask = SwarTables.emptyLanes(ctrl);
        if (emptyMask != 0L) {
          if (distinct == expandTableThreshold) {
            expandAndInsert(e, eHash);
            return;
          }
          int elemIdx = distinct;
          addDedupedElement(e);
          hashCode += eHash;
          int shift = SwarTables.firstLane(emptyMask) << 3;
          table[base] = ctrl | (((long) h2) << shift);
          table[base + 1] |= ((long) elemIdx) << shift;
          return;
        }
        step++;
        if (exceededProbeLimit(step, groupMask)) {
          fallbackToJdk(e);
          return;
        }
        g = (g + step) & groupMask;
      }
    }

    private void insertLargeTable(E e, long[] table) {
      int stride = SwarTables.strideForSize(expandTableThreshold);
      int groupMask = SwarTables.groupMaskForStride(table.length, stride);
      int eHash = e.hashCode();
      int h = Hashing.smear(eHash);
      int h2 = SwarTables.h2Top7(h);
      long h2Mask = SwarTables.broadcastTag(h2);
      int g = h & groupMask;
      Object[] elements = requireNonNull(this.dedupedElements);
      int step = 0;
      while (true) {
        int base = g * stride;
        long ctrl = table[base];
        long matches = SwarTables.matchTags(ctrl, h2Mask);
        while (matches != 0L) {
          int idx =
              SwarTables.readIndexForStride(table, base, stride, SwarTables.firstLane(matches));
          if (elements[idx].equals(e)) {
            return;
          }
          matches &= matches - 1;
        }
        long emptyMask = SwarTables.emptyLanes(ctrl);
        if (emptyMask != 0L) {
          if (distinct == expandTableThreshold) {
            expandAndInsert(e, eHash);
            return;
          }
          int elemIdx = distinct;
          addDedupedElement(e);
          hashCode += eHash;
          SwarTables.writeSlot(
              table, base, stride, ctrl, h2, SwarTables.firstLane(emptyMask), elemIdx);
          return;
        }
        step++;
        if (exceededProbeLimit(step, groupMask)) {
          fallbackToJdk(e);
          return;
        }
        g = (g + step) & groupMask;
      }
    }

    private void fallbackToJdk(E e) {
      switchToJdk();
      if (requireNonNull(jdkDelegate).add(e)) {
        addDedupedElement(e);
      }
    }

    /**
     * Grows the table (or promotes to the next index width) when {@code distinct ==
     * expandTableThreshold}.
     */
    private void expandAndInsert(E e, int eHash) {
      int numGroups = chooseNumGroups(distinct + 1);
      int tier = SwarTables.tierForSize(distinct + 1);
      addDedupedElement(e);
      hashCode += eHash;
      long[] newTable =
          rebuildSwissTable(numGroups, tier, requireNonNull(dedupedElements), distinct);
      if (newTable == null) {
        switchToJdk();
        return;
      }
      this.table = newTable;
      this.expandTableThreshold = computeExpandThreshold(numGroups, tier);
    }

    /**
     * Shrinks oversized Swiss tables (for example, when duplicates collapse or {@code
     * expectedCapacity} was an overestimate) and checks for adversarial full-group probe chains.
     */
    private void review() {
      long[] table = this.table;
      if (table == null) {
        return;
      }
      int curTier = SwarTables.tierForSize(expandTableThreshold);
      int curGroups = SwarTables.groupMaskForStride(table.length, curTier + 1) + 1;
      int targetGroups = chooseNumGroups(distinct);
      int targetTier = SwarTables.tierForSize(distinct);
      if (targetGroups < curGroups || targetTier < curTier) {
        long[] rebuilt =
            rebuildSwissTable(targetGroups, targetTier, requireNonNull(dedupedElements), distinct);
        if (rebuilt != null) {
          this.table = rebuilt;
          this.expandTableThreshold = computeExpandThreshold(targetGroups, targetTier);
        }
      }
    }

    @Override
    public ImmutableSet<E> build() {
      switch (distinct) {
        case 0:
          return of();
        case 1:
          if (firstElement != null) {
            return of(firstElement);
          }
          @SuppressWarnings("unchecked") // only E instances were added
          E single = (E) requireNonNull(dedupedElements)[0];
          return of(single);
        default:
          forceCopy = true;
          review();
          Object[] elements = requireNonNull(dedupedElements);
          if (distinct < elements.length) {
            elements = Arrays.copyOf(elements, distinct);
          }
          return new RegularImmutableSet<>(elements, hashCode, table, inlineCtrl);
      }
    }

    /**
     * Returns the smallest power-of-two group count {@code >= 2} that holds {@code setSize}
     * elements at {@code <= 7/8} (87.5%) max load.
     */
    private static int chooseNumGroups(int setSize) {
      if (setSize <= 14) {
        return 2;
      }
      checkArgument(setSize <= MAX_SWISS_CAPACITY, "collection too large");
      int neededGroups = (setSize - 1) / 7 + 1;
      return Integer.highestOneBit(neededGroups - 1) << 1;
    }

    /** Caps expansion at either 7/8 group load or the current tier's maximum representable size. */
    private static int computeExpandThreshold(int numGroups, int tier) {
      int maxByLoad = numGroups * 7;
      switch (tier) {
        case 1:
          return min(maxByLoad, SwarTables.TIER1_MAX_SIZE);
        case 2:
          return min(maxByLoad, SwarTables.TIER2_MAX_SIZE);
        case 3:
          return min(maxByLoad, SwarTables.TIER3_MAX_SIZE);
        default:
          return maxByLoad;
      }
    }

    private static boolean exceededProbeLimit(int step, int groupMask) {
      return step >= 8 && step >= maxGroupProbes(groupMask + 1);
    }

    /**
     * Builds a new tiered interleaved Swiss table from the first {@code n} objects in {@code
     * elements}, or returns {@code null} if a probe chain exceeds {@link #maxGroupProbes(int)}.
     */
    static long @Nullable [] rebuildSwissTable(int numGroups, int tier, Object[] elements, int n) {
      int stride = tier + 1;
      long[] table = new long[numGroups * stride];
      int groupMask = numGroups - 1;
      int maxProbes = maxGroupProbes(numGroups);
      for (int i = 0; i < n; i++) {
        int h = Hashing.smear(elements[i].hashCode());
        int h2 = SwarTables.h2Top7(h);
        int g = h & groupMask;
        int step = 0;
        while (true) {
          int base = g * stride;
          long ctrl = table[base];
          long emptyMask = SwarTables.emptyLanes(ctrl);
          if (emptyMask != 0L) {
            SwarTables.writeSlot(table, base, stride, ctrl, h2, SwarTables.firstLane(emptyMask), i);
            break;
          }
          step++;
          if (step >= maxProbes) {
            return null;
          }
          g = (g + step) & groupMask;
        }
      }
      return table;
    }

    /**
     * Checks the whole Swiss table for an adversarially long probe chain across full groups.
     *
     * <p>Online detection in {@code insert*} bounds insertion time to O(n log n), while this check
     * also catches hash distributions crafted to fill groups independently while creating an
     * O(n)-probe lookup chain along triangular probing.
     */
    static boolean swarHashFloodingDetected(long[] table, int tier, int numGroups) {
      int maxProbes = maxGroupProbes(numGroups);
      int groupMask = numGroups - 1;
      int stride = tier + 1;
      outer:
      for (int g = 0; g < numGroups; g++) {
        if (!SwarTables.hasEmptyLane(table[g * stride])) {
          int cur = g;
          for (int step = 1; step < maxProbes; step++) {
            cur = (cur + step) & groupMask;
            if (SwarTables.hasEmptyLane(table[cur * stride])) {
              continue outer;
            }
          }
          return true;
        }
      }
      return false;
    }

    static int maxGroupProbes(int numGroups) {
      return max(8, MAX_GROUP_PROBES_MULTIPLIER * (31 - Integer.numberOfLeadingZeros(numGroups)));
    }
  }

  private static int estimatedSizeForUnknownDuplication(int inputElementsIncludingAnyDuplicates) {
    if (inputElementsIncludingAnyDuplicates
        < ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY) {
      return inputElementsIncludingAnyDuplicates;
    }
    // Guess the size is "halfway between" all duplicates and no duplicates, on a log scale.
    return max(
        ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY,
        sqrt(inputElementsIncludingAnyDuplicates, RoundingMode.CEILING));
  }

  @GwtIncompatible @J2ktIncompatible   private static final long serialVersionUID = 0xcafebabe;
}

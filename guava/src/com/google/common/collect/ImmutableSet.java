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
import static com.google.common.math.IntMath.sqrt;
import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.IntMath;
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
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collector;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Set} whose contents will never change, with many other important properties detailed at
 * {@link ImmutableCollection}.
 *
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {
  static final int SPLITERATOR_CHARACTERISTICS =
      ImmutableCollection.SPLITERATOR_CHARACTERISTICS | Spliterator.DISTINCT;

  /**
   * Returns a {@code Collector} that accumulates the input elements into a new {@code
   * ImmutableSet}. Elements appear in the resulting set in the encounter order of the stream; if
   * the stream contains duplicates (according to {@link Object#equals(Object)}), only the first
   * duplicate in encounter order will appear in the result.
   *
   * @since 21.0
   */
  public static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {
    return CollectCollectors.toImmutableSet();
  }

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
   * TODO: b/315526394 - Skip the Builder entirely for the of(...) methods, since we don't need to
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
    return new RegularSetBuilderImpl<E>(2).add(e1).add(e2).review().build();
  }

  /**
   * Returns an immutable set containing the given elements, minus duplicates, in the order each was
   * first specified. That is, if multiple elements are {@linkplain Object#equals equal}, all except
   * the first are ignored.
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3) {
    return new RegularSetBuilderImpl<E>(3).add(e1).add(e2).add(e3).review().build();
  }

  /**
   * Returns an immutable set containing the given elements, minus duplicates, in the order each was
   * first specified. That is, if multiple elements are {@linkplain Object#equals equal}, all except
   * the first are ignored.
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4) {
    return new RegularSetBuilderImpl<E>(4).add(e1).add(e2).add(e3).add(e4).review().build();
  }

  /**
   * Returns an immutable set containing the given elements, minus duplicates, in the order each was
   * first specified. That is, if multiple elements are {@linkplain Object#equals equal}, all except
   * the first are ignored.
   */
  public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5) {
    return new RegularSetBuilderImpl<E>(5).add(e1).add(e2).add(e3).add(e4).add(e5).review().build();
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
    SetBuilderImpl<E> builder = new RegularSetBuilderImpl<>(6 + others.length);
    builder = builder.add(e1).add(e2).add(e3).add(e4).add(e5).add(e6);
    for (int i = 0; i < others.length; i++) {
      builder = builder.add(others[i]);
    }
    return builder.review().build();
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
  // This the best we could do to get copyOfEnumSet to compile in the mainline.
  // The suppression also covers the cast to E[], discussed below.
  // In the backport, we don't have those cases and thus don't need this suppression.
  // We keep it to minimize diffs.
  @SuppressWarnings("unchecked")
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
    } else if (elements instanceof EnumSet) {
      return copyOfEnumSet((EnumSet<?>) elements);
    }

    if (elements.isEmpty()) {
      // We avoid allocating anything.
      return of();
    }
    // Collection<E>.toArray() is required to contain only E instances, and all we do is read them.
    // TODO(cpovirk): Consider using Object[] anyway.
    E[] array = (E[]) elements.toArray();
    /*
     * For a Set, we guess that it contains no duplicates. That's just a guess for purpose of
     * sizing; if the Set uses different equality semantics, it might contain duplicates according
     * to equals(), and we will deduplicate those properly, albeit at some cost in allocations.
     */
    int expectedSize =
        elements instanceof Set ? array.length : estimatedSizeForUnknownDuplication(array.length);
    return fromArrayWithExpectedSize(array, expectedSize);
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
    return fromArrayWithExpectedSize(elements, estimatedSizeForUnknownDuplication(elements.length));
  }

  private static <E> ImmutableSet<E> fromArrayWithExpectedSize(E[] elements, int expectedSize) {
    switch (elements.length) {
      case 0:
        return of();
      case 1:
        return of(elements[0]);
      default:
        SetBuilderImpl<E> builder = new RegularSetBuilderImpl<>(expectedSize);
        for (int i = 0; i < elements.length; i++) {
          builder = builder.add(elements[i]);
        }
        return builder.review().build();
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"}) // necessary to compile against Java 8
  private static ImmutableSet copyOfEnumSet(EnumSet<?> enumSet) {
    return ImmutableEnumSet.asImmutable(EnumSet.copyOf((EnumSet) enumSet));
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

  @GwtCompatible
  abstract static class CachingAsList<E> extends ImmutableSet<E> {
    @LazyInit @RetainedWith private transient @Nullable ImmutableList<E> asList;

    @Override
    public ImmutableList<E> asList() {
      ImmutableList<E> result = asList;
      if (result == null) {
        return asList = createAsList();
      } else {
        return result;
      }
    }

    ImmutableList<E> createAsList() {
      return new RegularImmutableAsList<>(this, toArray());
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible // serialization
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
    }
  }

  abstract static class Indexed<E> extends CachingAsList<E> {
    abstract E get(int index);

    @Override
    public UnmodifiableIterator<E> iterator() {
      return asList().iterator();
    }

    @Override
    public Spliterator<E> spliterator() {
      return CollectSpliterators.indexed(size(), SPLITERATOR_CHARACTERISTICS, this::get);
    }

    @Override
    public void forEach(Consumer<? super E> consumer) {
      checkNotNull(consumer);
      int n = size();
      for (int i = 0; i < n; i++) {
        consumer.accept(get(i));
      }
    }

    @Override
    int copyIntoArray(@Nullable Object[] dst, int offset) {
      return asList().copyIntoArray(dst, offset);
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

        // redeclare to help optimizers with b/310253115
        @SuppressWarnings("RedundantOverride")
        @Override
        @J2ktIncompatible // serialization
        @GwtIncompatible // serialization
        Object writeReplace() {
          return super.writeReplace();
        }
      };
    }

    // redeclare to help optimizers with b/310253115
    @SuppressWarnings("RedundantOverride")
    @Override
    @J2ktIncompatible // serialization
    @GwtIncompatible // serialization
    Object writeReplace() {
      return super.writeReplace();
    }
  }

  /*
   * This class is used to serialize all ImmutableSet instances, except for
   * ImmutableEnumSet/ImmutableSortedSet, regardless of implementation type. It
   * captures their "logical contents" and they are reconstructed using public
   * static factories. This is necessary to ensure that the existence of a
   * particular implementation type is an implementation detail.
   */
  @J2ktIncompatible // serialization
  private static class SerializedForm implements Serializable {
    final Object[] elements;

    SerializedForm(Object[] elements) {
      this.elements = elements;
    }

    Object readResolve() {
      return copyOf(elements);
    }

    @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;
  }

  @Override
  @J2ktIncompatible // serialization
  Object writeReplace() {
    return new SerializedForm(toArray());
  }

  @J2ktIncompatible // serialization
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
     * `impl` is null only for instances of the subclass, ImmutableSortedSet.Builder. That subclass
     * overrides all the methods that access it here. Thus, all the methods here can safely assume
     * that this field is non-null.
     */
    private @Nullable SetBuilderImpl<E> impl;
    boolean forceCopy;

    public Builder() {
      this(0);
    }

    Builder(int capacity) {
      if (capacity > 0) {
        impl = new RegularSetBuilderImpl<>(capacity);
      } else {
        impl = EmptySetBuilderImpl.instance();
      }
    }

    Builder(@SuppressWarnings("unused") boolean subclass) {
      this.impl = null; // unused
    }

    @VisibleForTesting
    void forceJdk() {
      requireNonNull(impl); // see the comment on the field
      this.impl = new JdkBackedSetBuilderImpl<>(impl);
    }

    final void copyIfNecessary() {
      if (forceCopy) {
        copy();
        forceCopy = false;
      }
    }

    void copy() {
      requireNonNull(impl); // see the comment on the field
      impl = impl.copy();
    }

    @Override
    @CanIgnoreReturnValue
    public Builder<E> add(E element) {
      requireNonNull(impl); // see the comment on the field
      checkNotNull(element);
      copyIfNecessary();
      impl = impl.add(element);
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
      requireNonNull(impl);
      requireNonNull(other.impl);
      /*
       * For discussion of requireNonNull, see the comment on the field.
       *
       * (And I don't believe there's any situation in which we call x.combine(y) when x is a plain
       * ImmutableSet.Builder but y is an ImmutableSortedSet.Builder (or vice versa). Certainly
       * ImmutableSortedSet.Builder.combine() is written as if its argument will never be a plain
       * ImmutableSet.Builder: It casts immediately to ImmutableSortedSet.Builder.)
       */
      copyIfNecessary();
      this.impl = this.impl.combine(other.impl);
      return this;
    }

    @Override
    public ImmutableSet<E> build() {
      requireNonNull(impl); // see the comment on the field
      forceCopy = true;
      impl = impl.review();
      return impl.build();
    }
  }

  /** Swappable internal implementation of an ImmutableSet.Builder. */
  private abstract static class SetBuilderImpl<E> {
    // The first `distinct` elements are non-null.
    // Since we can never access null elements, we don't mark this nullable.
    E[] dedupedElements;
    int distinct;

    @SuppressWarnings("unchecked")
    SetBuilderImpl(int expectedCapacity) {
      this.dedupedElements = (E[]) new Object[expectedCapacity];
      this.distinct = 0;
    }

    /** Initializes this SetBuilderImpl with a copy of the deduped elements array from toCopy. */
    SetBuilderImpl(SetBuilderImpl<E> toCopy) {
      this.dedupedElements = Arrays.copyOf(toCopy.dedupedElements, toCopy.dedupedElements.length);
      this.distinct = toCopy.distinct;
    }

    /**
     * Resizes internal data structures if necessary to store the specified number of distinct
     * elements.
     */
    private void ensureCapacity(int minCapacity) {
      if (minCapacity > dedupedElements.length) {
        int newCapacity =
            ImmutableCollection.Builder.expandedCapacity(dedupedElements.length, minCapacity);
        dedupedElements = Arrays.copyOf(dedupedElements, newCapacity);
      }
    }

    /** Adds e to the insertion-order array of deduplicated elements. Calls ensureCapacity. */
    final void addDedupedElement(E e) {
      ensureCapacity(distinct + 1);
      dedupedElements[distinct++] = e;
    }

    /**
     * Adds e to this SetBuilderImpl, returning the updated result. Only use the returned
     * SetBuilderImpl, since we may switch implementations if e.g. hash flooding is detected.
     */
    abstract SetBuilderImpl<E> add(E e);

    /** Adds all the elements from the specified SetBuilderImpl to this SetBuilderImpl. */
    final SetBuilderImpl<E> combine(SetBuilderImpl<E> other) {
      SetBuilderImpl<E> result = this;
      for (int i = 0; i < other.distinct; i++) {
        /*
         * requireNonNull is safe because we ensure that the first `distinct` elements have been
         * populated.
         */
        result = result.add(requireNonNull(other.dedupedElements[i]));
      }
      return result;
    }

    /**
     * Creates a new copy of this SetBuilderImpl. Modifications to that SetBuilderImpl will not
     * affect this SetBuilderImpl or sets constructed from this SetBuilderImpl via build().
     */
    abstract SetBuilderImpl<E> copy();

    /**
     * Call this before build(). Does a final check on the internal data structures, e.g. shrinking
     * unnecessarily large structures or detecting previously unnoticed hash flooding.
     */
    SetBuilderImpl<E> review() {
      return this;
    }

    abstract ImmutableSet<E> build();
  }

  private static final class EmptySetBuilderImpl<E> extends SetBuilderImpl<E> {
    private static final EmptySetBuilderImpl<Object> INSTANCE = new EmptySetBuilderImpl<>();

    @SuppressWarnings("unchecked")
    static <E> SetBuilderImpl<E> instance() {
      return (SetBuilderImpl<E>) INSTANCE;
    }

    private EmptySetBuilderImpl() {
      super(0);
    }

    @Override
    SetBuilderImpl<E> add(E e) {
      return new RegularSetBuilderImpl<E>(Builder.DEFAULT_INITIAL_CAPACITY).add(e);
    }

    @Override
    SetBuilderImpl<E> copy() {
      return this;
    }

    @Override
    ImmutableSet<E> build() {
      return ImmutableSet.of();
    }
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
   * Default implementation of the guts of ImmutableSet.Builder, creating an open-addressed hash
   * table and deduplicating elements as they come, so it only allocates O(max(distinct,
   * expectedCapacity)) rather than O(calls to add).
   *
   * <p>This implementation attempts to detect hash flooding, and if it's identified, falls back to
   * JdkBackedSetBuilderImpl.
   */
  private static final class RegularSetBuilderImpl<E> extends SetBuilderImpl<E> {
    // null until at least two elements are present
    private @Nullable Object @Nullable [] hashTable;
    private int maxRunBeforeFallback;
    private int expandTableThreshold;
    private int hashCode;

    RegularSetBuilderImpl(int expectedCapacity) {
      super(expectedCapacity);
      this.hashTable = null;
      this.maxRunBeforeFallback = 0;
      this.expandTableThreshold = 0;
    }

    RegularSetBuilderImpl(RegularSetBuilderImpl<E> toCopy) {
      super(toCopy);
      this.hashTable = (toCopy.hashTable == null) ? null : toCopy.hashTable.clone();
      this.maxRunBeforeFallback = toCopy.maxRunBeforeFallback;
      this.expandTableThreshold = toCopy.expandTableThreshold;
      this.hashCode = toCopy.hashCode;
    }

    @Override
    SetBuilderImpl<E> add(E e) {
      checkNotNull(e);
      if (hashTable == null) {
        if (distinct == 0) {
          addDedupedElement(e);
          return this;
        } else {
          ensureTableCapacity(dedupedElements.length);
          E elem = dedupedElements[0];
          distinct--;
          return insertInHashTable(elem).add(e);
        }
      }
      return insertInHashTable(e);
    }

    private SetBuilderImpl<E> insertInHashTable(E e) {
      requireNonNull(hashTable);
      int eHash = e.hashCode();
      int i0 = Hashing.smear(eHash);
      int mask = hashTable.length - 1;
      for (int i = i0; i - i0 < maxRunBeforeFallback; i++) {
        int index = i & mask;
        Object tableEntry = hashTable[index];
        if (tableEntry == null) {
          addDedupedElement(e);
          hashTable[index] = e;
          hashCode += eHash;
          ensureTableCapacity(distinct); // rebuilds table if necessary
          return this;
        } else if (tableEntry.equals(e)) { // not a new element, ignore
          return this;
        }
      }
      // we fell out of the loop due to a long run; fall back to JDK impl
      return new JdkBackedSetBuilderImpl<E>(this).add(e);
    }

    @Override
    SetBuilderImpl<E> copy() {
      return new RegularSetBuilderImpl<>(this);
    }

    @Override
    SetBuilderImpl<E> review() {
      if (hashTable == null) {
        return this;
      }
      int targetTableSize = chooseTableSize(distinct);
      if (targetTableSize * 2 < hashTable.length) {
        hashTable = rebuildHashTable(targetTableSize, dedupedElements, distinct);
        maxRunBeforeFallback = maxRunBeforeFallback(targetTableSize);
        expandTableThreshold = (int) (DESIRED_LOAD_FACTOR * targetTableSize);
      }
      return hashFloodingDetected(hashTable) ? new JdkBackedSetBuilderImpl<E>(this) : this;
    }

    @Override
    ImmutableSet<E> build() {
      switch (distinct) {
        case 0:
          return of();
        case 1:
          /*
           * requireNonNull is safe because we ensure that the first `distinct` elements have been
           * populated.
           */
          return of(requireNonNull(dedupedElements[0]));
        default:
          /*
           * The suppression is safe because we ensure that the first `distinct` elements have been
           * populated.
           */
          @SuppressWarnings("nullness")
          Object[] elements =
              (distinct == dedupedElements.length)
                  ? dedupedElements
                  : Arrays.copyOf(dedupedElements, distinct);
          return new RegularImmutableSet<>(
              elements, hashCode, requireNonNull(hashTable), hashTable.length - 1);
      }
    }

    /** Builds a new open-addressed hash table from the first n objects in elements. */
    static @Nullable Object[] rebuildHashTable(int newTableSize, Object[] elements, int n) {
      @Nullable Object[] hashTable = new @Nullable Object[newTableSize];
      int mask = hashTable.length - 1;
      for (int i = 0; i < n; i++) {
        // requireNonNull is safe because we ensure that the first n elements have been populated.
        Object e = requireNonNull(elements[i]);
        int j0 = Hashing.smear(e.hashCode());
        for (int j = j0; ; j++) {
          int index = j & mask;
          if (hashTable[index] == null) {
            hashTable[index] = e;
            break;
          }
        }
      }
      return hashTable;
    }

    void ensureTableCapacity(int minCapacity) {
      int newTableSize;
      if (hashTable == null) {
        newTableSize = chooseTableSize(minCapacity);
        hashTable = new Object[newTableSize];
      } else if (minCapacity > expandTableThreshold && hashTable.length < MAX_TABLE_SIZE) {
        newTableSize = hashTable.length * 2;
        hashTable = rebuildHashTable(newTableSize, dedupedElements, distinct);
      } else {
        return;
      }
      maxRunBeforeFallback = maxRunBeforeFallback(newTableSize);
      expandTableThreshold = (int) (DESIRED_LOAD_FACTOR * newTableSize);
    }

    /**
     * We attempt to detect deliberate hash flooding attempts. If one is detected, we fall back to a
     * wrapper around j.u.HashSet, which has built-in flooding protection. MAX_RUN_MULTIPLIER was
     * determined experimentally to match our desired probability of false positives.
     */
    // NB: yes, this is surprisingly high, but that's what the experiments said was necessary
    // Raising this number slows the worst-case contains behavior, speeds up hashFloodingDetected,
    // and reduces the false-positive probability.
    static final int MAX_RUN_MULTIPLIER = 13;

    /**
     * Checks the whole hash table for poor hash distribution. Takes O(n) in the worst case, O(n /
     * log n) on average.
     *
     * <p>The online hash flooding detecting in RegularSetBuilderImpl.add can detect e.g. many
     * exactly matching hash codes, which would cause construction to take O(n^2), but can't detect
     * e.g. hash codes adversarially designed to go into ascending table locations, which keeps
     * construction O(n) (as desired) but then can have O(n) queries later.
     *
     * <p>If this returns false, then no query can take more than O(log n).
     *
     * <p>Note that for a RegularImmutableSet with elements with truly random hash codes, contains
     * operations take expected O(1) time but with high probability take O(log n) for at least some
     * element. (https://en.wikipedia.org/wiki/Linear_probing#Analysis)
     *
     * <p>This method may return {@code true} even on truly random input, but {@code
     * ImmutableSetTest} tests that the probability of that is low.
     */
    static boolean hashFloodingDetected(@Nullable Object[] hashTable) {
      int maxRunBeforeFallback = maxRunBeforeFallback(hashTable.length);
      int mask = hashTable.length - 1;

      // Invariant: all elements at indices in [knownRunStart, knownRunEnd) are nonnull.
      // If knownRunStart == knownRunEnd, this is vacuously true.
      // When knownRunEnd exceeds hashTable.length, it "wraps", detecting runs around the end
      // of the table.
      int knownRunStart = 0;
      int knownRunEnd = 0;

      outerLoop:
      while (knownRunStart < hashTable.length) {
        if (knownRunStart == knownRunEnd && hashTable[knownRunStart] == null) {
          if (hashTable[(knownRunStart + maxRunBeforeFallback - 1) & mask] == null) {
            // There are only maxRunBeforeFallback - 1 elements between here and there,
            // so even if they were all nonnull, we wouldn't detect a hash flood.  Therefore,
            // we can skip them all.
            knownRunStart += maxRunBeforeFallback;
          } else {
            knownRunStart++; // the only case in which maxRunEnd doesn't increase by mRBF
            // happens about f * (1-f) for f = DESIRED_LOAD_FACTOR, so around 21% of the time
          }
          knownRunEnd = knownRunStart;
        } else {
          for (int j = knownRunStart + maxRunBeforeFallback - 1; j >= knownRunEnd; j--) {
            if (hashTable[j & mask] == null) {
              knownRunEnd = knownRunStart + maxRunBeforeFallback;
              knownRunStart = j + 1;
              continue outerLoop;
            }
          }
          return true;
        }
      }
      return false;
    }

    /**
     * If more than this many consecutive positions are filled in a table of the specified size,
     * report probable hash flooding. ({@link #hashFloodingDetected} may also report hash flooding
     * if fewer consecutive positions are filled; see that method for details.)
     */
    static int maxRunBeforeFallback(int tableSize) {
      return MAX_RUN_MULTIPLIER * IntMath.log2(tableSize, RoundingMode.UNNECESSARY);
    }
  }

  /**
   * SetBuilderImpl version that uses a JDK HashSet, which has built in hash flooding protection.
   */
  private static final class JdkBackedSetBuilderImpl<E> extends SetBuilderImpl<E> {
    private final Set<Object> delegate;

    JdkBackedSetBuilderImpl(SetBuilderImpl<E> toCopy) {
      super(toCopy); // initializes dedupedElements and distinct
      delegate = Sets.newHashSetWithExpectedSize(distinct);
      for (int i = 0; i < distinct; i++) {
        /*
         * requireNonNull is safe because we ensure that the first `distinct` elements have been
         * populated.
         */
        delegate.add(requireNonNull(dedupedElements[i]));
      }
    }

    @Override
    SetBuilderImpl<E> add(E e) {
      checkNotNull(e);
      if (delegate.add(e)) {
        addDedupedElement(e);
      }
      return this;
    }

    @Override
    SetBuilderImpl<E> copy() {
      return new JdkBackedSetBuilderImpl<>(this);
    }

    @Override
    ImmutableSet<E> build() {
      switch (distinct) {
        case 0:
          return of();
        case 1:
          /*
           * requireNonNull is safe because we ensure that the first `distinct` elements have been
           * populated.
           */
          return of(requireNonNull(dedupedElements[0]));
        default:
          return new JdkBackedImmutableSet<>(
              delegate, ImmutableList.asImmutableList(dedupedElements, distinct));
      }
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

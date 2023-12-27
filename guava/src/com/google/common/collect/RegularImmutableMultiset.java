/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.Multisets.ImmutableEntry;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of {@link ImmutableMultiset} with zero or more elements.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true, serializable = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
@ElementTypesAreNonnullByDefault
class RegularImmutableMultiset<E> extends ImmutableMultiset<E> {
  private static final ImmutableEntry<?>[] EMPTY_ARRAY = new ImmutableEntry<?>[0];
  static final ImmutableMultiset<Object> EMPTY = create(ImmutableList.<Entry<Object>>of());

  static <E> ImmutableMultiset<E> create(Collection<? extends Entry<? extends E>> entries) {
    int distinct = entries.size();
    @SuppressWarnings({"unchecked", "rawtypes"})
    ImmutableEntry<E>[] entryArray = new ImmutableEntry[distinct];
    if (distinct == 0) {
      return new RegularImmutableMultiset<>(entryArray, EMPTY_ARRAY, 0, 0, ImmutableSet.of());
    }
    int tableSize = Hashing.closedTableSize(distinct, MAX_LOAD_FACTOR);
    int mask = tableSize - 1;
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    ImmutableEntry<E>[] hashTable = new @Nullable ImmutableEntry[tableSize];

    int index = 0;
    int hashCode = 0;
    long size = 0;
    for (Entry<? extends E> entryWithWildcard : entries) {
      @SuppressWarnings("unchecked") // safe because we only read from it
      Entry<E> entry = (Entry<E>) entryWithWildcard;
      E element = checkNotNull(entry.getElement());
      int count = entry.getCount();
      int hash = element.hashCode();
      int bucket = Hashing.smear(hash) & mask;
      ImmutableEntry<E> bucketHead = hashTable[bucket];
      ImmutableEntry<E> newEntry;
      if (bucketHead == null) {
        boolean canReuseEntry =
            entry instanceof ImmutableEntry && !(entry instanceof NonTerminalEntry);
        newEntry =
            canReuseEntry ? (ImmutableEntry<E>) entry : new ImmutableEntry<E>(element, count);
      } else {
        newEntry = new NonTerminalEntry<E>(element, count, bucketHead);
      }
      hashCode += hash ^ count;
      entryArray[index++] = newEntry;
      hashTable[bucket] = newEntry;
      size += count;
    }

    return hashFloodingDetected(hashTable)
        ? JdkBackedImmutableMultiset.create(ImmutableList.asImmutableList(entryArray))
        : new RegularImmutableMultiset<E>(
            entryArray, hashTable, Ints.saturatedCast(size), hashCode, null);
  }

  private static boolean hashFloodingDetected(@Nullable ImmutableEntry<?>[] hashTable) {
    for (int i = 0; i < hashTable.length; i++) {
      int bucketLength = 0;
      for (ImmutableEntry<?> entry = hashTable[i]; entry != null; entry = entry.nextInBucket()) {
        bucketLength++;
        if (bucketLength > MAX_HASH_BUCKET_LENGTH) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Closed addressing tends to perform well even with high load factors. Being conservative here
   * ensures that the table is still likely to be relatively sparse (hence it misses fast) while
   * saving space.
   */
  @VisibleForTesting static final double MAX_LOAD_FACTOR = 1.0;

  /**
   * Maximum allowed false positive probability of detecting a hash flooding attack given random
   * input.
   */
  @VisibleForTesting static final double HASH_FLOODING_FPP = 0.001;

  /**
   * Maximum allowed length of a hash table bucket before falling back to a j.u.HashMap based
   * implementation. Experimentally determined.
   */
  @VisibleForTesting static final int MAX_HASH_BUCKET_LENGTH = 9;

  private final transient ImmutableEntry<E>[] entries;
  private final transient @Nullable ImmutableEntry<?>[] hashTable;
  private final transient int size;
  private final transient int hashCode;

  @LazyInit @CheckForNull private transient ImmutableSet<E> elementSet;

  private RegularImmutableMultiset(
      ImmutableEntry<E>[] entries,
      @Nullable ImmutableEntry<?>[] hashTable,
      int size,
      int hashCode,
      @CheckForNull ImmutableSet<E> elementSet) {
    this.entries = entries;
    this.hashTable = hashTable;
    this.size = size;
    this.hashCode = hashCode;
    this.elementSet = elementSet;
  }

  private static final class NonTerminalEntry<E> extends ImmutableEntry<E> {
    private final ImmutableEntry<E> nextInBucket;

    NonTerminalEntry(E element, int count, ImmutableEntry<E> nextInBucket) {
      super(element, count);
      this.nextInBucket = nextInBucket;
    }

    @Override
    public ImmutableEntry<E> nextInBucket() {
      return nextInBucket;
    }
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  public int count(@CheckForNull Object element) {
    @Nullable ImmutableEntry<?>[] hashTable = this.hashTable;
    if (element == null || hashTable.length == 0) {
      return 0;
    }
    int hash = Hashing.smearedHash(element);
    int mask = hashTable.length - 1;
    for (ImmutableEntry<?> entry = hashTable[hash & mask];
        entry != null;
        entry = entry.nextInBucket()) {
      if (Objects.equal(element, entry.getElement())) {
        return entry.getCount();
      }
    }
    return 0;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public ImmutableSet<E> elementSet() {
    ImmutableSet<E> result = elementSet;
    return (result == null) ? elementSet = new ElementSet<E>(Arrays.asList(entries), this) : result;
  }

  @Override
  Entry<E> getEntry(int index) {
    return entries[index];
  }

  @Override
  public int hashCode() {
    return hashCode;
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

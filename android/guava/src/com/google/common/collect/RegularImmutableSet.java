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

import static com.google.common.collect.Hashing.smearedHash;
import static com.google.common.collect.SwarTables.STRIDE_8;
import static com.google.common.collect.SwarTables.TIER1_MAX_SIZE;
import static com.google.common.collect.SwarTables.broadcastH2Top7;
import static com.google.common.collect.SwarTables.emptyLanes;
import static com.google.common.collect.SwarTables.firstLane;
import static com.google.common.collect.SwarTables.groupMaskForStride;
import static com.google.common.collect.SwarTables.hasEmptyLane;
import static com.google.common.collect.SwarTables.matchTags;
import static com.google.common.collect.SwarTables.read8BitIndex;
import static com.google.common.collect.SwarTables.readIndexForStride;
import static com.google.common.collect.SwarTables.strideForSize;
import static java.lang.System.arraycopy;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import java.util.Spliterator;
import java.util.Spliterators;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of {@link ImmutableSet} with two or more elements.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
final class RegularImmutableSet<E> extends ImmutableSet<E> {
  private static final Object[] EMPTY_ARRAY = new Object[0];
  static final RegularImmutableSet<Object> EMPTY =
      new RegularImmutableSet<>(EMPTY_ARRAY, 0, null, 0L);

  private final transient Object[] elements;
  private final transient int hashCode;

  /*
   * Two compact storage modes share this single class so ImmutableSet.contains() call sites stay
   * strictly bimorphic with SingletonImmutableSet:
   *
   * 1. Inline mode (elements.length <= 8, table == null):
   *    Up to 8 occupied h2 control bytes sit directly in `inlineCtrl` (byte lane `k` corresponds to
   *    `elements[k]`), avoiding any secondary table allocation or cache miss.
   *
   * 2. Tiered interleaved Swiss-table mode (elements.length > 8, table != null):
   *    Groups of 8 slots are packed contiguously inside a single primitive `long[]`, capped at 7/8
   *    (87.5%) max load and probed with triangular group steps `g_k = (g_0 + k*(k+1)/2) & mask`:
   *      - Word 0 of each group is the 8-byte SWAR control word: 0x00 = empty (zero-initialized by
   *        the JVM for free), 0x80 | h2_7bit (0x80..0xFF) = occupied.
   *      - Words 1..stride-1 pack element indices (8-, 16-, 24-, or 32-bit by tier, strides 2..5)
   *        next to their control word in the same cache line, with zero object references for GC.
   *
   * Keeping `inlineCtrl` in place of the former `mask` field and omitting `CachingAsList` preserves
   * RegularImmutableSet's existing 32-byte compressed object size.
   */
  @VisibleForTesting final transient long @Nullable [] table;
  private final transient long inlineCtrl;

  RegularImmutableSet(Object[] elements, int hashCode, long @Nullable [] table, long inlineCtrl) {
    this.elements = elements;
    this.hashCode = hashCode;
    this.table = table;
    this.inlineCtrl = inlineCtrl;
  }

  /*
   * Hot lookup methods keep the inline (n <= 8) and Tier-1 (n <= 256, 98.9% cumulative) paths small
   * and dedicated, and unify Tiers 2..4 (n > 256) in one helper—all well below HotSpot C2's
   * 325-byte FreqInlineSize threshold.
   */
  @Override
  public boolean contains(@Nullable Object target) {
    if (target == null) {
      return false;
    }
    long[] table = this.table;
    if (table == null) {
      long ctrl = this.inlineCtrl;
      if (ctrl == 0L) {
        return false;
      }
      long matches = matchTags(ctrl, broadcastH2Top7(smearedHash(target)));
      Object[] elements = this.elements;
      while (matches != 0L) {
        // matchTags never triggers on empty lanes, so lane is guaranteed < elements.length.
        if (elements[firstLane(matches)].equals(target)) {
          return true;
        }
        matches &= matches - 1;
      }
      return false;
    }
    return containsInTable(target, table);
  }

  private boolean containsInTable(Object target, long[] table) {
    Object[] elements = this.elements;
    int n = elements.length;
    if (n > TIER1_MAX_SIZE) {
      return containsInLargeTable(target, table, n);
    }
    int h = smearedHash(target);
    long h2Mask = broadcastH2Top7(h);
    int groupMask = groupMaskForStride(table.length, STRIDE_8);
    int g = h & groupMask;
    int step = 0;
    while (true) {
      int base = g << 1;
      long ctrl = table[base];
      long matches = matchTags(ctrl, h2Mask);
      if (matches != 0L) {
        long indices = table[base + 1];
        do {
          if (elements[read8BitIndex(indices, firstLane(matches))].equals(target)) {
            return true;
          }
          matches &= matches - 1;
        } while (matches != 0L);
      }
      if (hasEmptyLane(ctrl)) {
        return false;
      }
      step++;
      g = (g + step) & groupMask;
    }
  }

  private boolean containsInLargeTable(Object target, long[] table, int n) {
    int stride = strideForSize(n);
    int groupMask = groupMaskForStride(table.length, stride);
    int h = smearedHash(target);
    long h2Mask = broadcastH2Top7(h);
    int g = h & groupMask;
    Object[] elements = this.elements;
    int step = 0;
    while (true) {
      int base = g * stride;
      long ctrl = table[base];
      long matches = matchTags(ctrl, h2Mask);
      while (matches != 0L) {
        if (elements[readIndexForStride(table, base, stride, firstLane(matches))].equals(target)) {
          return true;
        }
        matches &= matches - 1;
      }
      if (emptyLanes(ctrl) != 0L) {
        return false;
      }
      step++;
      g = (g + step) & groupMask;
    }
  }

  @Override
  public int size() {
    return elements.length;
  }

  // We're careful to put only E instances into the array in the mainline.
  // (In the backport, we don't need this suppression, but we keep it to minimize diffs.)
  @SuppressWarnings("unchecked")
  @Override
  public UnmodifiableIterator<E> iterator() {
    return (UnmodifiableIterator<E>) Iterators.forArray(elements);
  }

  @Override
  public Spliterator<E> spliterator() {
    return Spliterators.spliterator(elements, SPLITERATOR_CHARACTERISTICS);
  }

  @Override
  Object[] internalArray() {
    return elements;
  }

  @Override
  int internalArrayStart() {
    return 0;
  }

  @Override
  int internalArrayEnd() {
    return elements.length;
  }

  @Override
  int copyIntoArray(@Nullable Object[] dst, int offset) {
    arraycopy(elements, 0, dst, offset, elements.length);
    return offset + elements.length;
  }

  @Override
  public ImmutableList<E> asList() {
    return (elements.length == 0)
        ? ImmutableList.of()
        : new RegularImmutableAsList<>(this, elements);
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  boolean isHashCodeFast() {
    return true;
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

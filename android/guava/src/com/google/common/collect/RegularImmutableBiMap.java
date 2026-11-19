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
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static com.google.common.collect.SwarTables.STRIDE_16;
import static com.google.common.collect.SwarTables.STRIDE_24;
import static com.google.common.collect.SwarTables.STRIDE_32;
import static com.google.common.collect.SwarTables.STRIDE_8;
import static com.google.common.collect.SwarTables.TIER1_MAX_SIZE;
import static com.google.common.collect.SwarTables.TIER2_MAX_SIZE;
import static com.google.common.collect.SwarTables.TIER3_MAX_SIZE;
import static com.google.common.collect.SwarTables.broadcastH2Low7;
import static com.google.common.collect.SwarTables.emptyLanes;
import static com.google.common.collect.SwarTables.firstLane;
import static com.google.common.collect.SwarTables.h2Low7;
import static com.google.common.collect.SwarTables.hasEmptyLane;
import static com.google.common.collect.SwarTables.matchTags;
import static com.google.common.collect.SwarTables.read16BitIndex;
import static com.google.common.collect.SwarTables.read24BitIndex;
import static com.google.common.collect.SwarTables.read32BitIndex;
import static com.google.common.collect.SwarTables.read8BitIndex;
import static com.google.common.collect.SwarTables.readIndexForStride;
import static com.google.common.collect.SwarTables.strideForSize;
import static com.google.common.collect.SwarTables.writeSlot;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.j2objc.annotations.RetainedWith;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.function.BiConsumer;
import org.jspecify.annotations.Nullable;

/**
 * Bimap with zero or more mappings, backed by a flat alternating key/value array and interleaved
 * 8-slot SWAR Swiss hash tables with fast-range group indexing.
 *
 * <p>For non-empty maps ({@code n >= 2}), {@link #alternatingKeysAndValues} stores {@code k0, v0,
 * k1, v1, ...} in insertion order, while {@link #tables} stores both the forward key table ({@code
 * [0, half)}) and the inverse value table ({@code [half, 2 * half)}) in a single {@code long[]}
 * array of interleaved 8-slot groups. Each group occupies {@code stride} consecutive {@code long}s:
 *
 * <ul>
 *   <li>{@code table[base]}: 64-bit SWAR control word containing 8 one-byte tags ({@code 0x00} for
 *       empty, {@code 0x80 | h2} for occupied, where {@code h2} is the 7-bit fingerprint).
 *   <li>{@code table[base + 1 .. base + stride - 1]}: 8 packed entry indices into {@code
 *       alternatingKeysAndValues}, sized by map cardinality:
 *       <ul>
 *         <li>{@code n <= 256} ({@code stride = 2}): 1 word of 8 × 8-bit indices.
 *         <li>{@code n <= 65,536} ({@code stride = 3}): 2 words of 8 × 16-bit indices.
 *         <li>{@code n > 65,536} ({@code stride = 5}): 4 words of 8 × 32-bit indices.
 *       </ul>
 * </ul>
 *
 * <p>If insertion encounters excessive H2 collisions or probe/full-group run lengths (indicating
 * adversarial hash flooding), construction falls back to {@link JdkBackedImmutableBiMap}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
final class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
  private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

  static final RegularImmutableBiMap<Object, Object> EMPTY =
      new RegularImmutableBiMap<>(null, EMPTY_OBJECT_ARRAY, 0);

  /** Target max entries per 8-slot group (62.5% max load factor). */
  private static final int MAX_PER_GROUP = 5;

  /** Max H2 tag matches checked during a single insertion before falling back to JDK HashMap. */
  private static final int MAX_H2_MATCHES = 12;

  /**
   * For {@code EMPTY}: {@code null}. Otherwise, a single {@code long[]} holding the forward key
   * SWAR Swiss table in {@code [0, tables.length >>> 1)} and the inverse value SWAR Swiss table in
   * {@code [tables.length >>> 1, tables.length)}.
   */
  private final transient long @Nullable [] tables;

  /** Exact-sized ({@code 2 * size}) flat array storing {@code k0, v0, k1, v1, ...}. */
  @VisibleForTesting final transient Object[] alternatingKeysAndValues;

  private final transient int hashCode;

  static <K, V> ImmutableBiMap<K, V> fromEntries(Entry<K, V>... entries) {
    return fromEntryArray(entries.length, entries);
  }

  static <K, V> ImmutableBiMap<K, V> fromEntryArray(int n, @Nullable Entry<K, V>[] entryArray) {
    checkPositionIndex(n, entryArray.length);
    if (n == 0) {
      return ImmutableBiMap.of();
    }
    if (n == 1) {
      Entry<K, V> onlyEntry = requireNonNull(entryArray[0]);
      return ImmutableBiMap.of(onlyEntry.getKey(), onlyEntry.getValue());
    }
    @Nullable Object[] alternatingKeysAndValues = new @Nullable Object[n * 2];
    for (int i = 0; i < n; i++) {
      // requireNonNull is safe because the first `n` elements have been filled in.
      Entry<K, V> entry = requireNonNull(entryArray[i]);
      K key = entry.getKey();
      V value = entry.getValue();
      checkEntryNotNull(key, value);
      alternatingKeysAndValues[i << 1] = key;
      alternatingKeysAndValues[(i << 1) + 1] = value;
    }
    return fromAlternatingKeysAndValues(n, alternatingKeysAndValues);
  }

  static <K, V> ImmutableBiMap<K, V> fromAlternatingKeysAndValues(
      int n, @Nullable Object[] alternatingKeysAndValues) {
    checkPositionIndex(n * 2, alternatingKeysAndValues.length);
    if (n == 0) {
      return ImmutableBiMap.of();
    }
    if (n == 1) {
      @SuppressWarnings("unchecked")
      K key = (K) alternatingKeysAndValues[0];
      @SuppressWarnings("unchecked")
      V value = (V) alternatingKeysAndValues[1];
      checkEntryNotNull(key, value);
      return ImmutableBiMap.of(key, value);
    }
    /*
     * Safe cast: every element in [0, 2 * n) is verified non-null via checkEntryNotNull below
     * before `akv` escapes into the returned RegularImmutableBiMap.
     */
    @SuppressWarnings("nullness")
    Object[] akv =
        (n * 2 == alternatingKeysAndValues.length)
            ? alternatingKeysAndValues
            : Arrays.copyOf(alternatingKeysAndValues, n * 2);

    // Validate all entries non-null up front so duplicate-key checks never mask a null value.
    for (int i = 0; i < n; i++) {
      checkEntryNotNull(akv[i << 1], akv[(i << 1) + 1]);
    }

    int numGroups = (n - 1) / MAX_PER_GROUP + 1;
    int stride = strideForSize(n);
    long halfLong = (long) numGroups * stride;
    if (halfLong > Integer.MAX_VALUE / 2) {
      throw new IllegalArgumentException("BiMap too large: " + n);
    }
    int half = (int) halfLong;
    long[] tables = new long[half * 2];
    int hashCode = 0;

    for (int i = 0; i < n; i++) {
      Object key = akv[i << 1];
      Object value = akv[(i << 1) + 1];
      int keyHash = key.hashCode();
      int valueHash = value.hashCode();
      insert(tables, 0, half, akv, 0, numGroups, stride, numGroups, i, key, keyHash);
      insert(
          tables, half, tables.length, akv, 1, numGroups, stride, numGroups, i, value, valueHash);
      hashCode += keyHash ^ valueHash;
    }
    return new RegularImmutableBiMap<>(tables, akv, hashCode);
  }

  /**
   * Returns {@code true} if the sub-table {@code tables[tableStart..tableEnd)} contains more than
   * {@code maxRun} consecutive full 8-slot groups (including wrap-around from {@code tableEnd} to
   * {@code tableStart}).
   *
   * <p>Guards against adversarial inputs with distinct hashes that fill a long contiguous run of
   * groups without triggering per-insertion probe limits, which would otherwise make miss lookups
   * scan {@code O(n)} full groups.
   */
  private static boolean hasContiguousFullGroupRun(
      long[] tables, int tableStart, int tableEnd, int stride, int maxRun) {
    int run = 0;
    int leadingRun = 0;
    boolean inLeading = true;
    for (int base = tableStart; base < tableEnd; base += stride) {
      if (!hasEmptyLane(tables[base])) {
        run++;
        if (run > maxRun) {
          return true;
        }
        if (inLeading) {
          leadingRun++;
        }
      } else {
        inLeading = false;
        run = 0;
      }
    }
    // Check wrap-around run combining trailing full groups with leading full groups.
    return !inLeading && (run + leadingRun > maxRun);
  }

  private static boolean insert(
      long[] tables,
      int tableStart,
      int tableEnd,
      Object[] akv,
      int keyOffset,
      int numGroups,
      int stride,
      int maxProbeGroups,
      int index,
      Object target,
      int rawHash) {
    int h = Hashing.smear(rawHash);
    int h2 = h2Low7(h);
    long pattern = broadcastH2Low7(h);
    int base = tableStart + fastRangeGroup(h, numGroups) * stride;
    int probeGroups = 0;
    int h2Matches = 0;

    while (true) {
      long ctrl = tables[base];
      long matchMask = matchTags(ctrl, pattern);
      while (matchMask != 0L) {
        int slot = firstLane(matchMask);
        int idx = readIndexForStride(tables, base, stride, slot);
        if (target.equals(akv[(idx << 1) + keyOffset])) {
          throw conflictException(
              keyOffset == 0 ? "key" : "value",
              akv[idx << 1] + "=" + akv[(idx << 1) + 1],
              akv[index << 1] + "=" + akv[(index << 1) + 1]);
        }
        if (++h2Matches > MAX_H2_MATCHES) {
          return false;
        }
        matchMask &= matchMask - 1;
      }
      long emptyMask = emptyLanes(ctrl);
      if (emptyMask != 0L) {
        int slot = firstLane(emptyMask);
        writeSlot(tables, base, stride, ctrl, h2, slot, index);
        return true;
      }
      if (++probeGroups > maxProbeGroups) {
        return false;
      }
      base = nextGroupBase(base, stride, tableStart, tableEnd);
    }
  }

  /**
   * Maps {@code smearedHash} onto {@code [0, numGroups)} via Lemire's fast-range without division,
   * consuming the high bits of {@code smearedHash}.
   */
  private static int fastRangeGroup(int smearedHash, int numGroups) {
    return (int) (((smearedHash & 0xFFFFFFFFL) * numGroups) >>> 32);
  }

  /**
   * Advances {@code base} to the next group in {@code [tableStart, tableEnd)}, wrapping around to
   * {@code tableStart} at {@code tableEnd}.
   */
  private static int nextGroupBase(int base, int stride, int tableStart, int tableEnd) {
    base += stride;
    return (base == tableEnd) ? tableStart : base;
  }

  private RegularImmutableBiMap(
      long @Nullable [] tables, Object[] alternatingKeysAndValues, int hashCode) {
    this.tables = tables;
    this.alternatingKeysAndValues = alternatingKeysAndValues;
    this.hashCode = hashCode;
  }

  @SuppressWarnings("unchecked")
  @Override
  public @Nullable V get(@Nullable Object key) {
    if (key == null || tables == null) {
      return null;
    }
    Object result = lookup(tables, 0, tables.length >>> 1, alternatingKeysAndValues, 0, key);
    /*
     * Avoid casting @Nullable Object directly to @Nullable V due to
     * https://github.com/jspecify/checker-framework/issues/8.
     */
    if (result == null) {
      return null;
    } else {
      return (V) result;
    }
  }

  /**
   * Searches {@code tables[tableStart..tableEnd)} for {@code target} (matching against {@code
   * akv[(idx << 1) + keyOffset]} and returning the paired {@code akv[pos ^ 1]}). Dispatches to
   * specialized 8/16/32-bit index readers to keep each hot lookup loop compact.
   */
  private static @Nullable Object lookup(
      long[] tables, int tableStart, int tableEnd, Object[] akv, int keyOffset, Object target) {
    int n = akv.length >>> 1;
    if (n <= TIER1_MAX_SIZE) {
      return lookup8(tables, tableStart, tableEnd, akv, keyOffset, target);
    } else if (n <= TIER2_MAX_SIZE) {
      return lookup16(tables, tableStart, tableEnd, akv, keyOffset, target);
    } else if (n <= TIER3_MAX_SIZE) {
      return lookup24(tables, tableStart, tableEnd, akv, keyOffset, target);
    } else {
      return lookup32(tables, tableStart, tableEnd, akv, keyOffset, target);
    }
  }

  private static @Nullable Object lookup8(
      long[] tables, int tableStart, int tableEnd, Object[] akv, int keyOffset, Object target) {
    int h = Hashing.smear(target.hashCode());
    long pattern = broadcastH2Low7(h);
    int tableLen = tableEnd - tableStart;
    int base = tableStart + (fastRangeGroup(h, tableLen >>> 1) << 1);
    while (true) {
      long ctrl = tables[base];
      long matchMask = matchTags(ctrl, pattern);
      if (matchMask != 0L) {
        long indices = tables[base + 1];
        do {
          int idx = read8BitIndex(indices, firstLane(matchMask));
          int pos = (idx << 1) + keyOffset;
          if (target.equals(akv[pos])) {
            return akv[pos ^ 1];
          }
          matchMask &= matchMask - 1;
        } while (matchMask != 0L);
      }
      if (hasEmptyLane(ctrl)) {
        return null;
      }
      base = nextGroupBase(base, STRIDE_8, tableStart, tableEnd);
    }
  }

  private static @Nullable Object lookup16(
      long[] tables, int tableStart, int tableEnd, Object[] akv, int keyOffset, Object target) {
    int h = Hashing.smear(target.hashCode());
    long pattern = broadcastH2Low7(h);
    int tableLen = tableEnd - tableStart;
    int base = tableStart + fastRangeGroup(h, tableLen / STRIDE_16) * STRIDE_16;
    while (true) {
      long ctrl = tables[base];
      long matchMask = matchTags(ctrl, pattern);
      while (matchMask != 0L) {
        int idx = read16BitIndex(tables, base, firstLane(matchMask));
        int pos = (idx << 1) + keyOffset;
        if (target.equals(akv[pos])) {
          return akv[pos ^ 1];
        }
        matchMask &= matchMask - 1;
      }
      if (hasEmptyLane(ctrl)) {
        return null;
      }
      base = nextGroupBase(base, STRIDE_16, tableStart, tableEnd);
    }
  }

  private static @Nullable Object lookup24(
      long[] tables, int tableStart, int tableEnd, Object[] akv, int keyOffset, Object target) {
    int h = Hashing.smear(target.hashCode());
    long pattern = broadcastH2Low7(h);
    int tableLen = tableEnd - tableStart;
    int base = tableStart + (fastRangeGroup(h, tableLen >>> 2) << 2);
    while (true) {
      long ctrl = tables[base];
      long matchMask = matchTags(ctrl, pattern);
      while (matchMask != 0L) {
        int idx = read24BitIndex(tables, base, firstLane(matchMask));
        int pos = (idx << 1) + keyOffset;
        if (target.equals(akv[pos])) {
          return akv[pos ^ 1];
        }
        matchMask &= matchMask - 1;
      }
      if (hasEmptyLane(ctrl)) {
        return null;
      }
      base = nextGroupBase(base, STRIDE_24, tableStart, tableEnd);
    }
  }

  private static @Nullable Object lookup32(
      long[] tables, int tableStart, int tableEnd, Object[] akv, int keyOffset, Object target) {
    int h = Hashing.smear(target.hashCode());
    long pattern = broadcastH2Low7(h);
    int tableLen = tableEnd - tableStart;
    int base = tableStart + fastRangeGroup(h, tableLen / STRIDE_32) * STRIDE_32;
    while (true) {
      long ctrl = tables[base];
      long matchMask = matchTags(ctrl, pattern);
      while (matchMask != 0L) {
        int idx = read32BitIndex(tables, base, firstLane(matchMask));
        int pos = (idx << 1) + keyOffset;
        if (target.equals(akv[pos])) {
          return akv[pos ^ 1];
        }
        matchMask &= matchMask - 1;
      }
      if (hasEmptyLane(ctrl)) {
        return null;
      }
      base = nextGroupBase(base, STRIDE_32, tableStart, tableEnd);
    }
  }

  @Override
  public ImmutableSet<Entry<K, V>> entrySet() {
    return isEmpty() ? ImmutableSet.of() : new FlatEntrySet<>(this, alternatingKeysAndValues, 0);
  }

  @Override
  public ImmutableSet<K> keySet() {
    return isEmpty() ? ImmutableSet.of() : new FlatKeySet<>(this, alternatingKeysAndValues, 0);
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    checkNotNull(action);
    for (int i = 0; i < alternatingKeysAndValues.length; i += 2) {
      @SuppressWarnings("unchecked")
      K key = (K) alternatingKeysAndValues[i];
      @SuppressWarnings("unchecked")
      V value = (V) alternatingKeysAndValues[i + 1];
      action.accept(key, value);
    }
  }

  @Override
  boolean isHashCodeFast() {
    return true;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  public int size() {
    return alternatingKeysAndValues.length >>> 1;
  }

  @LazyInit @RetainedWith private transient @Nullable ImmutableBiMap<V, K> inverse;

  @Override
  public ImmutableBiMap<V, K> inverse() {
    if (isEmpty()) {
      return ImmutableBiMap.of();
    }
    ImmutableBiMap<V, K> result = inverse;
    if (result == null) {
      result = inverse = new Inverse();
    }
    return result;
  }

  private final class Inverse extends ImmutableBiMap<V, K> {

    @Override
    public int size() {
      return RegularImmutableBiMap.this.size();
    }

    @Override
    public ImmutableBiMap<K, V> inverse() {
      return RegularImmutableBiMap.this;
    }

    @Override
    public void forEach(BiConsumer<? super V, ? super K> action) {
      checkNotNull(action);
      for (int i = 0; i < alternatingKeysAndValues.length; i += 2) {
        @SuppressWarnings("unchecked")
        K key = (K) alternatingKeysAndValues[i];
        @SuppressWarnings("unchecked")
        V value = (V) alternatingKeysAndValues[i + 1];
        action.accept(value, key);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable K get(@Nullable Object value) {
      if (value == null || tables == null) {
        return null;
      }
      Object result =
          lookup(tables, tables.length >>> 1, tables.length, alternatingKeysAndValues, 1, value);
      if (result == null) {
        return null;
      } else {
        return (K) result;
      }
    }

    @Override
    public ImmutableSet<V> keySet() {
      return new FlatKeySet<>(this, alternatingKeysAndValues, 1);
    }

    @Override
    public ImmutableSet<Entry<V, K>> entrySet() {
      return new FlatEntrySet<>(this, alternatingKeysAndValues, 1);
    }

    @Override
    boolean isHashCodeFast() {
      return true;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    boolean isPartialView() {
      return false;
    }

    @Override
    @J2ktIncompatible
    @GwtIncompatible
        Object writeReplace() {
      return new InverseSerializedForm<>(RegularImmutableBiMap.this);
    }

    @J2ktIncompatible // java.io.ObjectInputStream
    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
      throw new InvalidObjectException("Use InverseSerializedForm");
    }
  }

  @J2ktIncompatible // serialization
  private static final class InverseSerializedForm<K, V> implements Serializable {
    private final ImmutableBiMap<K, V> forward;

    InverseSerializedForm(ImmutableBiMap<K, V> forward) {
      this.forward = forward;
    }

    Object readResolve() {
      return forward.inverse();
    }

    @GwtIncompatible private static final long serialVersionUID = 1;
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

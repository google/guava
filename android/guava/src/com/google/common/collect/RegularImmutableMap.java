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

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static com.google.common.collect.SwarTables.STRIDE_16;
import static com.google.common.collect.SwarTables.STRIDE_24;
import static com.google.common.collect.SwarTables.STRIDE_32;
import static com.google.common.collect.SwarTables.TIER2_MAX_SIZE;
import static com.google.common.collect.SwarTables.TIER3_MAX_SIZE;
import static com.google.common.collect.SwarTables.broadcastH2Top7;
import static com.google.common.collect.SwarTables.broadcastTag;
import static com.google.common.collect.SwarTables.emptyLanes;
import static com.google.common.collect.SwarTables.firstLane;
import static com.google.common.collect.SwarTables.groupMaskForStride;
import static com.google.common.collect.SwarTables.h2Top7;
import static com.google.common.collect.SwarTables.hasEmptyLane;
import static com.google.common.collect.SwarTables.matchTags;
import static com.google.common.collect.SwarTables.read16BitIndex;
import static com.google.common.collect.SwarTables.read24BitIndex;
import static com.google.common.collect.SwarTables.read32BitIndex;
import static com.google.common.collect.SwarTables.readIndexForStride;
import static com.google.common.collect.SwarTables.strideForSize;
import static com.google.common.collect.SwarTables.writeSlot;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of {@link ImmutableMap} used for 0 entries and for 2+ entries. Additional
 * implementations exist for particular cases, like {@link ImmutableTable} views and hash flooding.
 *
 * <p><strong>Single-class layout:</strong> Every non-singleton, non-flooded map uses this single
 * concrete class with just two instance fields ({@link #alternatingKeysAndValues} and {@link
 * #hashTable}). Keeping a single concrete class (rather than separate subclasses per table tier)
 * keeps call sites that mix {@link SingletonImmutableBiMap} and {@code RegularImmutableMap}
 * bimorphic ({@code TypeProfileWidth == 2}) so HotSpot C2 can inline {@link #get} cleanly, keeps
 * instances compact (16 bytes under Compact Object Headers), and preserves single-type
 * compatibility for field-level serializers.
 *
 * <p><strong>Flat entry storage:</strong> Entries are stored in insertion order in {@link
 * #alternatingKeysAndValues} as {@code [k0, v0, k1, v1, ...]}, trimmed to exact length {@code 2 *
 * size}. This eliminates per-entry {@link ImmutableMapEntry} allocations at construction time;
 * {@link #entrySet} materializes {@link SimpleImmutableEntry} instances on demand (which HotSpot C2
 * scalar-replaces in typical iteration loops).
 *
 * <p><strong>Two-tier open-addressed hash table:</strong> {@link #hashTable} adapts to map size
 * {@code n}:
 *
 * <ul>
 *   <li>{@code n == 0}: {@code hashTable == null} and {@code alternatingKeysAndValues ==
 *       EMPTY_KV_ARRAY}.
 *   <li><strong>Tier 1 ({@code 1 <= n <= 255}) — linear-probing {@code byte[]}:</strong> Power-of-2
 *       length ({@code 2..512}) at {@code <= 70%} load factor. Each slot stores a 0-based entry
 *       index ({@code 0..254}, referencing {@code alternatingKeysAndValues[2 * idx]}) or {@link
 *       #ABSENT_BYTE} ({@code -1}, unsigned {@code 0xFF}) for empty slots. Storing 0-based indices
 *       with {@code -1} as the sentinel avoids subtracting 1 on every probe hit and produces
 *       tighter HotSpot C2 code than 1-based indices with a {@code 0} sentinel.
 *   <li><strong>Tier 2 ({@code n >= 256}) — interleaved 8-slot SWAR SwissTable in {@code
 *       long[]}:</strong> Power-of-2 number of 8-slot groups at {@code <= 6/8 = 75%} load factor,
 *       probed by triangular numbers ({@code g, g+1, g+3, g+6, ...}). Each group places its 8
 *       control bytes in word 0 ({@code 0x00} = empty, {@code 0x80..0xFF} = full with 7-bit H2 tag)
 *       immediately followed by its 8 entry indices in the same cache line:
 *       <ul>
 *         <li><em>SWAR-16 ({@code 256 <= n <= 65,536}):</em> stride 3 longs/group (1 tag word + 2
 *             words packing four unsigned 16-bit entry indices each).
 *         <li><em>SWAR-24 ({@code 65,537 <= n <= 16,777,216}):</em> stride 4 longs/group (1 tag
 *             word + 3 words packing eight unsigned 24-bit entry indices).
 *         <li><em>SWAR-32 ({@code n > 16,777,216}):</em> stride 5 longs/group (1 tag word + 4 words
 *             packing two 32-bit entry indices each).
 *       </ul>
 *       The group count is recovered from {@code table.length} (via {@code table.length >>> 2} for
 *       SWAR-24, or {@link Integer#highestOneBit} in {@link #swarShortGroupMask} and {@link
 *       #swarIntGroupMask}), avoiding a mask field on the map object.
 * </ul>
 *
 * <p><strong>Hash-flooding protection:</strong> If construction exceeds {@link #maxLinearProbes} in
 * Tier 1 or {@link #MAX_SWAR_PROBE_GROUPS} probe steps in Tier 2, it aborts with {@link
 * BucketOverflowException} and {@link #create} falls back to {@link JdkBackedImmutableMap}.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @author Gregory Kick
 */
@GwtCompatible
final class RegularImmutableMap<K, V> extends ImmutableMap<K, V> {
  private static final Object[] EMPTY_KV_ARRAY = new Object[0];

  @SuppressWarnings(
      "unchecked") // EMPTY contains no entries, so any <K, V> parameterization is safe
  static final ImmutableMap<Object, Object> EMPTY =
      new RegularImmutableMap<>(/* hashTable= */ null, EMPTY_KV_ARRAY);

  /**
   * Maximum allowed false positive probability of detecting a hash flooding attack given random
   * input.
   */
  @VisibleForTesting static final double HASH_FLOODING_FPP = 0.001;

  /**
   * Maximum allowed length of a hash table bucket before falling back to a {@code
   * java.util.HashMap}-based implementation. Still used by {@link RegularImmutableBiMap}.
   */
  static final int MAX_HASH_BUCKET_LENGTH = 8;

  /**
   * Sentinel stored in empty slots of a Tier-1 {@code byte[]} hash table ({@code 0xFF} unsigned).
   *
   * <p>Using {@code -1} as the absent sentinel allows occupied slots to hold 0-based entry indices
   * {@code 0..254}, avoiding a {@code - 1} adjustment before indexing {@link
   * #alternatingKeysAndValues} on the {@link #get} hot path.
   */
  private static final byte ABSENT_BYTE = -1;

  private static final int BYTE_MASK = 0xFF;

  /**
   * Maximum map size backed by a Tier-1 {@code byte[]} linear-probing table (0-based entry indices
   * {@code 0..254}, reserving {@code 255}/{@code 0xFF} for {@link #ABSENT_BYTE}). Maps of size
   * {@code >= 256} use the Tier-2 interleaved SWAR {@code long[]} table.
   */
  private static final int LINEAR_MAX_SIZE = 255;

  /**
   * Maximum triangular-number probe steps after the initial group in SWAR probing (17 groups / 136
   * slots total) before declaring hash flooding and falling back to {@link JdkBackedImmutableMap}.
   */
  private static final int MAX_SWAR_PROBE_GROUPS = 16;

  /**
   * Fallback power-of-2 SWAR group count if {@code highestOneBit(minGroups - 1) << 1} ever
   * overflows {@code int} (for {@code size <= MAX_ENTRIES}, {@code numGroups <= 2^28} and {@code 5
   * * 2^28 < Integer.MAX_VALUE}).
   */
  private static final int MAX_SWAR_GROUPS = 1 << 27;

  /**
   * Flat alternating keys and values in insertion order ({@code [k0, v0, k1, v1, ...]}), with no
   * {@code null} elements and always trimmed to exact length {@code 2 * size}.
   */
  @VisibleForTesting final transient Object[] alternatingKeysAndValues;

  /**
   * Backing hash table: {@code null} for {@link #EMPTY}, {@code byte[]} for {@code 1 <= n <= 255},
   * or interleaved SWAR {@code long[]} for {@code n >= 256}.
   */
  private final transient @Nullable Object hashTable;

  static <K, V> ImmutableMap<K, V> fromEntries(Entry<K, V>... entries) {
    return fromEntryArray(entries.length, entries, /* throwIfDuplicateKeys= */ true);
  }

  /**
   * Creates an {@link ImmutableMap} from the first {@code n} entries in {@code entryArray}, copying
   * their keys and values into a flat {@code alternatingKeysAndValues} array.
   */
  static <K, V> ImmutableMap<K, V> fromEntryArray(
      int n, @Nullable Entry<K, V>[] entryArray, boolean throwIfDuplicateKeys) {
    checkPositionIndex(n, entryArray.length);
    if (n == 0) {
      @SuppressWarnings("unchecked") // EMPTY contains no entries
      ImmutableMap<K, V> empty = (ImmutableMap<K, V>) EMPTY;
      return empty;
    }
    checkMaxEntryCount(n);
    @Nullable Object[] kv = new @Nullable Object[n * 2];
    for (int i = 0; i < n; i++) {
      Entry<K, V> entry = requireNonNull(entryArray[i]);
      K key = entry.getKey();
      V value = entry.getValue();
      checkEntryNotNull(key, value);
      kv[i << 1] = key;
      kv[(i << 1) + 1] = value;
    }
    return create(n, kv, throwIfDuplicateKeys, /* builder= */ null);
  }

  /**
   * Creates an {@link ImmutableMap} from the first {@code n} key-value pairs in {@code
   * alternatingKeysAndValues}, throwing on duplicate keys.
   *
   * <p>The array must have length at least {@code 2 * n} with non-null elements in {@code [0, 2 *
   * n)}. Takes ownership of {@code alternatingKeysAndValues} without copying when its length is
   * already {@code 2 * n} and no duplicates are removed.
   */
  static <K, V> ImmutableMap<K, V> create(int n, @Nullable Object[] alternatingKeysAndValues) {
    return create(
        n, alternatingKeysAndValues, /* throwIfDuplicateKeys= */ true, /* builder= */ null);
  }

  /**
   * Creates an {@link ImmutableMap} from the first {@code n} key-value pairs in {@code
   * alternatingKeysAndValues}, falling back to {@link JdkBackedImmutableMap} if hash flooding is
   * detected.
   *
   * <p>When {@code throwIfDuplicateKeys} is {@code false} (for {@link Builder#buildKeepingLast}),
   * duplicate keys retain their first-inserted position with their last-put value, and the first
   * duplicate encountered is recorded on {@code builder} (if non-null) so a later {@link
   * Builder#buildOrThrow} fails immediately. Never mutates {@code alternatingKeysAndValues} in
   * place if it is shared with a {@link Builder}.
   */
  static <K, V> ImmutableMap<K, V> create(
      int n,
      @Nullable Object[] alternatingKeysAndValues,
      boolean throwIfDuplicateKeys,
      @Nullable Builder<K, V> builder) {
    checkPositionIndex(n, alternatingKeysAndValues.length >>> 1);
    if (n == 0) {
      @SuppressWarnings("unchecked") // EMPTY contains no entries
      ImmutableMap<K, V> empty = (ImmutableMap<K, V>) EMPTY;
      return empty;
    }
    return createCheckingFlooding(n, alternatingKeysAndValues, throwIfDuplicateKeys, builder);
  }

  private RegularImmutableMap(@Nullable Object hashTable, Object[] alternatingKeysAndValues) {
    this.hashTable = hashTable;
    this.alternatingKeysAndValues = alternatingKeysAndValues;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The {@code byte[]} linear-probing fast path is kept inline here (125 bytecodes total) so
   * HotSpot C2 can inline {@code RegularImmutableMap.get} bimorphically alongside {@link
   * SingletonImmutableBiMap#get}, while the {@code long[]} SWAR path is outlined into {@link
   * #getSwar}.
   */
  @Override
  @SuppressWarnings("unchecked") // alternatingKeysAndValues stores K at even indices and V at odd
  public @Nullable V get(@Nullable Object key) {
    Object hashTableObject = this.hashTable;
    if (key == null || hashTableObject == null) {
      return null;
    }
    Object[] kv = this.alternatingKeysAndValues;
    int smearedHash = Hashing.smear(key.hashCode());
    if (hashTableObject instanceof byte[]) {
      byte[] hashTable = (byte[]) hashTableObject;
      int mask = hashTable.length - 1;
      for (int slot = smearedHash & mask; ; slot = (slot + 1) & mask) {
        int entry = hashTable[slot] & BYTE_MASK;
        if (entry == BYTE_MASK) {
          return null;
        }
        int keyIndex = entry << 1;
        if (key.equals(kv[keyIndex])) {
          return (V) kv[keyIndex + 1];
        }
      }
    }
    return (V) getSwar((long[]) hashTableObject, kv, key, smearedHash);
  }

  /** Computes the map hash code directly over {@link #alternatingKeysAndValues}. */
  @Override
  public int hashCode() {
    int hash = 0;
    Object[] kv = this.alternatingKeysAndValues;
    for (int i = 0; i < kv.length; i += 2) {
      hash += kv[i].hashCode() ^ kv[i + 1].hashCode();
    }
    return hash;
  }

  /** Scans odd indices of {@link #alternatingKeysAndValues} directly without creating a view. */
  @Override
  public boolean containsValue(@Nullable Object value) {
    if (value == null) {
      return false;
    }
    Object[] kv = this.alternatingKeysAndValues;
    for (int i = 1; i < kv.length; i += 2) {
      if (value.equals(kv[i])) {
        return true;
      }
    }
    return false;
  }

  /**
   * Compares directly against {@code object} by scanning {@link #alternatingKeysAndValues},
   * avoiding {@link EntrySet} and {@link SimpleImmutableEntry} allocations.
   */
  @Override
  public boolean equals(@Nullable Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof Map)) {
      return false;
    }
    Map<?, ?> other = (Map<?, ?>) object;
    Object[] kv = this.alternatingKeysAndValues;
    if ((kv.length >>> 1) != other.size()) {
      return false;
    }
    try {
      for (int i = 0; i < kv.length; i += 2) {
        // Values in this map are always non-null, so a null return from other.get() never matches.
        if (!kv[i + 1].equals(other.get(kv[i]))) {
          return false;
        }
      }
    } catch (ClassCastException | NullPointerException unused) {
      return false;
    }
    return true;
  }

  @Override
  public int size() {
    return alternatingKeysAndValues.length >>> 1;
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  public ImmutableSet<Entry<K, V>> entrySet() {
    return hashTable == null
        ? ImmutableSet.of()
        : new FlatEntrySet<>(this, alternatingKeysAndValues, 0);
  }

  @Override
  public ImmutableSet<K> keySet() {
    return hashTable == null
        ? ImmutableSet.of()
        : new FlatKeySet<>(this, alternatingKeysAndValues, 0);
  }

  /*
   * While we *could* declare a return type of ImmutableList on this package-private implementation
   * type, no user would see it, so all we'd end up with is an extra method in the class file.
   *
   * (Arguably it would have been even better not to have returned an ImmutableList in the first
   * place. It's especially a bit strange given that ImmutableMap.of(k, v).values() (a
   * SingletonImmutableBiMap) returns an ImmutubleSet. Surely users have already come to depend on
   * these implementation details, such as through `equals` calls. Maybe we should have returned a
   * more vanilla ImmutableCollection with a fast asList() method?))
   */
  @SuppressWarnings("PreferredInterfaceType")
  @Override
  public ImmutableCollection<V> values() {
    return hashTable == null ? ImmutableList.of() : new Values<>(this);
  }

  /** Values view reading odd indices of {@link RegularImmutableMap#alternatingKeysAndValues}. */
  private static final class Values<K, V> extends ImmutableList<V> {
    final RegularImmutableMap<K, V> map;

    Values(RegularImmutableMap<K, V> map) {
      this.map = map;
    }

    @Override
    @SuppressWarnings("unchecked") // alternatingKeysAndValues stores V at odd indices
    public V get(int index) {
      // Bounds check before shifting to prevent index << 1 wraparound (e.g. 1 << 30).
      checkElementIndex(index, map.size());
      return (V) map.alternatingKeysAndValues[(index << 1) + 1];
    }

    @Override
    @GwtIncompatible // not used in GWT
    int copyIntoArray(@Nullable Object[] dst, int offset) {
      Object[] kv = map.alternatingKeysAndValues;
      for (int i = 1; i < kv.length; i += 2) {
        dst[offset++] = kv[i];
      }
      return offset;
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    boolean isPartialView() {
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

    // No longer used for new writes, but kept so that old data can still be read.
    @GwtIncompatible
    @J2ktIncompatible
    @SuppressWarnings("unused")
    private static final class SerializedForm<V> implements Serializable {
      final ImmutableMap<?, V> map;

      SerializedForm(ImmutableMap<?, V> map) {
        this.map = map;
      }

      Object readResolve() {
        return map.values();
      }

      private static final long serialVersionUID = 0;
    }
  }

  // redeclare to help optimizers with b/310253115
  @SuppressWarnings("RedundantOverride")
  @Override
  @J2ktIncompatible
  @GwtIncompatible
    Object writeReplace() {
    return super.writeReplace();
  }

  // This class is never actually serialized directly, but we have to make the
  // warning go away (and suppressing would suppress for all nested classes too)
  @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;

  // -------------------------------------------------------------------------
  // Private construction helpers
  // -------------------------------------------------------------------------

  /**
   * Trims {@code rawKv} if over-allocated and builds either a Tier-1 {@code byte[]} table ({@code n
   * <= 255}) or a Tier-2 SWAR {@code long[]} table ({@code n >= 256}).
   */
  private static <K, V> ImmutableMap<K, V> createCheckingFlooding(
      int n,
      @Nullable Object[] rawKv,
      boolean throwIfDuplicateKeys,
      @Nullable Builder<K, V> builder) {
    // Always trim to exact length 2 * n so the built map never retains over-allocated builder
    // capacity, and so deduplication knows whether it already owns a private copy of kv.
    boolean ownsKv = rawKv.length != (n << 1);
    @SuppressWarnings("nullness") // first 2 * n elements are verified non-null by callers
    Object[] kv = ownsKv ? Arrays.copyOf(rawKv, n << 1) : (Object[]) rawKv;

    if (n <= LINEAR_MAX_SIZE) {
      int tableSize = chooseLinearTableSize(n);
      int mask = tableSize - 1;
      int maxProbes = maxLinearProbes(tableSize);
      return createLinearByte(
          kv, n, tableSize, mask, maxProbes, ownsKv, throwIfDuplicateKeys, builder);
    } else {
      int stride = n <= TIER2_MAX_SIZE ? STRIDE_16 : strideForSize(n);
      return createSwar(kv, n, stride, ownsKv, throwIfDuplicateKeys, builder);
    }
  }

  /**
   * Builds a Tier-1 linear-probing {@code byte[]} map for {@code 1 <= n <= 255}.
   *
   * <p>When {@code throwIfDuplicateKeys} is {@code false}, duplicate keys overwrite the value at
   * their first-inserted slot (cloning {@code kv} first if not already owned) and subsequent unique
   * entries are compacted left in {@code kv} so stored 0-based indices stay contiguous.
   */
  private static <K, V> ImmutableMap<K, V> createLinearByte(
      Object[] kv,
      int n,
      int tableSize,
      int mask,
      int maxProbes,
      boolean ownsKv,
      boolean throwIfDuplicateKeys,
      @Nullable Builder<K, V> builder) {
    byte[] hashTable = new byte[tableSize];
    Arrays.fill(hashTable, ABSENT_BYTE);
    Builder.DuplicateKey firstDuplicate = null;
    int uniqueCount = 0;
    entries:
    for (int i = 0; i < n; i++) {
      int kvIndex = i << 1;
      Object key = requireNonNull(kv[kvIndex]);
      Object value = requireNonNull(kv[kvIndex + 1]);
      int smearedHash = Hashing.smear(key.hashCode());
      int probes = 0;
      for (int slot = smearedHash & mask; ; slot = (slot + 1) & mask) {
        int existingEntryIndex = hashTable[slot] & BYTE_MASK;
        if (existingEntryIndex == BYTE_MASK) {
          hashTable[slot] = (byte) uniqueCount;
          break;
        }
        int existingKvIndex = existingEntryIndex << 1;
        if (key.equals(kv[existingKvIndex])) {
          Builder.DuplicateKey duplicate =
              new Builder.DuplicateKey(key, value, kv[existingKvIndex + 1]);
          if (throwIfDuplicateKeys) {
            throw duplicate.exception();
          }
          if (firstDuplicate == null) {
            firstDuplicate = duplicate;
          }
          if (!ownsKv) {
            kv = kv.clone();
            ownsKv = true;
          }
          kv[existingKvIndex + 1] = value;
          continue entries;
        }
      }
      if (uniqueCount < i) {
        int destKvIndex = uniqueCount << 1;
        kv[destKvIndex] = key;
        kv[destKvIndex + 1] = value;
      }
      uniqueCount++;
    }
    if (firstDuplicate != null) {
      return finishAfterDeduplication(
          hashTable,
          kv,
          uniqueCount,
          /* needsRehash= */ chooseLinearTableSize(uniqueCount) != tableSize,
          firstDuplicate,
          builder);
    }
    return new RegularImmutableMap<>(hashTable, kv);
  }

  /**
   * Builds a Tier-2 interleaved SWAR SwissTable map for {@code n >= 256}, using 16-bit ({@link
   * #SWAR_SHORT_GROUP_STRIDE}), 24-bit ({@link #SWAR_24_GROUP_STRIDE}), or 32-bit ({@link
   * #SWAR_INT_GROUP_STRIDE}) entry indices according to {@code stride}.
   *
   * <p>Groups are probed by triangular numbers ({@code g, g+1, g+3, g+6, ...}), which visit every
   * group in a power-of-2 table without cycling. Newly allocated {@code long[]} arrays are
   * zero-filled by the JVM, which already matches the {@code 0x00} empty-lane tag encoding.
   */
  private static <K, V> ImmutableMap<K, V> createSwar(
      Object[] kv,
      int n,
      int stride,
      boolean ownsKv,
      boolean throwIfDuplicateKeys,
      @Nullable Builder<K, V> builder) {
    int numGroups = chooseSwarNumGroups(n);
    int groupMask = numGroups - 1;
    long[] table = new long[numGroups * stride];
    Builder.DuplicateKey firstDuplicate = null;
    int uniqueCount = 0;
    entries:
    for (int i = 0; i < n; i++) {
      int kvIndex = i << 1;
      Object key = requireNonNull(kv[kvIndex]);
      Object value = requireNonNull(kv[kvIndex + 1]);
      int smearedHash = Hashing.smear(key.hashCode());
      int tag = h2Top7(smearedHash);
      long broadcastTag = broadcastTag(tag);
      int groupIndex = smearedHash & groupMask;
      int probeStep = 0;
      while (true) {
        int groupBase = groupIndex * stride;
        long groupTags = table[groupBase];
        for (long matchMask = matchTags(groupTags, broadcastTag);
            matchMask != 0L;
            matchMask &= matchMask - 1) {
          int lane = firstLane(matchMask);
          int existingEntryIndex = readIndexForStride(table, groupBase, stride, lane);
          int existingKvIndex = existingEntryIndex << 1;
          if (key.equals(kv[existingKvIndex])) {
            Builder.DuplicateKey duplicate =
                new Builder.DuplicateKey(key, value, kv[existingKvIndex + 1]);
            if (throwIfDuplicateKeys) {
              throw duplicate.exception();
            }
            if (firstDuplicate == null) {
              firstDuplicate = duplicate;
            }
            if (!ownsKv) {
              kv = kv.clone();
              ownsKv = true;
            }
            kv[existingKvIndex + 1] = value;
            continue entries;
          }
        }
        long emptyMask = emptyLanes(groupTags);
        if (emptyMask != 0L) {
          writeSlot(table, groupBase, stride, groupTags, tag, firstLane(emptyMask), uniqueCount);
          break;
        }
        ++probeStep;
        groupIndex = (groupIndex + probeStep) & groupMask;
      }
      if (uniqueCount < i) {
        int destKvIndex = uniqueCount << 1;
        kv[destKvIndex] = key;
        kv[destKvIndex + 1] = value;
      }
      uniqueCount++;
    }
    if (firstDuplicate != null) {
      int targetStride = uniqueCount <= TIER2_MAX_SIZE ? STRIDE_16 : strideForSize(uniqueCount);
      boolean needsRehash =
          uniqueCount <= LINEAR_MAX_SIZE
              || targetStride != stride
              || chooseSwarNumGroups(uniqueCount) != numGroups;
      return finishAfterDeduplication(table, kv, uniqueCount, needsRehash, firstDuplicate, builder);
    }
    return new RegularImmutableMap<>(table, kv);
  }

  /**
   * Finalizes map construction after {@link Builder#buildKeepingLast} deduplicated one or more
   * keys.
   *
   * <p>Records {@code firstDuplicate} on {@code builder} so any subsequent {@link
   * Builder#buildOrThrow} fails immediately, trims {@code kv} to exact length {@code 2 *
   * uniqueCount}, and rehashes when {@code needsRehash} is {@code true} so the map neither retains
   * an oversized hash table nor violates tier-dispatch invariants (notably, {@link #getSwar}
   * chooses SWAR-16 vs. SWAR-32 based on {@code kv.length}, so shrinking across {@link
   * #SWAR_SHORT_MAX_SIZE} must rehash into stride-3 groups).
   */
  private static <K, V> ImmutableMap<K, V> finishAfterDeduplication(
      Object hashTable,
      Object[] kv,
      int uniqueCount,
      boolean needsRehash,
      Builder.DuplicateKey firstDuplicate,
      @Nullable Builder<K, V> builder) {
    if (builder != null) {
      builder.duplicateKey = firstDuplicate;
    }
    Object[] trimmedKv = Arrays.copyOf(kv, uniqueCount << 1);
    if (needsRehash) {
      return createCheckingFlooding(
          uniqueCount, trimmedKv, /* throwIfDuplicateKeys= */ true, /* builder= */ null);
    }
    return new RegularImmutableMap<>(hashTable, trimmedKv);
  }

  /**
   * Chooses the smallest power-of-2 {@code byte[]} table size {@code >= 2} keeping load factor
   * {@code <= 70%}.
   */
  private static int chooseLinearTableSize(int size) {
    // Exact integer ceiling of size / 0.7 is (size * 10 + 6) / 7; clamp to >= 2 for size == 1.
    int minSlots = Math.max((size * 10 + 6) / 7, 2);
    return Integer.highestOneBit(minSlots - 1) << 1;
  }

  /**
   * Returns the maximum linear-probe displacement allowed for a Tier-1 {@code byte[]} table of size
   * {@code tableSize} before declaring hash flooding.
   *
   * <p>Calibrated for {@code <= 70%} load factor so random keys trigger a false positive with
   * probability {@code <=} {@link #HASH_FLOODING_FPP} ({@code 0.001}), while adversarial hash
   * collisions abort quickly rather than degrading to quadratic construction time.
   */
  private static int maxLinearProbes(int tableSize) {
    if (tableSize <= 64) {
      return 32;
    } else if (tableSize <= 128) {
      return 48;
    } else {
      return 72;
    }
  }

  /**
   * Chooses the smallest power-of-2 number of 8-slot SWAR groups keeping load factor {@code <= 6/8
   * = 75%}.
   *
   * <p>Capping occupancy at 6 of 8 slots per group (rather than 7 of 8) guarantees at least 2 empty
   * lanes per group on average, keeping triangular-number probe chains short and allowing misses to
   * terminate in the first group with high probability.
   */
  private static int chooseSwarNumGroups(int size) {
    int minGroups = (size + 5) / 6;
    if (minGroups <= 1) {
      return 1;
    }
    int numGroups = Integer.highestOneBit(minGroups - 1) << 1;
    return numGroups > 0 ? numGroups : MAX_SWAR_GROUPS;
  }

  // -------------------------------------------------------------------------
  // Private SWAR lookup and bit-manipulation helpers
  // -------------------------------------------------------------------------

  /**
   * Probes a Tier-2 interleaved SWAR {@code long[]} hash table for {@code key}, dispatching to
   * SWAR-16, SWAR-24, or SWAR-32 based on {@code kv.length}.
   *
   * <p>Outlined from {@link #get} so {@link #get} stays compact enough for HotSpot C2 bimorphic
   * inlining alongside {@link SingletonImmutableBiMap#get}.
   */
  private static @Nullable Object getSwar(long[] table, Object[] kv, Object key, int smearedHash) {
    long broadcastTag = broadcastH2Top7(smearedHash);
    int entryCount = kv.length >>> 1;
    if (entryCount <= TIER2_MAX_SIZE) {
      return probeSwarShort(table, kv, key, smearedHash, broadcastTag);
    } else if (entryCount <= TIER3_MAX_SIZE) {
      return probeSwar24(table, kv, key, smearedHash, broadcastTag);
    } else {
      return probeSwarInt(table, kv, key, smearedHash, broadcastTag);
    }
  }

  /** Probes a SWAR-16 table ({@link SwarTables#STRIDE_16} longs/group, 16-bit indices). */
  private static @Nullable Object probeSwarShort(
      long[] table, Object[] kv, Object key, int smearedHash, long broadcastTag) {
    int groupMask = groupMaskForStride(table.length, STRIDE_16);
    int groupIndex = smearedHash & groupMask;
    int probeStep = 0;
    while (true) {
      int groupBase = groupIndex * STRIDE_16;
      long groupTags = table[groupBase];
      for (long matchMask = matchTags(groupTags, broadcastTag);
          matchMask != 0L;
          matchMask &= matchMask - 1) {
        int entryIndex = read16BitIndex(table, groupBase, firstLane(matchMask));
        int kvIndex = entryIndex << 1;
        if (key.equals(kv[kvIndex])) {
          return kv[kvIndex + 1];
        }
      }
      if (hasEmptyLane(groupTags) || ++probeStep > MAX_SWAR_PROBE_GROUPS) {
        return null;
      }
      groupIndex = (groupIndex + probeStep) & groupMask;
    }
  }

  /** Probes a SWAR-24 table ({@link SwarTables#STRIDE_24} longs/group, 24-bit indices). */
  private static @Nullable Object probeSwar24(
      long[] table, Object[] kv, Object key, int smearedHash, long broadcastTag) {
    int groupMask = groupMaskForStride(table.length, STRIDE_24);
    int groupIndex = smearedHash & groupMask;
    int probeStep = 0;
    while (true) {
      int groupBase = groupIndex * STRIDE_24;
      long groupTags = table[groupBase];
      for (long matchMask = matchTags(groupTags, broadcastTag);
          matchMask != 0L;
          matchMask &= matchMask - 1) {
        int entryIndex = read24BitIndex(table, groupBase, firstLane(matchMask));
        int kvIndex = entryIndex << 1;
        if (key.equals(kv[kvIndex])) {
          return kv[kvIndex + 1];
        }
      }
      if (hasEmptyLane(groupTags) || ++probeStep > MAX_SWAR_PROBE_GROUPS) {
        return null;
      }
      groupIndex = (groupIndex + probeStep) & groupMask;
    }
  }

  /** Probes a SWAR-32 table ({@link SwarTables#STRIDE_32} longs/group, 32-bit indices). */
  private static @Nullable Object probeSwarInt(
      long[] table, Object[] kv, Object key, int smearedHash, long broadcastTag) {
    int groupMask = groupMaskForStride(table.length, STRIDE_32);
    int groupIndex = smearedHash & groupMask;
    int probeStep = 0;
    while (true) {
      int groupBase = groupIndex * STRIDE_32;
      long groupTags = table[groupBase];
      for (long matchMask = matchTags(groupTags, broadcastTag);
          matchMask != 0L;
          matchMask &= matchMask - 1) {
        int entryIndex = read32BitIndex(table, groupBase, firstLane(matchMask));
        int kvIndex = entryIndex << 1;
        if (key.equals(kv[kvIndex])) {
          return kv[kvIndex + 1];
        }
      }
      if (hasEmptyLane(groupTags) || ++probeStep > MAX_SWAR_PROBE_GROUPS) {
        return null;
      }
      groupIndex = (groupIndex + probeStep) & groupMask;
    }
  }
}

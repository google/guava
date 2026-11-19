/*
 * Copyright (C) 2026 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;

/**
 * Shared 64-bit SWAR (SIMD Within A Register) SwissTable control-word and packed-index primitives
 * for immutable hash-based collections.
 *
 * <p>Each 8-slot group occupies {@code stride} consecutive {@code long}s in a backing {@code
 * long[]} table:
 *
 * <ul>
 *   <li>{@code table[base]}: 64-bit control word containing 8 one-byte tags ({@code 0x00} for
 *       empty, {@code 0x80 | h2} for occupied with a 7-bit fingerprint).
 *   <li>{@code table[base + 1 .. base + stride - 1]}: 8 packed entry/element indices, sized by
 *       collection cardinality:
 *       <ul>
 *         <li>Tier 1 ({@code n <= 256}, {@code stride = 2}): 1 word of 8 × 8-bit indices.
 *         <li>Tier 2 ({@code n <= 65,536}, {@code stride = 3}): 2 words of 8 × 16-bit indices.
 *         <li>Tier 3 ({@code n <= 16,777,216}, {@code stride = 4}): branchless split-plane 24-bit
 *             indices (1 word of 8 high bytes in {@code table[base + 1]} + 2 words of 8 low 16-bit
 *             halves in {@code table[base + 2..3]}).
 *         <li>Tier 4 ({@code n > 16,777,216}, {@code stride = 5}): 4 words of 8 × 32-bit indices.
 *       </ul>
 * </ul>
 *
 * <p>All methods are small {@code static} helpers intended to be inlined by HotSpot C2 with zero
 * abstraction overhead.
 */
@GwtCompatible
final class SwarTables {
  private SwarTables() {}

  /** Broadcasts a byte across all 8 lanes of a {@code long} for SWAR comparisons. */
  static final long REPEAT_ONE = 0x0101_0101_0101_0101L;

  /** Selects bit 7 (the occupied/match bit) of each byte lane in a {@code long}. */
  static final long HIGH_BITS = 0x8080_8080_8080_8080L;

  static final int TIER1_MAX_SIZE = 256;
  static final int TIER2_MAX_SIZE = 65_536;
  static final int TIER3_MAX_SIZE = 16_777_216;

  static final int STRIDE_8 = 2;
  static final int STRIDE_16 = 3;
  static final int STRIDE_24 = 4;
  static final int STRIDE_32 = 5;

  /**
   * Computes the 8-bit control tag ({@code 0x80..0xFF}) from the top 7 bits of {@code smearedHash}.
   * Used when initial group selection consumes the low bits ({@code smearedHash & groupMask}).
   */
  static int h2Top7(int smearedHash) {
    return (smearedHash >>> 25) | 0x80;
  }

  /**
   * Computes the 8-bit control tag ({@code 0x80..0xFF}) from the low 7 bits of {@code smearedHash}.
   * Used when initial group selection consumes the high bits (e.g. Lemire's fast-range).
   */
  static int h2Low7(int smearedHash) {
    return (smearedHash & 0x7F) | 0x80;
  }

  /** Replicates a single control byte {@code h2} across all 8 byte lanes of a {@code long}. */
  static long broadcastTag(int h2) {
    return h2 * REPEAT_ONE;
  }

  /** Broadcasts {@link #h2Top7(int)} across all 8 byte lanes of a {@code long}. */
  static long broadcastH2Top7(int smearedHash) {
    return h2Top7(smearedHash) * REPEAT_ONE;
  }

  /** Broadcasts {@link #h2Low7(int)} across all 8 byte lanes of a {@code long}. */
  static long broadcastH2Low7(int smearedHash) {
    return h2Low7(smearedHash) * REPEAT_ONE;
  }

  /**
   * Returns a bitmask with bit 7 ({@code 0x80}) set in each byte lane of {@code ctrl} equal to the
   * broadcast byte in {@code broadcastH2}.
   *
   * <p>Applies Mycroft's zero-byte detector to {@code diff = ctrl ^ broadcastH2}. Because every
   * empty lane has {@code ctrl_k == 0x00} and {@code h2 >= 0x80}, {@code diff_k} always has bit 7
   * set on empty lanes ({@code ~diff_k & 0x80 == 0}), so cross-lane borrows can never cause a false
   * positive on an empty lane.
   */
  static long matchTags(long ctrl, long broadcastH2) {
    long diff = ctrl ^ broadcastH2;
    return (diff - REPEAT_ONE) & ~diff & HIGH_BITS;
  }

  /**
   * Returns a bitmask with bit 7 set in each empty ({@code 0x00}) byte lane of {@code ctrl}. Relies
   * on occupied lanes always having bit 7 set ({@code 0x80..0xFF}).
   */
  static long emptyLanes(long ctrl) {
    return ~ctrl & HIGH_BITS;
  }

  /** Returns {@code true} if {@code ctrl} contains at least one empty ({@code 0x00}) lane. */
  static boolean hasEmptyLane(long ctrl) {
    return (ctrl & HIGH_BITS) != HIGH_BITS;
  }

  /**
   * Returns the lane index ({@code 0..7}) corresponding to the lowest set {@code 0x80} bit in
   * {@code mask}.
   */
  static int firstLane(long mask) {
    return Long.numberOfTrailingZeros(mask) >>> 3;
  }

  /** Returns the index-width tier ({@code 1..4}) required to address {@code size} elements. */
  static int tierForSize(int size) {
    if (size <= TIER1_MAX_SIZE) {
      return 1;
    } else if (size <= TIER2_MAX_SIZE) {
      return 2;
    } else if (size <= TIER3_MAX_SIZE) {
      return 3;
    } else {
      return 4;
    }
  }

  /** Returns the group stride in {@code long}s ({@code 2..5}) required for {@code size} items. */
  static int strideForSize(int size) {
    return tierForSize(size) + 1;
  }

  /**
   * Recovers {@code numGroups - 1} from {@code tableLength = stride * numGroups} for power-of-two
   * {@code numGroups} without storing a mask field or dividing. Even strides ({@code 2, 4}) shift
   * right; odd strides ({@code 3, 5}) isolate the lowest set bit ({@code L & -L == numGroups}).
   */
  static int groupMaskForStride(int tableLength, int stride) {
    switch (stride) {
      case STRIDE_8:
        return (tableLength >>> 1) - 1;
      case STRIDE_24:
        return (tableLength >>> 2) - 1;
      default:
        return (tableLength & -tableLength) - 1;
    }
  }

  /** Reads an 8-bit element index from a packed index word. */
  static int read8BitIndex(long indicesWord, int lane) {
    return (int) ((indicesWord >>> (lane << 3)) & 0xFFL);
  }

  /** Reads a 16-bit element index from {@code table[base + 1 .. base + 2]}. */
  static int read16BitIndex(long[] table, int base, int lane) {
    return (int) ((table[base + 1 + (lane >>> 2)] >>> ((lane & 3) << 4)) & 0xFFFFL);
  }

  /**
   * Reads a split-plane 24-bit element index from {@code table[base + 1 .. base + 3]}, where {@code
   * table[base + 1]} holds the 8 high bytes and {@code table[base + 2..3]} hold the 8 low 16-bit
   * halves.
   */
  static int read24BitIndex(long[] table, int base, int lane) {
    return (read8BitIndex(table[base + 1], lane) << 16) | read16BitIndex(table, base + 1, lane);
  }

  /** Reads a 32-bit element index from {@code table[base + 1 .. base + 4]}. */
  static int read32BitIndex(long[] table, int base, int lane) {
    return (int) (table[base + 1 + (lane >>> 1)] >>> ((lane & 1) << 5));
  }

  /** Reads an element index for {@code stride} ({@code 2..5}) at {@code lane}. */
  static int readIndexForStride(long[] table, int base, int stride, int lane) {
    switch (stride) {
      case STRIDE_8:
        return read8BitIndex(table[base + 1], lane);
      case STRIDE_16:
        return read16BitIndex(table, base, lane);
      case STRIDE_24:
        return read24BitIndex(table, base, lane);
      default:
        return read32BitIndex(table, base, lane);
    }
  }

  /**
   * Writes both the 8-bit control {@code h2} tag and the packed {@code index} into {@code lane}.
   */
  static void writeSlot(
      long[] table, int base, int stride, long ctrl, int h2, int lane, int index) {
    int byteShift = lane << 3;
    table[base] = ctrl | (((long) h2) << byteShift);
    switch (stride) {
      case STRIDE_8:
        table[base + 1] |= (index & 0xFFL) << byteShift;
        break;
      case STRIDE_16:
        table[base + 1 + (lane >>> 2)] |= (index & 0xFFFFL) << ((lane & 3) << 4);
        break;
      case STRIDE_24:
        table[base + 1] |= ((long) (index >>> 16)) << byteShift;
        table[base + 2 + (lane >>> 2)] |= (index & 0xFFFFL) << ((lane & 3) << 4);
        break;
      default:
        table[base + 1 + (lane >>> 1)] |= (index & 0xFFFF_FFFFL) << ((lane & 1) << 5);
        break;
    }
  }
}

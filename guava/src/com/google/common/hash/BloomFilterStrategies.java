// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.hash;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.math.IntMath;

import java.math.RoundingMode;

/**
 * Collections of strategies of generating the {@code k * log(M)} bits required for an element to
 * be mapped to a {@link BloomFilter} of {@code M} bits and {@code k} hash functions. These
 * strategies are part of the serialized form of the Bloom filters that use them, thus they must be
 * preserved as is (no updates allowed, only introduction of new versions).
 *
 * @author andreou@google.com (Dimitris Andreou)
 */
enum BloomFilterStrategies implements BloomFilter.Strategy {
  /**
   * See "Less Hashing, Same Performance: Building a Better Bloom Filter" by Adam Kirsch and
   * Michael Mitzenmacher. The paper argues that this trick doesn't significantly deteriorate the
   * performance of a Bloom filter (yet only needs two 32bit hash functions).
   */
  MURMUR128_MITZ_32() {
    @Override public <T> void put(T object, Funnel<? super T> funnel,
        int numHashFunctions, BitArray bits) {
      // TODO(user): when the murmur's shortcuts are implemented, update this code
      long hash64 = Hashing.murmur3_128().newHasher().putObject(object, funnel).hash().asLong();
      int hash1 = (int) hash64;
      int hash2 = (int) (hash64 >>> 32);
      for (int i = 1; i <= numHashFunctions; i++) {
        int nextHash = hash1 + i * hash2;
        if (nextHash < 0) {
          nextHash = ~nextHash;
        }
        // up to here, the code is identical with the next method
        bits.set(nextHash % bits.size());
      }
    }

    @Override public <T> boolean mightContain(T object, Funnel<? super T> funnel,
        int numHashFunctions, BitArray bits) {
      long hash64 = Hashing.murmur3_128().newHasher().putObject(object, funnel).hash().asLong();
      int hash1 = (int) hash64;
      int hash2 = (int) (hash64 >>> 32);
      for (int i = 1; i <= numHashFunctions; i++) {
        int nextHash = hash1 + i * hash2;
        if (nextHash < 0) {
          nextHash = ~nextHash;
        }
        // up to here, the code is identical with the previous method
        if (!bits.get(nextHash % bits.size())) {
          return false;
        }
      }
      return true;
    }
  };

  static class BitArray {
    final long[] data;

    BitArray(int bits) {
      this(new long[IntMath.divide(bits, 64, RoundingMode.CEILING)]);
    }

    // Used by serialization
    BitArray(long[] data) {
      checkArgument(data.length > 0, "data length is zero!");
      this.data = data;
    }

    void set(int index) {
      data[index >> 6] |= (1L << index);
    }

    boolean get(int index) {
      return (data[index >> 6] & (1L << index)) != 0;
    }

    /** Number of bits */
    int size() {
      return data.length * Long.SIZE;
    }
  }
}

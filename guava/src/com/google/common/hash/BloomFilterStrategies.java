// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.hash;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Collections of strategies of generating the {@code k * log(M)} bits required for an element to
 * be mapped to a {@link BloomFilter} of {@code M} bits and {@code k} hash functions. These
 * strategies are part of the serialized form of the Bloom filters that use them, thus they must be
 * preserved as is (no updates allowed, only introduction of new versions).
 * 
 * @author andreou@google.com (Dimitris Andreou)
 */
final class BloomFilterStrategies {
  private BloomFilterStrategies() {}
  
  /**
   * See "Less Hashing, Same Performance: Building a Better Bloom Filter" by Adam Kirsch and 
   * Michael Mitzenmacher. The paper argues that this trick doesn't significantly deteriorate the 
   * performance of a Bloom filter (yet only needs two 32bit hash functions).
   */
  static class From128ToN extends AbstractCompositeHashFunction implements Serializable {
    private final int bits;
    private final HashFunction hashFunction;
    
    private From128ToN(int longs, HashFunction hashFunction) {
      super(hashFunction);
      this.hashFunction = hashFunction;
      this.bits = longs;
    }
    
    static From128ToN withBits(int bits, HashFunction hashFunction) {
      return new From128ToN(checkPositiveAndMakeMultipleOf64(bits), hashFunction);
    }
    
    @Override
    HashCode makeHash(Hasher[] hashers) {
      ByteBuffer buf = ByteBuffer.wrap(hashers[0].hash().asBytes());
      long hash1 = buf.getLong();
      long hash2 = buf.getLong();
      return compose64(hash1, hash2, bits);
    }

    @Override
    public int bits() {
      return bits;
    }
    
    private Object writeReplace() {
      return new SerialForm(this);
    }
    
    private static class SerialForm implements Serializable {
      final int bits;
      final HashFunction hashFunction;
      SerialForm(From128ToN object) {
        this.bits = object.bits;
        this.hashFunction = object.hashFunction;
      }
      Object readResolve() {
        return From128ToN.withBits(bits, hashFunction);
      }
      private static final long serialVersionUID = 0L;
    }
  }
  
  private static int checkPositiveAndMakeMultipleOf64(int bits) {
    checkArgument(bits > 0, "Number of bits must be positive");
    return (bits + 63) & ~63;
  }
  
  private static HashCode compose64(long hash1, long hash2, int bits) {
    byte[] bytes = new byte[bits / 8];
    ByteBuffer buf = ByteBuffer.wrap(bytes);
    for (long i = 1, numLongs = bits / 64; i <= numLongs; i++) {
      buf.putLong(hash1 + i * hash2);
    }
    return HashCodes.fromBytes(bytes);
  }
}

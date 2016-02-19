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

/*
 * MurmurHash3 was written by Austin Appleby, and is placed in the public
 * domain. The author hereby disclaims copyright to this source code.
 */

/*
 * Source:
 * http://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp
 * (Modified to adapt to Guava coding conventions and to use the HashFunction interface)
 */

package com.google.common.hash;

import static com.google.common.primitives.UnsignedBytes.toInt;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.Nullable;

/**
 * See MurmurHash3_x64_128 in <a href="http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp">the
 * C++ implementation</a>.
 *
 * @author Austin Appleby
 * @author Dimitris Andreou
 */
final class Murmur3_128HashFunction extends AbstractStreamingHashFunction implements Serializable {
  // TODO(user): when the shortcuts are implemented, update BloomFilterStrategies
  private final int seed;

  Murmur3_128HashFunction(int seed) {
    this.seed = seed;
  }

  @Override
  public int bits() {
    return 128;
  }

  @Override
  public Hasher newHasher() {
    return new Murmur3_128Hasher(seed);
  }

  @Override
  public String toString() {
    return "Hashing.murmur3_128(" + seed + ")";
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof Murmur3_128HashFunction) {
      Murmur3_128HashFunction other = (Murmur3_128HashFunction) object;
      return seed == other.seed;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() ^ seed;
  }

  private static final class Murmur3_128Hasher extends AbstractStreamingHasher {
    private static final int CHUNK_SIZE = 16;
    private static final long C1 = 0x87c37b91114253d5L;
    private static final long C2 = 0x4cf5ad432745937fL;
    private long h1;
    private long h2;
    private int length;

    Murmur3_128Hasher(int seed) {
      super(CHUNK_SIZE);
      this.h1 = seed;
      this.h2 = seed;
      this.length = 0;
    }

    @Override
    protected void process(ByteBuffer bb) {
      long k1 = bb.getLong();
      long k2 = bb.getLong();
      bmix64(k1, k2);
      length += CHUNK_SIZE;
    }

    private void bmix64(long k1, long k2) {
      h1 ^= mixK1(k1);

      h1 = Long.rotateLeft(h1, 27);
      h1 += h2;
      h1 = h1 * 5 + 0x52dce729;

      h2 ^= mixK2(k2);

      h2 = Long.rotateLeft(h2, 31);
      h2 += h1;
      h2 = h2 * 5 + 0x38495ab5;
    }

    @Override
    protected void processRemaining(ByteBuffer bb) {
      long k1 = 0;
      long k2 = 0;
      length += bb.remaining();
      switch (bb.remaining()) {
        case 15:
          k2 ^= (long) toInt(bb.get(14)) << 48; // fall through
        case 14:
          k2 ^= (long) toInt(bb.get(13)) << 40; // fall through
        case 13:
          k2 ^= (long) toInt(bb.get(12)) << 32; // fall through
        case 12:
          k2 ^= (long) toInt(bb.get(11)) << 24; // fall through
        case 11:
          k2 ^= (long) toInt(bb.get(10)) << 16; // fall through
        case 10:
          k2 ^= (long) toInt(bb.get(9)) << 8; // fall through
        case 9:
          k2 ^= (long) toInt(bb.get(8)); // fall through
        case 8:
          k1 ^= bb.getLong();
          break;
        case 7:
          k1 ^= (long) toInt(bb.get(6)) << 48; // fall through
        case 6:
          k1 ^= (long) toInt(bb.get(5)) << 40; // fall through
        case 5:
          k1 ^= (long) toInt(bb.get(4)) << 32; // fall through
        case 4:
          k1 ^= (long) toInt(bb.get(3)) << 24; // fall through
        case 3:
          k1 ^= (long) toInt(bb.get(2)) << 16; // fall through
        case 2:
          k1 ^= (long) toInt(bb.get(1)) << 8; // fall through
        case 1:
          k1 ^= (long) toInt(bb.get(0));
          break;
        default:
          throw new AssertionError("Should never get here.");
      }
      h1 ^= mixK1(k1);
      h2 ^= mixK2(k2);
    }

    @Override
    public HashCode makeHash() {
      h1 ^= length;
      h2 ^= length;

      h1 += h2;
      h2 += h1;

      h1 = fmix64(h1);
      h2 = fmix64(h2);

      h1 += h2;
      h2 += h1;

      return HashCode.fromBytesNoCopy(
          ByteBuffer.wrap(new byte[CHUNK_SIZE])
              .order(ByteOrder.LITTLE_ENDIAN)
              .putLong(h1)
              .putLong(h2)
              .array());
    }

    private static long fmix64(long k) {
      k ^= k >>> 33;
      k *= 0xff51afd7ed558ccdL;
      k ^= k >>> 33;
      k *= 0xc4ceb9fe1a85ec53L;
      k ^= k >>> 33;
      return k;
    }

    private static long mixK1(long k1) {
      k1 *= C1;
      k1 = Long.rotateLeft(k1, 31);
      k1 *= C2;
      return k1;
    }

    private static long mixK2(long k2) {
      k2 *= C2;
      k2 = Long.rotateLeft(k2, 33);
      k2 *= C1;
      return k2;
    }
  }

  private static final long serialVersionUID = 0L;
}

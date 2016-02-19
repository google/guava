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

import com.google.common.primitives.Chars;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.io.Serializable;
import java.nio.ByteBuffer;

import javax.annotation.Nullable;

/**
 * See MurmurHash3_x86_32 in <a href="http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp">the
 * C++ implementation</a>.
 *
 * @author Austin Appleby
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 */
final class Murmur3_32HashFunction extends AbstractStreamingHashFunction implements Serializable {
  private static final int C1 = 0xcc9e2d51;
  private static final int C2 = 0x1b873593;

  private final int seed;

  Murmur3_32HashFunction(int seed) {
    this.seed = seed;
  }

  @Override
  public int bits() {
    return 32;
  }

  @Override
  public Hasher newHasher() {
    return new Murmur3_32Hasher(seed);
  }

  @Override
  public String toString() {
    return "Hashing.murmur3_32(" + seed + ")";
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof Murmur3_32HashFunction) {
      Murmur3_32HashFunction other = (Murmur3_32HashFunction) object;
      return seed == other.seed;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() ^ seed;
  }

  @Override
  public HashCode hashInt(int input) {
    int k1 = mixK1(input);
    int h1 = mixH1(seed, k1);

    return fmix(h1, Ints.BYTES);
  }

  @Override
  public HashCode hashLong(long input) {
    int low = (int) input;
    int high = (int) (input >>> 32);

    int k1 = mixK1(low);
    int h1 = mixH1(seed, k1);

    k1 = mixK1(high);
    h1 = mixH1(h1, k1);

    return fmix(h1, Longs.BYTES);
  }

  // TODO(kak): Maybe implement #hashBytes instead?
  @Override
  public HashCode hashUnencodedChars(CharSequence input) {
    int h1 = seed;

    // step through the CharSequence 2 chars at a time
    for (int i = 1; i < input.length(); i += 2) {
      int k1 = input.charAt(i - 1) | (input.charAt(i) << 16);
      k1 = mixK1(k1);
      h1 = mixH1(h1, k1);
    }

    // deal with any remaining characters
    if ((input.length() & 1) == 1) {
      int k1 = input.charAt(input.length() - 1);
      k1 = mixK1(k1);
      h1 ^= k1;
    }

    return fmix(h1, Chars.BYTES * input.length());
  }

  private static int mixK1(int k1) {
    k1 *= C1;
    k1 = Integer.rotateLeft(k1, 15);
    k1 *= C2;
    return k1;
  }

  private static int mixH1(int h1, int k1) {
    h1 ^= k1;
    h1 = Integer.rotateLeft(h1, 13);
    h1 = h1 * 5 + 0xe6546b64;
    return h1;
  }

  // Finalization mix - force all bits of a hash block to avalanche
  private static HashCode fmix(int h1, int length) {
    h1 ^= length;
    h1 ^= h1 >>> 16;
    h1 *= 0x85ebca6b;
    h1 ^= h1 >>> 13;
    h1 *= 0xc2b2ae35;
    h1 ^= h1 >>> 16;
    return HashCode.fromInt(h1);
  }

  private static final class Murmur3_32Hasher extends AbstractStreamingHasher {
    private static final int CHUNK_SIZE = 4;
    private int h1;
    private int length;

    Murmur3_32Hasher(int seed) {
      super(CHUNK_SIZE);
      this.h1 = seed;
      this.length = 0;
    }

    @Override
    protected void process(ByteBuffer bb) {
      int k1 = Murmur3_32HashFunction.mixK1(bb.getInt());
      h1 = Murmur3_32HashFunction.mixH1(h1, k1);
      length += CHUNK_SIZE;
    }

    @Override
    protected void processRemaining(ByteBuffer bb) {
      length += bb.remaining();
      int k1 = 0;
      for (int i = 0; bb.hasRemaining(); i += 8) {
        k1 ^= toInt(bb.get()) << i;
      }
      h1 ^= Murmur3_32HashFunction.mixK1(k1);
    }

    @Override
    public HashCode makeHash() {
      return Murmur3_32HashFunction.fmix(h1, length);
    }
  }

  private static final long serialVersionUID = 0L;
}

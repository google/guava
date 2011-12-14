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

package com.google.common.hash;

import static com.google.common.primitives.UnsignedBytes.toInt;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * See http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp
 * MurmurHash3_x86_32
 * 
 * @author aappleby@google.com (Austin Appleby)
 * @author andreou@google.com (Dimitris Andreou)
 */
final class Murmur3_32HashFunction extends AbstractStreamingHashFunction implements Serializable {
  private final int seed;
  
  Murmur3_32HashFunction(int seed) {
    this.seed = seed;
  }

  @Override public int bits() {
    return 32;
  }

  @Override public Hasher newHasher() {
    return new Murmur3_32Hasher(seed);
  }

  private static final class Murmur3_32Hasher extends AbstractStreamingHasher {
    int h1;
    int c1 = 0xcc9e2d51;
    int c2 = 0x1b873593;
    int len;

    Murmur3_32Hasher(int seed) {
      super(4);
      h1 = seed;
    }

    @Override protected void process(ByteBuffer bb) {
      int k1 = bb.getInt();
      len += 4;
      
      k1 *= c1; 
      k1 = Integer.rotateLeft(k1, 15); 
      k1 *= c2;
      
      h1 ^= k1;
      h1 = Integer.rotateLeft(h1, 13);
      h1 = h1 * 5 + 0xe6546b64;
    }
    
    @Override protected void processRemaining(ByteBuffer bb) {
      len += bb.remaining();
      int k1 = 0;
      switch (bb.remaining()) {
        case 3:
          k1 ^= toInt(bb.get(2)) << 16;
          // fall through
        case 2:
          k1 ^= toInt(bb.get(1)) << 8;
          // fall through
        case 1:
          k1 ^= toInt(bb.get(0));
          // fall through
        default:
          k1 *= c1;
          k1 = Integer.rotateLeft(k1, 15);
          k1 *= c2;
          h1 ^= k1;
      }
    }
    
    @Override public HashCode makeHash() {
      h1 ^= len;

      h1 ^= h1 >>> 16;
      h1 *= 0x85ebca6b;
      h1 ^= h1 >>> 13;
      h1 *= 0xc2b2ae35;
      h1 ^= h1 >>> 16;
          
      return HashCodes.fromInt(h1);
    }
  }
  
  private static final long serialVersionUID = 0L;
}

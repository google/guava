/*
 * Copyright (C) 2012 The Guava Authors
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
 * SipHash-c-d was designed by Jean-Philippe Aumasson and Daniel J. Bernstein and is described in
 * "SipHash: a fast short-input PRF" (available at https://131002.net/siphash/siphash.pdf).
 */

package com.google.common.hash;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.nio.ByteBuffer;

import javax.annotation.Nullable;

/**
 * {@link HashFunction} implementation of SipHash-c-d.
 *
 * @author Kurt Alfred Kluever
 * @author Jean-Philippe Aumasson
 * @author Daniel J. Bernstein
 */
final class SipHashFunction extends AbstractStreamingHashFunction implements Serializable {

  // The number of compression rounds.
  private final int c;
  // The number of finalization rounds.
  private final int d;
  // Two 64-bit keys (represent a single 128-bit key).
  private final long k0;
  private final long k1;

  /**
   * @param c the number of compression rounds (must be positive)
   * @param d the number of finalization rounds (must be positive)
   * @param k0 the first half of the key
   * @param k1 the second half of the key
   */
  SipHashFunction(int c, int d, long k0, long k1) {
    checkArgument(
        c > 0, "The number of SipRound iterations (c=%s) during Compression must be positive.", c);
    checkArgument(
        d > 0, "The number of SipRound iterations (d=%s) during Finalization must be positive.", d);
    this.c = c;
    this.d = d;
    this.k0 = k0;
    this.k1 = k1;
  }

  @Override
  public int bits() {
    return 64;
  }

  @Override
  public Hasher newHasher() {
    return new SipHasher(c, d, k0, k1);
  }

  // TODO(kak): Implement and benchmark the hashFoo() shortcuts.

  @Override
  public String toString() {
    return "Hashing.sipHash" + c + "" + d + "(" + k0 + ", " + k1 + ")";
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof SipHashFunction) {
      SipHashFunction other = (SipHashFunction) object;
      return (c == other.c)
          && (d == other.d)
          && (k0 == other.k0)
          && (k1 == other.k1);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (int) (getClass().hashCode() ^ c ^ d ^ k0 ^ k1);
  }

  private static final class SipHasher extends AbstractStreamingHasher {
    private static final int CHUNK_SIZE = 8;

    // The number of compression rounds.
    private final int c;
    // The number of finalization rounds.
    private final int d;

    // Four 64-bit words of internal state.
    // The initial state corresponds to the ASCII string "somepseudorandomlygeneratedbytes",
    // big-endian encoded. There is nothing special about this value; the only requirement
    // was some asymmetry so that the initial v0 and v1 differ from v2 and v3.
    private long v0 = 0x736f6d6570736575L;
    private long v1 = 0x646f72616e646f6dL;
    private long v2 = 0x6c7967656e657261L;
    private long v3 = 0x7465646279746573L;

    // The number of bytes in the input.
    private long b = 0;

    // The final 64-bit chunk includes the last 0 through 7 bytes of m followed by null bytes
    // and ending with a byte encoding the positive integer b mod 256.
    private long finalM = 0;

    SipHasher(int c, int d, long k0, long k1) {
      super(CHUNK_SIZE);
      this.c = c;
      this.d = d;
      this.v0 ^= k0;
      this.v1 ^= k1;
      this.v2 ^= k0;
      this.v3 ^= k1;
    }

    @Override
    protected void process(ByteBuffer buffer) {
      b += CHUNK_SIZE;
      processM(buffer.getLong());
    }

    @Override
    protected void processRemaining(ByteBuffer buffer) {
      b += buffer.remaining();
      for (int i = 0; buffer.hasRemaining(); i += 8) {
        finalM ^= (buffer.get() & 0xFFL) << i;
      }
    }

    @Override
    public HashCode makeHash() {
      // End with a byte encoding the positive integer b mod 256.
      finalM ^= b << 56;
      processM(finalM);

      // Finalization
      v2 ^= 0xFFL;
      sipRound(d);
      return HashCode.fromLong(v0 ^ v1 ^ v2 ^ v3);
    }

    private void processM(long m) {
      v3 ^= m;
      sipRound(c);
      v0 ^= m;
    }

    private void sipRound(int iterations) {
      for (int i = 0; i < iterations; i++) {
        v0 += v1;
        v2 += v3;
        v1 = Long.rotateLeft(v1, 13);
        v3 = Long.rotateLeft(v3, 16);
        v1 ^= v0;
        v3 ^= v2;
        v0 = Long.rotateLeft(v0, 32);
        v2 += v1;
        v0 += v3;
        v1 = Long.rotateLeft(v1, 17);
        v3 = Long.rotateLeft(v3, 21);
        v1 ^= v2;
        v3 ^= v0;
        v2 = Long.rotateLeft(v2, 32);
      }
    }
  }

  private static final long serialVersionUID = 0L;
}

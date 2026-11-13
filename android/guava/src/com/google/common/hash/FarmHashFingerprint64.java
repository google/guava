/*
 * Copyright (C) 2015 The Guava Authors
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

import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.hash.LittleEndianByteArray.load32;
import static com.google.common.hash.LittleEndianByteArray.load64;
import static com.google.common.hash.LittleEndianByteArray.store64;
import static java.lang.Long.rotateRight;
import static java.lang.System.arraycopy;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Implementation of FarmHash Fingerprint64, an open-source fingerprinting algorithm for strings.
 *
 * <p>Its speed is comparable to CityHash64, and its quality of hashing is at least as good.
 *
 * <p>Note to maintainers: This implementation relies on signed arithmetic being bit-wise equivalent
 * to unsigned arithmetic in all cases except:
 *
 * <ul>
 *   <li>comparisons (signed values can be negative)
 *   <li>division (avoided here)
 *   <li>shifting (right shift must be unsigned)
 * </ul>
 *
 * @author Kyle Maddison
 * @author Geoff Pike
 */
@Immutable
final class FarmHashFingerprint64 extends AbstractHashFunction {
  // Some primes between 2^63 and 2^64 for various uses.
  private static final long K0 = 0xc3a5c85c97cb3127L;
  private static final long K1 = 0xb492b66fbe98f273L;
  private static final long K2 = 0x9ae16a3b2f90404fL;

  private static final int BLOCK_SIZE = 64;
  private static final int SEED = 81;

  @SuppressWarnings("ConstantOverflow")
  private static final long INITIAL_X_BASE = SEED * K2;

  @SuppressWarnings("ConstantOverflow")
  private static final long INITIAL_Y = SEED * K1 + 113;

  private static final long INITIAL_Z = shiftMix(INITIAL_Y * K2 + 113) * K2;

  static final HashFunction FARMHASH_FINGERPRINT_64 = new FarmHashFingerprint64();

  @Override
  public Hasher newHasher() {
    return new FarmHashFingerprint64Hasher();
  }

  @Override
  public HashCode hashInt(int input) {
    long mul = K2 + 8;
    long a = input & 0xFFFFFFFFL;
    return HashCode.fromLong(hashLength16(4 + (a << 3), a, mul));
  }

  @Override
  public HashCode hashLong(long input) {
    long mul = K2 + 16;
    long a = input + K2;
    long c = rotateRight(input, 37) * mul + a;
    long d = (rotateRight(a, 25) + input) * mul;
    return HashCode.fromLong(hashLength16(c, d, mul));
  }

  @SuppressWarnings("HashStringGetBytesCharset") // suggests infinite recursion
  @Override
  public HashCode hashString(CharSequence input, Charset charset) {
    return hashBytes(input.toString().getBytes(charset));
  }

  @Override
  public HashCode hashBytes(byte[] input, int off, int len) {
    checkPositionIndexes(off, off + len, input.length);
    return HashCode.fromLong(fingerprint(input, off, len));
  }

  @Override
  public HashCode hashBytes(ByteBuffer input) {
    if (input.hasArray()) {
      HashCode result =
          hashBytes(input.array(), input.arrayOffset() + input.position(), input.remaining());
      Java8Compatibility.position(input, input.limit());
      return result;
    }
    return newHasher().putBytes(input).hash();
  }

  @Override
  public int bits() {
    return 64;
  }

  @Override
  public String toString() {
    return "Hashing.farmHashFingerprint64()";
  }

  // End of public functions.

  @VisibleForTesting
  static long fingerprint(byte[] bytes, int offset, int length) {
    if (length <= BLOCK_SIZE) {
      return hashLength0to64(bytes, offset, length);
    } else {
      return hashLength65Plus(bytes, offset, length);
    }
  }

  private static long hashLength0to64(byte[] bytes, int offset, int length) {
    if (length <= 32) {
      if (length <= 16) {
        return hashLength0to16(bytes, offset, length);
      } else {
        return hashLength17to32(bytes, offset, length);
      }
    } else {
      return hashLength33To64(bytes, offset, length);
    }
  }

  private static long shiftMix(long val) {
    return val ^ (val >>> 47);
  }

  private static long hashLength16(long u, long v, long mul) {
    long a = (u ^ v) * mul;
    a ^= a >>> 47;
    long b = (v ^ a) * mul;
    b ^= b >>> 47;
    b *= mul;
    return b;
  }

  private static long hashLength0to16(byte[] bytes, int offset, int length) {
    if (length >= 8) {
      long mul = K2 + length * 2L;
      long a = load64(bytes, offset) + K2;
      long b = load64(bytes, offset + length - 8);
      long c = rotateRight(b, 37) * mul + a;
      long d = (rotateRight(a, 25) + b) * mul;
      return hashLength16(c, d, mul);
    }
    if (length >= 4) {
      @SuppressWarnings("NarrowCalculation") // length is at most 7 because of the check above
      long mul = K2 + length * 2;
      long a = load32(bytes, offset) & 0xFFFFFFFFL;
      return hashLength16(length + (a << 3), load32(bytes, offset + length - 4) & 0xFFFFFFFFL, mul);
    }
    if (length > 0) {
      byte a = bytes[offset];
      byte b = bytes[offset + (length >> 1)];
      byte c = bytes[offset + (length - 1)];
      int y = (a & 0xFF) + ((b & 0xFF) << 8);
      int z = length + ((c & 0xFF) << 2);
      return shiftMix(y * K2 ^ z * K0) * K2;
    }
    return K2;
  }

  private static long hashLength17to32(byte[] bytes, int offset, int length) {
    long mul = K2 + length * 2L;
    long a = load64(bytes, offset) * K1;
    long b = load64(bytes, offset + 8);
    long c = load64(bytes, offset + length - 8) * mul;
    long d = load64(bytes, offset + length - 16) * K2;
    return hashLength16(
        rotateRight(a + b, 43) + rotateRight(c, 30) + d, a + rotateRight(b + K2, 18) + c, mul);
  }

  private static long hashLength33To64(byte[] bytes, int offset, int length) {
    long mul = K2 + length * 2L;
    long a = load64(bytes, offset) * K2;
    long b = load64(bytes, offset + 8);
    long c = load64(bytes, offset + length - 8) * mul;
    long d = load64(bytes, offset + length - 16) * K2;
    long y = rotateRight(a + b, 43) + rotateRight(c, 30) + d;
    long z = hashLength16(y, a + rotateRight(b + K2, 18) + c, mul);
    long e = load64(bytes, offset + 16) * mul;
    long f = load64(bytes, offset + 24);
    long g = (y + load64(bytes, offset + length - 32)) * mul;
    long h = (z + load64(bytes, offset + length - 24)) * mul;
    return hashLength16(
        rotateRight(e + f, 43) + rotateRight(g, 30) + h, e + rotateRight(f + a, 18) + g, mul);
  }

  /*
   * Compute an 8-byte hash of a byte array of length greater than 64 bytes.
   */
  private static long hashLength65Plus(byte[] bytes, int offset, int length) {
    // For strings over 64 bytes we loop. Internal state consists of 56 bytes: v, w, x, y, and z.
    long x = INITIAL_X_BASE + load64(bytes, offset);
    long y = INITIAL_Y;
    long z = INITIAL_Z;
    long v0 = 0;
    long v1 = 0;
    long w0 = 0;
    long w1 = 0;

    // Set end so that after the loop we have 1 to 64 bytes left to process.
    int tail = (length - 1) & (BLOCK_SIZE - 1);
    int end = offset + ((length - 1) & ~(BLOCK_SIZE - 1));
    // Keep in sync with FarmHashFingerprint64Hasher.mixBlocks.
    do {
      long part1 = load64(bytes, offset + 8);
      long part5 = load64(bytes, offset + 40);
      long part6 = load64(bytes, offset + 48);

      x = rotateRight(x + y + v0 + part1, 37) * K1;
      y = rotateRight(y + v1 + part6, 42) * K1;
      x ^= w1;
      y += v0 + part5;
      z = rotateRight(z + w0, 33) * K1;

      long part0 = load64(bytes, offset);
      long part2 = load64(bytes, offset + 16);
      long part3 = load64(bytes, offset + 24);
      long vSeedA = v1 * K1 + part0;
      long vSeedB = rotateRight(x + w0 + vSeedA + part3, 21);
      long vC = vSeedA;
      vSeedA += part1 + part2;
      vSeedB += rotateRight(vSeedA, 44);
      v0 = vSeedA + part3;
      v1 = vSeedB + vC;

      long part4 = load64(bytes, offset + 32);
      long part7 = load64(bytes, offset + 56);
      long wSeedA = z + w1 + part4;
      long wSeedB = rotateRight(y + part2 + wSeedA + part7, 21);
      long wC = wSeedA;
      wSeedA += part5 + part6;
      wSeedB += rotateRight(wSeedA, 44);
      w0 = wSeedA + part7;
      w1 = wSeedB + wC;

      long tmp = x;
      x = z;
      z = tmp;
      offset += BLOCK_SIZE;
    } while (offset != end);
    return finalMix(x, y, z, v0, v1, w0, w1, bytes, end + tail - 63, tail);
  }

  private static long finalMix(
      long x,
      long y,
      long z,
      long v0,
      long v1,
      long w0,
      long w1,
      byte[] bytes,
      int offset,
      int lastBlockRemainder) {
    long part0 = load64(bytes, offset);
    long part1 = load64(bytes, offset + 8);
    long part2 = load64(bytes, offset + 16);
    long part3 = load64(bytes, offset + 24);
    long part4 = load64(bytes, offset + 32);
    long part5 = load64(bytes, offset + 40);
    long part6 = load64(bytes, offset + 48);
    long part7 = load64(bytes, offset + 56);

    long mul = K1 + ((z & 0xFF) << 1);
    // Operate on the last 64 bytes of input.
    w0 += lastBlockRemainder;
    v0 += w0;
    w0 += v0;
    x = rotateRight(x + y + v0 + part1, 37) * mul;
    y = rotateRight(y + v1 + part6, 42) * mul;
    x ^= w1 * 9;
    y += v0 * 9 + part5;
    z = rotateRight(z + w0, 33) * mul;

    long vSeedA = v1 * mul + part0;
    long vSeedB = rotateRight(x + w0 + vSeedA + part3, 21);
    long vC = vSeedA;
    vSeedA += part1 + part2;
    vSeedB += rotateRight(vSeedA, 44);
    v0 = vSeedA + part3;
    v1 = vSeedB + vC;

    long wSeedA = z + w1 + part4;
    long wSeedB = rotateRight(y + part2 + wSeedA + part7, 21);
    long wC = wSeedA;
    wSeedA += part5 + part6;
    wSeedB += rotateRight(wSeedA, 44);
    w0 = wSeedA + part7;
    w1 = wSeedB + wC;

    return hashLength16(
        hashLength16(v0, w0, mul) + shiftMix(y) * K0 + x, hashLength16(v1, w1, mul) + z, mul);
  }

  private static final class FarmHashFingerprint64Hasher extends AbstractHasher {
    // Circular 64-byte window in buffer[0..63]; buffer[64..127] is scratch space used in hash() to
    // linearize the last 64 bytes without allocating when initialized && pos < BLOCK_SIZE.
    //
    // Invariants:
    // - Before initialization (!initialized): totalBytes == pos, with 0 <= pos <= BLOCK_SIZE, and
    //   buffer[0..pos - 1] contains all input bytes seen so far.
    // - After initialization (initialized): totalBytes >= BLOCK_SIZE + 1, with 1 <= pos <=
    //   BLOCK_SIZE; all bytes except the last `pos` bytes have been mixed in 64-byte blocks;
    //   buffer[0..pos - 1] holds those `pos` unmixed bytes; and buffer[pos..BLOCK_SIZE - 1] holds
    //   the trailing (BLOCK_SIZE - pos) bytes of the most-recently-mixed 64-byte block.
    // - hash() does not mutate buffer[0..BLOCK_SIZE - 1], pos, or the mixing state fields (it only
    //   writes to scratch space in buffer[BLOCK_SIZE..127] and passes copies of state to finalMix),
    //   so repeated hash() calls and further put* calls after hash() remain well-defined (matching
    //   the historical AbstractNonStreamingHashFunction behavior relied on by some callers/tests).
    private final byte[] buffer = new byte[BLOCK_SIZE * 2];
    private int pos;
    private boolean initialized;
    private long x;
    private long y;
    private long z;
    private long v0;
    private long v1;
    private long w0;
    private long w1;

    @Override
    @CanIgnoreReturnValue
    public Hasher putByte(byte b) {
      if (pos == BLOCK_SIZE) {
        mixBlocks(buffer, 0, BLOCK_SIZE);
        pos = 0;
      }
      buffer[pos++] = b;
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Hasher putShort(short s) {
      if (pos > BLOCK_SIZE - 2) {
        if (pos != BLOCK_SIZE) {
          return super.putShort(s);
        }
        mixBlocks(buffer, 0, BLOCK_SIZE);
        pos = 0;
      }
      buffer[pos] = (byte) s;
      buffer[pos + 1] = (byte) (s >>> 8);
      pos += 2;
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Hasher putChar(char c) {
      if (pos > BLOCK_SIZE - 2) {
        if (pos != BLOCK_SIZE) {
          return super.putChar(c);
        }
        mixBlocks(buffer, 0, BLOCK_SIZE);
        pos = 0;
      }
      buffer[pos] = (byte) c;
      buffer[pos + 1] = (byte) (c >>> 8);
      pos += 2;
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Hasher putInt(int i) {
      if (pos > BLOCK_SIZE - 4) {
        if (pos != BLOCK_SIZE) {
          return super.putInt(i);
        }
        mixBlocks(buffer, 0, BLOCK_SIZE);
        pos = 0;
      }
      buffer[pos] = (byte) i;
      buffer[pos + 1] = (byte) (i >>> 8);
      buffer[pos + 2] = (byte) (i >>> 16);
      buffer[pos + 3] = (byte) (i >>> 24);
      pos += 4;
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Hasher putLong(long l) {
      if (pos > BLOCK_SIZE - 8) {
        if (pos != BLOCK_SIZE) {
          return super.putLong(l);
        }
        mixBlocks(buffer, 0, BLOCK_SIZE);
        pos = 0;
      }
      store64(buffer, pos, l);
      pos += 8;
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Hasher putBytes(byte[] bytes, int off, int len) {
      checkPositionIndexes(off, off + len, bytes.length);
      if (len == 0) {
        return this;
      }
      int remainingInBuffer = BLOCK_SIZE - pos;
      if (len <= remainingInBuffer) {
        arraycopy(bytes, off, buffer, pos, len);
        pos += len;
        return this;
      }
      if (pos > 0) {
        arraycopy(bytes, off, buffer, pos, remainingInBuffer);
        off += remainingInBuffer;
        len -= remainingInBuffer;
        mixBlocks(buffer, 0, BLOCK_SIZE);
        pos = 0;
      }
      if (len > BLOCK_SIZE) {
        int bytesToMix = (len - 1) & ~(BLOCK_SIZE - 1);
        int end = off + bytesToMix;
        mixBlocks(bytes, off, end);
        off = end;
        int tailLen = len - bytesToMix;
        arraycopy(bytes, off, buffer, 0, tailLen);
        if (tailLen < BLOCK_SIZE) {
          arraycopy(bytes, off - BLOCK_SIZE + tailLen, buffer, tailLen, BLOCK_SIZE - tailLen);
        }
        pos = tailLen;
        return this;
      }
      arraycopy(bytes, off, buffer, 0, len);
      pos = len;
      return this;
    }

    @Override
    @CanIgnoreReturnValue
    public Hasher putBytes(ByteBuffer bb) {
      if (bb.hasArray()) {
        putBytes(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
        Java8Compatibility.position(bb, bb.limit());
        return this;
      }
      int len = bb.remaining();
      if (len == 0) {
        return this;
      }
      int remainingInBuffer = BLOCK_SIZE - pos;
      if (len <= remainingInBuffer) {
        bb.get(buffer, pos, len);
        pos += len;
        return this;
      }
      if (pos > 0) {
        bb.get(buffer, pos, remainingInBuffer);
        len -= remainingInBuffer;
        mixBlocks(buffer, 0, BLOCK_SIZE);
        pos = 0;
      }
      while (len > BLOCK_SIZE) {
        bb.get(buffer, 0, BLOCK_SIZE);
        len -= BLOCK_SIZE;
        mixBlocks(buffer, 0, BLOCK_SIZE);
      }
      bb.get(buffer, 0, len);
      pos = len;
      return this;
    }

    // Must not mutate buffer[0..BLOCK_SIZE - 1], pos, or mixing state; see class invariant above.
    @Override
    public HashCode hash() {
      if (!initialized) {
        return HashCode.fromLong(hashLength0to64(buffer, 0, pos));
      }
      int offset;
      if (pos == BLOCK_SIZE) {
        offset = 0;
      } else {
        arraycopy(buffer, 0, buffer, BLOCK_SIZE, pos);
        offset = pos;
      }
      return HashCode.fromLong(finalMix(x, y, z, v0, v1, w0, w1, buffer, offset, pos - 1));
    }

    private void mixBlocks(byte[] bytes, int offset, int end) {
      if (!initialized) {
        x = INITIAL_X_BASE + load64(bytes, offset);
        y = INITIAL_Y;
        z = INITIAL_Z;
        initialized = true;
      }
      long x = this.x;
      long y = this.y;
      long z = this.z;
      long v0 = this.v0;
      long v1 = this.v1;
      long w0 = this.w0;
      long w1 = this.w1;
      // Keep in sync with hashLength65Plus.
      do {
        long part1 = load64(bytes, offset + 8);
        long part5 = load64(bytes, offset + 40);
        long part6 = load64(bytes, offset + 48);

        x = rotateRight(x + y + v0 + part1, 37) * K1;
        y = rotateRight(y + v1 + part6, 42) * K1;
        x ^= w1;
        y += v0 + part5;
        z = rotateRight(z + w0, 33) * K1;

        long part0 = load64(bytes, offset);
        long part2 = load64(bytes, offset + 16);
        long part3 = load64(bytes, offset + 24);
        long vSeedA = v1 * K1 + part0;
        long vSeedB = rotateRight(x + w0 + vSeedA + part3, 21);
        long vC = vSeedA;
        vSeedA += part1 + part2;
        vSeedB += rotateRight(vSeedA, 44);
        v0 = vSeedA + part3;
        v1 = vSeedB + vC;

        long part4 = load64(bytes, offset + 32);
        long part7 = load64(bytes, offset + 56);
        long wSeedA = z + w1 + part4;
        long wSeedB = rotateRight(y + part2 + wSeedA + part7, 21);
        long wC = wSeedA;
        wSeedA += part5 + part6;
        wSeedB += rotateRight(wSeedA, 44);
        w0 = wSeedA + part7;
        w1 = wSeedB + wC;

        long tmp = x;
        x = z;
        z = tmp;
        offset += BLOCK_SIZE;
      } while (offset != end);
      this.x = x;
      this.y = y;
      this.z = z;
      this.v0 = v0;
      this.v1 = v1;
      this.w0 = w0;
      this.w1 = w1;
    }
  }
}

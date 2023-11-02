// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.hash;

import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.hash.LittleEndianByteArray.load64;
import static com.google.common.hash.LittleEndianByteArray.load64Safely;
import static java.lang.Long.rotateRight;

import com.google.common.annotations.VisibleForTesting;

/**
 * Implementation of Geoff Pike's fingerprint2011 hash function. See {@link Hashing#fingerprint2011}
 * for information on the behaviour of the algorithm.
 *
 * <p>On Intel Core2 2.66, on 1000 bytes, fingerprint2011 takes 0.9 microseconds compared to
 * fingerprint at 4.0 microseconds and md5 at 4.5 microseconds.
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
 * @author kylemaddison@google.com (Kyle Maddison)
 * @author gpike@google.com (Geoff Pike)
 */
@ElementTypesAreNonnullByDefault
final class Fingerprint2011 extends AbstractNonStreamingHashFunction {
  static final HashFunction FINGERPRINT_2011 = new Fingerprint2011();

  // Some primes between 2^63 and 2^64 for various uses.
  private static final long K0 = 0xa5b85c5e198ed849L;
  private static final long K1 = 0x8d58ac26afe12e47L;
  private static final long K2 = 0xc47b6e9e3a970ed3L;
  private static final long K3 = 0xc6a4a7935bd1e995L;

  @Override
  public HashCode hashBytes(byte[] input, int off, int len) {
    checkPositionIndexes(off, off + len, input.length);
    return HashCode.fromLong(fingerprint(input, off, len));
  }

  @Override
  public int bits() {
    return 64;
  }

  @Override
  public String toString() {
    return "Hashing.fingerprint2011()";
  }

  // End of public functions.

  @VisibleForTesting
  static long fingerprint(byte[] bytes, int offset, int length) {
    long result;

    if (length <= 32) {
      result = murmurHash64WithSeed(bytes, offset, length, K0 ^ K1 ^ K2);
    } else if (length <= 64) {
      result = hashLength33To64(bytes, offset, length);
    } else {
      result = fullFingerprint(bytes, offset, length);
    }

    long u = length >= 8 ? load64(bytes, offset) : K0;
    long v = length >= 9 ? load64(bytes, offset + length - 8) : K0;
    result = hash128to64(result + v, u);
    return result == 0 || result == 1 ? result + ~1 : result;
  }

  private static long shiftMix(long val) {
    return val ^ (val >>> 47);
  }

  /** Implementation of Hash128to64 from util/hash/hash128to64.h */
  @VisibleForTesting
  static long hash128to64(long high, long low) {
    long a = (low ^ high) * K3;
    a ^= (a >>> 47);
    long b = (high ^ a) * K3;
    b ^= (b >>> 47);
    b *= K3;
    return b;
  }

  /**
   * Computes intermediate hash of 32 bytes of byte array from the given offset. Results are
   * returned in the output array - this is 12% faster than allocating new arrays every time.
   */
  private static void weakHashLength32WithSeeds(
      byte[] bytes, int offset, long seedA, long seedB, long[] output) {
    long part1 = load64(bytes, offset);
    long part2 = load64(bytes, offset + 8);
    long part3 = load64(bytes, offset + 16);
    long part4 = load64(bytes, offset + 24);

    seedA += part1;
    seedB = rotateRight(seedB + seedA + part4, 51);
    long c = seedA;
    seedA += part2;
    seedA += part3;
    seedB += rotateRight(seedA, 23);
    output[0] = seedA + part4;
    output[1] = seedB + c;
  }

  /*
   * Compute an 8-byte hash of a byte array of length greater than 64 bytes.
   */
  private static long fullFingerprint(byte[] bytes, int offset, int length) {
    // For lengths over 64 bytes we hash the end first, and then as we
    // loop we keep 56 bytes of state: v, w, x, y, and z.
    long x = load64(bytes, offset);
    long y = load64(bytes, offset + length - 16) ^ K1;
    long z = load64(bytes, offset + length - 56) ^ K0;
    long[] v = new long[2];
    long[] w = new long[2];
    weakHashLength32WithSeeds(bytes, offset + length - 64, length, y, v);
    weakHashLength32WithSeeds(bytes, offset + length - 32, length * K1, K0, w);
    z += shiftMix(v[1]) * K1;
    x = rotateRight(z + x, 39) * K1;
    y = rotateRight(y, 33) * K1;

    // Decrease length to the nearest multiple of 64, and operate on 64-byte chunks.
    length = (length - 1) & ~63;
    do {
      x = rotateRight(x + y + v[0] + load64(bytes, offset + 16), 37) * K1;
      y = rotateRight(y + v[1] + load64(bytes, offset + 48), 42) * K1;
      x ^= w[1];
      y ^= v[0];
      z = rotateRight(z ^ w[0], 33);
      weakHashLength32WithSeeds(bytes, offset, v[1] * K1, x + w[0], v);
      weakHashLength32WithSeeds(bytes, offset + 32, z + w[1], y, w);
      long tmp = z;
      z = x;
      x = tmp;
      offset += 64;
      length -= 64;
    } while (length != 0);
    return hash128to64(hash128to64(v[0], w[0]) + shiftMix(y) * K1 + z, hash128to64(v[1], w[1]) + x);
  }

  private static long hashLength33To64(byte[] bytes, int offset, int length) {
    long z = load64(bytes, offset + 24);
    long a = load64(bytes, offset) + (length + load64(bytes, offset + length - 16)) * K0;
    long b = rotateRight(a + z, 52);
    long c = rotateRight(a, 37);
    a += load64(bytes, offset + 8);
    c += rotateRight(a, 7);
    a += load64(bytes, offset + 16);
    long vf = a + z;
    long vs = b + rotateRight(a, 31) + c;
    a = load64(bytes, offset + 16) + load64(bytes, offset + length - 32);
    z = load64(bytes, offset + length - 8);
    b = rotateRight(a + z, 52);
    c = rotateRight(a, 37);
    a += load64(bytes, offset + length - 24);
    c += rotateRight(a, 7);
    a += load64(bytes, offset + length - 16);
    long wf = a + z;
    long ws = b + rotateRight(a, 31) + c;
    long r = shiftMix((vf + ws) * K2 + (wf + vs) * K0);
    return shiftMix(r * K0 + vs) * K2;
  }

  @VisibleForTesting
  static long murmurHash64WithSeed(byte[] bytes, int offset, int length, long seed) {
    long mul = K3;
    int topBit = 0x7;

    int lengthAligned = length & ~topBit;
    int lengthRemainder = length & topBit;
    long hash = seed ^ (length * mul);

    for (int i = 0; i < lengthAligned; i += 8) {
      long loaded = load64(bytes, offset + i);
      long data = shiftMix(loaded * mul) * mul;
      hash ^= data;
      hash *= mul;
    }

    if (lengthRemainder != 0) {
      long data = load64Safely(bytes, offset + lengthAligned, lengthRemainder);
      hash ^= data;
      hash *= mul;
    }

    hash = shiftMix(hash) * mul;
    hash = shiftMix(hash);
    return hash;
  }
}

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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedInts;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Iterator;

/**
 * Static methods to obtain {@link HashFunction} instances, and other static
 * hashing-related utilities.
 *
 * @author Kevin Bourrillion
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 * @since 11.0
 */
@Beta
public final class Hashing {
  private Hashing() {}

  /**
   * Used to randomize {@link #goodFastHash} instances, so that programs which persist anything
   * dependent on hashcodes of those, will fail sooner than later.
   */
  private static final int GOOD_FAST_HASH_SEED = (int) System.currentTimeMillis();

  // Used by goodFastHash when minimumBits == 32.
  private static final HashFunction GOOD_FAST_HASH_FUNCTION_32 = murmur3_32(GOOD_FAST_HASH_SEED);

  // Used by goodFastHash when 32 < minimumBits <= 128.
  private static final HashFunction GOOD_FAST_HASH_FUNCTION_128 = murmur3_128(GOOD_FAST_HASH_SEED);

  /**
   * Returns a general-purpose, <b>non-cryptographic-strength</b>, streaming hash function that
   * produces hash codes of length at least {@code minimumBits}. Users without specific
   * compatibility requirements and who do not persist the hash codes are encouraged to
   * choose this hash function.
   *
   * <p>Repeated calls to {@link #goodFastHash} with the same {@code minimumBits} value will
   * return {@link HashFunction} instances with identical behavior (but not necessarily the
   * same instance) for the duration of the current virtual machine.
   *
   * <p><b>Warning: the implementation is unspecified and is subject to change.</b>
   *
   * @throws IllegalArgumentException if {@code minimumBits} is not positive
   */
  public static HashFunction goodFastHash(int minimumBits) {
    int bits = checkPositiveAndMakeMultipleOf32(minimumBits);

    if (bits == 32) {
      return GOOD_FAST_HASH_FUNCTION_32;
    }
    if (bits <= 128) {
      return GOOD_FAST_HASH_FUNCTION_128;
    }

    // Otherwise, join together some 128-bit murmur3s
    int hashFunctionsNeeded = (bits + 127) / 128;
    HashFunction[] hashFunctions = new HashFunction[hashFunctionsNeeded];
    hashFunctions[0] = GOOD_FAST_HASH_FUNCTION_128;
    int seed = GOOD_FAST_HASH_SEED;
    for (int i = 1; i < hashFunctionsNeeded; i++) {
      seed += 1500450271; // a prime; shouldn't matter
      hashFunctions[i] = murmur3_128(seed);
    }
    return new ConcatenatedHashFunction(hashFunctions);
  }

  /**
   * Returns a hash function implementing the
   * <a href="http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp">32-bit murmur3
   * algorithm</a> (little-endian variant), using the given seed value.
   */
  public static HashFunction murmur3_32(int seed) {
    return new Murmur3_32HashFunction(seed);
  }

  /**
   * Returns a hash function implementing the
   * <a href="http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp">32-bit murmur3
   * algorithm</a> (little-endian variant), using a seed value of zero.
   */
  public static HashFunction murmur3_32() {
    return MURMUR3_32;
  }

  private static final Murmur3_32HashFunction MURMUR3_32 = new Murmur3_32HashFunction(0);

  /**
   * Returns a hash function implementing the
   * <a href="http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp">
   * 128-bit murmur3 algorithm, x64 variant</a> (little-endian variant), using the given seed
   * value.
   */
  public static HashFunction murmur3_128(int seed) {
    return new Murmur3_128HashFunction(seed);
  }

  /**
   * Returns a hash function implementing the
   * <a href="http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp">
   * 128-bit murmur3 algorithm, x64 variant</a>  (little-endian variant), using a seed value
   * of zero.
   */
  public static HashFunction murmur3_128() {
    return MURMUR3_128;
  }

  private static final Murmur3_128HashFunction MURMUR3_128 = new Murmur3_128HashFunction(0);

  /**
   * Returns a hash function implementing the MD5 hash algorithm (128 hash bits) by delegating to
   * the MD5 {@link MessageDigest}.
   */
  public static HashFunction md5() {
    return MD5;
  }

  private static final HashFunction MD5 = new MessageDigestHashFunction("MD5");

  /**
   * Returns a hash function implementing the SHA-1 algorithm (160 hash bits) by delegating to the
   * SHA-1 {@link MessageDigest}.
   */
  public static HashFunction sha1() {
    return SHA_1;
  }

  private static final HashFunction SHA_1 = new MessageDigestHashFunction("SHA-1");

  /**
   * Returns a hash function implementing the SHA-256 algorithm (256 hash bits) by delegating to
   * the SHA-256 {@link MessageDigest}.
   */
  public static HashFunction sha256() {
    return SHA_256;
  }

  private static final HashFunction SHA_256 = new MessageDigestHashFunction("SHA-256");

  /**
   * Returns a hash function implementing the SHA-512 algorithm (512 hash bits) by delegating to the
   * SHA-512 {@link MessageDigest}.
   */
  public static HashFunction sha512() {
    return SHA_512;
  }

  private static final HashFunction SHA_512 = new MessageDigestHashFunction("SHA-512");

  // Lazy initiliazation holder class idiom.

  /**
   * If {@code hashCode} has enough bits, returns {@code hashCode.asLong()}, otherwise
   * returns a {@code long} value with {@code hashCode.asInt()} as the least-significant
   * four bytes and {@code 0x00} as each of the most-significant four bytes.
   */
  public static long padToLong(HashCode hashCode) {
    return (hashCode.bits() < 64) ? UnsignedInts.toLong(hashCode.asInt()) : hashCode.asLong();
  }

  /**
   * Assigns to {@code hashCode} a "bucket" in the range {@code [0, buckets)}, in a uniform
   * manner that minimizes the need for remapping as {@code buckets} grows. That is,
   * {@code consistentHash(h, n)} equals:
   *
   * <ul>
   * <li>{@code n - 1}, with approximate probability {@code 1/n}
   * <li>{@code consistentHash(h, n - 1)}, otherwise (probability {@code 1 - 1/n})
   * </ul>
   *
   * <p>See the <a href="http://en.wikipedia.org/wiki/Consistent_hashing">wikipedia
   * article on consistent hashing</a> for more information.
   * <p>
   * If you might want to have weights for the buckets in the future, take a look at
   * {@code weightedConsistentHash}.
   */
  public static int consistentHash(HashCode hashCode, int buckets) {
    return consistentHash(padToLong(hashCode), buckets);
  }

  /**
   * Assigns to {@code input} a "bucket" in the range {@code [0, buckets)}, in a uniform
   * manner that minimizes the need for remapping as {@code buckets} grows. That is,
   * {@code consistentHash(h, n)} equals:
   *
   * <ul>
   * <li>{@code n - 1}, with approximate probability {@code 1/n}
   * <li>{@code consistentHash(h, n - 1)}, otherwise (probability {@code 1 - 1/n})
   * </ul>
   *
   * <p>See the <a href="http://en.wikipedia.org/wiki/Consistent_hashing">wikipedia
   * article on consistent hashing</a> for more information.
   * <p>
   * If you might want to have weights for the buckets in the future, take a look at
   * {@code weightedConsistentHash}.
   */
  public static int consistentHash(long input, int buckets) {
    checkArgument(buckets > 0, "buckets must be positive: %s", buckets);
    LinearCongruentialGenerator generator = new LinearCongruentialGenerator(input);
    int candidate = 0;
    int next;

    // Jump from bucket to bucket until we go out of range
    while (true) {
      next = (int) ((candidate + 1) / generator.nextDouble());
      if (next >= 0 && next < buckets) {
        candidate = next;
      } else {
        return candidate;
      }
    }
  }

  /**
   * Returns a hash code, having the same bit length as each of the input hash codes,
   * that combines the information of these hash codes in an ordered fashion. That
   * is, whenever two equal hash codes are produced by two calls to this method, it
   * is <i>as likely as possible</i> that each was computed from the <i>same</i>
   * input hash codes in the <i>same</i> order.
   *
   * @throws IllegalArgumentException if {@code hashCodes} is empty, or the hash codes
   *     do not all have the same bit length
   */
  public static HashCode combineOrdered(Iterable<HashCode> hashCodes) {
    Iterator<HashCode> iterator = hashCodes.iterator();
    checkArgument(iterator.hasNext(), "Must be at least 1 hash code to combine.");
    int bits = iterator.next().bits();
    byte[] resultBytes = new byte[bits / 8];
    for (HashCode hashCode : hashCodes) {
      byte[] nextBytes = hashCode.asBytes();
      checkArgument(nextBytes.length == resultBytes.length,
          "All hashcodes must have the same bit length.");
      for (int i = 0; i < nextBytes.length; i++) {
        resultBytes[i] = (byte) (resultBytes[i] * 37 ^ nextBytes[i]);
      }
    }
    return HashCodes.fromBytesNoCopy(resultBytes);
  }

  /**
   * Returns a hash code, having the same bit length as each of the input hash codes,
   * that combines the information of these hash codes in an unordered fashion. That
   * is, whenever two equal hash codes are produced by two calls to this method, it
   * is <i>as likely as possible</i> that each was computed from the <i>same</i>
   * input hash codes in <i>some</i> order.
   *
   * @throws IllegalArgumentException if {@code hashCodes} is empty, or the hash codes
   *     do not all have the same bit length
   */
  public static HashCode combineUnordered(Iterable<HashCode> hashCodes) {
    Iterator<HashCode> iterator = hashCodes.iterator();
    checkArgument(iterator.hasNext(), "Must be at least 1 hash code to combine.");
    byte[] resultBytes = new byte[iterator.next().bits() / 8];
    for (HashCode hashCode : hashCodes) {
      byte[] nextBytes = hashCode.asBytes();
      checkArgument(nextBytes.length == resultBytes.length,
          "All hashcodes must have the same bit length.");
      for (int i = 0; i < nextBytes.length; i++) {
        resultBytes[i] += nextBytes[i];
      }
    }
    return HashCodes.fromBytesNoCopy(resultBytes);
  }

  /**
   * Checks that the passed argument is positive, and ceils it to a multiple of 32.
   */
  static int checkPositiveAndMakeMultipleOf32(int bits) {
    checkArgument(bits > 0, "Number of bits must be positive");
    return (bits + 31) & ~31;
  }

  // TODO(kevinb): Maybe expose this class via a static Hashing method?
  @VisibleForTesting
  static final class ConcatenatedHashFunction extends AbstractCompositeHashFunction {
    private final int bits;

    ConcatenatedHashFunction(HashFunction... functions) {
      super(functions);
      int bitSum = 0;
      for (HashFunction function : functions) {
        bitSum += function.bits();
      }
      this.bits = bitSum;
    }

    @Override
    HashCode makeHash(Hasher[] hashers) {
      // TODO(user): Get rid of the ByteBuffer here?
      byte[] bytes = new byte[bits / 8];
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      for (Hasher hasher : hashers) {
        buffer.put(hasher.hash().asBytes());
      }
      return HashCodes.fromBytesNoCopy(bytes);
    }

    @Override
    public int bits() {
      return bits;
    }
  }

  private static final class LinearCongruentialGenerator {
    private long state;

    public LinearCongruentialGenerator(long seed) {
      this.state = seed;
    }

    public double nextDouble() {
      state = 2862933555777941757L * state + 1;
      return ((double) ((int) (state >>> 33) + 1)) / (0x1.0p31);
    }
  }
}

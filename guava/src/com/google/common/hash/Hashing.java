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
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.base.Supplier;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * Static methods to obtain {@link HashFunction} instances, and other static hashing-related
 * utilities.
 *
 * <p>A comparison of the various hash functions can be found
 * <a href="http://goo.gl/jS7HH">here</a>.
 *
 * @author Kevin Bourrillion
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 * @since 11.0
 */
@Beta
@CheckReturnValue
public final class Hashing {
  /**
   * Returns a general-purpose, <b>temporary-use</b>, non-cryptographic hash function. The algorithm
   * the returned function implements is unspecified and subject to change without notice.
   *
   * <p><b>Warning:</b> a new random seed for these functions is chosen each time the {@code
   * Hashing} class is loaded. <b>Do not use this method</b> if hash codes may escape the current
   * process in any way, for example being sent over RPC, or saved to disk.
   *
   * <p>Repeated calls to this method on the same loaded {@code Hashing} class, using the same value
   * for {@code minimumBits}, will return identically-behaving {@link HashFunction} instances.
   *
   * @param minimumBits a positive integer (can be arbitrarily large)
   * @return a hash function, described above, that produces hash codes of length {@code
   *     minimumBits} or greater
   */
  public static HashFunction goodFastHash(int minimumBits) {
    int bits = checkPositiveAndMakeMultipleOf32(minimumBits);

    if (bits == 32) {
      return Murmur3_32Holder.GOOD_FAST_HASH_FUNCTION_32;
    }
    if (bits <= 128) {
      return Murmur3_128Holder.GOOD_FAST_HASH_FUNCTION_128;
    }

    // Otherwise, join together some 128-bit murmur3s
    int hashFunctionsNeeded = (bits + 127) / 128;
    HashFunction[] hashFunctions = new HashFunction[hashFunctionsNeeded];
    hashFunctions[0] = Murmur3_128Holder.GOOD_FAST_HASH_FUNCTION_128;
    int seed = GOOD_FAST_HASH_SEED;
    for (int i = 1; i < hashFunctionsNeeded; i++) {
      seed += 1500450271; // a prime; shouldn't matter
      hashFunctions[i] = murmur3_128(seed);
    }
    return new ConcatenatedHashFunction(hashFunctions);
  }

  /**
   * Used to randomize {@link #goodFastHash} instances, so that programs which persist anything
   * dependent on the hash codes they produce will fail sooner.
   */
  private static final int GOOD_FAST_HASH_SEED = (int) System.currentTimeMillis();

  /**
   * Returns a hash function implementing the
   * <a href="http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp">
   * 32-bit murmur3 algorithm, x86 variant</a> (little-endian variant),
   * using the given seed value.
   *
   * <p>The exact C++ equivalent is the MurmurHash3_x86_32 function (Murmur3A).
   */
  public static HashFunction murmur3_32(int seed) {
    return new Murmur3_32HashFunction(seed);
  }

  /**
   * Returns a hash function implementing the
   * <a href="http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp">
   * 32-bit murmur3 algorithm, x86 variant</a> (little-endian variant),
   * using a seed value of zero.
   *
   * <p>The exact C++ equivalent is the MurmurHash3_x86_32 function (Murmur3A).
   */
  public static HashFunction murmur3_32() {
    return Murmur3_32Holder.MURMUR3_32;
  }

  private static class Murmur3_32Holder {
    static final HashFunction MURMUR3_32 = new Murmur3_32HashFunction(0);

    /** Returned by {@link #goodFastHash} when {@code minimumBits <= 32}. */
    static final HashFunction GOOD_FAST_HASH_FUNCTION_32 = murmur3_32(GOOD_FAST_HASH_SEED);
  }

  /**
   * Returns a hash function implementing the
   * <a href="http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp">
   * 128-bit murmur3 algorithm, x64 variant</a> (little-endian variant),
   * using the given seed value.
   *
   * <p>The exact C++ equivalent is the MurmurHash3_x64_128 function (Murmur3F).
   */
  public static HashFunction murmur3_128(int seed) {
    return new Murmur3_128HashFunction(seed);
  }

  /**
   * Returns a hash function implementing the
   * <a href="http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp">
   * 128-bit murmur3 algorithm, x64 variant</a> (little-endian variant),
   * using a seed value of zero.
   *
   * <p>The exact C++ equivalent is the MurmurHash3_x64_128 function (Murmur3F).
   */
  public static HashFunction murmur3_128() {
    return Murmur3_128Holder.MURMUR3_128;
  }

  private static class Murmur3_128Holder {
    static final HashFunction MURMUR3_128 = new Murmur3_128HashFunction(0);

    /** Returned by {@link #goodFastHash} when {@code 32 < minimumBits <= 128}. */
    static final HashFunction GOOD_FAST_HASH_FUNCTION_128 = murmur3_128(GOOD_FAST_HASH_SEED);
  }

  /**
   * Returns a hash function implementing the
   * <a href="https://131002.net/siphash/">64-bit SipHash-2-4 algorithm</a>
   * using a seed value of {@code k = 00 01 02 ...}.
   *
   * @since 15.0
   */
  public static HashFunction sipHash24() {
    return SipHash24Holder.SIP_HASH_24;
  }

  private static class SipHash24Holder {
    static final HashFunction SIP_HASH_24 =
        new SipHashFunction(2, 4, 0x0706050403020100L, 0x0f0e0d0c0b0a0908L);
  }

  /**
   * Returns a hash function implementing the
   * <a href="https://131002.net/siphash/">64-bit SipHash-2-4 algorithm</a>
   * using the given seed.
   *
   * @since 15.0
   */
  public static HashFunction sipHash24(long k0, long k1) {
    return new SipHashFunction(2, 4, k0, k1);
  }

  /**
   * Returns a hash function implementing the MD5 hash algorithm (128 hash bits) by delegating to
   * the MD5 {@link MessageDigest}.
   */
  public static HashFunction md5() {
    return Md5Holder.MD5;
  }

  private static class Md5Holder {
    static final HashFunction MD5 = new MessageDigestHashFunction("MD5", "Hashing.md5()");
  }

  /**
   * Returns a hash function implementing the SHA-1 algorithm (160 hash bits) by delegating to the
   * SHA-1 {@link MessageDigest}.
   */
  public static HashFunction sha1() {
    return Sha1Holder.SHA_1;
  }

  private static class Sha1Holder {
    static final HashFunction SHA_1 = new MessageDigestHashFunction("SHA-1", "Hashing.sha1()");
  }

  /**
   * Returns a hash function implementing the SHA-256 algorithm (256 hash bits) by delegating to
   * the SHA-256 {@link MessageDigest}.
   */
  public static HashFunction sha256() {
    return Sha256Holder.SHA_256;
  }

  private static class Sha256Holder {
    static final HashFunction SHA_256 =
        new MessageDigestHashFunction("SHA-256", "Hashing.sha256()");
  }

  /**
   * Returns a hash function implementing the SHA-384 algorithm (384 hash bits) by delegating to
   * the SHA-384 {@link MessageDigest}.
   *
   * @since 19.0
   */
  public static HashFunction sha384() {
    return Sha384Holder.SHA_384;
  }

  private static class Sha384Holder {
    static final HashFunction SHA_384 =
        new MessageDigestHashFunction("SHA-384", "Hashing.sha384()");
  }

  /**
   * Returns a hash function implementing the SHA-512 algorithm (512 hash bits) by delegating to the
   * SHA-512 {@link MessageDigest}.
   */
  public static HashFunction sha512() {
    return Sha512Holder.SHA_512;
  }

  private static class Sha512Holder {
    static final HashFunction SHA_512 =
        new MessageDigestHashFunction("SHA-512", "Hashing.sha512()");
  }

  /**
   * Returns a hash function implementing the CRC32C checksum algorithm (32 hash bits) as described
   * by RFC 3720, Section 12.1.
   *
   * @since 18.0
   */
  public static HashFunction crc32c() {
    return Crc32cHolder.CRC_32_C;
  }

  private static final class Crc32cHolder {
    static final HashFunction CRC_32_C = new Crc32cHashFunction();
  }

  /**
   * Returns a hash function implementing the CRC-32 checksum algorithm (32 hash bits) by delegating
   * to the {@link CRC32} {@link Checksum}.
   *
   * <p>To get the {@code long} value equivalent to {@link Checksum#getValue()} for a
   * {@code HashCode} produced by this function, use {@link HashCode#padToLong()}.
   *
   * @since 14.0
   */
  public static HashFunction crc32() {
    return Crc32Holder.CRC_32;
  }

  private static class Crc32Holder {
    static final HashFunction CRC_32 = checksumHashFunction(ChecksumType.CRC_32, "Hashing.crc32()");
  }

  /**
   * Returns a hash function implementing the Adler-32 checksum algorithm (32 hash bits) by
   * delegating to the {@link Adler32} {@link Checksum}.
   *
   * <p>To get the {@code long} value equivalent to {@link Checksum#getValue()} for a
   * {@code HashCode} produced by this function, use {@link HashCode#padToLong()}.
   *
   * @since 14.0
   */
  public static HashFunction adler32() {
    return Adler32Holder.ADLER_32;
  }

  private static class Adler32Holder {
    static final HashFunction ADLER_32 =
        checksumHashFunction(ChecksumType.ADLER_32, "Hashing.adler32()");
  }

  private static HashFunction checksumHashFunction(ChecksumType type, String toString) {
    return new ChecksumHashFunction(type, type.bits, toString);
  }

  enum ChecksumType implements Supplier<Checksum> {
    CRC_32(32) {
      @Override
      public Checksum get() {
        return new CRC32();
      }
    },
    ADLER_32(32) {
      @Override
      public Checksum get() {
        return new Adler32();
      }
    };

    private final int bits;

    ChecksumType(int bits) {
      this.bits = bits;
    }

    @Override
    public abstract Checksum get();
  }

  /**
   * Assigns to {@code hashCode} a "bucket" in the range {@code [0, buckets)}, in a uniform manner
   * that minimizes the need for remapping as {@code buckets} grows. That is, {@code
   * consistentHash(h, n)} equals:
   *
   * <ul>
   * <li>{@code n - 1}, with approximate probability {@code 1/n}
   * <li>{@code consistentHash(h, n - 1)}, otherwise (probability {@code 1 - 1/n})
   * </ul>
   *
   * <p>This method is suitable for the common use case of dividing work among buckets that meet the
   * following conditions:
   *
   * <ul>
   * <li>You want to assign the same fraction of inputs to each bucket.
   * <li>When you reduce the number of buckets, you can accept that the most recently added buckets
   * will be removed first. More concretely, if you are dividing traffic among tasks, you can
   * decrease the number of tasks from 15 and 10, killing off the final 5 tasks, and {@code
   * consistentHash} will handle it. If, however, you are dividing traffic among servers {@code
   * alpha}, {@code bravo}, and {@code charlie} and you occasionally need to take each of the
   * servers offline, {@code consistentHash} will be a poor fit: It provides no way for you to
   * specify which of the three buckets is disappearing. Thus, if your buckets change from {@code
   * [alpha, bravo, charlie]} to {@code [bravo, charlie]}, it will assign all the old {@code alpha}
   * traffic to {@code bravo} and all the old {@code bravo} traffic to {@code charlie}, rather than
   * letting {@code bravo} keep its traffic.
   * </ul>
   *
   *
   * <p>See the <a href="http://en.wikipedia.org/wiki/Consistent_hashing">Wikipedia article on
   * consistent hashing</a> for more information.
   */
  public static int consistentHash(HashCode hashCode, int buckets) {
    return consistentHash(hashCode.padToLong(), buckets);
  }

  /**
   * Assigns to {@code input} a "bucket" in the range {@code [0, buckets)}, in a uniform manner that
   * minimizes the need for remapping as {@code buckets} grows. That is, {@code consistentHash(h,
   * n)} equals:
   *
   * <ul>
   * <li>{@code n - 1}, with approximate probability {@code 1/n}
   * <li>{@code consistentHash(h, n - 1)}, otherwise (probability {@code 1 - 1/n})
   * </ul>
   *
   * <p>This method is suitable for the common use case of dividing work among buckets that meet the
   * following conditions:
   *
   * <ul>
   * <li>You want to assign the same fraction of inputs to each bucket.
   * <li>When you reduce the number of buckets, you can accept that the most recently added buckets
   * will be removed first. More concretely, if you are dividing traffic among tasks, you can
   * decrease the number of tasks from 15 and 10, killing off the final 5 tasks, and {@code
   * consistentHash} will handle it. If, however, you are dividing traffic among servers {@code
   * alpha}, {@code bravo}, and {@code charlie} and you occasionally need to take each of the
   * servers offline, {@code consistentHash} will be a poor fit: It provides no way for you to
   * specify which of the three buckets is disappearing. Thus, if your buckets change from {@code
   * [alpha, bravo, charlie]} to {@code [bravo, charlie]}, it will assign all the old {@code alpha}
   * traffic to {@code bravo} and all the old {@code bravo} traffic to {@code charlie}, rather than
   * letting {@code bravo} keep its traffic.
   * </ul>
   *
   *
   * <p>See the <a href="http://en.wikipedia.org/wiki/Consistent_hashing">Wikipedia article on
   * consistent hashing</a> for more information.
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
      checkArgument(
          nextBytes.length == resultBytes.length, "All hashcodes must have the same bit length.");
      for (int i = 0; i < nextBytes.length; i++) {
        resultBytes[i] = (byte) (resultBytes[i] * 37 ^ nextBytes[i]);
      }
    }
    return HashCode.fromBytesNoCopy(resultBytes);
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
      checkArgument(
          nextBytes.length == resultBytes.length, "All hashcodes must have the same bit length.");
      for (int i = 0; i < nextBytes.length; i++) {
        resultBytes[i] += nextBytes[i];
      }
    }
    return HashCode.fromBytesNoCopy(resultBytes);
  }

  /**
   * Checks that the passed argument is positive, and ceils it to a multiple of 32.
   */
  static int checkPositiveAndMakeMultipleOf32(int bits) {
    checkArgument(bits > 0, "Number of bits must be positive");
    return (bits + 31) & ~31;
  }

  /**
   * Returns a hash function which computes its hash code by concatenating the hash codes of the
   * underlying hash functions together. This can be useful if you need to generate hash codes
   * of a specific length.
   *
   * <p>For example, if you need 1024-bit hash codes, you could join two {@link Hashing#sha512}
   * hash functions together: {@code Hashing.concatenating(Hashing.sha512(), Hashing.sha512())}.
   *
   * @since 19.0
   */
  public static HashFunction concatenating(
      HashFunction first, HashFunction second, HashFunction... rest) {
    // We can't use Lists.asList() here because there's no hash->collect dependency
    List<HashFunction> list = new ArrayList<HashFunction>();
    list.add(first);
    list.add(second);
    for (HashFunction hashFunc : rest) {
      list.add(hashFunc);
    }
    return new ConcatenatedHashFunction(list.toArray(new HashFunction[0]));
  }

  /**
   * Returns a hash function which computes its hash code by concatenating the hash codes of the
   * underlying hash functions together. This can be useful if you need to generate hash codes
   * of a specific length.
   *
   * <p>For example, if you need 1024-bit hash codes, you could join two {@link Hashing#sha512}
   * hash functions together: {@code Hashing.concatenating(Hashing.sha512(), Hashing.sha512())}.
   *
   * @since 19.0
   */
  public static HashFunction concatenating(Iterable<HashFunction> hashFunctions) {
    checkNotNull(hashFunctions);
    // We can't use Iterables.toArray() here because there's no hash->collect dependency
    List<HashFunction> list = new ArrayList<HashFunction>();
    for (HashFunction hashFunction : hashFunctions) {
      list.add(hashFunction);
    }
    checkArgument(list.size() > 0, "number of hash functions (%s) must be > 0", list.size());
    return new ConcatenatedHashFunction(list.toArray(new HashFunction[0]));
  }

  private static final class ConcatenatedHashFunction extends AbstractCompositeHashFunction {
    private final int bits;

    private ConcatenatedHashFunction(HashFunction... functions) {
      super(functions);
      int bitSum = 0;
      for (HashFunction function : functions) {
        bitSum += function.bits();
        checkArgument(
            function.bits() % 8 == 0,
            "the number of bits (%s) in hashFunction (%s) must be divisible by 8",
            function.bits(),
            function);
      }
      this.bits = bitSum;
    }

    @Override
    HashCode makeHash(Hasher[] hashers) {
      byte[] bytes = new byte[bits / 8];
      int i = 0;
      for (Hasher hasher : hashers) {
        HashCode newHash = hasher.hash();
        i += newHash.writeBytesTo(bytes, i, newHash.bits() / 8);
      }
      return HashCode.fromBytesNoCopy(bytes);
    }

    @Override
    public int bits() {
      return bits;
    }

    @Override
    public boolean equals(@Nullable Object object) {
      if (object instanceof ConcatenatedHashFunction) {
        ConcatenatedHashFunction other = (ConcatenatedHashFunction) object;
        return Arrays.equals(functions, other.functions);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(functions) * 31 + bits;
    }
  }

  /**
   * Linear CongruentialGenerator to use for consistent hashing.
   * See http://en.wikipedia.org/wiki/Linear_congruential_generator
   */
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

  private Hashing() {}
}

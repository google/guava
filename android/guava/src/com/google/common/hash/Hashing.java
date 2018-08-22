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
import com.google.errorprone.annotations.Immutable;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import javax.crypto.spec.SecretKeySpec;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Static methods to obtain {@link HashFunction} instances, and other static hashing-related
 * utilities.
 *
 * <p>A comparison of the various hash functions can be found <a
 * href="http://goo.gl/jS7HH">here</a>.
 *
 * @author Kevin Bourrillion
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 * @since 11.0
 */
@Beta
public final class Hashing {
  /**
   * Returns a general-purpose, <b>temporary-use</b>, non-cryptographic hash function. The algorithm
   * the returned function implements is unspecified and subject to change without notice.
   *
   * <p><b>Warning:</b> a new random seed for these functions is chosen each time the {@code
   * Hashing} class is loaded. <b>Do not use this method</b> if hash codes may escape the current
   * process in any way, for example being sent over RPC, or saved to disk. For a general-purpose,
   * non-cryptographic hash function that will never change behavior, we suggest {@link
   * #murmur3_128}.
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
      return Murmur3_32HashFunction.GOOD_FAST_HASH_32;
    }
    if (bits <= 128) {
      return Murmur3_128HashFunction.GOOD_FAST_HASH_128;
    }

    // Otherwise, join together some 128-bit murmur3s
    int hashFunctionsNeeded = (bits + 127) / 128;
    HashFunction[] hashFunctions = new HashFunction[hashFunctionsNeeded];
    hashFunctions[0] = Murmur3_128HashFunction.GOOD_FAST_HASH_128;
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
  static final int GOOD_FAST_HASH_SEED = (int) System.currentTimeMillis();

  /**
   * Returns a hash function implementing the <a
   * href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">32-bit murmur3
   * algorithm, x86 variant</a> (little-endian variant), using the given seed value.
   *
   * <p>The exact C++ equivalent is the MurmurHash3_x86_32 function (Murmur3A).
   */
  public static HashFunction murmur3_32(int seed) {
    return new Murmur3_32HashFunction(seed);
  }

  /**
   * Returns a hash function implementing the <a
   * href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">32-bit murmur3
   * algorithm, x86 variant</a> (little-endian variant), using a seed value of zero.
   *
   * <p>The exact C++ equivalent is the MurmurHash3_x86_32 function (Murmur3A).
   */
  public static HashFunction murmur3_32() {
    return Murmur3_32HashFunction.MURMUR3_32;
  }

  /**
   * Returns a hash function implementing the <a
   * href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">128-bit murmur3
   * algorithm, x64 variant</a> (little-endian variant), using the given seed value.
   *
   * <p>The exact C++ equivalent is the MurmurHash3_x64_128 function (Murmur3F).
   */
  public static HashFunction murmur3_128(int seed) {
    return new Murmur3_128HashFunction(seed);
  }

  /**
   * Returns a hash function implementing the <a
   * href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">128-bit murmur3
   * algorithm, x64 variant</a> (little-endian variant), using a seed value of zero.
   *
   * <p>The exact C++ equivalent is the MurmurHash3_x64_128 function (Murmur3F).
   */
  public static HashFunction murmur3_128() {
    return Murmur3_128HashFunction.MURMUR3_128;
  }

  /**
   * Returns a hash function implementing the <a href="https://131002.net/siphash/">64-bit
   * SipHash-2-4 algorithm</a> using a seed value of {@code k = 00 01 02 ...}.
   *
   * @since 15.0
   */
  public static HashFunction sipHash24() {
    return SipHashFunction.SIP_HASH_24;
  }

  /**
   * Returns a hash function implementing the <a href="https://131002.net/siphash/">64-bit
   * SipHash-2-4 algorithm</a> using the given seed.
   *
   * @since 15.0
   */
  public static HashFunction sipHash24(long k0, long k1) {
    return new SipHashFunction(2, 4, k0, k1);
  }

  /**
   * Returns a hash function implementing the MD5 hash algorithm (128 hash bits).
   *
   * @deprecated If you must interoperate with a system that requires MD5, then use this method,
   *     despite its deprecation. But if you can choose your hash function, avoid MD5, which is
   *     neither fast nor secure. As of January 2017, we suggest:
   *     <ul>
   *       <li>For security:
   *           {@link Hashing#sha256} or a higher-level API.
   *       <li>For speed: {@link Hashing#goodFastHash}, though see its docs for caveats.
   *     </ul>
   */
  @Deprecated
  public static HashFunction md5() {
    return Md5Holder.MD5;
  }

  private static class Md5Holder {
    static final HashFunction MD5 = new MessageDigestHashFunction("MD5", "Hashing.md5()");
  }

  /**
   * Returns a hash function implementing the SHA-1 algorithm (160 hash bits).
   *
   * @deprecated If you must interoperate with a system that requires SHA-1, then use this method,
   *     despite its deprecation. But if you can choose your hash function, avoid SHA-1, which is
   *     neither fast nor secure. As of January 2017, we suggest:
   *     <ul>
   *       <li>For security:
   *           {@link Hashing#sha256} or a higher-level API.
   *       <li>For speed: {@link Hashing#goodFastHash}, though see its docs for caveats.
   *     </ul>
   */
  @Deprecated
  public static HashFunction sha1() {
    return Sha1Holder.SHA_1;
  }

  private static class Sha1Holder {
    static final HashFunction SHA_1 = new MessageDigestHashFunction("SHA-1", "Hashing.sha1()");
  }

  /** Returns a hash function implementing the SHA-256 algorithm (256 hash bits). */
  public static HashFunction sha256() {
    return Sha256Holder.SHA_256;
  }

  private static class Sha256Holder {
    static final HashFunction SHA_256 =
        new MessageDigestHashFunction("SHA-256", "Hashing.sha256()");
  }

  /**
   * Returns a hash function implementing the SHA-384 algorithm (384 hash bits).
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

  /** Returns a hash function implementing the SHA-512 algorithm (512 hash bits). */
  public static HashFunction sha512() {
    return Sha512Holder.SHA_512;
  }

  private static class Sha512Holder {
    static final HashFunction SHA_512 =
        new MessageDigestHashFunction("SHA-512", "Hashing.sha512()");
  }

  /**
   * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
   * MD5 (128 hash bits) hash function and the given secret key.
   *
   *
   * @param key the secret key
   * @throws IllegalArgumentException if the given key is inappropriate for initializing this MAC
   * @since 20.0
   */
  public static HashFunction hmacMd5(Key key) {
    return new MacHashFunction("HmacMD5", key, hmacToString("hmacMd5", key));
  }

  /**
   * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
   * MD5 (128 hash bits) hash function and a {@link SecretKeySpec} created from the given byte array
   * and the MD5 algorithm.
   *
   *
   * @param key the key material of the secret key
   * @since 20.0
   */
  public static HashFunction hmacMd5(byte[] key) {
    return hmacMd5(new SecretKeySpec(checkNotNull(key), "HmacMD5"));
  }

  /**
   * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
   * SHA-1 (160 hash bits) hash function and the given secret key.
   *
   *
   * @param key the secret key
   * @throws IllegalArgumentException if the given key is inappropriate for initializing this MAC
   * @since 20.0
   */
  public static HashFunction hmacSha1(Key key) {
    return new MacHashFunction("HmacSHA1", key, hmacToString("hmacSha1", key));
  }

  /**
   * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
   * SHA-1 (160 hash bits) hash function and a {@link SecretKeySpec} created from the given byte
   * array and the SHA-1 algorithm.
   *
   *
   * @param key the key material of the secret key
   * @since 20.0
   */
  public static HashFunction hmacSha1(byte[] key) {
    return hmacSha1(new SecretKeySpec(checkNotNull(key), "HmacSHA1"));
  }

  /**
   * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
   * SHA-256 (256 hash bits) hash function and the given secret key.
   *
   *
   * @param key the secret key
   * @throws IllegalArgumentException if the given key is inappropriate for initializing this MAC
   * @since 20.0
   */
  public static HashFunction hmacSha256(Key key) {
    return new MacHashFunction("HmacSHA256", key, hmacToString("hmacSha256", key));
  }

  /**
   * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
   * SHA-256 (256 hash bits) hash function and a {@link SecretKeySpec} created from the given byte
   * array and the SHA-256 algorithm.
   *
   *
   * @param key the key material of the secret key
   * @since 20.0
   */
  public static HashFunction hmacSha256(byte[] key) {
    return hmacSha256(new SecretKeySpec(checkNotNull(key), "HmacSHA256"));
  }

  /**
   * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
   * SHA-512 (512 hash bits) hash function and the given secret key.
   *
   *
   * @param key the secret key
   * @throws IllegalArgumentException if the given key is inappropriate for initializing this MAC
   * @since 20.0
   */
  public static HashFunction hmacSha512(Key key) {
    return new MacHashFunction("HmacSHA512", key, hmacToString("hmacSha512", key));
  }

  /**
   * Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
   * SHA-512 (512 hash bits) hash function and a {@link SecretKeySpec} created from the given byte
   * array and the SHA-512 algorithm.
   *
   *
   * @param key the key material of the secret key
   * @since 20.0
   */
  public static HashFunction hmacSha512(byte[] key) {
    return hmacSha512(new SecretKeySpec(checkNotNull(key), "HmacSHA512"));
  }

  private static String hmacToString(String methodName, Key key) {
    return String.format(
        "Hashing.%s(Key[algorithm=%s, format=%s])",
        methodName, key.getAlgorithm(), key.getFormat());
  }

  /**
   * Returns a hash function implementing the CRC32C checksum algorithm (32 hash bits) as described
   * by RFC 3720, Section 12.1.
   *
   * <p>This function is best understood as a <a
   * href="https://en.wikipedia.org/wiki/Checksum">checksum</a> rather than a true <a
   * href="https://en.wikipedia.org/wiki/Hash_function">hash function</a>.
   *
   * @since 18.0
   */
  public static HashFunction crc32c() {
    return Crc32cHashFunction.CRC_32_C;
  }

  /**
   * Returns a hash function implementing the CRC-32 checksum algorithm (32 hash bits).
   *
   * <p>To get the {@code long} value equivalent to {@link Checksum#getValue()} for a {@code
   * HashCode} produced by this function, use {@link HashCode#padToLong()}.
   *
   * <p>This function is best understood as a <a
   * href="https://en.wikipedia.org/wiki/Checksum">checksum</a> rather than a true <a
   * href="https://en.wikipedia.org/wiki/Hash_function">hash function</a>.
   *
   * @since 14.0
   */
  public static HashFunction crc32() {
    return ChecksumType.CRC_32.hashFunction;
  }

  /**
   * Returns a hash function implementing the Adler-32 checksum algorithm (32 hash bits).
   *
   * <p>To get the {@code long} value equivalent to {@link Checksum#getValue()} for a {@code
   * HashCode} produced by this function, use {@link HashCode#padToLong()}.
   *
   * <p>This function is best understood as a <a
   * href="https://en.wikipedia.org/wiki/Checksum">checksum</a> rather than a true <a
   * href="https://en.wikipedia.org/wiki/Hash_function">hash function</a>.
   *
   * @since 14.0
   */
  public static HashFunction adler32() {
    return ChecksumType.ADLER_32.hashFunction;
  }

  @Immutable
  enum ChecksumType implements ImmutableSupplier<Checksum> {
    CRC_32("Hashing.crc32()") {
      @Override
      public Checksum get() {
        return new CRC32();
      }
    },
    ADLER_32("Hashing.adler32()") {
      @Override
      public Checksum get() {
        return new Adler32();
      }
    };

    public final HashFunction hashFunction;

    ChecksumType(String toString) {
      this.hashFunction = new ChecksumHashFunction(this, 32, toString);
    }
  }

  /**
   * Returns a hash function implementing FarmHash's Fingerprint64, an open-source algorithm.
   *
   * <p>This is designed for generating persistent fingerprints of strings. It isn't
   * cryptographically secure, but it produces a high-quality hash with fewer collisions than some
   * alternatives we've used in the past.
   *
   * <p>FarmHash fingerprints are encoded by {@link HashCode#asBytes} in little-endian order. This
   * means {@link HashCode#asLong} is guaranteed to return the same value that
   * farmhash::Fingerprint64() would for the same input (when compared using {@link
   * com.google.common.primitives.UnsignedLongs}'s encoding of 64-bit unsigned numbers).
   *
   * <p>This function is best understood as a <a
   * href="https://en.wikipedia.org/wiki/Fingerprint_(computing)">fingerprint</a> rather than a true
   * <a href="https://en.wikipedia.org/wiki/Hash_function">hash function</a>.
   *
   * @since 20.0
   */
  public static HashFunction farmHashFingerprint64() {
    return FarmHashFingerprint64.FARMHASH_FINGERPRINT_64;
  }

  /**
   * Assigns to {@code hashCode} a "bucket" in the range {@code [0, buckets)}, in a uniform manner
   * that minimizes the need for remapping as {@code buckets} grows. That is, {@code
   * consistentHash(h, n)} equals:
   *
   * <ul>
   *   <li>{@code n - 1}, with approximate probability {@code 1/n}
   *   <li>{@code consistentHash(h, n - 1)}, otherwise (probability {@code 1 - 1/n})
   * </ul>
   *
   * <p>This method is suitable for the common use case of dividing work among buckets that meet the
   * following conditions:
   *
   * <ul>
   *   <li>You want to assign the same fraction of inputs to each bucket.
   *   <li>When you reduce the number of buckets, you can accept that the most recently added
   *       buckets will be removed first. More concretely, if you are dividing traffic among tasks,
   *       you can decrease the number of tasks from 15 and 10, killing off the final 5 tasks, and
   *       {@code consistentHash} will handle it. If, however, you are dividing traffic among
   *       servers {@code alpha}, {@code bravo}, and {@code charlie} and you occasionally need to
   *       take each of the servers offline, {@code consistentHash} will be a poor fit: It provides
   *       no way for you to specify which of the three buckets is disappearing. Thus, if your
   *       buckets change from {@code [alpha, bravo, charlie]} to {@code [bravo, charlie]}, it will
   *       assign all the old {@code alpha} traffic to {@code bravo} and all the old {@code bravo}
   *       traffic to {@code charlie}, rather than letting {@code bravo} keep its traffic.
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
   *   <li>{@code n - 1}, with approximate probability {@code 1/n}
   *   <li>{@code consistentHash(h, n - 1)}, otherwise (probability {@code 1 - 1/n})
   * </ul>
   *
   * <p>This method is suitable for the common use case of dividing work among buckets that meet the
   * following conditions:
   *
   * <ul>
   *   <li>You want to assign the same fraction of inputs to each bucket.
   *   <li>When you reduce the number of buckets, you can accept that the most recently added
   *       buckets will be removed first. More concretely, if you are dividing traffic among tasks,
   *       you can decrease the number of tasks from 15 and 10, killing off the final 5 tasks, and
   *       {@code consistentHash} will handle it. If, however, you are dividing traffic among
   *       servers {@code alpha}, {@code bravo}, and {@code charlie} and you occasionally need to
   *       take each of the servers offline, {@code consistentHash} will be a poor fit: It provides
   *       no way for you to specify which of the three buckets is disappearing. Thus, if your
   *       buckets change from {@code [alpha, bravo, charlie]} to {@code [bravo, charlie]}, it will
   *       assign all the old {@code alpha} traffic to {@code bravo} and all the old {@code bravo}
   *       traffic to {@code charlie}, rather than letting {@code bravo} keep its traffic.
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
   * Returns a hash code, having the same bit length as each of the input hash codes, that combines
   * the information of these hash codes in an ordered fashion. That is, whenever two equal hash
   * codes are produced by two calls to this method, it is <i>as likely as possible</i> that each
   * was computed from the <i>same</i> input hash codes in the <i>same</i> order.
   *
   * @throws IllegalArgumentException if {@code hashCodes} is empty, or the hash codes do not all
   *     have the same bit length
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
   * Returns a hash code, having the same bit length as each of the input hash codes, that combines
   * the information of these hash codes in an unordered fashion. That is, whenever two equal hash
   * codes are produced by two calls to this method, it is <i>as likely as possible</i> that each
   * was computed from the <i>same</i> input hash codes in <i>some</i> order.
   *
   * @throws IllegalArgumentException if {@code hashCodes} is empty, or the hash codes do not all
   *     have the same bit length
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

  /** Checks that the passed argument is positive, and ceils it to a multiple of 32. */
  static int checkPositiveAndMakeMultipleOf32(int bits) {
    checkArgument(bits > 0, "Number of bits must be positive");
    return (bits + 31) & ~31;
  }

  /**
   * Returns a hash function which computes its hash code by concatenating the hash codes of the
   * underlying hash functions together. This can be useful if you need to generate hash codes of a
   * specific length.
   *
   * <p>For example, if you need 1024-bit hash codes, you could join two {@link Hashing#sha512} hash
   * functions together: {@code Hashing.concatenating(Hashing.sha512(), Hashing.sha512())}.
   *
   * @since 19.0
   */
  public static HashFunction concatenating(
      HashFunction first, HashFunction second, HashFunction... rest) {
    // We can't use Lists.asList() here because there's no hash->collect dependency
    List<HashFunction> list = new ArrayList<>();
    list.add(first);
    list.add(second);
    list.addAll(Arrays.asList(rest));
    return new ConcatenatedHashFunction(list.toArray(new HashFunction[0]));
  }

  /**
   * Returns a hash function which computes its hash code by concatenating the hash codes of the
   * underlying hash functions together. This can be useful if you need to generate hash codes of a
   * specific length.
   *
   * <p>For example, if you need 1024-bit hash codes, you could join two {@link Hashing#sha512} hash
   * functions together: {@code Hashing.concatenating(Hashing.sha512(), Hashing.sha512())}.
   *
   * @since 19.0
   */
  public static HashFunction concatenating(Iterable<HashFunction> hashFunctions) {
    checkNotNull(hashFunctions);
    // We can't use Iterables.toArray() here because there's no hash->collect dependency
    List<HashFunction> list = new ArrayList<>();
    for (HashFunction hashFunction : hashFunctions) {
      list.add(hashFunction);
    }
    checkArgument(list.size() > 0, "number of hash functions (%s) must be > 0", list.size());
    return new ConcatenatedHashFunction(list.toArray(new HashFunction[0]));
  }

  private static final class ConcatenatedHashFunction extends AbstractCompositeHashFunction {

    private ConcatenatedHashFunction(HashFunction... functions) {
      super(functions);
      for (HashFunction function : functions) {
        checkArgument(
            function.bits() % 8 == 0,
            "the number of bits (%s) in hashFunction (%s) must be divisible by 8",
            function.bits(),
            function);
      }
    }

    @Override
    HashCode makeHash(Hasher[] hashers) {
      byte[] bytes = new byte[bits() / 8];
      int i = 0;
      for (Hasher hasher : hashers) {
        HashCode newHash = hasher.hash();
        i += newHash.writeBytesTo(bytes, i, newHash.bits() / 8);
      }
      return HashCode.fromBytesNoCopy(bytes);
    }

    @Override
    public int bits() {
      int bitSum = 0;
      for (HashFunction function : functions) {
        bitSum += function.bits();
      }
      return bitSum;
    }

    @Override
    public boolean equals(@NullableDecl Object object) {
      if (object instanceof ConcatenatedHashFunction) {
        ConcatenatedHashFunction other = (ConcatenatedHashFunction) object;
        return Arrays.equals(functions, other.functions);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(functions);
    }
  }

  /**
   * Linear CongruentialGenerator to use for consistent hashing. See
   * http://en.wikipedia.org/wiki/Linear_congruential_generator
   */
  private static final class LinearCongruentialGenerator {
    private long state;

    public LinearCongruentialGenerator(long seed) {
      this.state = seed;
    }

    public double nextDouble() {
      state = 2862933555777941757L * state + 1;
      return ((double) ((int) (state >>> 33) + 1)) / 0x1.0p31;
    }
  }

  private Hashing() {}
}

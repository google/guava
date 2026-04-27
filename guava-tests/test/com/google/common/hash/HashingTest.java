/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.hash;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.shuffle;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table.Cell;
import com.google.common.primitives.Ints;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.util.concurrent.AtomicLongMap;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit tests for {@link Hashing}.
 *
 * <p>TODO(b/33919189): Migrate repeated testing methods to {@link HashTestUtils} and tweak unit
 * tests to reference them from there.
 *
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 */
@NullUnmarked
public class HashingTest extends TestCase {
  public void testMd5() {
    HashTestUtils.checkAvalanche(Hashing.md5(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.md5());
    HashTestUtils.checkNoFunnels(Hashing.md5());
    HashTestUtils.assertInvariants(Hashing.md5());
    assertThat(Hashing.md5().toString()).isEqualTo("Hashing.md5()");
  }

  public void testSha1() {
    HashTestUtils.checkAvalanche(Hashing.sha1(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sha1());
    HashTestUtils.checkNoFunnels(Hashing.sha1());
    HashTestUtils.assertInvariants(Hashing.sha1());
    assertThat(Hashing.sha1().toString()).isEqualTo("Hashing.sha1()");
  }

  public void testSha256() {
    HashTestUtils.checkAvalanche(Hashing.sha256(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sha256());
    HashTestUtils.checkNoFunnels(Hashing.sha256());
    HashTestUtils.assertInvariants(Hashing.sha256());
    assertThat(Hashing.sha256().toString()).isEqualTo("Hashing.sha256()");
  }

  @J2ktIncompatible
  public void testSha384() {
    HashTestUtils.checkAvalanche(Hashing.sha384(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sha384());
    HashTestUtils.checkNoFunnels(Hashing.sha384());
    HashTestUtils.assertInvariants(Hashing.sha384());
    assertThat(Hashing.sha384().toString()).isEqualTo("Hashing.sha384()");
  }

  @J2ktIncompatible
  public void testSha512() {
    HashTestUtils.checkAvalanche(Hashing.sha512(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sha512());
    HashTestUtils.checkNoFunnels(Hashing.sha512());
    HashTestUtils.assertInvariants(Hashing.sha512());
    assertThat(Hashing.sha512().toString()).isEqualTo("Hashing.sha512()");
  }

  @J2ktIncompatible
  public void testCrc32() {
    HashTestUtils.assertInvariants(Hashing.crc32());
    assertThat(Hashing.crc32().toString()).isEqualTo("Hashing.crc32()");
  }

  @J2ktIncompatible
  public void testAdler32() {
    HashTestUtils.assertInvariants(Hashing.adler32());
    assertThat(Hashing.adler32().toString()).isEqualTo("Hashing.adler32()");
  }

  @J2ktIncompatible
  public void testMurmur3_128() {
    HashTestUtils.check2BitAvalanche(Hashing.murmur3_128(), 250, 0.20);
    HashTestUtils.checkAvalanche(Hashing.murmur3_128(), 250, 0.17);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.murmur3_128());
    HashTestUtils.checkNoFunnels(Hashing.murmur3_128());
    HashTestUtils.assertInvariants(Hashing.murmur3_128());
    assertThat(Hashing.murmur3_128().toString()).isEqualTo("Hashing.murmur3_128(0)");
  }

  @J2ktIncompatible
  public void testMurmur3_32() {
    HashTestUtils.check2BitAvalanche(Hashing.murmur3_32(), 250, 0.20);
    HashTestUtils.checkAvalanche(Hashing.murmur3_32(), 250, 0.17);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.murmur3_32());
    HashTestUtils.checkNoFunnels(Hashing.murmur3_32());
    HashTestUtils.assertInvariants(Hashing.murmur3_32());
    assertThat(Hashing.murmur3_32().toString()).isEqualTo("Hashing.murmur3_32(0)");
  }

  @J2ktIncompatible
  public void testSipHash24() {
    HashTestUtils.check2BitAvalanche(Hashing.sipHash24(), 250, 0.14);
    HashTestUtils.checkAvalanche(Hashing.sipHash24(), 250, 0.10);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sipHash24());
    HashTestUtils.checkNoFunnels(Hashing.sipHash24());
    HashTestUtils.assertInvariants(Hashing.sipHash24());
    assertThat(Hashing.sipHash24().toString())
        .isEqualTo("Hashing.sipHash24(506097522914230528, 1084818905618843912)");
  }

  @J2ktIncompatible
  public void testFingerprint2011() {
    HashTestUtils.check2BitAvalanche(Hashing.fingerprint2011(), 100, 0.4);
    HashTestUtils.checkAvalanche(Hashing.fingerprint2011(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.fingerprint2011());
    HashTestUtils.checkNoFunnels(Hashing.fingerprint2011());
    HashTestUtils.assertInvariants(Hashing.fingerprint2011());
    assertThat(Hashing.fingerprint2011().toString()).isEqualTo("Hashing.fingerprint2011()");
  }

  @J2ktIncompatible
  @AndroidIncompatible // slow TODO(cpovirk): Maybe just reduce iterations under Android.
  public void testGoodFastHash() {
    for (int i = 1; i < 200; i += 17) {
      HashFunction hasher = Hashing.goodFastHash(i);
      assertThat(hasher.bits()).isAtLeast(i);
      HashTestUtils.assertInvariants(hasher);
    }
  }

  // goodFastHash(32) uses Murmur3_32. Use the same epsilon bounds.
  @J2ktIncompatible
  public void testGoodFastHash32() {
    HashTestUtils.check2BitAvalanche(Hashing.goodFastHash(32), 250, 0.20);
    HashTestUtils.checkAvalanche(Hashing.goodFastHash(32), 250, 0.17);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.goodFastHash(32));
    HashTestUtils.checkNoFunnels(Hashing.goodFastHash(32));
    HashTestUtils.assertInvariants(Hashing.goodFastHash(32));
  }

  // goodFastHash(128) uses Murmur3_128. Use the same epsilon bounds.
  @J2ktIncompatible
  public void testGoodFastHash128() {
    HashTestUtils.check2BitAvalanche(Hashing.goodFastHash(128), 250, 0.20);
    HashTestUtils.checkAvalanche(Hashing.goodFastHash(128), 500, 0.17);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.goodFastHash(128));
    HashTestUtils.checkNoFunnels(Hashing.goodFastHash(128));
    HashTestUtils.assertInvariants(Hashing.goodFastHash(128));
  }

  // goodFastHash(256) uses Murmur3_128. Use the same epsilon bounds.
  @J2ktIncompatible
  public void testGoodFastHash256() {
    HashTestUtils.check2BitAvalanche(Hashing.goodFastHash(256), 250, 0.20);
    HashTestUtils.checkAvalanche(Hashing.goodFastHash(256), 500, 0.17);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.goodFastHash(256));
    HashTestUtils.checkNoFunnels(Hashing.goodFastHash(256));
    HashTestUtils.assertInvariants(Hashing.goodFastHash(256));
  }

  @J2ktIncompatible
  public void testConsistentHash_correctness() {
    long[] interestingValues = {-1, 0, 1, 2, Long.MAX_VALUE, Long.MIN_VALUE};
    for (long h : interestingValues) {
      checkConsistentHashCorrectness(h);
    }
    Random r = new Random(7);
    for (int i = 0; i < 20; i++) {
      checkConsistentHashCorrectness(r.nextLong());
    }
  }

  @J2ktIncompatible
  private void checkConsistentHashCorrectness(long hashCode) {
    int last = 0;
    for (int shards = 1; shards <= 100000; shards++) {
      int b = Hashing.consistentHash(hashCode, shards);
      if (b != last) {
        assertEquals(shards - 1, b);
        last = b;
      }
    }
  }

  @J2ktIncompatible
  public void testConsistentHash_probabilities() {
    AtomicLongMap<Integer> map = AtomicLongMap.create();
    Random r = new Random(9);
    for (int i = 0; i < ITERS; i++) {
      countRemaps(r.nextLong(), map);
    }
    for (int shard = 2; shard <= MAX_SHARDS; shard++) {
      // Rough: don't exceed 1.2x the expected number of remaps by more than 20
      assertThat((double) map.get(shard)).isAtMost(1.2 * ITERS / shard + 20);
    }
  }

  @J2ktIncompatible
  private void countRemaps(long h, AtomicLongMap<Integer> map) {
    int last = 0;
    for (int shards = 2; shards <= MAX_SHARDS; shards++) {
      int chosen = Hashing.consistentHash(h, shards);
      if (chosen != last) {
        map.incrementAndGet(shards);
        last = chosen;
      }
    }
  }

  private static final int ITERS = 10000;
  private static final int MAX_SHARDS = 500;

  @J2ktIncompatible
  public void testConsistentHash_outOfRange() {
    assertThrows(IllegalArgumentException.class, () -> Hashing.consistentHash(5L, 0));
  }

  @J2ktIncompatible
  public void testConsistentHash_ofHashCode() {
    checkSameResult(HashCode.fromLong(1), 1);
    checkSameResult(HashCode.fromLong(0x9999999999999999L), 0x9999999999999999L);
    checkSameResult(HashCode.fromInt(0x99999999), 0x0000000099999999L);
  }

  @J2ktIncompatible
  public void checkSameResult(HashCode hashCode, long equivLong) {
    assertEquals(Hashing.consistentHash(equivLong, 5555), Hashing.consistentHash(hashCode, 5555));
  }

  /**
   * Check a few "golden" values to see that implementations across languages are equivalent.
   *
   */
  @J2ktIncompatible
  public void testConsistentHash_linearCongruentialGeneratorCompatibility() {
    int[] golden100 = {
      0, 55, 62, 8, 45, 59, 86, 97, 82, 59,
      73, 37, 17, 56, 86, 21, 90, 37, 38, 83
    };
    for (int i = 0; i < golden100.length; i++) {
      assertEquals(golden100[i], Hashing.consistentHash(i, 100));
    }
    assertEquals(6, Hashing.consistentHash(10863919174838991L, 11));
    assertEquals(3, Hashing.consistentHash(2016238256797177309L, 11));
    assertEquals(5, Hashing.consistentHash(1673758223894951030L, 11));
    assertEquals(80343, Hashing.consistentHash(2, 100001));
    assertEquals(22152, Hashing.consistentHash(2201, 100001));
    assertEquals(15018, Hashing.consistentHash(2202, 100001));
  }

  private static final double MAX_PERCENT_SPREAD = 0.5;
  private static final long RANDOM_SEED = 177L;

  @J2ktIncompatible
  public void testCombineOrdered_empty() {
    assertThrows(
        IllegalArgumentException.class, () -> Hashing.combineOrdered(Collections.emptySet()));
  }

  @J2ktIncompatible
  public void testCombineOrdered_differentBitLengths() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          HashCode unused =
              Hashing.combineOrdered(
                  ImmutableList.of(HashCode.fromInt(32), HashCode.fromLong(32L)));
        });
  }

  @J2ktIncompatible
  public void testCombineOrdered() {
    HashCode hash31 = HashCode.fromInt(31);
    HashCode hash32 = HashCode.fromInt(32);
    assertEquals(hash32, Hashing.combineOrdered(ImmutableList.of(hash32)));
    assertEquals(
        HashCode.fromBytes(new byte[] {(byte) 0x80, 0, 0, 0}),
        Hashing.combineOrdered(ImmutableList.of(hash32, hash32)));
    assertEquals(
        HashCode.fromBytes(new byte[] {(byte) 0xa0, 0, 0, 0}),
        Hashing.combineOrdered(ImmutableList.of(hash32, hash32, hash32)));
    assertFalse(
        Hashing.combineOrdered(ImmutableList.of(hash31, hash32))
            .equals(Hashing.combineOrdered(ImmutableList.of(hash32, hash31))));
  }

  @J2ktIncompatible
  public void testCombineOrdered_randomHashCodes() {
    Random random = new Random(7);
    List<HashCode> hashCodes = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      hashCodes.add(HashCode.fromLong(random.nextLong()));
    }
    HashCode hashCode1 = Hashing.combineOrdered(hashCodes);
    shuffle(hashCodes, random);
    HashCode hashCode2 = Hashing.combineOrdered(hashCodes);

    assertFalse(hashCode1.equals(hashCode2));
  }

  @J2ktIncompatible
  public void testCombineUnordered_empty() {
    assertThrows(
        IllegalArgumentException.class, () -> Hashing.combineUnordered(Collections.emptySet()));
  }

  @J2ktIncompatible
  public void testCombineUnordered_differentBitLengths() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          HashCode unused =
              Hashing.combineUnordered(
                  ImmutableList.of(HashCode.fromInt(32), HashCode.fromLong(32L)));
        });
  }

  @J2ktIncompatible
  public void testCombineUnordered() {
    HashCode hash31 = HashCode.fromInt(31);
    HashCode hash32 = HashCode.fromInt(32);
    assertEquals(hash32, Hashing.combineUnordered(ImmutableList.of(hash32)));
    assertEquals(HashCode.fromInt(64), Hashing.combineUnordered(ImmutableList.of(hash32, hash32)));
    assertEquals(
        HashCode.fromInt(96), Hashing.combineUnordered(ImmutableList.of(hash32, hash32, hash32)));
    assertEquals(
        Hashing.combineUnordered(ImmutableList.of(hash31, hash32)),
        Hashing.combineUnordered(ImmutableList.of(hash32, hash31)));
  }

  @J2ktIncompatible
  public void testCombineUnordered_randomHashCodes() {
    Random random = new Random(RANDOM_SEED);
    List<HashCode> hashCodes = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      hashCodes.add(HashCode.fromLong(random.nextLong()));
    }
    HashCode hashCode1 = Hashing.combineUnordered(hashCodes);
    shuffle(hashCodes);
    HashCode hashCode2 = Hashing.combineUnordered(hashCodes);

    assertEquals(hashCode1, hashCode2);
  }

  // This isn't specified by contract, but it'll still be nice to know if this behavior changes.
  @J2ktIncompatible
  public void testConcatenating_equals() {
    new EqualsTester()
        .addEqualityGroup(Hashing.concatenating(asList(Hashing.md5())))
        .addEqualityGroup(Hashing.concatenating(asList(Hashing.murmur3_32())))
        .addEqualityGroup(
            Hashing.concatenating(Hashing.md5(), Hashing.md5()),
            Hashing.concatenating(asList(Hashing.md5(), Hashing.md5())))
        .addEqualityGroup(
            Hashing.concatenating(Hashing.murmur3_32(), Hashing.md5()),
            Hashing.concatenating(asList(Hashing.murmur3_32(), Hashing.md5())))
        .addEqualityGroup(
            Hashing.concatenating(Hashing.md5(), Hashing.murmur3_32()),
            Hashing.concatenating(asList(Hashing.md5(), Hashing.murmur3_32())))
        .testEquals();
  }

  @J2ktIncompatible
  public void testConcatenatingIterable_bits() {
    assertEquals(
        Hashing.md5().bits() + Hashing.md5().bits(),
        Hashing.concatenating(asList(Hashing.md5(), Hashing.md5())).bits());
    assertEquals(
        Hashing.md5().bits() + Hashing.murmur3_32().bits(),
        Hashing.concatenating(asList(Hashing.md5(), Hashing.murmur3_32())).bits());
    assertEquals(
        Hashing.md5().bits() + Hashing.murmur3_32().bits() + Hashing.murmur3_128().bits(),
        Hashing.concatenating(asList(Hashing.md5(), Hashing.murmur3_32(), Hashing.murmur3_128()))
            .bits());
  }

  @J2ktIncompatible
  public void testConcatenatingVarArgs_bits() {
    assertEquals(
        Hashing.md5().bits() + Hashing.md5().bits(),
        Hashing.concatenating(Hashing.md5(), Hashing.md5()).bits());
    assertEquals(
        Hashing.md5().bits() + Hashing.murmur3_32().bits(),
        Hashing.concatenating(Hashing.md5(), Hashing.murmur3_32()).bits());
    assertEquals(
        Hashing.md5().bits() + Hashing.murmur3_32().bits() + Hashing.murmur3_128().bits(),
        Hashing.concatenating(Hashing.md5(), Hashing.murmur3_32(), Hashing.murmur3_128()).bits());
  }

  @J2ktIncompatible
  public void testConcatenatingHashFunction_makeHash() {
    byte[] md5Hash = Hashing.md5().hashLong(42L).asBytes();
    byte[] murmur3Hash = Hashing.murmur3_32().hashLong(42L).asBytes();
    byte[] combined = new byte[md5Hash.length + murmur3Hash.length];
    ByteBuffer buffer = ByteBuffer.wrap(combined);
    buffer.put(md5Hash);
    buffer.put(murmur3Hash);
    HashCode expected = HashCode.fromBytes(combined);

    assertEquals(
        expected, Hashing.concatenating(Hashing.md5(), Hashing.murmur3_32()).hashLong(42L));
    assertEquals(
        expected, Hashing.concatenating(asList(Hashing.md5(), Hashing.murmur3_32())).hashLong(42L));
  }

  @J2ktIncompatible
  public void testHashIntReverseBytesVsHashBytesIntsToByteArray() {
    int input = 42;
    assertEquals(
        Hashing.md5().hashBytes(Ints.toByteArray(input)),
        Hashing.md5().hashInt(Integer.reverseBytes(input)));
  }

  @J2ktIncompatible
  public void testHashIntVsForLoop() {
    int input = 42;
    HashCode expected = Hashing.md5().hashInt(input);

    Hasher hasher = Hashing.md5().newHasher();
    for (int i = 0; i < 32; i += 8) {
      hasher.putByte((byte) (input >> i));
    }
    HashCode actual = hasher.hash();

    assertEquals(expected, actual);
  }

  @J2ktIncompatible
  public void testAllHashFunctionsHaveKnownHashes() throws Exception {
    for (Method method : Hashing.class.getDeclaredMethods()) {
      if (shouldHaveKnownHashes(method)) {
        HashFunction hashFunction = (HashFunction) method.invoke(Hashing.class);
        assertTrue(
            "There should be at least 3 entries in KNOWN_HASHES for " + hashFunction,
            TestPlatform.getKnownHashes().row(hashFunction).size() >= 3);
      }
    }
  }

  public void testKnownUtf8Hashing() {
    for (Cell<HashFunction, String, String> cell : TestPlatform.getKnownHashes().cellSet()) {
      HashFunction func = cell.getRowKey();
      String input = cell.getColumnKey();
      String expected = cell.getValue();
      assertWithMessage(String.format(Locale.ROOT, "Known hash for hash(%s, UTF_8) failed", input))
          .that(func.hashString(input, UTF_8).toString())
          .isEqualTo(expected);
    }
  }

  @J2ktIncompatible
  public void testNullPointers() {
    NullPointerTester tester =
        new NullPointerTester()
            .setDefault(byte[].class, "secret key".getBytes(UTF_8))
            .setDefault(HashCode.class, HashCode.fromLong(0));
    tester.testAllPublicStaticMethods(Hashing.class);
  }

  @J2ktIncompatible
  public void testSeedlessHashFunctionEquals() throws Exception {
    assertSeedlessHashFunctionEquals(Hashing.class);
  }

  @J2ktIncompatible
  public void testSeededHashFunctionEquals() throws Exception {
    assertSeededHashFunctionEquals(Hashing.class);
  }

  /**
   * Tests equality of {@link Hashing#goodFastHash} instances. This test must be separate from
   * {@link #testSeededHashFunctionEquals} because the parameter to {@code goodFastHash} is a size,
   * not a seed, and because that size is rounded up. Thus, {@code goodFastHash} instances with
   * different parameters can be equal. That fact is a problem for {@code
   * testSeededHashFunctionEquals}.
   */
  @J2ktIncompatible
  public void testGoodFastHashEquals() throws Exception {
    HashFunction hashFunction1a = Hashing.goodFastHash(1);
    HashFunction hashFunction1b = Hashing.goodFastHash(32);
    HashFunction hashFunction2a = Hashing.goodFastHash(33);
    HashFunction hashFunction2b = Hashing.goodFastHash(128);
    HashFunction hashFunction3a = Hashing.goodFastHash(129);
    HashFunction hashFunction3b = Hashing.goodFastHash(256);
    HashFunction hashFunction4a = Hashing.goodFastHash(257);
    HashFunction hashFunction4b = Hashing.goodFastHash(384);

    new EqualsTester()
        .addEqualityGroup(hashFunction1a, hashFunction1b)
        .addEqualityGroup(hashFunction2a, hashFunction2b)
        .addEqualityGroup(hashFunction3a, hashFunction3b)
        .addEqualityGroup(hashFunction4a, hashFunction4b)
        .testEquals();

    assertThat(hashFunction1b.toString()).isEqualTo(hashFunction1a.toString());
    assertThat(hashFunction2b.toString()).isEqualTo(hashFunction2a.toString());
    assertThat(hashFunction3b.toString()).isEqualTo(hashFunction3a.toString());
    assertThat(hashFunction4b.toString()).isEqualTo(hashFunction4a.toString());
  }

  @J2ktIncompatible
  static void assertSeedlessHashFunctionEquals(Class<?> clazz) throws Exception {
    for (Method method : clazz.getDeclaredMethods()) {
      if (shouldHaveKnownHashes(method)) {
        HashFunction hashFunction1a = (HashFunction) method.invoke(clazz);
        HashFunction hashFunction1b = (HashFunction) method.invoke(clazz);

        new EqualsTester().addEqualityGroup(hashFunction1a, hashFunction1b).testEquals();

        // Make sure we're returning not only equal instances, but constants.
        assertThat(hashFunction1a).isSameInstanceAs(hashFunction1b);

        assertThat(hashFunction1a.toString()).isEqualTo(hashFunction1b.toString());
      }
    }
  }

  @J2ktIncompatible
  private static boolean shouldHaveKnownHashes(Method method) {
    // The following legacy hashing function methods have been covered by unit testing already.
    ImmutableSet<String> legacyHashingMethodNames =
        ImmutableSet.of("murmur2_64", "fprint96", "highwayFingerprint64", "highwayFingerprint128");
    return method.getReturnType().equals(HashFunction.class) // must return HashFunction
        && Modifier.isPublic(method.getModifiers()) // only the public methods
        && method.getParameterTypes().length == 0 // only the seedless hash functions
        && !legacyHashingMethodNames.contains(method.getName());
  }

  @J2ktIncompatible
  static void assertSeededHashFunctionEquals(Class<?> clazz) throws Exception {
    Random random = new Random(RANDOM_SEED);
    for (Method method : clazz.getDeclaredMethods()) {
      if (method.getReturnType().equals(HashFunction.class) // must return HashFunction
          && Modifier.isPublic(method.getModifiers()) // only the public methods
          && method.getParameterTypes().length != 0 // only the seeded hash functions
          && !method.getName().equals("concatenating") // don't test Hashing.concatenating()
          && !method.getName().equals("goodFastHash") // tested in testGoodFastHashEquals
          && !method.getName().startsWith("hmac")) { // skip hmac functions
        Object[] params1 = new Object[method.getParameterTypes().length];
        Object[] params2 = new Object[method.getParameterTypes().length];
        for (int i = 0; i < params1.length; i++) {
          if (method.getParameterTypes()[i] == int.class) {
            params1[i] = random.nextInt();
            params2[i] = random.nextInt();
          } else if (method.getParameterTypes()[i] == long.class) {
            params1[i] = random.nextLong();
            params2[i] = random.nextLong();
          } else {
            fail("Unable to create a random parameter for " + method.getParameterTypes()[i]);
          }
        }
        HashFunction hashFunction1a = (HashFunction) method.invoke(clazz, params1);
        HashFunction hashFunction1b = (HashFunction) method.invoke(clazz, params1);
        HashFunction hashFunction2 = (HashFunction) method.invoke(clazz, params2);

        new EqualsTester()
            .addEqualityGroup(hashFunction1a, hashFunction1b)
            .addEqualityGroup(hashFunction2)
            .testEquals();

        assertThat(hashFunction1b.toString()).isEqualTo(hashFunction1a.toString());
      }
    }
  }

  // Parity tests taken from //util/hash/hash_unittest.cc
}

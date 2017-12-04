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

import static com.google.common.base.Charsets.UTF_8;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table.Cell;
import com.google.common.primitives.Ints;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.util.concurrent.AtomicLongMap;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import junit.framework.TestCase;

/**
 * Unit tests for {@link Hashing}.
 *
 * <p>TODO(b/33919189): Migrate repeated testing methods to {@link #HashTestUtils} and tweak unit
 * tests to reference them from there.
 *
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 */
public class HashingTest extends TestCase {
  public void testMd5() {
    HashTestUtils.checkAvalanche(Hashing.md5(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.md5());
    HashTestUtils.checkNoFunnels(Hashing.md5());
    HashTestUtils.assertInvariants(Hashing.md5());
    assertEquals("Hashing.md5()", Hashing.md5().toString());
  }

  public void testSha1() {
    HashTestUtils.checkAvalanche(Hashing.sha1(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sha1());
    HashTestUtils.checkNoFunnels(Hashing.sha1());
    HashTestUtils.assertInvariants(Hashing.sha1());
    assertEquals("Hashing.sha1()", Hashing.sha1().toString());
  }

  public void testSha256() {
    HashTestUtils.checkAvalanche(Hashing.sha256(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sha256());
    HashTestUtils.checkNoFunnels(Hashing.sha256());
    HashTestUtils.assertInvariants(Hashing.sha256());
    assertEquals("Hashing.sha256()", Hashing.sha256().toString());
  }

  public void testSha384() {
    HashTestUtils.checkAvalanche(Hashing.sha384(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sha384());
    HashTestUtils.checkNoFunnels(Hashing.sha384());
    HashTestUtils.assertInvariants(Hashing.sha384());
    assertEquals("Hashing.sha384()", Hashing.sha384().toString());
  }

  public void testSha512() {
    HashTestUtils.checkAvalanche(Hashing.sha512(), 100, 0.4);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sha512());
    HashTestUtils.checkNoFunnels(Hashing.sha512());
    HashTestUtils.assertInvariants(Hashing.sha512());
    assertEquals("Hashing.sha512()", Hashing.sha512().toString());
  }

  public void testCrc32() {
    HashTestUtils.assertInvariants(Hashing.crc32());
    assertEquals("Hashing.crc32()", Hashing.crc32().toString());
  }

  public void testAdler32() {
    HashTestUtils.assertInvariants(Hashing.adler32());
    assertEquals("Hashing.adler32()", Hashing.adler32().toString());
  }

  public void testMurmur3_128() {
    HashTestUtils.check2BitAvalanche(Hashing.murmur3_128(), 250, 0.20);
    HashTestUtils.checkAvalanche(Hashing.murmur3_128(), 250, 0.17);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.murmur3_128());
    HashTestUtils.checkNoFunnels(Hashing.murmur3_128());
    HashTestUtils.assertInvariants(Hashing.murmur3_128());
    assertEquals("Hashing.murmur3_128(0)", Hashing.murmur3_128().toString());
  }

  public void testMurmur3_32() {
    HashTestUtils.check2BitAvalanche(Hashing.murmur3_32(), 250, 0.20);
    HashTestUtils.checkAvalanche(Hashing.murmur3_32(), 250, 0.17);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.murmur3_32());
    HashTestUtils.checkNoFunnels(Hashing.murmur3_32());
    HashTestUtils.assertInvariants(Hashing.murmur3_32());
    assertEquals("Hashing.murmur3_32(0)", Hashing.murmur3_32().toString());
  }

  public void testSipHash24() {
    HashTestUtils.check2BitAvalanche(Hashing.sipHash24(), 250, 0.14);
    HashTestUtils.checkAvalanche(Hashing.sipHash24(), 250, 0.10);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.sipHash24());
    HashTestUtils.checkNoFunnels(Hashing.sipHash24());
    HashTestUtils.assertInvariants(Hashing.sipHash24());
    assertEquals(
        "Hashing.sipHash24(506097522914230528, 1084818905618843912)",
        Hashing.sipHash24().toString());
  }

  @AndroidIncompatible // slow TODO(cpovirk): Maybe just reduce iterations under Android.
  public void testGoodFastHash() {
    for (int i = 1; i < 200; i += 17) {
      HashFunction hasher = Hashing.goodFastHash(i);
      assertTrue(hasher.bits() >= i);
      HashTestUtils.assertInvariants(hasher);
    }
  }

  // goodFastHash(32) uses Murmur3_32. Use the same epsilon bounds.
  public void testGoodFastHash32() {
    HashTestUtils.check2BitAvalanche(Hashing.goodFastHash(32), 250, 0.20);
    HashTestUtils.checkAvalanche(Hashing.goodFastHash(32), 250, 0.17);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.goodFastHash(32));
    HashTestUtils.checkNoFunnels(Hashing.goodFastHash(32));
    HashTestUtils.assertInvariants(Hashing.goodFastHash(32));
  }

  // goodFastHash(128) uses Murmur3_128. Use the same epsilon bounds.
  public void testGoodFastHash128() {
    HashTestUtils.check2BitAvalanche(Hashing.goodFastHash(128), 250, 0.20);
    HashTestUtils.checkAvalanche(Hashing.goodFastHash(128), 250, 0.17);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.goodFastHash(128));
    HashTestUtils.checkNoFunnels(Hashing.goodFastHash(128));
    HashTestUtils.assertInvariants(Hashing.goodFastHash(128));
  }

  // goodFastHash(256) uses Murmur3_128. Use the same epsilon bounds.
  public void testGoodFastHash256() {
    HashTestUtils.check2BitAvalanche(Hashing.goodFastHash(256), 250, 0.20);
    HashTestUtils.checkAvalanche(Hashing.goodFastHash(256), 250, 0.17);
    HashTestUtils.checkNo2BitCharacteristics(Hashing.goodFastHash(256));
    HashTestUtils.checkNoFunnels(Hashing.goodFastHash(256));
    HashTestUtils.assertInvariants(Hashing.goodFastHash(256));
  }

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

  public void testConsistentHash_probabilities() {
    AtomicLongMap<Integer> map = AtomicLongMap.create();
    Random r = new Random(9);
    for (int i = 0; i < ITERS; i++) {
      countRemaps(r.nextLong(), map);
    }
    for (int shard = 2; shard <= MAX_SHARDS; shard++) {
      // Rough: don't exceed 1.2x the expected number of remaps by more than 20
      assertTrue(map.get(shard) <= 1.2 * ITERS / shard + 20);
    }
  }

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

  public void testConsistentHash_outOfRange() {
    try {
      Hashing.consistentHash(5L, 0);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConsistentHash_ofHashCode() {
    checkSameResult(HashCode.fromLong(1), 1);
    checkSameResult(HashCode.fromLong(0x9999999999999999L), 0x9999999999999999L);
    checkSameResult(HashCode.fromInt(0x99999999), 0x0000000099999999L);
  }

  public void checkSameResult(HashCode hashCode, long equivLong) {
    assertEquals(Hashing.consistentHash(equivLong, 5555), Hashing.consistentHash(hashCode, 5555));
  }

  /**
   * Check a few "golden" values to see that implementations across languages are equivalent.
   *
   */
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

  public void testCombineOrdered_empty() {
    try {
      Hashing.combineOrdered(Collections.<HashCode>emptySet());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCombineOrdered_differentBitLengths() {
    try {
      HashCode unused =
          Hashing.combineOrdered(ImmutableList.of(HashCode.fromInt(32), HashCode.fromLong(32L)));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

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

  public void testCombineOrdered_randomHashCodes() {
    Random random = new Random(7);
    List<HashCode> hashCodes = Lists.newArrayList();
    for (int i = 0; i < 10; i++) {
      hashCodes.add(HashCode.fromLong(random.nextLong()));
    }
    HashCode hashCode1 = Hashing.combineOrdered(hashCodes);
    Collections.shuffle(hashCodes, random);
    HashCode hashCode2 = Hashing.combineOrdered(hashCodes);

    assertFalse(hashCode1.equals(hashCode2));
  }

  public void testCombineUnordered_empty() {
    try {
      Hashing.combineUnordered(Collections.<HashCode>emptySet());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCombineUnordered_differentBitLengths() {
    try {
      HashCode unused =
          Hashing.combineUnordered(ImmutableList.of(HashCode.fromInt(32), HashCode.fromLong(32L)));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

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

  public void testCombineUnordered_randomHashCodes() {
    Random random = new Random(RANDOM_SEED);
    List<HashCode> hashCodes = Lists.newArrayList();
    for (int i = 0; i < 10; i++) {
      hashCodes.add(HashCode.fromLong(random.nextLong()));
    }
    HashCode hashCode1 = Hashing.combineUnordered(hashCodes);
    Collections.shuffle(hashCodes);
    HashCode hashCode2 = Hashing.combineUnordered(hashCodes);

    assertEquals(hashCode1, hashCode2);
  }

  // This isn't specified by contract, but it'll still be nice to know if this behavior changes.
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

  public void testHashIntReverseBytesVsHashBytesIntsToByteArray() {
    int input = 42;
    assertEquals(
        Hashing.md5().hashBytes(Ints.toByteArray(input)),
        Hashing.md5().hashInt(Integer.reverseBytes(input)));
  }

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

  private static final String EMPTY_STRING = "";
  private static final String TQBFJOTLD = "The quick brown fox jumps over the lazy dog";
  private static final String TQBFJOTLDP = "The quick brown fox jumps over the lazy dog.";

  private static final ImmutableTable<HashFunction, String, String> KNOWN_HASHES =
      ImmutableTable.<HashFunction, String, String>builder()
          .put(Hashing.adler32(), EMPTY_STRING, "01000000")
          .put(Hashing.adler32(), TQBFJOTLD, "da0fdc5b")
          .put(Hashing.adler32(), TQBFJOTLDP, "0810e46b")
          .put(Hashing.md5(), EMPTY_STRING, "d41d8cd98f00b204e9800998ecf8427e")
          .put(Hashing.md5(), TQBFJOTLD, "9e107d9d372bb6826bd81d3542a419d6")
          .put(Hashing.md5(), TQBFJOTLDP, "e4d909c290d0fb1ca068ffaddf22cbd0")
          .put(Hashing.murmur3_128(), EMPTY_STRING, "00000000000000000000000000000000")
          .put(Hashing.murmur3_128(), TQBFJOTLD, "6c1b07bc7bbc4be347939ac4a93c437a")
          .put(Hashing.murmur3_128(), TQBFJOTLDP, "c902e99e1f4899cde7b68789a3a15d69")
          .put(Hashing.murmur3_32(), EMPTY_STRING, "00000000")
          .put(Hashing.murmur3_32(), TQBFJOTLD, "23f74f2e")
          .put(Hashing.murmur3_32(), TQBFJOTLDP, "fc8bc4d5")
          .put(Hashing.sha1(), EMPTY_STRING, "da39a3ee5e6b4b0d3255bfef95601890afd80709")
          .put(Hashing.sha1(), TQBFJOTLD, "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12")
          .put(Hashing.sha1(), TQBFJOTLDP, "408d94384216f890ff7a0c3528e8bed1e0b01621")
          .put(
              Hashing.sha256(),
              EMPTY_STRING,
              "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
          .put(
              Hashing.sha256(),
              TQBFJOTLD,
              "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592")
          .put(
              Hashing.sha256(),
              TQBFJOTLDP,
              "ef537f25c895bfa782526529a9b63d97aa631564d5d789c2b765448c8635fb6c")
          .put(
              Hashing.sha384(),
              EMPTY_STRING,
              "38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da2"
                  + "74edebfe76f65fbd51ad2f14898b95b")
          .put(
              Hashing.sha384(),
              TQBFJOTLD,
              "ca737f1014a48f4c0b6dd43cb177b0afd9e5169367544c494011e3317dbf9a509"
                  + "cb1e5dc1e85a941bbee3d7f2afbc9b1")
          .put(
              Hashing.sha384(),
              TQBFJOTLDP,
              "ed892481d8272ca6df370bf706e4d7bc1b5739fa2177aae6c50e946678718fc67"
                  + "a7af2819a021c2fc34e91bdb63409d7")
          .put(
              Hashing.sha512(),
              EMPTY_STRING,
              "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce"
                  + "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e")
          .put(
              Hashing.sha512(),
              TQBFJOTLD,
              "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb64"
                  + "2e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6")
          .put(
              Hashing.sha512(),
              TQBFJOTLDP,
              "91ea1245f20d46ae9a037a989f54f1f790f0a47607eeb8a14d12890cea77a1bb"
                  + "c6c7ed9cf205e67b7f2b8fd4c7dfd3a7a8617e45f3c463d481c7e586c39ac1ed")
          .put(Hashing.crc32(), EMPTY_STRING, "00000000")
          .put(Hashing.crc32(), TQBFJOTLD, "39a34f41")
          .put(Hashing.crc32(), TQBFJOTLDP, "e9259051")
          .put(Hashing.sipHash24(), EMPTY_STRING, "310e0edd47db6f72")
          .put(Hashing.sipHash24(), TQBFJOTLD, "e46f1fdc05612752")
          .put(Hashing.sipHash24(), TQBFJOTLDP, "9b602581fce4d4f8")
          .put(Hashing.crc32c(), EMPTY_STRING, "00000000")
          .put(Hashing.crc32c(), TQBFJOTLD, "04046222")
          .put(Hashing.crc32c(), TQBFJOTLDP, "b3970019")
          .put(Hashing.farmHashFingerprint64(), EMPTY_STRING, "4f40902f3b6ae19a")
          .put(Hashing.farmHashFingerprint64(), TQBFJOTLD, "34511b3bf383beab")
          .put(Hashing.farmHashFingerprint64(), TQBFJOTLDP, "737d7e5f8660653e")
          .build();

  public void testAllHashFunctionsHaveKnownHashes() throws Exception {
    // The following legacy hashing function methods have been covered by unit testing already.
    List<String> legacyHashingMethodNames = ImmutableList.of("murmur2_64", "fprint96");
    for (Method method : Hashing.class.getDeclaredMethods()) {
      if (method.getReturnType().equals(HashFunction.class) // must return HashFunction
          && Modifier.isPublic(method.getModifiers()) // only the public methods
          && method.getParameterTypes().length == 0 // only the seed-less grapes^W hash functions
          && !legacyHashingMethodNames.contains(method.getName())) {
        HashFunction hashFunction = (HashFunction) method.invoke(Hashing.class);
        assertTrue(
            "There should be at least 3 entries in KNOWN_HASHES for " + hashFunction,
            KNOWN_HASHES.row(hashFunction).size() >= 3);
      }
    }
  }

  public void testKnownUtf8Hashing() {
    for (Cell<HashFunction, String, String> cell : KNOWN_HASHES.cellSet()) {
      HashFunction func = cell.getRowKey();
      String input = cell.getColumnKey();
      String expected = cell.getValue();
      assertEquals(
          String.format(Locale.ROOT, "Known hash for hash(%s, UTF_8) failed", input),
          expected,
          func.hashString(input, UTF_8).toString());
    }
  }

  public void testNullPointers() {
    NullPointerTester tester =
        new NullPointerTester()
            .setDefault(byte[].class, "secret key".getBytes(UTF_8))
            .setDefault(HashCode.class, HashCode.fromLong(0));
    tester.testAllPublicStaticMethods(Hashing.class);
  }

  public void testSeedlessHashFunctionEquals() throws Exception {
    assertSeedlessHashFunctionEquals(Hashing.class);
  }

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

    assertEquals(hashFunction1a.toString(), hashFunction1b.toString());
    assertEquals(hashFunction2a.toString(), hashFunction2b.toString());
    assertEquals(hashFunction3a.toString(), hashFunction3b.toString());
    assertEquals(hashFunction4a.toString(), hashFunction4b.toString());
  }

  static void assertSeedlessHashFunctionEquals(Class<?> clazz) throws Exception {
    for (Method method : clazz.getDeclaredMethods()) {
      if (method.getReturnType().equals(HashFunction.class) // must return HashFunction
          && Modifier.isPublic(method.getModifiers()) // only the public methods
          && method.getParameterTypes().length == 0) { // only the seed-less hash functions
        HashFunction hashFunction1a = (HashFunction) method.invoke(clazz);
        HashFunction hashFunction1b = (HashFunction) method.invoke(clazz);

        new EqualsTester().addEqualityGroup(hashFunction1a, hashFunction1b).testEquals();

        // Make sure we're returning not only equal instances, but constants.
        assertSame(hashFunction1a, hashFunction1b);

        assertEquals(hashFunction1a.toString(), hashFunction1b.toString());
      }
    }
  }

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

        assertEquals(hashFunction1a.toString(), hashFunction1b.toString());
      }
    }
  }

  // Parity tests taken from //util/hash/hash_unittest.cc
}

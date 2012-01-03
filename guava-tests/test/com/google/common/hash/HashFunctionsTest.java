// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.hash;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.hash.HashTestUtils.RandomHasherAction;

import junit.framework.TestCase;

import java.util.Random;
import java.util.Set;

/**
 * Tests for HashFunctions.
 *
 * @author andreou@google.com (Dimitris Andreou)
 */
public class HashFunctionsTest extends TestCase {
  public void testMd5() {
    assertInvariants(Hashing.md5());
  }

  public void testSha1() {
    assertInvariants(Hashing.sha1());
  }

  public void testSha256() {
    assertInvariants(Hashing.sha256());
  }

  public void testSha512() {
    assertInvariants(Hashing.sha512());
  }

  public void testMurmur3_138() {
    assertInvariants(Hashing.murmur3_128());
  }

  public void testMurmur3_32() {
    assertInvariants(Hashing.murmur3_32());
  }

  public void testGoodFastHash() {
    for (int i = 1; i < 500; i++) {
      HashFunction hasher = Hashing.goodFastHash(i);
      assertTrue(hasher.bits() >= i);
      assertInvariants(hasher);
    }
  }

  /**
   * Checks that a Hasher returns the same HashCode when given the same input, and also
   * that the collision rate looks sane.
   */
  private static void assertInvariants(HashFunction hashFunction) {
    int objects = 100;
    Set<HashCode> hashcodes = Sets.newHashSetWithExpectedSize(objects);
    for (int i = 0; i < objects; i++) {
      Object o = new Object();
      HashCode hashcode1 = hashFunction.newHasher().putObject(o, HashTestUtils.BAD_FUNNEL).hash();
      HashCode hashcode2 = hashFunction.newHasher().putObject(o, HashTestUtils.BAD_FUNNEL).hash();
      assertEquals(hashcode1, hashcode2); // idempotent
      assertEquals(hashFunction.bits(), hashcode1.bits());
      assertEquals(hashFunction.bits(), hashcode1.asBytes().length * 8);
      hashcodes.add(hashcode1);
    }
    assertTrue(hashcodes.size() > objects * 0.95); // quite relaxed test

    assertHashBytesThrowsCorrectExceptions(hashFunction);
    assertIndependentHashers(hashFunction);
  }

  private static void assertHashBytesThrowsCorrectExceptions(HashFunction hashFunction) {
    hashFunction.hashBytes(new byte[64], 0, 0);

    try {
      hashFunction.hashBytes(new byte[128], -1, 128);
      fail();
    } catch (IndexOutOfBoundsException ok) {}
    try {
      hashFunction.hashBytes(new byte[128], 64, 256 /* too long len */);
      fail();
    } catch (IndexOutOfBoundsException ok) {}
    try {
      hashFunction.hashBytes(new byte[64], 0, -1);
      fail();
    } catch (IndexOutOfBoundsException ok) {}
  }

  private static void assertIndependentHashers(HashFunction hashFunction) {
    int numActions = 100;
    // hashcodes from non-overlapping hash computations
    HashCode expected1 = randomHash(hashFunction, new Random(1L), numActions);
    HashCode expected2 = randomHash(hashFunction, new Random(2L), numActions);

    // equivalent, but overlapping, computations (should produce the same results as above)
    Random random1 = new Random(1L);
    Random random2 = new Random(2L);
    Hasher hasher1 = hashFunction.newHasher();
    Hasher hasher2 = hashFunction.newHasher();
    for (int i = 0; i < numActions; i++) {
      RandomHasherAction.pickAtRandom(random1).performAction(random1, ImmutableSet.of(hasher1));
      RandomHasherAction.pickAtRandom(random2).performAction(random2, ImmutableSet.of(hasher2));
    }

    assertEquals(expected1, hasher1.hash());
    assertEquals(expected2, hasher2.hash());
  }

  private static HashCode randomHash(HashFunction hashFunction, Random random, int numActions) {
    Hasher hasher = hashFunction.newHasher();
    for (int i = 0; i < numActions; i++) {
      RandomHasherAction.pickAtRandom(random).performAction(random, ImmutableSet.of(hasher));
    }
    return hasher.hash();
  }
}

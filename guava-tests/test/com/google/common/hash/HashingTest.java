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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicLongMap;

import junit.framework.TestCase;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Unit tests for functions of {@code Hashing} that don't have their own tests.
 */
public class HashingTest extends TestCase {
  public void testPadToLong() {
    assertEquals(0x1111111111111111L, Hashing.padToLong(HashCodes.fromLong(0x1111111111111111L)));
    assertEquals(0x9999999999999999L, Hashing.padToLong(HashCodes.fromLong(0x9999999999999999L)));
    assertEquals(0x0000000011111111L, Hashing.padToLong(HashCodes.fromInt(0x11111111)));
    assertEquals(0x0000000099999999L, Hashing.padToLong(HashCodes.fromInt(0x99999999)));
  }

  public void testConsistentHash_correctness() {
    long[] interestingValues = { -1, 0, 1, 2, Long.MAX_VALUE, Long.MIN_VALUE };
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
    checkSameResult(HashCodes.fromLong(1), 1);
    checkSameResult(HashCodes.fromLong(0x9999999999999999L), 0x9999999999999999L);
    checkSameResult(HashCodes.fromInt(0x99999999), 0x0000000099999999L);
  }

  public void checkSameResult(HashCode hashCode, long equivLong) {
    assertEquals(Hashing.consistentHash(equivLong, 5555), Hashing.consistentHash(hashCode, 5555));
  }

  public void testCombineOrdered_null() {
    try {
      Hashing.combineOrdered(null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testCombineOrdered_empty() {
    try {
      Hashing.combineOrdered(Collections.<HashCode>emptySet());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCombineOrdered_differentBitLengths() {
    try {
      Hashing.combineOrdered(ImmutableList.of(HashCodes.fromInt(32), HashCodes.fromLong(32L)));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCombineOrdered() {
    HashCode hash31 = HashCodes.fromInt(31);
    HashCode hash32 = HashCodes.fromInt(32);
    assertEquals(hash32, Hashing.combineOrdered(ImmutableList.of(hash32)));
    assertEquals(HashCodes.fromBytes(new byte[] { (byte) 0x80, 0, 0, 0 }),
        Hashing.combineOrdered(ImmutableList.of(hash32, hash32)));
    assertEquals(HashCodes.fromBytes(new byte[] { (byte) 0xa0, 0, 0, 0 }),
        Hashing.combineOrdered(ImmutableList.of(hash32, hash32, hash32)));
    assertFalse(
        Hashing.combineOrdered(ImmutableList.of(hash31, hash32)).equals(
        Hashing.combineOrdered(ImmutableList.of(hash32, hash31))));
  }

  public void testCombineOrdered_randomHashCodes() {
    Random random = new Random(7);
    List<HashCode> hashCodes = Lists.newArrayList();
    for (int i = 0; i < 10; i++) {
      hashCodes.add(HashCodes.fromLong(random.nextLong()));
    }
    HashCode hashCode1 = Hashing.combineOrdered(hashCodes);
    Collections.shuffle(hashCodes, random);
    HashCode hashCode2 = Hashing.combineOrdered(hashCodes);

    assertFalse(hashCode1.equals(hashCode2));
  }

  public void testCombineUnordered_null() {
    try {
      Hashing.combineUnordered(null);
      fail();
    } catch (NullPointerException expected) {
    }
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
      Hashing.combineUnordered(ImmutableList.of(HashCodes.fromInt(32), HashCodes.fromLong(32L)));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCombineUnordered() {
    HashCode hash31 = HashCodes.fromInt(31);
    HashCode hash32 = HashCodes.fromInt(32);
    assertEquals(hash32, Hashing.combineUnordered(ImmutableList.of(hash32)));
    assertEquals(HashCodes.fromInt(64), Hashing.combineUnordered(ImmutableList.of(hash32, hash32)));
    assertEquals(HashCodes.fromInt(96),
        Hashing.combineUnordered(ImmutableList.of(hash32, hash32, hash32)));
    assertEquals(
        Hashing.combineUnordered(ImmutableList.of(hash31, hash32)),
        Hashing.combineUnordered(ImmutableList.of(hash32, hash31)));
  }

  public void testCombineUnordered_randomHashCodes() {
    Random random = new Random();
    List<HashCode> hashCodes = Lists.newArrayList();
    for (int i = 0; i < 10; i++) {
      hashCodes.add(HashCodes.fromLong(random.nextLong()));
    }
    HashCode hashCode1 = Hashing.combineUnordered(hashCodes);
    Collections.shuffle(hashCodes);
    HashCode hashCode2 = Hashing.combineUnordered(hashCodes);

    assertEquals(hashCode1, hashCode2);
  }
}

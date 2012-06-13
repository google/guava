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

import static com.google.common.hash.Hashing.murmur3_32;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashTestUtils.HashFn;

import junit.framework.TestCase;

/**
 * Tests for {@link Murmur3_32HashFunction}.
 */
public class Murmur3Hash32Test extends TestCase {
  public void testKnownIntegerInputs() {
    assertEquals(593689054, murmur3_32().hashInt(0).asInt());
    assertEquals(-189366624, murmur3_32().hashInt(-42).asInt());
    assertEquals(-1134849565, murmur3_32().hashInt(42).asInt());
    assertEquals(-1718298732, murmur3_32().hashInt(Integer.MIN_VALUE).asInt());
    assertEquals(-1653689534, murmur3_32().hashInt(Integer.MAX_VALUE).asInt());
  }

  public void testKnownLongInputs() {
    assertEquals(1669671676, murmur3_32().hashLong(0L).asInt());
    assertEquals(-846261623, murmur3_32().hashLong(-42L).asInt());
    assertEquals(1871679806, murmur3_32().hashLong(42L).asInt());
    assertEquals(1366273829, murmur3_32().hashLong(Long.MIN_VALUE).asInt());
    assertEquals(-2106506049, murmur3_32().hashLong(Long.MAX_VALUE).asInt());
  }

  public void testKnownStringInputs() {
    assertEquals(0, murmur3_32().hashString("").asInt());
    assertEquals(679745764, murmur3_32().hashString("k").asInt());
    assertEquals(-675079799, murmur3_32().hashString("hello").asInt());
    assertEquals(1935035788, murmur3_32().hashString("http://www.google.com/").asInt());
    assertEquals(-528633700,
        murmur3_32().hashString("The quick brown fox jumps over the lazy dog").asInt());
  }

  public void testParanoid() {
    HashFn hf = new HashFn() {
      @Override public byte[] hash(byte[] input, int seed) {
        Hasher hasher = murmur3_32(seed).newHasher();
        Funnels.byteArrayFunnel().funnel(input, hasher);
        return hasher.hash().asBytes();
      }
    };
    // Murmur3A, MurmurHash3 for x86, 32-bit (MurmurHash3_x86_32)
    // http://code.google.com/p/smhasher/source/browse/trunk/main.cpp
    HashTestUtils.verifyHashFunction(hf, 32, 0xB0F57EE3);
  }

  public void testInvariants() {
    HashTestUtils.assertInvariants(murmur3_32());
  }
}

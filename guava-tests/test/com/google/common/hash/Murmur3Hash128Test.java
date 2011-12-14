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

import static com.google.common.hash.HashTestUtils.ascii;
import static com.google.common.hash.HashTestUtils.toBytes;
import static com.google.common.hash.Hashing.murmur3_128;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashTestUtils.HashFn;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Tests for Murmur3Hash128.
 */
public class Murmur3Hash128Test extends TestCase {
  public void testCompatibilityWithCPlusPlus() {
    assertHash(0, toBytes(LITTLE_ENDIAN, 0x629942693e10f867L, 0x92db0b82baeb5347L),
        ascii("hell"));
    assertHash(1, toBytes(LITTLE_ENDIAN, 0xa78ddff5adae8d10L, 0x128900ef20900135L),
        ascii("hello"));
    assertHash(2, toBytes(LITTLE_ENDIAN, 0x8a486b23f422e826L, 0xf962a2c58947765fL),
        ascii("hello "));
    assertHash(3, toBytes(LITTLE_ENDIAN, 0x2ea59f466f6bed8cL, 0xc610990acc428a17L),
        ascii("hello w"));
    assertHash(4, toBytes(LITTLE_ENDIAN, 0x79f6305a386c572cL, 0x46305aed3483b94eL),
        ascii("hello wo"));
    assertHash(5, toBytes(LITTLE_ENDIAN, 0xc2219d213ec1f1b5L, 0xa1d8e2e0a52785bdL),
        ascii("hello wor"));
    assertHash(0, toBytes(LITTLE_ENDIAN, 0xe34bbc7bbc071b6cL, 0x7a433ca9c49a9347L),
        ascii("The quick brown fox jumps over the lazy dog"));
    assertHash(0, toBytes(LITTLE_ENDIAN, 0x658ca970ff85269aL, 0x43fee3eaa68e5c3eL),
        ascii("The quick brown fox jumps over the lazy cog"));
  }
  
  private static void assertHash(int seed, byte[] expectedHash, byte[] input) {
    byte[] hash = murmur3_128(seed).newHasher().putBytes(input).hash().asBytes();
    assertTrue(Arrays.equals(expectedHash, hash));
  }

  public void testParanoid() {
    HashFn hf = new HashFn() {
      @Override public byte[] hash(byte[] input, int seed) {
        Hasher hasher = murmur3_128(seed).newHasher();
        Funnels.byteArrayFunnel().funnel(input, hasher);
        return hasher.hash().asBytes();
      }
    };
    // the magic number comes from:
    // http://code.google.com/p/smhasher/source/browse/trunk/main.cpp, #74
    HashTestUtils.verifyHashFunction(hf, 128, 0x6384BA69);
  }
}

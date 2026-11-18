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

import static com.google.common.hash.Funnels.byteArrayFunnel;
import static com.google.common.hash.Hashing.murmur3_128;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.J2ktIncompatible;
import com.google.common.hash.HashTestUtils.HashFn;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/** Tests for {@link Murmur3_128HashFunction}. */
@NullUnmarked
@J2ktIncompatible
public class Murmur3Hash128Test extends TestCase {
  public void testKnownValues() {
    assertHash(0, 0x629942693e10f867L, 0x92db0b82baeb5347L, "hell");
    assertHash(1, 0xa78ddff5adae8d10L, 0x128900ef20900135L, "hello");
    assertHash(2, 0x8a486b23f422e826L, 0xf962a2c58947765fL, "hello ");
    assertHash(3, 0x2ea59f466f6bed8cL, 0xc610990acc428a17L, "hello w");
    assertHash(4, 0x79f6305a386c572cL, 0x46305aed3483b94eL, "hello wo");
    assertHash(5, 0xc2219d213ec1f1b5L, 0xa1d8e2e0a52785bdL, "hello wor");
    assertHash(
        0, 0xe34bbc7bbc071b6cL, 0x7a433ca9c49a9347L, "The quick brown fox jumps over the lazy dog");
    assertHash(
        0, 0x658ca970ff85269aL, 0x43fee3eaa68e5c3eL, "The quick brown fox jumps over the lazy cog");

    // Known output from Python smhasher
    HashCode foxHash =
        murmur3_128(0).hashString("The quick brown fox jumps over the lazy dog", UTF_8);
    assertThat(foxHash.toString()).isEqualTo("6c1b07bc7bbc4be347939ac4a93c437a");
  }

  private static void assertHash(int seed, long expected1, long expected2, String stringInput) {
    HashCode expected = toHashCode(expected1, expected2);
    byte[] input = HashTestUtils.ascii(stringInput);
    assertEquals(expected, murmur3_128(seed).hashBytes(input));
    assertEquals(expected, murmur3_128(seed).newHasher().putBytes(input).hash());
  }

  /** Returns a {@link HashCode} for a sequence of longs, in big-endian order. */
  private static HashCode toHashCode(long... longs) {
    ByteBuffer bb = ByteBuffer.wrap(new byte[longs.length * 8]).order(ByteOrder.LITTLE_ENDIAN);
    for (long x : longs) {
      bb.putLong(x);
    }
    return HashCode.fromBytes(bb.array());
  }

  public void testParanoid() {
    HashFn hf =
        new HashFn() {
          @Override
          public byte[] hash(byte[] input, int seed) {
            Hasher hasher = murmur3_128(seed).newHasher();
            byteArrayFunnel().funnel(input, hasher);
            return hasher.hash().asBytes();
          }
        };
    // Murmur3F, MurmurHash3 for x64, 128-bit (MurmurHash3_x64_128)
    // From http://code.google.com/p/smhasher/source/browse/trunk/main.cpp
    HashTestUtils.verifyHashFunction(hf, 128, 0x6384BA69);
  }

  public void testInvariants() {
    HashTestUtils.assertInvariants(murmur3_128());
  }

  public void testNegativeSeed() {
    String fox = "The quick brown fox jumps over the lazy dog";
    // TODO(b/134667902): Should zero-extend seed like C++ MurmurHash3_x64_128 (uint32_t seed):
    // empty -> ecc93b9d4ddff16a6b44e61e12217485, fox -> 8aa100a8731d1c6912b4406409677d64.
    assertThat(murmur3_128(-1).hashString("", UTF_8).toString())
        .isEqualTo("edd65bc6388fc1e4af0d6220e98afd8e");
    assertThat(murmur3_128(-1).hashString(fox, UTF_8).toString())
        .isEqualTo("ce61803cae2cdc7d00cf75b7ab26aab3");
  }
}

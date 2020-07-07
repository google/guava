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

import com.google.common.base.Charsets;
import com.google.common.hash.HashTestUtils.HashFn;
import java.util.Random;
import junit.framework.TestCase;

/** Tests for {@link Murmur3_32HashFunction}. */
public class Murmur3Hash32Test extends TestCase {
  public void testKnownIntegerInputs() {
    assertHash(593689054, murmur3_32().hashInt(0));
    assertHash(-189366624, murmur3_32().hashInt(-42));
    assertHash(-1134849565, murmur3_32().hashInt(42));
    assertHash(-1718298732, murmur3_32().hashInt(Integer.MIN_VALUE));
    assertHash(-1653689534, murmur3_32().hashInt(Integer.MAX_VALUE));
  }

  public void testKnownLongInputs() {
    assertHash(1669671676, murmur3_32().hashLong(0L));
    assertHash(-846261623, murmur3_32().hashLong(-42L));
    assertHash(1871679806, murmur3_32().hashLong(42L));
    assertHash(1366273829, murmur3_32().hashLong(Long.MIN_VALUE));
    assertHash(-2106506049, murmur3_32().hashLong(Long.MAX_VALUE));
  }

  public void testKnownStringInputs() {
    assertHash(0, murmur3_32().hashUnencodedChars(""));
    assertHash(679745764, murmur3_32().hashUnencodedChars("k"));
    assertHash(1510782915, murmur3_32().hashUnencodedChars("hell"));
    assertHash(-675079799, murmur3_32().hashUnencodedChars("hello"));
    assertHash(1935035788, murmur3_32().hashUnencodedChars("http://www.google.com/"));
    assertHash(
        -528633700, murmur3_32().hashUnencodedChars("The quick brown fox jumps over the lazy dog"));
  }

  public void testKnownUtf8StringInputs() {
    assertHash(0, murmur3_32().hashString("", Charsets.UTF_8));
    assertHash(0xcfbda5d1, murmur3_32().hashString("k", Charsets.UTF_8));
    assertHash(0xa167dbf3, murmur3_32().hashString("hell", Charsets.UTF_8));
    assertHash(0x248bfa47, murmur3_32().hashString("hello", Charsets.UTF_8));
    assertHash(0x3d41b97c, murmur3_32().hashString("http://www.google.com/", Charsets.UTF_8));
    assertHash(
        0x2e4ff723,
        murmur3_32().hashString("The quick brown fox jumps over the lazy dog", Charsets.UTF_8));
    assertHash(0xfc5ba834, murmur3_32().hashString("毎月１日,毎週月曜日", Charsets.UTF_8));
  }

  @SuppressWarnings("deprecation")
  public void testSimpleStringUtf8() {
    assertEquals(
        murmur3_32().hashBytes("ABCDefGHI\u0799".getBytes(Charsets.UTF_8)),
        murmur3_32().hashString("ABCDefGHI\u0799", Charsets.UTF_8));
  }

  @SuppressWarnings("deprecation")
  public void testStringInputsUtf8() {
    Random rng = new Random(0);
    for (int z = 0; z < 100; z++) {
      String str;
      int[] codePoints = new int[rng.nextInt(8)];
      for (int i = 0; i < codePoints.length; i++) {
        do {
          codePoints[i] = rng.nextInt(0x800);
        } while (!Character.isValidCodePoint(codePoints[i])
            || (codePoints[i] >= Character.MIN_SURROGATE
                && codePoints[i] <= Character.MAX_SURROGATE));
      }
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < codePoints.length; i++) {
        builder.appendCodePoint(codePoints[i]);
      }
      str = builder.toString();
      assertEquals(
          murmur3_32().hashBytes(str.getBytes(Charsets.UTF_8)),
          murmur3_32().hashString(str, Charsets.UTF_8));
    }
  }

  private static void assertHash(int expected, HashCode actual) {
    assertEquals(HashCode.fromInt(expected), actual);
  }

  public void testParanoidHashBytes() {
    HashFn hf =
        new HashFn() {
          @Override
          public byte[] hash(byte[] input, int seed) {
            return murmur3_32(seed).hashBytes(input).asBytes();
          }
        };
    // Murmur3A, MurmurHash3 for x86, 32-bit (MurmurHash3_x86_32)
    // https://github.com/aappleby/smhasher/blob/master/src/main.cpp
    HashTestUtils.verifyHashFunction(hf, 32, 0xB0F57EE3);
  }

  public void testParanoid() {
    HashFn hf =
        new HashFn() {
          @Override
          public byte[] hash(byte[] input, int seed) {
            Hasher hasher = murmur3_32(seed).newHasher();
            Funnels.byteArrayFunnel().funnel(input, hasher);
            return hasher.hash().asBytes();
          }
        };
    // Murmur3A, MurmurHash3 for x86, 32-bit (MurmurHash3_x86_32)
    // https://github.com/aappleby/smhasher/blob/master/src/main.cpp
    HashTestUtils.verifyHashFunction(hf, 32, 0xB0F57EE3);
  }

  public void testInvariants() {
    HashTestUtils.assertInvariants(murmur3_32());
  }

  @SuppressWarnings("deprecation")
  public void testInvalidUnicodeHashString() {
    String str =
        new String(
            new char[] {'a', Character.MIN_HIGH_SURROGATE, Character.MIN_HIGH_SURROGATE, 'z'});
    assertEquals(
        murmur3_32().hashBytes(str.getBytes(Charsets.UTF_8)),
        murmur3_32().hashString(str, Charsets.UTF_8));
  }

  public void testInvalidUnicodeHasherPutString() {
    String str =
        new String(
            new char[] {'a', Character.MIN_HIGH_SURROGATE, Character.MIN_HIGH_SURROGATE, 'z'});
    assertEquals(
        murmur3_32().hashBytes(str.getBytes(Charsets.UTF_8)),
        murmur3_32().newHasher().putString(str, Charsets.UTF_8).hash());
  }
}

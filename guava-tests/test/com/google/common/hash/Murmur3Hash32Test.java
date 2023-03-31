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
import static com.google.common.hash.Hashing.murmur3_32_fixed;

import com.google.common.base.Charsets;
import com.google.common.hash.HashTestUtils.HashFn;
import java.nio.charset.Charset;
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

  @SuppressWarnings("deprecation")
  public void testKnownEncodedStringInputs() {
    assertStringHash(0, "", Charsets.UTF_8);
    assertStringHash(0xcfbda5d1, "k", Charsets.UTF_8);
    assertStringHash(0xa167dbf3, "hell", Charsets.UTF_8);
    assertStringHash(0x248bfa47, "hello", Charsets.UTF_8);
    assertStringHash(0x3d41b97c, "http://www.google.com/", Charsets.UTF_8);
    assertStringHash(0x2e4ff723, "The quick brown fox jumps over the lazy dog", Charsets.UTF_8);
    assertStringHash(0xb5a4be05, "ABCDefGHI\u0799", Charsets.UTF_8);
    assertStringHash(0xfc5ba834, "毎月１日,毎週月曜日", Charsets.UTF_8);
    assertStringHash(0x8a5c3699, "surrogate pair: \uD83D\uDCB0", Charsets.UTF_8);

    assertStringHash(0, "", Charsets.UTF_16LE);
    assertStringHash(0x288418e4, "k", Charsets.UTF_16LE);
    assertStringHash(0x5a0cb7c3, "hell", Charsets.UTF_16LE);
    assertStringHash(0xd7c31989, "hello", Charsets.UTF_16LE);
    assertStringHash(0x73564d8c, "http://www.google.com/", Charsets.UTF_16LE);
    assertStringHash(0xe07db09c, "The quick brown fox jumps over the lazy dog", Charsets.UTF_16LE);
    assertStringHash(0xfefa3e76, "ABCDefGHI\u0799", Charsets.UTF_16LE);
    assertStringHash(0x6a7be132, "毎月１日,毎週月曜日", Charsets.UTF_16LE);
    assertStringHash(0x5a2d41c7, "surrogate pair: \uD83D\uDCB0", Charsets.UTF_16LE);
  }

  @SuppressWarnings("deprecation")
  private void assertStringHash(int expected, String string, Charset charset) {
    if (allBmp(string)) {
      assertHash(expected, murmur3_32().hashString(string, charset));
    }
    assertHash(expected, murmur3_32_fixed().hashString(string, charset));
    assertHash(expected, murmur3_32().newHasher().putString(string, charset).hash());
    assertHash(expected, murmur3_32_fixed().newHasher().putString(string, charset).hash());
    assertHash(expected, murmur3_32().hashBytes(string.getBytes(charset)));
    assertHash(expected, murmur3_32_fixed().hashBytes(string.getBytes(charset)));
    assertHash(expected, murmur3_32().newHasher().putBytes(string.getBytes(charset)).hash());
    assertHash(expected, murmur3_32_fixed().newHasher().putBytes(string.getBytes(charset)).hash());
  }

  private boolean allBmp(String string) {
    // Ordinarily we'd use something like i += Character.charCount(string.codePointAt(i)) here. But
    // we can get away with i++ because the whole point of this method is to return false if we find
    // a code point that doesn't fit in a char.
    for (int i = 0; i < string.length(); i++) {
      if (string.codePointAt(i) > 0xffff) {
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings("deprecation")
  public void testSimpleStringUtf8() {
    assertEquals(
        murmur3_32().hashBytes("ABCDefGHI\u0799".getBytes(Charsets.UTF_8)),
        murmur3_32().hashString("ABCDefGHI\u0799", Charsets.UTF_8));
  }

  @SuppressWarnings("deprecation")
  public void testEncodedStringInputs() {
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
      HashCode hashUtf8 = murmur3_32().hashBytes(str.getBytes(Charsets.UTF_8));
      assertEquals(
          hashUtf8, murmur3_32().newHasher().putBytes(str.getBytes(Charsets.UTF_8)).hash());
      assertEquals(hashUtf8, murmur3_32().hashString(str, Charsets.UTF_8));
      assertEquals(hashUtf8, murmur3_32().newHasher().putString(str, Charsets.UTF_8).hash());
      HashCode hashUtf16 = murmur3_32().hashBytes(str.getBytes(Charsets.UTF_16));
      assertEquals(
          hashUtf16, murmur3_32().newHasher().putBytes(str.getBytes(Charsets.UTF_16)).hash());
      assertEquals(hashUtf16, murmur3_32().hashString(str, Charsets.UTF_16));
      assertEquals(hashUtf16, murmur3_32().newHasher().putString(str, Charsets.UTF_16).hash());
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
    assertEquals(
        murmur3_32_fixed().hashBytes(str.getBytes(Charsets.UTF_8)),
        murmur3_32().hashString(str, Charsets.UTF_8));
  }

  @SuppressWarnings("deprecation")
  public void testInvalidUnicodeHasherPutString() {
    String str =
        new String(
            new char[] {'a', Character.MIN_HIGH_SURROGATE, Character.MIN_HIGH_SURROGATE, 'z'});
    assertEquals(
        murmur3_32().hashBytes(str.getBytes(Charsets.UTF_8)),
        murmur3_32().newHasher().putString(str, Charsets.UTF_8).hash());
    assertEquals(
        murmur3_32_fixed().hashBytes(str.getBytes(Charsets.UTF_8)),
        murmur3_32_fixed().newHasher().putString(str, Charsets.UTF_8).hash());
  }
}

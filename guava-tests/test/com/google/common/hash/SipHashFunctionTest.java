/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.base.Charsets.UTF_8;

import com.google.common.collect.ImmutableSet;
import junit.framework.TestCase;

/**
 * Unit tests for {@link SipHashFunction}.
 *
 * @author Kurt Alfred Kluever
 */
public class SipHashFunctionTest extends TestCase {

  // From https://131002.net/siphash/siphash24.c
  // k = 00 01 02 ...
  private static final long K0 = 0x0706050403020100L;
  private static final long K1 = 0x0f0e0d0c0b0a0908L;
  private static final HashFunction SIP_WITH_KEY = Hashing.sipHash24(K0, K1);
  private static final HashFunction SIP_WITHOUT_KEY = Hashing.sipHash24();

  // These constants were originally ported from https://www.131002.net/siphash/siphash24.c. See:
  // https://github.com/nahi/siphash-java-inline/blob/master/src/test/java/org/jruby/util/SipHashInlineTest.java
  private static final long[] EXPECTED =
      new long[] {
        0x726fdb47dd0e0e31L,
        0x74f839c593dc67fdL,
        0x0d6c8009d9a94f5aL,
        0x85676696d7fb7e2dL,
        0xcf2794e0277187b7L,
        0x18765564cd99a68dL,
        0xcbc9466e58fee3ceL,
        0xab0200f58b01d137L,
        0x93f5f5799a932462L,
        0x9e0082df0ba9e4b0L,
        0x7a5dbbc594ddb9f3L,
        0xf4b32f46226bada7L,
        0x751e8fbc860ee5fbL,
        0x14ea5627c0843d90L,
        0xf723ca908e7af2eeL,
        0xa129ca6149be45e5L,
        0x3f2acc7f57c29bdbL,
        0x699ae9f52cbe4794L,
        0x4bc1b3f0968dd39cL,
        0xbb6dc91da77961bdL,
        0xbed65cf21aa2ee98L,
        0xd0f2cbb02e3b67c7L,
        0x93536795e3a33e88L,
        0xa80c038ccd5ccec8L,
        0xb8ad50c6f649af94L,
        0xbce192de8a85b8eaL,
        0x17d835b85bbb15f3L,
        0x2f2e6163076bcfadL,
        0xde4daaaca71dc9a5L,
        0xa6a2506687956571L,
        0xad87a3535c49ef28L,
        0x32d892fad841c342L,
        0x7127512f72f27cceL,
        0xa7f32346f95978e3L,
        0x12e0b01abb051238L,
        0x15e034d40fa197aeL,
        0x314dffbe0815a3b4L,
        0x027990f029623981L,
        0xcadcd4e59ef40c4dL,
        0x9abfd8766a33735cL,
        0x0e3ea96b5304a7d0L,
        0xad0c42d6fc585992L,
        0x187306c89bc215a9L,
        0xd4a60abcf3792b95L,
        0xf935451de4f21df2L,
        0xa9538f0419755787L,
        0xdb9acddff56ca510L,
        0xd06c98cd5c0975ebL,
        0xe612a3cb9ecba951L,
        0xc766e62cfcadaf96L,
        0xee64435a9752fe72L,
        0xa192d576b245165aL,
        0x0a8787bf8ecb74b2L,
        0x81b3e73d20b49b6fL,
        0x7fa8220ba3b2eceaL,
        0x245731c13ca42499L,
        0xb78dbfaf3a8d83bdL,
        0xea1ad565322a1a0bL,
        0x60e61c23a3795013L,
        0x6606d7e446282b93L,
        0x6ca4ecb15c5f91e1L,
        0x9f626da15c9625f3L,
        0xe51b38608ef25f57L,
        0x958a324ceb064572L
      };

  public void testVectors() {
    for (int i = 0; i < EXPECTED.length; ++i) {
      byte[] msg = new byte[i];
      for (int j = 0; j < i; ++j) {
        msg[j] = (byte) j;
      }
      assertSip(msg, EXPECTED[i]);
    }
  }

  // This test data comes from "SipHash: a fast short-input PRF", "Appendix A: Test values".
  // It can be downloaded here: https://131002.net/siphash/siphash.pdf
  public void test15ByteStringFromSipHashPaper() {
    byte[] message =
        new byte[] {
          0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e
        };
    long k0 = 0x0706050403020100L;
    long k1 = 0x0f0e0d0c0b0a0908L;

    assertEquals(0xa129ca6149be45e5L, Hashing.sipHash24(k0, k1).hashBytes(message).asLong());
  }

  // From https://github.com/BrandonHaynes/siphash-csharp/blob/master/tests/Tests.cs
  public void testKnownValues() {
    assertSip(new byte[] {}, 0x726fdb47dd0e0e31L);
    assertSip(new byte[] {0x61}, 0x2ba3e8e9a71148caL);
    assertSip(new byte[1000000], 0x28205108397aa742L);
    assertSip("12345678", 0x02130609caea37ebL);
    assertSip("abcdef", 0x2a6e77e733c7c05dL);
    assertSip("SipHash", 0x8325093242a96f60L);
  }

  // Test for common pitfall regarding sign extension.
  // For example: (long) data[i++] | (long) data[i++] << 8 | ...
  // If data[i] == (byte) 0x80, the first cast will sign-extend it to 0xffffffffffffff80,
  // masking the remaining seven bytes.
  // To test this, we give an input where bit 7 is not cleared. For example:
  // (1) 00 01 02 03 04 05 06 07 80
  // (2) 00 01 02 03 04 05 06 07 81
  // (3) 00 01 02 03 04 05 06 07 ff (or anything in between)
  // A fault implementation will generate collisions for these inputs.
  public void testCollisionsDueToIncorrectSignExtension() {
    byte[] col1 = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, (byte) 0x80};
    byte[] col2 = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, (byte) 0x81};
    byte[] col3 = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, (byte) 0xff};

    ImmutableSet<HashCode> hashCodes =
        ImmutableSet.of(
            SIP_WITH_KEY.hashBytes(col1),
            SIP_WITH_KEY.hashBytes(col2),
            SIP_WITH_KEY.hashBytes(col3));
    assertEquals(3, hashCodes.size());
  }

  public void testToString() {
    assertEquals("Hashing.sipHash24(" + K0 + ", " + K1 + ")", SIP_WITH_KEY.toString());
    assertEquals("Hashing.sipHash24(" + K0 + ", " + K1 + ")", SIP_WITHOUT_KEY.toString());
    assertEquals("Hashing.sipHash24(20, 13)", Hashing.sipHash24(20, 13).toString());
  }

  private static void assertSip(String input, long expected) {
    assertEquals(expected, SIP_WITH_KEY.hashString(input, UTF_8).asLong());
    assertEquals(expected, SIP_WITH_KEY.newHasher().putString(input, UTF_8).hash().asLong());
    assertEquals(expected, SIP_WITHOUT_KEY.hashString(input, UTF_8).asLong());
    assertEquals(expected, SIP_WITHOUT_KEY.newHasher().putString(input, UTF_8).hash().asLong());
  }

  private static void assertSip(byte[] input, long expected) {
    assertEquals(expected, SIP_WITH_KEY.hashBytes(input).asLong());
    assertEquals(expected, SIP_WITH_KEY.newHasher().putBytes(input).hash().asLong());
    assertEquals(expected, SIP_WITHOUT_KEY.hashBytes(input).asLong());
    assertEquals(expected, SIP_WITHOUT_KEY.newHasher().putBytes(input).hash().asLong());
  }
}

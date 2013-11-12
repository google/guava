/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

/**
 * Unit tests for {@link Utf8}.
 *
 * @author Jon Perlow
 * @author Martin Buchholz
 * @author Clément Roux
 */
@GwtCompatible(emulated = true)
public class Utf8Test extends TestCase {
  public void testEncodedLength_validStrings() {
    assertEquals(0, Utf8.encodedLength(""));
    assertEquals(11, Utf8.encodedLength("Hello world"));
    assertEquals(8, Utf8.encodedLength("Résumé"));
    assertEquals(461, Utf8.encodedLength("威廉·莎士比亞（William Shakespeare，"
        + "1564年4月26號—1616年4月23號[1]）係隻英國嗰演員、劇作家同詩人，"
        + "有時間佢簡稱莎翁；中國清末民初哈拕翻譯做舌克斯毕、沙斯皮耳、筛斯比耳、"
        + "莎基斯庇尔、索士比尔、夏克思芘尔、希哀苦皮阿、叶斯壁、沙克皮尔、"
        + "狹斯丕爾。[2]莎士比亞編寫過好多作品，佢嗰劇作響西洋文學好有影響，"
        + "哈都拕人翻譯做好多話。"));
    // A surrogate pair
    assertEquals(4, Utf8.encodedLength(
        newString(Character.MIN_HIGH_SURROGATE, Character.MIN_LOW_SURROGATE)));
  }

  public void testEncodedLength_invalidStrings() {
    testEncodedLengthFails(newString(Character.MIN_HIGH_SURROGATE), 0);
    testEncodedLengthFails("foobar" + newString(Character.MIN_HIGH_SURROGATE), 6);
    testEncodedLengthFails(newString(Character.MIN_LOW_SURROGATE), 0);
    testEncodedLengthFails("foobar" + newString(Character.MIN_LOW_SURROGATE), 6);
    testEncodedLengthFails(
        newString(
            Character.MIN_HIGH_SURROGATE,
            Character.MIN_HIGH_SURROGATE), 0);
  }

  private static void testEncodedLengthFails(String invalidString,
      int invalidCodePointIndex) {
    try {
      Utf8.encodedLength(invalidString);
      fail();
    } catch (IllegalArgumentException expected) {
      assertEquals("Unpaired surrogate at index " + invalidCodePointIndex,
          expected.getMessage());
    }
  }

  // 128 - [chars 0x0000 to 0x007f]
  private static final long ONE_BYTE_ROUNDTRIPPABLE_CHARACTERS =
      0x007f - 0x0000 + 1;

  // 128
  private static final long EXPECTED_ONE_BYTE_ROUNDTRIPPABLE_COUNT =
      ONE_BYTE_ROUNDTRIPPABLE_CHARACTERS;

  // 1920 [chars 0x0080 to 0x07FF]
  private static final long TWO_BYTE_ROUNDTRIPPABLE_CHARACTERS =
      0x07FF - 0x0080 + 1;

  // 18,304
  private static final long EXPECTED_TWO_BYTE_ROUNDTRIPPABLE_COUNT =
      // Both bytes are one byte characters
      (long) Math.pow(EXPECTED_ONE_BYTE_ROUNDTRIPPABLE_COUNT, 2) +
      // The possible number of two byte characters
      TWO_BYTE_ROUNDTRIPPABLE_CHARACTERS;

  // 2048
  private static final long THREE_BYTE_SURROGATES = 2 * 1024;

  // 61,440 [chars 0x0800 to 0xFFFF, minus surrogates]
  private static final long THREE_BYTE_ROUNDTRIPPABLE_CHARACTERS =
      0xFFFF - 0x0800 + 1 - THREE_BYTE_SURROGATES;

  // 2,650,112
  private static final long EXPECTED_THREE_BYTE_ROUNDTRIPPABLE_COUNT =
      // All one byte characters
      (long) Math.pow(EXPECTED_ONE_BYTE_ROUNDTRIPPABLE_COUNT, 3) +
      // One two byte character and a one byte character
      2 * TWO_BYTE_ROUNDTRIPPABLE_CHARACTERS *
          ONE_BYTE_ROUNDTRIPPABLE_CHARACTERS +
       // Three byte characters
      THREE_BYTE_ROUNDTRIPPABLE_CHARACTERS;

  // 1,048,576 [chars 0x10000L to 0x10FFFF]
  private static final long FOUR_BYTE_ROUNDTRIPPABLE_CHARACTERS =
      0x10FFFF - 0x10000L + 1;

  // 289,571,839
  private static final long EXPECTED_FOUR_BYTE_ROUNDTRIPPABLE_COUNT =
      // All one byte characters
      (long) Math.pow(EXPECTED_ONE_BYTE_ROUNDTRIPPABLE_COUNT, 4) +
      // One and three byte characters
      2 * THREE_BYTE_ROUNDTRIPPABLE_CHARACTERS *
          ONE_BYTE_ROUNDTRIPPABLE_CHARACTERS +
      // Two two byte characters
      TWO_BYTE_ROUNDTRIPPABLE_CHARACTERS * TWO_BYTE_ROUNDTRIPPABLE_CHARACTERS +
      // Permutations of one and two byte characters
      3 * TWO_BYTE_ROUNDTRIPPABLE_CHARACTERS *
          ONE_BYTE_ROUNDTRIPPABLE_CHARACTERS *
          ONE_BYTE_ROUNDTRIPPABLE_CHARACTERS +
      // Four byte characters
      FOUR_BYTE_ROUNDTRIPPABLE_CHARACTERS;

  /**
   * Tests that round tripping of a sample of four byte permutations work.
   * All permutations are prohibitively expensive to test for automated runs.
   * This method tests specific four-byte cases.
   */
  public void testIsWellFormed_4BytesSamples() {
    // Valid 4 byte.
    assertWellFormed(0xF0, 0xA4, 0xAD, 0xA2);
    // Bad trailing bytes
    assertNotWellFormed(0xF0, 0xA4, 0xAD, 0x7F);
    assertNotWellFormed(0xF0, 0xA4, 0xAD, 0xC0);
    // Special cases for byte2
    assertNotWellFormed(0xF0, 0x8F, 0xAD, 0xA2);
    assertNotWellFormed(0xF4, 0x90, 0xAD, 0xA2);
  }

  /** Tests some hard-coded test cases. */
  public void testSomeSequences() {
    // Empty
    assertWellFormed();
    // One-byte characters, including control characters
    assertWellFormed(0x00, 0x61, 0x62, 0x63, 0x7F); // "\u0000abc\u007f"
    // Two-byte characters
    assertWellFormed(0xC2, 0xA2, 0xC2, 0xA2); // "\u00a2\u00a2"
    // Three-byte characters
    assertWellFormed(0xc8, 0x8a, 0x63, 0xc8, 0x8a, 0x63); // "\u020ac\u020ac"
    // Four-byte characters
    // "\u024B62\u024B62"
    assertWellFormed(0xc9, 0x8b, 0x36, 0x32, 0xc9, 0x8b, 0x36, 0x32);
    // Mixed string
    // "a\u020ac\u00a2b\\u024B62u020acc\u00a2de\u024B62"
    assertWellFormed(0x61, 0xc8, 0x8a, 0x63, 0xc2, 0xa2, 0x62, 0x5c, 0x75, 0x30,
        0x32, 0x34, 0x42, 0x36, 0x32, 0x75, 0x30, 0x32, 0x30, 0x61, 0x63, 0x63,
        0xc2, 0xa2, 0x64, 0x65, 0xc9, 0x8b, 0x36, 0x32);
    // Not a valid string
    assertNotWellFormed(-1, 0, -1, 0);
  }

  public void testShardsHaveExpectedRoundTrippables() {
    // A sanity check.
    long actual = 0;
    for (long expected : generateFourByteShardsExpectedRunnables()) {
      actual += expected;
    }
    assertEquals(EXPECTED_FOUR_BYTE_ROUNDTRIPPABLE_COUNT, actual);
  }

  private String newString(char... chars) {
    return new String(chars);
  }

  private byte[] toByteArray(int... bytes) {
    byte[] realBytes = new byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      realBytes[i] = (byte) bytes[i];
    }
    return realBytes;
  }

  private void assertWellFormed(int... bytes) {
    assertTrue(Utf8.isWellFormed(toByteArray(bytes)));
  }

  private void assertNotWellFormed(int... bytes) {
    assertFalse(Utf8.isWellFormed(toByteArray(bytes)));
  }

  private static long[] generateFourByteShardsExpectedRunnables() {
    long[] expected = new long[128];
    // 0-63 are all 5300224
    for (int i = 0; i <= 63; i++) {
      expected[i] = 5300224;
    }
    // 97-111 are all 2342912
    for (int i = 97; i <= 111; i++) {
     expected[i] = 2342912;
    }
    // 113-117 are all 1048576
    for (int i = 113; i <= 117; i++) {
      expected[i] = 1048576;
    }
    // One offs
    expected[112] = 786432;
    expected[118] = 786432;
    expected[119] = 1048576;
    expected[120] = 458752;
    expected[121] = 524288;
    expected[122] = 65536;
    // Anything not assigned was the default 0.
    return expected;
  }
}


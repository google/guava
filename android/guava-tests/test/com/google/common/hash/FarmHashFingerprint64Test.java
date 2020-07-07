/*
 * Copyright (C) 2015 The Guava Authors
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

import static com.google.common.base.Charsets.ISO_8859_1;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Strings;
import java.util.Arrays;
import junit.framework.TestCase;

/**
 * Unit test for FarmHashFingerprint64.
 *
 * @author Kyle Maddison
 * @author Geoff Pike
 */
public class FarmHashFingerprint64Test extends TestCase {

  private static final HashFunction HASH_FN = Hashing.farmHashFingerprint64();

  // If this test fails, all bets are off
  public void testReallySimpleFingerprints() {
    assertEquals(8581389452482819506L, fingerprint("test".getBytes(UTF_8)));
    // 32 characters long
    assertEquals(-4196240717365766262L, fingerprint(Strings.repeat("test", 8).getBytes(UTF_8)));
    // 256 characters long
    assertEquals(3500507768004279527L, fingerprint(Strings.repeat("test", 64).getBytes(UTF_8)));
  }

  public void testStringsConsistency() {
    for (String s : Arrays.asList("", "some", "test", "strings", "to", "try")) {
      assertEquals(HASH_FN.newHasher().putUnencodedChars(s).hash(), HASH_FN.hashUnencodedChars(s));
    }
  }

  public void testUtf8() {
    char[] charsA = new char[128];
    char[] charsB = new char[128];

    for (int i = 0; i < charsA.length; i++) {
      if (i < 100) {
        charsA[i] = 'a';
        charsB[i] = 'a';
      } else {
        // Both two-byte characters, but must be different
        charsA[i] = (char) (0x0180 + i);
        charsB[i] = (char) (0x0280 + i);
      }
    }

    String stringA = new String(charsA);
    String stringB = new String(charsB);
    assertThat(stringA).isNotEqualTo(stringB);
    assertThat(HASH_FN.hashUnencodedChars(stringA))
        .isNotEqualTo(HASH_FN.hashUnencodedChars(stringB));
    assertThat(fingerprint(stringA.getBytes(UTF_8)))
        .isNotEqualTo(fingerprint(stringB.getBytes(UTF_8)));

    // ISO 8859-1 only has 0-255 (ubyte) representation so throws away UTF-8 characters
    // greater than 127 (ie with their top bit set).
    // Don't attempt to do this in real code.
    assertEquals(
        fingerprint(stringA.getBytes(ISO_8859_1)), fingerprint(stringB.getBytes(ISO_8859_1)));
  }

  public void testPutNonChars() {
    Hasher hasher = HASH_FN.newHasher();
    // Expected data is 0x0100010100000000
    hasher
        .putBoolean(true)
        .putBoolean(true)
        .putBoolean(false)
        .putBoolean(true)
        .putBoolean(false)
        .putBoolean(false)
        .putBoolean(false)
        .putBoolean(false);
    final long hashCode = hasher.hash().asLong();

    hasher = HASH_FN.newHasher();
    hasher
        .putByte((byte) 0x01)
        .putByte((byte) 0x01)
        .putByte((byte) 0x00)
        .putByte((byte) 0x01)
        .putByte((byte) 0x00)
        .putByte((byte) 0x00)
        .putByte((byte) 0x00)
        .putByte((byte) 0x00);
    assertEquals(hashCode, hasher.hash().asLong());

    hasher = HASH_FN.newHasher();
    hasher
        .putChar((char) 0x0101)
        .putChar((char) 0x0100)
        .putChar((char) 0x0000)
        .putChar((char) 0x0000);
    assertEquals(hashCode, hasher.hash().asLong());

    hasher = HASH_FN.newHasher();
    hasher.putBytes(new byte[] {0x01, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00});
    assertEquals(hashCode, hasher.hash().asLong());

    hasher = HASH_FN.newHasher();
    hasher.putLong(0x0000000001000101L);
    assertEquals(hashCode, hasher.hash().asLong());

    hasher = HASH_FN.newHasher();
    hasher
        .putShort((short) 0x0101)
        .putShort((short) 0x0100)
        .putShort((short) 0x0000)
        .putShort((short) 0x0000);
    assertEquals(hashCode, hasher.hash().asLong());
  }

  public void testHashFloatIsStable() {
    // Just a spot check.  Better than nothing.
    Hasher hasher = HASH_FN.newHasher();
    hasher.putFloat(0x01000101f).putFloat(0f);
    assertEquals(0x49f9d18ee8ae1b28L, hasher.hash().asLong());

    hasher = HASH_FN.newHasher();
    hasher.putDouble(0x0000000001000101d);
    assertEquals(0x388ee898bad75cbfL, hasher.hash().asLong());
  }

  /** Convenience method to compute a fingerprint on a full bytes array. */
  private static long fingerprint(byte[] bytes) {
    return fingerprint(bytes, bytes.length);
  }

  /** Convenience method to compute a fingerprint on a subset of a byte array. */
  private static long fingerprint(byte[] bytes, int length) {
    return HASH_FN.hashBytes(bytes, 0, length).asLong();
  }

  /**
   * Tests that the Java port of FarmHashFingerprint64 provides the same results on buffers up to
   * 800 bytes long as the C++ reference implementation.
   */
  public void testMultipleLengths() {
    int iterations = 800;
    byte[] buf = new byte[iterations * 4];
    int bufLen = 0;
    long h = 0;
    for (int i = 0; i < iterations; ++i) {
      h ^= fingerprint(buf, i);
      h = remix(h);
      buf[bufLen++] = getChar(h);

      h ^= fingerprint(buf, i * i % bufLen);
      h = remix(h);
      buf[bufLen++] = getChar(h);

      h ^= fingerprint(buf, i * i * i % bufLen);
      h = remix(h);
      buf[bufLen++] = getChar(h);

      h ^= fingerprint(buf, bufLen);
      h = remix(h);
      buf[bufLen++] = getChar(h);

      int x0 = buf[bufLen - 1] & 0xff;
      int x1 = buf[bufLen - 2] & 0xff;
      int x2 = buf[bufLen - 3] & 0xff;
      int x3 = buf[bufLen / 2] & 0xff;
      buf[((x0 << 16) + (x1 << 8) + x2) % bufLen] ^= x3;
      buf[((x1 << 16) + (x2 << 8) + x3) % bufLen] ^= i % 256;
    }
    assertEquals(0x7a1d67c50ec7e167L, h);
  }

  private static long remix(long h) {
    h ^= h >>> 41;
    h *= 949921979;
    return h;
  }

  private static byte getChar(long h) {
    return (byte) ('a' + ((h & 0xfffff) % 26));
  }
}

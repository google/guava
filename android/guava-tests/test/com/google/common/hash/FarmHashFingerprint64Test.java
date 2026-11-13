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

import static com.google.common.base.Strings.repeat;
import static com.google.common.hash.Hashing.farmHashFingerprint64;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashTestUtils.RandomHasherAction;
import java.nio.ByteBuffer;
import java.util.Random;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit test for FarmHashFingerprint64.
 *
 * @author Kyle Maddison
 * @author Geoff Pike
 */
@NullUnmarked
public class FarmHashFingerprint64Test extends TestCase {

  private static final HashFunction HASH_FN = farmHashFingerprint64();

  // If this test fails, all bets are off
  @SuppressWarnings("InlineMeInliner") // String.repeat unavailable under Java 8
  public void testReallySimpleFingerprints() {
    assertEquals(8581389452482819506L, fingerprint("test".getBytes(UTF_8)));
    // 32 characters long
    assertEquals(-4196240717365766262L, fingerprint(repeat("test", 8).getBytes(UTF_8)));
    // 256 characters long
    assertEquals(3500507768004279527L, fingerprint(repeat("test", 64).getBytes(UTF_8)));
  }

  public void testStringsConsistency() {
    for (String s : asList("", "some", "test", "strings", "to", "try")) {
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
    long hashCode = hasher.hash().asLong();

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

  public void testStreamingAllLengthsByteByByte() {
    byte[] data = new byte[260];
    new Random(0x12345678L).nextBytes(data);
    for (int len = 0; len <= data.length; len++) {
      Hasher hasher = HASH_FN.newHasher();
      for (int i = 0; i < len; i++) {
        hasher.putByte(data[i]);
      }
      assertEquals("byte-by-byte length " + len, HASH_FN.hashBytes(data, 0, len), hasher.hash());
    }
  }

  public void testStreamingChunkSizesAndBoundaries() {
    byte[] data = new byte[320];
    new Random(0x87654321L).nextBytes(data);
    int sliceOffset = 3;
    int slicePosition = 4;
    int totalPrefix = sliceOffset + slicePosition;
    byte[] padded = new byte[totalPrefix + data.length];
    new Random(0x11223344L).nextBytes(padded);
    arraycopy(data, 0, padded, totalPrefix, data.length);

    int[] lengths = {
      0, 1, 7, 8, 16, 17, 32, 33, 63, 64, 65, 66, 127, 128, 129, 191, 192, 193, 255, 256, 257, 320
    };
    int[] chunkSizes = {1, 2, 3, 7, 16, 31, 32, 63, 64, 65, 99, 128, 129, 256};
    byte[] empty = new byte[0];

    for (int len : lengths) {
      HashCode expected = HASH_FN.hashBytes(data, 0, len);
      for (int chunkSize : chunkSizes) {
        Hasher arrayHasher = HASH_FN.newHasher();
        Hasher slicedArrayBufferHasher = HASH_FN.newHasher();
        Hasher readOnlyBufferHasher = HASH_FN.newHasher();
        for (int off = 0; off < len; off += chunkSize) {
          int n = min(chunkSize, len - off);
          // Interleave empty writes to verify zero-length puts never prematurely mix a full block.
          arrayHasher.putBytes(empty);
          arrayHasher.putBytes(data, off, n);

          slicedArrayBufferHasher.putBytes(ByteBuffer.wrap(padded, sliceOffset, 0).slice());
          ByteBuffer sliced = ByteBuffer.wrap(padded, sliceOffset + off, slicePosition + n).slice();
          Java8Compatibility.position(sliced, slicePosition);
          assertEquals(sliceOffset + off, sliced.arrayOffset());
          slicedArrayBufferHasher.putBytes(sliced);
          assertEquals(0, sliced.remaining());

          ByteBuffer ro = ByteBuffer.wrap(data, off, n).asReadOnlyBuffer();
          readOnlyBufferHasher.putBytes(ByteBuffer.wrap(empty).asReadOnlyBuffer());
          readOnlyBufferHasher.putBytes(ro);
          assertEquals(0, ro.remaining());
        }
        arrayHasher.putBytes(empty);
        slicedArrayBufferHasher.putBytes(ByteBuffer.wrap(padded, sliceOffset, 0).slice());
        readOnlyBufferHasher.putBytes(ByteBuffer.wrap(empty).asReadOnlyBuffer());
        assertEquals("array len=" + len + " chunk=" + chunkSize, expected, arrayHasher.hash());
        assertEquals(
            "slicedArrayBuffer len=" + len + " chunk=" + chunkSize,
            expected,
            slicedArrayBufferHasher.hash());
        assertEquals(
            "readOnlyBuffer len=" + len + " chunk=" + chunkSize,
            expected,
            readOnlyBufferHasher.hash());
      }
      // Also test hashBytes(ByteBuffer) on a sliced array-backed buffer (arrayOffset > 0 and
      // position > 0) and on a read-only (non-array-backed) ByteBuffer.
      ByteBuffer fullSliced = ByteBuffer.wrap(padded, sliceOffset, slicePosition + len).slice();
      Java8Compatibility.position(fullSliced, slicePosition);
      assertEquals(sliceOffset, fullSliced.arrayOffset());
      assertEquals("hashBytes(sliced) len=" + len, expected, HASH_FN.hashBytes(fullSliced));
      assertEquals(0, fullSliced.remaining());

      ByteBuffer fullReadOnly = ByteBuffer.wrap(data, 0, len).asReadOnlyBuffer();
      assertEquals("hashBytes(ro) len=" + len, expected, HASH_FN.hashBytes(fullReadOnly));
      assertEquals(0, fullReadOnly.remaining());
    }
  }

  private static HashFunction nonStreamingFarmHash() {
    return new AbstractNonStreamingHashFunction() {
      @Override
      public HashCode hashBytes(byte[] input, int off, int len) {
        return HASH_FN.hashBytes(input, off, len);
      }

      @Override
      public int bits() {
        return 64;
      }
    };
  }

  public void testStreamingRandomActionsAgainstNonStreaming() {
    HashFunction nonStreaming = nonStreamingFarmHash();
    Random random = new Random(0xCAFEBABEL);
    for (int trial = 0; trial < 200; trial++) {
      Hasher streamingHasher = HASH_FN.newHasher();
      Hasher nonStreamingHasher = nonStreaming.newHasher();
      ImmutableList<Hasher> hashers = ImmutableList.of(streamingHasher, nonStreamingHasher);
      int ops = random.nextInt(40);
      for (int op = 0; op < ops; op++) {
        if (random.nextInt(10) == 0) {
          // RandomHasherAction.PUT_BYTE_BUFFER only uses array-backed buffers; also mix in
          // non-array-backed read-only buffers.
          byte[] buf = new byte[random.nextInt(220)];
          random.nextBytes(buf);
          int pos = random.nextInt(buf.length + 1);
          int len = random.nextInt(buf.length - pos + 1);
          for (Hasher hasher : hashers) {
            ByteBuffer bb =
                ByteBuffer.wrap(buf, pos, len)
                    .asReadOnlyBuffer()
                    .order(random.nextBoolean() ? LITTLE_ENDIAN : BIG_ENDIAN);
            hasher.putBytes(bb);
            assertEquals(0, bb.remaining());
          }
        } else {
          RandomHasherAction.pickAtRandom(random).performAction(random, hashers);
        }
        if (random.nextInt(10) == 0) {
          assertEquals(nonStreamingHasher.hash(), streamingHasher.hash());
        }
      }
      assertEquals(nonStreamingHasher.hash(), streamingHasher.hash());
    }
  }

  public void testStreamingPrimitivesAcrossBlockBoundaries() {
    HashFunction nonStreaming = nonStreamingFarmHash();
    Random random = new Random(0x12345678L);

    // prefixBytes in 0..7 covers aligned pos == 64 boundaries and every possible straddling offset
    // (pos == 63 for short/char, 61..63 for int, 57..63 for long).
    for (int prefixBytes = 0; prefixBytes <= 7; prefixBytes++) {
      Hasher shortStreaming = HASH_FN.newHasher();
      Hasher shortNonStreaming = nonStreaming.newHasher();
      Hasher charStreaming = HASH_FN.newHasher();
      Hasher charNonStreaming = nonStreaming.newHasher();
      Hasher intStreaming = HASH_FN.newHasher();
      Hasher intNonStreaming = nonStreaming.newHasher();
      Hasher longStreaming = HASH_FN.newHasher();
      Hasher longNonStreaming = nonStreaming.newHasher();

      for (int p = 0; p < prefixBytes; p++) {
        byte b = (byte) random.nextInt();
        shortStreaming.putByte(b);
        shortNonStreaming.putByte(b);
        charStreaming.putByte(b);
        charNonStreaming.putByte(b);
        intStreaming.putByte(b);
        intNonStreaming.putByte(b);
        longStreaming.putByte(b);
        longNonStreaming.putByte(b);
      }

      for (int i = 0; i < 100; i++) {
        short s = (short) random.nextInt();
        shortStreaming.putShort(s);
        shortNonStreaming.putShort(s);
        assertEquals(
            "short prefix=" + prefixBytes + " step=" + i,
            shortNonStreaming.hash(),
            shortStreaming.hash());
      }
      for (int i = 0; i < 100; i++) {
        char c = (char) random.nextInt();
        charStreaming.putChar(c);
        charNonStreaming.putChar(c);
        assertEquals(
            "char prefix=" + prefixBytes + " step=" + i,
            charNonStreaming.hash(),
            charStreaming.hash());
      }
      for (int i = 0; i < 50; i++) {
        int value = random.nextInt();
        intStreaming.putInt(value);
        intNonStreaming.putInt(value);
        assertEquals(
            "int prefix=" + prefixBytes + " step=" + i,
            intNonStreaming.hash(),
            intStreaming.hash());
      }
      for (int i = 0; i < 25; i++) {
        long value = random.nextLong();
        longStreaming.putLong(value);
        longNonStreaming.putLong(value);
        assertEquals(
            "long prefix=" + prefixBytes + " step=" + i,
            longNonStreaming.hash(),
            longStreaming.hash());
      }
    }
  }

  public void testStreamingLargeInput() {
    byte[] large = new byte[200_000];
    new Random(0xDEADBEEFL).nextBytes(large);
    HashCode expected = HASH_FN.hashBytes(large);

    Hasher hasher = HASH_FN.newHasher();
    for (int off = 0; off < large.length; off += 777) {
      hasher.putBytes(large, off, min(777, large.length - off));
    }
    assertEquals(expected, hasher.hash());
  }

  @J2ktIncompatible
  public void testStreamingDirectByteBuffer() {
    byte[] data = new byte[1024];
    new Random(0xBEEFCAFEL).nextBytes(data);
    int[] lengths = {0, 1, 63, 64, 65, 127, 128, 129, 500, 1000};
    for (int len : lengths) {
      ByteBuffer direct = ByteBuffer.allocateDirect(len + 10);
      Java8Compatibility.position(direct, 5);
      direct.put(data, 0, len);
      Java8Compatibility.limit(direct, 5 + len);
      Java8Compatibility.position(direct, 5);

      HashCode expected = HASH_FN.hashBytes(data, 0, len);

      direct.order(BIG_ENDIAN);
      assertEquals(expected, HASH_FN.hashBytes(direct));
      assertEquals(BIG_ENDIAN, direct.order());
      assertEquals(0, direct.remaining());

      Java8Compatibility.position(direct, 5);
      direct.order(LITTLE_ENDIAN);
      Hasher hasher = HASH_FN.newHasher();
      // Feed in two halves (testing partial buffer + direct buffer streaming).
      int mid = len / 2;
      Java8Compatibility.limit(direct, 5 + mid);
      hasher.putBytes(direct);
      Java8Compatibility.limit(direct, 5 + len);
      hasher.putBytes(direct);
      assertEquals(LITTLE_ENDIAN, direct.order());
      assertEquals(expected, hasher.hash());
    }
  }
}

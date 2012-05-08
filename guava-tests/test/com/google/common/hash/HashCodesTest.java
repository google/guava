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

import static com.google.common.testing.SerializableTester.reserializeAndAssert;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Tests for HashCodes, especially making sure that their endianness promises (big-endian)
 * are upheld.
 *
 * @author Dimitris Andreou
 */
public class HashCodesTest extends TestCase {
  // note: asInt(), asLong() are in little endian
  private static final ImmutableList<ExpectedHashCode> expectedHashCodes = ImmutableList.of(
      new ExpectedHashCode(new byte[] {
        (byte) 0xef, (byte) 0xcd, (byte) 0xab, (byte) 0x89,
        (byte) 0x67, (byte) 0x45, (byte) 0x23, (byte) 0x01},
        0x89abcdef, 0x0123456789abcdefL, "efcdab8967452301"),

      new ExpectedHashCode(new byte[] {
        (byte) 0xef, (byte) 0xcd, (byte) 0xab, (byte) 0x89,
        (byte) 0x67, (byte) 0x45, (byte) 0x23, (byte) 0x01, // up to here, same bytes as above
        (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
        (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08},
        0x89abcdef, 0x0123456789abcdefL, // asInt/asLong as above, due to equal eight first bytes
        "efcdab89674523010102030405060708"),

      new ExpectedHashCode(new byte[] { (byte) 0xdf, (byte) 0x9b, (byte) 0x57, (byte) 0x13 },
        0x13579bdf, null, "df9b5713"),

      new ExpectedHashCode(new byte[] {
          (byte) 0xcd, (byte) 0xab, (byte) 0x00, (byte) 0x00},
          0x0000abcd, null, "cdab0000"),

      new ExpectedHashCode(new byte[] {
          (byte) 0xef, (byte) 0xcd, (byte) 0xab, (byte) 0x00,
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00},
          0x00abcdef, 0x0000000000abcdefL, "efcdab0000000000")
    );

  // expectedHashCodes must contain at least one hash code with 4 bytes
  public void testFromInt() {
    for (ExpectedHashCode expected : expectedHashCodes) {
      if (expected.bytes.length == 4) {
        HashCode fromInt = HashCodes.fromInt(expected.asInt);
        assertExpectedHashCode(expected, fromInt);
      }
    }
  }

  // expectedHashCodes must contain at least one hash code with 8 bytes
  public void testFromLong() {
    for (ExpectedHashCode expected : expectedHashCodes) {
      if (expected.bytes.length == 8) {
        HashCode fromLong = HashCodes.fromLong(expected.asLong);
        assertExpectedHashCode(expected, fromLong);
      }
    }
  }

  public void testFromBytes() {
    for (ExpectedHashCode expected : expectedHashCodes) {
      HashCode fromBytes = HashCodes.fromBytes(expected.bytes);
      assertExpectedHashCode(expected, fromBytes);
    }
  }

  public void testFromBytes_copyOccurs() {
    byte[] bytes = new byte[] { (byte) 0xcd, (byte) 0xab, (byte) 0x00, (byte) 0x00 };
    HashCode hashCode = HashCodes.fromBytes(bytes);
    int expectedInt = 0x0000abcd;
    String expectedToString = "cdab0000";

    assertEquals(expectedInt, hashCode.asInt());
    assertEquals(expectedToString, hashCode.toString());

    bytes[0] = (byte) 0x00;

    assertEquals(expectedInt, hashCode.asInt());
    assertEquals(expectedToString, hashCode.toString());
  }

  public void testFromBytesNoCopy_noCopyOccurs() {
    byte[] bytes = new byte[] { (byte) 0xcd, (byte) 0xab, (byte) 0x00, (byte) 0x00 };
    HashCode hashCode = HashCodes.fromBytesNoCopy(bytes);

    assertEquals(0x0000abcd, hashCode.asInt());
    assertEquals("cdab0000", hashCode.toString());

    bytes[0] = (byte) 0x00;

    assertEquals(0x0000ab00, hashCode.asInt());
    assertEquals("00ab0000", hashCode.toString());
  }

  public void testSerialization_IntHashCode() {
    reserializeAndAssert(HashCodes.fromInt(42));
  }

  public void testSerialization_LongHashCode() {
    reserializeAndAssert(HashCodes.fromLong(42L));
  }

  public void testSerialization_BytesHashCode() {
    reserializeAndAssert(HashCodes.fromBytes(expectedHashCodes.get(0).bytes));
  }

  public void testSerialization_BytesHashCode_noCopy() {
    reserializeAndAssert(HashCodes.fromBytesNoCopy(expectedHashCodes.get(0).bytes));
  }

  private void assertExpectedHashCode(ExpectedHashCode expected, HashCode hash) {
    assertTrue(Arrays.equals(expected.bytes, hash.asBytes()));
    byte[] bb = new byte[hash.bits() / 8];
    hash.writeBytesTo(bb, 0, bb.length);
    assertTrue(Arrays.equals(expected.bytes, bb));
    assertEquals(expected.asInt, hash.asInt());
    if (expected.asLong == null) {
      try {
        hash.asLong();
        fail();
      } catch (IllegalStateException ok) {}
    } else {
      assertEquals(expected.asLong.longValue(), hash.asLong());
    }
    assertEquals(expected.toString, hash.toString());
    assertSideEffectFree(hash);
    assertReadableBytes(hash);
  }

  private void assertSideEffectFree(HashCode hash) {
    byte[] original = hash.asBytes();
    byte[] mutated = hash.asBytes();
    mutated[0]++;
    assertTrue(Arrays.equals(original, hash.asBytes()));
  }

  private void assertReadableBytes(HashCode hashCode) {
    assertTrue(hashCode.bits() >= 32); // sanity
    byte[] hashBytes = hashCode.asBytes();
    int totalBytes = hashCode.bits() / 8;

    for (int bytes = 0; bytes < totalBytes; bytes++) {
      byte[] bb = new byte[bytes];
      hashCode.writeBytesTo(bb, 0, bb.length);

      assertTrue(Arrays.equals(Arrays.copyOf(hashBytes, bytes), bb));
    }
  }

  private static class ExpectedHashCode {
    final byte[] bytes;
    final int asInt;
    final Long asLong; // null means that asLong should throw an exception
    final String toString;
    ExpectedHashCode(byte[] bytes, int asInt, Long asLong, String toString) {
      this.bytes = bytes;
      this.asInt = asInt;
      this.asLong = asLong;
      this.toString = toString;
    }
  }
}

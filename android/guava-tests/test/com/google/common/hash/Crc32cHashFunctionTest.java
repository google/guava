/*
 * Copyright (C) 2011 The Guava Authors
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

import java.util.Arrays;
import junit.framework.TestCase;

/**
 * Unit tests for {@link Crc32c}. Known test values are from RFC 3720, Section B.4.
 *
 * @author Patrick Costello
 * @author Kurt Alfred Kluever
 */
public class Crc32cHashFunctionTest extends TestCase {

  public void testZeros() {
    // Test 32 byte array of 0x00.
    byte[] zeros = new byte[32];
    Arrays.fill(zeros, (byte) 0x00);
    assertCrc(0x8a9136aa, zeros);
  }

  public void testFull() {
    // Test 32 byte array of 0xFF.
    byte[] fulls = new byte[32];
    Arrays.fill(fulls, (byte) 0xFF);
    assertCrc(0x62a8ab43, fulls);
  }

  public void testAscending() {
    // Test 32 byte arrays of ascending.
    byte[] ascending = new byte[32];
    for (int i = 0; i < 32; i++) {
      ascending[i] = (byte) i;
    }
    assertCrc(0x46dd794e, ascending);
  }

  public void testDescending() {
    // Test 32 byte arrays of descending.
    byte[] descending = new byte[32];
    for (int i = 0; i < 32; i++) {
      descending[i] = (byte) (31 - i);
    }
    assertCrc(0x113fdb5c, descending);
  }

  public void testScsiReadCommand() {
    // Test SCSI read command.
    byte[] scsiReadCommand =
        new byte[] {
          0x01, (byte) 0xc0, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00,
          0x14, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x04, 0x00,
          0x00, 0x00, 0x00, 0x14,
          0x00, 0x00, 0x00, 0x18,
          0x28, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00,
          0x02, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00
        };
    assertCrc(0xd9963a56, scsiReadCommand);
  }

  // Known values from http://www.evanjones.ca/crc32c.html
  public void testSomeOtherKnownValues() {
    assertCrc(0x22620404, "The quick brown fox jumps over the lazy dog".getBytes(UTF_8));
    assertCrc(0xE3069283, "123456789".getBytes(UTF_8));
    assertCrc(0xf3dbd4fe, "1234567890".getBytes(UTF_8));
    assertCrc(0xBFE92A83, "23456789".getBytes(UTF_8));
  }

  /**
   * Verifies that the crc of an array of byte data matches the expected value.
   *
   * @param expectedCrc the expected crc value.
   * @param data the data to run the checksum on.
   */
  private static void assertCrc(int expectedCrc, byte[] data) {
    int actualCrc = Hashing.crc32c().hashBytes(data).asInt();
    assertEquals(expectedCrc, actualCrc);
  }

  // From RFC 3720, Section 12.1, the polynomial generator is 0x11EDC6F41.
  // We calculate the constant below by:
  //   1. Omitting the most significant bit (because it's always 1). => 0x1EDC6F41
  //   2. Flipping the bits of the constant so we can process a byte at a time. => 0x82F63B78
  private static final int CRC32C_GENERATOR = 0x1EDC6F41; // 0x11EDC6F41
  private static final int CRC32C_GENERATOR_FLIPPED = Integer.reverse(CRC32C_GENERATOR);

  public void testCrc32cLookupTable() {
    // See Hacker's Delight 2nd Edition, Figure 14-7.
    int[] expected = new int[256];
    for (int i = 0; i < expected.length; i++) {
      int crc = i;
      for (int j = 7; j >= 0; j--) {
        int mask = -(crc & 1);
        crc = ((crc >>> 1) ^ (CRC32C_GENERATOR_FLIPPED & mask));
      }
      expected[i] = crc;
    }

    int[] actual = Crc32cHashFunction.Crc32cHasher.CRC_TABLE;
    assertTrue(
        "Expected: \n" + Arrays.toString(expected) + "\nActual:\n" + Arrays.toString(actual),
        Arrays.equals(expected, actual));
  }
}

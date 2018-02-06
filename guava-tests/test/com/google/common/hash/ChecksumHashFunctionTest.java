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

import static com.google.common.hash.Hashing.ChecksumType.ADLER_32;
import static com.google.common.hash.Hashing.ChecksumType.CRC_32;

import java.util.zip.Checksum;
import junit.framework.TestCase;

/**
 * Tests for ChecksumHashFunction.
 *
 * @author Colin Decker
 */
public class ChecksumHashFunctionTest extends TestCase {

  public void testCrc32_equalsChecksumValue() throws Exception {
    assertChecksum(CRC_32, "");
    assertChecksum(CRC_32, "Z");
    assertChecksum(CRC_32, "foobar");
  }

  public void testAdler32_equalsChecksumValue() throws Exception {
    assertChecksum(ADLER_32, "");
    assertChecksum(ADLER_32, "Z");
    assertChecksum(ADLER_32, "foobar");
  }

  public void testCrc32_knownValues() throws Exception {
    assertHash32(0x1C8600E3, CRC_32, "hell");
    assertHash32(0x3610A686, CRC_32, "hello");
    assertHash32(0xED81F9F6, CRC_32, "hello ");
    assertHash32(0x4850DDC2, CRC_32, "hello w");
    assertHash32(0x7A2D6005, CRC_32, "hello wo");
    assertHash32(0x1C192672, CRC_32, "hello wor");
    assertHash32(0x414FA339, CRC_32, "The quick brown fox jumps over the lazy dog");
    assertHash32(0x4400B5BC, CRC_32, "The quick brown fox jumps over the lazy cog");
  }

  public void testAdler32_knownValues() throws Exception {
    assertHash32(0x041701A6, ADLER_32, "hell");
    assertHash32(0x062C0215, ADLER_32, "hello");
    assertHash32(0x08610235, ADLER_32, "hello ");
    assertHash32(0x0B0D02AC, ADLER_32, "hello w");
    assertHash32(0x0E28031B, ADLER_32, "hello wo");
    assertHash32(0x11B5038D, ADLER_32, "hello wor");
    assertHash32(0x5BDC0FDA, ADLER_32, "The quick brown fox jumps over the lazy dog");
    assertHash32(0x5BD90FD9, ADLER_32, "The quick brown fox jumps over the lazy cog");
  }

  private static void assertChecksum(ImmutableSupplier<Checksum> supplier, String input) {
    byte[] bytes = HashTestUtils.ascii(input);

    Checksum checksum = supplier.get();
    checksum.update(bytes, 0, bytes.length);
    long value = checksum.getValue();

    String toString = "name";
    HashFunction func = new ChecksumHashFunction(supplier, 32, toString);
    assertEquals(toString, func.toString());
    assertEquals(value, func.hashBytes(bytes).padToLong());
  }

  private static void assertHash32(
      int expected, ImmutableSupplier<Checksum> supplier, String input) {
    byte[] bytes = HashTestUtils.ascii(input);
    String toString = "name";
    HashFunction func = new ChecksumHashFunction(supplier, 32, toString);
    assertEquals(expected, func.hashBytes(bytes).asInt());
    assertEquals(toString, func.toString());
  }
}

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

import com.google.common.base.Charsets;

import junit.framework.TestCase;

/**
 * This tests CRC8 module using externally calculated checksums from
 * http://www.zorc.breitbandkatze.de/crc.html
 * with the ATM HEC paramters:
 * CRC-8, Polynomial 0x07, Initial value 0x00, Final XOR value 0x55
 * (direct, don't reverse data bytes, don't reverse CRC before final XOR)
 *
 * @author Nicholas Yu
 */
public class Crc8HashFunctionTest extends TestCase {
  public void testGenerateKnownValues() {
    assertCrc8("Google", (byte) 1);
    assertCrc8("GOOGLE", (byte) -90);
    assertCrc8("My CRC 8!", (byte) -36);
    assertCrc8("Z", (byte) -44);
    assertCrc8("", (byte) 85);
  }

  private static void assertCrc8(String value, byte expectedCrc) {
    assertEquals(expectedCrc, Hashing.crc8().hashString(value, Charsets.US_ASCII).asBytes()[0]);
  }
}

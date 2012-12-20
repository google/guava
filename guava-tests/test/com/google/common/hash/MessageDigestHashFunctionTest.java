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

import com.google.common.collect.ImmutableSet;

import junit.framework.TestCase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Tests for the MessageDigestHashFunction.
 *
 * @author Kurt Alfred Kluever
 */
public class MessageDigestHashFunctionTest extends TestCase {
  private static final ImmutableSet<String> INPUTS = ImmutableSet.of("", "Z", "foobar");
  private static final ImmutableSet<String> ALGORITHMS = ImmutableSet.of(
        "MD5", "SHA1", "SHA-1", "SHA-256", "SHA-512");

  public void testHashing() {
    for (String stringToTest : INPUTS) {
      for (String algorithmToTest : ALGORITHMS) {
        assertMessageDigestHashing(HashTestUtils.ascii(stringToTest), algorithmToTest);
      }
    }
  }

  public void testToString() {
    assertEquals("Hashing.md5()", Hashing.md5().toString());
    assertEquals("Hashing.sha1()", Hashing.sha1().toString());
    assertEquals("Hashing.sha256()", Hashing.sha256().toString());
    assertEquals("Hashing.sha512()", Hashing.sha512().toString());
  }

  private static void assertMessageDigestHashing(byte[] input, String algorithmName) {
    try {
      MessageDigest digest = MessageDigest.getInstance(algorithmName);
      assertEquals(
          HashCodes.fromBytes(digest.digest(input)),
          new MessageDigestHashFunction(algorithmName, algorithmName).hashBytes(input));
      for (int bytes = 4; bytes <= digest.getDigestLength(); bytes++) {
        assertEquals(
            HashCodes.fromBytes(Arrays.copyOf(digest.digest(input), bytes)),
            new MessageDigestHashFunction(algorithmName, bytes, algorithmName).hashBytes(input));
      }
      try {
        int maxSize = digest.getDigestLength();
        new MessageDigestHashFunction(algorithmName, maxSize + 1, algorithmName);
        fail();
      } catch (IllegalArgumentException expected) {
      }
    } catch (NoSuchAlgorithmException nsae) {
      throw new AssertionError(nsae);
    }
  }
}

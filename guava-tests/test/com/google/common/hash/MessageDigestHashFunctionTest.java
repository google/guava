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

import static org.junit.Assert.assertThrows;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import junit.framework.TestCase;

/**
 * Tests for the MessageDigestHashFunction.
 *
 * @author Kurt Alfred Kluever
 */
public class MessageDigestHashFunctionTest extends TestCase {
  private static final ImmutableSet<String> INPUTS = ImmutableSet.of("", "Z", "foobar");

  // From "How Provider Implementations Are Requested and Supplied" from
  // http://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html
  //  - Some providers may choose to also include alias names.
  //  - For example, the "SHA-1" algorithm might be referred to as "SHA1".
  //  - The algorithm name is not case-sensitive.
  private static final ImmutableMap<String, HashFunction> ALGORITHMS =
      new ImmutableMap.Builder<String, HashFunction>()
          .put("MD5", Hashing.md5())
          .put("SHA", Hashing.sha1()) // Not the official name, but still works
          .put("SHA1", Hashing.sha1()) // Not the official name, but still works
          .put("sHa-1", Hashing.sha1()) // Not the official name, but still works
          .put("SHA-1", Hashing.sha1())
          .put("SHA-256", Hashing.sha256())
          .put("SHA-384", Hashing.sha384())
          .put("SHA-512", Hashing.sha512())
          .build();

  public void testHashing() {
    for (String stringToTest : INPUTS) {
      for (String algorithmToTest : ALGORITHMS.keySet()) {
        assertMessageDigestHashing(HashTestUtils.ascii(stringToTest), algorithmToTest);
      }
    }
  }

  public void testPutAfterHash() {
    Hasher sha1 = Hashing.sha1().newHasher();

    assertEquals(
        "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12",
        sha1.putString("The quick brown fox jumps over the lazy dog", Charsets.UTF_8)
            .hash()
            .toString());
    assertThrows(IllegalStateException.class, () -> sha1.putInt(42));
  }

  public void testHashTwice() {
    Hasher sha1 = Hashing.sha1().newHasher();

    assertEquals(
        "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12",
        sha1.putString("The quick brown fox jumps over the lazy dog", Charsets.UTF_8)
            .hash()
            .toString());
    assertThrows(IllegalStateException.class, () -> sha1.hash());
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
          HashCode.fromBytes(digest.digest(input)), ALGORITHMS.get(algorithmName).hashBytes(input));
      for (int bytes = 4; bytes <= digest.getDigestLength(); bytes++) {
        assertEquals(
            HashCode.fromBytes(Arrays.copyOf(digest.digest(input), bytes)),
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

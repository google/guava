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

import static com.google.common.hash.Hashing.sha512;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for the MessageDigestHashFunction.
 *
 * @author Kurt Alfred Kluever
 */
@NullUnmarked
public class MessageDigestHashFunctionTest extends TestCase {
  private static final ImmutableSet<String> INPUTS = ImmutableSet.of("", "Z", "foobar");

  public void testHashing() throws Exception {
    for (String stringToTest : INPUTS) {
      for (String algorithmToTest : TestPlatform.getAlgorithms().keySet()) {
        assertMessageDigestHashing(HashTestUtils.ascii(stringToTest), algorithmToTest);
      }
    }
  }

  @J2ktIncompatible
  public void testPutAfterHash() {
    Hasher hasher = sha512().newHasher();

    assertThat(
            hasher
                .putString("The quick brown fox jumps over the lazy dog", UTF_8)
                .hash()
                .toString())
        .isEqualTo(
            "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb642e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6");
    assertThrows(IllegalStateException.class, () -> hasher.putInt(42));
  }

  @J2ktIncompatible
  public void testHashTwice() {
    Hasher hasher = sha512().newHasher();

    assertThat(
            hasher
                .putString("The quick brown fox jumps over the lazy dog", UTF_8)
                .hash()
                .toString())
        .isEqualTo(
            "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb642e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6");
    assertThrows(IllegalStateException.class, hasher::hash);
  }

  @SuppressWarnings("deprecation") // We still need to test our deprecated APIs.
  public void testToString() {
    ImmutableMap<String, HashFunction> algorithms = TestPlatform.getAlgorithms();
    if (algorithms.containsKey("MD5")) {
      assertThat(algorithms.get("MD5").toString()).isEqualTo("Hashing.md5()");
    }
    if (algorithms.containsKey("SHA-1")) {
      assertThat(algorithms.get("SHA-1").toString()).isEqualTo("Hashing.sha1()");
    }
    if (algorithms.containsKey("SHA-256")) {
      assertThat(algorithms.get("SHA-256").toString()).isEqualTo("Hashing.sha256()");
    }
    if (algorithms.containsKey("SHA-512")) {
      assertThat(algorithms.get("SHA-512").toString()).isEqualTo("Hashing.sha512()");
    }
  }

  private static void assertMessageDigestHashing(byte[] input, String algorithmName)
      throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance(algorithmName);
    assertEquals(
        HashCode.fromBytes(digest.digest(input)),
        TestPlatform.getAlgorithms().get(algorithmName).hashBytes(input));
    for (int bytes = 4; bytes <= digest.getDigestLength(); bytes++) {
      assertEquals(
          HashCode.fromBytes(Arrays.copyOf(digest.digest(input), bytes)),
          new MessageDigestHashFunction(algorithmName, bytes, algorithmName).hashBytes(input));
    }
    int maxSize = digest.getDigestLength();
    assertThrows(
        IllegalArgumentException.class,
        () -> new MessageDigestHashFunction(algorithmName, maxSize + 1, algorithmName));
  }
}

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

import junit.framework.TestCase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Tests for the MessageDigestHashFunction.
 *
 * @author Kurt Alfred Kluever
 */
public class MessageDigestHashFunctionTest extends TestCase {
  public void testMd5Hashing() throws Exception {
    assertMessageDigestHashing(HashTestUtils.ascii(""), "MD5");
    assertMessageDigestHashing(HashTestUtils.ascii("Z"), "MD5");
    assertMessageDigestHashing(HashTestUtils.ascii("foobar"), "MD5");
  }

  public void testSha1Hashing() throws Exception {
    assertMessageDigestHashing(HashTestUtils.ascii(""), "SHA1");
    assertMessageDigestHashing(HashTestUtils.ascii("Z"), "SHA1");
    assertMessageDigestHashing(HashTestUtils.ascii("foobar"), "SHA1");
  }

  private static void assertMessageDigestHashing(byte[] input, String algorithmName)
      throws NoSuchAlgorithmException {
    assertEquals(
        HashCodes.fromBytes(MessageDigest.getInstance(algorithmName).digest(input)),
        new MessageDigestHashFunction(algorithmName).hashBytes(input));
  }
}

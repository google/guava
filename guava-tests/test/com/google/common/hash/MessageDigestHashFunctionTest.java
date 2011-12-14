// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import junit.framework.TestCase;

/**
 * Tests for the MessageDigestHashFunction.
 *
 * @author kak@google.com (Kurt Alfred Kluever)
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
    HashTestUtils.assertEqualHashes(
        MessageDigest.getInstance(algorithmName).digest(input),
        new MessageDigestHashFunction(algorithmName).hashBytes(input).asBytes());
  }
}

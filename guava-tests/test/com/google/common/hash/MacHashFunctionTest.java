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

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.BaseEncoding.base16;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.testing.NullPointerTester;
import java.security.Key;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;
import sun.security.jca.ProviderList;
import sun.security.jca.Providers;

/**
 * Tests for the MacHashFunction.
 *
 * @author Kurt Alfred Kluever
 */
public class MacHashFunctionTest extends TestCase {

  private static final ImmutableSet<String> INPUTS = ImmutableSet.of("", "Z", "foobar");

  private static final SecretKey MD5_KEY =
      new SecretKeySpec("secret key".getBytes(UTF_8), "HmacMD5");
  private static final SecretKey SHA1_KEY =
      new SecretKeySpec("secret key".getBytes(UTF_8), "HmacSHA1");
  private static final SecretKey SHA256_KEY =
      new SecretKeySpec("secret key".getBytes(UTF_8), "HmacSHA256");
  private static final SecretKey SHA512_KEY =
      new SecretKeySpec("secret key".getBytes(UTF_8), "HmacSHA512");

  // From http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Mac
  private static final ImmutableTable<String, SecretKey, HashFunction> ALGORITHMS =
      new ImmutableTable.Builder<String, SecretKey, HashFunction>()
          .put("HmacMD5", MD5_KEY, Hashing.hmacMd5(MD5_KEY))
          .put("HmacSHA1", SHA1_KEY, Hashing.hmacSha1(SHA1_KEY))
          .put("HmacSHA256", SHA256_KEY, Hashing.hmacSha256(SHA256_KEY))
          .put("HmacSHA512", SHA512_KEY, Hashing.hmacSha512(SHA512_KEY))
          .build();

  public void testNulls() {
    NullPointerTester tester =
        new NullPointerTester().setDefault(String.class, "HmacMD5").setDefault(Key.class, MD5_KEY);
    tester.testAllPublicConstructors(MacHashFunction.class);
    tester.testAllPublicInstanceMethods(new MacHashFunction("HmacMD5", MD5_KEY, "toString"));
  }

  public void testHashing() throws Exception {
    for (String stringToTest : INPUTS) {
      for (Table.Cell<String, SecretKey, HashFunction> cell : ALGORITHMS.cellSet()) {
        String algorithm = cell.getRowKey();
        SecretKey key = cell.getColumnKey();
        HashFunction hashFunc = cell.getValue();
        assertMacHashing(HashTestUtils.ascii(stringToTest), algorithm, key, hashFunc);
      }
    }
  }

  @AndroidIncompatible // sun.security
  public void testNoProviders() {
    ProviderList providers = Providers.getProviderList();
    Providers.setProviderList(ProviderList.newList());
    try {
      Hashing.hmacMd5(MD5_KEY);
      fail("expected ISE");
    } catch (IllegalStateException expected) {
    } finally {
      Providers.setProviderList(providers);
    }
  }

  public void testMultipleUpdates() throws Exception {
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(SHA1_KEY);
    mac.update("hello".getBytes(UTF_8));
    mac.update("world".getBytes(UTF_8));

    assertEquals(
        HashCode.fromBytes(mac.doFinal()),
        Hashing.hmacSha1(SHA1_KEY)
            .newHasher()
            .putString("hello", UTF_8)
            .putString("world", UTF_8)
            .hash());
  }

  public void testMultipleUpdatesDoFinal() throws Exception {
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(SHA1_KEY);
    mac.update("hello".getBytes(UTF_8));
    mac.update("world".getBytes(UTF_8));

    assertEquals(
        HashCode.fromBytes(mac.doFinal("!!!".getBytes(UTF_8))),
        Hashing.hmacSha1(SHA1_KEY)
            .newHasher()
            .putString("hello", UTF_8)
            .putString("world", UTF_8)
            .putString("!!!", UTF_8)
            .hash());
  }

  public void testCustomKey() throws Exception {
    SecretKey customKey =
        new SecretKey() {
          @Override
          public String getAlgorithm() {
            return "HmacMD5";
          }

          @Override
          public byte[] getEncoded() {
            return new byte[8];
          }

          @Override
          public String getFormat() {
            return "RAW";
          }
        };
    assertEquals(
        "ad262969c53bc16032f160081c4a07a0",
        Hashing.hmacMd5(customKey)
            .hashString("The quick brown fox jumps over the lazy dog", UTF_8)
            .toString());
  }

  public void testBadKey_emptyKey() throws Exception {
    SecretKey badKey =
        new SecretKey() {
          @Override
          public String getAlgorithm() {
            return "HmacMD5";
          }

          @Override
          public byte @Nullable [] getEncoded() {
            return null;
          }

          @Override
          public String getFormat() {
            return "RAW";
          }
        };
    try {
      Hashing.hmacMd5(badKey);
      fail();
    } catch (IllegalArgumentException expected) {
    } catch (NullPointerException toleratedOnAndroid) {
      // TODO(cpovirk): In an ideal world, we'd check here that we're running on Android.
    }
  }

  public void testEmptyInputs() throws Exception {
    String knownOutput = "8cbf764cbe2e4623d99a41354adfd390";

    Mac mac = Mac.getInstance("HmacMD5");
    mac.init(MD5_KEY);
    assertEquals(knownOutput, HashCode.fromBytes(mac.doFinal()).toString());
    assertEquals(knownOutput, Hashing.hmacMd5(MD5_KEY).newHasher().hash().toString());
  }

  public void testEmptyInputs_mixedAlgorithms() throws Exception {
    String knownOutput = "8cbf764cbe2e4623d99a41354adfd390";

    Mac mac = Mac.getInstance("HmacMD5");
    mac.init(SHA1_KEY);
    assertEquals(knownOutput, HashCode.fromBytes(mac.doFinal()).toString());
    assertEquals(knownOutput, Hashing.hmacMd5(SHA1_KEY).newHasher().hash().toString());
  }

  public void testKnownInputs() throws Exception {
    String knownOutput = "9753980fe94daa8ecaa82216519393a9";
    String input = "The quick brown fox jumps over the lazy dog";

    Mac mac = Mac.getInstance("HmacMD5");
    mac.init(MD5_KEY);
    mac.update(input.getBytes(UTF_8));
    assertEquals(knownOutput, HashCode.fromBytes(mac.doFinal()).toString());
    assertEquals(knownOutput, HashCode.fromBytes(mac.doFinal(input.getBytes(UTF_8))).toString());
    assertEquals(knownOutput, Hashing.hmacMd5(MD5_KEY).hashString(input, UTF_8).toString());
    assertEquals(knownOutput, Hashing.hmacMd5(MD5_KEY).hashBytes(input.getBytes(UTF_8)).toString());
  }

  public void testKnownInputs_mixedAlgorithms() throws Exception {
    String knownOutput = "9753980fe94daa8ecaa82216519393a9";
    String input = "The quick brown fox jumps over the lazy dog";

    Mac mac = Mac.getInstance("HmacMD5");
    mac.init(SHA1_KEY);
    mac.update(input.getBytes(UTF_8));
    assertEquals(knownOutput, HashCode.fromBytes(mac.doFinal()).toString());
    assertEquals(knownOutput, HashCode.fromBytes(mac.doFinal(input.getBytes(UTF_8))).toString());
    assertEquals(knownOutput, Hashing.hmacMd5(SHA1_KEY).hashString(input, UTF_8).toString());
    assertEquals(
        knownOutput, Hashing.hmacMd5(SHA1_KEY).hashBytes(input.getBytes(UTF_8)).toString());
  }

  public void testPutAfterHash() {
    Hasher hasher = Hashing.hmacMd5(MD5_KEY).newHasher();

    assertEquals(
        "9753980fe94daa8ecaa82216519393a9",
        hasher.putString("The quick brown fox jumps over the lazy dog", UTF_8).hash().toString());
    assertThrows(IllegalStateException.class, () -> hasher.putInt(42));
  }

  public void testHashTwice() {
    Hasher hasher = Hashing.hmacMd5(MD5_KEY).newHasher();

    assertEquals(
        "9753980fe94daa8ecaa82216519393a9",
        hasher.putString("The quick brown fox jumps over the lazy dog", UTF_8).hash().toString());
    assertThrows(IllegalStateException.class, () -> hasher.hash());
  }

  public void testToString() {
    byte[] keyData = "secret key".getBytes(UTF_8);

    assertEquals(
        "Hashing.hmacMd5(Key[algorithm=HmacMD5, format=RAW])", Hashing.hmacMd5(MD5_KEY).toString());
    assertEquals(
        "Hashing.hmacMd5(Key[algorithm=HmacMD5, format=RAW])", Hashing.hmacMd5(keyData).toString());

    assertEquals(
        "Hashing.hmacSha1(Key[algorithm=HmacSHA1, format=RAW])",
        Hashing.hmacSha1(SHA1_KEY).toString());
    assertEquals(
        "Hashing.hmacSha1(Key[algorithm=HmacSHA1, format=RAW])",
        Hashing.hmacSha1(keyData).toString());

    assertEquals(
        "Hashing.hmacSha256(Key[algorithm=HmacSHA256, format=RAW])",
        Hashing.hmacSha256(SHA256_KEY).toString());
    assertEquals(
        "Hashing.hmacSha256(Key[algorithm=HmacSHA256, format=RAW])",
        Hashing.hmacSha256(keyData).toString());

    assertEquals(
        "Hashing.hmacSha512(Key[algorithm=HmacSHA512, format=RAW])",
        Hashing.hmacSha512(SHA512_KEY).toString());
    assertEquals(
        "Hashing.hmacSha512(Key[algorithm=HmacSHA512, format=RAW])",
        Hashing.hmacSha512(keyData).toString());
  }

  private static void assertMacHashing(
      byte[] input, String algorithm, SecretKey key, HashFunction hashFunc) throws Exception {
    Mac mac = Mac.getInstance(algorithm);
    mac.init(key);
    mac.update(input);

    assertEquals(HashCode.fromBytes(mac.doFinal()), hashFunc.hashBytes(input));
    assertEquals(HashCode.fromBytes(mac.doFinal(input)), hashFunc.hashBytes(input));
  }

  // Tests from RFC2022: https://tools.ietf.org/html/rfc2202

  public void testRfc2202_hmacSha1_case1() {
    byte[] key = fillByteArray(20, 0x0b);
    String data = "Hi There";

    checkSha1("b617318655057264e28bc0b6fb378c8ef146be00", key, data);
  }

  public void testRfc2202_hmacSha1_case2() {
    byte[] key = "Jefe".getBytes(UTF_8);
    String data = "what do ya want for nothing?";

    checkSha1("effcdf6ae5eb2fa2d27416d5f184df9c259a7c79", key, data);
  }

  public void testRfc2202_hmacSha1_case3() {
    byte[] key = fillByteArray(20, 0xaa);
    byte[] data = fillByteArray(50, 0xdd);

    checkSha1("125d7342b9ac11cd91a39af48aa17b4f63f175d3", key, data);
  }

  public void testRfc2202_hmacSha1_case4() {
    byte[] key = base16().lowerCase().decode("0102030405060708090a0b0c0d0e0f10111213141516171819");
    byte[] data = fillByteArray(50, 0xcd);

    checkSha1("4c9007f4026250c6bc8414f9bf50c86c2d7235da", key, data);
  }

  public void testRfc2202_hmacSha1_case5() {
    byte[] key = fillByteArray(20, 0x0c);
    String data = "Test With Truncation";

    checkSha1("4c1a03424b55e07fe7f27be1d58bb9324a9a5a04", key, data);
  }

  public void testRfc2202_hmacSha1_case6() {
    byte[] key = fillByteArray(80, 0xaa);
    String data = "Test Using Larger Than Block-Size Key - Hash Key First";

    checkSha1("aa4ae5e15272d00e95705637ce8a3b55ed402112", key, data);
  }

  public void testRfc2202_hmacSha1_case7() {
    byte[] key = fillByteArray(80, 0xaa);
    String data = "Test Using Larger Than Block-Size Key and Larger Than One Block-Size Data";

    checkSha1("e8e99d0f45237d786d6bbaa7965c7808bbff1a91", key, data);
  }

  public void testRfc2202_hmacMd5_case1() {
    byte[] key = fillByteArray(16, 0x0b);
    String data = "Hi There";

    checkMd5("9294727a3638bb1c13f48ef8158bfc9d", key, data);
  }

  public void testRfc2202_hmacMd5_case2() {
    byte[] key = "Jefe".getBytes(UTF_8);
    String data = "what do ya want for nothing?";

    checkMd5("750c783e6ab0b503eaa86e310a5db738", key, data);
  }

  public void testRfc2202_hmacMd5_case3() {
    byte[] key = fillByteArray(16, 0xaa);
    byte[] data = fillByteArray(50, 0xdd);

    checkMd5("56be34521d144c88dbb8c733f0e8b3f6", key, data);
  }

  public void testRfc2202_hmacMd5_case4() {
    byte[] key = base16().lowerCase().decode("0102030405060708090a0b0c0d0e0f10111213141516171819");
    byte[] data = fillByteArray(50, 0xcd);

    checkMd5("697eaf0aca3a3aea3a75164746ffaa79", key, data);
  }

  public void testRfc2202_hmacMd5_case5() {
    byte[] key = fillByteArray(16, 0x0c);
    String data = "Test With Truncation";

    checkMd5("56461ef2342edc00f9bab995690efd4c", key, data);
  }

  public void testRfc2202_hmacMd5_case6() {
    byte[] key = fillByteArray(80, 0xaa);
    String data = "Test Using Larger Than Block-Size Key - Hash Key First";

    checkMd5("6b1ab7fe4bd7bf8f0b62e6ce61b9d0cd", key, data);
  }

  public void testRfc2202_hmacMd5_case7() {
    byte[] key = fillByteArray(80, 0xaa);
    String data = "Test Using Larger Than Block-Size Key and Larger Than One Block-Size Data";

    checkMd5("6f630fad67cda0ee1fb1f562db3aa53e", key, data);
  }

  private static void checkSha1(String expected, byte[] key, String data) {
    checkSha1(expected, key, data.getBytes(UTF_8));
  }

  private static void checkSha1(String expected, byte[] key, byte[] data) {
    checkHmac(expected, Hashing.hmacSha1(key), data);
  }

  private static void checkMd5(String expected, byte[] key, String data) {
    checkMd5(expected, key, data.getBytes(UTF_8));
  }

  private static void checkMd5(String expected, byte[] key, byte[] data) {
    checkHmac(expected, Hashing.hmacMd5(key), data);
  }

  private static void checkHmac(String expected, HashFunction hashFunc, byte[] data) {
    assertEquals(HashCode.fromString(expected), hashFunc.hashBytes(data));
  }

  private static byte[] fillByteArray(int size, int toFillWith) {
    byte[] array = new byte[size];
    Arrays.fill(array, (byte) toFillWith);
    return array;
  }
}

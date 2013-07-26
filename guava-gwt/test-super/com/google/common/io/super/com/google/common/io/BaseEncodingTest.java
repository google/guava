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

package com.google.common.io;

import static com.google.common.io.BaseEncoding.base16;
import static com.google.common.io.BaseEncoding.base32;
import static com.google.common.io.BaseEncoding.base32Hex;
import static com.google.common.io.BaseEncoding.base64;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding.DecodingException;

import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;

/**
 * Tests for {@code BaseEncoding}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class BaseEncodingTest extends TestCase {
  public static void assertEquals(byte[] expected, byte[] actual) {
    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], actual[i]);
    }
  }

  public void testSeparatorsExplicitly() {
    testEncodes(base64().withSeparator("\n", 3), "foobar", "Zm9\nvYm\nFy");
    testEncodes(base64().withSeparator("$", 4), "foobar", "Zm9v$YmFy");
    testEncodes(base32().withSeparator("*", 4), "foobar", "MZXW*6YTB*OI==*====");
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void testSeparatorSameAsPadChar() {
    try {
      base64().withSeparator("=", 3);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {}

    try {
      base64().withPadChar('#').withSeparator("!#!", 3);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {}
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void testAtMostOneSeparator() {
    BaseEncoding separated = base64().withSeparator("\n", 3);
    try {
      separated.withSeparator("$", 4);
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {}
  }

  public void testBase64() {
    // The following test vectors are specified in RFC 4648 itself
    testEncodingWithSeparators(base64(), "", "");
    testEncodingWithSeparators(base64(), "f", "Zg==");
    testEncodingWithSeparators(base64(), "fo", "Zm8=");
    testEncodingWithSeparators(base64(), "foo", "Zm9v");
    testEncodingWithSeparators(base64(), "foob", "Zm9vYg==");
    testEncodingWithSeparators(base64(), "fooba", "Zm9vYmE=");
    testEncodingWithSeparators(base64(), "foobar", "Zm9vYmFy");
  }

  public void testBase64LenientPadding() {
    testDecodes(base64(), "Zg", "f");
    testDecodes(base64(), "Zg=", "f");
    testDecodes(base64(), "Zg==", "f"); // proper padding length
    testDecodes(base64(), "Zg===", "f");
    testDecodes(base64(), "Zg====", "f");
  }

  public void testBase64InvalidDecodings() {
    // These contain bytes not in the decodabet.
    assertFailsToDecode(base64(), "\u007f");
    assertFailsToDecode(base64(), "Wf2!");
    // This sentence just isn't base64() encoded.
    assertFailsToDecode(base64(), "let's not talk of love or chains!");
    // A 4n+1 length string is never legal base64().
    assertFailsToDecode(base64(), "12345");
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void testBase64CannotUpperCase() {
    try {
      base64().upperCase();
      fail();
    } catch (IllegalStateException expected) {
      // success
    }
  }

  @SuppressWarnings("ReturnValueIgnored")
  public void testBase64CannotLowerCase() {
    try {
      base64().lowerCase();
      fail();
    } catch (IllegalStateException expected) {
      // success
    }
  }

  public void testBase64AlternatePadding() {
    BaseEncoding enc = base64().withPadChar('~');
    testEncodingWithSeparators(enc, "", "");
    testEncodingWithSeparators(enc, "f", "Zg~~");
    testEncodingWithSeparators(enc, "fo", "Zm8~");
    testEncodingWithSeparators(enc, "foo", "Zm9v");
    testEncodingWithSeparators(enc, "foob", "Zm9vYg~~");
    testEncodingWithSeparators(enc, "fooba", "Zm9vYmE~");
    testEncodingWithSeparators(enc, "foobar", "Zm9vYmFy");
  }

  public void testBase64OmitPadding() {
    BaseEncoding enc = base64().omitPadding();
    testEncodingWithSeparators(enc, "", "");
    testEncodingWithSeparators(enc, "f", "Zg");
    testEncodingWithSeparators(enc, "fo", "Zm8");
    testEncodingWithSeparators(enc, "foo", "Zm9v");
    testEncodingWithSeparators(enc, "foob", "Zm9vYg");
    testEncodingWithSeparators(enc, "fooba", "Zm9vYmE");
    testEncodingWithSeparators(enc, "foobar", "Zm9vYmFy");
  }

  public void testBase32() {
    // The following test vectors are specified in RFC 4648 itself
    testEncodingWithCasing(base32(), "", "");
    testEncodingWithCasing(base32(), "f", "MY======");
    testEncodingWithCasing(base32(), "fo", "MZXQ====");
    testEncodingWithCasing(base32(), "foo", "MZXW6===");
    testEncodingWithCasing(base32(), "foob", "MZXW6YQ=");
    testEncodingWithCasing(base32(), "fooba", "MZXW6YTB");
    testEncodingWithCasing(base32(), "foobar", "MZXW6YTBOI======");
  }

  public void testBase32LenientPadding() {
    testDecodes(base32(), "MZXW6", "foo");
    testDecodes(base32(), "MZXW6=", "foo");
    testDecodes(base32(), "MZXW6==", "foo");
    testDecodes(base32(), "MZXW6===", "foo"); // proper padding length
    testDecodes(base32(), "MZXW6====", "foo");
    testDecodes(base32(), "MZXW6=====", "foo");
  }

  public void testBase32AlternatePadding() {
    BaseEncoding enc = base32().withPadChar('~');
    testEncodingWithCasing(enc, "", "");
    testEncodingWithCasing(enc, "f", "MY~~~~~~");
    testEncodingWithCasing(enc, "fo", "MZXQ~~~~");
    testEncodingWithCasing(enc, "foo", "MZXW6~~~");
    testEncodingWithCasing(enc, "foob", "MZXW6YQ~");
    testEncodingWithCasing(enc, "fooba", "MZXW6YTB");
    testEncodingWithCasing(enc, "foobar", "MZXW6YTBOI~~~~~~");
  }

  public void testBase32InvalidDecodings() {
    // These contain bytes not in the decodabet.
    assertFailsToDecode(base32(), "\u007f");
    assertFailsToDecode(base32(), "Wf2!");
    // This sentence just isn't base32() encoded.
    assertFailsToDecode(base32(), "let's not talk of love or chains!");
    // An 8n+{1,3,6} length string is never legal base32.
    assertFailsToDecode(base32(), "A");
    assertFailsToDecode(base32(), "ABC");
    assertFailsToDecode(base32(), "ABCDEF");
  }

  public void testBase32UpperCaseIsNoOp() {
    assertSame(base32(), base32().upperCase());
  }

  public void testBase32Hex() {
    // The following test vectors are specified in RFC 4648 itself
    testEncodingWithCasing(base32Hex(), "", "");
    testEncodingWithCasing(base32Hex(), "f", "CO======");
    testEncodingWithCasing(base32Hex(), "fo", "CPNG====");
    testEncodingWithCasing(base32Hex(), "foo", "CPNMU===");
    testEncodingWithCasing(base32Hex(), "foob", "CPNMUOG=");
    testEncodingWithCasing(base32Hex(), "fooba", "CPNMUOJ1");
    testEncodingWithCasing(base32Hex(), "foobar", "CPNMUOJ1E8======");
  }

  public void testBase32HexLenientPadding() {
    testDecodes(base32Hex(), "CPNMU", "foo");
    testDecodes(base32Hex(), "CPNMU=", "foo");
    testDecodes(base32Hex(), "CPNMU==", "foo");
    testDecodes(base32Hex(), "CPNMU===", "foo"); // proper padding length
    testDecodes(base32Hex(), "CPNMU====", "foo");
    testDecodes(base32Hex(), "CPNMU=====", "foo");
  }

  public void testBase32HexInvalidDecodings() {
    // These contain bytes not in the decodabet.
    assertFailsToDecode(base32Hex(), "\u007f");
    assertFailsToDecode(base32Hex(), "Wf2!");
    // This sentence just isn't base32 encoded.
    assertFailsToDecode(base32Hex(), "let's not talk of love or chains!");
    // An 8n+{1,3,6} length string is never legal base32.
    assertFailsToDecode(base32Hex(), "A");
    assertFailsToDecode(base32Hex(), "ABC");
    assertFailsToDecode(base32Hex(), "ABCDEF");
  }

  public void testBase32HexUpperCaseIsNoOp() {
    assertSame(base32Hex(), base32Hex().upperCase());
  }

  public void testBase16() {
    testEncodingWithCasing(base16(), "", "");
    testEncodingWithCasing(base16(), "f", "66");
    testEncodingWithCasing(base16(), "fo", "666F");
    testEncodingWithCasing(base16(), "foo", "666F6F");
    testEncodingWithCasing(base16(), "foob", "666F6F62");
    testEncodingWithCasing(base16(), "fooba", "666F6F6261");
    testEncodingWithCasing(base16(), "foobar", "666F6F626172");
  }

  public void testBase16UpperCaseIsNoOp() {
    assertSame(base16(), base16().upperCase());
  }

  private static void testEncodingWithCasing(
      BaseEncoding encoding, String decoded, String encoded) {
    testEncodingWithSeparators(encoding, decoded, encoded);
    testEncodingWithSeparators(encoding.upperCase(), decoded, Ascii.toUpperCase(encoded));
    testEncodingWithSeparators(encoding.lowerCase(), decoded, Ascii.toLowerCase(encoded));
  }

  private static void testEncodingWithSeparators(
      BaseEncoding encoding, String decoded, String encoded) {
    testEncoding(encoding, decoded, encoded);

    // test separators work
    for (int sepLength = 3; sepLength <= 5; sepLength++) {
      for (String separator : ImmutableList.of(",", "\n", ";;", "")) {
        testEncoding(encoding.withSeparator(separator, sepLength), decoded,
            Joiner.on(separator).join(Splitter.fixedLength(sepLength).split(encoded)));
      }
    }
  }

  private static void testEncoding(BaseEncoding encoding, String decoded, String encoded) {
    testEncodes(encoding, decoded, encoded);
    testDecodes(encoding, encoded, decoded);
  }

  private static void testEncodes(BaseEncoding encoding, String decoded, String encoded) {
    byte[] bytes;
    try {
      // GWT does not support String.getBytes(Charset)
      bytes = decoded.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError();
    }
    assertEquals(encoded, encoding.encode(bytes));
  }

  private static void testDecodes(BaseEncoding encoding, String encoded, String decoded) {
    byte[] bytes;
    try {
      // GWT does not support String.getBytes(Charset)
      bytes = decoded.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError();
    }
    assertEquals(bytes, encoding.decode(encoded));
  }

  private static void assertFailsToDecode(BaseEncoding encoding, String cannotDecode) {
    try {
      encoding.decode(cannotDecode);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // success
    }
    try {
      encoding.decodeChecked(cannotDecode);
      fail("Expected DecodingException");
    } catch (DecodingException expected) {
      // success
    }
  }

  public void testToString() {
    assertEquals("BaseEncoding.base64().withPadChar(=)", BaseEncoding.base64().toString());
    assertEquals("BaseEncoding.base32Hex().omitPadding()",
        BaseEncoding.base32Hex().omitPadding().toString());
    assertEquals("BaseEncoding.base32().lowerCase().withPadChar($)",
        BaseEncoding.base32().lowerCase().withPadChar('$').toString());
    assertEquals("BaseEncoding.base16().withSeparator(\"\n\", 10)",
        BaseEncoding.base16().withSeparator("\n", 10).toString());
  }
}


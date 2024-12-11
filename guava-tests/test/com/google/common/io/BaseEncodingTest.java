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
import static com.google.common.io.BaseEncoding.base64Url;
import static com.google.common.io.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding.DecodingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@code BaseEncoding}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class BaseEncodingTest extends TestCase {

  public void testSeparatorsExplicitly() {
    testEncodes(base64().withSeparator("\n", 3), "foobar", "Zm9\nvYm\nFy");
    testEncodes(base64().withSeparator("$", 4), "foobar", "Zm9v$YmFy");
    testEncodes(base32().withSeparator("*", 4), "foobar", "MZXW*6YTB*OI==*====");
  }

  public void testSeparatorSameAsPadChar() {
    assertThrows(IllegalArgumentException.class, () -> base64().withSeparator("=", 3));

    assertThrows(
        IllegalArgumentException.class, () -> base64().withPadChar('#').withSeparator("!#!", 3));
  }

  public void testAtMostOneSeparator() {
    BaseEncoding separated = base64().withSeparator("\n", 3);
    assertThrows(UnsupportedOperationException.class, () -> separated.withSeparator("$", 4));
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

  @GwtIncompatible // Reader/Writer
  public void testBase64Streaming() throws IOException {
    // The following test vectors are specified in RFC 4648 itself
    testStreamingEncodingWithSeparators(base64(), "", "");
    testStreamingEncodingWithSeparators(base64(), "f", "Zg==");
    testStreamingEncodingWithSeparators(base64(), "fo", "Zm8=");
    testStreamingEncodingWithSeparators(base64(), "foo", "Zm9v");
    testStreamingEncodingWithSeparators(base64(), "foob", "Zm9vYg==");
    testStreamingEncodingWithSeparators(base64(), "fooba", "Zm9vYmE=");
    testStreamingEncodingWithSeparators(base64(), "foobar", "Zm9vYmFy");
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
    assertFailsToDecode(base64(), "A\u007f", "Unrecognized character: 0x7f");
    assertFailsToDecode(base64(), "Wf2!", "Unrecognized character: !");
    // This sentence just isn't base64() encoded.
    assertFailsToDecode(base64(), "let's not talk of love or chains!");
    // A 4n+1 length string is never legal base64().
    assertFailsToDecode(base64(), "12345", "Invalid input length 5");
    // These have a combination of invalid length, unrecognized characters and wrong padding.
    assertFailsToDecode(base64(), "AB=C", "Unrecognized character: =");
    assertFailsToDecode(base64(), "A=BCD", "Invalid input length 5");
    assertFailsToDecode(base64(), "?", "Invalid input length 1");
  }

  public void testBase64CannotUpperCase() {
    assertThrows(IllegalStateException.class, () -> base64().upperCase());
  }

  public void testBase64CannotLowerCase() {
    assertThrows(IllegalStateException.class, () -> base64().lowerCase());
  }

  public void testBase64CannotIgnoreCase() {
    assertThrows(IllegalStateException.class, () -> base64().ignoreCase());
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

  @GwtIncompatible // Reader/Writer
  public void testBase64StreamingAlternatePadding() throws IOException {
    BaseEncoding enc = base64().withPadChar('~');
    testStreamingEncodingWithSeparators(enc, "", "");
    testStreamingEncodingWithSeparators(enc, "f", "Zg~~");
    testStreamingEncodingWithSeparators(enc, "fo", "Zm8~");
    testStreamingEncodingWithSeparators(enc, "foo", "Zm9v");
    testStreamingEncodingWithSeparators(enc, "foob", "Zm9vYg~~");
    testStreamingEncodingWithSeparators(enc, "fooba", "Zm9vYmE~");
    testStreamingEncodingWithSeparators(enc, "foobar", "Zm9vYmFy");
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

  @GwtIncompatible // Reader/Writer
  public void testBase64StreamingOmitPadding() throws IOException {
    BaseEncoding enc = base64().omitPadding();
    testStreamingEncodingWithSeparators(enc, "", "");
    testStreamingEncodingWithSeparators(enc, "f", "Zg");
    testStreamingEncodingWithSeparators(enc, "fo", "Zm8");
    testStreamingEncodingWithSeparators(enc, "foo", "Zm9v");
    testStreamingEncodingWithSeparators(enc, "foob", "Zm9vYg");
    testStreamingEncodingWithSeparators(enc, "fooba", "Zm9vYmE");
    testStreamingEncodingWithSeparators(enc, "foobar", "Zm9vYmFy");
  }

  public void testBase64Offset() {
    testEncodesWithOffset(base64(), "foobar", 0, 6, "Zm9vYmFy");
    testEncodesWithOffset(base64(), "foobar", 1, 5, "b29iYXI=");
    testEncodesWithOffset(base64(), "foobar", 2, 3, "b2Jh");
    testEncodesWithOffset(base64(), "foobar", 3, 1, "Yg==");
    testEncodesWithOffset(base64(), "foobar", 4, 0, "");
  }

  public void testBase64Url() {
    testDecodesByBytes(base64Url(), "_zzz", new byte[] {-1, 60, -13});
    testDecodesByBytes(base64Url(), "-zzz", new byte[] {-5, 60, -13});
  }

  public void testBase64UrlInvalidDecodings() {
    assertFailsToDecode(base64Url(), "+zzz", "Unrecognized character: +");
    assertFailsToDecode(base64Url(), "/zzz", "Unrecognized character: /");
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

  @GwtIncompatible // Reader/Writer
  public void testBase32Streaming() throws IOException {
    // The following test vectors are specified in RFC 4648 itself
    testStreamingEncodingWithCasing(base32(), "", "");
    testStreamingEncodingWithCasing(base32(), "f", "MY======");
    testStreamingEncodingWithCasing(base32(), "fo", "MZXQ====");
    testStreamingEncodingWithCasing(base32(), "foo", "MZXW6===");
    testStreamingEncodingWithCasing(base32(), "foob", "MZXW6YQ=");
    testStreamingEncodingWithCasing(base32(), "fooba", "MZXW6YTB");
    testStreamingEncodingWithCasing(base32(), "foobar", "MZXW6YTBOI======");
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
    assertFailsToDecode(base32(), "A ", "Unrecognized character: 0x20");
    assertFailsToDecode(base32(), "Wf2!", "Unrecognized character: f");
    // This sentence just isn't base32() encoded.
    assertFailsToDecode(base32(), "let's not talk of love or chains!");
    // An 8n+{1,3,6} length string is never legal base32.
    assertFailsToDecode(base32(), "A", "Invalid input length 1");
    assertFailsToDecode(base32(), "ABC");
    assertFailsToDecode(base32(), "ABCDEF");
    // These have a combination of invalid length, unrecognized characters and wrong padding.
    assertFailsToDecode(base32(), "AB=C", "Unrecognized character: =");
    assertFailsToDecode(base32(), "A=BCDE", "Invalid input length 6");
    assertFailsToDecode(base32(), "?", "Invalid input length 1");
  }

  public void testBase32UpperCaseIsNoOp() {
    assertThat(base32().upperCase()).isSameInstanceAs(base32());
  }

  public void testBase32LowerCase() {
    testEncodingWithCasing(base32().lowerCase(), "foobar", "mzxw6ytboi======");
  }

  public void testBase32IgnoreCase() {
    BaseEncoding ignoreCase = base32().ignoreCase();
    assertThat(ignoreCase).isNotSameInstanceAs(base32());
    assertThat(ignoreCase).isSameInstanceAs(base32().ignoreCase());
    testDecodes(ignoreCase, "MZXW6YTBOI======", "foobar");
    testDecodes(ignoreCase, "mzxw6ytboi======", "foobar");
  }

  public void testBase32Offset() {
    testEncodesWithOffset(base32(), "foobar", 0, 6, "MZXW6YTBOI======");
    testEncodesWithOffset(base32(), "foobar", 1, 5, "N5XWEYLS");
    testEncodesWithOffset(base32(), "foobar", 2, 3, "N5RGC===");
    testEncodesWithOffset(base32(), "foobar", 3, 1, "MI======");
    testEncodesWithOffset(base32(), "foobar", 4, 0, "");
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

  @GwtIncompatible // Reader/Writer
  public void testBase32HexStreaming() throws IOException {
    // The following test vectors are specified in RFC 4648 itself
    testStreamingEncodingWithCasing(base32Hex(), "", "");
    testStreamingEncodingWithCasing(base32Hex(), "f", "CO======");
    testStreamingEncodingWithCasing(base32Hex(), "fo", "CPNG====");
    testStreamingEncodingWithCasing(base32Hex(), "foo", "CPNMU===");
    testStreamingEncodingWithCasing(base32Hex(), "foob", "CPNMUOG=");
    testStreamingEncodingWithCasing(base32Hex(), "fooba", "CPNMUOJ1");
    testStreamingEncodingWithCasing(base32Hex(), "foobar", "CPNMUOJ1E8======");
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
    assertFailsToDecode(base32Hex(), "A\u007f", "Unrecognized character: 0x7f");
    assertFailsToDecode(base32Hex(), "Wf2!", "Unrecognized character: W");
    // This sentence just isn't base32 encoded.
    assertFailsToDecode(base32Hex(), "let's not talk of love or chains!");
    // An 8n+{1,3,6} length string is never legal base32.
    assertFailsToDecode(base32Hex(), "A");
    assertFailsToDecode(base32Hex(), "ABC");
    assertFailsToDecode(base32Hex(), "ABCDEF");
  }

  public void testBase32HexUpperCaseIsNoOp() {
    assertThat(base32Hex().upperCase()).isSameInstanceAs(base32Hex());
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
    assertThat(base16().upperCase()).isSameInstanceAs(base16());
  }

  public void testBase16LowerCase() {
    BaseEncoding lowerCase = base16().lowerCase();
    assertThat(lowerCase).isNotSameInstanceAs(base16());
    assertThat(lowerCase).isSameInstanceAs(base16().lowerCase());
    testEncodingWithCasing(lowerCase, "foobar", "666f6f626172");
  }

  public void testBase16IgnoreCase() {
    BaseEncoding ignoreCase = base16().ignoreCase();
    assertThat(ignoreCase).isNotSameInstanceAs(base16());
    assertThat(ignoreCase).isSameInstanceAs(base16().ignoreCase());
    testEncodingWithCasing(ignoreCase, "foobar", "666F6F626172");
    testDecodes(ignoreCase, "666F6F626172", "foobar");
    testDecodes(ignoreCase, "666f6f626172", "foobar");
    testDecodes(ignoreCase, "666F6f626172", "foobar");
  }

  public void testBase16LowerCaseIgnoreCase() {
    BaseEncoding ignoreCase = base16().lowerCase().ignoreCase();
    assertThat(ignoreCase).isNotSameInstanceAs(base16());
    assertThat(ignoreCase).isSameInstanceAs(base16().lowerCase().ignoreCase());
    testEncodingWithCasing(ignoreCase, "foobar", "666f6f626172");
    testDecodes(ignoreCase, "666F6F626172", "foobar");
    testDecodes(ignoreCase, "666f6f626172", "foobar");
    testDecodes(ignoreCase, "666F6f626172", "foobar");
  }

  // order the methods are called should not matter
  public void testBase16IgnoreCaseLowerCase() {
    BaseEncoding ignoreCase = base16().ignoreCase().lowerCase();
    assertThat(ignoreCase).isNotSameInstanceAs(base16());
    testEncodingWithCasing(ignoreCase, "foobar", "666f6f626172");
    testDecodes(ignoreCase, "666F6F626172", "foobar");
    testDecodes(ignoreCase, "666f6f626172", "foobar");
    testDecodes(ignoreCase, "666F6f626172", "foobar");
  }

  public void testBase16InvalidDecodings() {
    // These contain bytes not in the decodabet.
    assertFailsToDecode(base16(), "\n\n", "Unrecognized character: 0xa");
    assertFailsToDecode(base16(), "EFGH", "Unrecognized character: G");
    // Valid base16 strings always have an even length.
    assertFailsToDecode(base16(), "A", "Invalid input length 1");
    assertFailsToDecode(base16(), "ABC");
    // These have a combination of invalid length and unrecognized characters.
    assertFailsToDecode(base16(), "?", "Invalid input length 1");
    assertFailsToDecode(base16(), "ab");
    assertFailsToDecode(base16().lowerCase(), "AB");
  }

  public void testBase16Offset() {
    testEncodesWithOffset(base16(), "foobar", 0, 6, "666F6F626172");
    testEncodesWithOffset(base16(), "foobar", 1, 5, "6F6F626172");
    testEncodesWithOffset(base16(), "foobar", 2, 3, "6F6261");
    testEncodesWithOffset(base16(), "foobar", 3, 1, "62");
    testEncodesWithOffset(base16(), "foobar", 4, 0, "");
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
        testEncoding(
            encoding.withSeparator(separator, sepLength),
            decoded,
            Joiner.on(separator).join(Splitter.fixedLength(sepLength).split(encoded)));
      }
    }
  }

  private static void testEncoding(BaseEncoding encoding, String decoded, String encoded) {
    testEncodes(encoding, decoded, encoded);
    testDecodes(encoding, encoded, decoded);
  }

  private static void testEncodes(BaseEncoding encoding, String decoded, String encoded) {
    assertThat(encoding.encode(decoded.getBytes(UTF_8))).isEqualTo(encoded);
  }

  private static void testEncodesWithOffset(
      BaseEncoding encoding, String decoded, int offset, int len, String encoded) {
    assertThat(encoding.encode(decoded.getBytes(UTF_8), offset, len)).isEqualTo(encoded);
  }

  private static void testDecodes(BaseEncoding encoding, String encoded, String decoded) {
    assertThat(encoding.canDecode(encoded)).isTrue();
    assertThat(encoding.decode(encoded)).isEqualTo(decoded.getBytes(UTF_8));
  }

  private static void testDecodesByBytes(BaseEncoding encoding, String encoded, byte[] decoded) {
    assertThat(encoding.canDecode(encoded)).isTrue();
    assertThat(encoding.decode(encoded)).isEqualTo(decoded);
  }

  private static void assertFailsToDecode(BaseEncoding encoding, String cannotDecode) {
    assertFailsToDecode(encoding, cannotDecode, null);
  }

  private static void assertFailsToDecode(
      BaseEncoding encoding, String cannotDecode, @Nullable String expectedMessage) {
    // We use this somewhat weird pattern with an enum for each assertion we want to make as a way
    // of dealing with the fact that one of the assertions is @GwtIncompatible but we don't want to
    // have to have duplicate @GwtIncompatible test methods just to make that assertion.
    for (AssertFailsToDecodeStrategy strategy : AssertFailsToDecodeStrategy.values()) {
      strategy.assertFailsToDecode(encoding, cannotDecode, expectedMessage);
    }
  }

  enum AssertFailsToDecodeStrategy {
    CAN_DECODE {
      @Override
      void assertFailsToDecode(
          BaseEncoding encoding, String cannotDecode, @Nullable String expectedMessage) {
        assertThat(encoding.canDecode(cannotDecode)).isFalse();
      }
    },
    DECODE {
      @Override
      void assertFailsToDecode(
          BaseEncoding encoding, String cannotDecode, @Nullable String expectedMessage) {
        try {
          encoding.decode(cannotDecode);
          fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
          if (expectedMessage != null) {
            assertThat(expected).hasCauseThat().hasMessageThat().isEqualTo(expectedMessage);
          }
        }
      }
    },
    DECODE_CHECKED {
      @Override
      void assertFailsToDecode(
          BaseEncoding encoding, String cannotDecode, @Nullable String expectedMessage) {
        try {
          encoding.decodeChecked(cannotDecode);
          fail("Expected DecodingException");
        } catch (DecodingException expected) {
          if (expectedMessage != null) {
            assertThat(expected).hasMessageThat().isEqualTo(expectedMessage);
          }
        }
      }
    },
    /*
     * This one comes last to work around b/367716565. (One *possible* alternative would be to not
     * declare any methods in this enum, converting assertFailsToDecode into a static method that is
     * implemented with a `switch`. I haven't tested that.)
     */
    @GwtIncompatible // decodingStream(Reader)
    DECODING_STREAM {
      @Override
      void assertFailsToDecode(
          BaseEncoding encoding, String cannotDecode, @Nullable String expectedMessage) {
        // Regression test for case where DecodingException was swallowed by default implementation
        // of
        // InputStream.read(byte[], int, int)
        // See https://github.com/google/guava/issues/3542
        Reader reader = new StringReader(cannotDecode);
        InputStream decodingStream = encoding.decodingStream(reader);
        try {
          ByteStreams.exhaust(decodingStream);
          fail("Expected DecodingException");
        } catch (DecodingException expected) {
          // Don't assert on the expectedMessage; the messages for exceptions thrown from the
          // decoding stream may differ from the messages for the decode methods.
        } catch (IOException e) {
          fail("Expected DecodingException but got: " + e);
        }
      }
    };

    abstract void assertFailsToDecode(
        BaseEncoding encoding, String cannotDecode, @Nullable String expectedMessage);
  }

  @GwtIncompatible // Reader/Writer
  private static void testStreamingEncodingWithCasing(
      BaseEncoding encoding, String decoded, String encoded) throws IOException {
    testStreamingEncodingWithSeparators(encoding, decoded, encoded);
    testStreamingEncodingWithSeparators(encoding.upperCase(), decoded, Ascii.toUpperCase(encoded));
    testStreamingEncodingWithSeparators(encoding.lowerCase(), decoded, Ascii.toLowerCase(encoded));
  }

  @GwtIncompatible // Reader/Writer
  private static void testStreamingEncodingWithSeparators(
      BaseEncoding encoding, String decoded, String encoded) throws IOException {
    testStreamingEncoding(encoding, decoded, encoded);

    // test separators work
    for (int sepLength = 3; sepLength <= 5; sepLength++) {
      for (String separator : ImmutableList.of(",", "\n", ";;", "")) {
        testStreamingEncoding(
            encoding.withSeparator(separator, sepLength),
            decoded,
            Joiner.on(separator).join(Splitter.fixedLength(sepLength).split(encoded)));
      }
    }
  }

  @GwtIncompatible // Reader/Writer
  private static void testStreamingEncoding(BaseEncoding encoding, String decoded, String encoded)
      throws IOException {
    testStreamingEncodes(encoding, decoded, encoded);
    testStreamingDecodes(encoding, encoded, decoded);
  }

  @GwtIncompatible // Writer
  private static void testStreamingEncodes(BaseEncoding encoding, String decoded, String encoded)
      throws IOException {
    StringWriter writer = new StringWriter();
    try (OutputStream encodingStream = encoding.encodingStream(writer)) {
      encodingStream.write(decoded.getBytes(UTF_8));
    }
    assertThat(writer.toString()).isEqualTo(encoded);
  }

  @GwtIncompatible // Reader
  private static void testStreamingDecodes(BaseEncoding encoding, String encoded, String decoded)
      throws IOException {
    byte[] bytes = decoded.getBytes(UTF_8);
    try (InputStream decodingStream = encoding.decodingStream(new StringReader(encoded))) {
      for (int i = 0; i < bytes.length; i++) {
        assertThat(decodingStream.read()).isEqualTo(bytes[i] & 0xFF);
      }
      assertThat(decodingStream.read()).isEqualTo(-1);
    }
  }

  public void testToString() {
    assertThat(base64().toString()).isEqualTo("BaseEncoding.base64().withPadChar('=')");
    assertThat(base32Hex().omitPadding().toString())
        .isEqualTo("BaseEncoding.base32Hex().omitPadding()");
    assertThat(base32().lowerCase().withPadChar('$').toString())
        .isEqualTo("BaseEncoding.base32().lowerCase().withPadChar('$')");
    assertThat(base16().withSeparator("\n", 10).toString())
        .isEqualTo("BaseEncoding.base16().withSeparator(\"\n\", 10)");
  }
}

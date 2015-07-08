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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.math.IntMath.divide;
import static com.google.common.math.IntMath.log2;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;
import static java.math.RoundingMode.UNNECESSARY;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * A binary encoding scheme for reversibly translating between byte sequences and printable ASCII
 * strings. This class includes several constants for encoding schemes specified by <a
 * href="http://tools.ietf.org/html/rfc4648">RFC 4648</a>. For example, the expression:
 *
 * <pre>   {@code
 *   BaseEncoding.base32().encode("foo".getBytes(Charsets.US_ASCII))}</pre>
 *
 * <p>returns the string {@code "MZXW6==="}, and <pre>   {@code
 *  byte[] decoded = BaseEncoding.base32().decode("MZXW6===");}</pre>
 *
 * <p>...returns the ASCII bytes of the string {@code "foo"}.
 *
 * <p>By default, {@code BaseEncoding}'s behavior is relatively strict and in accordance with
 * RFC 4648.  Decoding rejects characters in the wrong case, though padding is optional.
 * To modify encoding and decoding behavior, use configuration methods to obtain a new encoding
 * with modified behavior:
 *
 * <pre>   {@code
 *  BaseEncoding.base16().lowerCase().decode("deadbeef");}</pre>
 *
 * <p>Warning: BaseEncoding instances are immutable.  Invoking a configuration method has no effect
 * on the receiving instance; you must store and use the new encoding instance it returns, instead.
 *
 * <pre>   {@code
 *   // Do NOT do this
 *   BaseEncoding hex = BaseEncoding.base16();
 *   hex.lowerCase(); // does nothing!
 *   return hex.decode("deadbeef"); // throws an IllegalArgumentException}</pre>
 *
 * <p>It is guaranteed that {@code encoding.decode(encoding.encode(x))} is always equal to
 * {@code x}, but the reverse does not necessarily hold.
 *
 * <p>
 * <table>
 * <tr>
 * <th>Encoding
 * <th>Alphabet
 * <th>{@code char:byte} ratio
 * <th>Default padding
 * <th>Comments
 * <tr>
 * <td>{@link #base16()}
 * <td>0-9 A-F
 * <td>2.00
 * <td>N/A
 * <td>Traditional hexadecimal.  Defaults to upper case.
 * <tr>
 * <td>{@link #base32()}
 * <td>A-Z 2-7
 * <td>1.60
 * <td>=
 * <td>Human-readable; no possibility of mixing up 0/O or 1/I.  Defaults to upper case.
 * <tr>
 * <td>{@link #base32Hex()}
 * <td>0-9 A-V
 * <td>1.60
 * <td>=
 * <td>"Numerical" base 32; extended from the traditional hex alphabet.  Defaults to upper case.
 * <tr>
 * <td>{@link #base64()}
 * <td>A-Z a-z 0-9 + /
 * <td>1.33
 * <td>=
 * <td>
 * <tr>
 * <td>{@link #base64Url()}
 * <td>A-Z a-z 0-9 - _
 * <td>1.33
 * <td>=
 * <td>Safe to use as filenames, or to pass in URLs without escaping
 * </table>
 *
 * <p>
 * All instances of this class are immutable, so they may be stored safely as static constants.
 *
 * @author Louis Wasserman
 * @since 14.0
 */
@Beta
@GwtCompatible(emulated = true)
public abstract class BaseEncoding {
  // TODO(lowasser): consider making encodeTo(Appendable, byte[], int, int) public.

  BaseEncoding() {}

  /**
   * Exception indicating invalid base-encoded input encountered while decoding.
   *
   * @author Louis Wasserman
   * @since 15.0
   */
  public static final class DecodingException extends IOException {
    DecodingException(String message) {
      super(message);
    }

    DecodingException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Encodes the specified byte array, and returns the encoded {@code String}.
   */
  public String encode(byte[] bytes) {
    return encode(bytes, 0, bytes.length);
  }

  /**
   * Encodes the specified range of the specified byte array, and returns the encoded
   * {@code String}.
   */
  public final String encode(byte[] bytes, int off, int len) {
    checkPositionIndexes(off, off + len, bytes.length);
    StringBuilder result = new StringBuilder(maxEncodedSize(len));
    try {
      encodeTo(result, bytes, off, len);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
    return result.toString();
  }

  /**
   * Returns an {@code OutputStream} that encodes bytes using this encoding into the specified
   * {@code Writer}.  When the returned {@code OutputStream} is closed, so is the backing
   * {@code Writer}.
   */
  @GwtIncompatible("Writer,OutputStream")
  public abstract OutputStream encodingStream(Writer writer);

  /**
   * Returns a {@code ByteSink} that writes base-encoded bytes to the specified {@code CharSink}.
   */
  @GwtIncompatible("ByteSink,CharSink")
  public final ByteSink encodingSink(final CharSink encodedSink) {
    checkNotNull(encodedSink);
    return new ByteSink() {
      @Override
      public OutputStream openStream() throws IOException {
        return encodingStream(encodedSink.openStream());
      }
    };
  }

  // TODO(lowasser): document the extent of leniency, probably after adding ignore(CharMatcher)

  private static byte[] extract(byte[] result, int length) {
    if (length == result.length) {
      return result;
    } else {
      byte[] trunc = new byte[length];
      System.arraycopy(result, 0, trunc, 0, length);
      return trunc;
    }
  }

  /**
   * Decodes the specified character sequence, and returns the resulting {@code byte[]}.
   * This is the inverse operation to {@link #encode(byte[])}.
   *
   * @throws IllegalArgumentException if the input is not a valid encoded string according to this
   *         encoding.
   */
  public final byte[] decode(CharSequence chars) {
    try {
      return decodeChecked(chars);
    } catch (DecodingException badInput) {
      throw new IllegalArgumentException(badInput);
    }
  }

  /**
   * Decodes the specified character sequence, and returns the resulting {@code byte[]}.
   * This is the inverse operation to {@link #encode(byte[])}.
   *
   * @throws DecodingException if the input is not a valid encoded string according to this
   *         encoding.
   */
  final byte[] decodeChecked(CharSequence chars) throws DecodingException {
    chars = padding().trimTrailingFrom(chars);
    byte[] tmp = new byte[maxDecodedSize(chars.length())];
    int len = decodeTo(tmp, chars);
    return extract(tmp, len);
  }

  /**
   * Returns an {@code InputStream} that decodes base-encoded input from the specified
   * {@code Reader}.  The returned stream throws a {@link DecodingException} upon decoding-specific
   * errors.
   */
  @GwtIncompatible("Reader,InputStream")
  public abstract InputStream decodingStream(Reader reader);

  /**
   * Returns a {@code ByteSource} that reads base-encoded bytes from the specified
   * {@code CharSource}.
   */
  @GwtIncompatible("ByteSource,CharSource")
  public final ByteSource decodingSource(final CharSource encodedSource) {
    checkNotNull(encodedSource);
    return new ByteSource() {
      @Override
      public InputStream openStream() throws IOException {
        return decodingStream(encodedSource.openStream());
      }
    };
  }

  // Implementations for encoding/decoding

  abstract int maxEncodedSize(int bytes);

  abstract void encodeTo(Appendable target, byte[] bytes, int off, int len) throws IOException;

  abstract int maxDecodedSize(int chars);

  abstract int decodeTo(byte[] target, CharSequence chars) throws DecodingException;

  abstract CharMatcher padding();

  // Modified encoding generators

  /**
   * Returns an encoding that behaves equivalently to this encoding, but omits any padding
   * characters as specified by <a href="http://tools.ietf.org/html/rfc4648#section-3.2">RFC 4648
   * section 3.2</a>, Padding of Encoded Data.
   */
  @CheckReturnValue
  public abstract BaseEncoding omitPadding();

  /**
   * Returns an encoding that behaves equivalently to this encoding, but uses an alternate character
   * for padding.
   *
   * @throws IllegalArgumentException if this padding character is already used in the alphabet or a
   *         separator
   */
  @CheckReturnValue
  public abstract BaseEncoding withPadChar(char padChar);

  /**
   * Returns an encoding that behaves equivalently to this encoding, but adds a separator string
   * after every {@code n} characters. Any occurrences of any characters that occur in the separator
   * are skipped over in decoding.
   *
   * @throws IllegalArgumentException if any alphabet or padding characters appear in the separator
   *         string, or if {@code n <= 0}
   * @throws UnsupportedOperationException if this encoding already uses a separator
   */
  @CheckReturnValue
  public abstract BaseEncoding withSeparator(String separator, int n);

  /**
   * Returns an encoding that behaves equivalently to this encoding, but encodes and decodes with
   * uppercase letters. Padding and separator characters remain in their original case.
   *
   * @throws IllegalStateException if the alphabet used by this encoding contains mixed upper- and
   *         lower-case characters
   */
  @CheckReturnValue
  public abstract BaseEncoding upperCase();

  /**
   * Returns an encoding that behaves equivalently to this encoding, but encodes and decodes with
   * lowercase letters. Padding and separator characters remain in their original case.
   *
   * @throws IllegalStateException if the alphabet used by this encoding contains mixed upper- and
   *         lower-case characters
   */
  @CheckReturnValue
  public abstract BaseEncoding lowerCase();

  private static final BaseEncoding BASE64 = new Base64Encoding(
      "base64()", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/", '=');

  /**
   * The "base64" base encoding specified by <a
   * href="http://tools.ietf.org/html/rfc4648#section-4">RFC 4648 section 4</a>, Base 64 Encoding.
   * (This is the same as the base 64 encoding from <a
   * href="http://tools.ietf.org/html/rfc3548#section-3">RFC 3548</a>.)
   *
   * <p>The character {@code '='} is used for padding, but can be {@linkplain #omitPadding()
   * omitted} or {@linkplain #withPadChar(char) replaced}.
   *
   * <p>No line feeds are added by default, as per <a
   * href="http://tools.ietf.org/html/rfc4648#section-3.1"> RFC 4648 section 3.1</a>, Line Feeds in
   * Encoded Data. Line feeds may be added using {@link #withSeparator(String, int)}.
   */
  public static BaseEncoding base64() {
    return BASE64;
  }

  private static final BaseEncoding BASE64_URL = new Base64Encoding(
      "base64Url()", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_", '=');

  /**
   * The "base64url" encoding specified by <a
   * href="http://tools.ietf.org/html/rfc4648#section-5">RFC 4648 section 5</a>, Base 64 Encoding
   * with URL and Filename Safe Alphabet, also sometimes referred to as the "web safe Base64."
   * (This is the same as the base 64 encoding with URL and filename safe alphabet from <a
   * href="http://tools.ietf.org/html/rfc3548#section-4">RFC 3548</a>.)
   *
   * <p>The character {@code '='} is used for padding, but can be {@linkplain #omitPadding()
   * omitted} or {@linkplain #withPadChar(char) replaced}.
   *
   * <p>No line feeds are added by default, as per <a
   * href="http://tools.ietf.org/html/rfc4648#section-3.1"> RFC 4648 section 3.1</a>, Line Feeds in
   * Encoded Data. Line feeds may be added using {@link #withSeparator(String, int)}.
   */
  public static BaseEncoding base64Url() {
    return BASE64_URL;
  }

  private static final BaseEncoding BASE32 =
      new StandardBaseEncoding("base32()", "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567", '=');

  /**
   * The "base32" encoding specified by <a
   * href="http://tools.ietf.org/html/rfc4648#section-6">RFC 4648 section 6</a>, Base 32 Encoding.
   * (This is the same as the base 32 encoding from <a
   * href="http://tools.ietf.org/html/rfc3548#section-5">RFC 3548</a>.)
   *
   * <p>The character {@code '='} is used for padding, but can be {@linkplain #omitPadding()
   * omitted} or {@linkplain #withPadChar(char) replaced}.
   *
   * <p>No line feeds are added by default, as per <a
   * href="http://tools.ietf.org/html/rfc4648#section-3.1"> RFC 4648 section 3.1</a>, Line Feeds in
   * Encoded Data. Line feeds may be added using {@link #withSeparator(String, int)}.
   */
  public static BaseEncoding base32() {
    return BASE32;
  }

  private static final BaseEncoding BASE32_HEX =
      new StandardBaseEncoding("base32Hex()", "0123456789ABCDEFGHIJKLMNOPQRSTUV", '=');

  /**
   * The "base32hex" encoding specified by <a
   * href="http://tools.ietf.org/html/rfc4648#section-7">RFC 4648 section 7</a>, Base 32 Encoding
   * with Extended Hex Alphabet.  There is no corresponding encoding in RFC 3548.
   *
   * <p>The character {@code '='} is used for padding, but can be {@linkplain #omitPadding()
   * omitted} or {@linkplain #withPadChar(char) replaced}.
   *
   * <p>No line feeds are added by default, as per <a
   * href="http://tools.ietf.org/html/rfc4648#section-3.1"> RFC 4648 section 3.1</a>, Line Feeds in
   * Encoded Data. Line feeds may be added using {@link #withSeparator(String, int)}.
   */
  public static BaseEncoding base32Hex() {
    return BASE32_HEX;
  }

  private static final BaseEncoding BASE16 = new Base16Encoding("base16()", "0123456789ABCDEF");

  /**
   * The "base16" encoding specified by <a
   * href="http://tools.ietf.org/html/rfc4648#section-8">RFC 4648 section 8</a>, Base 16 Encoding.
   * (This is the same as the base 16 encoding from <a
   * href="http://tools.ietf.org/html/rfc3548#section-6">RFC 3548</a>.) This is commonly known as
   * "hexadecimal" format.
   *
   * <p>No padding is necessary in base 16, so {@link #withPadChar(char)} and
   * {@link #omitPadding()} have no effect.
   *
   * <p>No line feeds are added by default, as per <a
   * href="http://tools.ietf.org/html/rfc4648#section-3.1"> RFC 4648 section 3.1</a>, Line Feeds in
   * Encoded Data. Line feeds may be added using {@link #withSeparator(String, int)}.
   */
  public static BaseEncoding base16() {
    return BASE16;
  }

  private static final class Alphabet extends CharMatcher {
    private final String name;
    // this is meant to be immutable -- don't modify it!
    private final char[] chars;
    final int mask;
    final int bitsPerChar;
    final int charsPerChunk;
    final int bytesPerChunk;
    private final byte[] decodabet;
    private final boolean[] validPadding;

    Alphabet(String name, char[] chars) {
      this.name = checkNotNull(name);
      this.chars = checkNotNull(chars);
      try {
        this.bitsPerChar = log2(chars.length, UNNECESSARY);
      } catch (ArithmeticException e) {
        throw new IllegalArgumentException("Illegal alphabet length " + chars.length, e);
      }

      /*
       * e.g. for base64, bitsPerChar == 6, charsPerChunk == 4, and bytesPerChunk == 3. This makes
       * for the smallest chunk size that still has charsPerChunk * bitsPerChar be a multiple of 8.
       */
      int gcd = Math.min(8, Integer.lowestOneBit(bitsPerChar));
      this.charsPerChunk = 8 / gcd;
      this.bytesPerChunk = bitsPerChar / gcd;

      this.mask = chars.length - 1;

      byte[] decodabet = new byte[Ascii.MAX + 1];
      Arrays.fill(decodabet, (byte) -1);
      for (int i = 0; i < chars.length; i++) {
        char c = chars[i];
        checkArgument(CharMatcher.ASCII.matches(c), "Non-ASCII character: %s", c);
        checkArgument(decodabet[c] == -1, "Duplicate character: %s", c);
        decodabet[c] = (byte) i;
      }
      this.decodabet = decodabet;

      boolean[] validPadding = new boolean[charsPerChunk];
      for (int i = 0; i < bytesPerChunk; i++) {
        validPadding[divide(i * 8, bitsPerChar, CEILING)] = true;
      }
      this.validPadding = validPadding;
    }

    char encode(int bits) {
      return chars[bits];
    }

    boolean isValidPaddingStartPosition(int index) {
      return validPadding[index % charsPerChunk];
    }

    int decode(char ch) throws DecodingException {
      if (ch > Ascii.MAX || decodabet[ch] == -1) {
        throw new DecodingException("Unrecognized character: "
            + (CharMatcher.INVISIBLE.matches(ch) ? "0x" + Integer.toHexString(ch) : ch));
      }
      return decodabet[ch];
    }

    private boolean hasLowerCase() {
      for (char c : chars) {
        if (Ascii.isLowerCase(c)) {
          return true;
        }
      }
      return false;
    }

    private boolean hasUpperCase() {
      for (char c : chars) {
        if (Ascii.isUpperCase(c)) {
          return true;
        }
      }
      return false;
    }

    Alphabet upperCase() {
      if (!hasLowerCase()) {
        return this;
      } else {
        checkState(!hasUpperCase(), "Cannot call upperCase() on a mixed-case alphabet");
        char[] upperCased = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
          upperCased[i] = Ascii.toUpperCase(chars[i]);
        }
        return new Alphabet(name + ".upperCase()", upperCased);
      }
    }

    Alphabet lowerCase() {
      if (!hasUpperCase()) {
        return this;
      } else {
        checkState(!hasLowerCase(), "Cannot call lowerCase() on a mixed-case alphabet");
        char[] lowerCased = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
          lowerCased[i] = Ascii.toLowerCase(chars[i]);
        }
        return new Alphabet(name + ".lowerCase()", lowerCased);
      }
    }

    @Override
    public boolean matches(char c) {
      return CharMatcher.ASCII.matches(c) && decodabet[c] != -1;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  static class StandardBaseEncoding extends BaseEncoding {
    // TODO(lowasser): provide a useful toString
    final Alphabet alphabet;

    @Nullable
    final Character paddingChar;

    StandardBaseEncoding(String name, String alphabetChars, @Nullable Character paddingChar) {
      this(new Alphabet(name, alphabetChars.toCharArray()), paddingChar);
    }

    StandardBaseEncoding(Alphabet alphabet, @Nullable Character paddingChar) {
      this.alphabet = checkNotNull(alphabet);
      checkArgument(paddingChar == null || !alphabet.matches(paddingChar),
          "Padding character %s was already in alphabet", paddingChar);
      this.paddingChar = paddingChar;
    }

    @Override
    CharMatcher padding() {
      return (paddingChar == null) ? CharMatcher.NONE : CharMatcher.is(paddingChar.charValue());
    }

    @Override
    int maxEncodedSize(int bytes) {
      return alphabet.charsPerChunk * divide(bytes, alphabet.bytesPerChunk, CEILING);
    }

    @GwtIncompatible("Writer,OutputStream")
    @Override
    public OutputStream encodingStream(final Writer out) {
      checkNotNull(out);
      return new OutputStream() {
        int bitBuffer = 0;
        int bitBufferLength = 0;
        int writtenChars = 0;

        @Override
        public void write(int b) throws IOException {
          bitBuffer <<= 8;
          bitBuffer |= b & 0xFF;
          bitBufferLength += 8;
          while (bitBufferLength >= alphabet.bitsPerChar) {
            int charIndex = (bitBuffer >> (bitBufferLength - alphabet.bitsPerChar))
                & alphabet.mask;
            out.write(alphabet.encode(charIndex));
            writtenChars++;
            bitBufferLength -= alphabet.bitsPerChar;
          }
        }

        @Override
        public void flush() throws IOException {
          out.flush();
        }

        @Override
        public void close() throws IOException {
          if (bitBufferLength > 0) {
            int charIndex = (bitBuffer << (alphabet.bitsPerChar - bitBufferLength))
                & alphabet.mask;
            out.write(alphabet.encode(charIndex));
            writtenChars++;
            if (paddingChar != null) {
              while (writtenChars % alphabet.charsPerChunk != 0) {
                out.write(paddingChar.charValue());
                writtenChars++;
              }
            }
          }
          out.close();
        }
      };
    }

    @Override
    void encodeTo(Appendable target, byte[] bytes, int off, int len) throws IOException {
      checkNotNull(target);
      checkPositionIndexes(off, off + len, bytes.length);
      for (int i = 0; i < len; i += alphabet.bytesPerChunk) {
        encodeChunkTo(target, bytes, off + i, Math.min(alphabet.bytesPerChunk, len - i));
      }
    }

    void encodeChunkTo(Appendable target, byte[] bytes, int off, int len)
        throws IOException {
      checkNotNull(target);
      checkPositionIndexes(off, off + len, bytes.length);
      checkArgument(len <= alphabet.bytesPerChunk);
      long bitBuffer = 0;
      for (int i = 0; i < len; ++i) {
        bitBuffer |= bytes[off + i] & 0xFF;
        bitBuffer <<= 8; // Add additional zero byte in the end.
      }
      // Position of first character is length of bitBuffer minus bitsPerChar.
      final int bitOffset = (len + 1) * 8 - alphabet.bitsPerChar;
      int bitsProcessed = 0;
      while (bitsProcessed < len * 8) {
        int charIndex = (int) (bitBuffer >>> (bitOffset - bitsProcessed)) & alphabet.mask;
        target.append(alphabet.encode(charIndex));
        bitsProcessed += alphabet.bitsPerChar;
      }
      if (paddingChar != null) {
        while (bitsProcessed < alphabet.bytesPerChunk * 8) {
          target.append(paddingChar.charValue());
          bitsProcessed += alphabet.bitsPerChar;
        }
      }
    }

    @Override
    int maxDecodedSize(int chars) {
      return (int) ((alphabet.bitsPerChar * (long) chars + 7L) / 8L);
    }

    @Override
    int decodeTo(byte[] target, CharSequence chars) throws DecodingException {
      checkNotNull(target);
      chars = padding().trimTrailingFrom(chars);
      if (!alphabet.isValidPaddingStartPosition(chars.length())) {
        throw new DecodingException("Invalid input length " + chars.length());
      }
      int bytesWritten = 0;
      for (int charIdx = 0; charIdx < chars.length(); charIdx += alphabet.charsPerChunk) {
        long chunk = 0;
        int charsProcessed = 0;
        for (int i = 0; i < alphabet.charsPerChunk; i++) {
          chunk <<= alphabet.bitsPerChar;
          if (charIdx + i < chars.length()) {
            chunk |= alphabet.decode(chars.charAt(charIdx + charsProcessed++));
          }
        }
        final int minOffset = alphabet.bytesPerChunk * 8 - charsProcessed * alphabet.bitsPerChar;
        for (int offset = (alphabet.bytesPerChunk - 1) * 8; offset >= minOffset; offset -= 8) {
          target[bytesWritten++] = (byte) ((chunk >>> offset) & 0xFF);
        }
      }
      return bytesWritten;
    }

    @GwtIncompatible("Reader,InputStream")
    @Override
    public InputStream decodingStream(final Reader reader) {
      checkNotNull(reader);
      return new InputStream() {
        int bitBuffer = 0;
        int bitBufferLength = 0;
        int readChars = 0;
        boolean hitPadding = false;
        final CharMatcher paddingMatcher = padding();

        @Override
        public int read() throws IOException {
          while (true) {
            int readChar = reader.read();
            if (readChar == -1) {
              if (!hitPadding && !alphabet.isValidPaddingStartPosition(readChars)) {
                throw new DecodingException("Invalid input length " + readChars);
              }
              return -1;
            }
            readChars++;
            char ch = (char) readChar;
            if (paddingMatcher.matches(ch)) {
              if (!hitPadding
                  && (readChars == 1 || !alphabet.isValidPaddingStartPosition(readChars - 1))) {
                throw new DecodingException("Padding cannot start at index " + readChars);
              }
              hitPadding = true;
            } else if (hitPadding) {
              throw new DecodingException(
                  "Expected padding character but found '" + ch + "' at index " + readChars);
            } else {
              bitBuffer <<= alphabet.bitsPerChar;
              bitBuffer |= alphabet.decode(ch);
              bitBufferLength += alphabet.bitsPerChar;

              if (bitBufferLength >= 8) {
                bitBufferLength -= 8;
                return (bitBuffer >> bitBufferLength) & 0xFF;
              }
            }
          }
        }

        @Override
        public void close() throws IOException {
          reader.close();
        }
      };
    }

    @Override
    public BaseEncoding omitPadding() {
      return (paddingChar == null) ? this : newInstance(alphabet, null);
    }

    @Override
    public BaseEncoding withPadChar(char padChar) {
      if (8 % alphabet.bitsPerChar == 0 ||
          (paddingChar != null && paddingChar.charValue() == padChar)) {
        return this;
      } else {
        return newInstance(alphabet, padChar);
      }
    }

    @Override
    public BaseEncoding withSeparator(String separator, int afterEveryChars) {
      checkArgument(padding().or(alphabet).matchesNoneOf(separator),
          "Separator (%s) cannot contain alphabet or padding characters", separator);
      return new SeparatedBaseEncoding(this, separator, afterEveryChars);
    }

    private transient BaseEncoding upperCase;
    private transient BaseEncoding lowerCase;

    @Override
    public BaseEncoding upperCase() {
      BaseEncoding result = upperCase;
      if (result == null) {
        Alphabet upper = alphabet.upperCase();
        result = upperCase =
            (upper == alphabet) ? this : newInstance(upper, paddingChar);
      }
      return result;
    }

    @Override
    public BaseEncoding lowerCase() {
      BaseEncoding result = lowerCase;
      if (result == null) {
        Alphabet lower = alphabet.lowerCase();
        result = lowerCase =
            (lower == alphabet) ? this : newInstance(lower, paddingChar);
      }
      return result;
    }

    BaseEncoding newInstance(Alphabet alphabet, @Nullable Character paddingChar) {
      return new StandardBaseEncoding(alphabet, paddingChar);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder("BaseEncoding.");
      builder.append(alphabet.toString());
      if (8 % alphabet.bitsPerChar != 0) {
        if (paddingChar == null) {
          builder.append(".omitPadding()");
        } else {
          builder.append(".withPadChar(").append(paddingChar).append(')');
        }
      }
      return builder.toString();
    }
  }

  static final class Base16Encoding extends StandardBaseEncoding {
    final char[] encoding = new char[512];

    Base16Encoding(String name, String alphabetChars) {
      this(new Alphabet(name, alphabetChars.toCharArray()));
    }

    private Base16Encoding(Alphabet alphabet) {
      super(alphabet, null);
      checkArgument(alphabet.chars.length == 16);
      for (int i = 0; i < 256; ++i) {
        encoding[i] = alphabet.encode(i >>> 4);
        encoding[i | 0x100] = alphabet.encode(i & 0xF);
      }
    }

    @Override
    void encodeTo(Appendable target, byte[] bytes, int off, int len) throws IOException {
      checkNotNull(target);
      checkPositionIndexes(off, off + len, bytes.length);
      for (int i = 0; i < len; ++i) {
        int b = bytes[off + i] & 0xFF;
        target.append(encoding[b]);
        target.append(encoding[b | 0x100]);
      }
    }

    @Override
    int decodeTo(byte[] target, CharSequence chars) throws DecodingException {
      checkNotNull(target);
      if (chars.length() % 2 == 1) {
        throw new DecodingException("Invalid input length " + chars.length());
      }
      int bytesWritten = 0;
      for (int i = 0; i < chars.length(); i += 2) {
        int decoded = alphabet.decode(chars.charAt(i)) << 4 | alphabet.decode(chars.charAt(i + 1));
        target[bytesWritten++] = (byte) decoded;
      }
      return bytesWritten;
    }

    @Override
    BaseEncoding newInstance(Alphabet alphabet, @Nullable Character paddingChar) {
      return new Base16Encoding(alphabet);
    }
  }

  static final class Base64Encoding extends StandardBaseEncoding {
    Base64Encoding(String name, String alphabetChars, @Nullable Character paddingChar) {
      this(new Alphabet(name, alphabetChars.toCharArray()), paddingChar);
    }

    private Base64Encoding(Alphabet alphabet, @Nullable Character paddingChar) {
      super(alphabet, paddingChar);
      checkArgument(alphabet.chars.length == 64);
    }

    @Override
    void encodeTo(Appendable target, byte[] bytes, int off, int len) throws IOException {
      checkNotNull(target);
      checkPositionIndexes(off, off + len, bytes.length);
      int i = off;
      for (int remaining = len; remaining >= 3; remaining -= 3) {
        int chunk = (bytes[i++] & 0xFF) << 16 | (bytes[i++] & 0xFF) << 8 | bytes[i++] & 0xFF;
        target.append(alphabet.encode(chunk >>> 18));
        target.append(alphabet.encode((chunk >>> 12) & 0x3F));
        target.append(alphabet.encode((chunk >>> 6) & 0x3F));
        target.append(alphabet.encode(chunk & 0x3F));
      }
      if (i < off + len) {
        encodeChunkTo(target, bytes, i, off + len - i);
      }
    }

    @Override
    int decodeTo(byte[] target, CharSequence chars) throws DecodingException {
      checkNotNull(target);
      chars = padding().trimTrailingFrom(chars);
      if (!alphabet.isValidPaddingStartPosition(chars.length())) {
        throw new DecodingException("Invalid input length " + chars.length());
      }
      int bytesWritten = 0;
      for (int i = 0; i < chars.length();) {
        int chunk = alphabet.decode(chars.charAt(i++)) << 18;
        chunk |= alphabet.decode(chars.charAt(i++)) << 12;
        target[bytesWritten++] = (byte) (chunk >>> 16);
        if (i < chars.length()) {
          chunk |= alphabet.decode(chars.charAt(i++)) << 6;
          target[bytesWritten++] = (byte) ((chunk >>> 8) & 0xFF);
          if (i < chars.length()) {
            chunk |= alphabet.decode(chars.charAt(i++));
            target[bytesWritten++] = (byte) (chunk & 0xFF);
          }
        }
      }
      return bytesWritten;
    }

    @Override
    BaseEncoding newInstance(Alphabet alphabet, @Nullable Character paddingChar) {
      return new Base64Encoding(alphabet, paddingChar);
    }
  }

  @GwtIncompatible("Reader")
  static Reader ignoringReader(final Reader delegate, final CharMatcher toIgnore) {
    checkNotNull(delegate);
    checkNotNull(toIgnore);
    return new Reader() {
      @Override
      public int read() throws IOException {
        int readChar;
        do {
          readChar = delegate.read();
        } while (readChar != -1 && toIgnore.matches((char) readChar));
        return readChar;
      }

      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override
      public void close() throws IOException {
        delegate.close();
      }
    };
  }

  static Appendable separatingAppendable(
      final Appendable delegate, final String separator, final int afterEveryChars) {
    checkNotNull(delegate);
    checkNotNull(separator);
    checkArgument(afterEveryChars > 0);
    return new Appendable() {
      int charsUntilSeparator = afterEveryChars;

      @Override
      public Appendable append(char c) throws IOException {
        if (charsUntilSeparator == 0) {
          delegate.append(separator);
          charsUntilSeparator = afterEveryChars;
        }
        delegate.append(c);
        charsUntilSeparator--;
        return this;
      }

      @Override
      public Appendable append(CharSequence chars, int off, int len) throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override
      public Appendable append(CharSequence chars) throws IOException {
        throw new UnsupportedOperationException();
      }
    };
  }

  @GwtIncompatible("Writer")
  static Writer separatingWriter(
      final Writer delegate, final String separator, final int afterEveryChars) {
    final Appendable seperatingAppendable =
        separatingAppendable(delegate, separator, afterEveryChars);
    return new Writer() {
      @Override
      public void write(int c) throws IOException {
        seperatingAppendable.append((char) c);
      }

      @Override
      public void write(char[] chars, int off, int len) throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override
      public void flush() throws IOException {
        delegate.flush();
      }

      @Override
      public void close() throws IOException {
        delegate.close();
      }
    };
  }

  static final class SeparatedBaseEncoding extends BaseEncoding {
    private final BaseEncoding delegate;
    private final String separator;
    private final int afterEveryChars;
    private final CharMatcher separatorChars;

    SeparatedBaseEncoding(BaseEncoding delegate, String separator, int afterEveryChars) {
      this.delegate = checkNotNull(delegate);
      this.separator = checkNotNull(separator);
      this.afterEveryChars = afterEveryChars;
      checkArgument(
          afterEveryChars > 0, "Cannot add a separator after every %s chars", afterEveryChars);
      this.separatorChars = CharMatcher.anyOf(separator).precomputed();
    }

    @Override
    CharMatcher padding() {
      return delegate.padding();
    }

    @Override
    int maxEncodedSize(int bytes) {
      int unseparatedSize = delegate.maxEncodedSize(bytes);
      return unseparatedSize + separator.length()
          * divide(Math.max(0, unseparatedSize - 1), afterEveryChars, FLOOR);
    }

    @GwtIncompatible("Writer,OutputStream")
    @Override
    public OutputStream encodingStream(final Writer output) {
      return delegate.encodingStream(separatingWriter(output, separator, afterEveryChars));
    }

    @Override
    void encodeTo(Appendable target, byte[] bytes, int off, int len) throws IOException {
      delegate.encodeTo(separatingAppendable(target, separator, afterEveryChars), bytes, off, len);
    }

    @Override
    int maxDecodedSize(int chars) {
      return delegate.maxDecodedSize(chars);
    }

    @Override
    int decodeTo(byte[] target, CharSequence chars) throws DecodingException {
      return delegate.decodeTo(target, separatorChars.removeFrom(chars));
    }

    @GwtIncompatible("Reader,InputStream")
    @Override
    public InputStream decodingStream(final Reader reader) {
      return delegate.decodingStream(ignoringReader(reader, separatorChars));
    }

    @Override
    public BaseEncoding omitPadding() {
      return delegate.omitPadding().withSeparator(separator, afterEveryChars);
    }

    @Override
    public BaseEncoding withPadChar(char padChar) {
      return delegate.withPadChar(padChar).withSeparator(separator, afterEveryChars);
    }

    @Override
    public BaseEncoding withSeparator(String separator, int afterEveryChars) {
      throw new UnsupportedOperationException("Already have a separator");
    }

    @Override
    public BaseEncoding upperCase() {
      return delegate.upperCase().withSeparator(separator, afterEveryChars);
    }

    @Override
    public BaseEncoding lowerCase() {
      return delegate.lowerCase().withSeparator(separator, afterEveryChars);
    }

    @Override
    public String toString() {
      return delegate.toString() +
          ".withSeparator(\"" + separator + "\", " + afterEveryChars + ")";
    }
  }
}

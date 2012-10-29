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
import static com.google.common.math.IntMath.divide;
import static com.google.common.math.IntMath.log2;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.UNNECESSARY;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.Arrays;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * A binary encoding scheme for translating between byte sequences and ASCII strings. This class
 * includes several constants for encoding schemes specified by <a
 * href="http://tools.ietf.org/html/rfc4648">RFC 4648</a>.
 *
 * <p>All instances of this class are immutable, so they may be stored safely as static constants.
 *
 * @author Louis Wasserman
 * @since 14.0
 */
@Beta
@GwtCompatible
public abstract class BaseEncoding {
  BaseEncoding() {}

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
  public abstract String encode(byte[] bytes, int off, int len);

  // TODO(user): document the extent of leniency, probably after adding ignore(CharMatcher)

  /**
   * Decodes the specified character sequence, and returns the resulting {@code byte[]}.
   * This is the inverse operation to {@link #encode(byte[])}.
   *
   * @throws IllegalArgumentException if the input is not a valid encoded string according to this
   *         encoding.
   */
  public abstract byte[] decode(CharSequence chars);

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

  private static final BaseEncoding BASE64 = new StandardBaseEncoding(
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/", '=');

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

  private static final BaseEncoding BASE64_URL = new StandardBaseEncoding(
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_", '=');

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
      new StandardBaseEncoding("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567", '=');

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
      new StandardBaseEncoding("0123456789ABCDEFGHIJKLMNOPQRSTUV", '=');

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

  private static final BaseEncoding BASE16 = new StandardBaseEncoding("0123456789ABCDEF", null);

  /**
   * The "base16" encoding specified by <a
   * href="http://tools.ietf.org/html/rfc4648#section-8">RFC 4648 section 8</a>, Base 16 Encoding.
   * (This is the same as the base 16 encoding from <a
   * href="http://tools.ietf.org/html/rfc3548#section-6">RFC 3548</a>.)
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

  static final class StandardBaseEncoding extends BaseEncoding {
    private final String alphabet;
    private final int alphabetMask;
    private final int bitsPerChar;
    private final int charsPerChunk;
    private final int bytesPerChunk;

    @Nullable
    private final Character paddingChar;

    private final byte[] decodabet;

    // The lengths mod charsPerChunk that a non-padded encoded string might possibly have.
    private final boolean[] validPadding;

    StandardBaseEncoding(String alphabet, @Nullable Character paddingChar) {
      this.alphabet = checkNotNull(alphabet);
      try {
        this.bitsPerChar = log2(alphabet.length(), UNNECESSARY);
      } catch (ArithmeticException e) {
        throw new IllegalArgumentException("Illegal alphabet length " + alphabet.length(), e);
      }

      /*
       * e.g. for base64, bitsPerChar == 6, charsPerChunk == 4, and bytesPerChunk == 3. This makes
       * for the smallest chunk size that still has charsPerChunk * bitsPerChar be a multiple of 8.
       */
      int gcd = Math.min(8, Integer.lowestOneBit(bitsPerChar));
      this.charsPerChunk = 8 / gcd;
      this.bytesPerChunk = bitsPerChar / gcd;

      this.alphabetMask = alphabet.length() - 1;

      this.paddingChar = paddingChar;
      checkArgument(paddingChar == null || alphabet.indexOf(paddingChar.charValue()) == -1,
          "Padding character must not appear in alphabet");

      byte[] decodabet = new byte[128];
      Arrays.fill(decodabet, (byte) -1);
      for (int i = 0; i < alphabet.length(); i++) {
        char c = alphabet.charAt(i);
        decodabet[c] = (byte) i;
      }
      this.decodabet = decodabet;

      boolean[] validPadding = new boolean[charsPerChunk];
      for (int i = 0; i < bytesPerChunk; i++) {
        int chars = divide(i * 8, bitsPerChar, CEILING);
        validPadding[chars] = true;
      }
      this.validPadding = validPadding;
    }

    private CharMatcher paddingMatcher() {
      return (paddingChar == null) ? CharMatcher.NONE : CharMatcher.is(paddingChar.charValue());
    }

    @Override
    public String encode(byte[] bytes, int off, int len) {
      checkNotNull(bytes);
      checkPositionIndexes(off, off + len, bytes.length);

      int outputLength = charsPerChunk * divide(len, bytesPerChunk, CEILING);
      StringBuilder builder = new StringBuilder(outputLength);

      int bitBuffer = 0;
      int bitBufferLength = 0;
      for (int i = off; i < off + len; i++) {
        bitBuffer <<= 8;
        bitBuffer |= bytes[i] & 0xFF;
        bitBufferLength += 8;
        while (bitBufferLength >= bitsPerChar) {
          int charIndex = (bitBuffer >>> (bitBufferLength - bitsPerChar)) & alphabetMask;
          builder.append(alphabet.charAt(charIndex));
          bitBufferLength -= bitsPerChar;
        }
      }

      if (bitBufferLength > 0) {
        int charIndex = (bitBuffer << (bitsPerChar - bitBufferLength)) & alphabetMask;
        builder.append(alphabet.charAt(charIndex));
        if (paddingChar != null) {
          while (builder.length() % charsPerChunk != 0) {
            builder.append(paddingChar.charValue());
          }
        }
      }

      return builder.toString();
    }

    @Override
    public byte[] decode(CharSequence chars) {
      checkNotNull(chars);

      int expectedOutputLength = bytesPerChunk * divide(chars.length(), charsPerChunk, CEILING);
      byte[] output = new byte[expectedOutputLength];
      int outputPosition = 0;

      int bitBuffer = 0;
      int bitBufferLength = 0;

      CharMatcher paddingMatcher = paddingMatcher();

      boolean hitPadding = false;
      for (int i = 0; i < chars.length(); i++) {
        char c = chars.charAt(i);
        if (paddingMatcher.matches(c)) {
          if (!hitPadding && (i <= 0 || !validPadding[i % charsPerChunk])) {
            throw new IllegalArgumentException("Padding cannot start at index " + i);
          }
          hitPadding = true;
        } else if (hitPadding) {
          throw new IllegalArgumentException(
              "Expected padding character but found '" + c + "' at index " + i);
        } else if (!CharMatcher.ASCII.matches(c) || decodabet[c] == -1) {
          throw new IllegalArgumentException("Unrecognized character: " + c);
        } else {
          bitBuffer <<= bitsPerChar;
          bitBuffer |= decodabet[c] & alphabetMask;
          bitBufferLength += bitsPerChar;

          if (bitBufferLength >= 8) {
            output[outputPosition++] = (byte) (bitBuffer >> (bitBufferLength - 8));
            bitBufferLength -= 8;
          }
        }
      }

      int charsOverChunk = chars.length() % charsPerChunk;
      if (!hitPadding && !validPadding[charsOverChunk]) {
        throw new IllegalArgumentException("Bad trailing characters \""
            + chars.subSequence(chars.length() - charsOverChunk, chars.length()) + "\"");
      }

      return extract(output, outputPosition);
    }

    private static byte[] extract(byte[] result, int length) {
      if (length == result.length) {
        return result;
      } else {
        byte[] trunc = new byte[length];
        System.arraycopy(result, 0, trunc, 0, length);
        return trunc;
      }
    }

    @Override
    public BaseEncoding omitPadding() {
      return (paddingChar == null) ? this : new StandardBaseEncoding(alphabet, null);
    }

    @Override
    public BaseEncoding withPadChar(char padChar) {
      if (8 % bitsPerChar == 0 || (paddingChar != null && paddingChar.charValue() == padChar)) {
        return this;
      } else if (alphabet.indexOf(padChar) != -1) {
        throw new IllegalArgumentException("Padding character '" + padChar +
            "'appears in alphabet");
      } else {
        return new StandardBaseEncoding(alphabet, padChar);
      }
    }

    @Override
    public BaseEncoding withSeparator(String separator, int afterEveryChars) {
      checkNotNull(separator);
      checkArgument(paddingMatcher().or(CharMatcher.anyOf(alphabet)).matchesNoneOf(separator),
          "Separator cannot contain alphabet or padding characters");
      return new SeparatedBaseEncoding(this, separator, afterEveryChars);
    }

    private static final CharMatcher ASCII_UPPER_CASE = CharMatcher.inRange('A', 'Z');
    private static final CharMatcher ASCII_NOT_UPPER_CASE =
        CharMatcher.ASCII.and(ASCII_UPPER_CASE.negate());
    private static final CharMatcher ASCII_LOWER_CASE = CharMatcher.inRange('a', 'z');
    private static final CharMatcher ASCII_NOT_LOWER_CASE =
        CharMatcher.ASCII.and(ASCII_LOWER_CASE.negate());

    @Override
    public BaseEncoding upperCase() {
      if (ASCII_UPPER_CASE.matchesAnyOf(alphabet) && ASCII_LOWER_CASE.matchesAnyOf(alphabet)) {
        throw new IllegalStateException("alphabet uses both upper and lower case characters");
      } else if (ASCII_NOT_LOWER_CASE.matchesAllOf(alphabet)) {
        // already all uppercase or caseless
        return this;
      } else {
        return new StandardBaseEncoding(Ascii.toUpperCase(alphabet), paddingChar);
      }
    }

    @Override
    public BaseEncoding lowerCase() {
      if (ASCII_UPPER_CASE.matchesAnyOf(alphabet) && ASCII_LOWER_CASE.matchesAnyOf(alphabet)) {
        throw new IllegalStateException("alphabet uses both upper and lower case characters");
      } else if (ASCII_NOT_UPPER_CASE.matchesAllOf(alphabet)) {
        // already all lowercase or caseless
        return this;
      } else {
        return new StandardBaseEncoding(Ascii.toLowerCase(alphabet), paddingChar);
      }
    }
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
    public String encode(byte[] bytes, int off, int len) {
      // TODO(user): delegate to the streaming implementation when we add it
      String unseparated = delegate.encode(bytes, off, len);
      return Joiner.on(separator).join(Splitter.fixedLength(afterEveryChars).split(unseparated));
    }

    @Override
    public byte[] decode(CharSequence chars) {
      return delegate.decode(separatorChars.removeFrom(chars));
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
  }
}

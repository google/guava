/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.base;

import static com.google.common.base.Preconditions.checkPositionIndexes;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/**
 * A set of low-level, high-performance static utility methods related to the UTF-8 character
 * encoding.
 *
 * <p>There are several variants of UTF-8. The one implemented by this class is the restricted
 * definition of UTF-8 introduced in Unicode 3.1, which mandates the rejection of overlong byte
 * sequences as well as rejection of 3-byte surrogate codepoint byte sequences. Note that the UTF-8
 * decoder included in Oracle's JDK has been modified to also reject "overlong" byte sequences, but
 * (as of 2011) still accepts 3-byte surrogate codepoint byte sequences.
 *
 * <p>The byte sequences considered valid by this class are exactly those that can be roundtrip
 * converted to Strings and back to bytes using the UTF-8 charset, without loss:
 *
 * <pre>{@code Arrays.equals(bytes, new String(bytes, "UTF-8").getBytes("UTF-8"))}</pre>
 *
 * <p>See the Unicode Standard,</br> Table 3-6. <em>UTF-8 Bit Distribution</em>,</br> Table 3-7.
 * <em>Well Formed UTF-8 Byte Sequences</em>.
 *
 * @author Martin Buchholz
 * @author Clément Roux
 * @since 16.0
 */
@Beta
@GwtCompatible
public final class Utf8 {
  private Utf8() {}

  /**
   * Returns the number of bytes in the UTF-8 encoded form of {@code sequence}. Assuming that
   * {@code sequence} is a string that contains valid code points, this method is faster and more
   * memory efficient than {@code string.getBytes(Charsets.UTF_8).length}.
   *
   * @throws IllegalArgumentException if {@code sequence} contains unpaired surrogates
   */
  public static int utf8Length(CharSequence sequence) {
    long utf8Length = 0;
    int charIndex = 0;
    int charLength = sequence.length();
    while (charIndex < charLength) {
      char c = sequence.charAt(charIndex);
      // From http://en.wikipedia.org/wiki/UTF-8#Description
      if (c < 0x80) {
        utf8Length += 1;
      } else if (c < 0x800) {
        utf8Length += 2;
      } else if (c < Character.MIN_SURROGATE || Character.MAX_SURROGATE < c) {
        utf8Length += 3;
      } else {
        // Expect a surrogate pair.
        int cp = Character.codePointAt(sequence, charIndex);
        if (cp < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
          // The pair starts with a low surrogate, or no character follows the
          // high surrogate.
          // We throw an IllegalArgumentException instead of a
          // CharacterCodingException because CharacterCodingException is a
          // checked exception.
          throw new IllegalArgumentException(
              "Unpaired surrogate at " + charIndex + " (" + sequence + ")");
        }
        utf8Length += 4;
        charIndex++;
      }
      charIndex++;
    }
    int result = (int) utf8Length;
    if (result != utf8Length) {
      throw new IllegalArgumentException("UTF-8 length does not fit in int: " + utf8Length);
    }
    return result;
  }

  /**
   * Returns whether the given byte array is a well-formed UTF-8 byte sequence.
   */
  public static boolean isValidUtf8(byte[] bytes) {
    return isValidUtf8(bytes, 0, bytes.length);
  }

  /**
   * Returns whether the given byte array slice is a well-formed UTF-8 byte sequence.
   *
   * @param bytes the input buffer
   * @param off the offset in the buffer of the first byte to read
   * @param len the maximum number of bytes to read from the buffer
   */
  public static boolean isValidUtf8(byte[] bytes, int off, int len) {
    int end = off + len;
    checkPositionIndexes(off, end, bytes.length);
    // Look for the first non-ASCII character.
    for (int i = off; i < end; i++) {
      if (bytes[i] < 0) {
        return isValidUtf8NonAscii(bytes, i, end);
      }
    }
    return true;
  }

  private static boolean isValidUtf8NonAscii(byte[] bytes, int off, int end) {
    int index = off;
    while (true) {
      int byte1;

      // Optimize for interior runs of ASCII bytes.
      do {
        if (index >= end) {
          return true;
        }
      } while ((byte1 = bytes[index++]) >= 0);

      if (byte1 < (byte) 0xE0) {
        // Two-byte form.
        if (index == end) {
          return false;
        }
        // Simultaneously check for illegal trailing-byte in leading position
        // and overlong 2-byte form.
        if (byte1 < (byte) 0xC2 || bytes[index++] > (byte) 0xBF) {
          return false;
        }
      } else if (byte1 < (byte) 0xF0) {
        // Three-byte form.
        if (index + 1 >= end) {
          return false;
        }
        int byte2 = bytes[index++];
        if (byte2 > (byte) 0xBF
            // Overlong? 5 most significant bits must not all be zero.
            || (byte1 == (byte) 0xE0 && byte2 < (byte) 0xA0)
            // Check for illegal surrogate codepoints.
            || (byte1 == (byte) 0xED && (byte) 0xA0 <= byte2)
            // Third byte trailing-byte test.
            || bytes[index++] > (byte) 0xBF) {
          return false;
        }
      } else {
        // Four-byte form.
        if (index + 2 >= end) {
          return false;
        }
        int byte2 = bytes[index++];
        if (byte2 > (byte) 0xBF
            // Check that 1 <= plane <= 16. Tricky optimized form of:
            // if (byte1 > (byte) 0xF4
            //     || byte1 == (byte) 0xF0 && byte2 < (byte) 0x90
            //     || byte1 == (byte) 0xF4 && byte2 > (byte) 0x8F)
            || (((byte1 << 28) + (byte2 - (byte) 0x90)) >> 30) != 0
            // Third byte trailing-byte test
            || bytes[index++] > (byte) 0xBF
            // Fourth byte trailing-byte test
            || bytes[index++] > (byte) 0xBF) {
          return false;
        }
      }
    }
  }
}

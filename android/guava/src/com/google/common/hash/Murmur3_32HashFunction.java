/*
 * Copyright (C) 2011 The Guava Authors
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

/*
 * MurmurHash3 was written by Austin Appleby, and is placed in the public
 * domain. The author hereby disclaims copyright to this source code.
 */

/*
 * Source:
 * http://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp
 * (Modified to adapt to Guava coding conventions and to use the HashFunction interface)
 */

package com.google.common.hash;

import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.primitives.UnsignedBytes.toInt;

import com.google.common.base.Charsets;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import javax.annotation.CheckForNull;

/**
 * See MurmurHash3_x86_32 in <a
 * href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">the C++
 * implementation</a>.
 *
 * @author Austin Appleby
 * @author Dimitris Andreou
 * @author Kurt Alfred Kluever
 */
@Immutable
@ElementTypesAreNonnullByDefault
final class Murmur3_32HashFunction extends AbstractHashFunction implements Serializable {
  static final HashFunction MURMUR3_32 =
      new Murmur3_32HashFunction(0, /* supplementaryPlaneFix= */ false);
  static final HashFunction MURMUR3_32_FIXED =
      new Murmur3_32HashFunction(0, /* supplementaryPlaneFix= */ true);

  // We can include the non-BMP fix here because Hashing.goodFastHash stresses that the hash is a
  // temporary-use one. Therefore it shouldn't be persisted.
  static final HashFunction GOOD_FAST_HASH_32 =
      new Murmur3_32HashFunction(Hashing.GOOD_FAST_HASH_SEED, /* supplementaryPlaneFix= */ true);

  private static final int CHUNK_SIZE = 4;

  private static final int C1 = 0xcc9e2d51;
  private static final int C2 = 0x1b873593;

  private final int seed;
  private final boolean supplementaryPlaneFix;

  Murmur3_32HashFunction(int seed, boolean supplementaryPlaneFix) {
    this.seed = seed;
    this.supplementaryPlaneFix = supplementaryPlaneFix;
  }

  @Override
  public int bits() {
    return 32;
  }

  @Override
  public Hasher newHasher() {
    return new Murmur3_32Hasher(seed);
  }

  @Override
  public String toString() {
    return "Hashing.murmur3_32(" + seed + ")";
  }

  @Override
  public boolean equals(@CheckForNull Object object) {
    if (object instanceof Murmur3_32HashFunction) {
      Murmur3_32HashFunction other = (Murmur3_32HashFunction) object;
      return seed == other.seed && supplementaryPlaneFix == other.supplementaryPlaneFix;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() ^ seed;
  }

  @Override
  public HashCode hashInt(int input) {
    int k1 = mixK1(input);
    int h1 = mixH1(seed, k1);

    return fmix(h1, Ints.BYTES);
  }

  @Override
  public HashCode hashLong(long input) {
    int low = (int) input;
    int high = (int) (input >>> 32);

    int k1 = mixK1(low);
    int h1 = mixH1(seed, k1);

    k1 = mixK1(high);
    h1 = mixH1(h1, k1);

    return fmix(h1, Longs.BYTES);
  }

  @Override
  public HashCode hashUnencodedChars(CharSequence input) {
    int h1 = seed;

    // step through the CharSequence 2 chars at a time
    for (int i = 1; i < input.length(); i += 2) {
      int k1 = input.charAt(i - 1) | (input.charAt(i) << 16);
      k1 = mixK1(k1);
      h1 = mixH1(h1, k1);
    }

    // deal with any remaining characters
    if ((input.length() & 1) == 1) {
      int k1 = input.charAt(input.length() - 1);
      k1 = mixK1(k1);
      h1 ^= k1;
    }

    return fmix(h1, Chars.BYTES * input.length());
  }

  @SuppressWarnings("deprecation") // need to use Charsets for Android tests to pass
  @Override
  public HashCode hashString(CharSequence input, Charset charset) {
    if (Charsets.UTF_8.equals(charset)) {
      int utf16Length = input.length();
      int h1 = seed;
      int i = 0;
      int len = 0;

      // This loop optimizes for pure ASCII.
      while (i + 4 <= utf16Length) {
        char c0 = input.charAt(i);
        char c1 = input.charAt(i + 1);
        char c2 = input.charAt(i + 2);
        char c3 = input.charAt(i + 3);
        if (c0 < 0x80 && c1 < 0x80 && c2 < 0x80 && c3 < 0x80) {
          int k1 = c0 | (c1 << 8) | (c2 << 16) | (c3 << 24);
          k1 = mixK1(k1);
          h1 = mixH1(h1, k1);
          i += 4;
          len += 4;
        } else {
          break;
        }
      }

      long buffer = 0;
      int shift = 0;
      for (; i < utf16Length; i++) {
        char c = input.charAt(i);
        if (c < 0x80) {
          buffer |= (long) c << shift;
          shift += 8;
          len++;
        } else if (c < 0x800) {
          buffer |= charToTwoUtf8Bytes(c) << shift;
          shift += 16;
          len += 2;
        } else if (c < Character.MIN_SURROGATE || c > Character.MAX_SURROGATE) {
          buffer |= charToThreeUtf8Bytes(c) << shift;
          shift += 24;
          len += 3;
        } else {
          int codePoint = Character.codePointAt(input, i);
          if (codePoint == c) {
            // not a valid code point; let the JDK handle invalid Unicode
            return hashBytes(input.toString().getBytes(charset));
          }
          i++;
          buffer |= codePointToFourUtf8Bytes(codePoint) << shift;
          if (supplementaryPlaneFix) { // bug compatibility: earlier versions did not have this add
            shift += 32;
          }
          len += 4;
        }

        if (shift >= 32) {
          int k1 = mixK1((int) buffer);
          h1 = mixH1(h1, k1);
          buffer = buffer >>> 32;
          shift -= 32;
        }
      }

      int k1 = mixK1((int) buffer);
      h1 ^= k1;
      return fmix(h1, len);
    } else {
      return hashBytes(input.toString().getBytes(charset));
    }
  }

  @Override
  public HashCode hashBytes(byte[] input, int off, int len) {
    checkPositionIndexes(off, off + len, input.length);
    int h1 = seed;
    int i;
    for (i = 0; i + CHUNK_SIZE <= len; i += CHUNK_SIZE) {
      int k1 = mixK1(getIntLittleEndian(input, off + i));
      h1 = mixH1(h1, k1);
    }

    int k1 = 0;
    for (int shift = 0; i < len; i++, shift += 8) {
      k1 ^= toInt(input[off + i]) << shift;
    }
    h1 ^= mixK1(k1);
    return fmix(h1, len);
  }

  private static int getIntLittleEndian(byte[] input, int offset) {
    return Ints.fromBytes(input[offset + 3], input[offset + 2], input[offset + 1], input[offset]);
  }

  private static int mixK1(int k1) {
    k1 *= C1;
    k1 = Integer.rotateLeft(k1, 15);
    k1 *= C2;
    return k1;
  }

  private static int mixH1(int h1, int k1) {
    h1 ^= k1;
    h1 = Integer.rotateLeft(h1, 13);
    h1 = h1 * 5 + 0xe6546b64;
    return h1;
  }

  // Finalization mix - force all bits of a hash block to avalanche
  private static HashCode fmix(int h1, int length) {
    h1 ^= length;
    h1 ^= h1 >>> 16;
    h1 *= 0x85ebca6b;
    h1 ^= h1 >>> 13;
    h1 *= 0xc2b2ae35;
    h1 ^= h1 >>> 16;
    return HashCode.fromInt(h1);
  }

  @CanIgnoreReturnValue
  private static final class Murmur3_32Hasher extends AbstractHasher {
    private int h1;
    private long buffer;
    private int shift;
    private int length;
    private boolean isDone;

    Murmur3_32Hasher(int seed) {
      this.h1 = seed;
      this.length = 0;
      isDone = false;
    }

    private void update(int nBytes, long update) {
      // 1 <= nBytes <= 4
      buffer |= (update & 0xFFFFFFFFL) << shift;
      shift += nBytes * 8;
      length += nBytes;

      if (shift >= 32) {
        h1 = mixH1(h1, mixK1((int) buffer));
        buffer >>>= 32;
        shift -= 32;
      }
    }

    @Override
    public Hasher putByte(byte b) {
      update(1, b & 0xFF);
      return this;
    }

    @Override
    public Hasher putBytes(byte[] bytes, int off, int len) {
      checkPositionIndexes(off, off + len, bytes.length);
      int i;
      for (i = 0; i + 4 <= len; i += 4) {
        update(4, getIntLittleEndian(bytes, off + i));
      }
      for (; i < len; i++) {
        putByte(bytes[off + i]);
      }
      return this;
    }

    @Override
    public Hasher putBytes(ByteBuffer buffer) {
      ByteOrder bo = buffer.order();
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      while (buffer.remaining() >= 4) {
        putInt(buffer.getInt());
      }
      while (buffer.hasRemaining()) {
        putByte(buffer.get());
      }
      buffer.order(bo);
      return this;
    }

    @Override
    public Hasher putInt(int i) {
      update(4, i);
      return this;
    }

    @Override
    public Hasher putLong(long l) {
      update(4, (int) l);
      update(4, l >>> 32);
      return this;
    }

    @Override
    public Hasher putChar(char c) {
      update(2, c);
      return this;
    }

    @SuppressWarnings("deprecation") // need to use Charsets for Android tests to pass
    @Override
    public Hasher putString(CharSequence input, Charset charset) {
      if (Charsets.UTF_8.equals(charset)) {
        int utf16Length = input.length();
        int i = 0;

        // This loop optimizes for pure ASCII.
        while (i + 4 <= utf16Length) {
          char c0 = input.charAt(i);
          char c1 = input.charAt(i + 1);
          char c2 = input.charAt(i + 2);
          char c3 = input.charAt(i + 3);
          if (c0 < 0x80 && c1 < 0x80 && c2 < 0x80 && c3 < 0x80) {
            update(4, c0 | (c1 << 8) | (c2 << 16) | (c3 << 24));
            i += 4;
          } else {
            break;
          }
        }

        for (; i < utf16Length; i++) {
          char c = input.charAt(i);
          if (c < 0x80) {
            update(1, c);
          } else if (c < 0x800) {
            update(2, charToTwoUtf8Bytes(c));
          } else if (c < Character.MIN_SURROGATE || c > Character.MAX_SURROGATE) {
            update(3, charToThreeUtf8Bytes(c));
          } else {
            int codePoint = Character.codePointAt(input, i);
            if (codePoint == c) {
              // fall back to JDK getBytes instead of trying to handle invalid surrogates ourselves
              putBytes(input.subSequence(i, utf16Length).toString().getBytes(charset));
              return this;
            }
            i++;
            update(4, codePointToFourUtf8Bytes(codePoint));
          }
        }
        return this;
      } else {
        return super.putString(input, charset);
      }
    }

    @Override
    public HashCode hash() {
      checkState(!isDone);
      isDone = true;
      h1 ^= mixK1((int) buffer);
      return fmix(h1, length);
    }
  }

  private static long codePointToFourUtf8Bytes(int codePoint) {
    // codePoint has at most 21 bits
    return ((0xFL << 4) | (codePoint >>> 18))
        | ((0x80L | (0x3F & (codePoint >>> 12))) << 8)
        | ((0x80L | (0x3F & (codePoint >>> 6))) << 16)
        | ((0x80L | (0x3F & codePoint)) << 24);
  }

  private static long charToThreeUtf8Bytes(char c) {
    return ((0x7L << 5) | (c >>> 12))
        | ((0x80 | (0x3F & (c >>> 6))) << 8)
        | ((0x80 | (0x3F & c)) << 16);
  }

  private static long charToTwoUtf8Bytes(char c) {
    // c has at most 11 bits
    return ((0x3L << 6) | (c >>> 6)) | ((0x80 | (0x3F & c)) << 8);
  }

  private static final long serialVersionUID = 0L;
}

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

package com.google.common.hash;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

import java.security.MessageDigest;

/**
 * An immutable hash code of arbitrary bit length.
 *
 * @author Dimitris Andreou
 * @since 11.0
 */
@Beta
public abstract class HashCode {
  HashCode() {}

  /**
   * Returns the first four bytes of {@linkplain #asBytes() this hashcode's bytes}, converted to
   * an {@code int} value in little-endian order.
   */
  public abstract int asInt();

  /**
   * Returns the first eight bytes of {@linkplain #asBytes() this hashcode's bytes}, converted to
   * a {@code long} value in little-endian order.
   *
   * @throws IllegalStateException if {@code bits() < 64}
   */
  public abstract long asLong();

  /**
   * Returns the value of this hash code as a byte array. The caller may modify the byte array;
   * changes to it will <i>not</i> be reflected in this {@code HashCode} object or any other arrays
   * returned by this method.
   */
  // TODO(user): consider ByteString here, when that is available
  public abstract byte[] asBytes();

  /**
   * Copies bytes from this hash code into {@code dest}.
   *
   * @param dest the byte array into which the hash code will be written
   * @param offset the start offset in the data
   * @param maxLength the maximum number of bytes to write
   * @return the number of bytes written to {@code dest}
   * @throws IndexOutOfBoundsException if there is not enough room in {@code dest}
   */
  public int writeBytesTo(byte[] dest, int offset, int maxLength) {
    byte[] hash = asBytes();
    maxLength = Ints.min(maxLength, hash.length);
    Preconditions.checkPositionIndexes(offset, offset + maxLength, dest.length);
    System.arraycopy(hash, 0, dest, offset, maxLength);
    return maxLength;
  }

  /**
   * Returns the number of bits in this hash code; a positive multiple of 32.
   */
  public abstract int bits();

  @Override public boolean equals(Object object) {
    if (object instanceof HashCode) {
      HashCode that = (HashCode) object;
      // Undocumented: this is a non-short-circuiting equals(), in case this is a cryptographic
      // hash code, in which case we don't want to leak timing information
      return MessageDigest.isEqual(this.asBytes(), that.asBytes());
    }
    return false;
  }

  /**
   * Returns a "Java hash code" for this {@code HashCode} instance; this is well-defined
   * (so, for example, you can safely put {@code HashCode} instances into a {@code
   * HashSet}) but is otherwise probably not what you want to use.
   */
  @Override public int hashCode() {
    /*
     * As long as the hash function that produced this isn't of horrible quality, this
     * won't be of horrible quality either.
     */
    return asInt();
  }

  /**
   * Returns a string containing each byte of {@link #asBytes}, in order, as a two-digit unsigned
   * hexadecimal number in lower case.
   *
   * <p>Note that if the output is considered to be a single hexadecimal number, this hash code's
   * bytes are the <i>big-endian</i> representation of that number. This may be surprising since
   * everything else in the hashing API uniformly treats multibyte values as little-endian. But
   * this format conveniently matches that of utilities such as the UNIX {@code md5sum} command.
   */
  @Override public String toString() {
    byte[] bytes = asBytes();
    // TODO(user): Use c.g.common.base.ByteArrays once it is open sourced.
    StringBuilder sb = new StringBuilder(2 * bytes.length);
    for (byte b : bytes) {
      sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
    }
    return sb.toString();
  }

  private static final char[] hexDigits = "0123456789abcdef".toCharArray();
}

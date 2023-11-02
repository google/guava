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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * An object which can receive a stream of primitive values.
 *
 * @author Kevin Bourrillion
 * @since 12.0 (in 11.0 as {@code Sink})
 */
@Beta
@ElementTypesAreNonnullByDefault
public interface PrimitiveSink {
  /**
   * Puts a byte into this sink.
   *
   * @param b a byte
   * @return this instance
   */
  @CanIgnoreReturnValue
  PrimitiveSink putByte(byte b);

  /**
   * Puts an array of bytes into this sink.
   *
   * @param bytes a byte array
   * @return this instance
   */
  @CanIgnoreReturnValue
  PrimitiveSink putBytes(byte[] bytes);

  /**
   * Puts a chunk of an array of bytes into this sink. {@code bytes[off]} is the first byte written,
   * {@code bytes[off + len - 1]} is the last.
   *
   * @param bytes a byte array
   * @param off the start offset in the array
   * @param len the number of bytes to write
   * @return this instance
   * @throws IndexOutOfBoundsException if {@code off < 0} or {@code off + len > bytes.length} or
   *     {@code len < 0}
   */
  @CanIgnoreReturnValue
  PrimitiveSink putBytes(byte[] bytes, int off, int len);

  /**
   * Puts the remaining bytes of a byte buffer into this sink. {@code bytes.position()} is the first
   * byte written, {@code bytes.limit() - 1} is the last. The position of the buffer will be equal
   * to the limit when this method returns.
   *
   * @param bytes a byte buffer
   * @return this instance
   * @since 23.0
   */
  @CanIgnoreReturnValue
  PrimitiveSink putBytes(ByteBuffer bytes);

  /** Puts a short into this sink. */
  @CanIgnoreReturnValue
  PrimitiveSink putShort(short s);

  /** Puts an int into this sink. */
  @CanIgnoreReturnValue
  PrimitiveSink putInt(int i);

  /** Puts a long into this sink. */
  @CanIgnoreReturnValue
  PrimitiveSink putLong(long l);

  /** Puts a float into this sink. */
  @CanIgnoreReturnValue
  PrimitiveSink putFloat(float f);

  /** Puts a double into this sink. */
  @CanIgnoreReturnValue
  PrimitiveSink putDouble(double d);

  /** Puts a boolean into this sink. */
  @CanIgnoreReturnValue
  PrimitiveSink putBoolean(boolean b);

  /** Puts a character into this sink. */
  @CanIgnoreReturnValue
  PrimitiveSink putChar(char c);

  /**
   * Puts each 16-bit code unit from the {@link CharSequence} into this sink.
   *
   * <p><b>Warning:</b> This method will produce different output than most other languages do when
   * running on the equivalent input. For cross-language compatibility, use {@link #putString},
   * usually with a charset of UTF-8. For other use cases, use {@code putUnencodedChars}.
   *
   * @since 15.0 (since 11.0 as putString(CharSequence))
   */
  @CanIgnoreReturnValue
  PrimitiveSink putUnencodedChars(CharSequence charSequence);

  /**
   * Puts a string into this sink using the given charset.
   *
   * <p><b>Warning:</b> This method, which reencodes the input before processing it, is useful only
   * for cross-language compatibility. For other use cases, prefer {@link #putUnencodedChars}, which
   * is faster, produces the same output across Java releases, and processes every {@code char} in
   * the input, even if some are invalid.
   */
  @CanIgnoreReturnValue
  PrimitiveSink putString(CharSequence charSequence, Charset charset);
}

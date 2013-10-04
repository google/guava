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

import java.nio.charset.Charset;

/**
 * An object which can receive a stream of primitive values.
 *
 * @author Kevin Bourrillion
 * @since 12.0 (in 11.0 as {@code Sink})
 */
@Beta
public interface PrimitiveSink {
  /**
   * Puts a byte into this sink.
   *
   * @param b a byte
   * @return this instance
   */
  PrimitiveSink putByte(byte b);

  /**
   * Puts an array of bytes into this sink.
   *
   * @param bytes a byte array
   * @return this instance
   */
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
   *   {@code len < 0}
   */
  PrimitiveSink putBytes(byte[] bytes, int off, int len);

  /**
   * Puts a short into this sink.
   */
  PrimitiveSink putShort(short s);

  /**
   * Puts an int into this sink.
   */
  PrimitiveSink putInt(int i);

  /**
   * Puts a long into this sink.
   */
  PrimitiveSink putLong(long l);

  /**
   * Puts a float into this sink.
   */
  PrimitiveSink putFloat(float f);

  /**
   * Puts a double into this sink.
   */
  PrimitiveSink putDouble(double d);

  /**
   * Puts a boolean into this sink.
   */
  PrimitiveSink putBoolean(boolean b);

  /**
   * Puts a character into this sink.
   */
  PrimitiveSink putChar(char c);

  /**
   * Puts each 16-bit code unit from the {@link CharSequence} into this sink.
   *
   * @since 15.0 (since 11.0 as putString(CharSequence))
   */
  PrimitiveSink putUnencodedChars(CharSequence charSequence);

  /**
   * Puts a string into this sink using the given charset.
   */
  PrimitiveSink putString(CharSequence charSequence, Charset charset);
}

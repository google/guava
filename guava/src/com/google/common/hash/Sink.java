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
 * @since 11.0
 */
@Beta
public interface Sink {
  /**
   * Puts a byte into this sink.
   *
   * @param b a byte
   * @return this instance
   */
  Sink putByte(byte b);

  /**
   * Puts an array of bytes into this sink.
   *
   * @param bytes a byte array
   * @return this instance
   */
  Sink putBytes(byte[] bytes);
  
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
  Sink putBytes(byte[] bytes, int off, int len);

  /**
   * Puts a short into this sink.
   */
  Sink putShort(short s);

  /**
   * Puts an int into this sink.
   */
  Sink putInt(int i);

  /**
   * Puts a long into this sink.
   */
  Sink putLong(long l);

  /**
   * Puts a float into this sink.
   */
  Sink putFloat(float f);

  /**
   * Puts a double into this sink.
   */
  Sink putDouble(double d);

  /**
   * Puts a boolean into this sink.
   */
  Sink putBoolean(boolean b);

  /**
   * Puts a character into this sink.
   */
  Sink putChar(char c);

  /**
   * Puts a string into this sink.
   */
  Sink putString(CharSequence charSequence);

  /**
   * Puts a string into this sink using the given charset.
   */
  Sink putString(CharSequence charSequence, Charset charset);
}

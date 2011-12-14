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
 * A {@link Sink} that can compute a hash code after reading the input. Each hasher should
 * translate all multibyte values ({@link #putInt(int)}, {@link #putLong(long)}, etc) to bytes 
 * in little-endian order.
 *
 * @author Kevin Bourrillion
 * @since 11.0
 */
@Beta
public interface Hasher extends Sink {
  @Override Hasher putByte(byte b);
  @Override Hasher putBytes(byte[] bytes);
  @Override Hasher putBytes(byte[] bytes, int off, int len);
  @Override Hasher putShort(short s);
  @Override Hasher putInt(int i);
  @Override Hasher putLong(long l);
  /**
   * Equivalent to {@code putInt(Float.floatToRawIntBits(f))}.
   */
  @Override Hasher putFloat(float f);
  /**
   * Equivalent to {@code putLong(Double.doubleToRawLongBits(d))}.
   */
  @Override Hasher putDouble(double d);
  /**
   * Equivalent to {@code putByte(b ? (byte) 1 : (byte) 0)}.
   */
  @Override Hasher putBoolean(boolean b);
  @Override Hasher putChar(char c);
  /**
   * Equivalent to {@code putBytes(charSequence.toString().getBytes(Charsets.UTF_16LE)}.
   */
  @Override Hasher putString(CharSequence charSequence);
  /**
   * Equivalent to {@code putBytes(charSequence.toString().getBytes(charset)}.
   */
  @Override Hasher putString(CharSequence charSequence, Charset charset);

  /**
   * A simple convenience for {@code funnel.funnel(object, this)}.
   */
  <T> Hasher putObject(T instance, Funnel<? super T> funnel);

  /**
   * Computes a hash code based on the data that have been provided to this hasher. The result is
   * unspecified if this method is called more than once on the same instance. 
   */
  HashCode hash();
}

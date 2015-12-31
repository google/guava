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

import javax.annotation.CheckReturnValue;

/**
 * A {@link PrimitiveSink} that can compute a hash code after reading the input. Each hasher should
 * translate all multibyte values ({@link #putInt(int)}, {@link #putLong(long)}, etc) to bytes
 * in little-endian order.
 *
 * <p><b>Warning:</b> The result of calling any methods after calling {@link #hash} is undefined.
 *
 * <p><b>Warning:</b> Using a specific character encoding when hashing a {@link CharSequence} with
 * {@link #putString(CharSequence, Charset)} is generally only useful for cross-language
 * compatibility (otherwise prefer {@link #putUnencodedChars}). However, the character encodings
 * must be identical across languages. Also beware that {@link Charset} definitions may occasionally
 * change between Java releases.
 *
 * <p><b>Warning:</b> Chunks of data that are put into the {@link Hasher} are not delimited.
 * The resulting {@link HashCode} is dependent only on the bytes inserted, and the order in which
 * they were inserted, not how those bytes were chunked into discrete put() operations. For example,
 * the following three expressions all generate colliding hash codes: <pre>   {@code
 *
 *   newHasher().putByte(b1).putByte(b2).putByte(b3).hash()
 *   newHasher().putByte(b1).putBytes(new byte[] { b2, b3 }).hash()
 *   newHasher().putBytes(new byte[] { b1, b2, b3 }).hash()}</pre>
 *
 * <p>If you wish to avoid this, you should either prepend or append the size of each chunk. Keep in
 * mind that when dealing with char sequences, the encoded form of two concatenated char sequences
 * is not equivalent to the concatenation of their encoded form. Therefore,
 * {@link #putString(CharSequence, Charset)} should only be used consistently with <i>complete</i>
 * sequences and not broken into chunks.
 *
 * @author Kevin Bourrillion
 * @since 11.0
 */
@Beta
public interface Hasher extends PrimitiveSink {
  @Override
  Hasher putByte(byte b);

  @Override
  Hasher putBytes(byte[] bytes);

  @Override
  Hasher putBytes(byte[] bytes, int off, int len);

  @Override
  Hasher putShort(short s);

  @Override
  Hasher putInt(int i);

  @Override
  Hasher putLong(long l);

  /**
   * Equivalent to {@code putInt(Float.floatToRawIntBits(f))}.
   */
  @Override
  Hasher putFloat(float f);

  /**
   * Equivalent to {@code putLong(Double.doubleToRawLongBits(d))}.
   */
  @Override
  Hasher putDouble(double d);

  /**
   * Equivalent to {@code putByte(b ? (byte) 1 : (byte) 0)}.
   */
  @Override
  Hasher putBoolean(boolean b);

  @Override
  Hasher putChar(char c);

  /**
   * Equivalent to processing each {@code char} value in the {@code CharSequence}, in order.
   * The input must not be updated while this method is in progress.
   *
   * @since 15.0 (since 11.0 as putString(CharSequence)).
   */
  @Override
  Hasher putUnencodedChars(CharSequence charSequence);

  /**
   * Equivalent to {@code putBytes(charSequence.toString().getBytes(charset))}.
   */
  @Override
  Hasher putString(CharSequence charSequence, Charset charset);

  /**
   * A simple convenience for {@code funnel.funnel(object, this)}.
   */
  <T> Hasher putObject(T instance, Funnel<? super T> funnel);

  /**
   * Computes a hash code based on the data that have been provided to this hasher. The result is
   * unspecified if this method is called more than once on the same instance.
   */
  @CheckReturnValue
  HashCode hash();

  /**
   * {@inheritDoc}
   *
   * @deprecated This returns {@link Object#hashCode()}; you almost certainly mean to call
   *     {@code hash().asInt()}.
   */
  @Override
  @Deprecated
  int hashCode();
}

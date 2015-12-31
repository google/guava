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

import java.nio.charset.Charset;

/**
 * An abstract hasher, implementing {@link #putBoolean(boolean)}, {@link #putDouble(double)},
 * {@link #putFloat(float)}, {@link #putUnencodedChars(CharSequence)}, and
 * {@link #putString(CharSequence, Charset)} as prescribed by {@link Hasher}.
 *
 * @author Dimitris Andreou
 */
abstract class AbstractHasher implements Hasher {
  @Override
  public final Hasher putBoolean(boolean b) {
    return putByte(b ? (byte) 1 : (byte) 0);
  }

  @Override
  public final Hasher putDouble(double d) {
    return putLong(Double.doubleToRawLongBits(d));
  }

  @Override
  public final Hasher putFloat(float f) {
    return putInt(Float.floatToRawIntBits(f));
  }

  @Override
  public Hasher putUnencodedChars(CharSequence charSequence) {
    for (int i = 0, len = charSequence.length(); i < len; i++) {
      putChar(charSequence.charAt(i));
    }
    return this;
  }

  @Override
  public Hasher putString(CharSequence charSequence, Charset charset) {
    return putBytes(charSequence.toString().getBytes(charset));
  }
}

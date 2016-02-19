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

import com.google.common.base.Preconditions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Skeleton implementation of {@link HashFunction}, appropriate for non-streaming algorithms. All
 * the hash computation done using {@linkplain #newHasher()} are delegated to the
 * {@linkplain #hashBytes(byte[], int, int)} method.
 *
 * @author Dimitris Andreou
 */
abstract class AbstractNonStreamingHashFunction implements HashFunction {
  @Override
  public Hasher newHasher() {
    return new BufferingHasher(32);
  }

  @Override
  public Hasher newHasher(int expectedInputSize) {
    Preconditions.checkArgument(expectedInputSize >= 0);
    return new BufferingHasher(expectedInputSize);
  }

  @Override
  public <T> HashCode hashObject(T instance, Funnel<? super T> funnel) {
    return newHasher().putObject(instance, funnel).hash();
  }

  @Override
  public HashCode hashUnencodedChars(CharSequence input) {
    int len = input.length();
    Hasher hasher = newHasher(len * 2);
    for (int i = 0; i < len; i++) {
      hasher.putChar(input.charAt(i));
    }
    return hasher.hash();
  }

  @Override
  public HashCode hashString(CharSequence input, Charset charset) {
    return hashBytes(input.toString().getBytes(charset));
  }

  @Override
  public HashCode hashInt(int input) {
    return newHasher(4).putInt(input).hash();
  }

  @Override
  public HashCode hashLong(long input) {
    return newHasher(8).putLong(input).hash();
  }

  @Override
  public HashCode hashBytes(byte[] input) {
    return hashBytes(input, 0, input.length);
  }

  /**
   * In-memory stream-based implementation of Hasher.
   */
  private final class BufferingHasher extends AbstractHasher {
    final ExposedByteArrayOutputStream stream;
    static final int BOTTOM_BYTE = 0xFF;

    BufferingHasher(int expectedInputSize) {
      this.stream = new ExposedByteArrayOutputStream(expectedInputSize);
    }

    @Override
    public Hasher putByte(byte b) {
      stream.write(b);
      return this;
    }

    @Override
    public Hasher putBytes(byte[] bytes) {
      try {
        stream.write(bytes);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    @Override
    public Hasher putBytes(byte[] bytes, int off, int len) {
      stream.write(bytes, off, len);
      return this;
    }

    @Override
    public Hasher putShort(short s) {
      stream.write(s & BOTTOM_BYTE);
      stream.write((s >>> 8) & BOTTOM_BYTE);
      return this;
    }

    @Override
    public Hasher putInt(int i) {
      stream.write(i & BOTTOM_BYTE);
      stream.write((i >>> 8) & BOTTOM_BYTE);
      stream.write((i >>> 16) & BOTTOM_BYTE);
      stream.write((i >>> 24) & BOTTOM_BYTE);
      return this;
    }

    @Override
    public Hasher putLong(long l) {
      for (int i = 0; i < 64; i += 8) {
        stream.write((byte) ((l >>> i) & BOTTOM_BYTE));
      }
      return this;
    }

    @Override
    public Hasher putChar(char c) {
      stream.write(c & BOTTOM_BYTE);
      stream.write((c >>> 8) & BOTTOM_BYTE);
      return this;
    }

    @Override
    public <T> Hasher putObject(T instance, Funnel<? super T> funnel) {
      funnel.funnel(instance, this);
      return this;
    }

    @Override
    public HashCode hash() {
      return hashBytes(stream.byteArray(), 0, stream.length());
    }
  }

  // Just to access the byte[] without introducing an unnecessary copy
  private static final class ExposedByteArrayOutputStream extends ByteArrayOutputStream {
    ExposedByteArrayOutputStream(int expectedInputSize) {
      super(expectedInputSize);
    }

    byte[] byteArray() {
      return buf;
    }

    int length() {
      return count;
    }
  }
}

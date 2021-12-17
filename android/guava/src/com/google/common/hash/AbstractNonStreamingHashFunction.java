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
import com.google.errorprone.annotations.Immutable;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Skeleton implementation of {@link HashFunction}, appropriate for non-streaming algorithms. All
 * the hash computation done using {@linkplain #newHasher()} are delegated to the {@linkplain
 * #hashBytes(byte[], int, int)} method.
 *
 * @author Dimitris Andreou
 */
@Immutable
@ElementTypesAreNonnullByDefault
abstract class AbstractNonStreamingHashFunction extends AbstractHashFunction {
  @Override
  public Hasher newHasher() {
    return newHasher(32);
  }

  @Override
  public Hasher newHasher(int expectedInputSize) {
    Preconditions.checkArgument(expectedInputSize >= 0);
    return new BufferingHasher(expectedInputSize);
  }

  @Override
  public HashCode hashInt(int input) {
    return hashBytes(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(input).array());
  }

  @Override
  public HashCode hashLong(long input) {
    return hashBytes(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(input).array());
  }

  @Override
  public HashCode hashUnencodedChars(CharSequence input) {
    int len = input.length();
    ByteBuffer buffer = ByteBuffer.allocate(len * 2).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < len; i++) {
      buffer.putChar(input.charAt(i));
    }
    return hashBytes(buffer.array());
  }

  @Override
  public HashCode hashString(CharSequence input, Charset charset) {
    return hashBytes(input.toString().getBytes(charset));
  }

  @Override
  public abstract HashCode hashBytes(byte[] input, int off, int len);

  @Override
  public HashCode hashBytes(ByteBuffer input) {
    return newHasher(input.remaining()).putBytes(input).hash();
  }

  /** In-memory stream-based implementation of Hasher. */
  private final class BufferingHasher extends AbstractHasher {
    final ExposedByteArrayOutputStream stream;

    BufferingHasher(int expectedInputSize) {
      this.stream = new ExposedByteArrayOutputStream(expectedInputSize);
    }

    @Override
    public Hasher putByte(byte b) {
      stream.write(b);
      return this;
    }

    @Override
    public Hasher putBytes(byte[] bytes, int off, int len) {
      stream.write(bytes, off, len);
      return this;
    }

    @Override
    public Hasher putBytes(ByteBuffer bytes) {
      stream.write(bytes);
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

    void write(ByteBuffer input) {
      int remaining = input.remaining();
      if (count + remaining > buf.length) {
        buf = Arrays.copyOf(buf, count + remaining);
      }
      input.get(buf, count, remaining);
      count += remaining;
    }

    byte[] byteArray() {
      return buf;
    }

    int length() {
      return count;
    }
  }
}

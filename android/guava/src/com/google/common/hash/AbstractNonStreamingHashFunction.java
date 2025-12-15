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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * Skeleton implementation of {@link HashFunction}, appropriate for non-streaming algorithms. All
 * the hash computation done using {@linkplain #newHasher()} are delegated to the {@linkplain
 * #hashBytes(byte[], int, int)} method.
 *
 * @author Dimitris Andreou
 */
@Immutable
abstract class AbstractNonStreamingHashFunction extends AbstractHashFunction {
  @Override
  public Hasher newHasher() {
    return newHasher(32);
  }

  @Override
  public Hasher newHasher(int expectedInputSize) {
    Preconditions.checkArgument(expectedInputSize >= 0);
    return new ByteBufferHasher(expectedInputSize);
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

  /** In-memory ByteBuffer-based implementation of {@link Hasher}. */
  private final class ByteBufferHasher extends AbstractHasher {
    ByteBuffer buffer;

    ByteBufferHasher(int expectedInputSize) {
      this.buffer = ByteBuffer.allocate(expectedInputSize).order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Resizes the buffer if necessary. Guaranteed to leave `buffer` in Write Mode ready for new
     * data.
     */
    private void ensureCapacity(int needed) {
      if (buffer.remaining() >= needed) {
        return;
      }

      int currentCapacity = buffer.capacity();
      int requiredCapacity = buffer.position() + needed;
      int newCapacity = Math.max(currentCapacity * 2, requiredCapacity);

      ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity).order(ByteOrder.LITTLE_ENDIAN);

      // We must switch the old buffer to read mode to extract data
      Java8Compatibility.flip(buffer);

      newBuffer.put(buffer);

      // Swap references, newBuffer is already in write mode at the correct position
      this.buffer = newBuffer;
    }

    @Override
    public Hasher putByte(byte b) {
      ensureCapacity(Byte.BYTES);
      buffer.put(b);
      return this;
    }

    @Override
    public Hasher putBytes(byte[] bytes, int off, int len) {
      ensureCapacity(len);
      buffer.put(bytes, off, len);
      return this;
    }

    @Override
    public Hasher putBytes(ByteBuffer bytes) {
      ensureCapacity(bytes.remaining());
      buffer.put(bytes);
      return this;
    }

    @Override
    public Hasher putUnencodedChars(CharSequence charSequence) {
      ensureCapacity(charSequence.length() * Character.BYTES);
      for (int i = 0, len = charSequence.length(); i < len; i++) {
        buffer.putChar(charSequence.charAt(i));
      }
      return this;
    }

    @Override
    public Hasher putShort(short s) {
      ensureCapacity(Short.BYTES);
      buffer.putShort(s);
      return this;
    }

    @Override
    public Hasher putInt(int i) {
      ensureCapacity(Integer.BYTES);
      buffer.putInt(i);
      return this;
    }

    @Override
    public Hasher putLong(long l) {
      ensureCapacity(Long.BYTES);
      buffer.putLong(l);
      return this;
    }

    @Override
    public Hasher putChar(char c) {
      ensureCapacity(Character.BYTES);
      buffer.putChar(c);
      return this;
    }

    @Override
    public HashCode hash() {
      return hashBytes(buffer.array(), 0, buffer.position());
    }
  }
}

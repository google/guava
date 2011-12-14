// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.hash;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Skeleton implementation of {@link HashFunction}, appropriate for non-streaming algorithms.
 * All the hash computation done using {@linkplain #newHasher()} are delegated to the {@linkplain 
 * #hashBytes(byte[], int, int)} method. 
 * 
 * @author andreou@google.com (Dimitris Andreou)
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
        throw Throwables.propagate(e);
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
      stream.write((s >>> 8)  & BOTTOM_BYTE);
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

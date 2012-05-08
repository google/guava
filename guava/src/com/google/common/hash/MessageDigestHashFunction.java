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

import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.primitives.Chars;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * {@link HashFunction} adapter for {@link MessageDigest}s.
 *
 * @author Kevin Bourrillion
 * @author Dimitris Andreou
 */
final class MessageDigestHashFunction extends AbstractStreamingHashFunction {
  private final String algorithmName;
  private final int bits;

  MessageDigestHashFunction(String algorithmName) {
    this.algorithmName = algorithmName;
    this.bits = getMessageDigest(algorithmName).getDigestLength() * 8;
  }

  public int bits() {
    return bits;
  }

  private static MessageDigest getMessageDigest(String algorithmName) {
    try {
      return MessageDigest.getInstance(algorithmName);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  @Override public Hasher newHasher() {
    return new MessageDigestHasher(getMessageDigest(algorithmName));
  }

  private static class MessageDigestHasher implements Hasher {
    private final MessageDigest digest;
    private final ByteBuffer scratch; // lazy convenience
    private boolean done;

    private MessageDigestHasher(MessageDigest digest) {
      this.digest = digest;
      this.scratch = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override public Hasher putByte(byte b) {
      checkNotDone();
      digest.update(b);
      return this;
    }

    @Override public Hasher putBytes(byte[] bytes) {
      checkNotDone();
      digest.update(bytes);
      return this;
    }

    @Override public Hasher putBytes(byte[] bytes, int off, int len) {
      checkNotDone();
      checkPositionIndexes(off, off + len, bytes.length);
      digest.update(bytes, off, len);
      return this;
    }

    @Override public Hasher putShort(short s) {
      checkNotDone();
      scratch.putShort(s);
      digest.update(scratch.array(), 0, Shorts.BYTES);
      scratch.clear();
      return this;
    }

    @Override public Hasher putInt(int i) {
      checkNotDone();
      scratch.putInt(i);
      digest.update(scratch.array(), 0, Ints.BYTES);
      scratch.clear();
      return this;
    }

    @Override public Hasher putLong(long l) {
      checkNotDone();
      scratch.putLong(l);
      digest.update(scratch.array(), 0, Longs.BYTES);
      scratch.clear();
      return this;
    }

    @Override public Hasher putFloat(float f) {
      checkNotDone();
      scratch.putFloat(f);
      digest.update(scratch.array(), 0, 4);
      scratch.clear();
      return this;
    }

    @Override public Hasher putDouble(double d) {
      checkNotDone();
      scratch.putDouble(d);
      digest.update(scratch.array(), 0, 8);
      scratch.clear();
      return this;
    }

    @Override public Hasher putBoolean(boolean b) {
      return putByte(b ? (byte) 1 : (byte) 0);
    }

    @Override public Hasher putChar(char c) {
      checkNotDone();
      scratch.putChar(c);
      digest.update(scratch.array(), 0, Chars.BYTES);
      scratch.clear();
      return this;
    }

    @Override public Hasher putString(CharSequence charSequence) {
      for (int i = 0; i < charSequence.length(); i++) {
        putChar(charSequence.charAt(i));
      }
      return this;
    }

    @Override public Hasher putString(CharSequence charSequence, Charset charset) {
      return putBytes(charSequence.toString().getBytes(charset));
    }

    @Override public <T> Hasher putObject(T instance, Funnel<? super T> funnel) {
      checkNotDone();
      funnel.funnel(instance, this);
      return this;
    }

    private void checkNotDone() {
      checkState(!done, "Cannot use Hasher after calling #hash() on it");
    }

    public HashCode hash() {
      done = true;
      return HashCodes.fromBytesNoCopy(digest.digest());
    }
  }
}

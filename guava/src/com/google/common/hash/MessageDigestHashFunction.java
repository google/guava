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

import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * {@link HashFunction} adapter for {@link MessageDigest}s.
 *
 * @author Kevin Bourrillion
 * @author Dimitris Andreou
 */
final class MessageDigestHashFunction extends AbstractStreamingHashFunction
    implements Serializable {
  private final String algorithmName;
  private final int bits;

  MessageDigestHashFunction(String algorithmName) {
    this.algorithmName = algorithmName;
    this.bits = getMessageDigest(algorithmName).getDigestLength() * 8;
  }

  @Override public int bits() {
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

  /**
   * Hasher that updates a message digest.
   */
  private static final class MessageDigestHasher extends AbstractByteHasher {

    private final MessageDigest digest;
    private boolean done;

    private MessageDigestHasher(MessageDigest digest) {
      this.digest = digest;
    }

    @Override
    protected void update(byte b) {
      checkNotDone();
      digest.update(b);
    }

    @Override
    protected void update(byte[] b) {
      checkNotDone();
      digest.update(b);
    }

    @Override
    protected void update(byte[] b, int off, int len) {
      checkNotDone();
      digest.update(b, off, len);
    }

    private void checkNotDone() {
      checkState(!done, "Cannot use Hasher after calling #hash() on it");
    }

    @Override
    public HashCode hash() {
      done = true;
      return HashCodes.fromBytesNoCopy(digest.digest());
    }
  }

  private static final long serialVersionUID = 0L;
}

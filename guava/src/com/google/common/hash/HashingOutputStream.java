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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link OutputStream} that maintains a hash of the data written to it.
 *
 * @author Nick Piepmeier
 * @since 16.0
 */
@Beta
public final class HashingOutputStream extends FilterOutputStream {
  private final Hasher hasher;

  /**
   * Creates an output stream that hashes using the given {@link HashFunction}, and forwards all
   * data written to it to the underlying {@link OutputStream}.
   *
   * <p>The {@link OutputStream} should not be written to before or after the hand-off.
   */
  // TODO(user): Evaluate whether it makes sense to always piggyback the computation of a
  // HashCode on an existing OutputStream, compared to creating a separate OutputStream that could
  // be (optionally) be combined with another if needed (with something like
  // MultiplexingOutputStream).
  public HashingOutputStream(HashFunction hashFunction, OutputStream out) {
    super(checkNotNull(out));
    this.hasher = checkNotNull(hashFunction.newHasher());
  }

  @Override
  public void write(int b) throws IOException {
    hasher.putByte((byte) b);
    out.write(b);
  }

  @Override
  public void write(byte[] bytes, int off, int len) throws IOException {
    hasher.putBytes(bytes, off, len);
    out.write(bytes, off, len);
  }

  /**
   * Returns the {@link HashCode} based on the data written to this stream. The result is
   * unspecified if this method is called more than once on the same instance.
   */
  public HashCode hash() {
    return hasher.hash();
  }

  // Overriding close() because FilterOutputStream's close() method pre-JDK8 has bad behavior:
  // it silently ignores any exception thrown by flush(). Instead, just close the delegate stream.
  // It should flush itself if necessary.
  @Override
  public void close() throws IOException {
    out.close();
  }
}

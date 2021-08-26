/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtIncompatible;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import javax.annotation.CheckForNull;

/**
 * A {@link Reader} that reads the characters in a {@link CharSequence}. Like {@code StringReader},
 * but works with any {@link CharSequence}.
 *
 * @author Colin Decker
 */
// TODO(cgdecker): make this public? as a type, or a method in CharStreams?
@GwtIncompatible
@ElementTypesAreNonnullByDefault
final class CharSequenceReader extends Reader {

  @CheckForNull private CharSequence seq;
  private int pos;
  private int mark;

  /** Creates a new reader wrapping the given character sequence. */
  public CharSequenceReader(CharSequence seq) {
    this.seq = checkNotNull(seq);
  }

  private void checkOpen() throws IOException {
    if (seq == null) {
      throw new IOException("reader closed");
    }
  }

  private boolean hasRemaining() {
    return remaining() > 0;
  }

  private int remaining() {
    requireNonNull(seq); // safe as long as we call this only after checkOpen
    return seq.length() - pos;
  }

  /*
   * To avoid the need to call requireNonNull so much, we could consider more clever approaches,
   * such as:
   *
   * - Make checkOpen return the non-null `seq`. Then callers can assign that to a local variable or
   *   even back to `this.seq`. However, that may suggest that we're defending against concurrent
   *   mutation, which is not an actual risk because we use `synchronized`.
   * - Make `remaining` require a non-null `seq` argument. But this is a bit weird because the
   *   method, while it would avoid the instance field `seq` would still access the instance field
   *   `pos`.
   */

  @Override
  public synchronized int read(CharBuffer target) throws IOException {
    checkNotNull(target);
    checkOpen();
    requireNonNull(seq); // safe because of checkOpen
    if (!hasRemaining()) {
      return -1;
    }
    int charsToRead = Math.min(target.remaining(), remaining());
    for (int i = 0; i < charsToRead; i++) {
      target.put(seq.charAt(pos++));
    }
    return charsToRead;
  }

  @Override
  public synchronized int read() throws IOException {
    checkOpen();
    requireNonNull(seq); // safe because of checkOpen
    return hasRemaining() ? seq.charAt(pos++) : -1;
  }

  @Override
  public synchronized int read(char[] cbuf, int off, int len) throws IOException {
    checkPositionIndexes(off, off + len, cbuf.length);
    checkOpen();
    requireNonNull(seq); // safe because of checkOpen
    if (!hasRemaining()) {
      return -1;
    }
    int charsToRead = Math.min(len, remaining());
    for (int i = 0; i < charsToRead; i++) {
      cbuf[off + i] = seq.charAt(pos++);
    }
    return charsToRead;
  }

  @Override
  public synchronized long skip(long n) throws IOException {
    checkArgument(n >= 0, "n (%s) may not be negative", n);
    checkOpen();
    int charsToSkip = (int) Math.min(remaining(), n); // safe because remaining is an int
    pos += charsToSkip;
    return charsToSkip;
  }

  @Override
  public synchronized boolean ready() throws IOException {
    checkOpen();
    return true;
  }

  @Override
  public boolean markSupported() {
    return true;
  }

  @Override
  public synchronized void mark(int readAheadLimit) throws IOException {
    checkArgument(readAheadLimit >= 0, "readAheadLimit (%s) may not be negative", readAheadLimit);
    checkOpen();
    mark = pos;
  }

  @Override
  public synchronized void reset() throws IOException {
    checkOpen();
    pos = mark;
  }

  @Override
  public synchronized void close() throws IOException {
    seq = null;
  }
}

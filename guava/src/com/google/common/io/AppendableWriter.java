/*
 * Copyright (C) 2006 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Writer that places all output on an {@link Appendable} target. If the target is {@link Flushable}
 * or {@link Closeable}, flush()es and close()s will also be delegated to the target.
 *
 * @author Alan Green
 * @author Sebastian Kanthak
 * @since 1.0
 */
@GwtIncompatible
class AppendableWriter extends Writer {
  private final Appendable target;
  private boolean closed;

  /**
   * Creates a new writer that appends everything it writes to {@code target}.
   *
   * @param target target to which to append output
   */
  AppendableWriter(Appendable target) {
    this.target = checkNotNull(target);
  }

  /*
   * Abstract methods from Writer
   */

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    checkNotClosed();
    // It turns out that creating a new String is usually as fast, or faster
    // than wrapping cbuf in a light-weight CharSequence.
    target.append(new String(cbuf, off, len));
  }

  /*
   * Override a few functions for performance reasons to avoid creating unnecessary strings.
   */

  @Override
  public void write(int c) throws IOException {
    checkNotClosed();
    target.append((char) c);
  }

  @Override
  public void write(@NullableDecl String str) throws IOException {
    checkNotClosed();
    target.append(str);
  }

  @Override
  public void write(@NullableDecl String str, int off, int len) throws IOException {
    checkNotClosed();
    // tricky: append takes start, end pair...
    target.append(str, off, off + len);
  }

  @Override
  public void flush() throws IOException {
    checkNotClosed();
    if (target instanceof Flushable) {
      ((Flushable) target).flush();
    }
  }

  @Override
  public void close() throws IOException {
    this.closed = true;
    if (target instanceof Closeable) {
      ((Closeable) target).close();
    }
  }

  @Override
  public Writer append(char c) throws IOException {
    checkNotClosed();
    target.append(c);
    return this;
  }

  @Override
  public Writer append(@NullableDecl CharSequence charSeq) throws IOException {
    checkNotClosed();
    target.append(charSeq);
    return this;
  }

  @Override
  public Writer append(@NullableDecl CharSequence charSeq, int start, int end) throws IOException {
    checkNotClosed();
    target.append(charSeq, start, end);
    return this;
  }

  private void checkNotClosed() throws IOException {
    if (closed) {
      throw new IOException("Cannot write to a closed writer.");
    }
  }
}

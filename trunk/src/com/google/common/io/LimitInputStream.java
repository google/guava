/*
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.io;

import com.google.common.base.Preconditions;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * An InputStream that limits the number of bytes which can be read.
 *
 * @author Charles Fry
 * @since 2009.09.15 <b>tentative</b>
 */
public class LimitInputStream extends FilterInputStream {

  private long left;
  private long mark = -1;

  /**
   * Wraps another input stream, limiting the number of bytes which can be read.
   *
   * @param in the input stream to be wrapped
   * @param limit the maximum number of bytes to be read
   */
  public LimitInputStream(InputStream in, long limit) {
    super(in);
    Preconditions.checkNotNull(in);
    Preconditions.checkArgument(limit >= 0, "limit must be non-negative");
    left = limit;
  }

  @Override public int available() throws IOException {
    return (int) Math.min(in.available(), left);
  }

  @Override public void mark(int readlimit) {
    in.mark(readlimit);
    mark = left;
    // it's okay to mark even if mark isn't supported, as reset won't work
  }

  @Override public int read() throws IOException {
    if (left == 0) {
      return -1;
    }

    int result = in.read();
    if (result != -1) {
      --left;
    }
    return result;
  }

  @Override public int read(byte[] b, int off, int len) throws IOException {
    if (left == 0) {
      return -1;
    }

    len = (int) Math.min(len, left);
    int result = in.read(b, off, len);
    if (result != -1) {
      left -= result;
    }
    return result;
  }

  @Override public void reset() throws IOException {
    if (!in.markSupported()) {
      throw new IOException("Mark not supported");
    }
    if (mark == -1) {
      throw new IOException("Mark not set");
    }

    in.reset();
    left = mark;
  }

  @Override public long skip(long n) throws IOException {
    n = Math.min(n, left);
    long skipped = in.skip(n);
    left -= skipped;
    return skipped;
  }

}

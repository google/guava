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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An OutputStream that counts the number of bytes written.
 *
 * @author Chris Nokleberg
 * @since 2009.09.15 <b>tentative</b>
 */
public class CountingOutputStream extends FilterOutputStream {

  private long count;

  /**
   * Wraps another output stream, counting the number of bytes written.
   *
   * @param out the output stream to be wrapped
   */
  public CountingOutputStream(OutputStream out) {
    super(out);
  }

  /** Returns the number of bytes written. */
  public long getCount() {
    return count;
  }

  @Override public void write(byte[] b, int off, int len) throws IOException {
    out.write(b, off, len);
    count += len;
  }

  @Override public void write(int b) throws IOException {
    out.write(b);
    count++;
  }
}

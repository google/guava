/*
 * Copyright (C) 2007 The Guava Authors
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

import com.google.common.annotations.GwtIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;

/**
 * Package-protected abstract class that implements the line reading algorithm used by {@link
 * LineReader}. Line separators are per {@link java.io.BufferedReader}: line feed, carriage return,
 * or carriage return followed immediately by a linefeed.
 *
 * <p>Subclasses must implement {@link #handleLine}, call {@link #add} to pass character data, and
 * call {@link #finish} at the end of stream.
 *
 * @author Chris Nokleberg
 * @since 1.0
 */
@GwtIncompatible
abstract class LineBuffer {
  /** Holds partial line contents. */
  private StringBuilder line = new StringBuilder();
  /** Whether a line ending with a CR is pending processing. */
  private boolean sawReturn;

  /**
   * Process additional characters from the stream. When a line separator is found the contents of
   * the line and the line separator itself are passed to the abstract {@link #handleLine} method.
   *
   * @param cbuf the character buffer to process
   * @param off the offset into the buffer
   * @param len the number of characters to process
   * @throws IOException if an I/O error occurs
   * @see #finish
   */
  protected void add(char[] cbuf, int off, int len) throws IOException {
    int pos = off;
    if (sawReturn && len > 0) {
      // Last call to add ended with a CR; we can handle the line now.
      if (finishLine(cbuf[pos] == '\n')) {
        pos++;
      }
    }

    int start = pos;
    for (int end = off + len; pos < end; pos++) {
      switch (cbuf[pos]) {
        case '\r':
          line.append(cbuf, start, pos - start);
          sawReturn = true;
          if (pos + 1 < end) {
            if (finishLine(cbuf[pos + 1] == '\n')) {
              pos++;
            }
          }
          start = pos + 1;
          break;

        case '\n':
          line.append(cbuf, start, pos - start);
          finishLine(true);
          start = pos + 1;
          break;

        default:
          // do nothing
      }
    }
    line.append(cbuf, start, off + len - start);
  }

  /** Called when a line is complete. */
  @CanIgnoreReturnValue
  private boolean finishLine(boolean sawNewline) throws IOException {
    String separator = sawReturn ? (sawNewline ? "\r\n" : "\r") : (sawNewline ? "\n" : "");
    handleLine(line.toString(), separator);
    line = new StringBuilder();
    sawReturn = false;
    return sawNewline;
  }

  /**
   * Subclasses must call this method after finishing character processing, in order to ensure that
   * any unterminated line in the buffer is passed to {@link #handleLine}.
   *
   * @throws IOException if an I/O error occurs
   */
  protected void finish() throws IOException {
    if (sawReturn || line.length() > 0) {
      finishLine(false);
    }
  }

  /**
   * Called for each line found in the character data passed to {@link #add}.
   *
   * @param line a line of text (possibly empty), without any line separators
   * @param end the line separator; one of {@code "\r"}, {@code "\n"}, {@code "\r\n"}, or {@code ""}
   * @throws IOException if an I/O error occurs
   */
  protected abstract void handleLine(String line, String end) throws IOException;
}

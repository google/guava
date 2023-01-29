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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.CharStreams.createBuffer;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.AbstractIterator;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.MustBeClosed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.CharBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.CheckForNull;

/**
 * A class for reading lines of text. Provides the same functionality as {@link
 * java.io.BufferedReader#readLine()} but for all {@link Readable} objects, not just instances of
 * {@link Reader}.
 *
 * @author Chris Nokleberg
 * @since 1.0
 */
@Beta
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public final class LineReader {
  private final Readable readable;
  @CheckForNull private final Reader reader;
  private final CharBuffer cbuf = createBuffer();
  private final char[] buf = cbuf.array();

  private final Queue<String> lines = new ArrayDeque<>();
  private final LineBuffer lineBuf =
      new LineBuffer() {
        @Override
        protected void handleLine(String line, String end) {
          lines.add(line);
        }
      };

  /** Creates a new instance that will read lines from the given {@code Readable} object. */
  public LineReader(Readable readable) {
    this.readable = checkNotNull(readable);
    this.reader = (readable instanceof Reader) ? (Reader) readable : null;
  }

  /**
   * Reads a line of text. A line is considered to be terminated by any one of a line feed ({@code
   * '\n'}), a carriage return ({@code '\r'}), or a carriage return followed immediately by a
   * linefeed ({@code "\r\n"}).
   *
   * @return a {@code String} containing the contents of the line, not including any
   *     line-termination characters, or {@code null} if the end of the stream has been reached.
   * @throws IOException if an I/O error occurs
   */
  @CanIgnoreReturnValue // to skip a line
  @CheckForNull
  public String readLine() throws IOException {
    while (lines.peek() == null) {
      Java8Compatibility.clear(cbuf);
      // The default implementation of Reader#read(CharBuffer) allocates a
      // temporary char[], so we call Reader#read(char[], int, int) instead.
      int read = (reader != null) ? reader.read(buf, 0, buf.length) : readable.read(cbuf);
      if (read == -1) {
        lineBuf.finish();
        break;
      }
      lineBuf.add(buf, 0, read);
    }
    return lines.poll();
  }

  /**
   * Returns a {@code Stream}, the elements of which are lines read from this {@code LineReader}.
   *
   * <p>The returned stream is lazy and only reads from the source in the terminal operation. If an
   * I/O error occurs while the stream is reading from the source or when the stream is closed, an
   * {@link UncheckedIOException} is thrown.
   *
   * <p>Like {@link LineReader#readLine()}, this method reads a line of text. A line is
   * considered to be terminated by any one of a line feed ({@code '\n'}), a carriage return
   * ({@code '\r'}), or a carriage return followed immediately by a linefeed ({@code "\r\n"}).
   *
   * <p>The caller is responsible for ensuring that the returned stream is closed. For example:
   *
   * <pre>{@code
   * try (Stream<String> lines = source.lines()) {
   *   lines.map(...)
   *      .filter(...)
   *      .forEach(...);
   * }
   * }</pre>
   *
   * @return a {@code Stream<String>} providing the lines of text
   *     described by this {@code LineReader}
   * @throws UncheckedIOException if an I/O error occurs.
   */
  @MustBeClosed
  public Stream<String> lines() {
    AbstractIterator<String> iterator = new AbstractIterator<String>() {
      @CheckForNull
      @Override
      protected String computeNext() {
        try {
          String next = readLine();
          if (next != null) {
            return next;
          }
          return endOfData();
        } catch (IOException exception) {
          throw new UncheckedIOException(exception);
        }
      }
    };
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
      iterator, Spliterator.ORDERED | Spliterator.NONNULL), false);
  }
}

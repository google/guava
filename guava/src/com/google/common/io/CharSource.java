/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A readable source of characters, such as a text file. Unlike a {@link Reader}, a
 * {@code CharSource} is not an open, stateful stream of characters that can be read and closed.
 * Instead, it is an immutable <i>supplier</i> of {@code InputStream} instances.
 *
 * <p>{@code CharSource} provides two kinds of methods:
 * <ul>
 *   <li><b>Methods that return a reader:</b> These methods should return a <i>new</i>, independent
 *   instance each time they are called. The caller is responsible for ensuring that the returned
 *   reader is closed.
 *   <li><b>Convenience methods:</b> These are implementations of common operations that are
 *   typically implemented by opening a reader using one of the methods in the first category,
 *   doing something and finally closing the reader that was opened.
 * </ul>
 *
 * <p>Several methods in this class, such as {@link #readLines()}, break the contents of the
 * source into lines. Like {@link BufferedReader}, these methods break lines on any of {@code \n},
 * {@code \r} or {@code \r\n}, do not include the line separator in each line and do not consider
 * there to be an empty line at the end if the contents are terminated with a line separator.
 *
 * <p>Any {@link ByteSource} containing text encoded with a specific {@linkplain Charset character
 * encoding} may be viewed as a {@code CharSource} using {@link ByteSource#asCharSource(Charset)}.
 *
 * @since 14.0
 * @author Colin Decker
 */
public abstract class CharSource {

  /**
   * Opens a new {@link Reader} for reading from this source. This method should return a new,
   * independent reader each time it is called.
   *
   * <p>The caller is responsible for ensuring that the returned reader is closed.
   *
   * @throws IOException if an I/O error occurs in the process of opening the reader
   */
  public abstract Reader openStream() throws IOException;

  /**
   * Opens a new {@link BufferedReader} for reading from this source. This method should return a
   * new, independent reader each time it is called.
   *
   * <p>The caller is responsible for ensuring that the returned reader is closed.
   *
   * @throws IOException if an I/O error occurs in the process of opening the reader
   */
  public BufferedReader openBufferedStream() throws IOException {
    Reader reader = openStream();
    return (reader instanceof BufferedReader)
        ? (BufferedReader) reader
        : new BufferedReader(reader);
  }

  /**
   * Appends the contents of this source to the given {@link Appendable} (such as a {@link Writer}).
   * Does not close {@code appendable} if it is {@code Closeable}.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source or
   *     writing to {@code appendable}
   */
  public long copyTo(Appendable appendable) throws IOException {
    checkNotNull(appendable);

    Closer closer = Closer.create();
    try {
      Reader reader = closer.register(openStream());
      return CharStreams.copy(reader, appendable);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Copies the contents of this source to the given sink.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source or
   *     writing to {@code sink}
   */
  public long copyTo(CharSink sink) throws IOException {
    checkNotNull(sink);

    Closer closer = Closer.create();
    try {
      Reader reader = closer.register(openStream());
      Writer writer = closer.register(sink.openStream());
      return CharStreams.copy(reader, writer);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Reads the contents of this source as a string.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source
   */
  public String read() throws IOException {
    Closer closer = Closer.create();
    try {
      Reader reader = closer.register(openStream());
      return CharStreams.toString(reader);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Reads the first link of this source as a string. Returns {@code null} if this source is empty.
   *
   * <p>Like {@link BufferedReader}, this method breaks lines on any of {@code \n}, {@code \r} or
   * {@code \r\n}, does not include the line separator in the returned line and does not consider
   * there to be an extra empty line at the end if the content is terminated with a line separator.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source
   */
  public @Nullable String readFirstLine() throws IOException {
    Closer closer = Closer.create();
    try {
      BufferedReader reader = closer.register(openBufferedStream());
      return reader.readLine();
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Reads all the lines of this source as a list of strings. The returned list will be empty if
   * this source is empty.
   *
   * <p>Like {@link BufferedReader}, this method breaks lines on any of {@code \n}, {@code \r} or
   * {@code \r\n}, does not include the line separator in the returned lines and does not consider
   * there to be an extra empty line at the end if the content is terminated with a line separator.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source
   */
  public ImmutableList<String> readLines() throws IOException {
    Closer closer = Closer.create();
    try {
      BufferedReader reader = closer.register(openBufferedStream());
      List<String> result = Lists.newArrayList();
      String line;
      while ((line = reader.readLine()) != null) {
        result.add(line);
      }
      return ImmutableList.copyOf(result);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }
}

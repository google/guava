/*
 * Copyright (C) 2012 The Guava Authors
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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * A destination to which bytes can be written, such as a file. Unlike an {@link OutputStream}, a
 * {@code ByteSink} is not an open, stateful stream that can be written to and closed. Instead, it
 * is an immutable <i>supplier</i> of {@code OutputStream} instances.
 *
 * <p>{@code ByteSink} provides two kinds of methods:
 *
 * <ul>
 *   <li><b>Methods that return a stream:</b> These methods should return a <i>new</i>, independent
 *       instance each time they are called. The caller is responsible for ensuring that the
 *       returned stream is closed.
 *   <li><b>Convenience methods:</b> These are implementations of common operations that are
 *       typically implemented by opening a stream using one of the methods in the first category,
 *       doing something and finally closing the stream or channel that was opened.
 * </ul>
 *
 * @since 14.0
 * @author Colin Decker
 */
@GwtIncompatible
public abstract class ByteSink {

  /** Constructor for use by subclasses. */
  protected ByteSink() {}

  /**
   * Returns a {@link CharSink} view of this {@code ByteSink} that writes characters to this sink as
   * bytes encoded with the given {@link Charset charset}.
   */
  public CharSink asCharSink(Charset charset) {
    return new AsCharSink(charset);
  }

  /**
   * Opens a new {@link OutputStream} for writing to this sink. This method returns a new,
   * independent stream each time it is called.
   *
   * <p>The caller is responsible for ensuring that the returned stream is closed.
   *
   * @throws IOException if an I/O error occurs while opening the stream
   */
  public abstract OutputStream openStream() throws IOException;

  /**
   * Opens a new buffered {@link OutputStream} for writing to this sink. The returned stream is not
   * required to be a {@link BufferedOutputStream} in order to allow implementations to simply
   * delegate to {@link #openStream()} when the stream returned by that method does not benefit from
   * additional buffering (for example, a {@code ByteArrayOutputStream}). This method returns a new,
   * independent stream each time it is called.
   *
   * <p>The caller is responsible for ensuring that the returned stream is closed.
   *
   * @throws IOException if an I/O error occurs while opening the stream
   * @since 15.0 (in 14.0 with return type {@link BufferedOutputStream})
   */
  public OutputStream openBufferedStream() throws IOException {
    OutputStream out = openStream();
    return (out instanceof BufferedOutputStream)
        ? (BufferedOutputStream) out
        : new BufferedOutputStream(out);
  }

  /**
   * Writes all the given bytes to this sink.
   *
   * @throws IOException if an I/O occurs while writing to this sink
   */
  public void write(byte[] bytes) throws IOException {
    checkNotNull(bytes);

    Closer closer = Closer.create();
    try {
      OutputStream out = closer.register(openStream());
      out.write(bytes);
      out.flush(); // https://code.google.com/p/guava-libraries/issues/detail?id=1330
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Writes all the bytes from the given {@code InputStream} to this sink. Does not close {@code
   * input}.
   *
   * @return the number of bytes written
   * @throws IOException if an I/O occurs while reading from {@code input} or writing to this sink
   */
  @CanIgnoreReturnValue
  public long writeFrom(InputStream input) throws IOException {
    checkNotNull(input);

    Closer closer = Closer.create();
    try {
      OutputStream out = closer.register(openStream());
      long written = ByteStreams.copy(input, out);
      out.flush(); // https://code.google.com/p/guava-libraries/issues/detail?id=1330
      return written;
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * A char sink that encodes written characters with a charset and writes resulting bytes to this
   * byte sink.
   */
  private final class AsCharSink extends CharSink {

    private final Charset charset;

    private AsCharSink(Charset charset) {
      this.charset = checkNotNull(charset);
    }

    @Override
    public Writer openStream() throws IOException {
      return new OutputStreamWriter(ByteSink.this.openStream(), charset);
    }

    @Override
    public String toString() {
      return ByteSink.this.toString() + ".asCharSink(" + charset + ")";
    }
  }
}

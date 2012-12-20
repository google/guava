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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * A readable source of bytes, such as a file. Unlike an {@link InputStream}, a
 * {@code ByteSource} is not an open, stateful stream for input that can be read and closed.
 * Instead, it is an immutable <i>supplier</i> of {@code InputStream} instances.
 *
 * <p>{@code ByteSource} provides two kinds of methods:
 * <ul>
 *   <li><b>Methods that return a stream:</b> These methods should return a <i>new</i>, independent
 *   instance each time they are called. The caller is responsible for ensuring that the returned
 *   stream is closed.
 *   <li><b>Convenience methods:</b> These are implementations of common operations that are
 *   typically implemented by opening a stream using one of the methods in the first category, doing
 *   something and finally closing the stream that was opened.
 * </ul>
 *
 * @since 14.0
 * @author Colin Decker
 */
public abstract class ByteSource {

  private static final int BUF_SIZE = 0x1000; // 4K

  /**
   * Returns a {@link CharSource} view of this byte source that decodes bytes read from this source
   * as characters using the given {@link Charset}.
   */
  public CharSource asCharSource(Charset charset) {
    return new AsCharSource(charset);
  }

  /**
   * Opens a new {@link InputStream} for reading from this source. This method should return a new,
   * independent stream each time it is called.
   *
   * <p>The caller is responsible for ensuring that the returned stream is closed.
   *
   * @throws IOException if an I/O error occurs in the process of opening the stream
   */
  public abstract InputStream openStream() throws IOException;

  /**
   * Opens a new {@link BufferedInputStream} for reading from this source. This method should return
   * a new, independent stream each time it is called.
   *
   * <p>The caller is responsible for ensuring that the returned stream is closed.
   *
   * @throws IOException if an I/O error occurs in the process of opening the stream
   */
  public BufferedInputStream openBufferedStream() throws IOException {
    InputStream in = openStream();
    return (in instanceof BufferedInputStream)
        ? (BufferedInputStream) in
        : new BufferedInputStream(in);
  }

  /**
   * Returns a view of a slice of this byte source that is at most {@code length} bytes long
   * starting at the given {@code offset}.
   *
   * @throws IllegalArgumentException if {@code offset} or {@code length} is negative
   */
  public ByteSource slice(long offset, long length) {
    return new SlicedByteSource(offset, length);
  }

  /**
   * Returns the size of this source in bytes. For most implementations, this is a heavyweight
   * operation that will open a stream, read (or {@link InputStream#skip(long) skip}, if possible)
   * to the end of the stream and return the total number of bytes that were read.
   *
   * <p>For some sources, such as a file, this method may use a more efficient implementation. Note
   * that in such cases, it is <i>possible</i> that this method will return a different number of
   * bytes than would be returned by reading all of the bytes (for example, some special files may
   * return a size of 0 despite actually having content when read).
   *
   * <p>In either case, if this is a mutable source such as a file, the size it returns may not be
   * the same number of bytes a subsequent read would return.
   *
   * @throws IOException if an I/O error occurs in the process of reading the size of this source
   */
  public long size() throws IOException {
    Closer closer = Closer.create();
    try {
      InputStream in = closer.register(openStream());
      return countBySkipping(in);
    } catch (IOException e) {
      // skip may not be supported... at any rate, try reading
    } finally {
      closer.close();
    }

    closer = Closer.create();
    try {
      InputStream in = closer.register(openStream());
      return countByReading(in);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Counts the bytes in the given input stream using skip if possible. Returns SKIP_FAILED if the
   * first call to skip threw, in which case skip may just not be supported.
   */
  private long countBySkipping(InputStream in) throws IOException {
    long count = 0;
    while (true) {
      // don't try to skip more than available()
      // things may work really wrong with FileInputStream otherwise
      long skipped = in.skip(Math.min(in.available(), Integer.MAX_VALUE));
      if (skipped <= 0) {
        if (in.read() == -1) {
          return count;
        }
        count++;
      } else {
        count += skipped;
      }
    }
  }

  private static final byte[] countBuffer = new byte[BUF_SIZE];

  private long countByReading(InputStream in) throws IOException {
    long count = 0;
    long read;
    while ((read = in.read(countBuffer)) != -1) {
      count += read;
    }
    return count;
  }

  /**
   * Copies the contents of this byte source to the given {@code OutputStream}. Does not close
   * {@code output}.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source or
   *     writing to {@code output}
   */
  public long copyTo(OutputStream output) throws IOException {
    checkNotNull(output);

    Closer closer = Closer.create();
    try {
      InputStream in = closer.register(openStream());
      return ByteStreams.copy(in, output);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Copies the contents of this byte source to the given {@code ByteSink}.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source or
   *     writing to {@code sink}
   */
  public long copyTo(ByteSink sink) throws IOException {
    checkNotNull(sink);

    Closer closer = Closer.create();
    try {
      InputStream in = closer.register(openStream());
      OutputStream out = closer.register(sink.openStream());
      return ByteStreams.copy(in, out);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Reads the full contents of this byte source as a byte array.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source
   */
  public byte[] read() throws IOException {
    Closer closer = Closer.create();
    try {
      InputStream in = closer.register(openStream());
      return ByteStreams.toByteArray(in);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Hashes the contents of this byte source using the given hash function.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source
   */
  public HashCode hash(HashFunction hashFunction) throws IOException {
    Hasher hasher = hashFunction.newHasher();
    copyTo(Funnels.asOutputStream(hasher));
    return hasher.hash();
  }

  /**
   * Checks that the contents of this byte source are equal to the contents of the given byte
   * source.
   *
   * @throws IOException if an I/O error occurs in the process of reading from this source or
   *     {@code other}
   */
  public boolean contentEquals(ByteSource other) throws IOException {
    checkNotNull(other);

    byte[] buf1 = new byte[BUF_SIZE];
    byte[] buf2 = new byte[BUF_SIZE];

    Closer closer = Closer.create();
    try {
      InputStream in1 = closer.register(openStream());
      InputStream in2 = closer.register(other.openStream());
      while (true) {
        int read1 = ByteStreams.read(in1, buf1, 0, BUF_SIZE);
        int read2 = ByteStreams.read(in2, buf2, 0, BUF_SIZE);
        if (read1 != read2 || !Arrays.equals(buf1, buf2)) {
          return false;
        } else if (read1 != BUF_SIZE) {
          return true;
        }
      }
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * A char source that reads bytes from this source and decodes them as characters using a
   * charset.
   */
  private final class AsCharSource extends CharSource {

    private final Charset charset;

    private AsCharSource(Charset charset) {
      this.charset = checkNotNull(charset);
    }

    @Override
    public Reader openStream() throws IOException {
      return new InputStreamReader(ByteSource.this.openStream(), charset);
    }

    @Override
    public String toString() {
      return ByteSource.this.toString() + ".asCharSource(" + charset + ")";
    }
  }

  /**
   * A view of a subsection of the containing byte source.
   */
  private final class SlicedByteSource extends ByteSource {

    private final long offset;
    private final long length;

    private SlicedByteSource(long offset, long length) {
      checkArgument(offset >= 0, "offset (%s) may not be negative", offset);
      checkArgument(length >= 0, "length (%s) may not be negative", length);
      this.offset = offset;
      this.length = length;
    }

    @Override
    public InputStream openStream() throws IOException {
      InputStream in = ByteSource.this.openStream();
      if (offset > 0) {
        try {
          ByteStreams.skipFully(in, offset);
        } catch (Throwable e) {
          Closer closer = Closer.create();
          closer.register(in);
          try {
            throw closer.rethrow(e);
          } finally {
            closer.close();
          }
        }
      }
      return ByteStreams.limit(in, length);
    }

    @Override
    public ByteSource slice(long offset, long length) {
      checkArgument(offset >= 0, "offset (%s) may not be negative", offset);
      checkArgument(length >= 0, "length (%s) may not be negative", length);
      long maxLength = this.length - offset;
      return ByteSource.this.slice(this.offset + offset, Math.min(length, maxLength));
    }

    @Override
    public String toString() {
      return ByteSource.this.toString() + ".slice(" + offset + ", " + length + ")";
    }
  }
}

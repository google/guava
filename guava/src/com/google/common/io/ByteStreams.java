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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.math.IntMath;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides utility methods for working with byte arrays and I/O streams.
 *
 * @author Chris Nokleberg
 * @author Colin Decker
 * @since 1.0
 */
@J2ktIncompatible
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public final class ByteStreams {

  private static final int BUFFER_SIZE = 8192;

  /** Creates a new byte array for buffering reads or writes. */
  static byte[] createBuffer() {
    return new byte[BUFFER_SIZE];
  }

  /**
   * There are three methods to implement {@link FileChannel#transferTo(long, long,
   * WritableByteChannel)}:
   *
   * <ol>
   *   <li>Use sendfile(2) or equivalent. Requires that both the input channel and the output
   *       channel have their own file descriptors. Generally this only happens when both channels
   *       are files or sockets. This performs zero copies - the bytes never enter userspace.
   *   <li>Use mmap(2) or equivalent. Requires that either the input channel or the output channel
   *       have file descriptors. Bytes are copied from the file into a kernel buffer, then directly
   *       into the other buffer (userspace). Note that if the file is very large, a naive
   *       implementation will effectively put the whole file in memory. On many systems with paging
   *       and virtual memory, this is not a problem - because it is mapped read-only, the kernel
   *       can always page it to disk "for free". However, on systems where killing processes
   *       happens all the time in normal conditions (i.e., android) the OS must make a tradeoff
   *       between paging memory and killing other processes - so allocating a gigantic buffer and
   *       then sequentially accessing it could result in other processes dying. This is solvable
   *       via madvise(2), but that obviously doesn't exist in java.
   *   <li>Ordinary copy. Kernel copies bytes into a kernel buffer, from a kernel buffer into a
   *       userspace buffer (byte[] or ByteBuffer), then copies them from that buffer into the
   *       destination channel.
   * </ol>
   *
   * This value is intended to be large enough to make the overhead of system calls negligible,
   * without being so large that it causes problems for systems with atypical memory management if
   * approaches 2 or 3 are used.
   */
  private static final int ZERO_COPY_CHUNK_SIZE = 512 * 1024;

  private ByteStreams() {}

  /**
   * Copies all bytes from the input stream to the output stream. Does not close or flush either
   * stream.
   *
   * <p><b>Java 9 users and later:</b> this method should be treated as deprecated; use the
   * equivalent {@link InputStream#transferTo} method instead.
   *
   * @param from the input stream to read from
   * @param to the output stream to write to
   * @return the number of bytes copied
   * @throws IOException if an I/O error occurs
   */
  @CanIgnoreReturnValue
  public static long copy(InputStream from, OutputStream to) throws IOException {
    checkNotNull(from);
    checkNotNull(to);
    byte[] buf = createBuffer();
    long total = 0;
    while (true) {
      int r = from.read(buf);
      if (r == -1) {
        break;
      }
      to.write(buf, 0, r);
      total += r;
    }
    return total;
  }

  /**
   * Copies all bytes from the readable channel to the writable channel. Does not close or flush
   * either channel.
   *
   * @param from the readable channel to read from
   * @param to the writable channel to write to
   * @return the number of bytes copied
   * @throws IOException if an I/O error occurs
   */
  @CanIgnoreReturnValue
  public static long copy(ReadableByteChannel from, WritableByteChannel to) throws IOException {
    checkNotNull(from);
    checkNotNull(to);
    if (from instanceof FileChannel) {
      FileChannel sourceChannel = (FileChannel) from;
      long oldPosition = sourceChannel.position();
      long position = oldPosition;
      long copied;
      do {
        copied = sourceChannel.transferTo(position, ZERO_COPY_CHUNK_SIZE, to);
        position += copied;
        sourceChannel.position(position);
      } while (copied > 0 || position < sourceChannel.size());
      return position - oldPosition;
    }

    ByteBuffer buf = ByteBuffer.wrap(createBuffer());
    long total = 0;
    while (from.read(buf) != -1) {
      Java8Compatibility.flip(buf);
      while (buf.hasRemaining()) {
        total += to.write(buf);
      }
      Java8Compatibility.clear(buf);
    }
    return total;
  }

  /** Max array length on JVM. */
  private static final int MAX_ARRAY_LEN = Integer.MAX_VALUE - 8;

  /** Large enough to never need to expand, given the geometric progression of buffer sizes. */
  private static final int TO_BYTE_ARRAY_DEQUE_SIZE = 20;

  /**
   * Returns a byte array containing the bytes from the buffers already in {@code bufs} (which have
   * a total combined length of {@code totalLen} bytes) followed by all bytes remaining in the given
   * input stream.
   */
  private static byte[] toByteArrayInternal(InputStream in, Queue<byte[]> bufs, int totalLen)
      throws IOException {
    // Roughly size to match what has been read already. Some file systems, such as procfs, return 0
    // as their length. These files are very small, so it's wasteful to allocate an 8KB buffer.
    int initialBufferSize = min(BUFFER_SIZE, max(128, Integer.highestOneBit(totalLen) * 2));
    // Starting with an 8k buffer, double the size of each successive buffer. Smaller buffers
    // quadruple in size until they reach 8k, to minimize the number of small reads for longer
    // streams. Buffers are retained in a deque so that there's no copying between buffers while
    // reading and so all of the bytes in each new allocated buffer are available for reading from
    // the stream.
    for (int bufSize = initialBufferSize;
        totalLen < MAX_ARRAY_LEN;
        bufSize = IntMath.saturatedMultiply(bufSize, bufSize < 4096 ? 4 : 2)) {
      byte[] buf = new byte[min(bufSize, MAX_ARRAY_LEN - totalLen)];
      bufs.add(buf);
      int off = 0;
      while (off < buf.length) {
        // always OK to fill buf; its size plus the rest of bufs is never more than MAX_ARRAY_LEN
        int r = in.read(buf, off, buf.length - off);
        if (r == -1) {
          return combineBuffers(bufs, totalLen);
        }
        off += r;
        totalLen += r;
      }
    }

    // read MAX_ARRAY_LEN bytes without seeing end of stream
    if (in.read() == -1) {
      // oh, there's the end of the stream
      return combineBuffers(bufs, MAX_ARRAY_LEN);
    } else {
      throw new OutOfMemoryError("input is too large to fit in a byte array");
    }
  }

  private static byte[] combineBuffers(Queue<byte[]> bufs, int totalLen) {
    if (bufs.isEmpty()) {
      return new byte[0];
    }
    byte[] result = bufs.remove();
    if (result.length == totalLen) {
      return result;
    }
    int remaining = totalLen - result.length;
    result = Arrays.copyOf(result, totalLen);
    while (remaining > 0) {
      byte[] buf = bufs.remove();
      int bytesToCopy = min(remaining, buf.length);
      int resultOffset = totalLen - remaining;
      System.arraycopy(buf, 0, result, resultOffset, bytesToCopy);
      remaining -= bytesToCopy;
    }
    return result;
  }

  /**
   * Reads all bytes from an input stream into a byte array. Does not close the stream.
   *
   * <p><b>Java 9+ users:</b> use {@code in#readAllBytes()} instead.
   *
   * @param in the input stream to read from
   * @return a byte array containing all the bytes from the stream
   * @throws IOException if an I/O error occurs
   */
  public static byte[] toByteArray(InputStream in) throws IOException {
    checkNotNull(in);
    return toByteArrayInternal(in, new ArrayDeque<byte[]>(TO_BYTE_ARRAY_DEQUE_SIZE), 0);
  }

  /**
   * Reads all bytes from an input stream into a byte array. The given expected size is used to
   * create an initial byte array, but if the actual number of bytes read from the stream differs,
   * the correct result will be returned anyway.
   */
  static byte[] toByteArray(InputStream in, long expectedSize) throws IOException {
    checkArgument(expectedSize >= 0, "expectedSize (%s) must be non-negative", expectedSize);
    if (expectedSize > MAX_ARRAY_LEN) {
      throw new OutOfMemoryError(expectedSize + " bytes is too large to fit in a byte array");
    }

    byte[] bytes = new byte[(int) expectedSize];
    int remaining = (int) expectedSize;

    while (remaining > 0) {
      int off = (int) expectedSize - remaining;
      int read = in.read(bytes, off, remaining);
      if (read == -1) {
        // end of stream before reading expectedSize bytes
        // just return the bytes read so far
        return Arrays.copyOf(bytes, off);
      }
      remaining -= read;
    }

    // bytes is now full
    int b = in.read();
    if (b == -1) {
      return bytes;
    }

    // the stream was longer, so read the rest normally
    Queue<byte[]> bufs = new ArrayDeque<>(TO_BYTE_ARRAY_DEQUE_SIZE + 2);
    bufs.add(bytes);
    bufs.add(new byte[] {(byte) b});
    return toByteArrayInternal(in, bufs, bytes.length + 1);
  }

  /**
   * Reads and discards data from the given {@code InputStream} until the end of the stream is
   * reached. Returns the total number of bytes read. Does not close the stream.
   *
   * @since 20.0
   */
  @CanIgnoreReturnValue
  public static long exhaust(InputStream in) throws IOException {
    long total = 0;
    long read;
    byte[] buf = createBuffer();
    while ((read = in.read(buf)) != -1) {
      total += read;
    }
    return total;
  }

  /**
   * Returns a new {@link ByteArrayDataInput} instance to read from the {@code bytes} array from the
   * beginning.
   */
  public static ByteArrayDataInput newDataInput(byte[] bytes) {
    return newDataInput(new ByteArrayInputStream(bytes));
  }

  /**
   * Returns a new {@link ByteArrayDataInput} instance to read from the {@code bytes} array,
   * starting at the given position.
   *
   * @throws IndexOutOfBoundsException if {@code start} is negative or greater than the length of
   *     the array
   */
  public static ByteArrayDataInput newDataInput(byte[] bytes, int start) {
    checkPositionIndex(start, bytes.length);
    return newDataInput(new ByteArrayInputStream(bytes, start, bytes.length - start));
  }

  /**
   * Returns a new {@link ByteArrayDataInput} instance to read from the given {@code
   * ByteArrayInputStream}. The given input stream is not reset before being read from by the
   * returned {@code ByteArrayDataInput}.
   *
   * @since 17.0
   */
  public static ByteArrayDataInput newDataInput(ByteArrayInputStream byteArrayInputStream) {
    return new ByteArrayDataInputStream(checkNotNull(byteArrayInputStream));
  }

  private static class ByteArrayDataInputStream implements ByteArrayDataInput {
    final DataInput input;

    ByteArrayDataInputStream(ByteArrayInputStream byteArrayInputStream) {
      this.input = new DataInputStream(byteArrayInputStream);
    }

    @Override
    public void readFully(byte b[]) {
      try {
        input.readFully(b);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void readFully(byte b[], int off, int len) {
      try {
        input.readFully(b, off, len);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public int skipBytes(int n) {
      try {
        return input.skipBytes(n);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public boolean readBoolean() {
      try {
        return input.readBoolean();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public byte readByte() {
      try {
        return input.readByte();
      } catch (EOFException e) {
        throw new IllegalStateException(e);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public int readUnsignedByte() {
      try {
        return input.readUnsignedByte();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public short readShort() {
      try {
        return input.readShort();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public int readUnsignedShort() {
      try {
        return input.readUnsignedShort();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public char readChar() {
      try {
        return input.readChar();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public int readInt() {
      try {
        return input.readInt();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public long readLong() {
      try {
        return input.readLong();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public float readFloat() {
      try {
        return input.readFloat();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public double readDouble() {
      try {
        return input.readDouble();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    @CheckForNull
    public String readLine() {
      try {
        return input.readLine();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public String readUTF() {
      try {
        return input.readUTF();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  /** Returns a new {@link ByteArrayDataOutput} instance with a default size. */
  public static ByteArrayDataOutput newDataOutput() {
    return newDataOutput(new ByteArrayOutputStream());
  }

  /**
   * Returns a new {@link ByteArrayDataOutput} instance sized to hold {@code size} bytes before
   * resizing.
   *
   * @throws IllegalArgumentException if {@code size} is negative
   */
  public static ByteArrayDataOutput newDataOutput(int size) {
    // When called at high frequency, boxing size generates too much garbage,
    // so avoid doing that if we can.
    if (size < 0) {
      throw new IllegalArgumentException(String.format("Invalid size: %s", size));
    }
    return newDataOutput(new ByteArrayOutputStream(size));
  }

  /**
   * Returns a new {@link ByteArrayDataOutput} instance which writes to the given {@code
   * ByteArrayOutputStream}. The given output stream is not reset before being written to by the
   * returned {@code ByteArrayDataOutput} and new data will be appended to any existing content.
   *
   * <p>Note that if the given output stream was not empty or is modified after the {@code
   * ByteArrayDataOutput} is created, the contract for {@link ByteArrayDataOutput#toByteArray} will
   * not be honored (the bytes returned in the byte array may not be exactly what was written via
   * calls to {@code ByteArrayDataOutput}).
   *
   * @since 17.0
   */
  public static ByteArrayDataOutput newDataOutput(ByteArrayOutputStream byteArrayOutputStream) {
    return new ByteArrayDataOutputStream(checkNotNull(byteArrayOutputStream));
  }

  private static class ByteArrayDataOutputStream implements ByteArrayDataOutput {

    final DataOutput output;
    final ByteArrayOutputStream byteArrayOutputStream;

    ByteArrayDataOutputStream(ByteArrayOutputStream byteArrayOutputStream) {
      this.byteArrayOutputStream = byteArrayOutputStream;
      output = new DataOutputStream(byteArrayOutputStream);
    }

    @Override
    public void write(int b) {
      try {
        output.write(b);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void write(byte[] b) {
      try {
        output.write(b);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void write(byte[] b, int off, int len) {
      try {
        output.write(b, off, len);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void writeBoolean(boolean v) {
      try {
        output.writeBoolean(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void writeByte(int v) {
      try {
        output.writeByte(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void writeBytes(String s) {
      try {
        output.writeBytes(s);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void writeChar(int v) {
      try {
        output.writeChar(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void writeChars(String s) {
      try {
        output.writeChars(s);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void writeDouble(double v) {
      try {
        output.writeDouble(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void writeFloat(float v) {
      try {
        output.writeFloat(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void writeInt(int v) {
      try {
        output.writeInt(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void writeLong(long v) {
      try {
        output.writeLong(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void writeShort(int v) {
      try {
        output.writeShort(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public void writeUTF(String s) {
      try {
        output.writeUTF(s);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override
    public byte[] toByteArray() {
      return byteArrayOutputStream.toByteArray();
    }
  }

  private static final OutputStream NULL_OUTPUT_STREAM =
      new OutputStream() {
        /** Discards the specified byte. */
        @Override
        public void write(int b) {}

        /** Discards the specified byte array. */
        @Override
        public void write(byte[] b) {
          checkNotNull(b);
        }

        /** Discards the specified byte array. */
        @Override
        public void write(byte[] b, int off, int len) {
          checkNotNull(b);
          checkPositionIndexes(off, off + len, b.length);
        }

        @Override
        public String toString() {
          return "ByteStreams.nullOutputStream()";
        }
      };

  /**
   * Returns an {@link OutputStream} that simply discards written bytes.
   *
   * @since 14.0 (since 1.0 as com.google.common.io.NullOutputStream)
   */
  public static OutputStream nullOutputStream() {
    return NULL_OUTPUT_STREAM;
  }

  /**
   * Wraps a {@link InputStream}, limiting the number of bytes which can be read.
   *
   * @param in the input stream to be wrapped
   * @param limit the maximum number of bytes to be read
   * @return a length-limited {@link InputStream}
   * @since 14.0 (since 1.0 as com.google.common.io.LimitInputStream)
   */
  public static InputStream limit(InputStream in, long limit) {
    return new LimitedInputStream(in, limit);
  }

  private static final class LimitedInputStream extends FilterInputStream {

    private long left;
    private long mark = -1;

    LimitedInputStream(InputStream in, long limit) {
      super(in);
      checkNotNull(in);
      checkArgument(limit >= 0, "limit must be non-negative");
      left = limit;
    }

    @Override
    public int available() throws IOException {
      return (int) min(in.available(), left);
    }

    // it's okay to mark even if mark isn't supported, as reset won't work
    @Override
    public synchronized void mark(int readLimit) {
      in.mark(readLimit);
      mark = left;
    }

    @Override
    public int read() throws IOException {
      if (left == 0) {
        return -1;
      }

      int result = in.read();
      if (result != -1) {
        --left;
      }
      return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (left == 0) {
        return -1;
      }

      len = (int) min(len, left);
      int result = in.read(b, off, len);
      if (result != -1) {
        left -= result;
      }
      return result;
    }

    @Override
    public synchronized void reset() throws IOException {
      if (!in.markSupported()) {
        throw new IOException("Mark not supported");
      }
      if (mark == -1) {
        throw new IOException("Mark not set");
      }

      in.reset();
      left = mark;
    }

    @Override
    public long skip(long n) throws IOException {
      n = min(n, left);
      long skipped = in.skip(n);
      left -= skipped;
      return skipped;
    }
  }

  /**
   * Attempts to read enough bytes from the stream to fill the given byte array, with the same
   * behavior as {@link DataInput#readFully(byte[])}. Does not close the stream.
   *
   * @param in the input stream to read from.
   * @param b the buffer into which the data is read.
   * @throws EOFException if this stream reaches the end before reading all the bytes.
   * @throws IOException if an I/O error occurs.
   */
  public static void readFully(InputStream in, byte[] b) throws IOException {
    readFully(in, b, 0, b.length);
  }

  /**
   * Attempts to read {@code len} bytes from the stream into the given array starting at {@code
   * off}, with the same behavior as {@link DataInput#readFully(byte[], int, int)}. Does not close
   * the stream.
   *
   * @param in the input stream to read from.
   * @param b the buffer into which the data is read.
   * @param off an int specifying the offset into the data.
   * @param len an int specifying the number of bytes to read.
   * @throws EOFException if this stream reaches the end before reading all the bytes.
   * @throws IOException if an I/O error occurs.
   */
  public static void readFully(InputStream in, byte[] b, int off, int len) throws IOException {
    int read = read(in, b, off, len);
    if (read != len) {
      throw new EOFException(
          "reached end of stream after reading " + read + " bytes; " + len + " bytes expected");
    }
  }

  /**
   * Discards {@code n} bytes of data from the input stream. This method will block until the full
   * amount has been skipped. Does not close the stream.
   *
   * @param in the input stream to read from
   * @param n the number of bytes to skip
   * @throws EOFException if this stream reaches the end before skipping all the bytes
   * @throws IOException if an I/O error occurs, or the stream does not support skipping
   */
  public static void skipFully(InputStream in, long n) throws IOException {
    long skipped = skipUpTo(in, n);
    if (skipped < n) {
      throw new EOFException(
          "reached end of stream after skipping " + skipped + " bytes; " + n + " bytes expected");
    }
  }

  /**
   * Discards up to {@code n} bytes of data from the input stream. This method will block until
   * either the full amount has been skipped or until the end of the stream is reached, whichever
   * happens first. Returns the total number of bytes skipped.
   */
  static long skipUpTo(InputStream in, long n) throws IOException {
    long totalSkipped = 0;
    // A buffer is allocated if skipSafely does not skip any bytes.
    byte[] buf = null;

    while (totalSkipped < n) {
      long remaining = n - totalSkipped;
      long skipped = skipSafely(in, remaining);

      if (skipped == 0) {
        // Do a buffered read since skipSafely could return 0 repeatedly, for example if
        // in.available() always returns 0 (the default).
        int skip = (int) min(remaining, BUFFER_SIZE);
        if (buf == null) {
          // Allocate a buffer bounded by the maximum size that can be requested, for
          // example an array of BUFFER_SIZE is unnecessary when the value of remaining
          // is smaller.
          buf = new byte[skip];
        }
        if ((skipped = in.read(buf, 0, skip)) == -1) {
          // Reached EOF
          break;
        }
      }

      totalSkipped += skipped;
    }

    return totalSkipped;
  }

  /**
   * Attempts to skip up to {@code n} bytes from the given input stream, but not more than {@code
   * in.available()} bytes. This prevents {@code FileInputStream} from skipping more bytes than
   * actually remain in the file, something that it {@linkplain java.io.FileInputStream#skip(long)
   * specifies} it can do in its Javadoc despite the fact that it is violating the contract of
   * {@code InputStream.skip()}.
   */
  private static long skipSafely(InputStream in, long n) throws IOException {
    int available = in.available();
    return available == 0 ? 0 : in.skip(min(available, n));
  }

  /**
   * Process the bytes of the given input stream using the given processor.
   *
   * @param input the input stream to process
   * @param processor the object to which to pass the bytes of the stream
   * @return the result of the byte processor
   * @throws IOException if an I/O error occurs
   * @since 14.0
   */
  @CanIgnoreReturnValue // some processors won't return a useful result
  @ParametricNullness
  public static <T extends @Nullable Object> T readBytes(
      InputStream input, ByteProcessor<T> processor) throws IOException {
    checkNotNull(input);
    checkNotNull(processor);

    byte[] buf = createBuffer();
    int read;
    do {
      read = input.read(buf);
    } while (read != -1 && processor.processBytes(buf, 0, read));
    return processor.getResult();
  }

  /**
   * Reads some bytes from an input stream and stores them into the buffer array {@code b}. This
   * method blocks until {@code len} bytes of input data have been read into the array, or end of
   * file is detected. The number of bytes read is returned, possibly zero. Does not close the
   * stream.
   *
   * <p>A caller can detect EOF if the number of bytes read is less than {@code len}. All subsequent
   * calls on the same stream will return zero.
   *
   * <p>If {@code b} is null, a {@code NullPointerException} is thrown. If {@code off} is negative,
   * or {@code len} is negative, or {@code off+len} is greater than the length of the array {@code
   * b}, then an {@code IndexOutOfBoundsException} is thrown. If {@code len} is zero, then no bytes
   * are read. Otherwise, the first byte read is stored into element {@code b[off]}, the next one
   * into {@code b[off+1]}, and so on. The number of bytes read is, at most, equal to {@code len}.
   *
   * @param in the input stream to read from
   * @param b the buffer into which the data is read
   * @param off an int specifying the offset into the data
   * @param len an int specifying the number of bytes to read
   * @return the number of bytes read
   * @throws IOException if an I/O error occurs
   * @throws IndexOutOfBoundsException if {@code off} is negative, if {@code len} is negative, or if
   *     {@code off + len} is greater than {@code b.length}
   */
  @CanIgnoreReturnValue
  // Sometimes you don't care how many bytes you actually read, I guess.
  // (You know that it's either going to read len bytes or stop at EOF.)
  public static int read(InputStream in, byte[] b, int off, int len) throws IOException {
    checkNotNull(in);
    checkNotNull(b);
    if (len < 0) {
      throw new IndexOutOfBoundsException(String.format("len (%s) cannot be negative", len));
    }
    checkPositionIndexes(off, off + len, b.length);
    int total = 0;
    while (total < len) {
      int result = in.read(b, off + total, len - total);
      if (result == -1) {
        break;
      }
      total += result;
    }
    return total;
  }
}

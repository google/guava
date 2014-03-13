/*
 * Copyright (C) 2007 The Guava Authors
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
import static com.google.common.base.Preconditions.checkPositionIndex;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;

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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

/**
 * Provides utility methods for working with byte arrays and I/O streams.
 *
 * @author Chris Nokleberg
 * @author Colin Decker
 * @since 1.0
 */
@Beta
public final class ByteStreams {
  private static final int BUF_SIZE = 0x1000; // 4K

  private ByteStreams() {}

  /**
   * Returns a factory that will supply instances of
   * {@link ByteArrayInputStream} that read from the given byte array.
   *
   * @param b the input buffer
   * @return the factory
   * @deprecated Use {@link ByteSource#wrap(byte[])} instead. This method is
   *     scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static InputSupplier<ByteArrayInputStream> newInputStreamSupplier(
      byte[] b) {
    return asInputSupplier(ByteSource.wrap(b));
  }

  /**
   * Returns a factory that will supply instances of
   * {@link ByteArrayInputStream} that read from the given byte array.
   *
   * @param b the input buffer
   * @param off the offset in the buffer of the first byte to read
   * @param len the maximum number of bytes to read from the buffer
   * @return the factory
   * @deprecated Use {@code ByteSource.wrap(b).slice(off, len)} instead. This
   *     method is scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static InputSupplier<ByteArrayInputStream> newInputStreamSupplier(
      final byte[] b, final int off, final int len) {
    return asInputSupplier(ByteSource.wrap(b).slice(off, len));
  }

  /**
   * Writes a byte array to an output stream from the given supplier.
   *
   * @param from the bytes to write
   * @param to the output supplier
   * @throws IOException if an I/O error occurs
   * @deprecated Use {@link ByteSink#write(byte[])} instead. This method is
   *     scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static void write(byte[] from,
      OutputSupplier<? extends OutputStream> to) throws IOException {
    asByteSink(to).write(from);
  }

  /**
   * Opens input and output streams from the given suppliers, copies all
   * bytes from the input to the output, and closes the streams.
   *
   * @param from the input factory
   * @param to the output factory
   * @return the number of bytes copied
   * @throws IOException if an I/O error occurs
   * @deprecated Use {@link ByteSource#copyTo(ByteSink)} instead. This method
   *     is scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static long copy(InputSupplier<? extends InputStream> from,
      OutputSupplier<? extends OutputStream> to) throws IOException {
    return asByteSource(from).copyTo(asByteSink(to));
  }

  /**
   * Opens an input stream from the supplier, copies all bytes from the
   * input to the output, and closes the input stream. Does not close
   * or flush the output stream.
   *
   * @param from the input factory
   * @param to the output stream to write to
   * @return the number of bytes copied
   * @throws IOException if an I/O error occurs
   * @deprecated Use {@link ByteSource#copyTo(OutputStream)} instead. This
   *      method is scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static long copy(InputSupplier<? extends InputStream> from,
      OutputStream to) throws IOException {
    return asByteSource(from).copyTo(to);
  }

  /**
   * Opens an output stream from the supplier, copies all bytes from the input
   * to the output, and closes the output stream. Does not close or flush the
   * input stream.
   *
   * @param from the input stream to read from
   * @param to the output factory
   * @return the number of bytes copied
   * @throws IOException if an I/O error occurs
   * @since 10.0
   * @deprecated Use {@link ByteSink#writeFrom(InputStream)} instead. This
   *     method is scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static long copy(InputStream from,
      OutputSupplier<? extends OutputStream> to) throws IOException {
    return asByteSink(to).writeFrom(from);
  }

  /**
   * Copies all bytes from the input stream to the output stream.
   * Does not close or flush either stream.
   *
   * @param from the input stream to read from
   * @param to the output stream to write to
   * @return the number of bytes copied
   * @throws IOException if an I/O error occurs
   */
  public static long copy(InputStream from, OutputStream to)
      throws IOException {
    checkNotNull(from);
    checkNotNull(to);
    byte[] buf = new byte[BUF_SIZE];
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
   * Copies all bytes from the readable channel to the writable channel.
   * Does not close or flush either channel.
   *
   * @param from the readable channel to read from
   * @param to the writable channel to write to
   * @return the number of bytes copied
   * @throws IOException if an I/O error occurs
   */
  public static long copy(ReadableByteChannel from,
      WritableByteChannel to) throws IOException {
    checkNotNull(from);
    checkNotNull(to);
    ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);
    long total = 0;
    while (from.read(buf) != -1) {
      buf.flip();
      while (buf.hasRemaining()) {
        total += to.write(buf);
      }
      buf.clear();
    }
    return total;
  }

  /**
   * Reads all bytes from an input stream into a byte array.
   * Does not close the stream.
   *
   * @param in the input stream to read from
   * @return a byte array containing all the bytes from the stream
   * @throws IOException if an I/O error occurs
   */
  public static byte[] toByteArray(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    copy(in, out);
    return out.toByteArray();
  }

  /**
   * Reads all bytes from an input stream into a byte array. The given
   * expected size is used to create an initial byte array, but if the actual
   * number of bytes read from the stream differs, the correct result will be
   * returned anyway.
   */
  static byte[] toByteArray(
      InputStream in, int expectedSize) throws IOException {
    byte[] bytes = new byte[expectedSize];
    int remaining = expectedSize;

    while (remaining > 0) {
      int off = expectedSize - remaining;
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
    FastByteArrayOutputStream out = new FastByteArrayOutputStream();
    out.write(b); // write the byte we read when testing for end of stream
    copy(in, out);

    byte[] result = new byte[bytes.length + out.size()];
    System.arraycopy(bytes, 0, result, 0, bytes.length);
    out.writeTo(result, bytes.length);
    return result;
  }

  /**
   * BAOS that provides limited access to its internal byte array.
   */
  private static final class FastByteArrayOutputStream
      extends ByteArrayOutputStream {
    /**
     * Writes the contents of the internal buffer to the given array starting
     * at the given offset. Assumes the array has space to hold count bytes.
     */
    void writeTo(byte[] b, int off) {
      System.arraycopy(buf, 0, b, off, count);
    }
  }

  /**
   * Returns the data from a {@link InputStream} factory as a byte array.
   *
   * @param supplier the factory
   * @throws IOException if an I/O error occurs
   * @deprecated Use {@link ByteSource#read()} instead. This method is
   *     scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static byte[] toByteArray(
      InputSupplier<? extends InputStream> supplier) throws IOException {
    return asByteSource(supplier).read();
  }

  /**
   * Returns a new {@link ByteArrayDataInput} instance to read from the {@code
   * bytes} array from the beginning.
   */
  public static ByteArrayDataInput newDataInput(byte[] bytes) {
    return newDataInput(new ByteArrayInputStream(bytes));
  }

  /**
   * Returns a new {@link ByteArrayDataInput} instance to read from the {@code
   * bytes} array, starting at the given position.
   *
   * @throws IndexOutOfBoundsException if {@code start} is negative or greater
   *     than the length of the array
   */
  public static ByteArrayDataInput newDataInput(byte[] bytes, int start) {
    checkPositionIndex(start, bytes.length);
    return newDataInput(
        new ByteArrayInputStream(bytes, start, bytes.length - start));
  }

  /**
   * Returns a new {@link ByteArrayDataInput} instance to read from the given
   * {@code ByteArrayInputStream}. The given input stream is not reset before
   * being read from by the returned {@code ByteArrayDataInput}.
   *
   * @since 17.0
   */
  public static ByteArrayDataInput newDataInput(
      ByteArrayInputStream byteArrayInputStream) {
    return new ByteArrayDataInputStream(checkNotNull(byteArrayInputStream));
  }

  private static class ByteArrayDataInputStream implements ByteArrayDataInput {
    final DataInput input;

    ByteArrayDataInputStream(ByteArrayInputStream byteArrayInputStream) {
      this.input = new DataInputStream(byteArrayInputStream);
    }

    @Override public void readFully(byte b[]) {
      try {
        input.readFully(b);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public void readFully(byte b[], int off, int len) {
      try {
        input.readFully(b, off, len);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public int skipBytes(int n) {
      try {
        return input.skipBytes(n);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public boolean readBoolean() {
      try {
        return input.readBoolean();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public byte readByte() {
      try {
        return input.readByte();
      } catch (EOFException e) {
        throw new IllegalStateException(e);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public int readUnsignedByte() {
      try {
        return input.readUnsignedByte();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public short readShort() {
      try {
        return input.readShort();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public int readUnsignedShort() {
      try {
        return input.readUnsignedShort();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public char readChar() {
      try {
        return input.readChar();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public int readInt() {
      try {
        return input.readInt();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public long readLong() {
      try {
        return input.readLong();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public float readFloat() {
      try {
        return input.readFloat();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public double readDouble() {
      try {
        return input.readDouble();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public String readLine() {
      try {
        return input.readLine();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override public String readUTF() {
      try {
        return input.readUTF();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  /**
   * Returns a new {@link ByteArrayDataOutput} instance with a default size.
   */
  public static ByteArrayDataOutput newDataOutput() {
    return newDataOutput(new ByteArrayOutputStream());
  }

  /**
   * Returns a new {@link ByteArrayDataOutput} instance sized to hold
   * {@code size} bytes before resizing.
   *
   * @throws IllegalArgumentException if {@code size} is negative
   */
  public static ByteArrayDataOutput newDataOutput(int size) {
    checkArgument(size >= 0, "Invalid size: %s", size);
    return newDataOutput(new ByteArrayOutputStream(size));
  }

  /**
   * Returns a new {@link ByteArrayDataOutput} instance which writes to the
   * given {@code ByteArrayOutputStream}. The given output stream is not reset
   * before being written to by the returned {@code ByteArrayDataOutput} and
   * new data will be appended to any existing content.
   *
   * <p>Note that if the given output stream was not empty or is modified after
   * the {@code ByteArrayDataOutput} is created, the contract for
   * {@link ByteArrayDataOutput#toByteArray} will not be honored (the bytes
   * returned in the byte array may not be exactly what was written via calls to
   * {@code ByteArrayDataOutput}).
   *
   * @since 17.0
   */
  public static ByteArrayDataOutput newDataOutput(
      ByteArrayOutputStream byteArrayOutputSteam) {
    return new ByteArrayDataOutputStream(checkNotNull(byteArrayOutputSteam));
  }

  @SuppressWarnings("deprecation") // for writeBytes
  private static class ByteArrayDataOutputStream
      implements ByteArrayDataOutput {

    final DataOutput output;
    final ByteArrayOutputStream byteArrayOutputSteam;

    ByteArrayDataOutputStream(ByteArrayOutputStream byteArrayOutputSteam) {
      this.byteArrayOutputSteam = byteArrayOutputSteam;
      output = new DataOutputStream(byteArrayOutputSteam);
    }

    @Override public void write(int b) {
      try {
        output.write(b);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void write(byte[] b) {
      try {
        output.write(b);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void write(byte[] b, int off, int len) {
      try {
        output.write(b, off, len);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void writeBoolean(boolean v) {
      try {
        output.writeBoolean(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void writeByte(int v) {
      try {
        output.writeByte(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void writeBytes(String s) {
      try {
        output.writeBytes(s);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void writeChar(int v) {
      try {
        output.writeChar(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void writeChars(String s) {
      try {
        output.writeChars(s);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void writeDouble(double v) {
      try {
        output.writeDouble(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void writeFloat(float v) {
      try {
        output.writeFloat(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void writeInt(int v) {
      try {
        output.writeInt(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void writeLong(long v) {
      try {
        output.writeLong(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void writeShort(int v) {
      try {
        output.writeShort(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public void writeUTF(String s) {
      try {
        output.writeUTF(s);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    @Override public byte[] toByteArray() {
      return byteArrayOutputSteam.toByteArray();
    }
  }

  private static final OutputStream NULL_OUTPUT_STREAM =
      new OutputStream() {
        /** Discards the specified byte. */
        @Override public void write(int b) {
        }
        /** Discards the specified byte array. */
        @Override public void write(byte[] b) {
          checkNotNull(b);
        }
        /** Discards the specified byte array. */
        @Override public void write(byte[] b, int off, int len) {
          checkNotNull(b);
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
   * Wraps a {@link InputStream}, limiting the number of bytes which can be
   * read.
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

    @Override public int available() throws IOException {
      return (int) Math.min(in.available(), left);
    }

    // it's okay to mark even if mark isn't supported, as reset won't work
    @Override public synchronized void mark(int readLimit) {
      in.mark(readLimit);
      mark = left;
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

    @Override public synchronized void reset() throws IOException {
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

  /**
   * Returns the length of a supplied input stream, in bytes.
   *
   * @deprecated Use {@link ByteSource#size()} instead. This method is
   *     scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static long length(
      InputSupplier<? extends InputStream> supplier) throws IOException {
    return asByteSource(supplier).size();
  }

  /**
   * Returns true if the supplied input streams contain the same bytes.
   *
   * @throws IOException if an I/O error occurs
   * @deprecated Use {@link ByteSource#contentEquals(ByteSource)} instead. This
   *     method is scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static boolean equal(InputSupplier<? extends InputStream> supplier1,
      InputSupplier<? extends InputStream> supplier2) throws IOException {
    return asByteSource(supplier1).contentEquals(asByteSource(supplier2));
  }

  /**
   * Attempts to read enough bytes from the stream to fill the given byte array,
   * with the same behavior as {@link DataInput#readFully(byte[])}.
   * Does not close the stream.
   *
   * @param in the input stream to read from.
   * @param b the buffer into which the data is read.
   * @throws EOFException if this stream reaches the end before reading all
   *     the bytes.
   * @throws IOException if an I/O error occurs.
   */
  public static void readFully(InputStream in, byte[] b) throws IOException {
    readFully(in, b, 0, b.length);
  }

  /**
   * Attempts to read {@code len} bytes from the stream into the given array
   * starting at {@code off}, with the same behavior as
   * {@link DataInput#readFully(byte[], int, int)}. Does not close the
   * stream.
   *
   * @param in the input stream to read from.
   * @param b the buffer into which the data is read.
   * @param off an int specifying the offset into the data.
   * @param len an int specifying the number of bytes to read.
   * @throws EOFException if this stream reaches the end before reading all
   *     the bytes.
   * @throws IOException if an I/O error occurs.
   */
  public static void readFully(
      InputStream in, byte[] b, int off, int len) throws IOException {
    int read = read(in, b, off, len);
    if (read != len) {
      throw new EOFException("reached end of stream after reading "
          + read + " bytes; " + len + " bytes expected");
    }
  }

  /**
   * Discards {@code n} bytes of data from the input stream. This method
   * will block until the full amount has been skipped. Does not close the
   * stream.
   *
   * @param in the input stream to read from
   * @param n the number of bytes to skip
   * @throws EOFException if this stream reaches the end before skipping all
   *     the bytes
   * @throws IOException if an I/O error occurs, or the stream does not
   *     support skipping
   */
  public static void skipFully(InputStream in, long n) throws IOException {
    long toSkip = n;
    while (n > 0) {
      long amt = in.skip(n);
      if (amt == 0) {
        // Force a blocking read to avoid infinite loop
        if (in.read() == -1) {
          long skipped = toSkip - n;
          throw new EOFException("reached end of stream after skipping "
              + skipped + " bytes; " + toSkip + " bytes expected");
        }
        n--;
      } else {
        n -= amt;
      }
    }
  }

  /**
   * Process the bytes of a supplied stream
   *
   * @param supplier the input stream factory
   * @param processor the object to which to pass the bytes of the stream
   * @return the result of the byte processor
   * @throws IOException if an I/O error occurs
   * @deprecated Use {@link ByteSource#read(ByteProcessor)} instead. This
   *     method is scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static <T> T readBytes(
      InputSupplier<? extends InputStream> supplier,
      ByteProcessor<T> processor) throws IOException {
    checkNotNull(supplier);
    checkNotNull(processor);

    Closer closer = Closer.create();
    try {
      InputStream in = closer.register(supplier.getInput());
      return readBytes(in, processor);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
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
  public static <T> T readBytes(
      InputStream input, ByteProcessor<T> processor) throws IOException {
    checkNotNull(input);
    checkNotNull(processor);

    byte[] buf = new byte[BUF_SIZE];
    int read;
    do {
      read = input.read(buf);
    } while (read != -1 && processor.processBytes(buf, 0, read));
    return processor.getResult();
  }

  /**
   * Computes the hash code of the data supplied by {@code supplier} using {@code
   * hashFunction}.
   *
   * @param supplier the input stream factory
   * @param hashFunction the hash function to use to hash the data
   * @return the {@link HashCode} of all of the bytes in the input stream
   * @throws IOException if an I/O error occurs
   * @since 12.0
   * @deprecated Use {@link ByteSource#hash(HashFunction)} instead. This method
   *     is scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static HashCode hash(
      InputSupplier<? extends InputStream> supplier, HashFunction hashFunction)
      throws IOException {
    return asByteSource(supplier).hash(hashFunction);
  }

  /**
   * Reads some bytes from an input stream and stores them into the buffer array
   * {@code b}. This method blocks until {@code len} bytes of input data have
   * been read into the array, or end of file is detected. The number of bytes
   * read is returned, possibly zero. Does not close the stream.
   *
   * <p>A caller can detect EOF if the number of bytes read is less than
   * {@code len}. All subsequent calls on the same stream will return zero.
   *
   * <p>If {@code b} is null, a {@code NullPointerException} is thrown. If
   * {@code off} is negative, or {@code len} is negative, or {@code off+len} is
   * greater than the length of the array {@code b}, then an
   * {@code IndexOutOfBoundsException} is thrown. If {@code len} is zero, then
   * no bytes are read. Otherwise, the first byte read is stored into element
   * {@code b[off]}, the next one into {@code b[off+1]}, and so on. The number
   * of bytes read is, at most, equal to {@code len}.
   *
   * @param in the input stream to read from
   * @param b the buffer into which the data is read
   * @param off an int specifying the offset into the data
   * @param len an int specifying the number of bytes to read
   * @return the number of bytes read
   * @throws IOException if an I/O error occurs
   */
  public static int read(InputStream in, byte[] b, int off, int len)
      throws IOException {
    checkNotNull(in);
    checkNotNull(b);
    if (len < 0) {
      throw new IndexOutOfBoundsException("len is negative");
    }
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

  /**
   * Returns an {@link InputSupplier} that returns input streams from the
   * an underlying supplier, where each stream starts at the given
   * offset and is limited to the specified number of bytes.
   *
   * @param supplier the supplier from which to get the raw streams
   * @param offset the offset in bytes into the underlying stream where
   *     the returned streams will start
   * @param length the maximum length of the returned streams
   * @throws IllegalArgumentException if offset or length are negative
   * @deprecated Use {@link ByteSource#slice(int, int)} instead. This method is
   *     scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static InputSupplier<InputStream> slice(
      final InputSupplier<? extends InputStream> supplier,
      final long offset,
      final long length) {
    return asInputSupplier(asByteSource(supplier).slice(offset, length));
  }

  /**
   * Joins multiple {@link InputStream} suppliers into a single supplier.
   * Streams returned from the supplier will contain the concatenated data from
   * the streams of the underlying suppliers.
   *
   * <p>Only one underlying input stream will be open at a time. Closing the
   * joined stream will close the open underlying stream.
   *
   * <p>Reading from the joined stream will throw a {@link NullPointerException}
   * if any of the suppliers are null or return null.
   *
   * @param suppliers the suppliers to concatenate
   * @return a supplier that will return a stream containing the concatenated
   *     stream data
   * @deprecated Use {@link ByteSource#concat(Iterable)} instead. This method
   *     is scheduled for removal in Guava 18.0.
   */
  @Deprecated
  public static InputSupplier<InputStream> join(
      final Iterable<? extends InputSupplier<? extends InputStream>> suppliers) {
    checkNotNull(suppliers);
    Iterable<ByteSource> sources = Iterables.transform(suppliers,
        new Function<InputSupplier<? extends InputStream>, ByteSource>() {
          @Override
          public ByteSource apply(InputSupplier<? extends InputStream> input) {
            return asByteSource(input);
          }
        });
    return asInputSupplier(ByteSource.concat(sources));
  }

  /**
   * Varargs form of {@link #join(Iterable)}.
   *
   * @deprecated Use {@link ByteSource#concat(ByteSource[])} instead. This
   *     method is scheduled for removal in Guava 18.0.
   */
  @Deprecated
  @SuppressWarnings("unchecked") // suppress "possible heap pollution" warning in JDK7
  public static InputSupplier<InputStream> join(
      InputSupplier<? extends InputStream>... suppliers) {
    return join(Arrays.asList(suppliers));
  }

  // TODO(user): Remove these once Input/OutputSupplier methods are removed

  /**
   * Returns a view of the given {@code InputStream} supplier as a
   * {@code ByteSource}.
   *
   * <p>This method is a temporary method provided for easing migration from
   * suppliers to sources and sinks.
   *
   * @since 15.0
   * @deprecated Convert all {@code InputSupplier<? extends InputStream>}
   *     implementations to extend {@link ByteSource} or provide a method for
   *     viewing the object as a {@code ByteSource}. This method is scheduled
   *     for removal in Guava 18.0.
   */
  @Deprecated
  public static ByteSource asByteSource(
      final InputSupplier<? extends InputStream> supplier) {
    checkNotNull(supplier);
    return new ByteSource() {
      @Override
      public InputStream openStream() throws IOException {
        return supplier.getInput();
      }

      @Override
      public String toString() {
        return "ByteStreams.asByteSource(" + supplier + ")";
      }
    };
  }

  /**
   * Returns a view of the given {@code OutputStream} supplier as a
   * {@code ByteSink}.
   *
   * <p>This method is a temporary method provided for easing migration from
   * suppliers to sources and sinks.
   *
   * @since 15.0
   * @deprecated Convert all {@code OutputSupplier<? extends OutputStream>}
   *     implementations to extend {@link ByteSink} or provide a method for
   *     viewing the object as a {@code ByteSink}. This method is scheduled
   *     for removal in Guava 18.0.
   */
  @Deprecated
  public static ByteSink asByteSink(
      final OutputSupplier<? extends OutputStream> supplier) {
    checkNotNull(supplier);
    return new ByteSink() {
      @Override
      public OutputStream openStream() throws IOException {
        return supplier.getOutput();
      }

      @Override
      public String toString() {
        return "ByteStreams.asByteSink(" + supplier + ")";
      }
    };
  }

  @SuppressWarnings("unchecked") // used internally where known to be safe
  static <S extends InputStream> InputSupplier<S> asInputSupplier(
      final ByteSource source) {
    return (InputSupplier) checkNotNull(source);
  }

  @SuppressWarnings("unchecked") // used internally where known to be safe
  static <S extends OutputStream> OutputSupplier<S> asOutputSupplier(
      final ByteSink sink) {
    return (OutputSupplier) checkNotNull(sink);
  }
}

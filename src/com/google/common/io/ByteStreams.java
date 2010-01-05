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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.zip.Checksum;

/**
 * Provides utility methods for working with byte arrays and I/O streams.
 *
 * <p>All method parameters must be non-null unless documented otherwise.
 *
 * @author Chris Nokleberg
 * @since 2009.09.15 <b>tentative</b>
 */
public final class ByteStreams {
  private static final int BUF_SIZE = 0x1000; // 4K

  private ByteStreams() {}

  /**
   * Returns a factory that will supply instances of
   * {@link ByteArrayInputStream} that read from the given byte array.
   *
   * @param b the input buffer
   * @return the factory
   */
  public static InputSupplier<ByteArrayInputStream> newInputStreamSupplier(
      byte[] b) {
    return newInputStreamSupplier(b, 0, b.length);
  }

  /**
   * Returns a factory that will supply instances of
   * {@link ByteArrayInputStream} that read from the given byte array.
   *
   * @param b the input buffer
   * @param off the offset in the buffer of the first byte to read
   * @param len the maximum number of bytes to read from the buffer
   * @return the factory
   */
  public static InputSupplier<ByteArrayInputStream> newInputStreamSupplier(
      final byte[] b, final int off, final int len) {
    return new InputSupplier<ByteArrayInputStream>() {
      public ByteArrayInputStream getInput() {
        return new ByteArrayInputStream(b, off, len);
      }
    };
  }

  /**
   * Writes a byte array to an output stream from the given supplier.
   *
   * @param from the bytes to write
   * @param to the output supplier
   * @throws IOException if an I/O error occurs
   */
  public static void write(byte[] from,
      OutputSupplier<? extends OutputStream> to) throws IOException {
    Preconditions.checkNotNull(from);
    boolean threw = true;
    OutputStream out = to.getOutput();
    try {
      out.write(from);
      threw = false;
    } finally {
      Closeables.close(out, threw);
    }
  }

  /**
   * Opens input and output streams from the given suppliers, copies all
   * bytes from the input to the output, and closes the streams.
   *
   * @param from the input factory
   * @param to the output factory
   * @return the number of bytes copied
   * @throws IOException if an I/O error occurs
   */
  public static long copy(InputSupplier<? extends InputStream> from,
      OutputSupplier<? extends OutputStream> to) throws IOException {
    boolean threw = true;
    InputStream in = from.getInput();
    try {
      OutputStream out = to.getOutput();
      try {
        long count = copy(in, out);
        threw = false;
        return count;
      } finally {
        Closeables.close(out, threw);
      }
    } finally {
      Closeables.close(in, threw);
    }
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
   */
  public static long copy(InputSupplier<? extends InputStream> from,
      OutputStream to) throws IOException {
    boolean threw = true;
    InputStream in = from.getInput();
    try {
      long count = copy(in, to);
      threw = false;
      return count;
    } finally {
      Closeables.close(in, threw);
    }
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
   * Returns the data from a {@link InputStream} factory as a byte array.
   *
   * @param supplier the factory
   * @throws IOException if an I/O error occurs
   */
  public static byte[] toByteArray(
      InputSupplier<? extends InputStream> supplier) throws IOException {
    boolean threw = true;
    InputStream in = supplier.getInput();
    try {
      byte[] result = toByteArray(in);
      threw = false;
      return result;
    } finally {
      Closeables.close(in, threw);
    }
  }

  /**
   * Returns a new {@link ByteArrayDataInput} instance to read from the {@code
   * bytes} array from the beginning.
   */
  public static ByteArrayDataInput newDataInput(byte[] bytes) {
    return new ByteArrayDataInputStream(bytes);
  }

  /**
   * Returns a new {@link ByteArrayDataInput} instance to read from the {@code
   * bytes} array, starting at the given position.
   *
   * @throws IndexOutOfBoundsException if {@code start} is negative or greater
   *     than the length of the array
   */
  public static ByteArrayDataInput newDataInput(byte[] bytes, int start) {
    Preconditions.checkPositionIndex(start, bytes.length);
    return new ByteArrayDataInputStream(bytes, start);
  }

  private static class ByteArrayDataInputStream implements ByteArrayDataInput {
    final DataInput input;

    ByteArrayDataInputStream(byte[] bytes) {
      this.input = new DataInputStream(new ByteArrayInputStream(bytes));
    }

    ByteArrayDataInputStream(byte[] bytes, int start) {
      this.input = new DataInputStream(
          new ByteArrayInputStream(bytes, start, bytes.length - start));
    }

    /*@Override*/ public void readFully(byte b[]) {
      try {
        input.readFully(b);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public void readFully(byte b[], int off, int len) {
      try {
        input.readFully(b, off, len);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public int skipBytes(int n) {
      try {
        return input.skipBytes(n);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public boolean readBoolean() {
      try {
        return input.readBoolean();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public byte readByte() {
      try {
        return input.readByte();
      } catch (EOFException e) {
        throw new IllegalStateException(e);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public int readUnsignedByte() {
      try {
        return input.readUnsignedByte();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public short readShort() {
      try {
        return input.readShort();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public int readUnsignedShort() {
      try {
        return input.readUnsignedShort();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public char readChar() {
      try {
        return input.readChar();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public int readInt() {
      try {
        return input.readInt();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public long readLong() {
      try {
        return input.readLong();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public float readFloat() {
      try {
        return input.readFloat();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public double readDouble() {
      try {
        return input.readDouble();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public String readLine() {
      try {
        return input.readLine();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    /*@Override*/ public String readUTF() {
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
    return new ByteArrayDataOutputStream();
  }

  /**
   * Returns a new {@link ByteArrayDataOutput} instance sized to hold
   * {@code size} bytes before resizing.
   *
   * @throws IllegalArgumentException if {@code size} is negative
   */
  public static ByteArrayDataOutput newDataOutput(int size) {
    Preconditions.checkArgument(size >= 0, "Invalid size: %s", size);
    return new ByteArrayDataOutputStream(size);
  }

  @SuppressWarnings("deprecation") // for writeBytes
  private static class ByteArrayDataOutputStream
      implements ByteArrayDataOutput {

    final DataOutput output;
    final ByteArrayOutputStream byteArrayOutputSteam;

    ByteArrayDataOutputStream() {
      this(new ByteArrayOutputStream());
    }

    ByteArrayDataOutputStream(int size) {
      this(new ByteArrayOutputStream(size));
    }

    ByteArrayDataOutputStream(ByteArrayOutputStream byteArrayOutputSteam) {
      this.byteArrayOutputSteam = byteArrayOutputSteam;
      output = new DataOutputStream(byteArrayOutputSteam);
    }

    /*@Override*/ public void write(int b) {
      try {
        output.write(b);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void write(byte[] b) {
      try {
        output.write(b);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void write(byte[] b, int off, int len) {
      try {
        output.write(b, off, len);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void writeBoolean(boolean v) {
      try {
        output.writeBoolean(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void writeByte(int v) {
      try {
        output.writeByte(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void writeBytes(String s) {
      try {
        output.writeBytes(s);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void writeChar(int v) {
      try {
        output.writeChar(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void writeChars(String s) {
      try {
        output.writeChars(s);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void writeDouble(double v) {
      try {
        output.writeDouble(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void writeFloat(float v) {
      try {
        output.writeFloat(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void writeInt(int v) {
      try {
        output.writeInt(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void writeLong(long v) {
      try {
        output.writeLong(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void writeShort(int v) {
      try {
        output.writeShort(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public void writeUTF(String s) {
      try {
        output.writeUTF(s);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }

    /*@Override*/ public byte[] toByteArray() {
      return byteArrayOutputSteam.toByteArray();
    }

  }

  // TODO: Not all streams support skipping.
  /** Returns the length of a supplied input stream, in bytes. */
  public static long length(InputSupplier<? extends InputStream> supplier)
      throws IOException {
    long count = 0;
    boolean threw = true;
    InputStream in = supplier.getInput();
    try {
      while (true) {
        // We skip only Integer.MAX_VALUE due to JDK overflow bugs.
        long amt = in.skip(Integer.MAX_VALUE);
        if (amt == 0) {
          if (in.read() == -1) {
            threw = false;
            return count;
          }
          count++;
        } else {
          count += amt;
        }
      }
    } finally {
      Closeables.close(in, threw);
    }
  }

  /**
   * Returns true if the supplied input streams contain the same bytes.
   *
   * @throws IOException if an I/O error occurs
   */
  public static boolean equal(InputSupplier<? extends InputStream> supplier1,
      InputSupplier<? extends InputStream> supplier2) throws IOException {
    byte[] buf1 = new byte[BUF_SIZE];
    byte[] buf2 = new byte[BUF_SIZE];

    boolean threw = true;
    InputStream in1 = supplier1.getInput();
    try {
      InputStream in2 = supplier2.getInput();
      try {
        while (true) {
          int read1 = read(in1, buf1, 0, BUF_SIZE);
          int read2 = read(in2, buf2, 0, BUF_SIZE);
          if (read1 != read2 || !Arrays.equals(buf1, buf2)) {
            threw = false;
            return false;
          } else if (read1 != BUF_SIZE) {
            threw = false;
            return true;
          }
        }
      } finally {
        Closeables.close(in2, threw);
      }
    } finally {
      Closeables.close(in1, threw);
    }
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
  public static void readFully(InputStream in, byte[] b, int off, int len)
      throws IOException {
    if (read(in, b, off, len) != len) {
      throw new EOFException();
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
    while (n > 0) {
      long amt = in.skip(n);
      if (amt == 0) {
        // Force a blocking read to avoid infinite loop
        if (in.read() == -1) {
          throw new EOFException();
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
   */
  public static <T> T readBytes(InputSupplier<? extends InputStream> supplier,
      ByteProcessor<T> processor) throws IOException {
    byte[] buf = new byte[BUF_SIZE];
    boolean threw = true;
    InputStream in = supplier.getInput();
    try {
      int amt;
      do {
        amt = in.read(buf);
        if (amt == -1) {
          threw = false;
          break;
        }
      } while (processor.processBytes(buf, 0, amt));
      return processor.getResult();
    } finally {
      Closeables.close(in, threw);
    }
  }

  /**
   * Computes and returns the checksum value for a supplied input stream.
   * The checksum object is reset when this method returns successfully.
   *
   * @param supplier the input stream factory
   * @param checksum the checksum object
   * @return the result of {@link Checksum#getValue} after updating the
   *     checksum object with all of the bytes in the stream
   * @throws IOException if an I/O error occurs
   */
  public static long getChecksum(InputSupplier<? extends InputStream> supplier,
      final Checksum checksum) throws IOException {
    return readBytes(supplier, new ByteProcessor<Long>() {
      public boolean processBytes(byte[] buf, int off, int len) {
        checksum.update(buf, off, len);
        return true;
      }

      public Long getResult() {
        long result = checksum.getValue();
        checksum.reset();
        return result;
      }
    });
  }

  /**
   * Computes and returns the digest value for a supplied input stream.
   * The digest object is reset when this method returns successfully.
   *
   * @param supplier the input stream factory
   * @param md the digest object
   * @return the result of {@link MessageDigest#digest()} after updating the
   *     digest object with all of the bytes in the stream
   * @throws IOException if an I/O error occurs
   */
  public static byte[] getDigest(InputSupplier<? extends InputStream> supplier,
      final MessageDigest md) throws IOException {
    return readBytes(supplier, new ByteProcessor<byte[]>() {
      public boolean processBytes(byte[] buf, int off, int len) {
        md.update(buf, off, len);
        return true;
      }

      public byte[] getResult() {
        return md.digest();
      }
    });
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
   */
  public static InputSupplier<InputStream> slice(
      final InputSupplier<? extends InputStream> supplier,
      final long offset,
      final long length) {
    Preconditions.checkNotNull(supplier);
    Preconditions.checkArgument(offset >= 0, "offset is negative");
    Preconditions.checkArgument(length >= 0, "length is negative");
    return new InputSupplier<InputStream>() {
      /*@Override*/ public InputStream getInput() throws IOException {
        InputStream in = supplier.getInput();
        if (offset > 0) {
          try {
            skipFully(in, offset);
          } catch (IOException e) {
            Closeables.closeQuietly(in);
            throw e;
          }
        }
        return new LimitInputStream(in, length);
      }
    };
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
   */
  public static InputSupplier<InputStream> join(
     final Iterable<? extends InputSupplier<? extends InputStream>> suppliers) {
    return new InputSupplier<InputStream>() {
      /*@Override*/ public InputStream getInput() throws IOException {
        return new MultiInputStream(suppliers.iterator());
      }
    };
  }

  /** Varargs form of {@link #join(Iterable)}. */
  public static InputSupplier<InputStream> join(
      InputSupplier<? extends InputStream>... suppliers) {
    return join(Arrays.asList(suppliers));
  }
}

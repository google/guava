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

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An implementation of {@link DataInput} that uses little-endian byte ordering
 * for reading {@code short}, {@code int}, {@code float}, {@code double}, and
 * {@code long} values.
 * <p>
 * <b>Note:</b> This class intentionally violates the specification of its
 * supertype {@code DataInput}, which explicitly requires big-endian byte order.
 *
 * @author Chris Nokleberg
 * @author Keith Bottner
 * @since 8.0
 */
@Beta
public final class LittleEndianDataInputStream extends FilterInputStream
    implements DataInput {

  /**
   * Creates a {@code LittleEndianDataInputStream} that wraps the given stream.
   *
   * @param in the stream to delegate to
   */
  public LittleEndianDataInputStream(InputStream in) {
    super(Preconditions.checkNotNull(in));
  }

  /**
   * This method will throw an {@link UnsupportedOperationException}.
   */
  @Override
  public String readLine() {
    throw new UnsupportedOperationException("readLine is not supported");
  }

  @Override
  public void readFully(byte[] b) throws IOException {
    ByteStreams.readFully(this, b);
  }

  @Override
  public void readFully(byte[] b, int off, int len) throws IOException {
    ByteStreams.readFully(this, b, off, len);
  }

  @Override
  public int skipBytes(int n) throws IOException {
    return (int) in.skip(n);
  }

  @Override
  public int readUnsignedByte() throws IOException {
    int b1 = in.read();
    if (0 > b1) {
      throw new EOFException();
    }
    
    return b1;
  }

  /**
   * Reads an unsigned {@code short} as specified by
   * {@link DataInputStream#readUnsignedShort()}, except using little-endian
   * byte order.
   *
   * @return the next two bytes of the input stream, interpreted as an 
   *         unsigned 16-bit integer in little-endian byte order
   * @throws IOException if an I/O error occurs
   */
  @Override
  public int readUnsignedShort() throws IOException {
    byte b1 = readAndCheckByte();
    byte b2 = readAndCheckByte();

    return Ints.fromBytes((byte) 0, (byte) 0, b2, b1);
  }

  /**
   * Reads an integer as specified by {@link DataInputStream#readInt()}, except
   * using little-endian byte order.
   *
   * @return the next four bytes of the input stream, interpreted as an 
   *         {@code int} in little-endian byte order
   * @throws IOException if an I/O error occurs
   */
  @Override
  public int readInt() throws IOException {
    byte b1 = readAndCheckByte();
    byte b2 = readAndCheckByte();
    byte b3 = readAndCheckByte();
    byte b4 = readAndCheckByte();

    return Ints.fromBytes( b4, b3, b2, b1);
  }

  /**
   * Reads a {@code long} as specified by {@link DataInputStream#readLong()},
   * except using little-endian byte order.
   *
   * @return the next eight bytes of the input stream, interpreted as a 
   *         {@code long} in little-endian byte order
   * @throws IOException if an I/O error occurs
   */
  @Override
  public long readLong() throws IOException {
    byte b1 = readAndCheckByte();
    byte b2 = readAndCheckByte();
    byte b3 = readAndCheckByte();
    byte b4 = readAndCheckByte();
    byte b5 = readAndCheckByte();
    byte b6 = readAndCheckByte();
    byte b7 = readAndCheckByte();
    byte b8 = readAndCheckByte();

    return Longs.fromBytes(b8, b7, b6, b5, b4, b3, b2, b1);
  }

  /**
   * Reads a {@code float} as specified by {@link DataInputStream#readFloat()},
   * except using little-endian byte order.
   *
   * @return the next four bytes of the input stream, interpreted as a
   *         {@code float} in little-endian byte order
   * @throws IOException if an I/O error occurs
   */
  @Override
  public float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }

  /**
   * Reads a {@code double} as specified by
   * {@link DataInputStream#readDouble()}, except using little-endian byte
   * order.
   *
   * @return the next eight bytes of the input stream, interpreted as a
   *         {@code double} in little-endian byte order
   * @throws IOException if an I/O error occurs
   */
  @Override
  public double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }

  @Override
  public String readUTF() throws IOException {
    return new DataInputStream(in).readUTF();
  }

  /**
   * Reads a {@code short} as specified by {@link DataInputStream#readShort()},
   * except using little-endian byte order.
   *
   * @return the next two bytes of the input stream, interpreted as a
   *         {@code short} in little-endian byte order.
   * @throws IOException if an I/O error occurs.
   */
  @Override
  public short readShort() throws IOException {
    return (short) readUnsignedShort();
  }

  /**
   * Reads a char as specified by {@link DataInputStream#readChar()}, except
   * using little-endian byte order.
   *
   * @return the next two bytes of the input stream, interpreted as a 
   *         {@code char} in little-endian byte order
   * @throws IOException if an I/O error occurs
   */
  @Override
  public char readChar() throws IOException {
    return (char) readUnsignedShort();
  }

  @Override
  public byte readByte() throws IOException {
    return (byte) readUnsignedByte();
  }

  @Override
  public boolean readBoolean() throws IOException {
    return readUnsignedByte() != 0;
  }

  /**
   * Reads a byte from the input stream checking that the end of file (EOF)
   * has not been encountered.
   *  
   * @return byte read from input
   * @throws IOException if an error is encountered while reading
   * @throws EOFException if the end of file (EOF) is encountered.
   */
  private byte readAndCheckByte() throws IOException, EOFException {
    int b1 = in.read();

    if (-1 == b1) {
      throw new EOFException();
    }

    return (byte) b1;
  }

}

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

import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.ByteStreams.newInputStreamSupplier;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Bytes;
import com.google.common.testing.TestLogHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Random;

/**
 * Unit test for {@link ByteStreams}.
 *
 * @author Chris Nokleberg
 */
public class ByteStreamsTest extends IoTestCase {

  /** Provides an InputStream that throws an IOException on every read. */
  static final InputSupplier<InputStream> BROKEN_READ
      = new InputSupplier<InputStream>() {
        @Override
        public InputStream getInput() {
          return new InputStream() {
            @Override public int read() throws IOException {
              throw new IOException("broken read");
            }
          };
        }
      };

  /** Provides an OutputStream that throws an IOException on every write. */
  static final OutputSupplier<OutputStream> BROKEN_WRITE
      = new OutputSupplier<OutputStream>() {
        @Override
        public OutputStream getOutput() {
          return new OutputStream() {
            @Override public void write(int b) throws IOException {
              throw new IOException("broken write");
            }
          };
        }
      };

  /** Provides an InputStream that throws an IOException on close. */
  static final InputSupplier<InputStream> BROKEN_CLOSE_INPUT =
      new InputSupplier<InputStream>() {
        @Override
        public InputStream getInput() {
          return new FilterInputStream(new ByteArrayInputStream(new byte[10])) {
            @Override public void close() throws IOException {
              throw new IOException("broken close input");
            }
          };
        }
      };

  /** Provides an OutputStream that throws an IOException on every close. */
  static final OutputSupplier<OutputStream> BROKEN_CLOSE_OUTPUT =
      new OutputSupplier<OutputStream>() {
        @Override
        public OutputStream getOutput() {
          return new FilterOutputStream(new ByteArrayOutputStream()) {
            @Override public void close() throws IOException {
              throw new IOException("broken close output");
            }
          };
        }
      };

  /** Throws an IOException from getInput. */
  static final InputSupplier<InputStream> BROKEN_GET_INPUT =
      new InputSupplier<InputStream>() {
        @Override
        public InputStream getInput() throws IOException {
          throw new IOException("broken get input");
        }
      };

  /** Throws an IOException from getOutput. */
  static final OutputSupplier<OutputStream> BROKEN_GET_OUTPUT =
      new OutputSupplier<OutputStream>() {
        @Override
        public OutputStream getOutput() throws IOException {
          throw new IOException("broken get output");
        }
      };

  private static final ImmutableSet<InputSupplier<InputStream>> BROKEN_INPUTS =
      ImmutableSet.of(BROKEN_CLOSE_INPUT, BROKEN_GET_INPUT, BROKEN_READ);
  private static final ImmutableSet<OutputSupplier<OutputStream>> BROKEN_OUTPUTS
      = ImmutableSet.of(BROKEN_CLOSE_OUTPUT, BROKEN_GET_OUTPUT, BROKEN_WRITE);

  public void testByteSuppliers() throws IOException {
    byte[] range = newPreFilledByteArray(200);
    assertTrue(Arrays.equals(range,
        ByteStreams.toByteArray(ByteStreams.newInputStreamSupplier(range))));

    byte[] subRange = ByteStreams.toByteArray(
        ByteStreams.newInputStreamSupplier(range, 100, 50));
    assertEquals(50, subRange.length);
    assertEquals(100, subRange[0]);
    assertEquals((byte) 149, subRange[subRange.length - 1]);
  }

  public void testEqual() throws IOException {
    equalHelper(false, 0, 1);
    equalHelper(false, 400, 10000);
    equalHelper(false, 0x2000, 0x2001);
    equalHelper(false, new byte[]{ 0 }, new byte[]{ 1 });

    byte[] mutate = newPreFilledByteArray(10000);
    mutate[9000] = 0;
    equalHelper(false, mutate, newPreFilledByteArray(10000));

    equalHelper(true, 0, 0);
    equalHelper(true, 1, 1);
    equalHelper(true, 400, 400);

    final byte[] tenK = newPreFilledByteArray(10000);
    equalHelper(true, tenK, tenK);
    assertTrue(ByteStreams.equal(ByteStreams.newInputStreamSupplier(tenK),
        new InputSupplier<InputStream>() {
          @Override
          public InputStream getInput() {
            return new RandomAmountInputStream(new ByteArrayInputStream(tenK),
                new Random(301));
          }
        }));
  }

  private void equalHelper(boolean expect, int size1, int size2)
      throws IOException {
    equalHelper(expect, newPreFilledByteArray(size1),
        newPreFilledByteArray(size2));
  }

  private void equalHelper(boolean expect, byte[] a, byte[] b)
      throws IOException {
    assertEquals(expect, ByteStreams.equal(
        ByteStreams.newInputStreamSupplier(a),
        ByteStreams.newInputStreamSupplier(b)));
  }

  public void testAlwaysCloses() throws IOException {
    byte[] range = newPreFilledByteArray(100);
    CheckCloseSupplier.Input<InputStream> okRead
        = newCheckInput(ByteStreams.newInputStreamSupplier(range));
    CheckCloseSupplier.Output<OutputStream> okWrite
        = newCheckOutput(new OutputSupplier<OutputStream>() {
          @Override
          public OutputStream getOutput() {
            return new ByteArrayOutputStream();
          }
        });

    CheckCloseSupplier.Input<InputStream> brokenRead
        = newCheckInput(BROKEN_READ);
    CheckCloseSupplier.Output<OutputStream> brokenWrite
        = newCheckOutput(BROKEN_WRITE);

    // copy, both suppliers
    ByteStreams.copy(okRead, okWrite);
    assertTrue(okRead.areClosed());
    assertTrue(okWrite.areClosed());

    try {
      ByteStreams.copy(okRead, brokenWrite);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken write", e.getMessage());
    }
    assertTrue(okRead.areClosed());
    assertTrue(brokenWrite.areClosed());

    try {
      ByteStreams.copy(brokenRead, okWrite);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken read", e.getMessage());
    }
    assertTrue(brokenRead.areClosed());
    assertTrue(okWrite.areClosed());

    try {
      ByteStreams.copy(brokenRead, brokenWrite);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken read", e.getMessage());
    }
    assertTrue(brokenRead.areClosed());
    assertTrue(brokenWrite.areClosed());

    // copy, input supplier
    OutputStream out = okWrite.getOutput();
    ByteStreams.copy(okRead, out);
    assertTrue(okRead.areClosed());
    assertFalse(okWrite.areClosed());
    out.close();

    out = brokenWrite.getOutput();
    try {
      ByteStreams.copy(okRead, out);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken write", e.getMessage());
    }
    assertTrue(okRead.areClosed());
    assertFalse(brokenWrite.areClosed());
    out.close();

    out = okWrite.getOutput();
    try {
      ByteStreams.copy(brokenRead, out);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken read", e.getMessage());
    }
    assertTrue(brokenRead.areClosed());
    assertFalse(okWrite.areClosed());
    out.close();

    out = brokenWrite.getOutput();
    try {
      ByteStreams.copy(brokenRead, out);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken read", e.getMessage());
    }
    assertTrue(brokenRead.areClosed());
    assertFalse(brokenWrite.areClosed());
    out.close();

    // copy, output supplier
    InputStream in = okRead.getInput();
    ByteStreams.copy(in, okWrite);
    assertFalse(okRead.areClosed());
    assertTrue(okWrite.areClosed());
    in.close();

    in = okRead.getInput();
    try {
      ByteStreams.copy(in, brokenWrite);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken write", e.getMessage());
    }
    assertFalse(okRead.areClosed());
    assertTrue(brokenWrite.areClosed());
    in.close();

    in = brokenRead.getInput();
    try {
      ByteStreams.copy(in, okWrite);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken read", e.getMessage());
    }
    assertFalse(brokenRead.areClosed());
    assertTrue(okWrite.areClosed());
    in.close();

    in = brokenRead.getInput();
    try {
      ByteStreams.copy(in, brokenWrite);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken read", e.getMessage());
    }
    assertFalse(brokenRead.areClosed());
    assertTrue(brokenWrite.areClosed());
    in.close();

    // toByteArray
    assertTrue(Arrays.equals(range, ByteStreams.toByteArray(okRead)));
    assertTrue(okRead.areClosed());

    try {
      ByteStreams.toByteArray(brokenRead);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken read", e.getMessage());
    }
    assertTrue(brokenRead.areClosed());

    // equal
    try {
      ByteStreams.equal(brokenRead, okRead);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken read", e.getMessage());
    }
    assertTrue(brokenRead.areClosed());

    try {
      ByteStreams.equal(okRead, brokenRead);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken read", e.getMessage());
    }
    assertTrue(brokenRead.areClosed());

    // write
    try {
      ByteStreams.write(new byte[10], brokenWrite);
      fail("expected exception");
    } catch (IOException e) {
      assertEquals("broken write", e.getMessage());
    }
    assertTrue(brokenWrite.areClosed());
  }

  private static int getAndResetRecords(TestLogHandler logHandler) {
    int records = logHandler.getStoredLogRecords().size();
    logHandler.clear();
    return records;
  }

  private static void runFailureTest(
      InputSupplier<? extends InputStream> in, OutputSupplier<OutputStream> out) {
    try {
      copy(in, out);
      fail();
    } catch (IOException expected) {
    }
  }

  private static OutputSupplier<OutputStream> newByteArrayOutputStreamSupplier() {
    return new OutputSupplier<OutputStream>() {
      @Override public OutputStream getOutput() {
        return new ByteArrayOutputStream();
      }
    };
  }

  public void testWriteBytes() throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] expected = newPreFilledByteArray(100);
    ByteStreams.write(expected, new OutputSupplier<OutputStream>() {
      @Override public OutputStream getOutput() {
        return out;
      }
    });
    assertTrue(Arrays.equals(expected, out.toByteArray()));
  }

  public void testCopy() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] expected = newPreFilledByteArray(100);
    long num = ByteStreams.copy(new ByteArrayInputStream(expected), out);
    assertEquals(100, num);
    assertTrue(Arrays.equals(expected, out.toByteArray()));
  }

  public void testCopyChannel() throws IOException {
    byte[] expected = newPreFilledByteArray(100);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    WritableByteChannel outChannel = Channels.newChannel(out);

    ReadableByteChannel inChannel =
        Channels.newChannel(new ByteArrayInputStream(expected));
    ByteStreams.copy(inChannel, outChannel);
    assertTrue(Arrays.equals(expected, out.toByteArray()));
  }

  public void testReadFully() throws IOException {
    byte[] b = new byte[10];

    try {
      ByteStreams.readFully(newTestStream(10), null, 0, 10);
      fail("expected exception");
    } catch (NullPointerException e) {
    }

    try {
      ByteStreams.readFully(null, b, 0, 10);
      fail("expected exception");
    } catch (NullPointerException e) {
    }

    try {
      ByteStreams.readFully(newTestStream(10), b, -1, 10);
      fail("expected exception");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      ByteStreams.readFully(newTestStream(10), b, 0, -1);
      fail("expected exception");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      ByteStreams.readFully(newTestStream(10), b, 0, -1);
      fail("expected exception");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      ByteStreams.readFully(newTestStream(10), b, 2, 10);
      fail("expected exception");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      ByteStreams.readFully(newTestStream(5), b, 0, 10);
      fail("expected exception");
    } catch (EOFException e) {
    }

    Arrays.fill(b, (byte) 0);
    ByteStreams.readFully(newTestStream(10), b, 0, 0);
    assertTrue(Arrays.equals(new byte[10], b));

    Arrays.fill(b, (byte) 0);
    ByteStreams.readFully(newTestStream(10), b, 0, 10);
    assertTrue(Arrays.equals(newPreFilledByteArray(10), b));

    Arrays.fill(b, (byte) 0);
    ByteStreams.readFully(newTestStream(10), b, 0, 5);
    assertTrue(Arrays.equals(new byte[]{0, 1, 2, 3, 4, 0, 0, 0, 0, 0}, b));
  }

  public void testSkipFully() throws IOException {
    byte[] bytes = newPreFilledByteArray(100);
    skipHelper(0, 0, new ByteArrayInputStream(bytes));
    skipHelper(50, 50, new ByteArrayInputStream(bytes));
    skipHelper(50, 50, new SlowSkipper(new ByteArrayInputStream(bytes), 1));
    skipHelper(50, 50, new SlowSkipper(new ByteArrayInputStream(bytes), 0));
    skipHelper(100, -1, new ByteArrayInputStream(bytes));
    try {
      skipHelper(101, 0, new ByteArrayInputStream(bytes));
      fail("expected exception");
    } catch (EOFException e) {
    }
  }

  private void skipHelper(long n, int expect, InputStream in)
      throws IOException {
    ByteStreams.skipFully(in, n);
    assertEquals(expect, in.read());
    in.close();
  }

  // TODO(user): rename; violates rule that only immutable things can be all caps
  private static final byte[] BYTES = new byte[] {
      0x12, 0x34, 0x56, 0x78, 0x76, 0x54, 0x32, 0x10 };

  public void testNewDataInput_empty() {
    byte[] b = new byte[0];
    ByteArrayDataInput in = ByteStreams.newDataInput(b);
    try {
      in.readInt();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testNewDataInput_normal() {
    ByteArrayDataInput in = ByteStreams.newDataInput(BYTES);
    assertEquals(0x12345678, in.readInt());
    assertEquals(0x76543210, in.readInt());
    try {
      in.readInt();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testNewDataInput_readFully() {
    ByteArrayDataInput in = ByteStreams.newDataInput(BYTES);
    byte[] actual = new byte[BYTES.length];
    in.readFully(actual);
    assertEquals(BYTES, actual);
  }

  public void testNewDataInput_readFullyAndThenSome() {
    ByteArrayDataInput in = ByteStreams.newDataInput(BYTES);
    byte[] actual = new byte[BYTES.length * 2];
    try {
      in.readFully(actual);
      fail();
    } catch (IllegalStateException ex) {
      assertTrue(ex.getCause() instanceof EOFException);
    }
  }

  public void testNewDataInput_readFullyWithOffset() {
    ByteArrayDataInput in = ByteStreams.newDataInput(BYTES);
    byte[] actual = new byte[4];
    in.readFully(actual, 2, 2);
    assertEquals(0, actual[0]);
    assertEquals(0, actual[1]);
    assertEquals(BYTES[0], actual[2]);
    assertEquals(BYTES[1], actual[3]);
  }

  public void testNewDataInput_readLine() {
    ByteArrayDataInput in = ByteStreams.newDataInput(
        "This is a line\r\nThis too\rand this\nand also this".getBytes(Charsets.UTF_8));
    assertEquals("This is a line", in.readLine());
    assertEquals("This too", in.readLine());
    assertEquals("and this", in.readLine());
    assertEquals("and also this", in.readLine());
  }

  public void testNewDataInput_readFloat() {
    byte[] data = {0x12, 0x34, 0x56, 0x78, 0x76, 0x54, 0x32, 0x10};
    ByteArrayDataInput in = ByteStreams.newDataInput(data);
    assertEquals(Float.intBitsToFloat(0x12345678), in.readFloat(), 0.0);
    assertEquals(Float.intBitsToFloat(0x76543210), in.readFloat(), 0.0);
  }

  public void testNewDataInput_readDouble() {
    byte[] data = {0x12, 0x34, 0x56, 0x78, 0x76, 0x54, 0x32, 0x10};
    ByteArrayDataInput in = ByteStreams.newDataInput(data);
    assertEquals(Double.longBitsToDouble(0x1234567876543210L), in.readDouble(), 0.0);
  }

  public void testNewDataInput_readUTF() {
    byte[] data = new byte[17];
    data[1] = 15;
    System.arraycopy("Kilroy was here".getBytes(Charsets.UTF_8), 0, data, 2, 15);
    ByteArrayDataInput in = ByteStreams.newDataInput(data);
    assertEquals("Kilroy was here", in.readUTF());
  }

  public void testNewDataInput_readChar() {
    byte[] data = "qed".getBytes(Charsets.UTF_16BE);
    ByteArrayDataInput in = ByteStreams.newDataInput(data);
    assertEquals('q', in.readChar());
    assertEquals('e', in.readChar());
    assertEquals('d', in.readChar());
  }

  public void testNewDataInput_readUnsignedShort() {
    byte[] data = {0, 0, 0, 1, (byte) 0xFF, (byte) 0xFF, 0x12, 0x34};
    ByteArrayDataInput in = ByteStreams.newDataInput(data);
    assertEquals(0, in.readUnsignedShort());
    assertEquals(1, in.readUnsignedShort());
    assertEquals(65535, in.readUnsignedShort());
    assertEquals(0x1234, in.readUnsignedShort());
  }

  public void testNewDataInput_readLong() {
    byte[] data = {0x12, 0x34, 0x56, 0x78, 0x76, 0x54, 0x32, 0x10};
    ByteArrayDataInput in = ByteStreams.newDataInput(data);
    assertEquals(0x1234567876543210L, in.readLong());
  }

  public void testNewDataInput_readBoolean() {
    ByteArrayDataInput in = ByteStreams.newDataInput(BYTES);
    assertTrue(in.readBoolean());
  }

  public void testNewDataInput_readByte() {
    ByteArrayDataInput in = ByteStreams.newDataInput(BYTES);
    for (int i = 0; i < BYTES.length; i++) {
      assertEquals(BYTES[i], in.readByte());
    }
    try {
      in.readByte();
      fail();
    } catch (IllegalStateException ex) {
      assertTrue(ex.getCause() instanceof EOFException);
    }
  }

  public void testNewDataInput_readUnsignedByte() {
    ByteArrayDataInput in = ByteStreams.newDataInput(BYTES);
    for (int i = 0; i < BYTES.length; i++) {
      assertEquals(BYTES[i], in.readUnsignedByte());
    }
    try {
      in.readUnsignedByte();
      fail();
    } catch (IllegalStateException ex) {
      assertTrue(ex.getCause() instanceof EOFException);
    }
  }

  public void testNewDataInput_offset() {
    ByteArrayDataInput in = ByteStreams.newDataInput(BYTES, 2);
    assertEquals(0x56787654, in.readInt());
    try {
      in.readInt();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testNewDataInput_skip() {
    ByteArrayDataInput in = ByteStreams.newDataInput(new byte[2]);
    in.skipBytes(2);
    try {
      in.skipBytes(1);
    } catch (IllegalStateException expected) {
    }
  }

  public void testNewDataOutput_empty() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    assertEquals(0, out.toByteArray().length);
  }

  public void testNewDataOutput_writeInt() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeInt(0x12345678);
    out.writeInt(0x76543210);
    assertTrue(Arrays.equals(BYTES, out.toByteArray()));
  }

  public void testNewDataOutput_sized() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput(4);
    out.writeInt(0x12345678);
    out.writeInt(0x76543210);
    assertTrue(Arrays.equals(BYTES, out.toByteArray()));
  }

  public void testNewDataOutput_writeLong() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeLong(0x1234567876543210L);
    assertTrue(Arrays.equals(BYTES, out.toByteArray()));
  }

  public void testNewDataOutput_writeByteArray() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.write(BYTES);
    assertTrue(Arrays.equals(BYTES, out.toByteArray()));
  }

  public void testNewDataOutput_writeByte() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.write(0x12);
    out.writeByte(0x34);
    assertTrue(Arrays.equals(new byte[] {0x12, 0x34}, out.toByteArray()));
  }

  public void testNewDataOutput_writeByteOffset() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.write(BYTES, 4, 2);
    byte[] expected = {BYTES[4], BYTES[5]};
    assertEquals(expected, out.toByteArray());
  }

  public void testNewDataOutput_writeBoolean() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeBoolean(true);
    out.writeBoolean(false);
    byte[] expected = {(byte) 1, (byte) 0};
    assertEquals(expected, out.toByteArray());
  }

  public void testNewDataOutput_writeChar() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeChar('a');
    assertTrue(Arrays.equals(new byte[] {0, 97}, out.toByteArray()));
  }

  public void testNewDataOutput_writeChars() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeChars("r\u00C9sum\u00C9");
    // need to remove byte order mark before comparing
    byte[] expected = Arrays.copyOfRange("r\u00C9sum\u00C9".getBytes(Charsets.UTF_16), 2, 14);
    assertEquals(expected, out.toByteArray());
  }

  public void testNewDataOutput_writeUTF() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF("r\u00C9sum\u00C9");
    byte[] expected ="r\u00C9sum\u00C9".getBytes(Charsets.UTF_8);
    byte[] actual = out.toByteArray();
    // writeUTF writes the length of the string in 2 bytes
    assertEquals(0, actual[0]);
    assertEquals(expected.length, actual[1]);
    assertEquals(expected, Arrays.copyOfRange(actual, 2, actual.length));
  }

  public void testNewDataOutput_writeShort() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeShort(0x1234);
    assertTrue(Arrays.equals(new byte[] {0x12, 0x34}, out.toByteArray()));
  }

  public void testNewDataOutput_writeDouble() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeDouble(Double.longBitsToDouble(0x1234567876543210L));
    assertEquals(BYTES, out.toByteArray());
  }

  public void testNewDataOutput_writeFloat() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeFloat(Float.intBitsToFloat(0x12345678));
    out.writeFloat(Float.intBitsToFloat(0x76543210));
    assertEquals(BYTES, out.toByteArray());
  }

  public void testLength() throws IOException {
    lengthHelper(Long.MAX_VALUE);
    lengthHelper(7);
    lengthHelper(1);
    lengthHelper(0);

    assertEquals(0, ByteStreams.length(
        ByteStreams.newInputStreamSupplier(new byte[0])));
  }

  private void lengthHelper(final long skipLimit) throws IOException {
    assertEquals(100, ByteStreams.length(new InputSupplier<InputStream>() {
      @Override
      public InputStream getInput() {
        return new SlowSkipper(new ByteArrayInputStream(new byte[100]),
            skipLimit);
      }
    }));
  }

  public void testSlice() throws IOException {
    // Test preconditions
    InputSupplier<? extends InputStream> supplier
        = ByteStreams.newInputStreamSupplier(newPreFilledByteArray(100));
    try {
      ByteStreams.slice(supplier, -1, 10);
      fail("expected exception");
    } catch (IllegalArgumentException expected) {
    }

    try {
      ByteStreams.slice(supplier, 0, -1);
      fail("expected exception");
    } catch (IllegalArgumentException expected) {
    }

    try {
      ByteStreams.slice(null, 0, 10);
      fail("expected exception");
    } catch (NullPointerException expected) {
    }

    sliceHelper(0, 0, 0, 0);
    sliceHelper(0, 0, 1, 0);
    sliceHelper(100, 0, 10, 10);
    sliceHelper(100, 0, 100, 100);
    sliceHelper(100, 5, 10, 10);
    sliceHelper(100, 5, 100, 95);
    sliceHelper(100, 100, 0, 0);
    sliceHelper(100, 100, 10, 0);

    try {
      sliceHelper(100, 101, 10, 0);
      fail("expected exception");
    } catch (EOFException expected) {
    }
  }

  /**
   * @param input the size of the input stream
   * @param offset the first argument to {@link ByteStreams#slice}
   * @param length the second argument to {@link ByteStreams#slice}
   * @param expectRead the number of bytes we expect to read
   */
  private static void sliceHelper(
      int input, int offset, long length, int expectRead) throws IOException {
    Preconditions.checkArgument(expectRead == (int)
        Math.max(0, Math.min(input, offset + length) - offset));
    InputSupplier<? extends InputStream> supplier
        = ByteStreams.newInputStreamSupplier(newPreFilledByteArray(input));
    assertTrue(Arrays.equals(
        newPreFilledByteArray(offset, expectRead),
        ByteStreams.toByteArray(ByteStreams.slice(supplier, offset, length))));
  }

  private static InputStream newTestStream(int n) {
    return new ByteArrayInputStream(newPreFilledByteArray(n));
  }

  private static CheckCloseSupplier.Input<InputStream> newCheckInput(
      InputSupplier<? extends InputStream> delegate) {
    return new CheckCloseSupplier.Input<InputStream>(delegate) {
      @Override protected InputStream wrap(InputStream object,
          final Callback callback) {
        return new FilterInputStream(object) {
          @Override public void close() throws IOException {
            callback.delegateClosed();
            super.close();
          }
        };
      }
    };
  }

  private static CheckCloseSupplier.Output<OutputStream> newCheckOutput(
      OutputSupplier<? extends OutputStream> delegate) {
    return new CheckCloseSupplier.Output<OutputStream>(delegate) {
      @Override protected OutputStream wrap(OutputStream object,
          final Callback callback) {
        return new FilterOutputStream(object) {
          @Override public void close() throws IOException {
            callback.delegateClosed();
            super.close();
          }
        };
      }
    };
  }

  /** Stream that will skip a maximum number of bytes at a time. */
  private static class SlowSkipper extends FilterInputStream {
    private final long max;

    public SlowSkipper(InputStream in, long max) {
      super(in);
      this.max = max;
    }

    @Override public long skip(long n) throws IOException {
      return super.skip(Math.min(max, n));
    }
  }

  public void testByteProcessorStopEarly() throws IOException {
    byte[] array = newPreFilledByteArray(6000);
    assertEquals((Integer) 42,
        ByteStreams.readBytes(ByteStreams.newInputStreamSupplier(array),
            new ByteProcessor<Integer>() {
              @Override
              public boolean processBytes(byte[] buf, int off, int len) {
                assertTrue(Arrays.equals(
                    copyOfRange(buf, off, off + len),
                    newPreFilledByteArray(4096)));
                return false;
              }

              @Override
              public Integer getResult() {
                return 42;
              }
            }));
  }

  private static byte[] copyOfRange(byte[] in, int from, int to) {
    byte[] out = new byte[to - from];
    for (int i = 0; i < to - from; i++) {
      out[i] = in[from + i];
    }
    return out;
  }

  private static void assertEquals(byte[] expected, byte[] actual) {
    assertEquals(Bytes.asList(expected), Bytes.asList(actual));
  }
}

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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

/**
 * Unit test for {@link ByteStreams}.
 *
 * @author Chris Nokleberg
 */
public class ByteStreamsTest extends IoTestCase {

  public void testCopyChannel() throws IOException {
    byte[] expected = newPreFilledByteArray(100);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    WritableByteChannel outChannel = Channels.newChannel(out);

    ReadableByteChannel inChannel = Channels.newChannel(new ByteArrayInputStream(expected));
    ByteStreams.copy(inChannel, outChannel);
    assertThat(out.toByteArray()).isEqualTo(expected);
  }


  public void testCopyFileChannel() throws IOException {
    final int chunkSize = 14407; // Random prime, unlikely to match any internal chunk size
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    WritableByteChannel outChannel = Channels.newChannel(out);

    File testFile = createTempFile();
    byte[] dummyData = newPreFilledByteArray(chunkSize);
    try (FileOutputStream fos = new FileOutputStream(testFile)) {
      for (int i = 0; i < 500; i++) {
        fos.write(dummyData);
      }
    }
    try (ReadableByteChannel inChannel = new RandomAccessFile(testFile, "r").getChannel()) {
      ByteStreams.copy(inChannel, outChannel);
    }
    byte[] actual = out.toByteArray();
    for (int i = 0; i < 500 * chunkSize; i += chunkSize) {
      assertThat(Arrays.copyOfRange(actual, i, i + chunkSize)).isEqualTo(dummyData);
    }
  }

  public void testReadFully() throws IOException {
    byte[] b = new byte[10];

    try {
      ByteStreams.readFully(newTestStream(10), null, 0, 10);
      fail("expected exception");
    } catch (NullPointerException expected) {
    }

    try {
      ByteStreams.readFully(null, b, 0, 10);
      fail("expected exception");
    } catch (NullPointerException expected) {
    }

    try {
      ByteStreams.readFully(newTestStream(10), b, -1, 10);
      fail("expected exception");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      ByteStreams.readFully(newTestStream(10), b, 0, -1);
      fail("expected exception");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      ByteStreams.readFully(newTestStream(10), b, 0, -1);
      fail("expected exception");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      ByteStreams.readFully(newTestStream(10), b, 2, 10);
      fail("expected exception");
    } catch (IndexOutOfBoundsException expected) {
    }

    try {
      ByteStreams.readFully(newTestStream(5), b, 0, 10);
      fail("expected exception");
    } catch (EOFException expected) {
    }

    Arrays.fill(b, (byte) 0);
    ByteStreams.readFully(newTestStream(10), b, 0, 0);
    assertThat(b).isEqualTo(new byte[10]);

    Arrays.fill(b, (byte) 0);
    ByteStreams.readFully(newTestStream(10), b, 0, 10);
    assertThat(b).isEqualTo(newPreFilledByteArray(10));

    Arrays.fill(b, (byte) 0);
    ByteStreams.readFully(newTestStream(10), b, 0, 5);
    assertThat(b).isEqualTo(new byte[] {0, 1, 2, 3, 4, 0, 0, 0, 0, 0});
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
    } catch (EOFException expected) {
    }
  }

  private static void skipHelper(long n, int expect, InputStream in) throws IOException {
    ByteStreams.skipFully(in, n);
    assertEquals(expect, in.read());
    in.close();
  }

  private static final byte[] bytes = new byte[] {0x12, 0x34, 0x56, 0x78, 0x76, 0x54, 0x32, 0x10};

  public void testNewDataInput_empty() {
    byte[] b = new byte[0];
    ByteArrayDataInput in = ByteStreams.newDataInput(b);
    try {
      in.readInt();
      fail("expected exception");
    } catch (IllegalStateException expected) {
    }
  }

  public void testNewDataInput_normal() {
    ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
    assertEquals(0x12345678, in.readInt());
    assertEquals(0x76543210, in.readInt());
    try {
      in.readInt();
      fail("expected exception");
    } catch (IllegalStateException expected) {
    }
  }

  public void testNewDataInput_readFully() {
    ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
    byte[] actual = new byte[bytes.length];
    in.readFully(actual);
    assertThat(actual).isEqualTo(bytes);
  }

  public void testNewDataInput_readFullyAndThenSome() {
    ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
    byte[] actual = new byte[bytes.length * 2];
    try {
      in.readFully(actual);
      fail("expected exception");
    } catch (IllegalStateException ex) {
      assertThat(ex).hasCauseThat().isInstanceOf(EOFException.class);
    }
  }

  public void testNewDataInput_readFullyWithOffset() {
    ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
    byte[] actual = new byte[4];
    in.readFully(actual, 2, 2);
    assertEquals(0, actual[0]);
    assertEquals(0, actual[1]);
    assertEquals(bytes[0], actual[2]);
    assertEquals(bytes[1], actual[3]);
  }

  public void testNewDataInput_readLine() {
    ByteArrayDataInput in =
        ByteStreams.newDataInput(
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
    ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
    assertTrue(in.readBoolean());
  }

  public void testNewDataInput_readByte() {
    ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
    for (byte aByte : bytes) {
      assertEquals(aByte, in.readByte());
    }
    try {
      in.readByte();
      fail("expected exception");
    } catch (IllegalStateException expected) {
      assertThat(expected).hasCauseThat().isInstanceOf(EOFException.class);
    }
  }

  public void testNewDataInput_readUnsignedByte() {
    ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
    for (byte aByte : bytes) {
      assertEquals(aByte, in.readUnsignedByte());
    }
    try {
      in.readUnsignedByte();
      fail("expected exception");
    } catch (IllegalStateException expected) {
      assertThat(expected).hasCauseThat().isInstanceOf(EOFException.class);
    }
  }

  public void testNewDataInput_offset() {
    ByteArrayDataInput in = ByteStreams.newDataInput(bytes, 2);
    assertEquals(0x56787654, in.readInt());
    try {
      in.readInt();
      fail("expected exception");
    } catch (IllegalStateException expected) {
    }
  }

  public void testNewDataInput_skip() {
    ByteArrayDataInput in = ByteStreams.newDataInput(new byte[2]);
    assertEquals(2, in.skipBytes(2));
    assertEquals(0, in.skipBytes(1));
  }

  public void testNewDataInput_BAIS() {
    ByteArrayInputStream bais = new ByteArrayInputStream(new byte[] {0x12, 0x34, 0x56, 0x78});
    ByteArrayDataInput in = ByteStreams.newDataInput(bais);
    assertEquals(0x12345678, in.readInt());
  }

  public void testNewDataOutput_empty() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    assertEquals(0, out.toByteArray().length);
  }

  public void testNewDataOutput_writeInt() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeInt(0x12345678);
    out.writeInt(0x76543210);
    assertThat(out.toByteArray()).isEqualTo(bytes);
  }

  public void testNewDataOutput_sized() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput(4);
    out.writeInt(0x12345678);
    out.writeInt(0x76543210);
    assertThat(out.toByteArray()).isEqualTo(bytes);
  }

  public void testNewDataOutput_writeLong() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeLong(0x1234567876543210L);
    assertThat(out.toByteArray()).isEqualTo(bytes);
  }

  public void testNewDataOutput_writeByteArray() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.write(bytes);
    assertThat(out.toByteArray()).isEqualTo(bytes);
  }

  public void testNewDataOutput_writeByte() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.write(0x12);
    out.writeByte(0x34);
    assertThat(out.toByteArray()).isEqualTo(new byte[] {0x12, 0x34});
  }

  public void testNewDataOutput_writeByteOffset() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.write(bytes, 4, 2);
    byte[] expected = {bytes[4], bytes[5]};
    assertThat(out.toByteArray()).isEqualTo(expected);
  }

  public void testNewDataOutput_writeBoolean() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeBoolean(true);
    out.writeBoolean(false);
    byte[] expected = {(byte) 1, (byte) 0};
    assertThat(out.toByteArray()).isEqualTo(expected);
  }

  public void testNewDataOutput_writeChar() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeChar('a');
    assertThat(out.toByteArray()).isEqualTo(new byte[] {0, 97});
  }

  // Hardcoded because of Android problems. See testUtf16Expected.
  private static final byte[] utf16ExpectedWithBom =
      new byte[] {-2, -1, 0, 114, 0, -55, 0, 115, 0, 117, 0, 109, 0, -55};

  public void testNewDataOutput_writeChars() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeChars("r\u00C9sum\u00C9");
    // need to remove byte order mark before comparing
    byte[] expected = Arrays.copyOfRange(utf16ExpectedWithBom, 2, 14);
    assertThat(out.toByteArray()).isEqualTo(expected);
  }

  @AndroidIncompatible // https://code.google.com/p/android/issues/detail?id=196848
  public void testUtf16Expected() {
    byte[] hardcodedExpected = utf16ExpectedWithBom;
    byte[] computedExpected = "r\u00C9sum\u00C9".getBytes(Charsets.UTF_16);
    assertThat(computedExpected).isEqualTo(hardcodedExpected);
  }

  public void testNewDataOutput_writeUTF() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF("r\u00C9sum\u00C9");
    byte[] expected = "r\u00C9sum\u00C9".getBytes(Charsets.UTF_8);
    byte[] actual = out.toByteArray();
    // writeUTF writes the length of the string in 2 bytes
    assertEquals(0, actual[0]);
    assertEquals(expected.length, actual[1]);
    assertThat(Arrays.copyOfRange(actual, 2, actual.length)).isEqualTo(expected);
  }

  public void testNewDataOutput_writeShort() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeShort(0x1234);
    assertThat(out.toByteArray()).isEqualTo(new byte[] {0x12, 0x34});
  }

  public void testNewDataOutput_writeDouble() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeDouble(Double.longBitsToDouble(0x1234567876543210L));
    assertThat(out.toByteArray()).isEqualTo(bytes);
  }

  public void testNewDataOutput_writeFloat() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeFloat(Float.intBitsToFloat(0x12345678));
    out.writeFloat(Float.intBitsToFloat(0x76543210));
    assertThat(out.toByteArray()).isEqualTo(bytes);
  }

  public void testNewDataOutput_BAOS() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ByteArrayDataOutput out = ByteStreams.newDataOutput(baos);
    out.writeInt(0x12345678);
    assertEquals(4, baos.size());
    assertThat(baos.toByteArray()).isEqualTo(new byte[] {0x12, 0x34, 0x56, 0x78});
  }

  private static final byte[] PRE_FILLED_100 = newPreFilledByteArray(100);

  public void testToByteArray() throws IOException {
    InputStream in = new ByteArrayInputStream(PRE_FILLED_100);
    byte[] b = ByteStreams.toByteArray(in);
    assertThat(b).isEqualTo(PRE_FILLED_100);
  }

  public void testToByteArray_emptyStream() throws IOException {
    InputStream in = newTestStream(0);
    byte[] b = ByteStreams.toByteArray(in);
    assertThat(b).isEqualTo(new byte[0]);
  }

  public void testToByteArray_largeStream() throws IOException {
    // well, large enough to require multiple buffers
    byte[] expected = newPreFilledByteArray(10000000);
    InputStream in = new ByteArrayInputStream(expected);
    byte[] b = ByteStreams.toByteArray(in);
    assertThat(b).isEqualTo(expected);
  }

  public void testToByteArray_withSize_givenCorrectSize() throws IOException {
    InputStream in = new ByteArrayInputStream(PRE_FILLED_100);
    byte[] b = ByteStreams.toByteArray(in, 100);
    assertThat(b).isEqualTo(PRE_FILLED_100);
  }

  public void testToByteArray_withSize_givenSmallerSize() throws IOException {
    InputStream in = new ByteArrayInputStream(PRE_FILLED_100);
    byte[] b = ByteStreams.toByteArray(in, 80);
    assertThat(b).isEqualTo(PRE_FILLED_100);
  }

  public void testToByteArray_withSize_givenLargerSize() throws IOException {
    InputStream in = new ByteArrayInputStream(PRE_FILLED_100);
    byte[] b = ByteStreams.toByteArray(in, 120);
    assertThat(b).isEqualTo(PRE_FILLED_100);
  }

  public void testToByteArray_withSize_givenSizeZero() throws IOException {
    InputStream in = new ByteArrayInputStream(PRE_FILLED_100);
    byte[] b = ByteStreams.toByteArray(in, 0);
    assertThat(b).isEqualTo(PRE_FILLED_100);
  }

  public void testToByteArray_withSize_givenSizeOneSmallerThanActual() throws IOException {
    InputStream in = new ByteArrayInputStream(PRE_FILLED_100);
    // this results in toByteArrayInternal being called when the stream is actually exhausted
    byte[] b = ByteStreams.toByteArray(in, 99);
    assertThat(b).isEqualTo(PRE_FILLED_100);
  }

  public void testToByteArray_withSize_givenSizeTwoSmallerThanActual() throws IOException {
    InputStream in = new ByteArrayInputStream(PRE_FILLED_100);
    byte[] b = ByteStreams.toByteArray(in, 98);
    assertThat(b).isEqualTo(PRE_FILLED_100);
  }

  public void testExhaust() throws IOException {
    InputStream in = newTestStream(100);
    assertEquals(100, ByteStreams.exhaust(in));
    assertEquals(-1, in.read());
    assertEquals(0, ByteStreams.exhaust(in));

    InputStream empty = newTestStream(0);
    assertEquals(0, ByteStreams.exhaust(empty));
    assertEquals(-1, empty.read());
  }

  private static InputStream newTestStream(int n) {
    return new ByteArrayInputStream(newPreFilledByteArray(n));
  }

  /** Stream that will skip a maximum number of bytes at a time. */
  private static class SlowSkipper extends FilterInputStream {
    private final long max;

    SlowSkipper(InputStream in, long max) {
      super(in);
      this.max = max;
    }

    @Override
    public long skip(long n) throws IOException {
      return super.skip(Math.min(max, n));
    }
  }

  public void testReadBytes() throws IOException {
    final byte[] array = newPreFilledByteArray(1000);
    assertThat(ByteStreams.readBytes(new ByteArrayInputStream(array), new TestByteProcessor()))
        .isEqualTo(array);
  }

  private static class TestByteProcessor implements ByteProcessor<byte[]> {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @Override
    public boolean processBytes(byte[] buf, int off, int len) {
      out.write(buf, off, len);
      return true;
    }

    @Override
    public byte[] getResult() {
      return out.toByteArray();
    }
  }

  public void testByteProcessorStopEarly() throws IOException {
    byte[] array = newPreFilledByteArray(10000);
    assertEquals(
        (Integer) 42,
        ByteStreams.readBytes(
            new ByteArrayInputStream(array),
            new ByteProcessor<Integer>() {
              @Override
              public boolean processBytes(byte[] buf, int off, int len) {
                assertThat(newPreFilledByteArray(8192))
                    .isEqualTo(Arrays.copyOfRange(buf, off, off + len));
                return false;
              }

              @Override
              public Integer getResult() {
                return 42;
              }
            }));
  }

  public void testNullOutputStream() throws Exception {
    // create a null output stream
    OutputStream nos = ByteStreams.nullOutputStream();
    // write to the output stream
    nos.write('n');
    String test = "Test string for NullOutputStream";
    byte[] bytes = test.getBytes(Charsets.US_ASCII);
    nos.write(bytes);
    nos.write(bytes, 2, 10);
    nos.write(bytes, bytes.length - 5, 5);
    // nothing really to assert?
    assertSame(ByteStreams.nullOutputStream(), ByteStreams.nullOutputStream());
  }

  public void testNullOutputStream_exceptions() throws Exception {
    OutputStream nos = ByteStreams.nullOutputStream();
    try {
      nos.write(null);
      fail();
    } catch (NullPointerException expected) {
    }
    try {
      nos.write(null, 0, 1);
      fail();
    } catch (NullPointerException expected) {
    }
    byte[] tenBytes = new byte[10];
    try {
      nos.write(tenBytes, -1, 1);
      fail("Expected exception from negative offset");
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      nos.write(tenBytes, 1, -1);
      fail("Expected exception from negative length");
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      nos.write(tenBytes, 9, 2);
      fail("Expected exception from offset+length > array size");
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      nos.write(tenBytes, 9, 100);
      fail("Expected exception from offset+length > array size");
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testLimit() throws Exception {
    byte[] big = newPreFilledByteArray(5);
    InputStream bin = new ByteArrayInputStream(big);
    InputStream lin = ByteStreams.limit(bin, 2);

    // also test available
    lin.mark(2);
    assertEquals(2, lin.available());
    int read = lin.read();
    assertEquals(big[0], read);
    assertEquals(1, lin.available());
    read = lin.read();
    assertEquals(big[1], read);
    assertEquals(0, lin.available());
    read = lin.read();
    assertEquals(-1, read);

    lin.reset();
    byte[] small = new byte[5];
    read = lin.read(small);
    assertEquals(2, read);
    assertEquals(big[0], small[0]);
    assertEquals(big[1], small[1]);

    lin.reset();
    read = lin.read(small, 2, 3);
    assertEquals(2, read);
    assertEquals(big[0], small[2]);
    assertEquals(big[1], small[3]);
  }

  public void testLimit_mark() throws Exception {
    byte[] big = newPreFilledByteArray(5);
    InputStream bin = new ByteArrayInputStream(big);
    InputStream lin = ByteStreams.limit(bin, 2);

    int read = lin.read();
    assertEquals(big[0], read);
    lin.mark(2);

    read = lin.read();
    assertEquals(big[1], read);
    read = lin.read();
    assertEquals(-1, read);

    lin.reset();
    read = lin.read();
    assertEquals(big[1], read);
    read = lin.read();
    assertEquals(-1, read);
  }

  public void testLimit_skip() throws Exception {
    byte[] big = newPreFilledByteArray(5);
    InputStream bin = new ByteArrayInputStream(big);
    InputStream lin = ByteStreams.limit(bin, 2);

    // also test available
    lin.mark(2);
    assertEquals(2, lin.available());
    lin.skip(1);
    assertEquals(1, lin.available());

    lin.reset();
    assertEquals(2, lin.available());
    lin.skip(3);
    assertEquals(0, lin.available());
  }

  public void testLimit_markNotSet() {
    byte[] big = newPreFilledByteArray(5);
    InputStream bin = new ByteArrayInputStream(big);
    InputStream lin = ByteStreams.limit(bin, 2);

    try {
      lin.reset();
      fail();
    } catch (IOException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("Mark not set");
    }
  }

  public void testLimit_markNotSupported() {
    InputStream lin = ByteStreams.limit(new UnmarkableInputStream(), 2);

    try {
      lin.reset();
      fail();
    } catch (IOException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("Mark not supported");
    }
  }

  private static class UnmarkableInputStream extends InputStream {
    @Override
    public int read() throws IOException {
      return 0;
    }

    @Override
    public boolean markSupported() {
      return false;
    }
  }
}

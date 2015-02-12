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

import static com.google.common.base.Charsets.UTF_16;
import com.google.common.base.Charsets;
import com.google.common.jdk5backport.Arrays;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

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

    ReadableByteChannel inChannel =
        Channels.newChannel(new ByteArrayInputStream(expected));
    ByteStreams.copy(inChannel, outChannel);
    assertEquals(expected, out.toByteArray());
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
    assertEquals(new byte[10], b);

    Arrays.fill(b, (byte) 0);
    ByteStreams.readFully(newTestStream(10), b, 0, 10);
    assertEquals(newPreFilledByteArray(10), b);

    Arrays.fill(b, (byte) 0);
    ByteStreams.readFully(newTestStream(10), b, 0, 5);
    assertEquals(new byte[]{0, 1, 2, 3, 4, 0, 0, 0, 0, 0}, b);
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

  private static void skipHelper(long n, int expect, InputStream in)
      throws IOException {
    ByteStreams.skipFully(in, n);
    assertEquals(expect, in.read());
    in.close();
  }

  private static final byte[] bytes =
      new byte[] { 0x12, 0x34, 0x56, 0x78, 0x76, 0x54, 0x32, 0x10 };

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
    assertEquals(bytes, actual);
  }
  
  public void testNewDataInput_readFullyAndThenSome() {
    ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
    byte[] actual = new byte[bytes.length * 2];
    try {
      in.readFully(actual);
      fail("expected exception");
    } catch (IllegalStateException ex) {
      assertTrue(ex.getCause() instanceof EOFException);
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
  
  public void testNewDataInput_readLine() throws UnsupportedEncodingException {
    ByteArrayDataInput in = ByteStreams.newDataInput(
        "This is a line\r\nThis too\rand this\nand also this".getBytes(Charsets.UTF_8.name()));
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

  public void testNewDataInput_readUTF() throws UnsupportedEncodingException {
    byte[] data = new byte[17];
    data[1] = 15;
    System.arraycopy("Kilroy was here".getBytes(Charsets.UTF_8.name()), 0, data, 2, 15);
    ByteArrayDataInput in = ByteStreams.newDataInput(data);
    assertEquals("Kilroy was here", in.readUTF());
  }

  public void testNewDataInput_readChar() throws UnsupportedEncodingException {
    byte[] data = "qed".getBytes(Charsets.UTF_16BE.name());
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
    for (int i = 0; i < bytes.length; i++) {
      assertEquals(bytes[i], in.readByte());
    }
    try {
      in.readByte();
      fail("expected exception");
    } catch (IllegalStateException ex) {
      assertTrue(ex.getCause() instanceof EOFException);
    }
  }

  public void testNewDataInput_readUnsignedByte() {
    ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
    for (int i = 0; i < bytes.length; i++) {
      assertEquals(bytes[i], in.readUnsignedByte());
    }
    try {
      in.readUnsignedByte();
      fail("expected exception");
    } catch (IllegalStateException ex) {
      assertTrue(ex.getCause() instanceof EOFException);
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
    assertEquals(bytes, out.toByteArray());
  }

  public void testNewDataOutput_sized() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput(4);
    out.writeInt(0x12345678);
    out.writeInt(0x76543210);
    assertEquals(bytes, out.toByteArray());
  }

  public void testNewDataOutput_writeLong() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeLong(0x1234567876543210L);
    assertEquals(bytes, out.toByteArray());
  }

  public void testNewDataOutput_writeByteArray() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.write(bytes);
    assertEquals(bytes, out.toByteArray());
  }

  public void testNewDataOutput_writeByte() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.write(0x12);
    out.writeByte(0x34);
    assertEquals(new byte[] {0x12, 0x34}, out.toByteArray());
  }

  public void testNewDataOutput_writeByteOffset() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.write(bytes, 4, 2);
    byte[] expected = {bytes[4], bytes[5]};
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
    assertEquals(new byte[] {0, 97}, out.toByteArray());
  }

  public void testNewDataOutput_writeChars() throws UnsupportedEncodingException {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeChars("r\u00C9sum\u00C9");
    // need to remove byte order mark before comparing
    byte[] expected = Arrays.copyOfRange("r\u00C9sum\u00C9".getBytes(UTF_16.name()), 2, 14);
    assertEquals(expected, out.toByteArray());
  }

  public void testNewDataOutput_writeUTF() throws UnsupportedEncodingException {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF("r\u00C9sum\u00C9");
    byte[] expected ="r\u00C9sum\u00C9".getBytes(Charsets.UTF_8.name());
    byte[] actual = out.toByteArray();
    // writeUTF writes the length of the string in 2 bytes
    assertEquals(0, actual[0]);
    assertEquals(expected.length, actual[1]);
    assertEquals(expected, Arrays.copyOfRange(actual, 2, actual.length));
  }

  public void testNewDataOutput_writeShort() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeShort(0x1234);
    assertEquals(new byte[] {0x12, 0x34}, out.toByteArray());
  }

  public void testNewDataOutput_writeDouble() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeDouble(Double.longBitsToDouble(0x1234567876543210L));
    assertEquals(bytes, out.toByteArray());
  }

  public void testNewDataOutput_writeFloat() {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeFloat(Float.intBitsToFloat(0x12345678));
    out.writeFloat(Float.intBitsToFloat(0x76543210));
    assertEquals(bytes, out.toByteArray());
  }

  public void testChecksum() throws IOException {
    InputSupplier<ByteArrayInputStream> asciiBytes =
        ByteStreams.newInputStreamSupplier(ASCII.getBytes(Charsets.US_ASCII.name()));
    InputSupplier<ByteArrayInputStream> i18nBytes =
        ByteStreams.newInputStreamSupplier(I18N.getBytes(Charsets.UTF_8.name()));

    Checksum checksum = new CRC32();
    assertEquals(0L, checksum.getValue());
    assertEquals(3145994718L, ByteStreams.getChecksum(asciiBytes, checksum));
    assertEquals(0L, checksum.getValue());
    assertEquals(3145994718L, ByteStreams.getChecksum(asciiBytes, checksum));
    assertEquals(1138302340L, ByteStreams.getChecksum(i18nBytes, checksum));
    assertEquals(0L, checksum.getValue());
  }

  public void testNewDataOutput_BAOS() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ByteArrayDataOutput out = ByteStreams.newDataOutput(baos);
    out.writeInt(0x12345678);
    assertEquals(4, baos.size());
    assertEquals(new byte[] {0x12, 0x34, 0x56, 0x78}, baos.toByteArray());
  }

  public void testToByteArray_withSize_givenCorrectSize() throws IOException {
    InputStream in = newTestStream(100);
    byte[] b = ByteStreams.toByteArray(in, 100);
    assertEquals(100, b.length);
  }

  public void testToByteArray_withSize_givenSmallerSize() throws IOException {
    InputStream in = newTestStream(100);
    byte[] b = ByteStreams.toByteArray(in, 80);
    assertEquals(100, b.length);
  }

  public void testToByteArray_withSize_givenLargerSize() throws IOException {
    InputStream in = newTestStream(100);
    byte[] b = ByteStreams.toByteArray(in, 120);
    assertEquals(100, b.length);
  }

  public void testToByteArray_withSize_givenSizeZero() throws IOException {
    InputStream in = newTestStream(100);
    byte[] b = ByteStreams.toByteArray(in, 0);
    assertEquals(100, b.length);
  }

  private static InputStream newTestStream(int n) {
    return new ByteArrayInputStream(newPreFilledByteArray(n));
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

  public void testReadBytes() throws IOException {
    final byte[] array = newPreFilledByteArray(1000);
    assertEquals(array, ByteStreams.readBytes(
      new ByteArrayInputStream(array), new TestByteProcessor()));
  }

  private class TestByteProcessor implements ByteProcessor<byte[]> {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @Override
    public boolean processBytes(byte[] buf, int off, int len)
        throws IOException {
      out.write(buf, off, len);
      return true;
    }

    @Override
    public byte[] getResult() {
      return out.toByteArray();
    }
  }

  public void testByteProcessorStopEarly() throws IOException {
    byte[] array = newPreFilledByteArray(6000);
    assertEquals((Integer) 42,
        ByteStreams.readBytes(new ByteArrayInputStream(array),
            new ByteProcessor<Integer>() {
              @Override
              public boolean processBytes(byte[] buf, int off, int len) {
                assertEquals(
                    copyOfRange(buf, off, off + len),
                    newPreFilledByteArray(4096));
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
    nos.write(test.getBytes());
    nos.write(test.getBytes(), 2, 10);
    // nothing really to assert?
    assertSame(ByteStreams.nullOutputStream(), ByteStreams.nullOutputStream());
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
      assertEquals("Mark not set", expected.getMessage());
    }
  }
  
  public void testLimit_markNotSupported() {
    InputStream lin = ByteStreams.limit(new UnmarkableInputStream(), 2);

    try {
      lin.reset();
      fail();
    } catch (IOException expected) {
      assertEquals("Mark not supported", expected.getMessage());
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

  private static byte[] copyOfRange(byte[] in, int from, int to) {
    byte[] out = new byte[to - from];
    for (int i = 0; i < to - from; i++) {
      out[i] = in[from + i];
    }
    return out;
  }

  private static void assertEquals(byte[] expected, byte[] actual) {
    assertTrue(Arrays.equals(expected, actual));
  }
}

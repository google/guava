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

import static com.google.common.primitives.Bytes.asList;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.Math.min;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jspecify.annotations.NullUnmarked;

/**
 * Test class for {@link LittleEndianDataInputStream}.
 *
 * @author Chris Nokleberg
 */
@NullUnmarked
public class LittleEndianDataInputStreamTest extends IoTestCase {

  private byte[] data;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);

    initializeData(out);

    data = baos.toByteArray();
  }

  private static void initializeData(DataOutputStream out) throws IOException {
    /* Write out various test values NORMALLY */
    out.write(new byte[] {-100, 100});
    out.writeBoolean(true);
    out.writeBoolean(false);
    out.writeByte(100);
    out.writeByte(-100);
    out.writeByte((byte) 200);
    out.writeChar('a');
    out.writeShort((short) -30000);
    out.writeShort((short) 50000);
    out.writeInt(0xCAFEBABE);
    out.writeLong(0xDEADBEEFCAFEBABEL);
    out.writeUTF("Herby Derby");
    out.writeFloat(Float.intBitsToFloat(0xCAFEBABE));
    out.writeDouble(Double.longBitsToDouble(0xDEADBEEFCAFEBABEL));
  }

  public void testReadFully() throws IOException {
    DataInput in = new LittleEndianDataInputStream(new ByteArrayInputStream(data));
    byte[] b = new byte[data.length];
    in.readFully(b);
    assertEquals(asList(data), asList(b));
  }

  public void testReadUnsignedByte_eof() {
    DataInput in = new LittleEndianDataInputStream(new ByteArrayInputStream(new byte[0]));
    assertThrows(EOFException.class, in::readUnsignedByte);
  }

  public void testReadUnsignedShort_eof() {
    byte[] buf = {23};
    DataInput in = new LittleEndianDataInputStream(new ByteArrayInputStream(buf));
    assertThrows(EOFException.class, in::readUnsignedShort);
  }

  @SuppressWarnings("DoNotCall")
  public void testReadLine() {
    DataInput in = new LittleEndianDataInputStream(new ByteArrayInputStream(data));
    UnsupportedOperationException expected =
        assertThrows(UnsupportedOperationException.class, in::readLine);
    assertThat(expected).hasMessageThat().isEqualTo("readLine is not supported");
  }

  public void testReadLittleEndian() throws IOException {
    DataInput in = new LittleEndianDataInputStream(new ByteArrayInputStream(data));

    /* Read in various values in LITTLE ENDIAN FORMAT */
    byte[] b = new byte[2];
    in.readFully(b);
    assertEquals(-100, b[0]);
    assertEquals(100, b[1]);
    assertEquals(true, in.readBoolean());
    assertEquals(false, in.readBoolean());
    assertEquals(100, in.readByte());
    assertEquals(-100, in.readByte());
    assertEquals(200, in.readUnsignedByte());
    assertEquals('\u6100', in.readChar());
    assertEquals(-12150, in.readShort());
    assertEquals(20675, in.readUnsignedShort());
    assertEquals(0xBEBAFECA, in.readInt());
    assertEquals(0xBEBAFECAEFBEADDEL, in.readLong());
    assertThat(in.readUTF()).isEqualTo("Herby Derby");
    assertEquals(0xBEBAFECA, Float.floatToIntBits(in.readFloat()));
    assertEquals(0xBEBAFECAEFBEADDEL, Double.doubleToLongBits(in.readDouble()));
  }

  public void testSkipBytes() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);

    /* Write out various test values NORMALLY */
    out.write(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}); // 10 bytes of junk to skip
    initializeData(out);

    byte[] data = baos.toByteArray();

    DataInput in = new LittleEndianDataInputStream(new ByteArrayInputStream(data));
    int bytesSkipped = 0;
    while (bytesSkipped < 10) {
      bytesSkipped += in.skipBytes(10 - bytesSkipped);
    }

    /* Read in various values in LITTLE ENDIAN FORMAT */
    byte[] b = new byte[2];
    in.readFully(b);
    assertEquals(-100, b[0]);
    assertEquals(100, b[1]);
    assertTrue(in.readBoolean());
    assertFalse(in.readBoolean());
  }

  public void testSkipBytes_negative() throws IOException {
    byte[] data = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    File file = createTempFile();
    Files.write(data, file);
    // Unlike most streams, FileInputStream moves backward when asked to skip a negative count.
    try (FileInputStream fin = new FileInputStream(file)) {
      DataInput in = new LittleEndianDataInputStream(fin);
      assertEquals(0x03020100, in.readInt());

      assertEquals(0, in.skipBytes(-4));
      assertEquals(4, in.readUnsignedByte());
    }
  }

  public void testSkipBytes_shortSkip() throws IOException {
    byte[] data = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    // A delegate that skips at most one byte per call, as BufferedInputStream can.
    InputStream oneAtATime =
        new FilterInputStream(new ByteArrayInputStream(data)) {
          @Override
          public long skip(long n) throws IOException {
            return super.skip(min(n, 1));
          }
        };
    DataInput in = new LittleEndianDataInputStream(oneAtATime);

    assertEquals(8, in.skipBytes(8));
    assertEquals(8, in.readUnsignedByte());
  }

  public void testSkipBytes_zeroSkip() throws IOException {
    byte[] data = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    // A delegate whose skip returns 0 before EOF, as CipherInputStream can.
    InputStream zeroSkipper =
        new FilterInputStream(new ByteArrayInputStream(data)) {
          @Override
          public long skip(long n) {
            return 0;
          }
        };
    DataInput in = new LittleEndianDataInputStream(zeroSkipper);

    assertEquals(8, in.skipBytes(8));
    assertEquals(8, in.readUnsignedByte());
  }

  public void testSkipBytes_eof() throws IOException {
    DataInput in = new LittleEndianDataInputStream(new ByteArrayInputStream(new byte[] {0, 1, 2}));

    assertEquals(3, in.skipBytes(10));
    assertEquals(0, in.skipBytes(1));
  }

  public void testSkipBytes_eofFileInputStream() throws IOException {
    File file = createTempFile();
    Files.write(new byte[] {0, 1, 2}, file);
    // Unlike most streams, FileInputStream can skip past EOF.
    try (FileInputStream fin = new FileInputStream(file)) {
      DataInput in = new LittleEndianDataInputStream(fin);
      assertEquals(3, in.skipBytes(10));
      assertEquals(0, in.skipBytes(1));
    }
  }
}

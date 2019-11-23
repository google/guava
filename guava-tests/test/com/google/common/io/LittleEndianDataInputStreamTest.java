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

import com.google.common.primitives.Bytes;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import junit.framework.TestCase;

/**
 * Test class for {@link LittleEndianDataInputStream}.
 *
 * @author Chris Nokleberg
 */
public class LittleEndianDataInputStreamTest extends TestCase {

  private byte[] data;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);

    initializeData(out);

    data = baos.toByteArray();
  }

  private void initializeData(DataOutputStream out) throws IOException {
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
    assertEquals(Bytes.asList(data), Bytes.asList(b));
  }

  public void testReadUnsignedByte_eof() throws IOException {
    DataInput in = new LittleEndianDataInputStream(new ByteArrayInputStream(new byte[0]));
    try {
      in.readUnsignedByte();
      fail();
    } catch (EOFException expected) {
    }
  }

  public void testReadUnsignedShort_eof() throws IOException {
    byte[] buf = {23};
    DataInput in = new LittleEndianDataInputStream(new ByteArrayInputStream(buf));
    try {
      in.readUnsignedShort();
      fail();
    } catch (EOFException expected) {
    }
  }

  public void testReadLine() throws IOException {
    DataInput in = new LittleEndianDataInputStream(new ByteArrayInputStream(data));
    try {
      in.readLine();
      fail();
    } catch (UnsupportedOperationException expected) {
      assertThat(expected).hasMessageThat().isEqualTo("readLine is not supported");
    }
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
    assertEquals("Herby Derby", in.readUTF());
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
}

/*
 * Copyright (C) 2010 The Guava Authors
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

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Test class for {@link LittleEndianDataOutputStream}.
 *
 * @author Keith Bottner
 */
public class LittleEndianDataOutputStreamTest extends TestCase {

  public void testWriteLittleEndian() throws IOException {

    /* Setup output streams. */
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(baos);

    /* Write out various test values in LITTLE ENDIAN FORMAT */
    out.write(new byte[] { -100, 100 });
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

    byte[] data = baos.toByteArray();

    /* Setup input streams */
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
    DataInput in = dis;

    /* Read in various values NORMALLY */
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
}

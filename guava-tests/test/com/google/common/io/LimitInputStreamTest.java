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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Charles Fry
 */
public class LimitInputStreamTest extends IoTestCase {

  public void testLimit() throws Exception {
    byte[] big = newPreFilledByteArray(5);
    InputStream bin = new ByteArrayInputStream(big);
    InputStream lin = new LimitInputStream(bin, 2);

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

  public void testMark() throws Exception {
    byte[] big = newPreFilledByteArray(5);
    InputStream bin = new ByteArrayInputStream(big);
    InputStream lin = new LimitInputStream(bin, 2);

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

  public void testSkip() throws Exception {
    byte[] big = newPreFilledByteArray(5);
    InputStream bin = new ByteArrayInputStream(big);
    InputStream lin = new LimitInputStream(bin, 2);

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
  
  
  public void testMarkNotSet() {
    byte[] big = newPreFilledByteArray(5);
    InputStream bin = new ByteArrayInputStream(big);
    InputStream lin = new LimitInputStream(bin, 2);

    try {
      lin.reset();
      fail();
    } catch (IOException expected) {
      assertEquals("Mark not set", expected.getMessage());
    }
  }
  
  public void testMarkNotSupported() {
    InputStream lin = new LimitInputStream(new UnmarkableInputStream(), 2);

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
}

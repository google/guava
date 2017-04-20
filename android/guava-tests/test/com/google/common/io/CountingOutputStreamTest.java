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

import java.io.ByteArrayOutputStream;

/**
 * Unit tests for {@link CountingOutputStream}.
 *
 * @author Chris Nokleberg
 */
public class CountingOutputStreamTest extends IoTestCase {

  public void testCount() throws Exception {
    int written = 0;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CountingOutputStream counter = new CountingOutputStream(out);
    assertEquals(written, out.size());
    assertEquals(written, counter.getCount());

    counter.write(0);
    written += 1;
    assertEquals(written, out.size());
    assertEquals(written, counter.getCount());

    byte[] data = new byte[10];
    counter.write(data);
    written += 10;
    assertEquals(written, out.size());
    assertEquals(written, counter.getCount());

    counter.write(data, 0, 5);
    written += 5;
    assertEquals(written, out.size());
    assertEquals(written, counter.getCount());

    counter.write(data, 2, 5);
    written += 5;
    assertEquals(written, out.size());
    assertEquals(written, counter.getCount());

    // Test that illegal arguments do not affect count
    try {
      counter.write(data, 0, data.length + 1);
      fail("expected exception");
    } catch (IndexOutOfBoundsException expected) {
    }
    assertEquals(written, out.size());
    assertEquals(written, counter.getCount());
  }
}

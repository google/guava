/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.io.TestOption.CLOSE_THROWS;
import static com.google.common.io.TestOption.OPEN_THROWS;
import static com.google.common.io.TestOption.READ_THROWS;
import static com.google.common.io.TestOption.WRITE_THROWS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;

/**
 * Tests for the default implementations of {@code ByteSink} methods.
 *
 * @author Colin Decker
 */
public class ByteSinkTest extends IoTestCase {

  private final byte[] bytes = newPreFilledByteArray(10000);

  private TestByteSink sink;

  @Override
  protected void setUp() throws Exception {
    sink = new TestByteSink();
  }

  public void testOpenBufferedStream() throws IOException {
    OutputStream out = sink.openBufferedStream();
    assertTrue(sink.wasStreamOpened());
    assertFalse(sink.wasStreamClosed());

    out.write(new byte[] {1, 2, 3, 4});
    out.close();

    assertTrue(sink.wasStreamClosed());
    assertArrayEquals(new byte[] {1, 2, 3, 4}, sink.getBytes());
  }

  public void testWrite_bytes() throws IOException {
    assertArrayEquals(new byte[0], sink.getBytes());
    sink.write(bytes);

    assertTrue(sink.wasStreamOpened() && sink.wasStreamClosed());
    assertArrayEquals(bytes, sink.getBytes());
  }

  public void testWriteFrom_inputStream() throws IOException {
    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    sink.writeFrom(in);

    assertTrue(sink.wasStreamOpened() && sink.wasStreamClosed());
    assertArrayEquals(bytes, sink.getBytes());
  }

  public void testWriteFromStream_doesNotCloseThatStream() throws IOException {
    TestInputStream in = new TestInputStream(new ByteArrayInputStream(new byte[10]));
    assertFalse(in.closed());
    sink.writeFrom(in);
    assertFalse(in.closed());
  }

  public void testClosesOnErrors_copyingFromByteSourceThatThrows() {
    for (TestOption option : EnumSet.of(OPEN_THROWS, READ_THROWS, CLOSE_THROWS)) {
      TestByteSource failSource = new TestByteSource(new byte[10], option);
      TestByteSink okSink = new TestByteSink();
      assertThrows(IOException.class, () -> failSource.copyTo(okSink));
      // ensure stream was closed IF it was opened (depends on implementation whether or not it's
      // opened at all if source.newInputStream() throws).
      assertTrue(
          "stream not closed when copying from source with option: " + option,
          !okSink.wasStreamOpened() || okSink.wasStreamClosed());
    }
  }

  public void testClosesOnErrors_whenWriteThrows() {
    TestByteSink failSink = new TestByteSink(WRITE_THROWS);
    assertThrows(IOException.class, () -> new TestByteSource(new byte[10]).copyTo(failSink));
    assertTrue(failSink.wasStreamClosed());
  }

  public void testClosesOnErrors_writingFromInputStreamThatThrows() throws IOException {
    TestByteSink okSink = new TestByteSink();
    TestInputStream in = new TestInputStream(new ByteArrayInputStream(new byte[10]), READ_THROWS);
    assertThrows(IOException.class, () -> okSink.writeFrom(in));
    assertTrue(okSink.wasStreamClosed());
  }
}

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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.io.TestOption.CLOSE_THROWS;
import static com.google.common.io.TestOption.OPEN_THROWS;
import static com.google.common.io.TestOption.READ_THROWS;
import static com.google.common.io.TestOption.SKIP_THROWS;
import static com.google.common.io.TestOption.WRITE_THROWS;
import static org.junit.Assert.assertArrayEquals;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;

/**
 * Tests for the default implementations of {@code ByteSource} methods.
 *
 * @author Colin Decker
 */
public class ByteSourceTest extends IoTestCase {

  private static final byte[] bytes = newPreFilledByteArray(10000);

  private TestByteSource source;

  @Override
  protected void setUp() throws Exception {
    source = new TestByteSource(bytes);
  }

  public void testOpenBufferedStream() throws IOException {
    BufferedInputStream in = source.openBufferedStream();
    assertTrue(source.wasStreamOpened());
    assertFalse(source.wasStreamClosed());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteStreams.copy(in, out);
    in.close();
    out.close();

    assertTrue(source.wasStreamClosed());
    assertArrayEquals(bytes, out.toByteArray());
  }

  public void testSize() throws IOException {
    assertEquals(bytes.length, source.size());
    assertTrue(source.wasStreamOpened() && source.wasStreamClosed());

    // test that we can get the size even if skip() isn't supported
    assertEquals(bytes.length, new TestByteSource(bytes, SKIP_THROWS).size());
  }

  public void testCopyTo_outputStream() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    assertEquals(bytes.length, source.copyTo(out));
    assertTrue(source.wasStreamOpened() && source.wasStreamClosed());

    assertArrayEquals(bytes, out.toByteArray());
  }

  public void testCopyTo_byteSink() throws IOException {
    TestByteSink sink = new TestByteSink();

    assertFalse(sink.wasStreamOpened() || sink.wasStreamClosed());

    assertEquals(bytes.length, source.copyTo(sink));
    assertTrue(source.wasStreamOpened() && source.wasStreamClosed());
    assertTrue(sink.wasStreamOpened() && sink.wasStreamClosed());

    assertArrayEquals(bytes, sink.getBytes());
  }

  public void testRead_toArray() throws IOException {
    assertArrayEquals(bytes, source.read());
    assertTrue(source.wasStreamOpened() && source.wasStreamClosed());
  }

  public void testHash() throws IOException {
    ByteSource byteSource = new TestByteSource("hamburger\n".getBytes(Charsets.US_ASCII));

    // Pasted this expected string from `echo hamburger | md5sum`
    assertEquals("cfa0c5002275c90508338a5cdb2a9781", byteSource.hash(Hashing.md5()).toString());
  }

  public void testContentEquals() throws IOException {
    assertTrue(source.contentEquals(source));
    assertTrue(source.wasStreamOpened() && source.wasStreamClosed());

    ByteSource equalSource = new TestByteSource(bytes);
    assertTrue(source.contentEquals(equalSource));
    assertTrue(new TestByteSource(bytes).contentEquals(source));

    ByteSource fewerBytes = new TestByteSource(newPreFilledByteArray(bytes.length / 2));
    assertFalse(source.contentEquals(fewerBytes));

    byte[] copy = bytes.clone();
    copy[9876] = 1;
    ByteSource oneByteOff = new TestByteSource(copy);
    assertFalse(source.contentEquals(oneByteOff));
  }

  public void testSlice() throws IOException {
    // Test preconditions
    try {
      source.slice(-1, 10);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    try {
      source.slice(0, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }

    assertCorrectSlice(0, 0, 0, 0);
    assertCorrectSlice(0, 0, 1, 0);
    assertCorrectSlice(100, 0, 10, 10);
    assertCorrectSlice(100, 0, 100, 100);
    assertCorrectSlice(100, 5, 10, 10);
    assertCorrectSlice(100, 5, 100, 95);
    assertCorrectSlice(100, 100, 0, 0);
    assertCorrectSlice(100, 100, 10, 0);

    try {
      assertCorrectSlice(100, 101, 10, 0);
      fail();
    } catch (EOFException expected) {
    }
  }

  /**
   * @param input      the size of the input source
   * @param offset     the first argument to {@link ByteStreams#slice}
   * @param length     the second argument to {@link ByteStreams#slice}
   * @param expectRead the number of bytes we expect to read
   */
  private static void assertCorrectSlice(
      int input, int offset, long length, int expectRead) throws IOException {
    checkArgument(expectRead == (int) Math.max(0, Math.min(input, offset + length) - offset));

    byte[] expected = newPreFilledByteArray(offset, expectRead);

    ByteSource source = new TestByteSource(newPreFilledByteArray(input));
    ByteSource slice = source.slice(offset, length);

    assertArrayEquals(expected, slice.read());
  }

  public void testCopyToStream_doesNotCloseThatStream() throws IOException {
    TestOutputStream out = new TestOutputStream(ByteStreams.nullOutputStream());
    assertFalse(out.closed());
    source.copyTo(out);
    assertFalse(out.closed());
  }

  public void testClosesOnErrors_copyingToByteSinkThatThrows() {
    for (TestOption option : EnumSet.of(OPEN_THROWS, WRITE_THROWS, CLOSE_THROWS)) {
      TestByteSource okSource = new TestByteSource(bytes);
      try {
        okSource.copyTo(new TestByteSink(option));
        fail();
      } catch (IOException expected) {
      }
      // ensure stream was closed IF it was opened (depends on implementation whether or not it's
      // opened at all if sink.newOutputStream() throws).
      assertTrue("stream not closed when copying to sink with option: " + option,
          !okSource.wasStreamOpened() || okSource.wasStreamClosed());
    }
  }

  public void testClosesOnErrors_whenReadThrows() {
    TestByteSource failSource = new TestByteSource(bytes, READ_THROWS);
    try {
      failSource.copyTo(new TestByteSink());
      fail();
    } catch (IOException expected) {
    }
    assertTrue(failSource.wasStreamClosed());
  }

  public void testClosesOnErrors_copyingToOutputStreamThatThrows() {
    TestByteSource okSource = new TestByteSource(bytes);
    try {
      OutputStream out = new TestOutputStream(ByteStreams.nullOutputStream(), WRITE_THROWS);
      okSource.copyTo(out);
      fail();
    } catch (IOException expected) {
    }
    assertTrue(okSource.wasStreamClosed());
  }
}

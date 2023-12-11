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
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.EnumSet;

/**
 * Tests for the default implementations of {@code CharSink} methods.
 *
 * @author Colin Decker
 */
public class CharSinkTest extends IoTestCase {

  private static final String STRING = ASCII + I18N;

  private TestCharSink sink;

  @Override
  public void setUp() {
    sink = new TestCharSink();
  }

  public void testOpenBufferedStream() throws IOException {
    Writer writer = sink.openBufferedStream();
    assertTrue(sink.wasStreamOpened());
    assertFalse(sink.wasStreamClosed());

    writer.write(STRING);
    writer.close();

    assertTrue(sink.wasStreamClosed());
    assertEquals(STRING, sink.getString());
  }

  public void testWrite_string() throws IOException {
    assertEquals("", sink.getString());
    sink.write(STRING);

    assertTrue(sink.wasStreamOpened() && sink.wasStreamClosed());
    assertEquals(STRING, sink.getString());
  }

  public void testWriteFrom_reader() throws IOException {
    StringReader reader = new StringReader(STRING);
    sink.writeFrom(reader);

    assertTrue(sink.wasStreamOpened() && sink.wasStreamClosed());
    assertEquals(STRING, sink.getString());
  }

  public void testWriteFromStream_doesNotCloseThatStream() throws IOException {
    TestReader in = new TestReader();
    assertFalse(in.closed());
    sink.writeFrom(in);
    assertFalse(in.closed());
  }

  public void testWriteLines_withSpecificSeparator() throws IOException {
    sink.writeLines(ImmutableList.of("foo", "bar", "baz"), "\n");
    assertEquals("foo\nbar\nbaz\n", sink.getString());
  }

  public void testWriteLines_withDefaultSeparator() throws IOException {
    sink.writeLines(ImmutableList.of("foo", "bar", "baz"));
    String separator = System.getProperty("line.separator");
    assertEquals("foo" + separator + "bar" + separator + "baz" + separator, sink.getString());
  }

  public void testClosesOnErrors_copyingFromCharSourceThatThrows() {
    for (TestOption option : EnumSet.of(OPEN_THROWS, READ_THROWS, CLOSE_THROWS)) {
      TestCharSource failSource = new TestCharSource(STRING, option);
      TestCharSink okSink = new TestCharSink();
      assertThrows(IOException.class, () -> failSource.copyTo(okSink));
      // ensure writer was closed IF it was opened (depends on implementation whether or not it's
      // opened at all if source.newReader() throws).
      assertTrue(
          "stream not closed when copying from source with option: " + option,
          !okSink.wasStreamOpened() || okSink.wasStreamClosed());
    }
  }

  public void testClosesOnErrors_whenWriteThrows() {
    TestCharSink failSink = new TestCharSink(WRITE_THROWS);
    assertThrows(IOException.class, () -> new TestCharSource(STRING).copyTo(failSink));
    assertTrue(failSink.wasStreamClosed());
  }

  public void testClosesOnErrors_whenWritingFromReaderThatThrows() {
    TestCharSink okSink = new TestCharSink();
    assertThrows(IOException.class, () -> okSink.writeFrom(new TestReader(READ_THROWS)));
    assertTrue(okSink.wasStreamClosed());
  }
}

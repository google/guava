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

import com.google.common.collect.ImmutableList;

import junit.framework.TestSuite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.EnumSet;

/**
 * Tests for the default implementations of {@code CharSource} methods.
 *
 * @author Colin Decker
 */
public class CharSourceTest extends IoTestCase {

  public static TestSuite suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(CharSourceTester.tests("CharSource.wrap[CharSequence]",
        SourceSinkFactories.stringCharSourceFactory()));
    suite.addTest(CharSourceTester.tests("CharSource.empty[]",
        SourceSinkFactories.emptyCharSourceFactory()));
    suite.addTestSuite(CharStreamsTest.class);
    return suite;
  }

  private static final String STRING = ASCII + I18N;
  private static final String LINES = "foo\nbar\r\nbaz\rsomething";

  private TestCharSource source;

  @Override
  public void setUp() {
    source = new TestCharSource(STRING);
  }

  public void testOpenBufferedStream() throws IOException {
    BufferedReader reader = source.openBufferedStream();
    assertTrue(source.wasStreamOpened());
    assertFalse(source.wasStreamClosed());

    StringWriter writer = new StringWriter();
    char[] buf = new char[64];
    int read;
    while ((read = reader.read(buf)) != -1) {
      writer.write(buf, 0, read);
    }
    reader.close();
    writer.close();

    assertTrue(source.wasStreamClosed());
    assertEquals(STRING, writer.toString());
  }

  public void testCopyTo_appendable() throws IOException {
    StringBuilder builder = new StringBuilder();

    assertEquals(STRING.length(), source.copyTo(builder));
    assertTrue(source.wasStreamOpened() && source.wasStreamClosed());

    assertEquals(STRING, builder.toString());
  }

  public void testCopyTo_charSink() throws IOException {
    TestCharSink sink = new TestCharSink();

    assertFalse(sink.wasStreamOpened() || sink.wasStreamClosed());

    assertEquals(STRING.length(), source.copyTo(sink));
    assertTrue(source.wasStreamOpened() && source.wasStreamClosed());
    assertTrue(sink.wasStreamOpened() && sink.wasStreamClosed());

    assertEquals(STRING, sink.getString());
  }

  public void testRead_toString() throws IOException {
    assertEquals(STRING, source.read());
    assertTrue(source.wasStreamOpened() && source.wasStreamClosed());
  }

  public void testReadFirstLine() throws IOException {
    TestCharSource lines = new TestCharSource(LINES);
    assertEquals("foo", lines.readFirstLine());
    assertTrue(lines.wasStreamOpened() && lines.wasStreamClosed());
  }

  public void testReadLines_toList() throws IOException {
    TestCharSource lines = new TestCharSource(LINES);
    assertEquals(ImmutableList.of("foo", "bar", "baz", "something"), lines.readLines());
    assertTrue(lines.wasStreamOpened() && lines.wasStreamClosed());
  }

  public void testCopyToAppendable_doesNotCloseIfWriter() throws IOException {
    TestWriter writer = new TestWriter();
    assertFalse(writer.closed());
    source.copyTo(writer);
    assertFalse(writer.closed());
  }

  public void testClosesOnErrors_copyingToCharSinkThatThrows() {
    for (TestOption option : EnumSet.of(OPEN_THROWS, WRITE_THROWS, CLOSE_THROWS)) {
      TestCharSource okSource = new TestCharSource(STRING);
      try {
        okSource.copyTo(new TestCharSink(option));
        fail();
      } catch (IOException expected) {
      }
      // ensure reader was closed IF it was opened (depends on implementation whether or not it's
      // opened at all if sink.newWriter() throws).
      assertTrue("stream not closed when copying to sink with option: " + option,
          !okSource.wasStreamOpened() || okSource.wasStreamClosed());
    }
  }

  public void testClosesOnErrors_whenReadThrows() {
    TestCharSource failSource = new TestCharSource(STRING, READ_THROWS);
    try {
      failSource.copyTo(new TestCharSink());
      fail();
    } catch (IOException expected) {
    }
    assertTrue(failSource.wasStreamClosed());
  }

  public void testClosesOnErrors_copyingToWriterThatThrows() {
    TestCharSource okSource = new TestCharSource(STRING);
    try {
      okSource.copyTo(new TestWriter(WRITE_THROWS));
      fail();
    } catch (IOException expected) {
    }
    assertTrue(okSource.wasStreamClosed());
  }

  public void testConcat() throws IOException {
    CharSource c1 = CharSource.wrap("abc");
    CharSource c2 = CharSource.wrap("");
    CharSource c3 = CharSource.wrap("de");

    String expected = "abcde";

    assertEquals(expected,
        CharSource.concat(ImmutableList.of(c1, c2, c3)).read());
    assertEquals(expected,
        CharSource.concat(c1, c2, c3).read());
    assertEquals(expected,
        CharSource.concat(ImmutableList.of(c1, c2, c3).iterator()).read());
  }
}

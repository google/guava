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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.testing.TestLogHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.EnumSet;
import java.util.List;
import junit.framework.TestSuite;

/**
 * Tests for the default implementations of {@code CharSource} methods.
 *
 * @author Colin Decker
 */
public class CharSourceTest extends IoTestCase {

  @AndroidIncompatible // Android doesn't understand suites whose tests lack default constructors.
  public static TestSuite suite() {
    TestSuite suite = new TestSuite();
    for (boolean asByteSource : new boolean[] {false, true}) {
      suite.addTest(
          CharSourceTester.tests(
              "CharSource.wrap[CharSequence]",
              SourceSinkFactories.stringCharSourceFactory(),
              asByteSource));
      suite.addTest(
          CharSourceTester.tests(
              "CharSource.empty[]", SourceSinkFactories.emptyCharSourceFactory(), asByteSource));
    }
    suite.addTestSuite(CharSourceTest.class);
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

  public void testReadLines_withProcessor() throws IOException {
    TestCharSource lines = new TestCharSource(LINES);
    List<String> list =
        lines.readLines(
            new LineProcessor<List<String>>() {
              List<String> list = Lists.newArrayList();

              @Override
              public boolean processLine(String line) throws IOException {
                list.add(line);
                return true;
              }

              @Override
              public List<String> getResult() {
                return list;
              }
            });
    assertEquals(ImmutableList.of("foo", "bar", "baz", "something"), list);
    assertTrue(lines.wasStreamOpened() && lines.wasStreamClosed());
  }

  public void testReadLines_withProcessor_stopsOnFalse() throws IOException {
    TestCharSource lines = new TestCharSource(LINES);
    List<String> list =
        lines.readLines(
            new LineProcessor<List<String>>() {
              List<String> list = Lists.newArrayList();

              @Override
              public boolean processLine(String line) throws IOException {
                list.add(line);
                return false;
              }

              @Override
              public List<String> getResult() {
                return list;
              }
            });
    assertEquals(ImmutableList.of("foo"), list);
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
      assertTrue(
          "stream not closed when copying to sink with option: " + option,
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

    assertEquals(expected, CharSource.concat(ImmutableList.of(c1, c2, c3)).read());
    assertEquals(expected, CharSource.concat(c1, c2, c3).read());
    assertEquals(expected, CharSource.concat(ImmutableList.of(c1, c2, c3).iterator()).read());
    assertFalse(CharSource.concat(c1, c2, c3).isEmpty());

    CharSource emptyConcat = CharSource.concat(CharSource.empty(), CharSource.empty());
    assertTrue(emptyConcat.isEmpty());
  }

  public void testConcat_infiniteIterable() throws IOException {
    CharSource source = CharSource.wrap("abcd");
    Iterable<CharSource> cycle = Iterables.cycle(ImmutableList.of(source));
    CharSource concatenated = CharSource.concat(cycle);

    String expected = "abcdabcd";

    // read the first 8 chars manually, since there's no equivalent to ByteSource.slice
    // TODO(cgdecker): Add CharSource.slice?
    StringBuilder builder = new StringBuilder();
    Reader reader = concatenated.openStream(); // no need to worry about closing
    for (int i = 0; i < 8; i++) {
      builder.append((char) reader.read());
    }
    assertEquals(expected, builder.toString());
  }

  static final CharSource BROKEN_READ_SOURCE = new TestCharSource("ABC", READ_THROWS);
  static final CharSource BROKEN_CLOSE_SOURCE = new TestCharSource("ABC", CLOSE_THROWS);
  static final CharSource BROKEN_OPEN_SOURCE = new TestCharSource("ABC", OPEN_THROWS);
  static final CharSink BROKEN_WRITE_SINK = new TestCharSink(WRITE_THROWS);
  static final CharSink BROKEN_CLOSE_SINK = new TestCharSink(CLOSE_THROWS);
  static final CharSink BROKEN_OPEN_SINK = new TestCharSink(OPEN_THROWS);

  private static final ImmutableSet<CharSource> BROKEN_SOURCES =
      ImmutableSet.of(BROKEN_CLOSE_SOURCE, BROKEN_OPEN_SOURCE, BROKEN_READ_SOURCE);
  private static final ImmutableSet<CharSink> BROKEN_SINKS =
      ImmutableSet.of(BROKEN_CLOSE_SINK, BROKEN_OPEN_SINK, BROKEN_WRITE_SINK);

  public void testCopyExceptions() {
    if (!Closer.SuppressingSuppressor.isAvailable()) {
      // test that exceptions are logged

      TestLogHandler logHandler = new TestLogHandler();
      Closeables.logger.addHandler(logHandler);
      try {
        for (CharSource in : BROKEN_SOURCES) {
          runFailureTest(in, newNormalCharSink());
          assertTrue(logHandler.getStoredLogRecords().isEmpty());

          runFailureTest(in, BROKEN_CLOSE_SINK);
          assertEquals((in == BROKEN_OPEN_SOURCE) ? 0 : 1, getAndResetRecords(logHandler));
        }

        for (CharSink out : BROKEN_SINKS) {
          runFailureTest(newNormalCharSource(), out);
          assertTrue(logHandler.getStoredLogRecords().isEmpty());

          runFailureTest(BROKEN_CLOSE_SOURCE, out);
          assertEquals(1, getAndResetRecords(logHandler));
        }

        for (CharSource in : BROKEN_SOURCES) {
          for (CharSink out : BROKEN_SINKS) {
            runFailureTest(in, out);
            assertTrue(getAndResetRecords(logHandler) <= 1);
          }
        }
      } finally {
        Closeables.logger.removeHandler(logHandler);
      }
    } else {
      // test that exceptions are suppressed

      for (CharSource in : BROKEN_SOURCES) {
        int suppressed = runSuppressionFailureTest(in, newNormalCharSink());
        assertEquals(0, suppressed);

        suppressed = runSuppressionFailureTest(in, BROKEN_CLOSE_SINK);
        assertEquals((in == BROKEN_OPEN_SOURCE) ? 0 : 1, suppressed);
      }

      for (CharSink out : BROKEN_SINKS) {
        int suppressed = runSuppressionFailureTest(newNormalCharSource(), out);
        assertEquals(0, suppressed);

        suppressed = runSuppressionFailureTest(BROKEN_CLOSE_SOURCE, out);
        assertEquals(1, suppressed);
      }

      for (CharSource in : BROKEN_SOURCES) {
        for (CharSink out : BROKEN_SINKS) {
          int suppressed = runSuppressionFailureTest(in, out);
          assertTrue(suppressed <= 1);
        }
      }
    }
  }

  private static int getAndResetRecords(TestLogHandler logHandler) {
    int records = logHandler.getStoredLogRecords().size();
    logHandler.clear();
    return records;
  }

  private static void runFailureTest(CharSource in, CharSink out) {
    try {
      in.copyTo(out);
      fail();
    } catch (IOException expected) {
    }
  }

  /** @return the number of exceptions that were suppressed on the expected thrown exception */
  private static int runSuppressionFailureTest(CharSource in, CharSink out) {
    try {
      in.copyTo(out);
      fail();
    } catch (IOException expected) {
      return CloserTest.getSuppressed(expected).length;
    }
    throw new AssertionError(); // can't happen
  }

  private static CharSource newNormalCharSource() {
    return CharSource.wrap("ABC");
  }

  private static CharSink newNormalCharSink() {
    return new CharSink() {
      @Override
      public Writer openStream() {
        return new StringWriter();
      }
    };
  }
}

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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.SourceSinkFactory.ByteSourceFactory;
import com.google.common.io.SourceSinkFactory.CharSourceFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;
import junit.framework.TestSuite;

/**
 * A generator of {@code TestSuite} instances for testing {@code CharSource} implementations.
 * Generates tests of a all methods on a {@code CharSource} given various inputs the source is
 * expected to contain.
 *
 * @author Colin Decker
 */
@AndroidIncompatible // Android doesn't understand tests that lack default constructors.
public class CharSourceTester extends SourceSinkTester<CharSource, String, CharSourceFactory> {

  private static final ImmutableList<Method> testMethods = getTestMethods(CharSourceTester.class);

  static TestSuite tests(String name, CharSourceFactory factory, boolean testAsByteSource) {
    TestSuite suite = new TestSuite(name);
    for (Entry<String, String> entry : TEST_STRINGS.entrySet()) {
      if (testAsByteSource) {
        suite.addTest(
            suiteForBytes(
                factory, entry.getValue().getBytes(Charsets.UTF_8), name, entry.getKey(), true));
      } else {
        suite.addTest(suiteForString(factory, entry.getValue(), name, entry.getKey()));
      }
    }
    return suite;
  }

  static TestSuite suiteForBytes(
      CharSourceFactory factory, byte[] bytes, String name, String desc, boolean slice) {
    TestSuite suite = suiteForString(factory, new String(bytes, Charsets.UTF_8), name, desc);
    ByteSourceFactory byteSourceFactory = SourceSinkFactories.asByteSourceFactory(factory);
    suite.addTest(
        ByteSourceTester.suiteForBytes(
            byteSourceFactory, bytes, name + ".asByteSource[Charset]", desc, slice));
    return suite;
  }

  static TestSuite suiteForString(
      CharSourceFactory factory, String string, String name, String desc) {
    TestSuite suite = new TestSuite(name + " [" + desc + "]");
    for (Method method : testMethods) {
      suite.addTest(new CharSourceTester(factory, string, name, desc, method));
    }
    return suite;
  }

  private final ImmutableList<String> expectedLines;

  private CharSource source;

  public CharSourceTester(
      CharSourceFactory factory, String string, String suiteName, String caseDesc, Method method) {
    super(factory, string, suiteName, caseDesc, method);
    this.expectedLines = getLines(expected);
  }

  @Override
  protected void setUp() throws Exception {
    this.source = factory.createSource(data);
  }

  public void testOpenStream() throws IOException {
    Reader reader = source.openStream();

    StringWriter writer = new StringWriter();
    char[] buf = new char[64];
    int read;
    while ((read = reader.read(buf)) != -1) {
      writer.write(buf, 0, read);
    }
    reader.close();
    writer.close();

    assertExpectedString(writer.toString());
  }

  public void testOpenBufferedStream() throws IOException {
    BufferedReader reader = source.openBufferedStream();

    StringWriter writer = new StringWriter();
    char[] buf = new char[64];
    int read;
    while ((read = reader.read(buf)) != -1) {
      writer.write(buf, 0, read);
    }
    reader.close();
    writer.close();

    assertExpectedString(writer.toString());
  }

  public void testLines() throws IOException {
    try (Stream<String> lines = source.lines()) {
      assertExpectedLines(lines.collect(toImmutableList()));
    }
  }

  public void testCopyTo_appendable() throws IOException {
    StringBuilder builder = new StringBuilder();

    assertEquals(expected.length(), source.copyTo(builder));

    assertExpectedString(builder.toString());
  }

  public void testCopyTo_charSink() throws IOException {
    TestCharSink sink = new TestCharSink();

    assertEquals(expected.length(), source.copyTo(sink));

    assertExpectedString(sink.getString());
  }

  public void testRead_toString() throws IOException {
    String string = source.read();
    assertExpectedString(string);
  }

  public void testReadFirstLine() throws IOException {
    if (expectedLines.isEmpty()) {
      assertNull(source.readFirstLine());
    } else {
      assertEquals(expectedLines.get(0), source.readFirstLine());
    }
  }

  public void testReadLines_toList() throws IOException {
    assertExpectedLines(source.readLines());
  }

  public void testIsEmpty() throws IOException {
    assertEquals(expected.isEmpty(), source.isEmpty());
  }

  public void testLength() throws IOException {
    assertEquals(expected.length(), source.length());
  }

  public void testLengthIfKnown() throws IOException {
    Optional<Long> lengthIfKnown = source.lengthIfKnown();
    if (lengthIfKnown.isPresent()) {
      assertEquals(expected.length(), (long) lengthIfKnown.get());
    }
  }

  public void testReadLines_withProcessor() throws IOException {
    List<String> list =
        source.readLines(
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

    assertExpectedLines(list);
  }

  public void testReadLines_withProcessor_stopsOnFalse() throws IOException {
    List<String> list =
        source.readLines(
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

    if (expectedLines.isEmpty()) {
      assertTrue(list.isEmpty());
    } else {
      assertEquals(expectedLines.subList(0, 1), list);
    }
  }

  public void testForEachLine() throws IOException {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    source.forEachLine(builder::add);
    assertExpectedLines(builder.build());
  }

  private void assertExpectedString(String string) {
    assertEquals(expected, string);
  }

  private void assertExpectedLines(List<String> list) {
    assertEquals(expectedLines, list);
  }
}

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

import static com.google.common.io.SourceSinkFactory.ByteSourceFactory;
import static com.google.common.io.SourceSinkFactory.CharSourceFactory;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Random;
import junit.framework.TestSuite;

/**
 * A generator of {@code TestSuite} instances for testing {@code ByteSource} implementations.
 * Generates tests of all methods on a {@code ByteSource} given various inputs the source is
 * expected to contain as well as sub-suites for testing the {@code CharSource} view and {@code
 * slice()} views in the same way.
 *
 * @author Colin Decker
 */
@AndroidIncompatible // TODO(b/230620681): Make this available (even though we won't run it).
public class ByteSourceTester extends SourceSinkTester<ByteSource, byte[], ByteSourceFactory> {

  private static final ImmutableList<Method> testMethods = getTestMethods(ByteSourceTester.class);

  static TestSuite tests(String name, ByteSourceFactory factory, boolean testAsCharSource) {
    TestSuite suite = new TestSuite(name);
    for (Entry<String, String> entry : TEST_STRINGS.entrySet()) {
      if (testAsCharSource) {
        suite.addTest(suiteForString(factory, entry.getValue(), name, entry.getKey()));
      } else {
        suite.addTest(
            suiteForBytes(
                factory, entry.getValue().getBytes(Charsets.UTF_8), name, entry.getKey(), true));
      }
    }
    return suite;
  }

  static TestSuite suiteForString(
      ByteSourceFactory factory, String string, String name, String desc) {
    TestSuite suite = suiteForBytes(factory, string.getBytes(Charsets.UTF_8), name, desc, true);
    CharSourceFactory charSourceFactory = SourceSinkFactories.asCharSourceFactory(factory);
    suite.addTest(
        CharSourceTester.suiteForString(
            charSourceFactory, string, name + ".asCharSource[Charset]", desc));
    return suite;
  }

  static TestSuite suiteForBytes(
      ByteSourceFactory factory, byte[] bytes, String name, String desc, boolean slice) {
    TestSuite suite = new TestSuite(name + " [" + desc + "]");
    for (Method method : testMethods) {
      suite.addTest(new ByteSourceTester(factory, bytes, name, desc, method));
    }

    if (slice && bytes.length > 0) {
      // test a random slice() of the ByteSource
      Random random = new Random();
      byte[] expected = factory.getExpected(bytes);
      // if expected.length == 0, off has to be 0 but length doesn't matter--result will be empty
      int off = expected.length == 0 ? 0 : random.nextInt(expected.length);
      int len = expected.length == 0 ? 4 : random.nextInt(expected.length - off);

      ByteSourceFactory sliced = SourceSinkFactories.asSlicedByteSourceFactory(factory, off, len);
      suite.addTest(suiteForBytes(sliced, bytes, name + ".slice[long, long]", desc, false));

      // test a slice() of the ByteSource starting at a random offset with a length of
      // Long.MAX_VALUE
      ByteSourceFactory slicedLongMaxValue =
          SourceSinkFactories.asSlicedByteSourceFactory(factory, off, Long.MAX_VALUE);
      suite.addTest(
          suiteForBytes(
              slicedLongMaxValue, bytes, name + ".slice[long, Long.MAX_VALUE]", desc, false));

      // test a slice() of the ByteSource starting at an offset greater than its size
      ByteSourceFactory slicedOffsetPastEnd =
          SourceSinkFactories.asSlicedByteSourceFactory(
              factory, expected.length + 2, expected.length + 10);
      suite.addTest(
          suiteForBytes(slicedOffsetPastEnd, bytes, name + ".slice[size + 2, long]", desc, false));
    }

    return suite;
  }

  private ByteSource source;

  public ByteSourceTester(
      ByteSourceFactory factory, byte[] bytes, String suiteName, String caseDesc, Method method) {
    super(factory, bytes, suiteName, caseDesc, method);
  }

  @Override
  public void setUp() throws IOException {
    source = factory.createSource(data);
  }

  public void testOpenStream() throws IOException {
    InputStream in = source.openStream();
    try {
      byte[] readBytes = ByteStreams.toByteArray(in);
      assertExpectedBytes(readBytes);
    } finally {
      in.close();
    }
  }

  public void testOpenBufferedStream() throws IOException {
    InputStream in = source.openBufferedStream();
    try {
      byte[] readBytes = ByteStreams.toByteArray(in);
      assertExpectedBytes(readBytes);
    } finally {
      in.close();
    }
  }

  public void testRead() throws IOException {
    byte[] readBytes = source.read();
    assertExpectedBytes(readBytes);
  }

  public void testCopyTo_outputStream() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    source.copyTo(out);
    assertExpectedBytes(out.toByteArray());
  }

  public void testCopyTo_byteSink() throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    // HERESY! but it's ok just for this I guess
    source.copyTo(
        new ByteSink() {
          @Override
          public OutputStream openStream() throws IOException {
            return out;
          }
        });
    assertExpectedBytes(out.toByteArray());
  }

  public void testIsEmpty() throws IOException {
    assertEquals(expected.length == 0, source.isEmpty());
  }

  public void testSize() throws IOException {
    assertEquals(expected.length, source.size());
  }

  public void testSizeIfKnown() throws IOException {
    Optional<Long> sizeIfKnown = source.sizeIfKnown();
    if (sizeIfKnown.isPresent()) {
      assertEquals(expected.length, (long) sizeIfKnown.get());
    }
  }

  public void testContentEquals() throws IOException {
    assertTrue(
        source.contentEquals(
            new ByteSource() {
              @Override
              public InputStream openStream() throws IOException {
                return new RandomAmountInputStream(
                    new ByteArrayInputStream(expected), new Random());
              }
            }));
  }

  public void testRead_usingByteProcessor() throws IOException {
    byte[] readBytes =
        source.read(
            new ByteProcessor<byte[]>() {
              final ByteArrayOutputStream out = new ByteArrayOutputStream();

              @Override
              public boolean processBytes(byte[] buf, int off, int len) throws IOException {
                out.write(buf, off, len);
                return true;
              }

              @Override
              public byte[] getResult() {
                return out.toByteArray();
              }
            });

    assertExpectedBytes(readBytes);
  }

  public void testHash() throws IOException {
    HashCode expectedHash = Hashing.md5().hashBytes(expected);
    assertEquals(expectedHash, source.hash(Hashing.md5()));
  }

  public void testSlice_illegalArguments() {
    assertThrows(
        "expected IllegalArgumentException for call to slice with offset -1: " + source,
        IllegalArgumentException.class,
        () -> source.slice(-1, 0));

    assertThrows(
        "expected IllegalArgumentException for call to slice with length -1: " + source,
        IllegalArgumentException.class,
        () -> source.slice(0, -1));
  }

  // Test that you can not expand the readable data in a previously sliced ByteSource.
  public void testSlice_constrainedRange() throws IOException {
    long size = source.read().length;
    if (size >= 2) {
      ByteSource sliced = source.slice(1, size - 2);
      assertEquals(size - 2, sliced.read().length);
      ByteSource resliced = sliced.slice(0, size - 1);
      assertTrue(sliced.contentEquals(resliced));
    }
  }

  private void assertExpectedBytes(byte[] readBytes) {
    assertArrayEquals(expected, readBytes);
  }
}

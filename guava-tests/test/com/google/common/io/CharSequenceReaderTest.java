/*
 * Copyright (C) 2013 The Guava Authors
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

import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.CharBuffer;
import junit.framework.TestCase;

/**
 * Tests for {@link CharSequenceReader}.
 *
 * @author Colin Decker
 */
public class CharSequenceReaderTest extends TestCase {

  public void testReadEmptyString() throws IOException {
    assertReadsCorrectly("");
  }

  public void testReadsStringsCorrectly() throws IOException {
    assertReadsCorrectly("abc");
    assertReadsCorrectly("abcde");
    assertReadsCorrectly("abcdefghijkl");
    assertReadsCorrectly(
        ""
            + "abcdefghijklmnopqrstuvwxyz\n"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\r"
            + "0123456789\r\n"
            + "!@#$%^&*()-=_+\t[]{};':\",./<>?\\| ");
  }

  public void testMarkAndReset() throws IOException {
    String string = "abcdefghijklmnopqrstuvwxyz";
    CharSequenceReader reader = new CharSequenceReader(string);
    assertTrue(reader.markSupported());

    assertEquals(string, readFully(reader));
    assertFullyRead(reader);

    // reset and read again
    reader.reset();
    assertEquals(string, readFully(reader));
    assertFullyRead(reader);

    // reset, skip, mark, then read the rest
    reader.reset();
    assertEquals(5, reader.skip(5));
    reader.mark(Integer.MAX_VALUE);
    assertEquals(string.substring(5), readFully(reader));
    assertFullyRead(reader);

    // reset to the mark and then read the rest
    reader.reset();
    assertEquals(string.substring(5), readFully(reader));
    assertFullyRead(reader);
  }

  public void testIllegalArguments() throws IOException {
    CharSequenceReader reader = new CharSequenceReader("12345");

    char[] buf = new char[10];
    assertThrows(IndexOutOfBoundsException.class, () -> reader.read(buf, 0, 11));

    assertThrows(IndexOutOfBoundsException.class, () -> reader.read(buf, 10, 1));

    assertThrows(IndexOutOfBoundsException.class, () -> reader.read(buf, 11, 0));

    assertThrows(IndexOutOfBoundsException.class, () -> reader.read(buf, -1, 5));

    assertThrows(IndexOutOfBoundsException.class, () -> reader.read(buf, 5, -1));

    assertThrows(IndexOutOfBoundsException.class, () -> reader.read(buf, 0, 11));

    assertThrows(IllegalArgumentException.class, () -> reader.skip(-1));

    assertThrows(IllegalArgumentException.class, () -> reader.mark(-1));
  }

  public void testMethodsThrowWhenClosed() throws IOException {
    CharSequenceReader reader = new CharSequenceReader("");
    reader.close();

    assertThrows(IOException.class, () -> reader.read());

    assertThrows(IOException.class, () -> reader.read(new char[10]));

    assertThrows(IOException.class, () -> reader.read(new char[10], 0, 10));

    assertThrows(IOException.class, () -> reader.read(CharBuffer.allocate(10)));

    assertThrows(IOException.class, () -> reader.skip(10));

    assertThrows(IOException.class, () -> reader.ready());

    assertThrows(IOException.class, () -> reader.mark(10));

    assertThrows(IOException.class, () -> reader.reset());
  }

  /**
   * Creates a CharSequenceReader wrapping the given CharSequence and tests that the reader produces
   * the same sequence when read using each type of read method it provides.
   */
  private static void assertReadsCorrectly(CharSequence charSequence) throws IOException {
    String expected = charSequence.toString();

    // read char by char
    CharSequenceReader reader = new CharSequenceReader(charSequence);
    for (int i = 0; i < expected.length(); i++) {
      assertEquals(expected.charAt(i), reader.read());
    }
    assertFullyRead(reader);

    // read all to one array
    reader = new CharSequenceReader(charSequence);
    char[] buf = new char[expected.length()];
    assertEquals(expected.length() == 0 ? -1 : expected.length(), reader.read(buf));
    assertEquals(expected, new String(buf));
    assertFullyRead(reader);

    // read in chunks to fixed array
    reader = new CharSequenceReader(charSequence);
    buf = new char[5];
    StringBuilder builder = new StringBuilder();
    int read;
    while ((read = reader.read(buf, 0, buf.length)) != -1) {
      builder.append(buf, 0, read);
    }
    assertEquals(expected, builder.toString());
    assertFullyRead(reader);

    // read all to one CharBuffer
    reader = new CharSequenceReader(charSequence);
    CharBuffer buf2 = CharBuffer.allocate(expected.length());
    assertEquals(expected.length() == 0 ? -1 : expected.length(), reader.read(buf2));
    Java8Compatibility.flip(buf2);
    assertEquals(expected, buf2.toString());
    assertFullyRead(reader);

    // read in chunks to fixed CharBuffer
    reader = new CharSequenceReader(charSequence);
    buf2 = CharBuffer.allocate(5);
    builder = new StringBuilder();
    while (reader.read(buf2) != -1) {
      Java8Compatibility.flip(buf2);
      builder.append(buf2);
      Java8Compatibility.clear(buf2);
    }
    assertEquals(expected, builder.toString());
    assertFullyRead(reader);

    // skip fully
    reader = new CharSequenceReader(charSequence);
    assertEquals(expected.length(), reader.skip(Long.MAX_VALUE));
    assertFullyRead(reader);

    // skip 5 and read the rest
    if (expected.length() > 5) {
      reader = new CharSequenceReader(charSequence);
      assertEquals(5, reader.skip(5));

      buf = new char[expected.length() - 5];
      assertEquals(buf.length, reader.read(buf, 0, buf.length));
      assertEquals(expected.substring(5), new String(buf));
      assertFullyRead(reader);
    }
  }

  private static void assertFullyRead(CharSequenceReader reader) throws IOException {
    assertEquals(-1, reader.read());
    assertEquals(-1, reader.read(new char[10], 0, 10));
    assertEquals(-1, reader.read(CharBuffer.allocate(10)));
    assertEquals(0, reader.skip(10));
  }

  private static String readFully(CharSequenceReader reader) throws IOException {
    StringBuilder builder = new StringBuilder();
    int read;
    while ((read = reader.read()) != -1) {
      builder.append((char) read);
    }
    return builder.toString();
  }
}

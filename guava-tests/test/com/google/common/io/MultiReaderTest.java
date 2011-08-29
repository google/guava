/*
 * Copyright (C) 2008 The Guava Authors
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

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * @author ricebin
 */
public class MultiReaderTest extends IoTestCase {

  public void testOnlyOneOpen() throws Exception {
    String testString = "abcdefgh";
    final InputSupplier<Reader> supplier = newReader(testString);
    final int[] counter = new int[1];
    InputSupplier<Reader> reader = new InputSupplier<Reader>() {
      @Override
      public Reader getInput() throws IOException {
        if (counter[0]++ != 0) {
          throw new IllegalStateException("More than one supplier open");
        }
        return new FilterReader(supplier.getInput()) {
          @Override public void close() throws IOException {
            super.close();
            counter[0]--;
          }
        };
      }
    };
    @SuppressWarnings("unchecked")
    Reader joinedReader = CharStreams.join(reader, reader, reader).getInput();
    String result = CharStreams.toString(joinedReader);
    assertEquals(testString.length() * 3, result.length());
  }

  public void testSimple() throws Exception {
    String testString = "abcdefgh";
    InputSupplier<Reader> supplier = newReader(testString);
    @SuppressWarnings("unchecked")
    Reader joinedReader = CharStreams.join(supplier, supplier).getInput();

    String expectedString = testString + testString;
    assertEquals(expectedString, CharStreams.toString(joinedReader));
  }

  private static InputSupplier<Reader> newReader(final String text) {
    return new InputSupplier<Reader>() {
      @Override
      public Reader getInput() {
        return new StringReader(text);
      }
    };
  }

  public void testSkip() throws Exception {
    String begin = "abcde";
    String end = "fghij";
    @SuppressWarnings("unchecked")
    Reader joinedReader =
        CharStreams.join(newReader(begin), newReader(end)).getInput();

    String expected = begin + end;
    assertEquals(expected.charAt(0), joinedReader.read());
    CharStreams.skipFully(joinedReader, 1);
    assertEquals(expected.charAt(2), joinedReader.read());
    CharStreams.skipFully(joinedReader, 4);
    assertEquals(expected.charAt(7), joinedReader.read());
    CharStreams.skipFully(joinedReader, 1);
    assertEquals(expected.charAt(9), joinedReader.read());
    assertEquals(-1, joinedReader.read());
  }

}

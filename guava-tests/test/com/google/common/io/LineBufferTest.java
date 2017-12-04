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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link LineBuffer} and {@link LineReader}.
 *
 * @author Chris Nokleberg
 */
@AndroidIncompatible // occasionally very slow
public class LineBufferTest extends IoTestCase {

  public void testProcess() throws IOException {
    bufferHelper("");
    bufferHelper("\n", "\n");
    bufferHelper("\r\n", "\r\n");
    bufferHelper("\n\r", "\n", "\r");
    bufferHelper("\r", "\r");
    bufferHelper("\n\n", "\n", "\n");
    bufferHelper("\r\n\r\n", "\r\n", "\r\n");
    bufferHelper("\r\r", "\r", "\r");
    bufferHelper("\ra\r\n\n\r\r", "\r", "a\r\n", "\n", "\r", "\r");
    bufferHelper("no newlines at all", "no newlines at all");
    bufferHelper("two lines\nbut no newline at end", "two lines\n", "but no newline at end");
    bufferHelper(
        "\nempty first line\nno newline at end", "\n", "empty first line\n", "no newline at end");
    bufferHelper("three\rlines\rno newline at end", "three\r", "lines\r", "no newline at end");
    bufferHelper("mixed\nline\rendings\r\n", "mixed\n", "line\r", "endings\r\n");
  }

  private static final int[] CHUNK_SIZES = {1, 2, 3, Integer.MAX_VALUE};

  private static void bufferHelper(String input, String... expect) throws IOException {

    List<String> expectProcess = Arrays.asList(expect);
    List<String> expectRead =
        Lists.transform(
            expectProcess,
            new Function<String, String>() {
              @Override
              public String apply(String value) {
                return value.replaceAll("[\\r\\n]", "");
              }
            });

    for (int chunk : CHUNK_SIZES) {
      chunk = Math.max(1, Math.min(chunk, input.length()));
      assertEquals(expectProcess, bufferHelper(input, chunk));
      assertEquals(expectRead, readUsingJava(input, chunk));
      assertEquals(expectRead, readUsingReader(input, chunk, true));
      assertEquals(expectRead, readUsingReader(input, chunk, false));
    }
  }

  private static List<String> bufferHelper(String input, int chunk) throws IOException {
    final List<String> lines = Lists.newArrayList();
    LineBuffer lineBuf =
        new LineBuffer() {
          @Override
          protected void handleLine(String line, String end) {
            lines.add(line + end);
          }
        };
    char[] chars = input.toCharArray();
    int off = 0;
    while (off < chars.length) {
      int len = Math.min(chars.length, off + chunk) - off;
      lineBuf.add(chars, off, len);
      off += len;
    }
    lineBuf.finish();
    return lines;
  }

  private static List<String> readUsingJava(String input, int chunk) throws IOException {
    BufferedReader r = new BufferedReader(getChunkedReader(input, chunk));
    List<String> lines = Lists.newArrayList();
    String line;
    while ((line = r.readLine()) != null) {
      lines.add(line);
    }
    r.close();
    return lines;
  }

  private static List<String> readUsingReader(String input, int chunk, boolean asReader)
      throws IOException {
    Readable readable =
        asReader ? getChunkedReader(input, chunk) : getChunkedReadable(input, chunk);
    LineReader r = new LineReader(readable);
    List<String> lines = Lists.newArrayList();
    String line;
    while ((line = r.readLine()) != null) {
      lines.add(line);
    }
    return lines;
  }

  // Returns a Readable that is *not* a Reader.
  private static Readable getChunkedReadable(String input, int chunk) {
    final Reader reader = getChunkedReader(input, chunk);
    return new Readable() {
      @Override
      public int read(CharBuffer cbuf) throws IOException {
        return reader.read(cbuf);
      }
    };
  }

  private static Reader getChunkedReader(String input, final int chunk) {
    return new FilterReader(new StringReader(input)) {
      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        return super.read(cbuf, off, Math.min(chunk, len));
      }
    };
  }
}

/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.io;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Provides simple GWT-compatible substitutes for {@code InputStream}, {@code OutputStream},
 * {@code Reader}, and {@code Writer} so that {@code BaseEncoding} can use streaming implementations
 * while remaining GWT-compatible.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
final class GwtWorkarounds {
  private GwtWorkarounds() {}

  /**
   * A GWT-compatible substitute for a {@code Reader}.
   */
  interface CharInput {
    int read() throws IOException;
    void close() throws IOException;
  }

  /**
   * Views a {@code Reader} as a {@code CharInput}.
   */
  @GwtIncompatible("Reader")
  static CharInput asCharInput(final Reader reader) {
    checkNotNull(reader);
    return new CharInput() {
      @Override
      public int read() throws IOException {
        return reader.read();
      }

      @Override
      public void close() throws IOException {
        reader.close();
      }
    };
  }

  /**
   * Views a {@code CharSequence} as a {@code CharInput}.
   */
  static CharInput asCharInput(final CharSequence chars) {
    checkNotNull(chars);
    return new CharInput() {
      int index = 0;

      @Override
      public int read() {
        if (index < chars.length()) {
          return chars.charAt(index++);
        } else {
          return -1;
        }
      }

      @Override
      public void close() {
        index = chars.length();
      }
    };
  }

  /**
   * A GWT-compatible substitute for an {@code InputStream}.
   */
  interface ByteInput {
    int read() throws IOException;
    void close() throws IOException;
  }

  /**
   * Views a {@code ByteInput} as an {@code InputStream}.
   */
  @GwtIncompatible("InputStream")
  static InputStream asInputStream(final ByteInput input) {
    checkNotNull(input);
    return new InputStream() {
      @Override
      public int read() throws IOException {
        return input.read();
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        checkNotNull(b);
        checkPositionIndexes(off, off + len, b.length);
        if (len == 0) {
          return 0;
        }
        int firstByte = read();
        if (firstByte == -1) {
          return -1;
        }
        b[off] = (byte) firstByte;
        for (int dst = 1; dst < len; dst++) {
          int readByte = read();
          if (readByte == -1) {
            return dst;
          }
          b[off + dst] = (byte) readByte;
        }
        return len;
      }

      @Override
      public void close() throws IOException {
        input.close();
      }
    };
  }

  /**
   * A GWT-compatible substitute for an {@code OutputStream}.
   */
  interface ByteOutput {
    void write(byte b) throws IOException;
    void flush() throws IOException;
    void close() throws IOException;
  }

  /**
   * Views a {@code ByteOutput} as an {@code OutputStream}.
   */
  @GwtIncompatible("OutputStream")
  static OutputStream asOutputStream(final ByteOutput output) {
    checkNotNull(output);
    return new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        output.write((byte) b);
      }

      @Override
      public void flush() throws IOException {
        output.flush();
      }

      @Override
      public void close() throws IOException {
        output.close();
      }
    };
  }

  /**
   * A GWT-compatible substitute for a {@code Writer}.
   */
  interface CharOutput {
    void write(char c) throws IOException;
    void flush() throws IOException;
    void close() throws IOException;
  }

  /**
   * Views a {@code Writer} as a {@code CharOutput}.
   */
  @GwtIncompatible("Writer")
  static CharOutput asCharOutput(final Writer writer) {
    checkNotNull(writer);
    return new CharOutput() {
      @Override
      public void write(char c) throws IOException {
        writer.append(c);
      }

      @Override
      public void flush() throws IOException {
        writer.flush();
      }

      @Override
      public void close() throws IOException {
        writer.close();
      }
    };
  }

  /**
   * Returns a {@code CharOutput} whose {@code toString()} method can be used
   * to get the combined output.
   */
  static CharOutput stringBuilderOutput(int initialSize) {
    final StringBuilder builder = new StringBuilder(initialSize);
    return new CharOutput() {

      @Override
      public void write(char c) {
        builder.append(c);
      }

      @Override
      public void flush() {}

      @Override
      public void close() {}

      @Override
      public String toString() {
        return builder.toString();
      }
    };
  }
}

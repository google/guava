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

import com.google.common.annotations.GwtCompatible;

import java.io.IOException;

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
   * A GWT-compatible substitute for an {@code OutputStream}.
   */
  interface ByteOutput {
    void write(byte b) throws IOException;
    void flush() throws IOException;
    void close() throws IOException;
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


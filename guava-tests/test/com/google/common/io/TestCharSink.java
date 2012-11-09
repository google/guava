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

import static com.google.common.base.Charsets.UTF_8;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * A char sink for testing that has configurable behavior.
 *
 * @author Colin Decker
 */
public class TestCharSink extends CharSink implements TestStreamSupplier {

  private final TestByteSink byteSink;

  public TestCharSink(TestOption... options) {
    this.byteSink = new TestByteSink(options);
  }

  public String getString() {
    return new String(byteSink.getBytes(), UTF_8);
  }

  @Override
  public boolean wasStreamOpened() {
    return byteSink.wasStreamOpened();
  }

  @Override
  public boolean wasStreamClosed() {
    return byteSink.wasStreamClosed();
  }

  @Override
  public Writer openStream() throws IOException {
    // using TestByteSink's output stream to get option behavior, so flush to it on every write
    return new FilterWriter(new OutputStreamWriter(byteSink.openStream(), UTF_8)) {
      @Override
      public void write(int c) throws IOException {
        super.write(c);
        flush();
      }

      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        super.write(cbuf, off, len);
        flush();
      }

      @Override
      public void write(String str, int off, int len) throws IOException {
        super.write(str, off, len);
        flush();
      }
    };
  }
}

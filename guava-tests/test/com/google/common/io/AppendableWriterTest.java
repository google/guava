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

import static org.junit.Assert.assertThrows;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;

/**
 * Unit test for {@link AppendableWriter}.
 *
 * @author Alan Green
 */
public class AppendableWriterTest extends IoTestCase {

  /** Helper class for testing behavior with Flushable and Closeable targets. */
  private static class SpyAppendable implements Appendable, Flushable, Closeable {
    boolean flushed;
    boolean closed;
    StringBuilder result = new StringBuilder();

    @Override
    public Appendable append(CharSequence csq) {
      result.append(csq);
      return this;
    }

    @Override
    public Appendable append(char c) {
      result.append(c);
      return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) {
      result.append(csq, start, end);
      return this;
    }

    @Override
    public void flush() {
      flushed = true;
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  public void testWriteMethods() throws IOException {
    StringBuilder builder = new StringBuilder();
    Writer writer = new AppendableWriter(builder);

    writer.write("Hello".toCharArray());
    writer.write(',');
    writer.write(0xBEEF0020); // only lower 16 bits are important
    writer.write("Wo");
    writer.write("Whirled".toCharArray(), 3, 2);
    writer.write("Mad! Mad, I say", 2, 2);

    assertEquals("Hello, World!", builder.toString());
  }

  public void testAppendMethods() throws IOException {
    StringBuilder builder = new StringBuilder();
    Writer writer = new AppendableWriter(builder);

    writer.append("Hello,");
    writer.append(' ');
    writer.append("The World Wide Web", 4, 9);
    writer.append("!");

    assertEquals("Hello, World!", builder.toString());
  }

  public void testCloseFlush() throws IOException {
    SpyAppendable spy = new SpyAppendable();
    Writer writer = new AppendableWriter(spy);

    writer.write("Hello");
    assertFalse(spy.flushed);
    assertFalse(spy.closed);

    writer.flush();
    assertTrue(spy.flushed);
    assertFalse(spy.closed);

    writer.close();
    assertTrue(spy.flushed);
    assertTrue(spy.closed);
  }

  public void testCloseIsFinal() throws IOException {
    StringBuilder builder = new StringBuilder();
    Writer writer = new AppendableWriter(builder);

    writer.write("Hi");
    writer.close();

    assertThrows(IOException.class, () -> writer.write(" Greg"));

    assertThrows(IOException.class, () -> writer.flush());

    // close()ing already closed writer is allowed
    writer.close();
  }
}

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
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

/** @author Colin Decker */
public class TestWriter extends FilterWriter {

  private final TestOutputStream out;

  public TestWriter(TestOption... options) throws IOException {
    this(new TestOutputStream(ByteStreams.nullOutputStream(), options));
  }

  public TestWriter(TestOutputStream out) {
    super(new OutputStreamWriter(checkNotNull(out), UTF_8));
    this.out = out;
  }

  @Override
  public void write(int c) throws IOException {
    super.write(c);
    flush(); // flush write to TestOutputStream to get its behavior
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

  public boolean closed() {
    return out.closed();
  }
}

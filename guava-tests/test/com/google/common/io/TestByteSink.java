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

import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A byte sink for testing that has configurable behavior.
 *
 * @author Colin Decker
 */
public class TestByteSink extends ByteSink implements TestStreamSupplier {

  private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
  private final ImmutableSet<TestOption> options;

  private boolean outputStreamOpened;
  private boolean outputStreamClosed;

  public TestByteSink(TestOption... options) {
    this.options = ImmutableSet.copyOf(options);
  }

  byte[] getBytes() {
    return bytes.toByteArray();
  }

  @Override
  public boolean wasStreamOpened() {
    return outputStreamOpened;
  }

  @Override
  public boolean wasStreamClosed() {
    return outputStreamClosed;
  }

  @Override
  public OutputStream openStream() throws IOException {
    outputStreamOpened = true;
    bytes.reset(); // truncate
    return new Out();
  }

  private final class Out extends TestOutputStream {

    public Out() throws IOException {
      super(bytes, options);
    }

    @Override
    public void close() throws IOException {
      outputStreamClosed = true;
      super.close();
    }
  }
}

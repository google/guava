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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.TestOption.CLOSE_THROWS;
import static com.google.common.io.TestOption.FLUSH_THROWS;
import static com.google.common.io.TestOption.OPEN_THROWS;
import static com.google.common.io.TestOption.WRITE_THROWS;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.OutputStream;
import org.jspecify.annotations.NullUnmarked;

/**
 * @author Colin Decker
 */
@NullUnmarked
final class TestOutputStream extends OutputStream {
  private final OutputStream delegate;
  private final ImmutableSet<TestOption> options;
  private boolean closed;
  private boolean flushed;

  TestOutputStream(OutputStream delegate, TestOption... options) throws IOException {
    this(delegate, asList(options));
  }

  TestOutputStream(OutputStream delegate, Iterable<TestOption> options) throws IOException {
    this.delegate = checkNotNull(delegate);
    this.options = ImmutableSet.copyOf(options);
    throwIf(OPEN_THROWS);
  }

  boolean closed() {
    return closed;
  }

  boolean flushed() {
    return flushed;
  }

  @Override
  public void write(byte[] b) throws IOException {
    throwIf(closed);
    throwIf(WRITE_THROWS);
    delegate.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    throwIf(closed);
    throwIf(WRITE_THROWS);
    delegate.write(b, off, len);
  }

  @Override
  public void write(int b) throws IOException {
    throwIf(closed);
    throwIf(WRITE_THROWS);
    delegate.write(b);
  }

  @Override
  public void flush() throws IOException {
    throwIf(closed);
    flushed = true;
    delegate.flush();
    throwIf(FLUSH_THROWS);
  }

  @Override
  public void close() throws IOException {
    closed = true;
    delegate.close();
    throwIf(CLOSE_THROWS);
  }

  private void throwIf(TestOption option) throws IOException {
    throwIf(options.contains(option));
  }

  private static void throwIf(boolean condition) throws IOException {
    if (condition) {
      throw new IOException();
    }
  }
}

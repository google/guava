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

import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * A byte source for testing that has configurable behavior.
 *
 * @author Colin Decker
 */
public final class TestByteSource extends ByteSource implements TestStreamSupplier {

  private final byte[] bytes;
  private final ImmutableSet<TestOption> options;

  private boolean inputStreamOpened;
  private boolean inputStreamClosed;

  TestByteSource(byte[] bytes, TestOption... options) {
    this.bytes = checkNotNull(bytes);
    this.options = ImmutableSet.copyOf(options);
  }

  @Override
  public boolean wasStreamOpened() {
    return inputStreamOpened;
  }

  @Override
  public boolean wasStreamClosed() {
    return inputStreamClosed;
  }

  @Override
  public InputStream openStream() throws IOException {
    inputStreamOpened = true;
    return new RandomAmountInputStream(new In(), new Random());
  }

  private final class In extends TestInputStream {

    public In() throws IOException {
      super(new ByteArrayInputStream(bytes), options);
    }

    @Override
    public void close() throws IOException {
      inputStreamClosed = true;
      super.close();
    }
  }
}

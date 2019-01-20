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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * A char source for testing that has configurable options.
 *
 * @author Colin Decker
 */
public class TestCharSource extends CharSource implements TestStreamSupplier {

  private final TestByteSource byteSource;

  public TestCharSource(String content, TestOption... options) {
    this.byteSource = new TestByteSource(content.getBytes(UTF_8), options);
  }

  @Override
  public boolean wasStreamOpened() {
    return byteSource.wasStreamOpened();
  }

  @Override
  public boolean wasStreamClosed() {
    return byteSource.wasStreamClosed();
  }

  @Override
  public Reader openStream() throws IOException {
    return new InputStreamReader(byteSource.openStream(), UTF_8);
  }
}

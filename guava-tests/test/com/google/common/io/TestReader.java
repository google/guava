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

import java.io.ByteArrayInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStreamReader;

/** @author Colin Decker */
public class TestReader extends FilterReader {

  private final TestInputStream in;

  public TestReader(TestOption... options) throws IOException {
    this(new TestInputStream(new ByteArrayInputStream(new byte[10]), options));
  }

  public TestReader(TestInputStream in) {
    super(new InputStreamReader(checkNotNull(in), UTF_8));
    this.in = in;
  }

  public boolean closed() {
    return in.closed();
  }
}

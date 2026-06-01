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

import static com.google.common.io.ByteStreams.nullOutputStream;
import static com.google.common.io.TestOption.FLUSH_THROWS;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit tests for {@link Flushables}.
 *
 * <p>Checks proper flushing behavior, and ensures that IOExceptions on Flushable.flush() are not
 * propagated out from the {@link Flushables#flush} method if {@code swallowIOException} is true.
 *
 * @author Michael Lancaster
 */
@NullUnmarked
public class FlushablesTest extends TestCase {
  public void testFlush_clean_doNotSwallowException() throws IOException {
    TestOutputStream flushable = new TestOutputStream(nullOutputStream());
    Flushables.flush(flushable, /* swallowIOException= */ false);
    assertThat(flushable.flushed()).isTrue();
  }

  public void testFlush_clean_swallowIOException() throws IOException {
    TestOutputStream flushable = new TestOutputStream(nullOutputStream());
    Flushables.flush(flushable, /* swallowIOException= */ true);
    assertThat(flushable.flushed()).isTrue();
  }

  public void testFlush_flushableWithEatenException() throws IOException {
    TestOutputStream flushable = new TestOutputStream(nullOutputStream(), FLUSH_THROWS);
    Flushables.flush(flushable, /* swallowIOException= */ true);
    assertThat(flushable.flushed()).isTrue();
  }

  public void testFlush_flushableWithThrownException() throws IOException {
    TestOutputStream flushable = new TestOutputStream(nullOutputStream(), FLUSH_THROWS);
    assertThrows(
        IOException.class, () -> Flushables.flush(flushable, /* swallowIOException= */ false));
    assertThat(flushable.flushed()).isTrue();
  }

  public void testFlushQuietly_flushableWithEatenException() throws IOException {
    TestOutputStream flushable = new TestOutputStream(nullOutputStream(), FLUSH_THROWS);
    Flushables.flushQuietly(flushable);
    assertThat(flushable.flushed()).isTrue();
  }
}

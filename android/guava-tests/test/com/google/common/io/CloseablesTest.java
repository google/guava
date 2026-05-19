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

import static com.google.common.io.TestOption.CLOSE_THROWS;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit tests for {@link Closeables}.
 *
 * <p>Checks proper closing behavior, and ensures that IOExceptions on Closeable.close() are not
 * propagated out from the {@link Closeables#close} method if {@code swallowIOException} is true.
 *
 * @author Michael Lancaster
 */
@NullUnmarked
public class CloseablesTest extends TestCase {
  public void testClose_closeableClean_doNotSwallowException() throws IOException {
    TestInputStream closeable = new TestInputStream(emptyStream());
    Closeables.close(closeable, /* swallowIOException= */ false);
    assertThat(closeable.closed()).isTrue();
  }

  public void testClose_closeableClean_swallowIOException() throws IOException {
    TestInputStream closeable = new TestInputStream(emptyStream());
    Closeables.close(closeable, /* swallowIOException= */ true);
    assertThat(closeable.closed()).isTrue();
  }

  public void testClose_closeableWithEatenException() throws IOException {
    TestInputStream closeable = new TestInputStream(emptyStream(), CLOSE_THROWS);
    Closeables.close(closeable, /* swallowIOException= */ true);
    assertThat(closeable.closed()).isTrue();
  }

  public void testClose_closeableWithThrownException() throws IOException {
    TestInputStream closeable = new TestInputStream(emptyStream(), CLOSE_THROWS);
    assertThrows(
        IOException.class, () -> Closeables.close(closeable, /* swallowIOException= */ false));
    assertThat(closeable.closed()).isTrue();
  }

  public void testCloseQuietly_inputStreamWithEatenException() throws IOException {
    TestInputStream in = new TestInputStream(emptyStream(), CLOSE_THROWS);
    Closeables.closeQuietly(in);
    assertThat(in.closed()).isTrue();
  }

  public void testCloseQuietly_readerWithEatenException() throws IOException {
    TestReader reader = new TestReader(CLOSE_THROWS);
    Closeables.closeQuietly(reader);
    assertThat(reader.closed()).isTrue();
  }

  public void testCloseNull() throws IOException {
    Closeables.close(null, /* swallowIOException= */ true);
    Closeables.close(null, /* swallowIOException= */ false);
  }

  public void testCloseQuietlyNull_inputStream() {
    Closeables.closeQuietly((InputStream) null);
  }

  public void testCloseQuietlyNull_reader() {
    Closeables.closeQuietly((Reader) null);
  }

  private static ByteArrayInputStream emptyStream() {
    return new ByteArrayInputStream(new byte[0]);
  }
}

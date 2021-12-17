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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import junit.framework.TestCase;

/**
 * Unit tests for {@link Closeables}.
 *
 * <p>Checks proper closing behavior, and ensures that IOExceptions on Closeable.close() are not
 * propagated out from the {@link Closeables#close} method if {@code swallowException} is true.
 *
 * @author Michael Lancaster
 */
public class CloseablesTest extends TestCase {
  private Closeable mockCloseable;

  public void testClose_closeableClean() throws IOException {
    // make sure that no exception is thrown regardless of value of
    // 'swallowException' when the mock does not throw an exception.
    setupCloseable(false);
    doClose(mockCloseable, false, false);

    setupCloseable(false);
    doClose(mockCloseable, true, false);
  }

  public void testClose_closeableWithEatenException() throws IOException {
    // make sure that no exception is thrown if 'swallowException' is true
    // when the mock does throw an exception.
    setupCloseable(true);
    doClose(mockCloseable, true);
  }

  public void testClose_closeableWithThrownException() throws IOException {
    // make sure that the exception is thrown if 'swallowException' is false
    // when the mock does throw an exception.
    setupCloseable(true);
    doClose(mockCloseable, false);
  }

  public void testCloseQuietly_inputStreamWithEatenException() throws IOException {
    TestInputStream in =
        new TestInputStream(new ByteArrayInputStream(new byte[1]), TestOption.CLOSE_THROWS);
    Closeables.closeQuietly(in);
    assertTrue(in.closed());
  }

  public void testCloseQuietly_readerWithEatenException() throws IOException {
    TestReader in = new TestReader(TestOption.CLOSE_THROWS);
    Closeables.closeQuietly(in);
    assertTrue(in.closed());
  }

  public void testCloseNull() throws IOException {
    Closeables.close(null, true);
    Closeables.close(null, false);
  }

  public void testCloseQuietlyNull_inputStream() {
    Closeables.closeQuietly((InputStream) null);
  }

  public void testCloseQuietlyNull_reader() {
    Closeables.closeQuietly((Reader) null);
  }

  // Set up a closeable to expect to be closed, and optionally to throw an
  // exception.
  private void setupCloseable(boolean shouldThrow) throws IOException {
    mockCloseable = mock(Closeable.class);
    if (shouldThrow) {
      doThrow(new IOException("This should only appear in the logs. It should not be rethrown."))
          .when(mockCloseable)
          .close();
    }
  }

  private void doClose(Closeable closeable, boolean swallowException) throws IOException {
    doClose(closeable, swallowException, !swallowException);
  }

  // Close the closeable using the Closeables, passing in the swallowException
  // parameter. expectThrown determines whether we expect an exception to
  // be thrown by Closeables.close;
  private void doClose(Closeable closeable, boolean swallowException, boolean expectThrown)
      throws IOException {
    try {
      Closeables.close(closeable, swallowException);
      if (expectThrown) {
        fail("Didn't throw exception.");
      }
    } catch (IOException e) {
      if (!expectThrown) {
        fail("Threw exception");
      }
    }
    verify(closeable).close();
  }
}

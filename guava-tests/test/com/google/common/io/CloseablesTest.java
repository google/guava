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

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Unit tests for {@link Closeables}.
 *
 * <p>Checks proper closing behavior, and ensures that
 * IOExceptions on Closeable.close() are not
 * propagated out from the {@link Closeables#close} method if {@code
 * swallowException} is true.
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

  public void testCloseQuietly_closeableWithEatenException()
      throws IOException {
    // make sure that no exception is thrown by CloseQuietly when the mock does
    // throw an exception on close
    setupCloseable(true);
    Closeables.closeQuietly(mockCloseable);
  }

  public void testCloseQuietly_inputStreamWithEatenException() throws IOException {
    TestInputStream in = new TestInputStream(
        new ByteArrayInputStream(new byte[1]), TestOption.CLOSE_THROWS);
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
    Closeables.closeQuietly((Closeable) null);
  }

  public void testCloseQuietlyNull_inputStream() {
    Closeables.closeQuietly((InputStream) null);
  }

  public void testCloseQuietlyNull_reader() {
    Closeables.closeQuietly((Reader) null);
  }

  @Override protected void setUp() throws Exception {
    mockCloseable = createStrictMock(Closeable.class);
  }

  private void expectThrown() {
    expectLastCall().andThrow(new IOException("This should only appear in the "
        + "logs. It should not be rethrown."));
  }

  // Set up a closeable to expect to be closed, and optionally to throw an
  // exception.
  private void setupCloseable(boolean shouldThrow) throws IOException {
    reset(mockCloseable);
    mockCloseable.close();
    if (shouldThrow) {
      expectThrown();
    }
    replay(mockCloseable);
  }

  private void doClose(Closeable closeable, boolean swallowException) {
    doClose(closeable, swallowException, !swallowException);
  }

  // Close the closeable using the Closeables, passing in the swallowException
  // parameter. expectThrown determines whether we expect an exception to
  // be thrown by Closeables.close;
  private void doClose(Closeable closeable, boolean swallowException,
      boolean expectThrown) {
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
    verify(closeable);
  }
}

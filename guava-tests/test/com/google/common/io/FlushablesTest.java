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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.Flushable;
import java.io.IOException;
import junit.framework.TestCase;

/**
 * Unit tests for {@link Flushables}.
 *
 * <p>Checks proper flushing behavior, and ensures that IOExceptions on Flushable.flush() are not
 * propagated out from the {@link Flushables#flush} method if {@code swallowException} is true.
 *
 * @author Michael Lancaster
 */
public class FlushablesTest extends TestCase {
  private Flushable mockFlushable;

  public void testFlush_clean() throws IOException {
    // make sure that no exception is thrown regardless of value of
    // 'swallowException' when the mock does not throw an exception.
    setupFlushable(false);
    doFlush(mockFlushable, false, false);

    setupFlushable(false);
    doFlush(mockFlushable, true, false);
  }

  public void testFlush_flushableWithEatenException() throws IOException {
    // make sure that no exception is thrown if 'swallowException' is true
    // when the mock does throw an exception on flush.
    setupFlushable(true);
    doFlush(mockFlushable, true, false);
  }

  public void testFlush_flushableWithThrownException() throws IOException {
    // make sure that the exception is thrown if 'swallowException' is false
    // when the mock does throw an exception on flush.
    setupFlushable(true);
    doFlush(mockFlushable, false, true);
  }

  public void testFlushQuietly_flushableWithEatenException() throws IOException {
    // make sure that no exception is thrown by flushQuietly when the mock does
    // throw an exception on flush.
    setupFlushable(true);
    Flushables.flushQuietly(mockFlushable);
  }

  // Set up a flushable to expect to be flushed, and optionally to
  // throw an exception.
  private void setupFlushable(boolean shouldThrowOnFlush) throws IOException {
    mockFlushable = mock(Flushable.class);
    if (shouldThrowOnFlush) {
      doThrow(
              new IOException(
                  "This should only appear in the " + "logs. It should not be rethrown."))
          .when(mockFlushable)
          .flush();
    }
  }

  // Flush the flushable using the Flushables, passing in the swallowException
  // parameter. expectThrown determines whether we expect an exception to
  // be thrown by Flushables.flush;
  private void doFlush(Flushable flushable, boolean swallowException, boolean expectThrown)
      throws IOException {
    try {
      Flushables.flush(flushable, swallowException);
      if (expectThrown) {
        fail("Didn't throw exception.");
      }
    } catch (IOException e) {
      if (!expectThrown) {
        fail("Threw exception");
      }
    }
    verify(flushable).flush();
  }
}

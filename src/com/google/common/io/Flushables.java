/*
 * Copyright (C) 2007 Google Inc.
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

import java.io.Flushable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for working with {@link Flushable} objects.
 *
 * @author Michael Lancaster
 * @since 2009.09.15 <b>tentative</b>
 */
public final class Flushables {
  private static final Logger logger
      = Logger.getLogger(Flushables.class.getName());

  private Flushables() {}

  /**
   * Flush a {@link Flushable}, with control over whether an
   * {@code IOException} may be thrown.
   *
   * <p>If {@code swallowIOException} is true, then we don't rethrow
   * {@code IOException}, but merely log it.
   *
   * @param flushable the {@code Flushable} object to be flushed.
   * @param swallowIOException if true, don't propagate IO exceptions
   *     thrown by the {@code flush} method
   * @throws IOException if {@code swallowIOException} is false and
   *     {@link Flushable#flush} throws an {@code IOException}.
   * @see Closeables#close
   */
  public static void flush(Flushable flushable, boolean swallowIOException)
      throws IOException {
    try {
      flushable.flush();
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "IOException thrown while flushing Flushable.", e);
      if (!swallowIOException) {
        throw e;
      }
    }
  }

  /**
   * Equivalent to calling {@code flush(flushable, true)}, but with no
   * {@code IOException} in the signature.
   *
   * @param flushable the {@code Flushable} object to be flushed.
   */
  public static void flushQuietly(Flushable flushable) {
    try {
      flush(flushable, true);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "IOException should not have been thrown.", e);
    }
  }
}

/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility methods for working with {@link Closeable} objects.
 *
 * @author Michael Lancaster
 * @since 1.0
 */
@Beta
@GwtIncompatible
public final class Closeables {
  @VisibleForTesting static final Logger logger = Logger.getLogger(Closeables.class.getName());

  private Closeables() {}

  /**
   * Closes a {@link Closeable}, with control over whether an {@code IOException} may be thrown.
   * This is primarily useful in a finally block, where a thrown exception needs to be logged but
   * not propagated (otherwise the original exception will be lost).
   *
   * <p>If {@code swallowIOException} is true then we never throw {@code IOException} but merely log
   * it.
   *
   * <p>Example:
   *
   * <pre>{@code
   * public void useStreamNicely() throws IOException {
   *   SomeStream stream = new SomeStream("foo");
   *   boolean threw = true;
   *   try {
   *     // ... code which does something with the stream ...
   *     threw = false;
   *   } finally {
   *     // If an exception occurs, rethrow it only if threw==false:
   *     Closeables.close(stream, threw);
   *   }
   * }
   * }</pre>
   *
   * @param closeable the {@code Closeable} object to be closed, or null, in which case this method
   *     does nothing
   * @param swallowIOException if true, don't propagate IO exceptions thrown by the {@code close}
   *     methods
   * @throws IOException if {@code swallowIOException} is false and {@code close} throws an {@code
   *     IOException}.
   */
  public static void close(@Nullable Closeable closeable, boolean swallowIOException)
      throws IOException {
    if (closeable == null) {
      return;
    }
    try {
      closeable.close();
    } catch (IOException e) {
      if (swallowIOException) {
        logger.log(Level.WARNING, "IOException thrown while closing Closeable.", e);
      } else {
        throw e;
      }
    }
  }

  /**
   * Closes the given {@link InputStream}, logging any {@code IOException} that's thrown rather than
   * propagating it.
   *
   * <p>While it's not safe in the general case to ignore exceptions that are thrown when closing an
   * I/O resource, it should generally be safe in the case of a resource that's being used only for
   * reading, such as an {@code InputStream}. Unlike with writable resources, there's no chance that
   * a failure that occurs when closing the stream indicates a meaningful problem such as a failure
   * to flush all bytes to the underlying resource.
   *
   * @param inputStream the input stream to be closed, or {@code null} in which case this method
   *     does nothing
   * @since 17.0
   */
  public static void closeQuietly(@Nullable InputStream inputStream) {
    try {
      close(inputStream, true);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Closes the given {@link Reader}, logging any {@code IOException} that's thrown rather than
   * propagating it.
   *
   * <p>While it's not safe in the general case to ignore exceptions that are thrown when closing an
   * I/O resource, it should generally be safe in the case of a resource that's being used only for
   * reading, such as a {@code Reader}. Unlike with writable resources, there's no chance that a
   * failure that occurs when closing the reader indicates a meaningful problem such as a failure to
   * flush all bytes to the underlying resource.
   *
   * @param reader the reader to be closed, or {@code null} in which case this method does nothing
   * @since 17.0
   */
  public static void closeQuietly(@Nullable Reader reader) {
    try {
      close(reader, true);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }
}

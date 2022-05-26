/*
 * Copyright (C) 2009 The Guava Authors
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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotMock;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A callback interface to process bytes from a stream.
 *
 * <p>{@link #processBytes} will be called for each chunk of data that is read, and should return
 * {@code false} when you want to stop processing.
 *
 * @author Chris Nokleberg
 * @since 1.0
 */
@Beta
@DoNotMock("Implement it normally")
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public interface ByteProcessor<T extends @Nullable Object> {
  /**
   * This method will be called for each chunk of bytes in an input stream. The implementation
   * should process the bytes from {@code buf[off]} through {@code buf[off + len - 1]} (inclusive).
   *
   * @param buf the byte array containing the data to process
   * @param off the initial offset into the array
   * @param len the length of data to be processed
   * @return true to continue processing, false to stop
   */
  @CanIgnoreReturnValue // some uses know that their processor never returns false
  boolean processBytes(byte[] buf, int off, int len) throws IOException;

  /** Return the result of processing all the bytes. */
  @ParametricNullness
  T getResult();
}

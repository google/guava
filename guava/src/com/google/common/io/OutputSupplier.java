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

import java.io.IOException;

/**
 * A factory for writable streams of bytes or characters.
 *
 * @author Chris Nokleberg
 * @since 1.0
 * @deprecated For {@code OutputSupplier<? extends OutputStream>}, use
 *     {@link ByteSink} instead. For {@code OutputSupplier<? extends Writer>},
 *     use {@link CharSink}. Implementations of {@code OutputSupplier} that
 *     don't fall into one of those categories do not benefit from any of the
 *     methods in {@code common.io} and should use a different interface. This
 *     interface is scheduled for removal in December 2015.
 */
@Deprecated
public interface OutputSupplier<T> {

  /**
   * Returns an object that encapsulates a writable resource.
   * <p>
   * Like {@link Iterable#iterator}, this method may be called repeatedly to
   * get independent channels to the same underlying resource.
   * <p>
   * Where the channel maintains a position within the resource, moving that
   * cursor within one channel should not affect the starting position of
   * channels returned by other calls.
   */
  T getOutput() throws IOException;
}

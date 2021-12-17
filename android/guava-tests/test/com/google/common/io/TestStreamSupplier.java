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

/**
 * Interface for a supplier of streams that can report whether a stream was opened and whether that
 * stream was closed. Intended for use in a test where only a single stream should be opened and
 * possibly closed.
 *
 * @author Colin Decker
 */
public interface TestStreamSupplier {

  /** Returns whether or not a new stream was opened. */
  boolean wasStreamOpened();

  /** Returns whether or not an open stream was closed. */
  boolean wasStreamClosed();
}

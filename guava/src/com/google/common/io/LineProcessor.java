/*
 * Copyright (C) 2009 The Guava Authors
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

import com.google.common.annotations.Beta;

import java.io.IOException;

/**
 * A callback to be used with the streaming {@code readLines} methods.
 *
 * <p>{@link #processLine} will be called for each line that is read, and
 * should return {@code false} when you want to stop processing.
 *
 * @author Miles Barr
 * @since 1.0
 */
@Beta
public interface LineProcessor<T> {

  /**
   * This method will be called once for each line.
   *
   * @param line the line read from the input, without delimiter
   * @return true to continue processing, false to stop
   */
  boolean processLine(String line) throws IOException;

  /** Return the result of processing all the lines. */
  T getResult();
}

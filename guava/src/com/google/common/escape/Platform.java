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

package com.google.common.escape;

import com.google.common.annotations.GwtCompatible;

/**
 * Methods factored out so that they can be emulated differently in GWT.
 *
 * @author Jesse Wilson
 */
@GwtCompatible(emulated = true)
final class Platform {
  private Platform() {}

  /** Returns a thread-local 1024-char array. */
  static char[] charBufferFromThreadLocal() {
    return DEST_TL.get();
  }

  /**
   * A thread-local destination buffer to keep us from creating new buffers.
   * The starting size is 1024 characters.  If we grow past this we don't
   * put it back in the threadlocal, we just keep going and grow as needed.
   */
  private static final ThreadLocal<char[]> DEST_TL = new ThreadLocal<char[]>() {
    @Override
    protected char[] initialValue() {
      return new char[1024];
    }
  };
}

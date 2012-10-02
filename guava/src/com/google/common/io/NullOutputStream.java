/*
 * Copyright (C) 2004 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;

import java.io.OutputStream;

/**
 * Implementation of {@link OutputStream} that simply discards written bytes.
 *
 * @author Spencer Kimball
 * @since 1.0
 * @deprecated Use {@link ByteStreams#nullOutputStream} instead. This class is
 *     scheduled to be removed in Guava release 15.0.
 */
@Beta
@Deprecated
public final class NullOutputStream extends OutputStream {
  /** Discards the specified byte. */
  @Override public void write(int b) {
  }

  /** Discards the specified byte array. */
  @Override public void write(byte[] b, int off, int len) {
    checkNotNull(b);
  }
}

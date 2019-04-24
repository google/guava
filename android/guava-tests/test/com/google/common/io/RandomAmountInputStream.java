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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/** Returns a random portion of the requested bytes on each call. */
class RandomAmountInputStream extends FilterInputStream {
  private final Random random;

  public RandomAmountInputStream(InputStream in, Random random) {
    super(checkNotNull(in));
    this.random = checkNotNull(random);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return super.read(b, off, random.nextInt(len) + 1);
  }
}

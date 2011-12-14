/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.hash;

import com.google.common.annotations.Beta;

/**
 * Funnels for common types. All implementations are serializable.
 * 
 * @author Dimitris Andreou
 * @since 11.0
 */
@Beta
public final class Funnels {
  private Funnels() {}

  /**
   * Returns a funnel that extracts the bytes from a {@code byte} array.
   */
  public static Funnel<byte[]> byteArrayFunnel() {
    return ByteArrayFunnel.INSTANCE;
  }

  private enum ByteArrayFunnel implements Funnel<byte[]> {
    INSTANCE;

    public void funnel(byte[] from, Sink into) {
      into.putBytes(from);
    }

    @Override public String toString() {
      return "Funnels.byteArrayFunnel()";
    }
  }

  /**
   * Returns a funnel that extracts the characters from a {@code CharSequence}.
   */
  public static Funnel<CharSequence> stringFunnel() {
    return StringFunnel.INSTANCE;
  }

  private enum StringFunnel implements Funnel<CharSequence> {
    INSTANCE;

    public void funnel(CharSequence from, Sink into) {
      into.putString(from);
    }

    @Override public String toString() {
      return "Funnels.stringFunnel()";
    }
  }
}

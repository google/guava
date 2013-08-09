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

import java.io.Serializable;

/**
 * Static factories for creating {@link HashCode} instances; most users should never have to use
 * this. All returned instances are {@link Serializable}.
 *
 * @author Dimitris Andreou
 * @since 12.0
 * @deprecated Use the duplicated methods in {@link HashCode} instead. This class is scheduled
 *     to be removed in Guava 16.0.
 */
@Beta
@Deprecated
public final class HashCodes {
  private HashCodes() {}

  /**
   * Creates a 32-bit {@code HashCode}, of which the bytes will form the passed int, interpreted
   * in little endian order.
   *
   * @deprecated Use {@link HashCode#fromInt} instead. This method is scheduled to be removed in
   *     Guava 16.0.
   */
  @Deprecated
  public static HashCode fromInt(int hash) {
    return HashCode.fromInt(hash);
  }

  /**
   * Creates a 64-bit {@code HashCode}, of which the bytes will form the passed long, interpreted
   * in little endian order.
   *
   * @deprecated Use {@link HashCode#fromLong} instead. This method is scheduled to be removed in
   *     Guava 16.0.
   */
  @Deprecated
  public static HashCode fromLong(long hash) {
    return HashCode.fromLong(hash);
  }

  /**
   * Creates a {@code HashCode} from a byte array. The array is defensively copied to preserve
   * the immutability contract of {@code HashCode}. The array cannot be empty.
   *
   * @deprecated Use {@link HashCode#fromBytes} instead. This method is scheduled to be removed in
   *     Guava 16.0.
   */
  @Deprecated
  public static HashCode fromBytes(byte[] bytes) {
    return HashCode.fromBytes(bytes);
  }
}

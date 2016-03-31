/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.util.concurrent;

import com.google.common.annotations.GwtIncompatible;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to classes in the {@code java.util.concurrent.atomic} package.
 *
 * @author Kurt Alfred Kluever
 * @since 10.0
 */
@GwtIncompatible
public final class Atomics {
  private Atomics() {}

  /**
   * Creates an {@code AtomicReference} instance with no initial value.
   *
   * @return a new {@code AtomicReference} with no initial value
   */
  public static <V> AtomicReference<V> newReference() {
    return new AtomicReference<V>();
  }

  /**
   * Creates an {@code AtomicReference} instance with the given initial value.
   *
   * @param initialValue the initial value
   * @return a new {@code AtomicReference} with the given initial value
   */
  public static <V> AtomicReference<V> newReference(@Nullable V initialValue) {
    return new AtomicReference<V>(initialValue);
  }

  /**
   * Creates an {@code AtomicReferenceArray} instance of given length.
   *
   * @param length the length of the array
   * @return a new {@code AtomicReferenceArray} with the given length
   */
  public static <E> AtomicReferenceArray<E> newReferenceArray(int length) {
    return new AtomicReferenceArray<E>(length);
  }

  /**
   * Creates an {@code AtomicReferenceArray} instance with the same length as, and all elements
   * copied from, the given array.
   *
   * @param array the array to copy elements from
   * @return a new {@code AtomicReferenceArray} copied from the given array
   */
  public static <E> AtomicReferenceArray<E> newReferenceArray(E[] array) {
    return new AtomicReferenceArray<E>(array);
  }
}

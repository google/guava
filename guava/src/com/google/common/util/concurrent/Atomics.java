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
import com.google.errorprone.annotations.InlineMe;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import org.jspecify.annotations.Nullable;

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
   * <p><b>Note:</b> this method is now unnecessary and should be treated as deprecated. Instead,
   * use the {@code AtomicReference} {@linkplain AtomicReference#AtomicReference() constructor}
   * directly, taking advantage of <a
   * href="https://docs.oracle.com/javase/tutorial/java/generics/genTypeInference.html#type-inference-instantiation">"diamond"
   * syntax</a>.
   *
   * @return a new {@code AtomicReference} with no initial value
   */
  @InlineMe(
      replacement = "new AtomicReference<>()",
      imports = "java.util.concurrent.atomic.AtomicReference")
  public static <V> AtomicReference<@Nullable V> newReference() {
    return new AtomicReference<>();
  }

  /**
   * Creates an {@code AtomicReference} instance with the given initial value.
   *
   * <p><b>Note:</b> this method is now unnecessary and should be treated as deprecated. Instead,
   * use the {@code AtomicReference} {@linkplain AtomicReference#AtomicReference(Object)
   * constructor} directly, taking advantage of <a
   * href="https://docs.oracle.com/javase/tutorial/java/generics/genTypeInference.html#type-inference-instantiation">"diamond"
   * syntax</a>.
   *
   * @param initialValue the initial value
   * @return a new {@code AtomicReference} with the given initial value
   */
  @InlineMe(
      replacement = "new AtomicReference<>(initialValue)",
      imports = "java.util.concurrent.atomic.AtomicReference")
  public static <V extends @Nullable Object> AtomicReference<V> newReference(
      @ParametricNullness V initialValue) {
    return new AtomicReference<>(initialValue);
  }

  /**
   * Creates an {@code AtomicReferenceArray} instance of given length.
   *
   * <p><b>Note:</b> this method is now unnecessary and should be treated as deprecated. Instead,
   * use the {@code AtomicReferenceArray} {@linkplain AtomicReferenceArray#AtomicReferenceArray(int)
   * constructor} directly, taking advantage of <a
   * href="https://docs.oracle.com/javase/tutorial/java/generics/genTypeInference.html#type-inference-instantiation">"diamond"
   * syntax</a>.
   *
   * @param length the length of the array
   * @return a new {@code AtomicReferenceArray} with the given length
   */
  @InlineMe(
      replacement = "new AtomicReferenceArray<>(length)",
      imports = "java.util.concurrent.atomic.AtomicReferenceArray")
  public static <E> AtomicReferenceArray<@Nullable E> newReferenceArray(int length) {
    return new AtomicReferenceArray<>(length);
  }

  /**
   * Creates an {@code AtomicReferenceArray} instance with the same length as, and all elements
   * copied from, the given array.
   *
   * <p><b>Note:</b> this method is now unnecessary and should be treated as deprecated. Instead,
   * use the {@code AtomicReferenceArray} {@linkplain
   * AtomicReferenceArray#AtomicReferenceArray(Object[]) constructor} directly, taking advantage of
   * <a
   * href="https://docs.oracle.com/javase/tutorial/java/generics/genTypeInference.html#type-inference-instantiation">"diamond"
   * syntax</a>.
   *
   * @param array the array to copy elements from
   * @return a new {@code AtomicReferenceArray} copied from the given array
   */
  @InlineMe(
      replacement = "new AtomicReferenceArray<>(array)",
      imports = "java.util.concurrent.atomic.AtomicReferenceArray")
  public static <E extends @Nullable Object> AtomicReferenceArray<E> newReferenceArray(E[] array) {
    return new AtomicReferenceArray<>(array);
  }
}

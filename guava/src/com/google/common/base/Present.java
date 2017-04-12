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

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of an {@link Optional} containing a reference.
 */
@GwtCompatible
final class Present<T> extends Optional<T> {
  private final T reference;

  Present(T reference) {
    this.reference = reference;
  }

  @Override
  public boolean isPresent() {
    return true;
  }

  @Override
  public T get() {
    return reference;
  }

  @Override
  public T or(T defaultValue) {
    checkNotNull(defaultValue, "use Optional.orNull() instead of Optional.or(null)");
    return reference;
  }

  @Override
  public Optional<T> or(Optional<? extends T> secondChoice) {
    checkNotNull(secondChoice);
    return this;
  }

  @Override
  public T or(Supplier<? extends T> supplier) {
    checkNotNull(supplier);
    return reference;
  }

  @Override
  public T orNull() {
    return reference;
  }

  @Override
  public Set<T> asSet() {
    return Collections.singleton(reference);
  }

  @Override
  public <V> Optional<V> transform(Function<? super T, V> function) {
    return new Present<V>(
        checkNotNull(
            function.apply(reference),
            "the Function passed to Optional.transform() must not return null."));
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof Present) {
      Present<?> other = (Present<?>) object;

      if (reference instanceof Object[] && other.reference instanceof Object[]) {
        return Arrays.deepEquals((Object[]) reference, (Object[]) other.reference);
      } else if (reference instanceof boolean[] && other.reference instanceof boolean[]) {
        return Arrays.equals((boolean[]) reference, (boolean[]) other.reference);
      } else if (reference instanceof byte[] && other.reference instanceof byte[]) {
        return Arrays.equals((byte[]) reference, (byte[]) other.reference);
      } else if (reference instanceof char[] && other.reference instanceof char[]) {
        return Arrays.equals((char[]) reference, (char[]) other.reference);
      } else if (reference instanceof double[] && other.reference instanceof double[]) {
        return Arrays.equals((double[]) reference, (double[]) other.reference);
      } else if (reference instanceof float[] && other.reference instanceof float[]) {
        return Arrays.equals((float[]) reference, (float[]) other.reference);
      } else if (reference instanceof int[] && other.reference instanceof int[]) {
        return Arrays.equals((int[]) reference, (int[]) other.reference);
      } else if (reference instanceof long[] && other.reference instanceof long[]) {
        return Arrays.equals((long[]) reference, (long[]) other.reference);
      } else if (reference instanceof short[] && other.reference instanceof short[]) {
        return Arrays.equals((short[]) reference, (short[]) other.reference);
      }

      return reference.equals(other.reference);
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (reference instanceof Object[]) {
      return 0x598df91c + Arrays.deepHashCode((Object[]) reference);
    } else if (reference instanceof boolean[]) {
      return 0x598df91c + Arrays.hashCode((boolean[]) reference);
    } else if (reference instanceof byte[]) {
      return 0x598df91c + Arrays.hashCode((byte[]) reference);
    } else if (reference instanceof char[]) {
      return 0x598df91c + Arrays.hashCode((char[]) reference);
    } else if (reference instanceof double[]) {
      return 0x598df91c + Arrays.hashCode((double[]) reference);
    } else if (reference instanceof float[]) {
      return 0x598df91c + Arrays.hashCode((float[]) reference);
    } else if (reference instanceof int[]) {
      return 0x598df91c + Arrays.hashCode((int[]) reference);
    } else if (reference instanceof long[]) {
      return 0x598df91c + Arrays.hashCode((long[]) reference);
    } else if (reference instanceof short[]) {
      return 0x598df91c + Arrays.hashCode((short[]) reference);
    }
    return 0x598df91c + reference.hashCode();
  }

  @Override
  public String toString() {
    return "Optional.of(" + reference + ")";
  }

  private static final long serialVersionUID = 0;
}

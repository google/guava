/*
 * Copyright (C) 2021 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A utility method to perform unchecked casts to suppress errors produced by nullness analyses. */
@GwtCompatible
@ElementTypesAreNonnullByDefault
final class NullnessCasts {
  /**
   * Accepts a {@code @Nullable T} and returns a plain {@code T}, without performing any check that
   * that conversion is safe.
   *
   * <p>This method is intended to help with usages of type parameters that have {@linkplain
   * ParametricNullness parametric nullness}. If a type parameter instead ranges over only non-null
   * types (or if the type is a non-variable type, like {@code String}), then code should almost
   * never use this method, preferring instead to call {@code requireNonNull} so as to benefit from
   * its runtime check.
   *
   * <p>An example use case for this method is in implementing an {@code Iterator<T>} whose {@code
   * next} field is lazily initialized. The type of that field would be {@code @Nullable T}, and the
   * code would be responsible for populating a "real" {@code T} (which might still be the value
   * {@code null}!) before returning it to callers. Depending on how the code is structured, a
   * nullness analysis might not understand that the field has been populated. To avoid that problem
   * without having to add {@code @SuppressWarnings}, the code can call this method.
   *
   * <p>Why <i>not</i> just add {@code SuppressWarnings}? The problem is that this method is
   * typically useful for {@code return} statements. That leaves the code with two options: Either
   * add the suppression to the whole method (which turns off checking for a large section of code),
   * or extract a variable, and put the suppression on that. However, a local variable typically
   * doesn't work: Because nullness analyses typically infer the nullness of local variables,
   * there's no way to assign a {@code @Nullable T} to a field {@code T foo;} and instruct the
   * analysis that that means "plain {@code T}" rather than the inferred type {@code @Nullable T}.
   * (Even if supported added {@code @NonNull}, that would not help, since the problem case
   * addressed by this method is the case in which {@code T} has parametric nullness -- and thus its
   * value may be legitimately {@code null}.)
   */
  @SuppressWarnings("nullness")
  @ParametricNullness
  static <T extends @Nullable Object> T uncheckedCastNullableTToT(@CheckForNull T t) {
    return t;
  }

  /**
   * Returns {@code null} cast to any type.
   *
   * <p>This method is intended to help with usages of type parameters that have {@linkplain
   * ParametricNullness parametric nullness}. Sometimes, code may receive a null {@code T} but store
   * a "null sentinel" to take its place. When the time comes to convert it back to a {@code T} to
   * return to a caller, the code needs to a way to return {@code null} from a method that returns
   * "plain {@code T}." This API provides that.
   */
  @SuppressWarnings({"nullness", "TypeParameterUnusedInFormals", "ReturnMissingNullable"})
  // The warnings are legitimate. Each time we use this method, we document why.
  @ParametricNullness
  static <T extends @Nullable Object> T uncheckedNull() {
    return null;
  }

  private NullnessCasts() {}
}

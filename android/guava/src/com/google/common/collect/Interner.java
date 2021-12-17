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

package com.google.common.collect;

import com.google.common.annotations.GwtIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.DoNotMock;

/**
 * Provides similar behavior to {@link String#intern} for any immutable type. Common implementations
 * are available from the {@link Interners} class.
 *
 * <p>Note that {@code String.intern()} has some well-known performance limitations, and should
 * generally be avoided. Prefer {@link Interners#newWeakInterner} or another {@code Interner}
 * implementation even for {@code String} interning.
 *
 * @author Kevin Bourrillion
 * @since 3.0
 */
@DoNotMock("Use Interners.new*Interner")
@GwtIncompatible
@ElementTypesAreNonnullByDefault
public interface Interner<E> {
  /**
   * Chooses and returns the representative instance for any of a collection of instances that are
   * equal to each other. If two {@linkplain Object#equals equal} inputs are given to this method,
   * both calls will return the same instance. That is, {@code intern(a).equals(a)} always holds,
   * and {@code intern(a) == intern(b)} if and only if {@code a.equals(b)}. Note that {@code
   * intern(a)} is permitted to return one instance now and a different instance later if the
   * original interned instance was garbage-collected.
   *
   * <p><b>Warning:</b> do not use with mutable objects.
   *
   * @throws NullPointerException if {@code sample} is null
   */
  @CanIgnoreReturnValue // TODO(cpovirk): Consider removing this?
  E intern(E sample);
}

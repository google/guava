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

package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * A {@link ListenableFuture} whose result may be set by a {@link #set(Object)},
 * {@link #setException(Throwable)} or {@link #setFuture(ListenableFuture)} call. 
 * It may also be cancelled.
 *
 * @author Sven Mawson
 * @since 9.0 (in 1.0 as {@code ValueFuture})
 */
@GwtCompatible
public final class SettableFuture<V> extends AbstractFuture.TrustedFuture<V> {

  /**
   * Creates a new {@code SettableFuture} in the default state.
   */
  public static <V> SettableFuture<V> create() {
    return new SettableFuture<V>();
  }

  /**
   * Explicit private constructor, use the {@link #create} factory method to
   * create instances of {@code SettableFuture}.
   */
  private SettableFuture() {}

  @Override public boolean set(@Nullable V value) {
    return super.set(value);
  }

  @Override public boolean setException(Throwable throwable) {
    return super.setException(throwable);
  }

  @Beta
  @Override
  public boolean setFuture(ListenableFuture<? extends V> future) {
    return super.setFuture(future);
  }
}

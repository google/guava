/*
 * Copyright (C) 2009 Google Inc.
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

/**
 * A simple ListenableFuture that holds a value or an exception.
 *
 * @author Sven Mawson
 * @since 2009.09.15 <b>tentative</b>
 */
public class ValueFuture<V> extends AbstractListenableFuture<V> {

  /**
   * Creates a new {@code ValueFuture} in the default state.
   */
  public static <T> ValueFuture<T> create() {
    return new ValueFuture<T>();
  }

  /**
   * Explicit private constructor, use the {@link #create} factory method to
   * create instances of {@code ValueFuture}.
   */
  private ValueFuture() {}

  /**
   * Sets the value of this future.  This method will return {@code true} if
   * the value was successfully set, or {@code false} if the future has already
   * been set or cancelled.
   *
   * @param newValue the value the future should hold.
   * @return true if the value was successfully set.
   */
  @Override
  public boolean set(V newValue) {
    return super.set(newValue);
  }

  /**
   * Sets the future to having failed with the given exception.  This exception
   * will be wrapped in an ExecutionException and thrown from the get methods.
   * This method will return {@code true} if the exception was successfully set,
   * or {@code false} if the future has already been set or cancelled.
   *
   * @param t the exception the future should hold.
   * @return true if the exception was successfully set.
   */
  @Override
  public boolean setException(Throwable t) {
    return super.setException(t);
  }

  /**
   * {@inheritDoc}
   *
   * <p>A ValueFuture is never considered in the running state, so the
   * {@code mayInterruptIfRunning} argument is ignored.
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return super.cancel();
  }
}

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

import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.Set;

/**
 * The purpose of the CheckCloseSupplier is to report when all closeable objects
 * supplied by the delegate supplier are closed. To do this, the factory method
 * returns a decorated version of the {@code delegate} supplied in the
 * constructor. The decoration mechanism is left up to the subclass via the
 * abstract {@link #wrap} method.
 *
 * <p>The decorated object returned from {@link #wrap} should ideally override
 * its {@code close} method to not only call {@code super.close()} but to also
 * call {@code callback.delegateClosed()}.
 *
 * @author Chris Nokleberg
 */
abstract class CheckCloseSupplier<T> {
  private final Set<Callback> open = Sets.newHashSet();

  abstract static class Input<T> extends CheckCloseSupplier<T>
      implements InputSupplier<T> {
    private final InputSupplier<? extends T> delegate;

    public Input(InputSupplier<? extends T> delegate) {
      this.delegate = delegate;
    }

    @Override public T getInput() throws IOException {
      return wrap(delegate.getInput(), newCallback());
    }
  }

  abstract static class Output<T> extends CheckCloseSupplier<T>
      implements OutputSupplier<T> {
    private final OutputSupplier<? extends T> delegate;

    public Output(OutputSupplier<? extends T> delegate) {
      this.delegate = delegate;
    }

    @Override public T getOutput() throws IOException {
      return wrap(delegate.getOutput(), newCallback());
    }
  }

  public final class Callback {
    public void delegateClosed() {
      open.remove(this);
    }
  }

  protected Callback newCallback() {
    Callback callback = new Callback();
    open.add(callback);
    return callback;
  }

  /**
   * Subclasses should wrap the given object and call
   * {@link Callback#delegateClosed} when the close method of the delegate is
   * called, to inform the supplier that the underlying
   * {@code Closeable} is not longer open.
   *
   * @param object the object to wrap.
   * @param callback the object that the wrapper should call to signal that the
   */
  protected abstract T wrap(T object, Callback callback);

  /** Returns true if all the closeables have been closed closed */
  public boolean areClosed() {
    return open.isEmpty();
  }
}

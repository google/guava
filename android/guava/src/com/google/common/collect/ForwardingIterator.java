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

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An iterator which forwards all its method calls to another iterator. Subclasses should override
 * one or more methods to modify the behavior of the backing iterator as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>{@code default} method warning:</b> This class forwards calls to <i>only some</i> {@code
 * default} methods. Specifically, it forwards calls only for methods that existed <a
 * href="https://docs.oracle.com/javase/7/docs/api/java/util/Iterator.html">before {@code default}
 * methods were introduced</a>. For newer methods, like {@code forEachRemaining}, it inherits their
 * default implementations. When those implementations invoke methods, they invoke methods on the
 * {@code ForwardingIterator}.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public abstract class ForwardingIterator<T extends @Nullable Object> extends ForwardingObject
    implements Iterator<T> {

  /** Constructor for use by subclasses. */
  protected ForwardingIterator() {}

  @Override
  protected abstract Iterator<T> delegate();

  @Override
  public boolean hasNext() {
    return delegate().hasNext();
  }

  @CanIgnoreReturnValue
  @Override
  @ParametricNullness
  public T next() {
    return delegate().next();
  }

  @Override
  public void remove() {
    delegate().remove();
  }
}

/*
 * Copyright (C) 2010 The Guava Authors
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
import java.util.List;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * A list multimap which forwards all its method calls to another list multimap. Subclasses should
 * override one or more methods to modify the behavior of the backing multimap as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>{@code default} method warning:</b> This class does <i>not</i> forward calls to {@code
 * default} methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the {@code ForwardingListMultimap}.
 *
 * @author Kurt Alfred Kluever
 * @since 3.0
 */
@GwtCompatible
public abstract class ForwardingListMultimap<K, V> extends ForwardingMultimap<K, V>
    implements ListMultimap<K, V> {

  /** Constructor for use by subclasses. */
  protected ForwardingListMultimap() {}

  @Override
  protected abstract ListMultimap<K, V> delegate();

  @Override
  public List<V> get(@NullableDecl K key) {
    return delegate().get(key);
  }

  @CanIgnoreReturnValue
  @Override
  public List<V> removeAll(@NullableDecl Object key) {
    return delegate().removeAll(key);
  }

  @CanIgnoreReturnValue
  @Override
  public List<V> replaceValues(K key, Iterable<? extends V> values) {
    return delegate().replaceValues(key, values);
  }
}

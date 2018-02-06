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
import java.util.Comparator;
import java.util.SortedSet;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * A sorted set multimap which forwards all its method calls to another sorted set multimap.
 * Subclasses should override one or more methods to modify the behavior of the backing multimap as
 * desired per the <a href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>{@code default} method warning:</b> This class does <i>not</i> forward calls to {@code
 * default} methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the {@code ForwardingSortedSetMultimap}.
 *
 * @author Kurt Alfred Kluever
 * @since 3.0
 */
@GwtCompatible
public abstract class ForwardingSortedSetMultimap<K, V> extends ForwardingSetMultimap<K, V>
    implements SortedSetMultimap<K, V> {

  /** Constructor for use by subclasses. */
  protected ForwardingSortedSetMultimap() {}

  @Override
  protected abstract SortedSetMultimap<K, V> delegate();

  @Override
  public SortedSet<V> get(@NullableDecl K key) {
    return delegate().get(key);
  }

  @Override
  public SortedSet<V> removeAll(@NullableDecl Object key) {
    return delegate().removeAll(key);
  }

  @Override
  public SortedSet<V> replaceValues(K key, Iterable<? extends V> values) {
    return delegate().replaceValues(key, values);
  }

  @Override
  public Comparator<? super V> valueComparator() {
    return delegate().valueComparator();
  }
}

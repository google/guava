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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * A sorted map which forwards all its method calls to another sorted map. Subclasses should
 * override one or more methods to modify the behavior of the backing sorted map as desired per the
 * <a href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>Warning:</b> The methods of {@code ForwardingSortedMap} forward <i>indiscriminately</i> to
 * the methods of the delegate. For example, overriding {@link #put} alone <i>will not</i> change
 * the behavior of {@link #putAll}, which can lead to unexpected behavior. In this case, you should
 * override {@code putAll} as well, either providing your own implementation, or delegating to the
 * provided {@code standardPutAll} method.
 *
 * <p><b>{@code default} method warning:</b> This class does <i>not</i> forward calls to {@code
 * default} methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the {@code ForwardingSortedMap}.
 *
 * <p>Each of the {@code standard} methods, where appropriate, use the comparator of the map to test
 * equality for both keys and values, unlike {@code ForwardingMap}.
 *
 * <p>The {@code standard} methods and the collection views they return are not guaranteed to be
 * thread-safe, even when all of the methods that they depend on are thread-safe.
 *
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible
public abstract class ForwardingSortedMap<K, V> extends ForwardingMap<K, V>
    implements SortedMap<K, V> {
  // TODO(lowasser): identify places where thread safety is actually lost

  /** Constructor for use by subclasses. */
  protected ForwardingSortedMap() {}

  @Override
  protected abstract SortedMap<K, V> delegate();

  @Override
  public Comparator<? super K> comparator() {
    return delegate().comparator();
  }

  @Override
  public K firstKey() {
    return delegate().firstKey();
  }

  @Override
  public SortedMap<K, V> headMap(K toKey) {
    return delegate().headMap(toKey);
  }

  @Override
  public K lastKey() {
    return delegate().lastKey();
  }

  @Override
  public SortedMap<K, V> subMap(K fromKey, K toKey) {
    return delegate().subMap(fromKey, toKey);
  }

  @Override
  public SortedMap<K, V> tailMap(K fromKey) {
    return delegate().tailMap(fromKey);
  }

  /**
   * A sensible implementation of {@link SortedMap#keySet} in terms of the methods of {@code
   * ForwardingSortedMap}. In many cases, you may wish to override {@link
   * ForwardingSortedMap#keySet} to forward to this implementation or a subclass thereof.
   *
   * @since 15.0
   */
  @Beta
  protected class StandardKeySet extends Maps.SortedKeySet<K, V> {
    /** Constructor for use by subclasses. */
    public StandardKeySet() {
      super(ForwardingSortedMap.this);
    }
  }

  // unsafe, but worst case is a CCE is thrown, which callers will be expecting
  @SuppressWarnings("unchecked")
  private int unsafeCompare(Object k1, Object k2) {
    Comparator<? super K> comparator = comparator();
    if (comparator == null) {
      return ((Comparable<Object>) k1).compareTo(k2);
    } else {
      return ((Comparator<Object>) comparator).compare(k1, k2);
    }
  }

  /**
   * A sensible definition of {@link #containsKey} in terms of the {@code firstKey()} method of
   * {@link #tailMap}. If you override {@link #tailMap}, you may wish to override {@link
   * #containsKey} to forward to this implementation.
   *
   * @since 7.0
   */
  @Override
  @Beta
  protected boolean standardContainsKey(@NullableDecl Object key) {
    try {
      // any CCE will be caught
      @SuppressWarnings("unchecked")
      SortedMap<Object, V> self = (SortedMap<Object, V>) this;
      Object ceilingKey = self.tailMap(key).firstKey();
      return unsafeCompare(ceilingKey, key) == 0;
    } catch (ClassCastException | NoSuchElementException | NullPointerException e) {
      return false;
    }
  }

  /**
   * A sensible default implementation of {@link #subMap(Object, Object)} in terms of {@link
   * #headMap(Object)} and {@link #tailMap(Object)}. In some situations, you may wish to override
   * {@link #subMap(Object, Object)} to forward to this implementation.
   *
   * @since 7.0
   */
  @Beta
  protected SortedMap<K, V> standardSubMap(K fromKey, K toKey) {
    checkArgument(unsafeCompare(fromKey, toKey) <= 0, "fromKey must be <= toKey");
    return tailMap(fromKey).headMap(toKey);
  }
}

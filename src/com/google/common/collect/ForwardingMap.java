/*
 * Copyright (C) 2007 Google Inc.
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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.base.Supplier;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A map which forwards all its method calls to another map. Subclasses should
 * override one or more methods to modify the behavior of the backing map as
 * desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><em>Warning</em>: The methods of {@code ForwardingMap} forward
 * <em>indiscriminately</em> to the methods of the delegate. For example,
 * overriding {@link #put} alone <em>will not</em> change the behavior of {@link
 * #putAll}, which can lead to unexpected behavior. In this case, you should
 * override {@code putAll} as well, either providing your own implementation, or
 * delegating to the provided {@code standardPutAll} method.
 *
 * <p>Each of the {@code standard} methods, where appropriate, use {@link
 * Objects#equal} to test equality for both keys and values. This may not be
 * the desired behavior for map implementations that use non-standard notions of
 * key equality, such as a {@code SortedMap} whose comparator is not consistent
 * with {@code equals}.
 *
 * <p>The {@code standard} methods and the collection views they return are not
 * guaranteed to be thread-safe, even when all of the methods that they depend
 * on are thread-safe.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 2 (imported from Google Collections Library)
 */
@GwtCompatible
public abstract class ForwardingMap<K, V> extends ForwardingObject
    implements Map<K, V> {
  // TODO(user): identify places where thread safety is actually lost

  /** Constructor for use by subclasses. */
  protected ForwardingMap() {}

  @Override protected abstract Map<K, V> delegate();

  public int size() {
    return delegate().size();
  }

  public boolean isEmpty() {
    return delegate().isEmpty();
  }

  public V remove(Object object) {
    return delegate().remove(object);
  }

  public void clear() {
    delegate().clear();
  }

  public boolean containsKey(Object key) {
    return delegate().containsKey(key);
  }

  public boolean containsValue(Object value) {
    return delegate().containsValue(value);
  }

  public V get(Object key) {
    return delegate().get(key);
  }

  public V put(K key, V value) {
    return delegate().put(key, value);
  }

  public void putAll(Map<? extends K, ? extends V> map) {
    delegate().putAll(map);
  }

  public Set<K> keySet() {
    return delegate().keySet();
  }

  public Collection<V> values() {
    return delegate().values();
  }

  public Set<Entry<K, V>> entrySet() {
    return delegate().entrySet();
  }

  @Override public boolean equals(@Nullable Object object) {
    return object == this || delegate().equals(object);
  }

  @Override public int hashCode() {
    return delegate().hashCode();
  }

  /**
   * A sensible definition of {@link #putAll(Map)} in terms of {@link
   * #put(Object, Object)}. If you override {@link #put(Object, Object)}, you
   * may wish to override {@link #putAll(Map)} to forward to this
   * implementation.
   *
   * @since 7
   */
  @Beta protected void standardPutAll(Map<? extends K, ? extends V> map) {
    Maps.putAllImpl(this, map);
  }

  /**
   * A sensible, albeit inefficient, definition of {@link #remove} in terms of
   * the {@code iterator} method of {@link #entrySet}. If you override {@link
   * #entrySet}, you may wish to override {@link #remove} to forward to this
   * implementation.
   *
   * <p>Alternately, you may wish to override {@link #remove} with {@code
   * keySet().remove}, assuming that approach would not lead to an infinite
   * loop.
   *
   * @since 7
   */
  @Beta protected V standardRemove(@Nullable Object key) {
    Iterator<Entry<K, V>> entryIterator = entrySet().iterator();
    while (entryIterator.hasNext()) {
      Entry<K, V> entry = entryIterator.next();
      if (Objects.equal(entry.getKey(), key)) {
        V value = entry.getValue();
        entryIterator.remove();
        return value;
      }
    }
    return null;
  }

  /**
   * A sensible definition of {@link #clear} in terms of the {@code iterator}
   * method of {@link #entrySet}. In many cases, you may wish to override
   * {@link #clear} to forward to this implementation.
   *
   * @since 7
   */
  @Beta protected void standardClear() {
    Iterator<Entry<K, V>> entryIterator = entrySet().iterator();
    while (entryIterator.hasNext()) {
      entryIterator.next();
      entryIterator.remove();
    }
  }

  /**
   * A sensible definition of {@link #keySet} in terms of the following methods:
   * {@link #clear}, {@link #containsKey}, {@link #isEmpty}, {@link #remove},
   * {@link #size}, and the {@code iterator} method of {@link #entrySet}. In
   * many cases, you may wish to override {@link #keySet} to forward to this
   * implementation.
   *
   * @since 7
   */
  @Beta protected Set<K> standardKeySet() {
    return Maps.keySetImpl(this);
  }

  /**
   * A sensible, albeit inefficient, definition of {@link #containsKey} in terms
   * of the {@code iterator} method of {@link #entrySet}. If you override {@link
   * #entrySet}, you may wish to override {@link #containsKey} to forward to
   * this implementation.
   *
   * @since 7
   */
  @Beta protected boolean standardContainsKey(@Nullable Object key) {
    return Maps.containsKeyImpl(this, key);
  }

  /**
   * A sensible definition of {@link #values} in terms of the following methods:
   * {@link #clear}, {@link #containsValue}, {@link #isEmpty}, {@link #size},
   * and the {@code iterator} method of {@link #entrySet}. In many cases, you
   * may wish to override {@link #values} to forward to this implementation.
   *
   * @since 7
   */
  @Beta protected Collection<V> standardValues() {
    return Maps.valuesImpl(this);
  }

  /**
   * A sensible definition of {@link #containsValue} in terms of the {@code
   * iterator} method of {@link #entrySet}. If you override {@link #entrySet},
   * you may wish to override {@link #containsValue} to forward to this
   * implementation.
   *
   * @since 7
   */
  @Beta protected boolean standardContainsValue(@Nullable Object value) {
    return Maps.containsValueImpl(this, value);
  }

  /**
   * A sensible definition of {@link #entrySet} in terms of the specified {@code
   * Supplier}, which is used to generate iterators over the entry set, and in
   * terms of the following methods: {@link #clear}, {@link #containsKey},
   * {@link #get}, {@link #isEmpty}, {@link #remove}, and {@link #size}. In many
   * cases, you may wish to override {@link #entrySet} to forward to this
   * implementation.
   *
   * @param entryIteratorSupplier A creator for iterators over the entry set.
   *        Each call to {@code get} must return an iterator that will
   *        traverse the entire entry set.
   *
   * @since 7
   */
  @Beta protected Set<Entry<K, V>> standardEntrySet(
      Supplier<Iterator<Entry<K, V>>> entryIteratorSupplier) {
    return Maps.entrySetImpl(this, entryIteratorSupplier);
  }

  /**
   * A sensible definition of {@link #isEmpty} in terms of the {@code iterator}
   * method of {@link #entrySet}. If you override {@link #entrySet}, you may
   * wish to override {@link #isEmpty} to forward to this implementation.
   *
   * @since 7
   */
  @Beta protected boolean standardIsEmpty() {
    return !entrySet().iterator().hasNext();
  }

  /**
   * A sensible definition of {@link #equals} in terms of the {@code equals}
   * method of {@link #entrySet}. If you override {@link #entrySet}, you may
   * wish to override {@link #equals} to forward to this implementation.
   *
   * @since 7
   */
  @Beta protected boolean standardEquals(@Nullable Object object) {
    return Maps.equalsImpl(this, object);
  }

  /**
   * A sensible definition of {@link #hashCode} in terms of the {@code iterator}
   * method of {@link #entrySet}. If you override {@link #entrySet}, you may
   * wish to override {@link #hashCode} to forward to this implementation.
   *
   * @since 7
   */
  @Beta protected int standardHashCode() {
    return Sets.hashCodeImpl(entrySet());
  }

  /**
   * A sensible definition of {@link #toString} in terms of the {@code iterator}
   * method of {@link #entrySet}. If you override {@link #entrySet}, you may
   * wish to override {@link #toString} to forward to this implementation.
   *
   * @since 7
   */
  @Beta protected String standardToString() {
    return Maps.toStringImpl(this);
  }
}

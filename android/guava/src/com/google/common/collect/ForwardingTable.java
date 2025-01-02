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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * A table which forwards all its method calls to another table. Subclasses should override one or
 * more methods to modify the behavior of the backing map as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * @author Gregory Kick
 * @since 7.0
 */
@GwtCompatible
public abstract class ForwardingTable<
        R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
    extends ForwardingObject implements Table<R, C, V> {
  /** Constructor for use by subclasses. */
  protected ForwardingTable() {}

  @Override
  protected abstract Table<R, C, V> delegate();

  @Override
  public Set<Cell<R, C, V>> cellSet() {
    return delegate().cellSet();
  }

  @Override
  public void clear() {
    delegate().clear();
  }

  @Override
  public Map<R, V> column(@ParametricNullness C columnKey) {
    return delegate().column(columnKey);
  }

  @Override
  public Set<C> columnKeySet() {
    return delegate().columnKeySet();
  }

  @Override
  public Map<C, Map<R, V>> columnMap() {
    return delegate().columnMap();
  }

  @Override
  public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
    return delegate().contains(rowKey, columnKey);
  }

  @Override
  public boolean containsColumn(@Nullable Object columnKey) {
    return delegate().containsColumn(columnKey);
  }

  @Override
  public boolean containsRow(@Nullable Object rowKey) {
    return delegate().containsRow(rowKey);
  }

  @Override
  public boolean containsValue(@Nullable Object value) {
    return delegate().containsValue(value);
  }

  @Override
  public @Nullable V get(@Nullable Object rowKey, @Nullable Object columnKey) {
    return delegate().get(rowKey, columnKey);
  }

  @Override
  public boolean isEmpty() {
    return delegate().isEmpty();
  }

  @CanIgnoreReturnValue
  @Override
  public @Nullable V put(
      @ParametricNullness R rowKey, @ParametricNullness C columnKey, @ParametricNullness V value) {
    return delegate().put(rowKey, columnKey, value);
  }

  @Override
  public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
    delegate().putAll(table);
  }

  @CanIgnoreReturnValue
  @Override
  public @Nullable V remove(@Nullable Object rowKey, @Nullable Object columnKey) {
    return delegate().remove(rowKey, columnKey);
  }

  @Override
  public Map<C, V> row(@ParametricNullness R rowKey) {
    return delegate().row(rowKey);
  }

  @Override
  public Set<R> rowKeySet() {
    return delegate().rowKeySet();
  }

  @Override
  public Map<R, Map<C, V>> rowMap() {
    return delegate().rowMap();
  }

  @Override
  public int size() {
    return delegate().size();
  }

  @Override
  public Collection<V> values() {
    return delegate().values();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return (obj == this) || delegate().equals(obj);
  }

  @Override
  public int hashCode() {
    return delegate().hashCode();
  }
}

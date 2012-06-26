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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;

import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * An empty implementation of {@link ImmutableTable}.
 *
 * @author Gregory Kick
 */
@GwtCompatible
@Immutable
final class EmptyImmutableTable extends ImmutableTable<Object, Object, Object> {
  static final EmptyImmutableTable INSTANCE = new EmptyImmutableTable();

  private EmptyImmutableTable() {}

  @Override public int size() {
    return 0;
  }

  @Override public Object get(@Nullable Object rowKey,
      @Nullable Object columnKey) {
    return null;
  }

  @Override public boolean isEmpty() {
    return true;
  }

  @Override public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof Table) {
      Table<?, ?, ?> that = (Table<?, ?, ?>) obj;
      return that.isEmpty();
    } else {
      return false;
    }
  }

  @Override public int hashCode() {
    return 0;
  }

  @Override public ImmutableSet<Cell<Object, Object, Object>> cellSet() {
    return ImmutableSet.of();
  }

  @Override public ImmutableMap<Object, Object> column(Object columnKey) {
    checkNotNull(columnKey);
    return ImmutableMap.of();
  }

  @Override public ImmutableSet<Object> columnKeySet() {
    return ImmutableSet.of();
  }

  @Override public ImmutableMap<Object, Map<Object, Object>> columnMap() {
    return ImmutableMap.of();
  }

  @Override public boolean contains(@Nullable Object rowKey,
      @Nullable Object columnKey) {
    return false;
  }

  @Override public boolean containsColumn(@Nullable Object columnKey) {
    return false;
  }

  @Override public boolean containsRow(@Nullable Object rowKey) {
    return false;
  }

  @Override public boolean containsValue(@Nullable Object value) {
    return false;
  }

  @Override public ImmutableMap<Object, Object> row(Object rowKey) {
    checkNotNull(rowKey);
    return ImmutableMap.of();
  }

  @Override public ImmutableSet<Object> rowKeySet() {
    return ImmutableSet.of();
  }

  @Override public ImmutableMap<Object, Map<Object, Object>> rowMap() {
    return ImmutableMap.of();
  }

  @Override public String toString() {
    return "{}";
  }

  @Override public ImmutableCollection<Object> values() {
    return ImmutableSet.of();
  }

  Object readResolve() {
    return INSTANCE; // preserve singleton property
  }

  private static final long serialVersionUID = 0;
}

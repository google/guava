/*
 * Copyright (C) 2008 The Guava Authors
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
import java.io.Serializable;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/** @see com.google.common.collect.Maps#immutableEntry(Object, Object) */
@GwtCompatible(serializable = true)
class ImmutableEntry<K, V> extends AbstractMapEntry<K, V> implements Serializable {
  @NullableDecl final K key;
  @NullableDecl final V value;

  ImmutableEntry(@NullableDecl K key, @NullableDecl V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  @NullableDecl
  public final K getKey() {
    return key;
  }

  @Override
  @NullableDecl
  public final V getValue() {
    return value;
  }

  @Override
  public final V setValue(V value) {
    throw new UnsupportedOperationException();
  }

  private static final long serialVersionUID = 0;
}

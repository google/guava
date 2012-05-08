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

import java.util.Map.Entry;

/**
 * {@code values()} implementation for {@link ImmutableMap}.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
abstract class ImmutableMapValues<K, V> extends ImmutableCollection<V> {
  ImmutableMapValues() {}

  abstract ImmutableMap<K, V> map();

  @Override
  public int size() {
    return map().size();
  }

  @Override
  public UnmodifiableIterator<V> iterator() {
    return Maps.valueIterator(map().entrySet().iterator());
  }

  @Override
  public boolean contains(Object object) {
    return map().containsValue(object);
  }

  @Override
  boolean isPartialView() {
    return true;
  }

  @Override
  ImmutableList<V> createAsList() {
    final ImmutableList<Entry<K, V>> entryList = map().entrySet().asList();
    return new ImmutableAsList<V>() {
      @Override
      public V get(int index) {
        return entryList.get(index).getValue();
      }

      @Override
      ImmutableCollection<V> delegateCollection() {
        return ImmutableMapValues.this;
      }
    };
  }
}


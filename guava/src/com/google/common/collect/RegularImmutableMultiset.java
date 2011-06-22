/*
 * Copyright (C) 2011 The Guava Authors
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

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Implementation of {@link ImmutableMultiset} with one or more elements.
 * 
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true)
@SuppressWarnings("serial")
// uses writeReplace(), not default serialization
class RegularImmutableMultiset<E> extends ImmutableMultiset<E> {
  private final transient ImmutableMap<E, Integer> map;
  private final transient int size;

  RegularImmutableMultiset(ImmutableMap<E, Integer> map, int size) {
    this.map = map;
    this.size = size;
  }

  @Override
  boolean isPartialView() {
    return map.isPartialView();
  }

  @Override
  public int count(@Nullable Object element) {
    Integer value = map.get(element);
    return (value == null) ? 0 : value;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean contains(@Nullable Object element) {
    return map.containsKey(element);
  }

  @Override
  public ImmutableSet<E> elementSet() {
    return map.keySet();
  }

  @Override
  UnmodifiableIterator<Entry<E>> entryIterator() {
    final Iterator<Map.Entry<E, Integer>> mapIterator =
        map.entrySet().iterator();
    return new UnmodifiableIterator<Entry<E>>() {
      @Override
      public boolean hasNext() {
        return mapIterator.hasNext();
      }

      @Override
      public Entry<E> next() {
        Map.Entry<E, Integer> mapEntry = mapIterator.next();
        return Multisets.immutableEntry(mapEntry.getKey(), mapEntry.getValue());
      }
    };
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  int distinctElements() {
    return map.size();
  }
}

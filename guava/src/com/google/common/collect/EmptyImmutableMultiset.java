/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;

import javax.annotation.Nullable;

/**
 * An empty immutable multiset.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true)
final class EmptyImmutableMultiset extends ImmutableMultiset<Object> {
  static final EmptyImmutableMultiset INSTANCE = new EmptyImmutableMultiset();

  @Override
  public int count(@Nullable Object element) {
    return 0;
  }

  @Override
  public boolean contains(@Nullable Object object) {
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> targets) {
    return targets.isEmpty();
  }

  @Override
  public UnmodifiableIterator<Object> iterator() {
    return Iterators.emptyIterator();
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof Multiset) {
      Multiset<?> other = (Multiset<?>) object;
      return other.isEmpty();
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public ImmutableSet<Object> elementSet() {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<Entry<Object>> entrySet() {
    return ImmutableSet.of();
  }

  @Override
  ImmutableSet<Entry<Object>> createEntrySet() {
    throw new AssertionError("should never be called");
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  public Object[] toArray() {
    return ObjectArrays.EMPTY_ARRAY;
  }

  @Override
  public <T> T[] toArray(T[] other) {
    return asList().toArray(other);
  }

  @Override
  public ImmutableList<Object> asList() {
    return ImmutableList.of();
  }

  Object readResolve() {
    return INSTANCE; // preserve singleton property
  }

  private static final long serialVersionUID = 0;
}

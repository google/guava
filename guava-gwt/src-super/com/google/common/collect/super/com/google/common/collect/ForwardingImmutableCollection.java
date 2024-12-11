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

import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A GWT-only class only used by GWT emulations. It is used to consolidate the definitions of method
 * delegation to save code size.
 *
 * @author Hayward Chan
 */
// TODO: Make this class GWT serializable.
@ElementTypesAreNonnullByDefault
class ForwardingImmutableCollection<E> extends ImmutableCollection<E> {

  final transient Collection<E> delegate;

  ForwardingImmutableCollection(Collection<E> delegate) {
    this.delegate = delegate;
  }

  @Override
  public UnmodifiableIterator<E> iterator() {
    return Iterators.unmodifiableIterator(delegate.iterator());
  }

  @Override
  public boolean contains(@Nullable Object object) {
    return object != null && delegate.contains(object);
  }

  @Override
  public boolean containsAll(Collection<?> targets) {
    return delegate.containsAll(targets);
  }

  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public Object[] toArray() {
    return delegate.toArray();
  }

  @Override
  public <T extends @Nullable Object> T[] toArray(T[] other) {
    return delegate.toArray(other);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}

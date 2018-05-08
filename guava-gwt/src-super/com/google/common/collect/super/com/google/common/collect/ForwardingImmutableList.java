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
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * GWT emulated version of {@link ImmutableList}. TODO(cpovirk): more doc
 *
 * @author Hayward Chan
 */
abstract class ForwardingImmutableList<E> extends ImmutableList<E> {

  ForwardingImmutableList() {}

  abstract List<E> delegateList();

  public int indexOf(@Nullable Object object) {
    return delegateList().indexOf(object);
  }

  public int lastIndexOf(@Nullable Object object) {
    return delegateList().lastIndexOf(object);
  }

  public E get(int index) {
    return delegateList().get(index);
  }

  public ImmutableList<E> subList(int fromIndex, int toIndex) {
    return unsafeDelegateList(delegateList().subList(fromIndex, toIndex));
  }

  @Override
  public Object[] toArray() {
    // Note that ArrayList.toArray() doesn't work here because it returns E[]
    // instead of Object[].
    return delegateList().toArray(new Object[size()]);
  }

  @Override
  public boolean equals(Object obj) {
    return delegateList().equals(obj);
  }

  @Override
  public int hashCode() {
    return delegateList().hashCode();
  }

  @Override
  public UnmodifiableIterator<E> iterator() {
    return Iterators.unmodifiableIterator(delegateList().iterator());
  }

  @Override
  public boolean contains(@Nullable Object object) {
    return object != null && delegateList().contains(object);
  }

  @Override
  public boolean containsAll(Collection<?> targets) {
    return delegateList().containsAll(targets);
  }

  public int size() {
    return delegateList().size();
  }

  @Override
  public boolean isEmpty() {
    return delegateList().isEmpty();
  }

  @Override
  public <T> T[] toArray(T[] other) {
    return delegateList().toArray(other);
  }

  @Override
  public String toString() {
    return delegateList().toString();
  }
}

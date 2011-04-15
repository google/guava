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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * GWT emulated version of {@link ImmutableCollection}.
 *
 * @author Jesse Wilson
 */
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableCollection<E>
    implements Collection<E>, Serializable {

  static final ImmutableCollection<Object> EMPTY_IMMUTABLE_COLLECTION
      = new ForwardingImmutableCollection<Object>(Collections.emptyList());

  ImmutableCollection() {}

  public abstract UnmodifiableIterator<E> iterator();

  public Object[] toArray() {
    Object[] newArray = new Object[size()];
    return toArray(newArray);
  }

  public <T> T[] toArray(T[] other) {
    int size = size();
    if (other.length < size) {
      other = ObjectArrays.newArray(other, size);
    } else if (other.length > size) {
      other[size] = null;
    }

    // Writes will produce ArrayStoreException when the toArray() doc requires.
    Object[] otherAsObjectArray = other;
    int index = 0;
    for (E element : this) {
      otherAsObjectArray[index++] = element;
    }
    return other;
  }

  public boolean contains(@Nullable Object object) {
    if (object == null) {
      return false;
    }
    for (E element : this) {
      if (element.equals(object)) {
        return true;
      }
    }
    return false;
  }

  public boolean containsAll(Collection<?> targets) {
    for (Object target : targets) {
      if (!contains(target)) {
        return false;
      }
    }
    return true;
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public final boolean add(E e) {
    throw new UnsupportedOperationException();
  }

  public final boolean remove(Object object) {
    throw new UnsupportedOperationException();
  }

  public final boolean addAll(Collection<? extends E> newElements) {
    throw new UnsupportedOperationException();
  }

  public final boolean removeAll(Collection<?> oldElements) {
    throw new UnsupportedOperationException();
  }

  public final boolean retainAll(Collection<?> elementsToKeep) {
    throw new UnsupportedOperationException();
  }

  public final void clear() {
    throw new UnsupportedOperationException();
  }

  private transient ImmutableList<E> asList;

  public ImmutableList<E> asList() {
    ImmutableList<E> list = asList;
    return (list == null) ? (asList = createAsList()) : list;
  }

  ImmutableList<E> createAsList() {
    switch (size()) {
      case 0:
        return ImmutableList.of();
      case 1:
        return ImmutableList.of(iterator().next());
      default:
        @SuppressWarnings("unchecked")
        E[] castedArray = (E[]) toArray();
        return new ImmutableAsList<E>(Arrays.asList(castedArray));
    }
  }
  static <E> ImmutableCollection<E> unsafeDelegate(Collection<E> delegate) {
    return new ForwardingImmutableCollection<E>(delegate);
  }
  
  boolean isPartialView(){
    return false;
  }

  abstract static class Builder<E> {

    public abstract Builder<E> add(E element);

    public Builder<E> add(E... elements) {
      checkNotNull(elements); // for GWT
      for (E element : elements) {
        add(checkNotNull(element));
      }
      return this;
    }

    public Builder<E> addAll(Iterable<? extends E> elements) {
      checkNotNull(elements); // for GWT
      for (E element : elements) {
        add(checkNotNull(element));
      }
      return this;
    }

    public Builder<E> addAll(Iterator<? extends E> elements) {
      checkNotNull(elements); // for GWT
      while (elements.hasNext()) {
        add(checkNotNull(elements.next()));
      }
      return this;
    }

    public abstract ImmutableCollection<E> build();
  }
}

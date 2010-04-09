/*
 * Copyright (C) 2009 Google Inc.
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

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * List returned by {@link ImmutableCollection#asList} when the collection isn't
 * an {@link ImmutableList} or an {@link ImmutableSortedSet}.
 *
 * @author Jared Levy
 */
@SuppressWarnings("serial")
final class ImmutableAsList<E> extends RegularImmutableList<E> {
  private final transient ImmutableCollection<E> collection;

  ImmutableAsList(Object[] array, ImmutableCollection<E> collection) {
    super(array, 0, array.length);
    this.collection = collection;
  }

  @Override public boolean contains(Object target) {
    // The collection's contains() is at least as fast as RegularImmutableList's
    // and is often faster.
    return collection.contains(target);
  }

  /**
   * Serialized form that leads to the same performance as the original list.
   */
  static class SerializedForm implements Serializable {
    final ImmutableCollection<?> collection;
    SerializedForm(ImmutableCollection<?> collection) {
      this.collection = collection;
    }
    Object readResolve() {
      return collection.asList();
    }
    private static final long serialVersionUID = 0;
  }

  private void readObject(ObjectInputStream stream)
      throws InvalidObjectException {
    throw new InvalidObjectException("Use SerializedForm");
  }

  @Override Object writeReplace() {
    return new SerializedForm(collection);
  }
}

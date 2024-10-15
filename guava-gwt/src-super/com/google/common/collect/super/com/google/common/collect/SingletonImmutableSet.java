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

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * GWT emulation of {@link SingletonImmutableSet}.
 *
 * @author Hayward Chan
 */
@ElementTypesAreNonnullByDefault
final class SingletonImmutableSet<E> extends ImmutableSet<E> {

  // This reference is used both by the custom field serializer, and by the
  // GWT compiler to infer the elements of the lists that needs to be
  // serialized.
  //
  // Although this reference is non-final, it doesn't change after set creation.
  E element;

  SingletonImmutableSet(E element) {
    this.element = checkNotNull(element);
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public UnmodifiableIterator<E> iterator() {
    return Iterators.singletonIterator(element);
  }

  @Override
  public boolean contains(@Nullable Object object) {
    return element.equals(object);
  }
}

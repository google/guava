/*
 * Copyright (C) 2014 The Guava Authors
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

package com.google.common.graph;

import com.google.common.collect.Iterators;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.CheckReturnValue;

/**
 * An unmodifiable view of a set which may be backed by other sets; this view
 * will change as the backing sets do.
 */
@CheckReturnValue
abstract class SetView<E> extends AbstractSet<E> {

  SetView() {}

  @Override
  public int size() {
    return elements().size();
  }

  @Override
  public boolean isEmpty() {
    return elements().isEmpty();
  }

  @Override
  public Iterator<E> iterator() {
    return Iterators.unmodifiableIterator(elements().iterator());
  }

  @Override
  public boolean contains(Object object) {
    return elements().contains(object);
  }

  abstract Set<E> elements();
}

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

import com.google.common.annotations.GwtCompatible;

import java.util.NoSuchElementException;

/**
 * This class provides a skeletal implementation of an iterator across a fixed
 * number of elements that may be retrieved by position.
 *
 * @author Jared Levy
 */
@GwtCompatible
abstract class AbstractIndexedIterator<E> extends UnmodifiableIterator<E> {

  // TODO: Make public? If public, constructor should verify that size >= 0.

  private final int size;
  private int position;

  /**
   * Returns the element with the specified index. This method is called by
   * {@link #next()}.
   */
  protected abstract E get(int index);

  // TODO: Add constructor taking an offset.

  protected AbstractIndexedIterator(int size) {
    this.size = size;
  }

  public final boolean hasNext() {
    return position < size;
  }

  public final E next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return get(position++);
  }
}

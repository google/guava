/*
 * Copyright (C) 2018 The Guava Authors
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
import com.google.common.annotations.GwtIncompatible;
import org.checkerframework.checker.nullness.qual.Nullable;

@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
abstract class IndexedImmutableSet<E> extends ImmutableSet<E> {
  abstract E get(int index);

  @Override
  public UnmodifiableIterator<E> iterator() {
    return asList().iterator();
  }

  @Override
  @GwtIncompatible
  int copyIntoArray(@Nullable Object[] dst, int offset) {
    return asList().copyIntoArray(dst, offset);
  }

  @Override
  ImmutableList<E> createAsList() {
    return new ImmutableList<E>() {
      @Override
      public E get(int index) {
        return IndexedImmutableSet.this.get(index);
      }

      @Override
      boolean isPartialView() {
        return IndexedImmutableSet.this.isPartialView();
      }

      @Override
      public int size() {
        return IndexedImmutableSet.this.size();
      }
    };
  }
}

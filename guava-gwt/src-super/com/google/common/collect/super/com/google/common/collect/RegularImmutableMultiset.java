/*
 * Copyright (C) 2018 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.common.collect;

import java.util.Collection;

/** Never actually created; instead delegates to JdkBackedImmutableMultiset. */
class RegularImmutableMultiset<E> extends ImmutableMultiset<E> {
  static final ImmutableMultiset<Object> EMPTY =
      JdkBackedImmutableMultiset.create(ImmutableList.of());

  RegularImmutableMultiset() {}

  static <E> ImmutableMultiset<E> create(Collection<? extends Entry<? extends E>> entries) {
    if (entries.isEmpty()) {
      return ImmutableMultiset.of();
    } else {
      return JdkBackedImmutableMultiset.create(entries);
    }
  }

  @Override
  public int count(Object element) {
    throw new AssertionError();
  }

  @Override
  public ImmutableSet<E> elementSet() {
    throw new AssertionError();
  }

  @Override
  Entry<E> getEntry(int index) {
    throw new AssertionError();
  }

  @Override
  boolean isPartialView() {
    throw new AssertionError();
  }

  @Override
  public int size() {
    throw new AssertionError();
  }
}

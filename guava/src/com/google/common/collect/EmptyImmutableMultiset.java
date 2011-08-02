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
  public ImmutableSet<Object> elementSet() {
    return ImmutableSet.of();
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  UnmodifiableIterator<Entry<Object>> entryIterator() {
    return Iterators.emptyIterator();
  }

  @Override
  int distinctElements() {
    return 0;
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  ImmutableSet<Entry<Object>> createEntrySet() {
    return ImmutableSet.of();
  }

  private static final long serialVersionUID = 0;
}

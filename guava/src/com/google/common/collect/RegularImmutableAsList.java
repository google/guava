/*
 * Copyright (C) 2012 The Guava Authors
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
import com.google.common.annotations.J2ktIncompatible;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * An {@link ImmutableAsList} implementation specialized for when the delegate collection is already
 * backed by an {@code ImmutableList} or array.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("serial") // uses writeReplace, not default serialization
class RegularImmutableAsList<E> extends ImmutableAsList<E> {
  private final ImmutableCollection<E> delegate;
  private final ImmutableList<? extends E> delegateList;

  RegularImmutableAsList(ImmutableCollection<E> delegate, ImmutableList<? extends E> delegateList) {
    this.delegate = delegate;
    this.delegateList = delegateList;
  }

  RegularImmutableAsList(ImmutableCollection<E> delegate, Object[] array) {
    this(delegate, ImmutableList.<E>asImmutableList(array));
  }

  @Override
  ImmutableCollection<E> delegateCollection() {
    return delegate;
  }

  ImmutableList<? extends E> delegateList() {
    return delegateList;
  }

  @SuppressWarnings("unchecked") // safe covariant cast!
  @Override
  public UnmodifiableListIterator<E> listIterator(int index) {
    return (UnmodifiableListIterator<E>) delegateList.listIterator(index);
  }

  @GwtIncompatible // not present in emulated superclass
  @Override
  public void forEach(Consumer<? super E> action) {
    delegateList.forEach(action);
  }

  @GwtIncompatible // not present in emulated superclass
  @Override
  int copyIntoArray(@Nullable Object[] dst, int offset) {
    return delegateList.copyIntoArray(dst, offset);
  }

  @Override
  Object @Nullable [] internalArray() {
    return delegateList.internalArray();
  }

  @Override
  int internalArrayStart() {
    return delegateList.internalArrayStart();
  }

  @Override
  int internalArrayEnd() {
    return delegateList.internalArrayEnd();
  }

  @Override
  public E get(int index) {
    return delegateList.get(index);
  }

  // redeclare to help optimizers with b/310253115
  @SuppressWarnings("RedundantOverride")
  @Override
  @J2ktIncompatible // serialization
  @GwtIncompatible // serialization
  Object writeReplace() {
    return super.writeReplace();
  }
}

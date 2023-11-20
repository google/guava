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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import java.util.Set;
import javax.annotation.CheckForNull;

/**
 * ImmutableSet implementation backed by a JDK HashSet, used to defend against apparent hash
 * flooding. This implementation is never used on the GWT client side, but it must be present there
 * for serialization to work.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true)
@ElementTypesAreNonnullByDefault
final class JdkBackedImmutableSet<E> extends IndexedImmutableSet<E> {
  private final Set<?> delegate;
  private final ImmutableList<E> delegateList;

  JdkBackedImmutableSet(Set<?> delegate, ImmutableList<E> delegateList) {
    this.delegate = delegate;
    this.delegateList = delegateList;
  }

  @Override
  E get(int index) {
    return delegateList.get(index);
  }

  @Override
  public boolean contains(@CheckForNull Object object) {
    return delegate.contains(object);
  }

  @Override
  boolean isPartialView() {
    return false;
  }

  @Override
  public int size() {
    return delegateList.size();
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

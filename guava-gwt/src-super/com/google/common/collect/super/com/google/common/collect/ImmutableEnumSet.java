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

import java.util.Set;

/**
 * GWT emulation of {@link ImmutableEnumSet}. The type parameter is not bounded by {@code Enum<E>}
 * to avoid code-size bloat.
 *
 * @author Hayward Chan
 */
@ElementTypesAreNonnullByDefault
final class ImmutableEnumSet<E> extends ForwardingImmutableSet<E> {
  static <E> ImmutableSet<E> asImmutable(Set<E> delegate) {
    switch (delegate.size()) {
      case 0:
        return ImmutableSet.of();
      case 1:
        return ImmutableSet.of(Iterables.getOnlyElement(delegate));
      default:
        return new ImmutableEnumSet<E>(delegate);
    }
  }

  public ImmutableEnumSet(Set<E> delegate) {
    super(delegate);
  }
}

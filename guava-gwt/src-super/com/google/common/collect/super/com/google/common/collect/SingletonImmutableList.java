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
import static java.util.Collections.singletonList;

import java.util.List;

/**
 * GWT emulated version of {@link SingletonImmutableList}.
 *
 * @author Hayward Chan
 */
final class SingletonImmutableList<E> extends ForwardingImmutableList<E> {

  final transient List<E> delegate;
  // This reference is used both by the custom field serializer, and by the
  // GWT compiler to infer the elements of the lists that needs to be
  // serialized.
  E element;

  SingletonImmutableList(E element) {
    this.delegate = singletonList(checkNotNull(element));
    this.element = element;
  }

  @Override
  List<E> delegateList() {
    return delegate;
  }
}

/*
 * Copyright (C) 2008 The Guava Authors
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

/**
 * Bimap with no mappings.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("serial") // uses writeReplace(), not default serialization
final class EmptyImmutableBiMap extends ImmutableBiMap<Object, Object> {
  static final EmptyImmutableBiMap INSTANCE = new EmptyImmutableBiMap();

  private EmptyImmutableBiMap() {}

  @Override ImmutableMap<Object, Object> delegate() {
    return ImmutableMap.of();
  }
  @Override public ImmutableBiMap<Object, Object> inverse() {
    return this;
  }
  @Override boolean isPartialView() {
    return false;
  }
  Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}

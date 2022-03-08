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
import com.google.common.collect.testing.SampleElements.Colliders;
import java.util.Map;

@GwtCompatible
public class ImmutableMapWithBadHashesMapInterfaceTest
    extends AbstractImmutableMapMapInterfaceTest<Object, Integer> {
  @Override
  protected Map<Object, Integer> makeEmptyMap() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Map<Object, Integer> makePopulatedMap() {
    Colliders colliders = new Colliders();
    return ImmutableMap.of(
        colliders.e0(), 0,
        colliders.e1(), 1,
        colliders.e2(), 2,
        colliders.e3(), 3);
  }

  @Override
  protected Object getKeyNotInPopulatedMap() {
    return new Colliders().e4();
  }

  @Override
  protected Integer getValueNotInPopulatedMap() {
    return 4;
  }
}

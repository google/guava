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

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.SampleElements.Unhashables;
import com.google.common.collect.testing.UnhashableObject;
import java.util.Map;

@GwtIncompatible // GWT's ImmutableMap emulation is backed by java.util.HashMap.
public class RegularImmutableMapWithUnhashableValuesMapInterfaceTest
    extends AbstractImmutableMapMapInterfaceTest<Integer, UnhashableObject> {
  @Override
  protected Map<Integer, UnhashableObject> makeEmptyMap() {
    return ImmutableMap.of();
  }

  @Override
  protected Map<Integer, UnhashableObject> makePopulatedMap() {
    Unhashables unhashables = new Unhashables();
    return ImmutableMap.of(0, unhashables.e0(), 1, unhashables.e1(), 2, unhashables.e2());
  }

  @Override
  protected Integer getKeyNotInPopulatedMap() {
    return 3;
  }

  @Override
  protected UnhashableObject getValueNotInPopulatedMap() {
    return new Unhashables().e3();
  }
}

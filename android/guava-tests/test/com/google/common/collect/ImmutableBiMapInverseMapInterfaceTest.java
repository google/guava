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
import java.util.Map;

@GwtCompatible
public class ImmutableBiMapInverseMapInterfaceTest
    extends AbstractImmutableBiMapMapInterfaceTest<String, Integer> {
  @Override
  protected Map<String, Integer> makeEmptyMap() {
    return ImmutableBiMap.of();
  }

  @Override
  protected Map<String, Integer> makePopulatedMap() {
    return ImmutableBiMap.of(1, "one", 2, "two", 3, "three").inverse();
  }

  @Override
  protected String getKeyNotInPopulatedMap() {
    return "minus one";
  }

  @Override
  protected Integer getValueNotInPopulatedMap() {
    return -1;
  }
}

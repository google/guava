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

import com.google.common.annotations.GwtCompatible;
import java.util.SortedMap;

@GwtCompatible
public class ImmutableSortedMapTailMapMapInterfaceTest
    extends AbstractImmutableSortedMapMapInterfaceTest<String, Integer> {
  @Override
  protected SortedMap<String, Integer> makePopulatedMap() {
    return ImmutableSortedMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5).tailMap("b");
  }

  @Override
  protected String getKeyNotInPopulatedMap() {
    return "a";
  }

  @Override
  protected Integer getValueNotInPopulatedMap() {
    return 1;
  }
}

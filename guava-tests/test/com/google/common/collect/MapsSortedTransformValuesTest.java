/*
 * Copyright (C) 2011 The Guava Authors
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
import com.google.common.base.Function;
import com.google.common.base.Functions;
import java.util.SortedMap;

/**
 * Tests for {@link Maps#transformValues(SortedMap, Function)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MapsSortedTransformValuesTest extends AbstractMapsTransformValuesTest {
  @Override
  protected SortedMap<String, String> makeEmptyMap() {
    return Maps.transformValues(Maps.<String, String>newTreeMap(), Functions.<String>identity());
  }

  @Override
  protected SortedMap<String, String> makePopulatedMap() {
    SortedMap<String, Integer> underlying = Maps.newTreeMap();
    underlying.put("a", 1);
    underlying.put("b", 2);
    underlying.put("c", 3);
    return Maps.transformValues(underlying, Functions.toStringFunction());
  }
}

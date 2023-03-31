/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.SerializableTester;
import java.util.NavigableMap;
import java.util.SortedMap;

@GwtIncompatible // SerializableTester
public class ReserializedSafeTreeMapMapInterfaceTest
    extends SortedMapInterfaceTest<String, Integer> {
  public ReserializedSafeTreeMapMapInterfaceTest() {
    super(false, true, true, true, true);
  }

  @Override
  protected SortedMap<String, Integer> makePopulatedMap() {
    NavigableMap<String, Integer> map = new SafeTreeMap<>();
    map.put("one", 1);
    map.put("two", 2);
    map.put("three", 3);
    return SerializableTester.reserialize(map);
  }

  @Override
  protected SortedMap<String, Integer> makeEmptyMap() throws UnsupportedOperationException {
    NavigableMap<String, Integer> map = new SafeTreeMap<>();
    return SerializableTester.reserialize(map);
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

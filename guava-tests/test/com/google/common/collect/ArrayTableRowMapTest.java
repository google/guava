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
import com.google.common.collect.TableCollectionTest.RowMapTests;
import java.util.Arrays;
import java.util.Map;

@GwtIncompatible // TODO(hhchan): ArrayTable
public class ArrayTableRowMapTest extends RowMapTests {
  public ArrayTableRowMapTest() {
    super(true, false, false, false);
  }

  @Override
  Table<String, Integer, Character> makeTable() {
    return ArrayTable.create(Arrays.asList("foo", "bar", "dog"), Arrays.asList(1, 2, 3));
  }

  @Override
  protected Map<String, Map<Integer, Character>> makeEmptyMap() {
    throw new UnsupportedOperationException();
  }
}

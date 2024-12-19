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

import static java.util.Arrays.asList;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.TableCollectionTest.RowTests;
import java.util.Map;
import org.jspecify.annotations.NullUnmarked;

@GwtIncompatible // TODO(hhchan): ArrayTable
@NullUnmarked
public class ArrayTableRowTest extends RowTests {
  public ArrayTableRowTest() {
    super(true, true, false, false, false);
  }

  @Override
  protected String getKeyNotInPopulatedMap() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Map<String, Integer> makeEmptyMap() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Table<Character, String, Integer> makeTable() {
    return ArrayTable.create(asList('a', 'b', 'c'), asList("one", "two", "three", "four"));
  }
}

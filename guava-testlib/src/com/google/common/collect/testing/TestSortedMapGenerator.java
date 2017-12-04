/*
 * Copyright (C) 2012 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import java.util.Map.Entry;
import java.util.SortedMap;

/**
 * Creates sorted maps, containing sample elements, to be tested.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public interface TestSortedMapGenerator<K, V> extends TestMapGenerator<K, V> {
  @Override
  SortedMap<K, V> create(Object... elements);

  /**
   * Returns an entry with a key less than the keys of the {@link #samples()} and less than the key
   * of {@link #belowSamplesGreater()}.
   */
  Entry<K, V> belowSamplesLesser();

  /**
   * Returns an entry with a key less than the keys of the {@link #samples()} but greater than the
   * key of {@link #belowSamplesLesser()}.
   */
  Entry<K, V> belowSamplesGreater();

  /**
   * Returns an entry with a key greater than the keys of the {@link #samples()} but less than the
   * key of {@link #aboveSamplesGreater()}.
   */
  Entry<K, V> aboveSamplesLesser();

  /**
   * Returns an entry with a key greater than the keys of the {@link #samples()} and greater than
   * the key of {@link #aboveSamplesLesser()}.
   */
  Entry<K, V> aboveSamplesGreater();
}

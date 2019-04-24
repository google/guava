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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Functions;
import java.util.Collection;
import java.util.Map;

/**
 * Tests for Multimaps.transformValues().asMap().
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultimapsTransformValuesAsMapTest extends AbstractMultimapAsMapImplementsMapTest {

  public MultimapsTransformValuesAsMapTest() {
    super(true, true, true);
  }

  @Override
  protected Map<String, Collection<Integer>> makeEmptyMap() {
    return Multimaps.transformValues(
            ArrayListMultimap.<String, Integer>create(), Functions.<Integer>identity())
        .asMap();
  }

  @Override
  protected Map<String, Collection<Integer>> makePopulatedMap() {
    ListMultimap<String, Integer> delegate = ArrayListMultimap.create();
    populate(delegate);
    return Multimaps.transformValues(delegate, Functions.<Integer>identity()).asMap();
  }
}

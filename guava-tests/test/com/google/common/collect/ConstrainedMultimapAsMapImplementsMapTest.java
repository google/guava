/*
 * Copyright (C) 2007 The Guava Authors
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
import com.google.common.collect.testing.MapInterfaceTest;

import java.util.Collection;
import java.util.Map;

/**
 * Test {@link Multimap#asMap()} for a constrained multimap with
 * {@link MapInterfaceTest}.
 *
 * @author George van den Driessche
 */
@GwtCompatible
public class ConstrainedMultimapAsMapImplementsMapTest
    extends AbstractMultimapAsMapImplementsMapTest {

  public ConstrainedMultimapAsMapImplementsMapTest() {
    super(true, true, true);
  }

  @Override protected Map<String, Collection<Integer>> makeEmptyMap() {
    return MapConstraints.constrainedMultimap(
        ArrayListMultimap.<String, Integer>create(),
        MapConstraintsTest.TEST_CONSTRAINT)
        .asMap();
  }

  @Override protected Map<String, Collection<Integer>> makePopulatedMap() {
    Multimap<String, Integer> delegate = ArrayListMultimap.create();
    populate(delegate);
    return MapConstraints.constrainedMultimap(
            delegate, MapConstraintsTest.TEST_CONSTRAINT)
        .asMap();
  }
}

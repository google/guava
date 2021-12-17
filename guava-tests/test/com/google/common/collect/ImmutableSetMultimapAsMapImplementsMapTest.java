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
import com.google.common.collect.testing.MapInterfaceTest;
import java.util.Collection;
import java.util.Map;

/**
 * Test {@link Multimap#asMap()} for an {@link ImmutableSetMultimap} with {@link MapInterfaceTest}.
 *
 * @author Mike Ward
 */
@GwtCompatible
public class ImmutableSetMultimapAsMapImplementsMapTest
    extends AbstractMultimapAsMapImplementsMapTest {

  public ImmutableSetMultimapAsMapImplementsMapTest() {
    super(false, false, false);
  }

  @Override
  protected Map<String, Collection<Integer>> makeEmptyMap() {
    return ImmutableSetMultimap.<String, Integer>of().asMap();
  }

  @Override
  protected Map<String, Collection<Integer>> makePopulatedMap() {
    Multimap<String, Integer> delegate = HashMultimap.create();
    populate(delegate);
    return ImmutableSetMultimap.copyOf(delegate).asMap();
  }
}

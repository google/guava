/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.testing.features.MapFeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Testers for {@link SetMultimap#asMap}.
 * 
 * @author Louis Wasserman
 * @param <K> The key type of the tested multimap.
 * @param <V> The value type of the tested multimap.
 */
@GwtCompatible
public class SetMultimapAsMapTester<K, V> extends AbstractMultimapTester<K, V, SetMultimap<K, V>> {
  public void testAsMapValuesImplementSet() {
    for (Collection<V> valueCollection : multimap().asMap().values()) {
      assertTrue(valueCollection instanceof Set);
    }
  }
  
  public void testAsMapGetImplementsSet() {
    for (K key : multimap().keySet()) {
      assertTrue(multimap().asMap().get(key) instanceof Set);
    }
  }
  
  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testAsMapRemoveImplementsSet() {
    List<K> keys = new ArrayList<K>(multimap().keySet());
    for (K key : keys) {
      resetCollection();
      assertTrue(multimap().asMap().remove(key) instanceof Set);
    }
  }
}

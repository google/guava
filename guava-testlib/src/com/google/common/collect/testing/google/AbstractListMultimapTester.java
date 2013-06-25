/*
 * Copyright (C) 2012 The Guava Authors
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

import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ListMultimap;

import java.util.Arrays;
import java.util.Collection;

/**
 * Superclass for all {@code ListMultimap} testers.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class AbstractListMultimapTester<K, V>
    extends AbstractMultimapTester<K, V, ListMultimap<K, V>> {

  protected void assertGet(K key, V... values) {
    assertGet(key, Arrays.asList(values));
  }

  protected void assertGet(K key, Collection<V> values) {
    ASSERT.that(multimap().get(key)).has().exactlyAs(values).inOrder();

    if (!values.isEmpty()) {
      ASSERT.that(multimap().asMap().get(key)).has().exactlyAs(values).inOrder();
      assertFalse(multimap().isEmpty());
    } else {
      ASSERT.that(multimap().asMap().get(key)).isNull();
    }

    assertEquals(values.size(), multimap().get(key).size());
    assertEquals(values.size() > 0, multimap().containsKey(key));
    assertEquals(values.size() > 0, multimap().keySet().contains(key));
    assertEquals(values.size() > 0, multimap().keys().contains(key));
  }
}


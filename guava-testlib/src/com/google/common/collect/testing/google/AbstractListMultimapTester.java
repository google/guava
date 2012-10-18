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

import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ListMultimap;

/**
 * Superclass for all {@code ListMultimap} testers.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class AbstractListMultimapTester<K, V>
    extends AbstractMultimapTester<K, V, ListMultimap<K, V>> {

  protected void assertGet(K key, Object... values) {
    ASSERT.that(multimap().get(key)).hasContentsInOrder(values);

    if (values.length > 0) {
      ASSERT.that(multimap().asMap().get(key)).hasContentsInOrder(values);
      assertFalse(multimap().isEmpty());
    } else {
      ASSERT.that(multimap().asMap().get(key)).isNull();
    }

    assertEquals(values.length, multimap().get(key).size());
    assertEquals(values.length > 0, multimap().containsKey(key));
    assertEquals(values.length > 0, multimap().keySet().contains(key));
    assertEquals(values.length > 0, multimap().keys().contains(key));
  }
}


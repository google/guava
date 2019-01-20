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

import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ITERATOR_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEY_QUERIES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Iterator;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * Tester for {@code Multimap.keySet}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultimapKeySetTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  public void testKeySet() {
    for (Entry<K, V> entry : getSampleElements()) {
      assertTrue(multimap().keySet().contains(entry.getKey()));
    }
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testKeySetContainsNullKeyPresent() {
    initMultimapWithNullKey();
    assertTrue(multimap().keySet().contains(null));
  }

  @MapFeature.Require(ALLOWS_NULL_KEY_QUERIES)
  public void testKeySetContainsNullKeyAbsent() {
    assertFalse(multimap().keySet().contains(null));
  }

  @MapFeature.Require(SUPPORTS_REMOVE)
  public void testKeySetRemovePropagatesToMultimap() {
    int key0Count = multimap().get(k0()).size();
    assertEquals(key0Count > 0, multimap().keySet().remove(k0()));
    assertEquals(getNumElements() - key0Count, multimap().size());
    assertGet(k0());
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(SUPPORTS_ITERATOR_REMOVE)
  public void testKeySetIteratorRemove() {
    int key0Count = multimap().get(k0()).size();
    Iterator<K> keyItr = multimap().keySet().iterator();
    while (keyItr.hasNext()) {
      if (keyItr.next().equals(k0())) {
        keyItr.remove();
      }
    }
    assertEquals(getNumElements() - key0Count, multimap().size());
    assertGet(k0());
  }
}

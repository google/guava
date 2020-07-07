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

import static com.google.common.collect.testing.Helpers.copyToList;
import static com.google.common.collect.testing.Helpers.copyToSet;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Ignore;

/**
 * Tests for {@link SetMultimap#replaceValues}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class SetMultimapPutTester<K, V> extends AbstractMultimapTester<K, V, SetMultimap<K, V>> {
  // Tests for non-duplicate values are in MultimapPutTester

  @MapFeature.Require(SUPPORTS_PUT)
  @CollectionSize.Require(absent = ZERO)
  public void testPutDuplicateValuePreservesSize() {
    assertFalse(multimap().put(k0(), v0()));
    assertEquals(getNumElements(), multimap().size());
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutDuplicateValue() {
    List<Entry<K, V>> entries = copyToList(multimap().entries());

    for (Entry<K, V> entry : entries) {
      resetContainer();
      K k = entry.getKey();
      V v = entry.getValue();

      Set<V> values = multimap().get(k);
      Set<V> expectedValues = copyToSet(values);

      assertFalse(multimap().put(k, v));
      assertEquals(expectedValues, values);
      assertGet(k, expectedValues);
    }
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testPutDuplicateValue_null() {
    initMultimapWithNullValue();
    assertFalse(multimap().put(getKeyForNullValue(), null));
    expectContents(createArrayWithNullValue());
  }
}

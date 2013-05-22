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

import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

/**
 * Tester for {@code Multimap.toString()}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultimapToStringTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @CollectionSize.Require(ZERO)
  public void testToStringEmpty() {
    assertEquals("{}", multimap().toString());
  }

  @CollectionSize.Require(ONE)
  public void testToStringSingleton() {
    assertEquals("{" + sampleKeys().e0 + "=[" + sampleValues().e0 + "]}", multimap().toString());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testToStringWithNullKey() {
    initMultimapWithNullKey();
    testToStringMatchesAsMap();
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(ALLOWS_NULL_VALUES)
  public void testToStringWithNullValue() {
    initMultimapWithNullValue();
    testToStringMatchesAsMap();
  }

  public void testToStringMatchesAsMap() {
    assertEquals(multimap().asMap().toString(), multimap().toString());
  }
}

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

import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_REMOVE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.testing.features.MapFeature;

import java.util.Arrays;
import java.util.List;

/**
 * Testers for {@link ListMultimap#replaceValues(Object, Iterable)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class ListMultimapReplaceValuesTester<K, V> extends AbstractListMultimapTester<K, V> {
  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testReplaceValuesPreservesOrder() {
    @SuppressWarnings("unchecked")
    List<V> values = Arrays.asList(
        sampleValues().e3,
        sampleValues().e1,
        sampleValues().e4);

    for (K k : sampleKeys()) {
      resetContainer();
      multimap().replaceValues(k, values);
      assertGet(k, values);
    }
  }
}

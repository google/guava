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

import static com.google.common.collect.testing.Helpers.copyToSet;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.Ignore;

/**
 * Tests for {@link SetMultimap#replaceValues}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class SetMultimapPutAllTester<K, V> extends AbstractMultimapTester<K, V, SetMultimap<K, V>> {

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllHandlesDuplicates() {
    @SuppressWarnings("unchecked")
    List<V> valuesToPut = Arrays.asList(v0(), v1(), v0());

    for (K k : sampleKeys()) {
      resetContainer();

      Set<V> expectedValues = copyToSet(multimap().get(k));

      multimap().putAll(k, valuesToPut);
      expectedValues.addAll(valuesToPut);

      assertGet(k, expectedValues);
    }
  }
}

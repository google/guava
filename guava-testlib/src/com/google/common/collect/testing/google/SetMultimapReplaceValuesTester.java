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
import com.google.common.collect.SetMultimap;
import com.google.common.collect.testing.features.MapFeature;
import java.util.Arrays;
import java.util.List;
import org.junit.Ignore;

/**
 * Tests for {@link SetMultimap#replaceValues}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class SetMultimapReplaceValuesTester<K, V>
    extends AbstractMultimapTester<K, V, SetMultimap<K, V>> {

  @MapFeature.Require({SUPPORTS_PUT, SUPPORTS_REMOVE})
  public void testReplaceValuesHandlesDuplicates() {
    @SuppressWarnings("unchecked")
    List<V> values = Arrays.asList(v0(), v1(), v0());

    for (K k : sampleKeys()) {
      resetContainer();
      multimap().replaceValues(k, values);
      assertGet(k, v0(), v1());
    }
  }
}

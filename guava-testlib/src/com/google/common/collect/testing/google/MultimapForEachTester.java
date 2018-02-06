/*
 * Copyright (C) 2012 The Guava Authors
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
package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.Helpers.assertEqualIgnoringOrder;
import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionFeature.KNOWN_ORDER;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Ignore;

/**
 * Tests for {@link Multimap#forEach}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultimapForEachTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  public void testForEach() {
    List<Entry<K, V>> entries = new ArrayList<>();
    multimap().forEach((k, v) -> entries.add(mapEntry(k, v)));
    assertEqualIgnoringOrder(getSampleElements(), multimap().entries());
  }

  @CollectionFeature.Require(KNOWN_ORDER)
  public void testForEachOrder() {
    List<Entry<K, V>> entries = new ArrayList<>();
    multimap().forEach((k, v) -> entries.add(mapEntry(k, v)));
    assertEqualIgnoringOrder(getSampleElements(), multimap().entries());
  }
}

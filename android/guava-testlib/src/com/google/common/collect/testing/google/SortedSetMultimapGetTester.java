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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.SortedSetMultimap;
import org.junit.Ignore;

/**
 * Tester for {@link SortedSetMultimap#get(Object)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class SortedSetMultimapGetTester<K, V>
    extends AbstractMultimapTester<K, V, SortedSetMultimap<K, V>> {
  public void testValueComparator() {
    assertEquals(multimap().valueComparator(), multimap().get(k0()).comparator());
  }
}

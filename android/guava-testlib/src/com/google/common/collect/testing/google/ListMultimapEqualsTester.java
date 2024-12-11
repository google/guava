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

import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.EqualsTester;
import org.junit.Ignore;

/**
 * Testers for {@link ListMultimap#equals(Object)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class ListMultimapEqualsTester<K, V> extends AbstractListMultimapTester<K, V> {
  @CollectionSize.Require(SEVERAL)
  public void testOrderingAffectsEqualsComparisons() {
    ListMultimap<K, V> multimap1 =
        getSubjectGenerator()
            .create(mapEntry(k0(), v0()), mapEntry(k0(), v1()), mapEntry(k0(), v0()));
    ListMultimap<K, V> multimap2 =
        getSubjectGenerator()
            .create(mapEntry(k0(), v1()), mapEntry(k0(), v0()), mapEntry(k0(), v0()));
    new EqualsTester().addEqualityGroup(multimap1).addEqualityGroup(multimap2).testEquals();
  }
}

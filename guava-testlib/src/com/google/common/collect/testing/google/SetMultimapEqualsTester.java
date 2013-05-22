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

import static com.google.common.collect.testing.features.CollectionSize.SEVERAL;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.EqualsTester;

/**
 * Testers for {@link SetMultimap#equals(Object)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class SetMultimapEqualsTester<K, V> 
    extends AbstractMultimapTester<K, V, SetMultimap<K, V>> {
  @CollectionSize.Require(SEVERAL)
  public void testOrderingDoesntAffectEqualsComparisons() {
    SetMultimap<K, V> multimap1 = getSubjectGenerator().create(
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e0),
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e1),
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e4));
    SetMultimap<K, V> multimap2 = getSubjectGenerator().create(
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e1),
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e0),
        Helpers.mapEntry(sampleKeys().e0, sampleValues().e4));
    new EqualsTester()
        .addEqualityGroup(multimap1, multimap2)
        .testEquals();
  }
}

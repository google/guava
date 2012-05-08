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

import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;

import java.util.Collections;
import java.util.Iterator;

/**
 * Tests for {@link Multimap#putAll(Object, Iterable)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class MultimapPutIterableTester<K, V> extends AbstractMultimapTester<K, V> {
  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllNonEmptyOnPresentKey() {
    multimap().putAll(sampleKeys().e0, new Iterable<V>() {
      @Override
      public Iterator<V> iterator() {
        return Lists.newArrayList(sampleValues().e3, sampleValues().e4).iterator();
      }
    });
    assertGet(sampleKeys().e0, sampleValues().e0, sampleValues().e3, sampleValues().e4);
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllNonEmptyOnAbsentKey() {
    multimap().putAll(sampleKeys().e3, new Iterable<V>() {
      @Override
      public Iterator<V> iterator() {
        return Lists.newArrayList(sampleValues().e3, sampleValues().e4).iterator();
      }
    });
    assertGet(sampleKeys().e3, sampleValues().e3, sampleValues().e4);
  }

  private static final Object[] EMPTY = new Object[0];

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllEmptyIterableOnAbsentKey() {
    Iterable<V> iterable = Collections.emptyList();

    multimap().putAll(sampleKeys().e3, iterable);
    expectUnchanged();
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllEmptyIterableOnPresentKey() {
    multimap().putAll(sampleKeys().e0, Collections.<V>emptyList());
    expectUnchanged();
  }
}

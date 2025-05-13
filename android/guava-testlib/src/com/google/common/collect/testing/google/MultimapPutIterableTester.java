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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.testing.Helpers.assertContainsAllOf;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.MapFeature.SUPPORTS_PUT;
import static com.google.common.collect.testing.google.ReflectionFreeAssertThrows.assertThrows;
import static java.util.Collections.singletonList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import org.junit.Ignore;

/**
 * Tests for {@link Multimap#putAll(Object, Iterable)}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
@SuppressWarnings({
  // @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
  "JUnit4ClassUsedInJUnit3",
  // We use ::iterator so that we test passing a plain Iterable, not a Collection.
  "UnnecessaryMethodReference",
})
public class MultimapPutIterableTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllNonEmptyIterableOnPresentKey() {
    assertTrue(multimap().putAll(k0(), newArrayList(v3(), v4())::iterator));
    assertGet(k0(), v0(), v3(), v4());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllNonEmptyCollectionOnPresentKey() {
    assertTrue(multimap().putAll(k0(), newArrayList(v3(), v4())));
    assertGet(k0(), v0(), v3(), v4());
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllNonEmptyIterableOnAbsentKey() {
    assertTrue(multimap().putAll(k3(), newArrayList(v3(), v4())::iterator));
    assertGet(k3(), v3(), v4());
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllNonEmptyCollectionOnAbsentKey() {
    assertTrue(multimap().putAll(k3(), newArrayList(v3(), v4())));
    assertGet(k3(), v3(), v4());
  }

  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  public void testPutAllNullValueOnPresentKey_supported() {
    assertTrue(multimap().putAll(k0(), newArrayList(v3(), null)));
    assertGet(k0(), v0(), v3(), null);
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_VALUES})
  public void testPutAllNullValueOnAbsentKey_supported() {
    assertTrue(multimap().putAll(k3(), newArrayList(v3(), null)));
    assertGet(k3(), v3(), null);
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  public void testPutAllNullValueSingle_unsupported() {
    multimap().putAll(k1(), newArrayList((V) null));
    expectUnchanged();
  }

  // In principle, it would be nice to apply these two tests to keys with existing values, too.

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  public void testPutAllNullValueNullLast_unsupported() {
    int size = getNumElements();

    assertThrows(
        NullPointerException.class, () -> multimap().putAll(k3(), newArrayList(v3(), null)));

    Collection<V> values = multimap().get(k3());
    if (values.size() == 0) {
      expectUnchanged();
      // Be extra thorough in case internal state was corrupted by the expected null.
      assertEquals(new ArrayList<>(), new ArrayList<>(values));
      assertEquals(size, multimap().size());
    } else {
      assertEquals(newArrayList(v3()), new ArrayList<>(values));
      assertEquals(size + 1, multimap().size());
    }
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_VALUES)
  public void testPutAllNullValueNullFirst_unsupported() {
    int size = getNumElements();

    assertThrows(
        NullPointerException.class, () -> multimap().putAll(k3(), newArrayList(null, v3())));

    /*
     * In principle, a Multimap implementation could add e3 first before failing on the null. But
     * that seems unlikely enough to be worth complicating the test over, especially if there's any
     * chance that a permissive test could mask a bug.
     */
    expectUnchanged();
    // Be extra thorough in case internal state was corrupted by the expected null.
    assertEquals(new ArrayList<>(), new ArrayList<>(multimap().get(k3())));
    assertEquals(size, multimap().size());
  }

  @MapFeature.Require({SUPPORTS_PUT, ALLOWS_NULL_KEYS})
  public void testPutAllOnPresentNullKey() {
    assertTrue(multimap().putAll(null, newArrayList(v3(), v4())));
    assertGet(null, v3(), v4());
  }

  @MapFeature.Require(value = SUPPORTS_PUT, absent = ALLOWS_NULL_KEYS)
  public void testPutAllNullForbidden() {
    assertThrows(NullPointerException.class, () -> multimap().putAll(null, singletonList(v3())));
  }

  @SuppressWarnings("EmptyList") // ImmutableList doesn't support nullable element types
  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllEmptyCollectionOnAbsentKey() {
    assertFalse(multimap().putAll(k3(), Collections.<V>emptyList()));
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllEmptyIterableOnAbsentKey() {
    assertFalse(multimap().putAll(k3(), Collections::emptyIterator));
    expectUnchanged();
  }

  @SuppressWarnings("EmptyList") // ImmutableList doesn't support nullable element types
  @CollectionSize.Require(absent = ZERO)
  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllEmptyIterableOnPresentKey() {
    multimap().putAll(k0(), Collections.<V>emptyList());
    expectUnchanged();
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllOnlyCallsIteratorOnce() {
    Iterable<V> iterable =
        new Iterable<V>() {
          private boolean calledIteratorAlready = false;

          @Override
          public Iterator<V> iterator() {
            checkState(!calledIteratorAlready);
            calledIteratorAlready = true;
            return Iterators.forArray(v3());
          }
        };

    multimap().putAll(k3(), iterable);
  }

  @MapFeature.Require(SUPPORTS_PUT)
  public void testPutAllPropagatesToGet() {
    Collection<V> getCollection = multimap().get(k0());
    int getCollectionSize = getCollection.size();
    assertTrue(multimap().putAll(k0(), newArrayList(v3(), v4())));
    assertEquals(getCollectionSize + 2, getCollection.size());
    assertContainsAllOf(getCollection, v3(), v4());
  }
}

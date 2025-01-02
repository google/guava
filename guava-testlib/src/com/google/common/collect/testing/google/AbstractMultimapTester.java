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
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.AbstractContainerTester;
import com.google.common.collect.testing.SampleElements;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.Ignore;

/**
 * Superclass for all {@code Multimap} testers.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
@NullMarked
public abstract class AbstractMultimapTester<
        K extends @Nullable Object, V extends @Nullable Object, M extends Multimap<K, V>>
    extends AbstractContainerTester<M, Entry<K, V>> {

  private M multimap;

  protected M multimap() {
    return multimap;
  }

  /**
   * @return an array of the proper size with {@code null} as the key of the middle element.
   */
  protected Entry<K, V>[] createArrayWithNullKey() {
    Entry<K, V>[] array = createSamplesArray();
    int nullKeyLocation = getNullLocation();
    Entry<K, V> oldEntry = array[nullKeyLocation];
    array[nullKeyLocation] = mapEntry(null, oldEntry.getValue());
    return array;
  }

  /**
   * @return an array of the proper size with {@code null} as the value of the middle element.
   */
  protected Entry<K, V>[] createArrayWithNullValue() {
    Entry<K, V>[] array = createSamplesArray();
    int nullValueLocation = getNullLocation();
    Entry<K, V> oldEntry = array[nullValueLocation];
    array[nullValueLocation] = mapEntry(oldEntry.getKey(), null);
    return array;
  }

  /**
   * @return an array of the proper size with {@code null} as the key and value of the middle
   *     element.
   */
  protected Entry<K, V>[] createArrayWithNullKeyAndValue() {
    Entry<K, V>[] array = createSamplesArray();
    int nullValueLocation = getNullLocation();
    array[nullValueLocation] = mapEntry(null, null);
    return array;
  }

  protected V getValueForNullKey() {
    return getEntryNullReplaces().getValue();
  }

  protected K getKeyForNullValue() {
    return getEntryNullReplaces().getKey();
  }

  private Entry<K, V> getEntryNullReplaces() {
    Iterator<Entry<K, V>> entries = getSampleElements().iterator();
    for (int i = 0; i < getNullLocation(); i++) {
      entries.next();
    }
    return entries.next();
  }

  protected void initMultimapWithNullKey() {
    resetContainer(getSubjectGenerator().create((Object[]) createArrayWithNullKey()));
  }

  protected void initMultimapWithNullValue() {
    resetContainer(getSubjectGenerator().create((Object[]) createArrayWithNullValue()));
  }

  protected void initMultimapWithNullKeyAndValue() {
    resetContainer(getSubjectGenerator().create((Object[]) createArrayWithNullKeyAndValue()));
  }

  protected SampleElements<K> sampleKeys() {
    return ((TestMultimapGenerator<K, V, ? extends Multimap<K, V>>)
            getSubjectGenerator().getInnerGenerator())
        .sampleKeys();
  }

  protected SampleElements<V> sampleValues() {
    return ((TestMultimapGenerator<K, V, ? extends Multimap<K, V>>)
            getSubjectGenerator().getInnerGenerator())
        .sampleValues();
  }

  @Override
  protected Collection<Entry<K, V>> actualContents() {
    return multimap.entries();
  }

  // TODO: dispose of this once collection is encapsulated.
  @Override
  @CanIgnoreReturnValue
  protected M resetContainer(M newContents) {
    multimap = super.resetContainer(newContents);
    return multimap;
  }

  @CanIgnoreReturnValue
  protected Multimap<K, V> resetContainer(Entry<K, V>... newContents) {
    multimap = super.resetContainer(getSubjectGenerator().create((Object[]) newContents));
    return multimap;
  }

  /**
   * @see AbstractContainerTester#resetContainer()
   */
  protected void resetCollection() {
    resetContainer();
  }

  protected void assertGet(K key, V... values) {
    assertGet(key, asList(values));
  }

  protected void assertGet(K key, Collection<? extends V> values) {
    assertEqualIgnoringOrder(values, multimap().get(key));

    if (!values.isEmpty()) {
      assertEqualIgnoringOrder(values, multimap().asMap().get(key));
      assertFalse(multimap().isEmpty());
    } else {
      assertNull(multimap().asMap().get(key));
    }

    assertEquals(values.size(), multimap().get(key).size());

    assertEquals(values.size() > 0, multimap().containsKey(key));
    assertEquals(values.size() > 0, multimap().keySet().contains(key));
    assertEquals(values.size() > 0, multimap().keys().contains(key));
  }

  protected final K k0() {
    return e0().getKey();
  }

  protected final V v0() {
    return e0().getValue();
  }

  protected final K k1() {
    return e1().getKey();
  }

  protected final V v1() {
    return e1().getValue();
  }

  protected final K k2() {
    return e2().getKey();
  }

  protected final V v2() {
    return e2().getValue();
  }

  protected final K k3() {
    return e3().getKey();
  }

  protected final V v3() {
    return e3().getValue();
  }

  protected final K k4() {
    return e4().getKey();
  }

  protected final V v4() {
    return e4().getValue();
  }
}

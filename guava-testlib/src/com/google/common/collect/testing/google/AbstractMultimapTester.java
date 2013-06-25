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

import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.AbstractContainerTester;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.SampleElements;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Superclass for all {@code Multimap} testers.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public abstract class AbstractMultimapTester<K, V, M extends Multimap<K, V>>
    extends AbstractContainerTester<M, Map.Entry<K, V>> {

  private M multimap;

  protected M multimap() {
    return multimap;
  }

  /**
   * @return an array of the proper size with {@code null} as the key of the
   * middle element.
   */
  protected Map.Entry<K, V>[] createArrayWithNullKey() {
    Map.Entry<K, V>[] array = createSamplesArray();
    final int nullKeyLocation = getNullLocation();
    final Map.Entry<K, V> oldEntry = array[nullKeyLocation];
    array[nullKeyLocation] = Helpers.mapEntry(null, oldEntry.getValue());
    return array;
  }

  /**
   * @return an array of the proper size with {@code null} as the value of the
   * middle element.
   */
  protected Map.Entry<K, V>[] createArrayWithNullValue() {
    Map.Entry<K, V>[] array = createSamplesArray();
    final int nullValueLocation = getNullLocation();
    final Map.Entry<K, V> oldEntry = array[nullValueLocation];
    array[nullValueLocation] = Helpers.mapEntry(oldEntry.getKey(), null);
    return array;
  }

  /**
   * @return an array of the proper size with {@code null} as the key and value of the
   * middle element.
   */
  protected Map.Entry<K, V>[] createArrayWithNullKeyAndValue() {
    Map.Entry<K, V>[] array = createSamplesArray();
    final int nullValueLocation = getNullLocation();
    array[nullValueLocation] = Helpers.mapEntry(null, null);
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
    resetContainer(getSubjectGenerator().create(createArrayWithNullKey()));
  }

  protected void initMultimapWithNullValue() {
    resetContainer(getSubjectGenerator().create(createArrayWithNullValue()));
  }

  protected void initMultimapWithNullKeyAndValue() {
    resetContainer(getSubjectGenerator().create(createArrayWithNullKeyAndValue()));
  }

  protected SampleElements<K> sampleKeys() {
    return ((TestMultimapGenerator<K, V, ? extends Multimap<K, V>>) getSubjectGenerator()
        .getInnerGenerator()).sampleKeys();
  }

  protected SampleElements<V> sampleValues() {
    return ((TestMultimapGenerator<K, V, ? extends Multimap<K, V>>) getSubjectGenerator()
        .getInnerGenerator()).sampleValues();
  }

  @Override
  protected Collection<Entry<K, V>> actualContents() {
    return multimap.entries();
  }

  // TODO: dispose of this once collection is encapsulated.
  @Override
  protected M resetContainer(M newContents) {
    multimap = super.resetContainer(newContents);
    return multimap;
  }

  protected Multimap<K, V> resetContainer(Entry<K, V>... newContents) {
    multimap = super.resetContainer(getSubjectGenerator().create(newContents));
    return multimap;
  }

  /** @see AbstractContainerTester#resetContainer() */
  protected void resetCollection() {
    resetContainer();
  }

  protected void assertGet(K key, V... values) {
    assertGet(key, Arrays.asList(values));
  }

  protected void assertGet(K key, Collection<V> values) {
    ASSERT.that(multimap().get(key)).has().exactlyAs(values);

    if (!values.isEmpty()) {
      ASSERT.that(multimap().asMap().get(key)).has().exactlyAs(values);
      assertFalse(multimap().isEmpty());
    } else {
      ASSERT.that(multimap().asMap().get(key)).isNull();
    }

    // TODO(user): Add proper overrides to prevent autoboxing.
    // Truth+autoboxing == compile error. Cast int to long to fix:
    ASSERT.that(multimap().get(key).size()).is((long) values.size());

    assertEquals(values.size() > 0, multimap().containsKey(key));
    assertEquals(values.size() > 0, multimap().keySet().contains(key));
    assertEquals(values.size() > 0, multimap().keys().contains(key));
  }
}

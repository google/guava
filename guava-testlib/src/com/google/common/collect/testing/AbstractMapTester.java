/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Base class for map testers.
 *
 * TODO: see how much of this is actually needed once Map testers are written.
 * (It was cloned from AbstractCollectionTester.)
 *
 * @param <K> the key type of the map to be tested.
 * @param <V> the value type of the map to be tested.
 *
 * @author George van den Driessche
 */
@GwtCompatible
public abstract class AbstractMapTester<K, V> extends
    AbstractContainerTester<Map<K, V>, Map.Entry<K, V>> {
  protected Map<K, V> getMap() {
    return container;
  }

  @Override public void setUp() throws Exception {
    super.setUp();
    samples = this.getSubjectGenerator().samples();
    resetMap();
  }

  @Override protected Collection<Map.Entry<K, V>> actualContents() {
    return getMap().entrySet();
  }

  /** @see AbstractContainerTester#resetContainer() */
  protected void resetMap() {
    resetContainer();
  }

  protected void expectMissingKeys(K... elements) {
    for (K element : elements) {
      assertFalse("Should not contain key " + element,
          getMap().containsKey(element));
    }
  }

  protected void expectMissingValues(V... elements) {
    for (V element : elements) {
      assertFalse("Should not contain value " + element,
          getMap().containsValue(element));
    }
  }

  /**
   * @return an array of the proper size with {@code null} as the key of the
   * middle element.
   */
  protected Map.Entry<K, V>[] createArrayWithNullKey() {
    Map.Entry<K, V>[] array = createSamplesArray();
    final int nullKeyLocation = getNullLocation();
    final Map.Entry<K, V> oldEntry = array[nullKeyLocation];
    array[nullKeyLocation] = entry(null, oldEntry.getValue());
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

  /**
   * @return an array of the proper size with {@code null} as the value of the
   * middle element.
   */
  protected Map.Entry<K, V>[] createArrayWithNullValue() {
    Map.Entry<K, V>[] array = createSamplesArray();
    final int nullValueLocation = getNullLocation();
    final Map.Entry<K, V> oldEntry = array[nullValueLocation];
    array[nullValueLocation] = entry(oldEntry.getKey(), null);
    return array;
  }

  protected void initMapWithNullKey() {
    resetMap(createArrayWithNullKey());
  }

  protected void initMapWithNullValue() {
    resetMap(createArrayWithNullValue());
  }

  /**
   * Equivalent to {@link #expectMissingKeys(Object[]) expectMissingKeys}
   * {@code (null)}
   * except that the call to {@code contains(null)} is permitted to throw a
   * {@code NullPointerException}.
   * @param message message to use upon assertion failure
   */
  protected void expectNullKeyMissingWhenNullKeysUnsupported(String message) {
    try {
      assertFalse(message, getMap().containsKey(null));
    } catch (NullPointerException tolerated) {
      // Tolerated
    }
  }

  /**
   * Equivalent to {@link #expectMissingValues(Object[]) expectMissingValues}
   * {@code (null)}
   * except that the call to {@code contains(null)} is permitted to throw a
   * {@code NullPointerException}.
   * @param message message to use upon assertion failure
   */
  protected void expectNullValueMissingWhenNullValuesUnsupported(
      String message) {
    try {
      assertFalse(message, getMap().containsValue(null));
    } catch (NullPointerException tolerated) {
      // Tolerated
    }
  }

  @SuppressWarnings("unchecked")
  @Override protected MinimalCollection<Map.Entry<K, V>>
      createDisjointCollection() {
    return MinimalCollection.of(samples.e3, samples.e4);
  }

  protected int getNumEntries() {
    return getNumElements();
  }

  protected Collection<Map.Entry<K, V>> getSampleEntries(int howMany) {
    return getSampleElements(howMany);
  }

  protected Collection<Map.Entry<K, V>> getSampleEntries() {
    return getSampleElements();
  }

  @Override protected void expectMissing(Entry<K, V>... entries) {
    for (Entry<K, V> entry : entries) {
      assertFalse("Should not contain entry " + entry,
          actualContents().contains(entry));
      assertFalse("Should not contain key " + entry.getKey() + " mapped to"
          + " value " + entry.getValue(),
          equal(getMap().get(entry.getKey()), entry.getValue()));
    }
  }

  private static boolean equal(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  // This one-liner saves us from some ugly casts
  protected Entry<K, V> entry(K key, V value) {
    return Helpers.mapEntry(key, value);
  }

  @Override protected void expectContents(Collection<Entry<K, V>> expected) {
    // TODO: move this to invariant checks once the appropriate hook exists?
    super.expectContents(expected);
    for (Entry<K, V> entry : expected) {
      assertEquals("Wrong value for key " + entry.getKey(),
          entry.getValue(), getMap().get(entry.getKey()));
    }
  }

  protected final void expectReplacement(Entry<K, V> newEntry) {
    List<Entry<K, V>> expected = Helpers.copyToList(getSampleElements());
    replaceValue(expected, newEntry);
    expectContents(expected);
  }

  private void replaceValue(List<Entry<K, V>> expected, Entry<K, V> newEntry) {
    for (ListIterator<Entry<K, V>> i = expected.listIterator(); i.hasNext();) {
      if (Helpers.equal(i.next().getKey(), newEntry.getKey())) {
        i.set(newEntry);
        return;
      }
    }

    throw new IllegalArgumentException(Platform.format(
        "key %s not found in entries %s", newEntry.getKey(), expected));
  }

  /**
   * Wrapper for {@link Map#get(Object)} that forces the caller to pass in a key
   * of the same type as the map. Besides being slightly shorter than code that
   * uses {@link #getMap()}, it also ensures that callers don't pass an
   * {@link Entry} by mistake.
   */
  protected V get(K key) {
    return getMap().get(key);
  }

  protected void resetMap(Entry<K, V>[] entries) {
    resetContainer(getSubjectGenerator().create((Object[]) entries));
  }
}

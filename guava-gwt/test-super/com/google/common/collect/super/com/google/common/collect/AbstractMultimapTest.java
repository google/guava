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

package com.google.common.collect;

import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Tests for {@code Multimap} implementations. Caution: when subclassing avoid
 * accidental naming collisions with tests in this class!
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public abstract class AbstractMultimapTest extends TestCase {

  private Multimap<String, Integer> multimap;

  protected abstract Multimap<String, Integer> create();

  protected Multimap<String, Integer> createSample() {
    Multimap<String, Integer> sample = create();
    sample.putAll("foo", asList(3, -1, 2, 4, 1));
    sample.putAll("bar", asList(1, 2, 3, 1));
    return sample;
  }

  // public for GWT
  @Override public void setUp() throws Exception {
    super.setUp();
    multimap = create();
  }

  protected Multimap<String, Integer> getMultimap() {
    return multimap;
  }

  /**
   * Returns the key to use as a null placeholder in tests. The default
   * implementation returns {@code null}, but tests for multimaps that don't
   * support null keys should override it.
   */
  protected String nullKey() {
    return null;
  }

  /**
   * Returns the value to use as a null placeholder in tests. The default
   * implementation returns {@code null}, but tests for multimaps that don't
   * support null values should override it.
   */
  protected Integer nullValue() {
    return null;
  }

  /**
   * Validate multimap size by calling {@code size()} and also by iterating
   * through the entries. This tests cases where the {@code entries()} list is
   * stored separately, such as the {@link LinkedHashMultimap}. It also
   * verifies that the multimap contains every multimap entry.
   */
  protected void assertSize(int expectedSize) {
    assertEquals(expectedSize, multimap.size());

    int size = 0;
    for (Entry<String, Integer> entry : multimap.entries()) {
      assertTrue(multimap.containsEntry(entry.getKey(), entry.getValue()));
      size++;
    }
    assertEquals(expectedSize, size);

    int size2 = 0;
    for (Entry<String, Collection<Integer>> entry2 :
        multimap.asMap().entrySet()) {
      size2 += entry2.getValue().size();
    }
    assertEquals(expectedSize, size2);
  }

  protected boolean removedCollectionsAreModifiable() {
    return false;
  }

  public void testValues() {
    multimap.put("foo", 1);
    multimap.put("foo", nullValue());
    multimap.put(nullKey(), 3);
    Collection<Integer> values = multimap.values();
    assertEquals(3, values.size());
    assertTrue(values.contains(1));
    assertTrue(values.contains(3));
    assertTrue(values.contains(nullValue()));
    assertFalse(values.contains(5));
  }

  public void testValuesIteratorRemove() {
    multimap.put("foo", 1);
    multimap.put("foo", 2);
    multimap.put(nullKey(), 4);

    Iterator<Integer> iterator = multimap.values().iterator();
    while (iterator.hasNext()) {
      int value = iterator.next();
      if ((value % 2) == 0) {
        iterator.remove();
      }
    }

    assertSize(1);
    assertTrue(multimap.containsEntry("foo", 1));
  }
}


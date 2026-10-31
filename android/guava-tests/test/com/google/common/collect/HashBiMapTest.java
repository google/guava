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

import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@link HashBiMap}.
 *
 * @author Mike Bostock
 */
@GwtCompatible
@NullMarked
public class HashBiMapTest extends TestCase {

  @J2ktIncompatible
  @AndroidIncompatible // test-suite builders
  public static final class HashBiMapGenerator extends TestStringBiMapGenerator {
    @Override
    protected BiMap<String, String> create(Entry<String, String>[] entries) {
      BiMap<String, String> result = HashBiMap.create();
      for (Entry<String, String> entry : entries) {
        result.put(entry.getKey(), entry.getValue());
      }
      return result;
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // suite
  @AndroidIncompatible // test-suite builders
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        BiMapTestSuiteBuilder.using(new HashBiMapGenerator())
            .named("HashBiMap")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.KNOWN_ORDER,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_ANY_NULL_QUERIES,
                MapFeature.GENERAL_PURPOSE)
            .createTestSuite());
    suite.addTestSuite(HashBiMapTest.class);
    return suite;
  }

  public void testMapConstructor() {
    /* Test with non-empty Map. */
    Map<String, String> map =
        ImmutableMap.of(
            "canada", "dollar",
            "chile", "peso",
            "switzerland", "franc");
    HashBiMap<String, String> bimap = HashBiMap.create(map);
    assertThat(bimap.get("canada")).isEqualTo("dollar");
    assertThat(bimap.inverse().get("dollar")).isEqualTo("canada");
  }

  private static final int N = 1000;

  public void testBashIt() {
    BiMap<Integer, Integer> bimap = HashBiMap.create(N);
    BiMap<Integer, Integer> inverse = bimap.inverse();

    for (int i = 0; i < N; i++) {
      assertThat(bimap.put(2 * i, 2 * i + 1)).isNull();
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i + 1, (int) bimap.get(2 * i));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i, (int) inverse.get(2 * i + 1));
    }
    for (int i = 0; i < N; i++) {
      int oldValue = bimap.get(2 * i);
      assertEquals(2 * i + 1, (int) bimap.put(2 * i, oldValue - 2));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i - 1, (int) bimap.get(2 * i));
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * i, (int) inverse.get(2 * i - 1));
    }
    Set<Entry<Integer, Integer>> entries = bimap.entrySet();
    for (Entry<Integer, Integer> entry : entries) {
      entry.setValue(entry.getValue() + 2 * N);
    }
    for (int i = 0; i < N; i++) {
      assertEquals(2 * N + 2 * i - 1, (int) bimap.get(2 * i));
    }
  }

  private static <K extends @Nullable Object, V extends @Nullable Object>
      ImmutableList<HashBiMap<K, V>> createBiMaps() {
    return ImmutableList.of(HashBiMap.create());
  }

  public void testBiMapEntrySetIteratorRemove() {
    for (BiMap<Integer, String> map : HashBiMapTest.<Integer, String>createBiMaps()) {
      map.put(1, "one");
      Set<Entry<Integer, String>> entries = map.entrySet();
      Iterator<Entry<Integer, String>> iterator = entries.iterator();
      Entry<Integer, String> entry = iterator.next();
      entry.setValue("two"); // changes the iterator's current entry value
      assertThat(map.get(1)).isEqualTo("two");
      assertEquals(Integer.valueOf(1), map.inverse().get("two"));
      iterator.remove(); // removes the updated entry
      assertTrue(map.isEmpty());
    }
  }

  public void testInsertionOrder() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);
      assertThat(map.entrySet())
          .containsExactly(
              immutableEntry("foo", 1), immutableEntry("bar", 2), immutableEntry("quux", 3))
          .inOrder();
    }
  }

  public void testInsertionOrderAfterRemoveFirst() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);

      map.remove("foo");
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("bar", 2), immutableEntry("quux", 3))
          .inOrder();
    }
  }

  public void testInsertionOrderAfterRemoveMiddle() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);

      map.remove("bar");
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("foo", 1), immutableEntry("quux", 3))
          .inOrder();
    }
  }

  public void testInsertionOrderAfterRemoveLast() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);

      map.remove("quux");
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("foo", 1), immutableEntry("bar", 2))
          .inOrder();
    }
  }

  public void testInsertionOrderAfterForcePut() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);

      map.forcePut("quux", 1);
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("bar", 2), immutableEntry("quux", 1))
          .inOrder();
      assertThat(map.inverse().entrySet())
          .containsExactly(immutableEntry(2, "bar"), immutableEntry(1, "quux"))
          .inOrder();
    }
  }

  public void testInsertionOrderAfterInverseForcePut() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);

      map.inverse().forcePut(1, "quux");
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("bar", 2), immutableEntry("quux", 1))
          .inOrder();
    }
  }

  public void testInverseInsertionOrderAfterInverse() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("bar", 2);
      map.put("quux", 1);

      assertThat(map.inverse().entrySet())
          .containsExactly(immutableEntry(2, "bar"), immutableEntry(1, "quux"))
          .inOrder();
    }
  }

  public void testInverseInsertionOrderAfterInverseForcePut() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);

      map.inverse().forcePut(1, "quux");
      assertThat(map.inverse().entrySet())
          .containsExactly(immutableEntry(2, "bar"), immutableEntry(1, "quux"))
          .inOrder();
    }
  }

  public void testInverseInsertionOrderAfterInverseForcePutPresentKey() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("foo", 1);
      map.put("bar", 2);
      map.put("quux", 3);
      map.put("nab", 4);

      map.inverse().forcePut(4, "bar");
      assertThat(map.entrySet())
          .containsExactly(
              immutableEntry("foo", 1), immutableEntry("bar", 4), immutableEntry("quux", 3))
          .inOrder();
      assertThat(map.inverse().entrySet())
          .containsExactly(
              immutableEntry(1, "foo"), immutableEntry(4, "bar"), immutableEntry(3, "quux"))
          .inOrder();
    }
  }

  public void testInverseEntrySetValueNewKey() {
    for (BiMap<Integer, String> map : HashBiMapTest.<Integer, String>createBiMaps()) {
      map.put(1, "a");
      map.put(2, "b");
      Iterator<Entry<String, Integer>> inverseEntryItr = map.inverse().entrySet().iterator();
      Entry<String, Integer> entry = inverseEntryItr.next();
      entry.setValue(3);
      assertEquals(immutableEntry("b", 2), inverseEntryItr.next());
      assertFalse(inverseEntryItr.hasNext());
      assertThat(map.entrySet())
          .containsExactly(immutableEntry(2, "b"), immutableEntry(3, "a"))
          .inOrder();
    }
  }

  public void testInverseIterationOrderAfterUpdate() {
    for (BiMap<String, String> map : HashBiMapTest.<String, String>createBiMaps()) {
      map.put("a", "1");
      map.put("b", "2");
      map.put("c", "3");
      map.put("a", "4");

      assertThat(map.entrySet())
          .containsExactly(
              immutableEntry("a", "4"), immutableEntry("b", "2"), immutableEntry("c", "3"))
          .inOrder();

      assertThat(map.inverse().entrySet())
          .containsExactly(
              immutableEntry("4", "a"), immutableEntry("2", "b"), immutableEntry("3", "c"))
          .inOrder();
    }
  }

  public void testInverseReplaceAllIterationOrder() {
    for (BiMap<String, String> map : HashBiMapTest.<String, String>createBiMaps()) {
      map.put("bar", "2");
      map.put("quux", "1");

      map.inverse().replaceAll((v, k) -> k + v);

      assertThat(map.entrySet())
          .containsExactly(immutableEntry("bar2", "2"), immutableEntry("quux1", "1"))
          .inOrder();
      assertThat(map.inverse().entrySet())
          .containsExactly(immutableEntry("2", "bar2"), immutableEntry("1", "quux1"))
          .inOrder();
    }
  }

  public void testEntrySetSetValueAfterRemove() {
    for (BiMap<Integer, String> map : HashBiMapTest.<Integer, String>createBiMaps()) {
      map.put(1, "a");
      map.put(2, "b");
      map.put(3, "c");

      Iterator<Entry<Integer, String>> it = map.entrySet().iterator();
      Entry<Integer, String> first = it.next();
      it.remove();
      assertThat(first.setValue("x")).isNull();
      assertThat(first.setValue("a")).isEqualTo("x");

      Entry<Integer, String> second = map.entrySet().iterator().next();
      map.remove(2);
      assertThat(second.setValue("y")).isNull();
      map.remove(2);
      map.put(2, "b");
      assertThat(second.setValue("y")).isEqualTo("b");

      assertThat(map.entrySet())
          .containsExactly(immutableEntry(3, "c"), immutableEntry(1, "a"), immutableEntry(2, "y"))
          .inOrder();
      assertThat(map.inverse().entrySet())
          .containsExactly(immutableEntry("c", 3), immutableEntry("a", 1), immutableEntry("y", 2))
          .inOrder();
    }
  }

  public void testInverseEntrySetSetValueAfterRemove() {
    for (BiMap<Integer, String> map : HashBiMapTest.<Integer, String>createBiMaps()) {
      map.put(1, "a");
      map.put(2, "b");
      map.put(3, "c");

      Iterator<Entry<String, Integer>> it = map.inverse().entrySet().iterator();
      Entry<String, Integer> first = it.next();
      it.remove();
      assertThat(first.setValue(10)).isNull();
      assertThat(first.setValue(1)).isEqualTo(10);

      Entry<String, Integer> second = map.inverse().entrySet().iterator().next();
      map.inverse().remove("b");
      assertThat(second.setValue(20)).isNull();
      map.inverse().remove("b");
      map.put(2, "b");
      assertThat(second.setValue(20)).isEqualTo(2);

      assertThat(map.entrySet())
          .containsExactly(immutableEntry(3, "c"), immutableEntry(1, "a"), immutableEntry(20, "b"))
          .inOrder();
      assertThat(map.inverse().entrySet())
          .containsExactly(immutableEntry("c", 3), immutableEntry("a", 1), immutableEntry("b", 20))
          .inOrder();
    }
  }

  public void testEntrySetNullKeyAfterRemoveOther() {
    for (BiMap<@Nullable Integer, String> map :
        HashBiMapTest.<@Nullable Integer, String>createBiMaps()) {
      map.put(1, "a");
      map.put(null, "b");
      Iterator<Entry<@Nullable Integer, String>> iterator = map.entrySet().iterator();
      iterator.next();
      Entry<@Nullable Integer, String> nullEntry = iterator.next();
      map.remove(1);
      assertThat(nullEntry.getValue()).isEqualTo("b");
      assertThat(nullEntry.setValue("c")).isEqualTo("b");
      assertThat(nullEntry.getValue()).isEqualTo("c");
      assertThat(map).containsExactly(null, "c");
    }
  }

  public void testInverseEntrySetNullKeyAfterRemoveOther() {
    for (BiMap<Integer, @Nullable String> map :
        HashBiMapTest.<Integer, @Nullable String>createBiMaps()) {
      map.put(1, "a");
      map.put(2, null);
      Iterator<Entry<@Nullable String, Integer>> iterator = map.inverse().entrySet().iterator();
      iterator.next();
      Entry<@Nullable String, Integer> nullEntry = iterator.next();
      map.remove(1);
      assertThat(nullEntry.getValue()).isEqualTo(2);
      assertThat(nullEntry.setValue(3)).isEqualTo(2);
      assertThat(nullEntry.getValue()).isEqualTo(3);
      assertThat(map).containsExactly(3, null);
    }
  }

  public void testInversePutConcurrentModificationException() {
    for (BiMap<Integer, String> map : HashBiMapTest.<Integer, String>createBiMaps()) {
      map.put(1, "a");
      map.put(2, "b");
      Iterator<Entry<Integer, String>> iterator = map.entrySet().iterator();
      map.inverse().put("a", 3);
      assertThrows(ConcurrentModificationException.class, () -> iterator.next());
    }
  }

  public void testInverseForcePutConcurrentModificationException() {
    for (BiMap<Integer, String> map : HashBiMapTest.<Integer, String>createBiMaps()) {
      map.put(1, "a");
      map.put(2, "b");
      Iterator<Entry<Integer, String>> iterator = map.entrySet().iterator();
      map.inverse().forcePut("a", 3);
      assertThrows(ConcurrentModificationException.class, () -> iterator.next());
    }
  }

  public void testInverseForcePutPredecessorMoved() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("a", 1);
      map.put("b", 2);
      map.put("c", 3);
      map.remove("a");
      map.inverse().forcePut(4, "c");
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("b", 2), immutableEntry("c", 4))
          .inOrder();
      assertThat(map.inverse().entrySet())
          .containsExactly(immutableEntry(2, "b"), immutableEntry(4, "c"))
          .inOrder();
    }
  }

  public void testInverseForcePutPresentValueAndKeyPredecessorWasEntryAndMoved() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("a", 1);
      map.put("b", 2);
      map.put("c", 3);
      map.put("d", 4);
      map.remove("a");
      map.inverse().forcePut(3, "d");
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("b", 2), immutableEntry("d", 3))
          .inOrder();
      assertThat(map.inverse().entrySet())
          .containsExactly(immutableEntry(2, "b"), immutableEntry(3, "d"))
          .inOrder();
    }
  }

  public void testInverseForcePutPresentValueAndKeySuccessorWasEntryAndMoved() {
    for (BiMap<String, Integer> map : HashBiMapTest.<String, Integer>createBiMaps()) {
      map.put("a", 1);
      map.put("b", 2);
      map.put("c", 3);
      map.put("d", 4);
      map.remove("a");
      map.inverse().forcePut(3, "b");
      assertThat(map.entrySet())
          .containsExactly(immutableEntry("b", 3), immutableEntry("d", 4))
          .inOrder();
      assertThat(map.inverse().entrySet())
          .containsExactly(immutableEntry(3, "b"), immutableEntry(4, "d"))
          .inOrder();
    }
  }
}

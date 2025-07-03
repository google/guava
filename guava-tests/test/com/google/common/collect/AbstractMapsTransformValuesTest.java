/*
 * Copyright (C) 2008 The Guava Authors
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
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.testing.MapInterfaceTest;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Superclass for tests for {@link Maps#transformValues} overloads.
 *
 * @author Isaac Shum
 */
@GwtCompatible
@NullMarked
abstract class AbstractMapsTransformValuesTest extends MapInterfaceTest<String, String> {
  public AbstractMapsTransformValuesTest() {
    super(false, true, false, true, true);
  }

  @Override
  protected String getKeyNotInPopulatedMap() throws UnsupportedOperationException {
    return "z";
  }

  @Override
  protected String getValueNotInPopulatedMap() throws UnsupportedOperationException {
    return "26";
  }

  /** Helper assertion comparing two maps */
  private void assertMapsEqual(Map<?, ?> expected, Map<?, ?> map) {
    assertEquals(expected, map);
    assertEquals(expected.hashCode(), map.hashCode());
    assertEquals(expected.entrySet(), map.entrySet());

    // Assert that expectedValues > mapValues and that
    // mapValues > expectedValues; i.e. that expectedValues == mapValues.
    Collection<?> expectedValues = expected.values();
    Collection<?> mapValues = map.values();
    assertEquals(expectedValues.size(), mapValues.size());
    assertTrue(expectedValues.containsAll(mapValues));
    assertTrue(mapValues.containsAll(expectedValues));
  }

  public void testTransformEmptyMapEquality() {
    Map<String, String> map =
        transformValues(ImmutableMap.<String, Integer>of(), Functions.toStringFunction());
    assertMapsEqual(new HashMap<>(), map);
  }

  public void testTransformSingletonMapEquality() {
    Map<String, String> map =
        transformValues(ImmutableMap.of("a", 1), Functions.toStringFunction());
    Map<String, String> expected = ImmutableMap.of("a", "1");
    assertMapsEqual(expected, map);
    assertEquals(expected.get("a"), map.get("a"));
  }

  public void testTransformIdentityFunctionEquality() {
    Map<String, Integer> underlying = ImmutableMap.of("a", 1);
    Map<String, Integer> map = transformValues(underlying, Functions.<Integer>identity());
    assertMapsEqual(underlying, map);
  }

  public void testTransformPutEntryIsUnsupported() {
    Map<String, String> map =
        transformValues(ImmutableMap.of("a", 1), Functions.toStringFunction());
    assertThrows(UnsupportedOperationException.class, () -> map.put("b", "2"));

    assertThrows(UnsupportedOperationException.class, () -> map.putAll(ImmutableMap.of("b", "2")));

    assertThrows(
        UnsupportedOperationException.class,
        () -> map.entrySet().iterator().next().setValue("one"));
  }

  public void testTransformRemoveEntry() {
    Map<String, Integer> underlying = new HashMap<>();
    underlying.put("a", 1);
    Map<String, String> map = transformValues(underlying, Functions.toStringFunction());
    assertEquals("1", map.remove("a"));
    assertNull(map.remove("b"));
  }

  public void testTransformEqualityOfMapsWithNullValues() {
    Map<String, @Nullable String> underlying = new HashMap<>();
    underlying.put("a", null);
    underlying.put("b", "");

    Map<String, Boolean> map =
        transformValues(
            underlying,
            new Function<@Nullable String, Boolean>() {
              @Override
              public Boolean apply(@Nullable String from) {
                return from == null;
              }
            });
    Map<String, Boolean> expected = ImmutableMap.of("a", true, "b", false);
    assertMapsEqual(expected, map);
    assertEquals(expected.get("a"), map.get("a"));
    assertEquals(expected.containsKey("a"), map.containsKey("a"));
    assertEquals(expected.get("b"), map.get("b"));
    assertEquals(expected.containsKey("b"), map.containsKey("b"));
    assertEquals(expected.get("c"), map.get("c"));
    assertEquals(expected.containsKey("c"), map.containsKey("c"));
  }

  public void testTransformReflectsUnderlyingMap() {
    Map<String, Integer> underlying = new HashMap<>();
    underlying.put("a", 1);
    underlying.put("b", 2);
    underlying.put("c", 3);
    Map<String, String> map = transformValues(underlying, Functions.toStringFunction());
    assertEquals(underlying.size(), map.size());

    underlying.put("d", 4);
    assertEquals(underlying.size(), map.size());
    assertEquals("4", map.get("d"));

    underlying.remove("c");
    assertEquals(underlying.size(), map.size());
    assertFalse(map.containsKey("c"));

    underlying.clear();
    assertEquals(underlying.size(), map.size());
  }

  public void testTransformChangesAreReflectedInUnderlyingMap() {
    Map<String, Integer> underlying = new LinkedHashMap<>();
    underlying.put("a", 1);
    underlying.put("b", 2);
    underlying.put("c", 3);
    underlying.put("d", 4);
    underlying.put("e", 5);
    underlying.put("f", 6);
    underlying.put("g", 7);
    Map<String, String> map = transformValues(underlying, Functions.toStringFunction());

    map.remove("a");
    assertFalse(underlying.containsKey("a"));

    Set<String> keys = map.keySet();
    keys.remove("b");
    assertFalse(underlying.containsKey("b"));

    Iterator<String> keyIterator = keys.iterator();
    keyIterator.next();
    keyIterator.remove();
    assertFalse(underlying.containsKey("c"));

    Collection<String> values = map.values();
    values.remove("4");
    assertFalse(underlying.containsKey("d"));

    Iterator<String> valueIterator = values.iterator();
    valueIterator.next();
    valueIterator.remove();
    assertFalse(underlying.containsKey("e"));

    Set<Entry<String, String>> entries = map.entrySet();
    Entry<String, String> firstEntry = entries.iterator().next();
    entries.remove(firstEntry);
    assertFalse(underlying.containsKey("f"));

    Iterator<Entry<String, String>> entryIterator = entries.iterator();
    entryIterator.next();
    entryIterator.remove();
    assertFalse(underlying.containsKey("g"));

    assertTrue(underlying.isEmpty());
    assertTrue(map.isEmpty());
    assertTrue(keys.isEmpty());
    assertTrue(values.isEmpty());
    assertTrue(entries.isEmpty());
  }

  public void testTransformEquals() {
    Map<String, Integer> underlying = ImmutableMap.of("a", 0, "b", 1, "c", 2);
    Map<String, Integer> expected = transformValues(underlying, Functions.<Integer>identity());

    assertMapsEqual(expected, expected);

    Map<String, Integer> equalToUnderlying = Maps.newTreeMap();
    equalToUnderlying.putAll(underlying);
    Map<String, Integer> map = transformValues(equalToUnderlying, Functions.<Integer>identity());
    assertMapsEqual(expected, map);

    map =
        transformValues(
            ImmutableMap.of("a", 1, "b", 2, "c", 3),
            new Function<Integer, Integer>() {
              @Override
              public Integer apply(Integer from) {
                return from - 1;
              }
            });
    assertMapsEqual(expected, map);
  }

  public void testTransformEntrySetContains() {
    Map<@Nullable String, @Nullable Boolean> underlying = new HashMap<>();
    underlying.put("a", null);
    underlying.put("b", true);
    underlying.put(null, true);

    Map<@Nullable String, @Nullable Boolean> map =
        transformValues(
            underlying,
            new Function<@Nullable Boolean, @Nullable Boolean>() {
              @Override
              public @Nullable Boolean apply(@Nullable Boolean from) {
                return (from == null) ? true : null;
              }
            });

    Set<Entry<@Nullable String, @Nullable Boolean>> entries = map.entrySet();
    assertTrue(entries.contains(immutableEntry("a", true)));
    assertTrue(entries.contains(Maps.<String, @Nullable Boolean>immutableEntry("b", null)));
    assertTrue(
        entries.contains(Maps.<@Nullable String, @Nullable Boolean>immutableEntry(null, null)));

    assertFalse(entries.contains(Maps.<String, @Nullable Boolean>immutableEntry("c", null)));
    assertFalse(entries.contains(Maps.<@Nullable String, Boolean>immutableEntry(null, true)));
  }

  @Override
  public void testKeySetRemoveAllNullFromEmpty() {
    try {
      super.testKeySetRemoveAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's HashMap.keySet().removeAll(null) doesn't throws NPE.
    }
  }

  @Override
  public void testEntrySetRemoveAllNullFromEmpty() {
    try {
      super.testEntrySetRemoveAllNullFromEmpty();
    } catch (RuntimeException tolerated) {
      // GWT's HashMap.entrySet().removeAll(null) doesn't throws NPE.
    }
  }
}

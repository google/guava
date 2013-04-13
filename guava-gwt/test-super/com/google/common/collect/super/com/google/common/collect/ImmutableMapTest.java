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

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.testing.AnEnum;
import com.google.common.collect.testing.MapInterfaceTest;
import com.google.common.collect.testing.MinimalSet;
import com.google.common.collect.testing.SampleElements.Colliders;
import com.google.common.testing.EqualsTester;

import junit.framework.TestCase;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Tests for {@link ImmutableMap}.
 *
 * @author Kevin Bourrillion
 * @author Jesse Wilson
 */
@GwtCompatible(emulated = true)
public class ImmutableMapTest extends TestCase {

  public abstract static class AbstractMapTests<K, V>
      extends MapInterfaceTest<K, V> {
    public AbstractMapTests() {
      super(false, false, false, false, false);
    }

    @Override protected Map<K, V> makeEmptyMap() {
      throw new UnsupportedOperationException();
    }

    private static final Joiner joiner = Joiner.on(", ");

    @Override protected void assertMoreInvariants(Map<K, V> map) {
      // TODO: can these be moved to MapInterfaceTest?
      for (Entry<K, V> entry : map.entrySet()) {
        assertEquals(entry.getKey() + "=" + entry.getValue(),
            entry.toString());
      }

      assertEquals("{" + joiner.join(map.entrySet()) + "}",
          map.toString());
      assertEquals("[" + joiner.join(map.entrySet()) + "]",
          map.entrySet().toString());
      assertEquals("[" + joiner.join(map.keySet()) + "]",
          map.keySet().toString());
      assertEquals("[" + joiner.join(map.values()) + "]",
          map.values().toString());

      assertEquals(MinimalSet.from(map.entrySet()), map.entrySet());
      assertEquals(Sets.newHashSet(map.keySet()), map.keySet());
    }
  }

  public static class MapTests extends AbstractMapTests<String, Integer> {
    @Override protected Map<String, Integer> makeEmptyMap() {
      return ImmutableMap.of();
    }

    @Override protected Map<String, Integer> makePopulatedMap() {
      return ImmutableMap.of("one", 1, "two", 2, "three", 3);
    }

    @Override protected String getKeyNotInPopulatedMap() {
      return "minus one";
    }

    @Override protected Integer getValueNotInPopulatedMap() {
      return -1;
    }
  }

  public static class SingletonMapTests
      extends AbstractMapTests<String, Integer> {
    @Override protected Map<String, Integer> makePopulatedMap() {
      return ImmutableMap.of("one", 1);
    }

    @Override protected String getKeyNotInPopulatedMap() {
      return "minus one";
    }

    @Override protected Integer getValueNotInPopulatedMap() {
      return -1;
    }
  }

  public static class MapTestsWithBadHashes
      extends AbstractMapTests<Object, Integer> {

    @Override protected Map<Object, Integer> makeEmptyMap() {
      throw new UnsupportedOperationException();
    }

    @Override protected Map<Object, Integer> makePopulatedMap() {
      Colliders colliders = new Colliders();
      return ImmutableMap.of(
          colliders.e0, 0,
          colliders.e1, 1,
          colliders.e2, 2,
          colliders.e3, 3);
    }

    @Override protected Object getKeyNotInPopulatedMap() {
      return new Colliders().e4;
    }

    @Override protected Integer getValueNotInPopulatedMap() {
      return 4;
    }
  }

  public static class CreationTests extends TestCase {
    public void testEmptyBuilder() {
      ImmutableMap<String, Integer> map
          = new Builder<String, Integer>().build();
      assertEquals(Collections.<String, Integer>emptyMap(), map);
    }

    public void testSingletonBuilder() {
      ImmutableMap<String, Integer> map = new Builder<String, Integer>()
          .put("one", 1)
          .build();
      assertMapEquals(map, "one", 1);
    }

    public void testBuilder() {
      ImmutableMap<String, Integer> map = new Builder<String, Integer>()
          .put("one", 1)
          .put("two", 2)
          .put("three", 3)
          .put("four", 4)
          .put("five", 5)
          .build();
      assertMapEquals(map,
          "one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
    }

    public void testBuilder_withImmutableEntry() {
      ImmutableMap<String, Integer> map = new Builder<String, Integer>()
          .put(Maps.immutableEntry("one", 1))
          .build();
      assertMapEquals(map, "one", 1);
    }

    public void testBuilder_withImmutableEntryAndNullContents() {
      Builder<String, Integer> builder = new Builder<String, Integer>();
      try {
        builder.put(Maps.immutableEntry("one", (Integer) null));
        fail();
      } catch (NullPointerException expected) {
      }
      try {
        builder.put(Maps.immutableEntry((String) null, 1));
        fail();
      } catch (NullPointerException expected) {
      }
    }

    private static class StringHolder {
      String string;
    }

    public void testBuilder_withMutableEntry() {
      ImmutableMap.Builder<String, Integer> builder =
          new Builder<String, Integer>();
      final StringHolder holder = new StringHolder();
      holder.string = "one";
      Entry<String, Integer> entry = new AbstractMapEntry<String, Integer>() {
        @Override public String getKey() {
          return holder.string;
        }
        @Override public Integer getValue() {
          return 1;
        }
      };

      builder.put(entry);
      holder.string = "two";
      assertMapEquals(builder.build(), "one", 1);
    }

    public void testBuilderPutAllWithEmptyMap() {
      ImmutableMap<String, Integer> map = new Builder<String, Integer>()
          .putAll(Collections.<String, Integer>emptyMap())
          .build();
      assertEquals(Collections.<String, Integer>emptyMap(), map);
    }

    public void testBuilderPutAll() {
      Map<String, Integer> toPut = new LinkedHashMap<String, Integer>();
      toPut.put("one", 1);
      toPut.put("two", 2);
      toPut.put("three", 3);
      Map<String, Integer> moreToPut = new LinkedHashMap<String, Integer>();
      moreToPut.put("four", 4);
      moreToPut.put("five", 5);

      ImmutableMap<String, Integer> map = new Builder<String, Integer>()
          .putAll(toPut)
          .putAll(moreToPut)
          .build();
      assertMapEquals(map,
          "one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
    }

    public void testBuilderReuse() {
      Builder<String, Integer> builder = new Builder<String, Integer>();
      ImmutableMap<String, Integer> mapOne = builder
          .put("one", 1)
          .put("two", 2)
          .build();
      ImmutableMap<String, Integer> mapTwo = builder
          .put("three", 3)
          .put("four", 4)
          .build();

      assertMapEquals(mapOne, "one", 1, "two", 2);
      assertMapEquals(mapTwo, "one", 1, "two", 2, "three", 3, "four", 4);
    }
    
    public void testBuilderPutNullKeyFailsAtomically() {
      Builder<String, Integer> builder = new Builder<String, Integer>();
      try {
        builder.put(null, 1);
        fail();
      } catch (NullPointerException expected) {}
      builder.put("foo", 2);
      assertMapEquals(builder.build(), "foo", 2);
    }
    
    public void testBuilderPutImmutableEntryWithNullKeyFailsAtomically() {
      Builder<String, Integer> builder = new Builder<String, Integer>();
      try {
        builder.put(Maps.immutableEntry((String) null, 1));
        fail();
      } catch (NullPointerException expected) {}
      builder.put("foo", 2);
      assertMapEquals(builder.build(), "foo", 2);
    }
    
    // for GWT compatibility
    static class SimpleEntry<K, V> extends AbstractMapEntry<K, V> {
      public K key;
      public V value;
      
      SimpleEntry(K key, V value) {
        this.key = key;
        this.value = value;
      }

      @Override
      public K getKey() {
        return key;
      }

      @Override
      public V getValue() {
        return value;
      }
    }
    
    public void testBuilderPutMutableEntryWithNullKeyFailsAtomically() {
      Builder<String, Integer> builder = new Builder<String, Integer>();
      try {
        builder.put(new SimpleEntry<String, Integer>(null, 1));
        fail();
      } catch (NullPointerException expected) {}
      builder.put("foo", 2);
      assertMapEquals(builder.build(), "foo", 2);
    }

    public void testBuilderPutNullKey() {
      Builder<String, Integer> builder = new Builder<String, Integer>();
      try {
        builder.put(null, 1);
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testBuilderPutNullValue() {
      Builder<String, Integer> builder = new Builder<String, Integer>();
      try {
        builder.put("one", null);
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testBuilderPutNullKeyViaPutAll() {
      Builder<String, Integer> builder = new Builder<String, Integer>();
      try {
        builder.putAll(Collections.<String, Integer>singletonMap(null, 1));
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testBuilderPutNullValueViaPutAll() {
      Builder<String, Integer> builder = new Builder<String, Integer>();
      try {
        builder.putAll(Collections.<String, Integer>singletonMap("one", null));
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testPuttingTheSameKeyTwiceThrowsOnBuild() {
      Builder<String, Integer> builder = new Builder<String, Integer>()
          .put("one", 1)
          .put("one", 1); // throwing on this line would be even better

      try {
        builder.build();
        fail();
      } catch (IllegalArgumentException expected) {
      }
    }

    public void testOf() {
      assertMapEquals(
          ImmutableMap.of("one", 1),
          "one", 1);
      assertMapEquals(
          ImmutableMap.of("one", 1, "two", 2),
          "one", 1, "two", 2);
      assertMapEquals(
          ImmutableMap.of("one", 1, "two", 2, "three", 3),
          "one", 1, "two", 2, "three", 3);
      assertMapEquals(
          ImmutableMap.of("one", 1, "two", 2, "three", 3, "four", 4),
          "one", 1, "two", 2, "three", 3, "four", 4);
      assertMapEquals(
          ImmutableMap.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5),
          "one", 1, "two", 2, "three", 3, "four", 4, "five", 5);
    }

    public void testOfNullKey() {
      try {
        ImmutableMap.of(null, 1);
        fail();
      } catch (NullPointerException expected) {
      }

      try {
        ImmutableMap.of("one", 1, null, 2);
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testOfNullValue() {
      try {
        ImmutableMap.of("one", null);
        fail();
      } catch (NullPointerException expected) {
      }

      try {
        ImmutableMap.of("one", 1, "two", null);
        fail();
      } catch (NullPointerException expected) {
      }
    }

    public void testOfWithDuplicateKey() {
      try {
        ImmutableMap.of("one", 1, "one", 1);
        fail();
      } catch (IllegalArgumentException expected) {
      }
    }
    
    public void testCopyOfEmptyMap() {
      ImmutableMap<String, Integer> copy
          = ImmutableMap.copyOf(Collections.<String, Integer>emptyMap());
      assertEquals(Collections.<String, Integer>emptyMap(), copy);
      assertSame(copy, ImmutableMap.copyOf(copy));
    }

    public void testCopyOfSingletonMap() {
      ImmutableMap<String, Integer> copy
          = ImmutableMap.copyOf(Collections.singletonMap("one", 1));
      assertMapEquals(copy, "one", 1);
      assertSame(copy, ImmutableMap.copyOf(copy));
    }

    public void testCopyOf() {
      Map<String, Integer> original = new LinkedHashMap<String, Integer>();
      original.put("one", 1);
      original.put("two", 2);
      original.put("three", 3);

      ImmutableMap<String, Integer> copy = ImmutableMap.copyOf(original);
      assertMapEquals(copy, "one", 1, "two", 2, "three", 3);
      assertSame(copy, ImmutableMap.copyOf(copy));
    }
  }

  public void testNullGet() {
    ImmutableMap<String, Integer> map = ImmutableMap.of("one", 1);
    assertNull(map.get(null));
  }

  public void testAsMultimap() {
    ImmutableMap<String, Integer> map = ImmutableMap.of(
        "one", 1, "won", 1, "two", 2, "too", 2, "three", 3);
    ImmutableSetMultimap<String, Integer> expected = ImmutableSetMultimap.of(
        "one", 1, "won", 1, "two", 2, "too", 2, "three", 3);
    assertEquals(expected, map.asMultimap());
  }

  public void testAsMultimapWhenEmpty() {
    ImmutableMap<String, Integer> map = ImmutableMap.of();
    ImmutableSetMultimap<String, Integer> expected = ImmutableSetMultimap.of();
    assertEquals(expected, map.asMultimap());
  }

  public void testAsMultimapCaches() {
    ImmutableMap<String, Integer> map = ImmutableMap.of("one", 1);
    ImmutableSetMultimap<String, Integer> multimap1 = map.asMultimap();
    ImmutableSetMultimap<String, Integer> multimap2 = map.asMultimap();
    assertEquals(1, multimap1.asMap().size());
    assertSame(multimap1, multimap2);
  }

  private static <K, V> void assertMapEquals(Map<K, V> map,
      Object... alternatingKeysAndValues) {
    assertEquals(map.size(), alternatingKeysAndValues.length / 2);
    int i = 0;
    for (Entry<K, V> entry : map.entrySet()) {
      assertEquals(alternatingKeysAndValues[i++], entry.getKey());
      assertEquals(alternatingKeysAndValues[i++], entry.getValue());
    }
  }

  private static class IntHolder implements Serializable {
    public int value;

    public IntHolder(int value) {
      this.value = value;
    }

    @Override public boolean equals(Object o) {
      return (o instanceof IntHolder) && ((IntHolder) o).value == value;
    }

    @Override public int hashCode() {
      return value;
    }

    private static final long serialVersionUID = 5;
  }

  public void testMutableValues() {
    IntHolder holderA = new IntHolder(1);
    IntHolder holderB = new IntHolder(2);
    Map<String, IntHolder> map = ImmutableMap.of("a", holderA, "b", holderB);
    holderA.value = 3;
    assertTrue(map.entrySet().contains(
        Maps.immutableEntry("a", new IntHolder(3))));
    Map<String, Integer> intMap = ImmutableMap.of("a", 3, "b", 2);
    assertEquals(intMap.hashCode(), map.entrySet().hashCode());
    assertEquals(intMap.hashCode(), map.hashCode());
  }

  public void testCopyOfEnumMap() {
    EnumMap<AnEnum, String> map = new EnumMap<AnEnum, String>(AnEnum.class);
    map.put(AnEnum.B, "foo");
    map.put(AnEnum.C, "bar");
    assertTrue(ImmutableMap.copyOf(map) instanceof ImmutableEnumMap);
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(ImmutableList.of(), ImmutableList.of())
        .addEqualityGroup(ImmutableList.of(1), ImmutableList.of(1))
        .addEqualityGroup(ImmutableList.of(1, 2), ImmutableList.of(1, 2))
        .addEqualityGroup(ImmutableList.of(1, 2, 3))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6, 7))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        .addEqualityGroup(ImmutableList.of(100, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        .addEqualityGroup(ImmutableList.of(1, 200, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        .addEqualityGroup(ImmutableList.of(1, 2, 300, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 400, 5, 6, 7, 8, 9, 10, 11, 12))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 500, 6, 7, 8, 9, 10, 11, 12))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 600, 7, 8, 9, 10, 11, 12))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6, 700, 8, 9, 10, 11, 12))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 800, 9, 10, 11, 12))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 900, 10, 11, 12))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 1000, 11, 12))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1100, 12))
        .addEqualityGroup(ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 1200))
        .testEquals();

  }
}


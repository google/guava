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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestBiMapGenerator;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@code EnumHashBiMap}.
 *
 * @author Mike Bostock
 */
@GwtCompatible(emulated = true)
public class EnumHashBiMapTest extends TestCase {
  private enum Currency {
    DOLLAR,
    FRANC,
    PESO,
    POUND,
    YEN
  }

  private enum Country {
    CANADA,
    CHILE,
    JAPAN,
    SWITZERLAND,
    UK
  }

  public static final class EnumHashBiMapGenerator implements TestBiMapGenerator<Country, String> {
    @SuppressWarnings("unchecked")
    @Override
    public BiMap<Country, String> create(Object... entries) {
      BiMap<Country, String> result = EnumHashBiMap.create(Country.class);
      for (Object o : entries) {
        Entry<Country, String> entry = (Entry<Country, String>) o;
        result.put(entry.getKey(), entry.getValue());
      }
      return result;
    }

    @Override
    public SampleElements<Entry<Country, String>> samples() {
      return new SampleElements<>(
          Maps.immutableEntry(Country.CANADA, "DOLLAR"),
          Maps.immutableEntry(Country.CHILE, "PESO"),
          Maps.immutableEntry(Country.UK, "POUND"),
          Maps.immutableEntry(Country.JAPAN, "YEN"),
          Maps.immutableEntry(Country.SWITZERLAND, "FRANC"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Entry<Country, String>[] createArray(int length) {
      return new Entry[length];
    }

    @Override
    public Iterable<Entry<Country, String>> order(List<Entry<Country, String>> insertionOrder) {
      return insertionOrder;
    }

    @Override
    public Country[] createKeyArray(int length) {
      return new Country[length];
    }

    @Override
    public String[] createValueArray(int length) {
      return new String[length];
    }
  }

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        BiMapTestSuiteBuilder.using(new EnumHashBiMapGenerator())
            .named("EnumHashBiMap")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.SERIALIZABLE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.GENERAL_PURPOSE,
                CollectionFeature.KNOWN_ORDER)
            .createTestSuite());
    suite.addTestSuite(EnumHashBiMapTest.class);
    return suite;
  }

  public void testCreate() {
    EnumHashBiMap<Currency, String> bimap = EnumHashBiMap.create(Currency.class);
    assertTrue(bimap.isEmpty());
    assertEquals("{}", bimap.toString());
    assertEquals(HashBiMap.create(), bimap);
    bimap.put(Currency.DOLLAR, "dollar");
    assertEquals("dollar", bimap.get(Currency.DOLLAR));
    assertEquals(Currency.DOLLAR, bimap.inverse().get("dollar"));
  }

  public void testCreateFromMap() {
    /* Test with non-empty Map. */
    Map<Currency, String> map =
        ImmutableMap.of(
            Currency.DOLLAR, "dollar",
            Currency.PESO, "peso",
            Currency.FRANC, "franc");
    EnumHashBiMap<Currency, String> bimap = EnumHashBiMap.create(map);
    assertEquals("dollar", bimap.get(Currency.DOLLAR));
    assertEquals(Currency.DOLLAR, bimap.inverse().get("dollar"));

    /* Map must have at least one entry if not an EnumHashBiMap. */
    try {
      EnumHashBiMap.create(Collections.<Currency, String>emptyMap());
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException expected) {
    }

    /* Map can be empty if it's an EnumHashBiMap. */
    Map<Currency, String> emptyBimap = EnumHashBiMap.create(Currency.class);
    bimap = EnumHashBiMap.create(emptyBimap);
    assertTrue(bimap.isEmpty());

    /* Map can be empty if it's an EnumBiMap. */
    Map<Currency, Country> emptyBimap2 = EnumBiMap.create(Currency.class, Country.class);
    EnumHashBiMap<Currency, Country> bimap2 = EnumHashBiMap.create(emptyBimap2);
    assertTrue(bimap2.isEmpty());
  }

  public void testEnumHashBiMapConstructor() {
    /* Test that it copies existing entries. */
    EnumHashBiMap<Currency, String> bimap1 = EnumHashBiMap.create(Currency.class);
    bimap1.put(Currency.DOLLAR, "dollar");
    EnumHashBiMap<Currency, String> bimap2 = EnumHashBiMap.create(bimap1);
    assertEquals("dollar", bimap2.get(Currency.DOLLAR));
    assertEquals(bimap1, bimap2);
    bimap2.inverse().put("franc", Currency.FRANC);
    assertEquals("franc", bimap2.get(Currency.FRANC));
    assertNull(bimap1.get(Currency.FRANC));
    assertFalse(bimap2.equals(bimap1));

    /* Test that it can be empty. */
    EnumHashBiMap<Currency, String> emptyBimap = EnumHashBiMap.create(Currency.class);
    EnumHashBiMap<Currency, String> bimap3 = EnumHashBiMap.create(emptyBimap);
    assertEquals(bimap3, emptyBimap);
  }

  public void testEnumBiMapConstructor() {
    /* Test that it copies existing entries. */
    EnumBiMap<Currency, Country> bimap1 = EnumBiMap.create(Currency.class, Country.class);
    bimap1.put(Currency.DOLLAR, Country.SWITZERLAND);
    EnumHashBiMap<Currency, Object> bimap2 = // use supertype
        EnumHashBiMap.<Currency, Object>create(bimap1);
    assertEquals(Country.SWITZERLAND, bimap2.get(Currency.DOLLAR));
    assertEquals(bimap1, bimap2);
    bimap2.inverse().put("franc", Currency.FRANC);
    assertEquals("franc", bimap2.get(Currency.FRANC));
    assertNull(bimap1.get(Currency.FRANC));
    assertFalse(bimap2.equals(bimap1));

    /* Test that it can be empty. */
    EnumBiMap<Currency, Country> emptyBimap = EnumBiMap.create(Currency.class, Country.class);
    EnumHashBiMap<Currency, Country> bimap3 = // use exact type
        EnumHashBiMap.create(emptyBimap);
    assertEquals(bimap3, emptyBimap);
  }

  public void testKeyType() {
    EnumHashBiMap<Currency, String> bimap = EnumHashBiMap.create(Currency.class);
    assertEquals(Currency.class, bimap.keyType());
  }

  public void testEntrySet() {
    // Bug 3168290
    Map<Currency, String> map =
        ImmutableMap.of(
            Currency.DOLLAR, "dollar",
            Currency.PESO, "peso",
            Currency.FRANC, "franc");
    EnumHashBiMap<Currency, String> bimap = EnumHashBiMap.create(map);

    Set<Object> uniqueEntries = Sets.newIdentityHashSet();
    uniqueEntries.addAll(bimap.entrySet());
    assertEquals(3, uniqueEntries.size());
  }

  @GwtIncompatible // serialize
  public void testSerializable() {
    SerializableTester.reserializeAndAssert(EnumHashBiMap.create(Currency.class));
  }

  @GwtIncompatible // reflection
  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(EnumHashBiMap.class);
    new NullPointerTester().testAllPublicInstanceMethods(EnumHashBiMap.create(Currency.class));
  }
}

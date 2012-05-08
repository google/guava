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

import static com.google.common.collect.testing.Helpers.orderEntriesByKey;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestBiMapGenerator;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.Map.Entry;

/**
 * Collection tests for bimaps.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class BiMapCollectionTest extends TestCase {

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

  public static final class UnmodifiableBiMapGenerator extends TestStringBiMapGenerator {
    @Override
    protected BiMap<String, String> create(Entry<String, String>[] entries) {
      BiMap<String, String> result = HashBiMap.create();
      for (Entry<String, String> entry : entries) {
        result.put(entry.getKey(), entry.getValue());
      }
      return Maps.unmodifiableBiMap(result);
    }
  }

  private enum Currency { DOLLAR, FRANC, PESO, POUND, YEN }
  private enum Country { CANADA, CHILE, JAPAN, SWITZERLAND, UK }

  public static final class EnumBiMapGenerator implements TestBiMapGenerator<Country, Currency> {
    @SuppressWarnings("unchecked")
    @Override
    public BiMap<Country, Currency> create(Object... entries) {
      BiMap<Country, Currency> result = EnumBiMap.create(Country.class, Currency.class);
      for (Object object : entries) {
        Entry<Country, Currency> entry = (Entry<Country, Currency>) object;
        result.put(entry.getKey(), entry.getValue());
      }
      return result;
    }

    @Override
    public SampleElements<Entry<Country, Currency>> samples() {
      return new SampleElements<Entry<Country, Currency>>(
          Maps.immutableEntry(Country.CANADA, Currency.DOLLAR),
          Maps.immutableEntry(Country.CHILE, Currency.PESO),
          Maps.immutableEntry(Country.UK, Currency.POUND),
          Maps.immutableEntry(Country.JAPAN, Currency.YEN),
          Maps.immutableEntry(Country.SWITZERLAND, Currency.FRANC));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Entry<Country, Currency>[] createArray(int length) {
      return new Entry[length];
    }

    @Override
    public Iterable<Entry<Country, Currency>> order(List<Entry<Country, Currency>> insertionOrder) {
      return orderEntriesByKey(insertionOrder);
    }

    @Override
    public Country[] createKeyArray(int length) {
      return new Country[length];
    }

    @Override
    public Currency[] createValueArray(int length) {
      return new Currency[length];
    }
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
    public SampleElements<Entry<Country,String>> samples() {
      return new SampleElements<Entry<Country, String>>(
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

  @GwtIncompatible("suite")
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(BiMapTestSuiteBuilder.using(new HashBiMapGenerator())
        .named("HashBiMap")
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.SERIALIZABLE,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.GENERAL_PURPOSE)
        .createTestSuite());
    suite.addTest(BiMapTestSuiteBuilder.using(new EnumBiMapGenerator())
        .named("EnumBiMap")
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.SERIALIZABLE,
            MapFeature.GENERAL_PURPOSE,
            CollectionFeature.KNOWN_ORDER)
        .createTestSuite());
    suite.addTest(BiMapTestSuiteBuilder.using(new EnumHashBiMapGenerator())
        .named("EnumHashBiMap")
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.SERIALIZABLE,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.GENERAL_PURPOSE,
            CollectionFeature.KNOWN_ORDER)
        .createTestSuite());
    suite.addTest(BiMapTestSuiteBuilder.using(new UnmodifiableBiMapGenerator())
        .named("unmodifiableBiMap[HashBiMap]")
        .withFeatures(CollectionSize.ANY,
            CollectionFeature.SERIALIZABLE,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES)
        .createTestSuite());

    return suite;
  }
}

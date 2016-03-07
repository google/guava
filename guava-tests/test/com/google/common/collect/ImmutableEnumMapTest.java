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

package com.google.common.collect;

import static com.google.common.collect.testing.Helpers.mapEntry;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_QUERIES;
import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.AnEnum;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestEnumMapGenerator;
import com.google.common.collect.testing.features.CollectionSize;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Tests for {@code ImmutableEnumMap}.
 *
 * @author Louis Wasserman
 * @author Lovro Pandzic
 */
@GwtCompatible(emulated = true)
public class ImmutableEnumMapTest extends TestCase {
  public static class ImmutableEnumMapGenerator extends TestEnumMapGenerator {
    @Override
    protected Map<AnEnum, String> create(Entry<AnEnum, String>[] entries) {
      Map<AnEnum, String> map = Maps.newHashMap();
      for (Entry<AnEnum, String> entry : entries) {
        map.put(entry.getKey(), entry.getValue());
      }
      return Maps.immutableEnumMap(map);
    }
  }

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(MapTestSuiteBuilder.using(new ImmutableEnumMapGenerator())
        .named("Maps.immutableEnumMap")
        .withFeatures(CollectionSize.ANY,
            SERIALIZABLE,
            ALLOWS_NULL_QUERIES)
        .createTestSuite());
    suite.addTestSuite(ImmutableEnumMapTest.class);
    return suite;
  }

  public void testEmptyImmutableEnumMap() {
    ImmutableMap<AnEnum, String> map = Maps.immutableEnumMap(ImmutableMap.<AnEnum, String>of());
    assertEquals(ImmutableMap.of(), map);
  }

  public void testImmutableEnumMapOrdering() {
    ImmutableMap<AnEnum, String> map = Maps.immutableEnumMap(
        ImmutableMap.of(AnEnum.C, "c", AnEnum.A, "a", AnEnum.E, "e"));

    assertThat(map.entrySet()).containsExactly(
        mapEntry(AnEnum.A, "a"),
        mapEntry(AnEnum.C, "c"),
        mapEntry(AnEnum.E, "e")).inOrder();
  }

  public void testSingleParameterOf() {
    ImmutableEnumMap<SexaEnum, String> map = ImmutableEnumMap.of(SexaEnum.FIRST, "a");

    assertThat(map.entrySet()).containsExactly(mapEntry(SexaEnum.FIRST, "a"));
  }

  public void testDoubleParameterOf() {
    ImmutableEnumMap<SexaEnum, String> map = ImmutableEnumMap.of(SexaEnum.FIRST, "a", SexaEnum.SECOND, "b");

    assertThat(map.entrySet()).containsExactly(mapEntry(SexaEnum.FIRST, "a"), mapEntry(SexaEnum.SECOND, "b"));
  }

  public void testTripleParameterOf() {
    ImmutableEnumMap<SexaEnum, String> map = ImmutableEnumMap.of(
        SexaEnum.FIRST, "a", SexaEnum.SECOND, "b", SexaEnum.THIRD, "c");

    assertThat(map.entrySet()).containsExactly(
        mapEntry(SexaEnum.FIRST, "a"), mapEntry(SexaEnum.SECOND, "b"), mapEntry(SexaEnum.THIRD, "c"));
  }

  public void testQuadrupleParameterOf() {
    ImmutableEnumMap<SexaEnum, String> map = ImmutableEnumMap.of(
        SexaEnum.FIRST, "a", SexaEnum.SECOND, "b", SexaEnum.THIRD, "c", SexaEnum.FOURTH, "d");

    assertThat(map.entrySet()).containsExactly(
        mapEntry(SexaEnum.FIRST, "a"),
        mapEntry(SexaEnum.SECOND, "b"),
        mapEntry(SexaEnum.THIRD, "c"),
        mapEntry(SexaEnum.FOURTH, "d"));
  }

  public void testQuintupleParameterOf() {
    ImmutableEnumMap<SexaEnum, String> map = ImmutableEnumMap.of(
        SexaEnum.FIRST, "a", SexaEnum.SECOND, "b", SexaEnum.THIRD, "c", SexaEnum.FOURTH, "d", SexaEnum.FIFTH, "e");

    assertThat(map.entrySet()).containsExactly(
        mapEntry(SexaEnum.FIRST, "a"),
        mapEntry(SexaEnum.SECOND, "b"),
        mapEntry(SexaEnum.THIRD, "c"),
        mapEntry(SexaEnum.FOURTH, "d"),
        mapEntry(SexaEnum.FIFTH, "e"));
  }

  public void testSingleParameterForAllKeys() {
    ImmutableEnumMap<UniEnum, String> map = ImmutableEnumMap.forAllKeys(UniEnum.FIRST, "a");

    assertThat(map.entrySet()).containsExactly(mapEntry(UniEnum.FIRST, "a"));
  }

  public void testSingleParameterForAllKeysWithMissingKey() {

    try {
      ImmutableEnumMap.forAllKeys(DuEnum.FIRST, "a");
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testDoubleParameterForAllKeys() {
    ImmutableEnumMap<DuEnum, String> map = ImmutableEnumMap.forAllKeys(DuEnum.FIRST, "a", DuEnum.SECOND, "b");

    assertThat(map.entrySet()).containsExactly(mapEntry(DuEnum.FIRST, "a"), mapEntry(DuEnum.SECOND, "b"));
  }

  public void testDoubleParameterForAllKeysWithMissingKey() {

    try {
      ImmutableEnumMap.forAllKeys(TriEnum.FIRST, "a", TriEnum.SECOND, "b");
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testTripleParameterForAllKeys() {
    ImmutableEnumMap<TriEnum, String> map = ImmutableEnumMap.forAllKeys(
        TriEnum.FIRST, "a", TriEnum.SECOND, "b", TriEnum.THIRD, "c");

    assertThat(map.entrySet()).containsExactly(
        mapEntry(TriEnum.FIRST, "a"), mapEntry(TriEnum.SECOND, "b"), mapEntry(TriEnum.THIRD, "c"));
  }

  public void testTripleParameterForAllKeysWithMissingKey() {

    try {
      ImmutableEnumMap.forAllKeys(QuadriEnum.FIRST, "a", QuadriEnum.SECOND, "b", QuadriEnum.THIRD, "c");
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testQuadrupleParameterForAllKeys() {
    ImmutableEnumMap<QuadriEnum, String> map = ImmutableEnumMap.forAllKeys(
        QuadriEnum.FIRST, "a", QuadriEnum.SECOND, "b", QuadriEnum.THIRD, "c", QuadriEnum.FOURTH, "d");

    assertThat(map.entrySet()).containsExactly(
        mapEntry(QuadriEnum.FIRST, "a"),
        mapEntry(QuadriEnum.SECOND, "b"),
        mapEntry(QuadriEnum.THIRD, "c"),
        mapEntry(QuadriEnum.FOURTH, "d"));
  }

  public void testQuadrupleParameterForAllKeysWithMissingKey() {

    try {
      ImmutableEnumMap.forAllKeys(
          QuinQueEnum.FIRST, "a", QuinQueEnum.SECOND, "b", QuinQueEnum.THIRD, "c", QuinQueEnum.FOURTH, "d");
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testQuintupleParameterForAllKeys() {
    ImmutableEnumMap<QuinQueEnum, String> map = ImmutableEnumMap.forAllKeys(
        QuinQueEnum.FIRST, "a",
        QuinQueEnum.SECOND, "b",
        QuinQueEnum.THIRD, "c",
        QuinQueEnum.FOURTH, "d",
        QuinQueEnum.FIFTH, "e");

    assertThat(map.entrySet()).containsExactly(
        mapEntry(QuinQueEnum.FIRST, "a"),
        mapEntry(QuinQueEnum.SECOND, "b"),
        mapEntry(QuinQueEnum.THIRD, "c"),
        mapEntry(QuinQueEnum.FOURTH, "d"),
        mapEntry(QuinQueEnum.FIFTH, "e"));
  }

  public void testQuintupleParameterForAllKeysWithMissingKey() {

    try {
      ImmutableEnumMap.forAllKeys(
          SexaEnum.FIRST, "a", SexaEnum.SECOND, "b", SexaEnum.THIRD, "c", SexaEnum.FOURTH, "d", SexaEnum.FIFTH, "e");
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  enum UniEnum {
    FIRST
  }

  enum DuEnum {
    FIRST,
    SECOND
  }

  enum TriEnum {
    FIRST,
    SECOND,
    THIRD
  }

  enum QuadriEnum {
    FIRST,
    SECOND,
    THIRD,
    FOURTH
  }

  enum QuinQueEnum {
    FIRST,
    SECOND,
    THIRD,
    FOURTH,
    FIFTH
  }

  enum SexaEnum {
    FIRST,
    SECOND,
    THIRD,
    FOURTH,
    FIFTH,
    SIXTH
  }
}

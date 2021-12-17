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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_QUERIES;
import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.collect.testing.AnEnum;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestEnumMapGenerator;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for {@code ImmutableEnumMap}.
 *
 * @author Louis Wasserman
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
    suite.addTest(
        MapTestSuiteBuilder.using(new ImmutableEnumMapGenerator())
            .named("Maps.immutableEnumMap")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTestSuite(ImmutableEnumMapTest.class);
    return suite;
  }

  public void testIteratesOnce() {
    Map<AnEnum, AnEnum> map =
        Maps.asMap(
            ImmutableSet.of(AnEnum.A),
            new Function<AnEnum, AnEnum>() {
              boolean used = false;

              @Override
              public AnEnum apply(AnEnum ae) {
                checkState(!used, "should not be applied more than once");
                used = true;
                return ae;
              }
            });
    ImmutableMap<AnEnum, AnEnum> copy = Maps.immutableEnumMap(map);
    assertThat(copy.entrySet()).containsExactly(Helpers.mapEntry(AnEnum.A, AnEnum.A));
  }

  public void testEmptyImmutableEnumMap() {
    ImmutableMap<AnEnum, String> map = Maps.immutableEnumMap(ImmutableMap.<AnEnum, String>of());
    assertEquals(ImmutableMap.of(), map);
  }

  public void testImmutableEnumMapOrdering() {
    ImmutableMap<AnEnum, String> map =
        Maps.immutableEnumMap(ImmutableMap.of(AnEnum.C, "c", AnEnum.A, "a", AnEnum.E, "e"));

    assertThat(map.entrySet())
        .containsExactly(
            Helpers.mapEntry(AnEnum.A, "a"),
            Helpers.mapEntry(AnEnum.C, "c"),
            Helpers.mapEntry(AnEnum.E, "e"))
        .inOrder();
  }
}

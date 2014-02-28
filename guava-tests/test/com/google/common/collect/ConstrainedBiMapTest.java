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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.MapConstraintsTest.TestKeyException;
import com.google.common.collect.MapConstraintsTest.TestValueException;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Map.Entry;

/**
 * Tests for {@link MapConstraints#constrainedBiMap}.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class ConstrainedBiMapTest extends TestCase {

  private static final String TEST_KEY = "42";
  private static final String TEST_VALUE = "test";
  private static final MapConstraint<String, String> TEST_CONSTRAINT = new TestConstraint();

  @GwtIncompatible("suite")
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(BiMapTestSuiteBuilder
        .using(new ConstrainedBiMapGenerator())
        .named("Maps.constrainedBiMap[HashBiMap]")
        .withFeatures(
            CollectionSize.ANY,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.ALLOWS_ANY_NULL_QUERIES,
            MapFeature.GENERAL_PURPOSE,
            MapFeature.REJECTS_DUPLICATES_AT_CREATION)
        .createTestSuite());
    suite.addTestSuite(ConstrainedBiMapTest.class);
    return suite;
  }

  public void testPutWithForbiddenKeyForbiddenValue() {
    BiMap<String, String> map = MapConstraints.constrainedBiMap(
        HashBiMap.<String, String> create(),
        TEST_CONSTRAINT);
    try {
      map.put(TEST_KEY, TEST_VALUE);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // success
    }
  }

  public void testPutWithForbiddenKeyAllowedValue() {
    BiMap<String, String> map = MapConstraints.constrainedBiMap(
        HashBiMap.<String, String> create(),
        TEST_CONSTRAINT);
    try {
      map.put(TEST_KEY, "allowed");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // success
    }
  }

  public void testPutWithAllowedKeyForbiddenValue() {
    BiMap<String, String> map = MapConstraints.constrainedBiMap(
        HashBiMap.<String, String> create(),
        TEST_CONSTRAINT);
    try {
      map.put("allowed", TEST_VALUE);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // success
    }
  }

  public static final class ConstrainedBiMapGenerator extends TestStringBiMapGenerator {
    @Override
    protected BiMap<String, String> create(Entry<String, String>[] entries) {
      BiMap<String, String> bimap = MapConstraints.constrainedBiMap(
          HashBiMap.<String, String> create(),
          TEST_CONSTRAINT);
      for (Entry<String, String> entry : entries) {
        checkArgument(!bimap.containsKey(entry.getKey()));
        bimap.put(entry.getKey(), entry.getValue());
      }
      return bimap;
    }
  }

  private static final class TestConstraint implements MapConstraint<String, String> {
    @Override
    public void checkKeyValue(String key, String value) {
      if (TEST_KEY.equals(key)) {
        throw new TestKeyException();
      }
      if (TEST_VALUE.equals(value)) {
        throw new TestValueException();
      }
    }

    private static final long serialVersionUID = 0;
  }
}

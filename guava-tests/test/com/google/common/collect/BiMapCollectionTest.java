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
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Map.Entry;

/**
 * Collection tests for bimaps.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible
public class BiMapCollectionTest extends TestCase {

  public static final class HashBiMapGenerator extends TestStringBiMapGenerator {
    @Override
    protected BiMap<String, String> create(Entry<String, String>[] entries) {
      BiMap<String, String> result = HashBiMap.create();
      for (Entry<String, String> entry : entries) {
        checkArgument(!result.containsKey(entry.getKey()));
        result.put(entry.getKey(), entry.getValue());
      }
      return result;
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(BiMapTestSuiteBuilder.using(new HashBiMapGenerator())
        .named("HashBiMap")
        .withFeatures(CollectionSize.ANY,
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.GENERAL_PURPOSE,
            MapFeature.REJECTS_DUPLICATES_AT_CREATION)
        .createTestSuite());

    return suite;
  }
}

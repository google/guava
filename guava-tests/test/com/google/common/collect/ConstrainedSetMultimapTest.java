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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.SetMultimapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringSetMultimapGenerator;

import junit.framework.Test;
import junit.framework.TestCase;

import java.io.Serializable;
import java.util.Map.Entry;

/**
 * Tests for {@link MapConstraints#constrainedSetMultimap} not accounted for in
 * {@link MapConstraintsTest}.
 *
 * @author Jared Levy
 */
public class ConstrainedSetMultimapTest extends TestCase {
  private enum Constraint implements Serializable, MapConstraint<String, String> {
    INSTANCE;
    
    @Override
    public void checkKeyValue(String key, String value) {
      checkArgument(!"test".equals(key));
      checkArgument(!"test".equals(value));
    }
  }
  
  public static Test suite() {
    return SetMultimapTestSuiteBuilder.using(
        new TestStringSetMultimapGenerator() {
          
          @Override
          protected SetMultimap<String, String> create(Entry<String, String>[] entries) {
            SetMultimap<String, String> multimap = HashMultimap.create();
            for (Entry<String, String> entry : entries) {
              multimap.put(entry.getKey(), entry.getValue());
            }
            return MapConstraints.constrainedSetMultimap(multimap, Constraint.INSTANCE);
          }
        })
        .named("MapConstraints.constrainedSetMultimap")
        .withFeatures(
            MapFeature.ALLOWS_NULL_KEYS,
            MapFeature.ALLOWS_NULL_VALUES,
            MapFeature.ALLOWS_ANY_NULL_QUERIES,
            MapFeature.GENERAL_PURPOSE,
            CollectionSize.ANY,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.SUPPORTS_ITERATOR_REMOVE)
        .createTestSuite();
  }
}

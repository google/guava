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

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import java.util.Map;
import org.junit.Ignore;

/**
 * Basic serialization test for maps.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MapSerializationTester<K, V> extends AbstractMapTester<K, V> {
  @CollectionFeature.Require(SERIALIZABLE)
  public void testReserializeMap() {
    Map<K, V> deserialized = SerializableTester.reserialize(getMap());
    new EqualsTester().addEqualityGroup(getMap(), deserialized).testEquals();
  }
}

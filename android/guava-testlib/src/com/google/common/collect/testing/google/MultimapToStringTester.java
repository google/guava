/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect.testing.google;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.Ignore;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multimap;
import com.google.common.collect.testing.features.CollectionFeature;
import static com.google.common.collect.testing.features.CollectionFeature.NON_STANDARD_TOSTRING;
import com.google.common.collect.testing.features.CollectionSize;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import com.google.common.collect.testing.features.MapFeature;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_KEYS;
import static com.google.common.collect.testing.features.MapFeature.ALLOWS_NULL_VALUES;

/**
 * Tester for {@code Multimap.toString()}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
public class MultimapToStringTester<K, V> extends AbstractMultimapTester<K, V, Multimap<K, V>> {
  @CollectionSize.Require(ZERO)
  @CollectionFeature.Require(absent = NON_STANDARD_TOSTRING)
  public void testToStringEmpty() {
    assertEquals("{}", multimap().toString());
  }

  @CollectionSize.Require(ONE)
  @CollectionFeature.Require(absent = NON_STANDARD_TOSTRING)
  public void testToStringSingleton() {
    assertEquals("{" + k0() + "=[" + v0() + "]}", multimap().toString());
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(absent = NON_STANDARD_TOSTRING)
  @MapFeature.Require(ALLOWS_NULL_KEYS)
  public void testToStringWithNullKey() {
    initMultimapWithNullKey();
    testToStringMatchesAsMap();
  }

  @CollectionSize.Require(absent = ZERO)
  @CollectionFeature.Require(absent = NON_STANDARD_TOSTRING)
  @MapFeature.Require(ALLOWS_NULL_VALUES)
  public void testToStringWithNullValue() {
    initMultimapWithNullValue();
    testToStringMatchesAsMap();
  }

  @CollectionFeature.Require(absent = NON_STANDARD_TOSTRING)
  public void testToStringMatchesAsMap() {
    assertEquals(sortedToString(multimap().asMap().toString()), sortedToString(multimap().toString()));
  }

  private String sortedToString(String mapToString) {
    String content = mapToString.substring(1, mapToString.length() - 1);
    Pattern pattern = Pattern.compile(", (?![^\\{]*\\})");
    String[] entries = pattern.split(content);
    Arrays.sort(entries);
    return "{" + String.join(", ", entries) + "}";
  }
}

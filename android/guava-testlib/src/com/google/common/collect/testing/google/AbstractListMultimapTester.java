/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.collect.testing.Helpers.assertEqualInOrder;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.Ignore;

/**
 * Superclass for all {@code ListMultimap} testers.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
@NullMarked
public class AbstractListMultimapTester<K extends @Nullable Object, V extends @Nullable Object>
    extends AbstractMultimapTester<K, V, ListMultimap<K, V>> {

  @Override
  protected void assertGet(K key, V... values) {
    assertGet(key, asList(values));
  }

  @Override
  protected void assertGet(K key, Collection<? extends V> values) {
    assertEqualInOrder(values, multimap().get(key));

    if (!values.isEmpty()) {
      assertEqualInOrder(values, multimap().asMap().get(key));
      assertFalse(multimap().isEmpty());
    } else {
      assertNull(multimap().asMap().get(key));
    }

    assertEquals(values.size(), multimap().get(key).size());
    assertEquals(values.size() > 0, multimap().containsKey(key));
    assertEquals(values.size() > 0, multimap().keySet().contains(key));
    assertEquals(values.size() > 0, multimap().keys().contains(key));
  }
}

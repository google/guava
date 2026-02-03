/*
 * Copyright (C) 2025 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractMapTester;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.NullMarked;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests spliterator characteristics on concurrent map views. Verifies
 * that {@link ConcurrentMap} implementations return spliterators with the {@link
 * Spliterator#CONCURRENT} characteristic on their {@code entrySet()}, {@code keySet()}, and {@code
 * values()} views.
 *
 * <p>Can't be invoked directly; please see {@link
 * com.google.common.collect.testing.ConcurrentMapTestSuiteBuilder}.
 *
 * @author Guava Authors
 */
@GwtCompatible
@Ignore("test runners must not instantiate and run this directly, only via suites we build")
// @Ignore affects the Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
@NullMarked
public class ConcurrentMapSpliteratorTester<K, V> extends AbstractMapTester<K, V> {
  @Override
  protected ConcurrentMap<K, V> getMap() {
    return (ConcurrentMap<K, V>) super.getMap();
  }

  /**
   * Tests that the spliterator returned by {@code entrySet().spliterator()} has the {@code
   * CONCURRENT} characteristic.
   */
  public void testEntrySetSpliteratorHasConcurrent() {
    Spliterator<?> spliterator = getMap().entrySet().spliterator();
    assertTrue(
        "entrySet().spliterator() should have CONCURRENT characteristic",
        spliterator.hasCharacteristics(Spliterator.CONCURRENT));
  }

  /**
   * Tests that the spliterator returned by {@code keySet().spliterator()} has the {@code
   * CONCURRENT} characteristic.
   */
  public void testKeySetSpliteratorHasConcurrent() {
    Spliterator<?> spliterator = getMap().keySet().spliterator();
    assertTrue(
        "keySet().spliterator() should have CONCURRENT characteristic",
        spliterator.hasCharacteristics(Spliterator.CONCURRENT));
  }

  /**
   * Tests that the spliterator returned by {@code values().spliterator()} has the {@code
   * CONCURRENT} characteristic.
   */
  public void testValuesSpliteratorHasConcurrent() {
    Spliterator<?> spliterator = getMap().values().spliterator();
    assertTrue(
        "values().spliterator() should have CONCURRENT characteristic",
        spliterator.hasCharacteristics(Spliterator.CONCURRENT));
  }
}

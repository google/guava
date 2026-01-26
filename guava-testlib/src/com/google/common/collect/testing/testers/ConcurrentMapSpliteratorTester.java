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

import static com.google.common.collect.testing.Helpers.getMethod;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.AbstractMapTester;
import java.lang.reflect.Method;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.NullMarked;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests spliterator characteristics on concurrent map views. Verifies
 * that {@link ConcurrentMap} implementations return spliterators with the {@link
 * Spliterator#CONCURRENT} characteristic on their {@code entrySet()}, {@code keySet()}, and {@code
 * values()} views, and that no spliterator reports both {@code CONCURRENT} and {@code SIZED}
 * characteristics (which are mutually incompatible per the Java specification).
 *
 * <p>Can't be invoked directly; please see {@link
 * com.google.common.collect.testing.ConcurrentMapTestSuiteBuilder}.
 *
 * @author Guava Authors
 */
@GwtCompatible(emulated = true)
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

  /**
   * Tests that the spliterator returned by {@code entrySet().spliterator()} does not have both
   * {@code CONCURRENT} and {@code SIZED} characteristics, which are mutually incompatible per the
   * Java specification.
   */
  public void testEntrySetSpliteratorNotConcurrentAndSized() {
    Spliterator<?> spliterator = getMap().entrySet().spliterator();
    assertFalse(
        "entrySet().spliterator() should not have both CONCURRENT and SIZED characteristics",
        spliterator.hasCharacteristics(Spliterator.CONCURRENT)
            && spliterator.hasCharacteristics(Spliterator.SIZED));
  }

  /**
   * Tests that the spliterator returned by {@code keySet().spliterator()} does not have both {@code
   * CONCURRENT} and {@code SIZED} characteristics, which are mutually incompatible per the Java
   * specification.
   */
  public void testKeySetSpliteratorNotConcurrentAndSized() {
    Spliterator<?> spliterator = getMap().keySet().spliterator();
    assertFalse(
        "keySet().spliterator() should not have both CONCURRENT and SIZED characteristics",
        spliterator.hasCharacteristics(Spliterator.CONCURRENT)
            && spliterator.hasCharacteristics(Spliterator.SIZED));
  }

  /**
   * Tests that the spliterator returned by {@code values().spliterator()} does not have both {@code
   * CONCURRENT} and {@code SIZED} characteristics, which are mutually incompatible per the Java
   * specification.
   */
  public void testValuesSpliteratorNotConcurrentAndSized() {
    Spliterator<?> spliterator = getMap().values().spliterator();
    assertFalse(
        "values().spliterator() should not have both CONCURRENT and SIZED characteristics",
        spliterator.hasCharacteristics(Spliterator.CONCURRENT)
            && spliterator.hasCharacteristics(Spliterator.SIZED));
  }

  // Reflection methods for test suppression

  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getEntrySetSpliteratorHasConcurrentMethod() {
    return getMethod(ConcurrentMapSpliteratorTester.class, "testEntrySetSpliteratorHasConcurrent");
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getKeySetSpliteratorHasConcurrentMethod() {
    return getMethod(ConcurrentMapSpliteratorTester.class, "testKeySetSpliteratorHasConcurrent");
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getValuesSpliteratorHasConcurrentMethod() {
    return getMethod(ConcurrentMapSpliteratorTester.class, "testValuesSpliteratorHasConcurrent");
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getEntrySetSpliteratorNotConcurrentAndSizedMethod() {
    return getMethod(
        ConcurrentMapSpliteratorTester.class, "testEntrySetSpliteratorNotConcurrentAndSized");
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getKeySetSpliteratorNotConcurrentAndSizedMethod() {
    return getMethod(
        ConcurrentMapSpliteratorTester.class, "testKeySetSpliteratorNotConcurrentAndSized");
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getValuesSpliteratorNotConcurrentAndSizedMethod() {
    return getMethod(
        ConcurrentMapSpliteratorTester.class, "testValuesSpliteratorNotConcurrentAndSized");
  }
}

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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Function;
import com.google.common.collect.Interners.InternerImpl;
import com.google.common.collect.MapMakerInternalMap.Strength;
import com.google.common.testing.GcFinalization;
import com.google.common.testing.NullPointerTester;
import java.lang.ref.WeakReference;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit test for {@link Interners}.
 *
 * @author Kevin Bourrillion
 */
@NullUnmarked
public class InternersTest extends TestCase {

  public void testStrong_simplistic() {
    String canonical = "a";
    String not = new String("a");

    Interner<String> pool = Interners.newStrongInterner();
    assertThat(pool.intern(canonical)).isSameInstanceAs(canonical);
    assertThat(pool.intern(not)).isSameInstanceAs(canonical);
  }

  public void testStrong_null() {
    Interner<String> pool = Interners.newStrongInterner();
    assertThrows(NullPointerException.class, () -> pool.intern(null));
  }

  public void testStrong_builder() {
    int concurrencyLevel = 42;
    Interner<Object> interner =
        Interners.newBuilder().strong().concurrencyLevel(concurrencyLevel).build();
    InternerImpl<Object> internerImpl = (InternerImpl<Object>) interner;
    assertThat(internerImpl.map.keyStrength()).isEqualTo(Strength.STRONG);
  }

  public void testWeak_simplistic() {
    String canonical = "a";
    String not = new String("a");

    Interner<String> pool = Interners.newWeakInterner();
    assertThat(pool.intern(canonical)).isSameInstanceAs(canonical);
    assertThat(pool.intern(not)).isSameInstanceAs(canonical);
  }

  public void testWeak_null() {
    Interner<String> pool = Interners.newWeakInterner();
    assertThrows(NullPointerException.class, () -> pool.intern(null));
  }

  public void testWeak_builder() {
    int concurrencyLevel = 42;
    Interner<Object> interner =
        Interners.newBuilder().weak().concurrencyLevel(concurrencyLevel).build();
    InternerImpl<Object> internerImpl = (InternerImpl<Object>) interner;
    assertThat(internerImpl.map.keyStrength()).isEqualTo(Strength.WEAK);
    assertEquals(concurrencyLevel, internerImpl.map.concurrencyLevel);
  }


  public void testWeak_afterGC() throws InterruptedException {
    MyInteger canonical = new MyInteger(5);
    MyInteger not = new MyInteger(5);

    Interner<MyInteger> pool = Interners.newWeakInterner();
    assertThat(pool.intern(canonical)).isSameInstanceAs(canonical);

    WeakReference<MyInteger> signal = new WeakReference<>(canonical);
    canonical = null; // Hint to the JIT that canonical is unreachable

    GcFinalization.awaitClear(signal);
    assertThat(pool.intern(not)).isSameInstanceAs(not);
  }

  public void testAsFunction_simplistic() {
    String canonical = "a";
    String not = new String("a");

    Function<String, String> internerFunction = Interners.asFunction(Interners.newStrongInterner());

    assertThat(internerFunction.apply(canonical)).isSameInstanceAs(canonical);
    assertThat(internerFunction.apply(not)).isSameInstanceAs(canonical);
  }

  public void testNullPointerExceptions() {
    new NullPointerTester().testAllPublicStaticMethods(Interners.class);
  }

  public void testConcurrencyLevel_zero() {
    Interners.InternerBuilder builder = Interners.newBuilder();
    assertThrows(IllegalArgumentException.class, () -> builder.concurrencyLevel(0));
  }

  public void testConcurrencyLevel_negative() {
    Interners.InternerBuilder builder = Interners.newBuilder();
    assertThrows(IllegalArgumentException.class, () -> builder.concurrencyLevel(-42));
  }
}

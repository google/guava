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

import com.google.common.base.Function;
import com.google.common.collect.Interners.InternerImpl;
import com.google.common.collect.MapMakerInternalMap.Strength;
import com.google.common.testing.GcFinalization;
import com.google.common.testing.NullPointerTester;
import java.lang.ref.WeakReference;
import junit.framework.TestCase;

/**
 * Unit test for {@link Interners}.
 *
 * @author Kevin Bourrillion
 */
public class InternersTest extends TestCase {

  public void testStrong_simplistic() {
    String canonical = "a";
    String not = new String("a");

    Interner<String> pool = Interners.newStrongInterner();
    assertSame(canonical, pool.intern(canonical));
    assertSame(canonical, pool.intern(not));
  }

  public void testStrong_null() {
    Interner<String> pool = Interners.newStrongInterner();
    try {
      pool.intern(null);
      fail();
    } catch (NullPointerException ok) {
    }
  }

  public void testStrong_builder() {
    int concurrencyLevel = 42;
    Interner<Object> interner =
        Interners.newBuilder().strong().concurrencyLevel(concurrencyLevel).build();
    InternerImpl<Object> internerImpl = (InternerImpl<Object>) interner;
    assertEquals(Strength.STRONG, internerImpl.map.keyStrength());
  }

  public void testWeak_simplistic() {
    String canonical = "a";
    String not = new String("a");

    Interner<String> pool = Interners.newWeakInterner();
    assertSame(canonical, pool.intern(canonical));
    assertSame(canonical, pool.intern(not));
  }

  public void testWeak_null() {
    Interner<String> pool = Interners.newWeakInterner();
    try {
      pool.intern(null);
      fail();
    } catch (NullPointerException ok) {
    }
  }

  public void testWeak_builder() {
    int concurrencyLevel = 42;
    Interner<Object> interner =
        Interners.newBuilder().weak().concurrencyLevel(concurrencyLevel).build();
    InternerImpl<Object> internerImpl = (InternerImpl<Object>) interner;
    assertEquals(Strength.WEAK, internerImpl.map.keyStrength());
    assertEquals(concurrencyLevel, internerImpl.map.concurrencyLevel);
  }


  public void testWeak_afterGC() throws InterruptedException {
    Integer canonical = new Integer(5);
    Integer not = new Integer(5);

    Interner<Integer> pool = Interners.newWeakInterner();
    assertSame(canonical, pool.intern(canonical));

    WeakReference<Integer> signal = new WeakReference<>(canonical);
    canonical = null; // Hint to the JIT that canonical is unreachable

    GcFinalization.awaitClear(signal);
    assertSame(not, pool.intern(not));
  }

  public void testAsFunction_simplistic() {
    String canonical = "a";
    String not = new String("a");

    Function<String, String> internerFunction =
        Interners.asFunction(Interners.<String>newStrongInterner());

    assertSame(canonical, internerFunction.apply(canonical));
    assertSame(canonical, internerFunction.apply(not));
  }

  public void testNullPointerExceptions() {
    new NullPointerTester().testAllPublicStaticMethods(Interners.class);
  }

  public void testConcurrencyLevel_Zero() {
    Interners.InternerBuilder builder = Interners.newBuilder();
    try {
      builder.concurrencyLevel(0);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testConcurrencyLevel_Negative() {
    Interners.InternerBuilder builder = Interners.newBuilder();
    try {
      builder.concurrencyLevel(-42);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}

/*
 * Copyright (C) 2006 The Guava Authors
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

package com.google.common.reflect;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.testing.NullPointerTester;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/** Tests for {@link Reflection} */
@NullUnmarked
public class ReflectionTest extends TestCase {

  public void testGetPackageName() {
    assertThat(Reflection.getPackageName(Iterable.class)).isEqualTo("java.lang");
    assertThat(Reflection.getPackageName("java.MyType")).isEqualTo("java");
    assertThat(Reflection.getPackageName(Iterable.class.getName())).isEqualTo("java.lang");
    assertThat(Reflection.getPackageName("NoPackage")).isEqualTo("");
    assertThat(Reflection.getPackageName(Map.Entry.class)).isEqualTo("java.util");
  }

  public void testNewProxy() {
    Runnable runnable = Reflection.newProxy(Runnable.class, X_RETURNER);
    assertThat(runnable.toString()).isEqualTo("x");
  }

  public void testNewProxyCantWorkOnAClass() {
    assertThrows(
        IllegalArgumentException.class, () -> Reflection.newProxy(Object.class, X_RETURNER));
  }

  private static final InvocationHandler X_RETURNER =
      new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
          return "x";
        }
      };

  private static final AtomicInteger classesInitialized = new AtomicInteger(0);

  private static class A {
    static {
      classesInitialized.incrementAndGet();
    }
  }

  private static class B {
    static {
      classesInitialized.incrementAndGet();
    }
  }

  private static class C {
    static {
      classesInitialized.incrementAndGet();
    }
  }

  public void testInitialize() {
    assertEquals(
        "This test can't be included twice in the same suite.", 0, classesInitialized.get());

    Reflection.initialize(A.class);
    assertEquals(1, classesInitialized.get());

    Reflection.initialize(
        A.class, // Already initialized (above)
        B.class, C.class);
    assertEquals(3, classesInitialized.get());
  }

  public void testNullPointers() {
    new NullPointerTester().testAllPublicStaticMethods(Reflection.class);
  }
}

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

import com.google.common.testing.NullPointerTester;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import junit.framework.TestCase;

/** Tests for {@link Reflection} */
public class ReflectionTest extends TestCase {

  public void testGetPackageName() throws Exception {
    assertEquals("java.lang", Reflection.getPackageName(Iterable.class));
    assertEquals("java", Reflection.getPackageName("java.MyType"));
    assertEquals("java.lang", Reflection.getPackageName(Iterable.class.getName()));
    assertEquals("", Reflection.getPackageName("NoPackage"));
    assertEquals("java.util", Reflection.getPackageName(Map.Entry.class));
  }

  public void testNewProxy() throws Exception {
    Runnable runnable = Reflection.newProxy(Runnable.class, X_RETURNER);
    assertEquals("x", runnable.toString());
  }

  public void testNewProxyCantWorkOnAClass() throws Exception {
    try {
      Reflection.newProxy(Object.class, X_RETURNER);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  private static final InvocationHandler X_RETURNER =
      new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
          return "x";
        }
      };

  private static int classesInitialized = 0;

  private static class A {
    static {
      ++classesInitialized;
    }
  }

  private static class B {
    static {
      ++classesInitialized;
    }
  }

  private static class C {
    static {
      ++classesInitialized;
    }
  }

  public void testInitialize() {
    assertEquals("This test can't be included twice in the same suite.", 0, classesInitialized);

    Reflection.initialize(A.class);
    assertEquals(1, classesInitialized);

    Reflection.initialize(
        A.class, // Already initialized (above)
        B.class, C.class);
    assertEquals(3, classesInitialized);
  }

  public void testNullPointers() {
    new NullPointerTester().testAllPublicStaticMethods(Reflection.class);
  }
}

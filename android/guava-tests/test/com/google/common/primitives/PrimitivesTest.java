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

package com.google.common.primitives;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import java.util.Set;
import junit.framework.TestCase;

/**
 * Unit test for {@link Primitives}.
 *
 * @author Kevin Bourrillion
 */
public class PrimitivesTest extends TestCase {
  public void testIsWrapperType() {
    assertTrue(Primitives.isWrapperType(Void.class));
    assertFalse(Primitives.isWrapperType(void.class));
  }

  public void testWrap() {
    assertSame(Integer.class, Primitives.wrap(int.class));
    assertSame(Integer.class, Primitives.wrap(Integer.class));
    assertSame(String.class, Primitives.wrap(String.class));
  }

  public void testUnwrap() {
    assertSame(int.class, Primitives.unwrap(Integer.class));
    assertSame(int.class, Primitives.unwrap(int.class));
    assertSame(String.class, Primitives.unwrap(String.class));
  }

  public void testAllPrimitiveTypes() {
    Set<Class<?>> primitives = Primitives.allPrimitiveTypes();
    assertEquals(
        ImmutableSet.<Object>of(
            boolean.class,
            byte.class,
            char.class,
            double.class,
            float.class,
            int.class,
            long.class,
            short.class,
            void.class),
        primitives);

    try {
      primitives.remove(boolean.class);
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testAllWrapperTypes() {
    Set<Class<?>> wrappers = Primitives.allWrapperTypes();
    assertEquals(
        ImmutableSet.<Object>of(
            Boolean.class,
            Byte.class,
            Character.class,
            Double.class,
            Float.class,
            Integer.class,
            Long.class,
            Short.class,
            Void.class),
        wrappers);

    try {
      wrappers.remove(Boolean.class);
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Primitives.class);
  }
}

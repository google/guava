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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.NullPointerTester;
import java.util.Set;
import junit.framework.TestCase;

/**
 * Unit test for {@link Primitives}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class PrimitivesTest extends TestCase {
  public void testIsWrapperType() {
    assertThat(Primitives.isWrapperType(Void.class)).isTrue();
    assertThat(Primitives.isWrapperType(void.class)).isFalse();
  }

  public void testWrap() {
    assertThat(Primitives.wrap(int.class)).isSameInstanceAs(Integer.class);
    assertThat(Primitives.wrap(Integer.class)).isSameInstanceAs(Integer.class);
    assertThat(Primitives.wrap(String.class)).isSameInstanceAs(String.class);
  }

  public void testUnwrap() {
    assertThat(Primitives.unwrap(Integer.class)).isSameInstanceAs(int.class);
    assertThat(Primitives.unwrap(int.class)).isSameInstanceAs(int.class);
    assertThat(Primitives.unwrap(String.class)).isSameInstanceAs(String.class);
  }

  public void testAllPrimitiveTypes() {
    Set<Class<?>> primitives = Primitives.allPrimitiveTypes();
    assertThat(primitives)
        .containsExactly(
            boolean.class,
            byte.class,
            char.class,
            double.class,
            float.class,
            int.class,
            long.class,
            short.class,
            void.class);

    try {
      primitives.remove(boolean.class);
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testAllWrapperTypes() {
    Set<Class<?>> wrappers = Primitives.allWrapperTypes();
    assertThat(wrappers)
        .containsExactly(
            Boolean.class,
            Byte.class,
            Character.class,
            Double.class,
            Float.class,
            Integer.class,
            Long.class,
            Short.class,
            Void.class);

    try {
      wrappers.remove(Boolean.class);
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  @GwtIncompatible
  @J2ktIncompatible
  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Primitives.class);
  }
}

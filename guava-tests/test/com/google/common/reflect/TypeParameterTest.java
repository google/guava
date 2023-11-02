/*
 * Copyright (C) 2011 The Guava Authors
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

import static org.junit.Assert.assertThrows;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import junit.framework.TestCase;

/**
 * Unit test for {@link TypeParameter}.
 *
 * @author Ben Yu
 */
public class TypeParameterTest extends TestCase {

  public <T> void testCaptureTypeParameter() throws Exception {
    TypeVariable<?> variable = new TypeParameter<T>() {}.typeVariable;
    TypeVariable<?> expected =
        TypeParameterTest.class.getDeclaredMethod("testCaptureTypeParameter")
            .getTypeParameters()[0];
    assertEquals(expected, variable);
  }

  public void testConcreteTypeRejected() {
    assertThrows(IllegalArgumentException.class, () -> new TypeParameter<String>() {});
  }

  public <A, B> void testEquals() throws Exception {
    Method method = TypeParameterTest.class.getDeclaredMethod("testEquals");
    new EqualsTester()
        .addEqualityGroup(new TypeParameter<A>() {}, new TypeParameter<A>() {})
        .addEqualityGroup(new TypeParameter<B>() {})
        .testEquals();
  }

  public void testNullPointers() {
    new NullPointerTester().testAllPublicStaticMethods(TypeParameter.class);
  }
}

/*
 * Copyright (C) 2012 The Guava Authors
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

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import java.lang.reflect.Method;
import junit.framework.TestCase;

/**
 * Tests for {@link Parameter}.
 *
 * @author Ben Yu
 */
public class ParameterTest extends TestCase {

  public void testNulls() {
    try {
      Class.forName("java.lang.reflect.AnnotatedType");
    } catch (ClassNotFoundException runningInAndroidVm) {
      /*
       * Parameter declares a method that returns AnnotatedType, which isn't available on Android.
       * This would cause NullPointerTester, which calls Class.getDeclaredMethods, to throw
       * NoClassDefFoundError.
       */
      return;
    }
    for (Method method : ParameterTest.class.getDeclaredMethods()) {
      for (Parameter param : Invokable.from(method).getParameters()) {
        new NullPointerTester().testAllPublicInstanceMethods(param);
      }
    }
  }

  public void testEquals() {
    EqualsTester tester = new EqualsTester();
    for (Method method : ParameterTest.class.getDeclaredMethods()) {
      for (Parameter param : Invokable.from(method).getParameters()) {
        tester.addEqualityGroup(param);
      }
    }
    tester.testEquals();
  }

  @SuppressWarnings("unused")
  private void someMethod(int i, int j) {}

  @SuppressWarnings("unused")
  private void anotherMethod(int i, String s) {}
}

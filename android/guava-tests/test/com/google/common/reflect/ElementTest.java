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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import junit.framework.TestCase;

/**
 * Unit tests of {@link Element}.
 *
 * @author Ben Yu
 */
public class ElementTest extends TestCase {

  public void testPrivateField() throws Exception {
    Element element = A.field("privateField");
    assertTrue(element.isPrivate());
    assertFalse(element.isAbstract());
    assertFalse(element.isPackagePrivate());
    assertFalse(element.isProtected());
    assertFalse(element.isPublic());
    assertFalse(element.isFinal());
    assertFalse(element.isStatic());
    assertTrue(element.isAnnotationPresent(Tested.class));
  }

  public void testPackagePrivateField() throws Exception {
    Element element = A.field("packagePrivateField");
    assertFalse(element.isPrivate());
    assertTrue(element.isPackagePrivate());
    assertFalse(element.isProtected());
    assertFalse(element.isPublic());
    assertFalse(element.isFinal());
    assertFalse(element.isStatic());
    assertTrue(element.isAnnotationPresent(Tested.class));
  }

  public void testProtectedField() throws Exception {
    Element element = A.field("protectedField");
    assertFalse(element.isPrivate());
    assertFalse(element.isPackagePrivate());
    assertTrue(element.isProtected());
    assertFalse(element.isPublic());
    assertFalse(element.isFinal());
    assertFalse(element.isStatic());
    assertTrue(element.isAnnotationPresent(Tested.class));
  }

  public void testPublicField() throws Exception {
    Element element = A.field("publicField");
    assertFalse(element.isPrivate());
    assertFalse(element.isPackagePrivate());
    assertFalse(element.isProtected());
    assertTrue(element.isPublic());
    assertFalse(element.isFinal());
    assertFalse(element.isStatic());
    assertTrue(element.isAnnotationPresent(Tested.class));
  }

  public void testFinalField() throws Exception {
    Element element = A.field("finalField");
    assertTrue(element.isFinal());
    assertFalse(element.isStatic());
    assertTrue(element.isAnnotationPresent(Tested.class));
  }

  public void testStaticField() throws Exception {
    Element element = A.field("staticField");
    assertTrue(element.isStatic());
    assertTrue(element.isAnnotationPresent(Tested.class));
  }

  public void testVolatileField() throws Exception {
    Element element = A.field("volatileField");
    assertTrue(element.isVolatile());
  }

  public void testTransientField() throws Exception {
    Element element = A.field("transientField");
    assertTrue(element.isTransient());
  }

  public void testConstructor() throws Exception {
    Element element = A.constructor();
    assertTrue(element.isPublic());
    assertFalse(element.isPackagePrivate());
    assertFalse(element.isAbstract());
    assertFalse(element.isStatic());
    assertTrue(element.isAnnotationPresent(Tested.class));
  }

  public void testAbstractMethod() throws Exception {
    Element element = A.method("abstractMethod");
    assertTrue(element.isPackagePrivate());
    assertTrue(element.isAbstract());
    assertFalse(element.isFinal());
    assertTrue(element.isAnnotationPresent(Tested.class));
  }

  public void testOverridableMethod() throws Exception {
    Element element = A.method("overridableMethod");
    assertTrue(element.isPackagePrivate());
    assertFalse(element.isAbstract());
    assertFalse(element.isFinal());
    assertTrue(element.isAnnotationPresent(Tested.class));
  }

  public void testPrivateMethod() throws Exception {
    Element element = A.method("privateMethod");
    assertFalse(element.isAbstract());
    assertTrue(element.isPrivate());
    assertFalse(element.isPackagePrivate());
    assertFalse(element.isPublic());
    assertFalse(element.isProtected());
    assertTrue(element.isAnnotationPresent(Tested.class));
  }

  public void testProtectedMethod() throws Exception {
    Element element = A.method("protectedMethod");
    assertFalse(element.isAbstract());
    assertFalse(element.isPrivate());
    assertFalse(element.isPackagePrivate());
    assertFalse(element.isFinal());
    assertFalse(element.isPublic());
    assertTrue(element.isProtected());
    assertTrue(element.isAnnotationPresent(Tested.class));
  }

  public void testFinalMethod() throws Exception {
    Element element = A.method("publicFinalMethod");
    assertFalse(element.isAbstract());
    assertFalse(element.isPrivate());
    assertTrue(element.isFinal());
    assertTrue(element.isPublic());
    assertTrue(element.isAnnotationPresent(Tested.class));
  }

  public void testNativeMethod() throws Exception {
    Element element = A.method("nativeMethod");
    assertTrue(element.isNative());
    assertTrue(element.isPackagePrivate());
  }

  public void testSynchronizedMethod() throws Exception {
    Element element = A.method("synchronizedMethod");
    assertTrue(element.isSynchronized());
  }

  public void testUnannotatedMethod() throws Exception {
    Element element = A.method("notAnnotatedMethod");
    assertFalse(element.isAnnotationPresent(Tested.class));
  }

  public void testEquals() throws Exception {
    new EqualsTester()
        .addEqualityGroup(A.field("privateField"), A.field("privateField"))
        .addEqualityGroup(A.field("publicField"))
        .addEqualityGroup(A.constructor(), A.constructor())
        .addEqualityGroup(A.method("privateMethod"), A.method("privateMethod"))
        .addEqualityGroup(A.method("publicFinalMethod"))
        .testEquals();
  }

  public void testNulls() {
    new NullPointerTester().testAllPublicStaticMethods(Element.class);
  }

  @Retention(RetentionPolicy.RUNTIME)
  private @interface Tested {}

  private abstract static class A {
    @Tested private boolean privateField;
    @Tested int packagePrivateField;
    @Tested protected int protectedField;
    @Tested public String publicField;
    @Tested private static Iterable<String> staticField;
    @Tested private final Object finalField;
    private volatile char volatileField;
    private transient long transientField;

    @Tested
    public A(Object finalField) {
      this.finalField = finalField;
    }

    @Tested
    abstract void abstractMethod();

    @Tested
    void overridableMethod() {}

    @Tested
    protected void protectedMethod() {}

    @Tested
    private void privateMethod() {}

    @Tested
    public final void publicFinalMethod() {}

    void notAnnotatedMethod() {}

    static Element field(String name) throws Exception {
      Element element = new Element(A.class.getDeclaredField(name));
      assertEquals(name, element.getName());
      assertEquals(A.class, element.getDeclaringClass());
      return element;
    }

    static Element constructor() throws Exception {
      Constructor<?> constructor = A.class.getDeclaredConstructor(Object.class);
      Element element = new Element(constructor);
      assertEquals(constructor.getName(), element.getName());
      assertEquals(A.class, element.getDeclaringClass());
      return element;
    }

    static Element method(String name, Class<?>... parameterTypes) throws Exception {
      Element element = new Element(A.class.getDeclaredMethod(name, parameterTypes));
      assertEquals(name, element.getName());
      assertEquals(A.class, element.getDeclaringClass());
      return element;
    }

    native void nativeMethod();

    synchronized void synchronizedMethod() {}
  }
}

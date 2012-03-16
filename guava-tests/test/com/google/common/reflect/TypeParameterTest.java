// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.reflect;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.List;

/**
 * Unit test for {@link TypeParameter}.
 *
 * @author benyu@google.com (Ben Yu)
 */
public class TypeParameterTest extends TestCase {

  public <T> void testCaptureTypeParameter() throws Exception {
    TypeVariable<?> variable = new TypeParameter<T>() {}.typeVariable;
    TypeVariable<?> expected = TypeParameterTest.class
        .getDeclaredMethod("testCaptureTypeParameter")
        .getTypeParameters()[0];
    assertEquals(expected, variable);
  }
  
  public void testConcreteTypeRejected() {
    try {
      new TypeParameter<String>() {};
      fail();
    } catch (IllegalArgumentException expected) {}
  }
  
  public <A, B> void testEquals() throws Exception {
    Method method = TypeParameterTest.class.getDeclaredMethod("testEquals");
    TypeVariable<?> a = method.getTypeParameters()[0];
    TypeVariable<?> b = method.getTypeParameters()[1];
    new EqualsTester()
        .addEqualityGroup(
            new TypeParameter<A>() {}, new TypeParameter<A>() {}, TypeParameter.of(a))
        .addEqualityGroup(new TypeParameter<B>() {}, TypeParameter.of(b))
        .testEquals();
  }

  public void testNullPointers() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(TypeVariable.class, Types.newTypeVariable(List.class, "E"));
    tester.testAllPublicStaticMethods(TypeParameter.class);
  }
}

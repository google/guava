/*
 * Copyright (C) 2010 Google Inc.
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
package com.google.common.testing;

import junit.framework.TestCase;

/**
 * A test for object equality assertions.
 * 
 * @author cgruber@google.com (Christian Edward Gruber)
 *
 */
public class ObjectEqualityAssertTest extends TestCase {
  
  //
  // Test Identity/Sameness Assertions
  //

  public void testAssertSameSuccessfully() {
    Object o1 = new Object();
    Assert.assertSame(o1, o1);
  }
  
  public void testAssertSameButFail() {
    try {
      Assert.assertSame(new Object(), new Object());
    } catch (AssertionFailedError e) {
      // Success!
    }
  }
  
  public void testAssertNotSameSuccessfully() {
    Assert.assertNotSame(new Object(), new Object());
  }
  
  public void testAssertNotSameButFail() {
    try {
      Object o1 = new Object();
      Assert.assertNotSame(o1, o1);
    } catch (AssertionFailedError e) {
      // Success!
    }
  }

  //
  // Test Equality Assertions
  //
  
  public void testEqualsObjectSuccessfullyWithUniqueObjects() {
    String s1 = new StringBuilder().append('t').append("est").toString();
    String s2 = new StringBuilder().append("te").append("st").toString();
    Assert.assertNotSame(s1, s2);
    Assert.assertEquals(s1, s2);
  }
  
  public void testEqualsObjectSuccessfullyWithIdenticalObjects() {
    String s1 = "test", s2 = s1;
    Assert.assertSame(s1, s2);
    Assert.assertEquals(s1, s2);
  }
  
  public void testEqualsObjectButFailWithUncomparableObjects() {
    Object o1 = new Object(), o2 = new Object();
    try {
      Assert.assertEquals(o1, o2);
    } catch (AssertionFailedError e) {
      // Success!
    }
  }

  public void testEqualsObjectButFailWithUniqueComparableObjects() {
    String s1 = "test1", s2 = "test2";
    try {
      Assert.assertEquals(s1, s2);
    } catch (AssertionFailedError e) {
      // Success!
    }
  }

  //
  // Test Inequality Assertions
  //

  public void testNotEqualsObjectSuccessfullyWithUniqueComparableObjects() {
    String s1 = "test1", s2 = "test2";
    Assert.assertNotSame(s1, s2);
    Assert.assertNotEquals(s1, s2);
  }
  
  public void testNotEqualsObjectButFailWithUncomparableObjects() {
    Object o1 = new Object(), o2 = new Object();
    Assert.assertNotEquals(o1, o2);
  }
  
  public void testNotEqualsObjectSuccessfullyWithSameObjects() {
    String s1 = "test", s2 = s1;
    Assert.assertSame(s1, s2);
    try {
      Assert.assertEquals(s1, s2);
    } catch (AssertionFailedError e) {
      // Success!
    }
  }
  
  public void testNotEqualsObjectButFail() {
    String s1 = new StringBuilder().append('t').append("est").toString();
    String s2 = new StringBuilder().append("te").append("st").toString();
    try {
      Assert.assertEquals(s1, s2);
    } catch (AssertionFailedError e) {
      // Success!
    }
  }
  


}

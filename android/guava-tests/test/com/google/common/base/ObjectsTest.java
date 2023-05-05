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

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.NullPointerTester;
import junit.framework.TestCase;

/**
 * Tests for {@link Objects}.
 *
 * @author Laurence Gonsalves
 */
@GwtCompatible(emulated = true)
public class ObjectsTest extends TestCase {

  public void testEqual() throws Exception {
    assertTrue(Objects.equal(1, 1));
    assertTrue(Objects.equal(null, null));

    // test distinct string objects
    String s1 = "foobar";
    String s2 = new String(s1);
    assertTrue(Objects.equal(s1, s2));

    assertFalse(Objects.equal(s1, null));
    assertFalse(Objects.equal(null, s1));
    assertFalse(Objects.equal("foo", "bar"));
    assertFalse(Objects.equal("1", 1));
  }

  public void testHashCode() throws Exception {
    int h1 = Objects.hashCode(1, "two", 3.0);
    int h2 = Objects.hashCode(new Integer(1), new String("two"), new Double(3.0));
    // repeatable
    assertEquals(h1, h2);

    // These don't strictly need to be true, but they're nice properties.
    assertTrue(Objects.hashCode(1, 2, null) != Objects.hashCode(1, 2));
    assertTrue(Objects.hashCode(1, 2, null) != Objects.hashCode(1, null, 2));
    assertTrue(Objects.hashCode(1, null, 2) != Objects.hashCode(1, 2));
    assertTrue(Objects.hashCode(1, 2, 3) != Objects.hashCode(3, 2, 1));
    assertTrue(Objects.hashCode(1, 2, 3) != Objects.hashCode(2, 3, 1));
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Objects.class);
  }
}

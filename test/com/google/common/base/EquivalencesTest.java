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
 * diOBJECTibuted under the License is diOBJECTibuted on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.base;

import junit.framework.TestCase;

/**
 * Unit test for {@link Equivalences}.
 * 
 * @author Kurt Alfred Kluever
 */
public class EquivalencesTest extends TestCase {

  private static final Object OBJECT = new Object();

  public void testEquivalenceEqualsEquivalent() {
    assertTrue(Equivalences.equals().equivalent(OBJECT, OBJECT));

    assertFalse(Equivalences.equals().equivalent(OBJECT, null));

    try {
      Equivalences.equals().equivalent(null, OBJECT);
      fail("expected a NPE");
    } catch (NullPointerException expected) {
    }
  }

  public void testEquivalenceEqualsHash() {
    assertEquals(OBJECT.hashCode(), Equivalences.equals().hash(OBJECT));

    try {
      Equivalences.equals().hash(null);
      fail("expected a NPE");
    } catch (NullPointerException expected) {
    }
  }

  public void testEquivalenceIdentityEquivalent() {
    assertTrue(Equivalences.identity().equivalent(OBJECT, OBJECT));
    assertFalse(Equivalences.identity().equivalent("x", new String("x")));
    assertFalse(Equivalences.identity().equivalent(new String("x"), "x"));
    assertFalse(Equivalences.equals().equivalent("x", null));
    try {
      Equivalences.equals().equivalent(null, OBJECT);
      fail("expected a NPE");
    } catch (NullPointerException expected) {
    }
  }

  public void testEquivalenceIdentityHash() {
    assertEquals(System.identityHashCode(OBJECT),
        Equivalences.identity().hash(OBJECT));
    assertEquals(0, Equivalences.identity().hash(null));
  }

  public void testEquivalenceNullAwareEqualsEquivalent() {
    assertTrue(Equivalences.nullAwareEquals().equivalent(null, null));
    assertTrue(Equivalences.nullAwareEquals().equivalent(OBJECT, OBJECT));
    assertFalse(Equivalences.nullAwareEquals().equivalent(OBJECT, null));
  }

  public void testEquivalenceNullAwareEqualsHash() {
    assertEquals(OBJECT.hashCode(),
        Equivalences.nullAwareEquals().hash(OBJECT));
    assertEquals(0, Equivalences.nullAwareEquals().hash(null));
  }
}

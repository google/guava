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

package com.google.common.collect;

import static java.util.Arrays.asList;
import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.Collections;

/**
 * Common tests for a {@code Collection}.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 */
@GwtCompatible(emulated = true)
public abstract class AbstractCollectionTest extends TestCase {

  protected abstract <E> Collection<E> create();

  protected Collection<String> c;

  // public for GWT
  @Override public void setUp() throws Exception {
    super.setUp();
    c = create();
  }

  public void testIsEmptyYes() {
    assertTrue(c.isEmpty());
  }

  public void testIsEmptyNo() {
    c.add("a");
    assertFalse(c.isEmpty());
  }

  public void testAddOne() {
    assertTrue(c.add("a"));
    assertContents("a");
  }

  public void testAddSeveralTimes() {
    assertTrue(c.add("a"));
    assertTrue(c.add("b"));
    c.add("a");
    c.add("b");
    assertTrue(c.contains("a"));
    assertTrue(c.contains("b"));
  }

  public void testRemoveOneFromNoneStandard() {
    assertFalse(c.remove("a"));
    assertContents();
  }

  public void testRemoveOneFromOneStandard() {
    c.add("a");
    assertTrue(c.remove("a"));
    assertContents();
  }

  public void testContainsNo() {
    c.add("a");
    assertFalse(c.contains("b"));
  }

  public void testContainsOne() {
    c.add("a");
    assertTrue(c.contains(new String("a")));
  }

  public void testContainsAfterRemoval() {
    c.add("a");
    c.remove("a");
    assertFalse(c.contains("a"));
  }

  public void testContainsAllVacuous() {
    assertTrue(c.containsAll(Collections.emptySet()));
  }

  public void testRemoveAllVacuous() {
    assertFalse(c.removeAll(Collections.emptySet()));
  }

  public void testRetainAllVacuous() {
    assertFalse(c.retainAll(asList("a")));
    assertContents();
  }

  public void testRetainAllOfNothing() {
    c.add("a");
    assertTrue(c.retainAll(Collections.emptySet()));
    assertContents();
  }

  public void testClearNothing() {
    c.clear();
    assertContents();
  }

  public void testClear() {
    c = createSample();
    c.clear();
    assertContents();
  }

  public void testEqualsNo() {
    c.add("a");

    Collection<String> c2 = create();
    c2.add("b");

    assertFalse(c.equals(c2));
  }

  public void testEqualsYes() {
    c.add("a");
    c.add("b");
    c.add("b");

    Collection<String> c2 = create();
    c2.add("a");
    c2.add("b");
    c2.add("b");

    assertEquals(c, c2);
  }

  public void testEqualsSelf() {
    c.add("a");
    c.add("b");
    c.add("b");

    assertEquals(c, c);
  }

  public void testEqualsTricky() {
    c.add("a");
    c.add("a");

    Collection<String> c2 = create();
    c2.add("a");
    c2.add("a");
    c2.add("b");
    c2.add("b");
    c2.remove("b");
    c2.remove("b");

    assertEquals(c, c2);
  }

  public void testEqualsPartial() {
    c.add("a");
    c.add("b");

    Collection<String> c2 = create();
    c2.add("a");
    c2.add("c");

    assertFalse(c.equals(c2));

    Collection<String> c3 = create();
    c2.add("b");
    c2.add("c");

    assertFalse(c2.equals(c3));
  }

  public void testEqualsDifferentTypes() {
    c.add("a");
    assertFalse(c.equals("a"));
  }

  public void testToArrayOne() {
    c.add("a");
    String[] array = new String[3];
    assertSame(array, c.toArray(array));
    assertEquals("a", array[0]);
    assertNull(array[1]);
  }

  @GwtIncompatible("NullPointerTester")
  public void testNullPointerExceptions() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(c);
  }

  protected Collection<String> createSample() {
    Collection<String> c = create();
    c.add("a");
    c.add("b");
    c.add("b");
    c.add("c");
    c.add("d");
    c.add("d");
    c.add("d");
    return c;
  }

  protected void assertContents(String... expected) {
    ASSERT.that(c).hasContentsAnyOrder(expected);
  }
}

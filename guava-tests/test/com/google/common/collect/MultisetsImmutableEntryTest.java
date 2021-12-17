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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multiset.Entry;
import java.util.Collections;
import junit.framework.TestCase;

/**
 * Tests for {@link Multisets#immutableEntry}.
 *
 * @author Mike Bostock
 */
@GwtCompatible
public class MultisetsImmutableEntryTest extends TestCase {
  private static final String NE = null;

  private static <E> Entry<E> entry(final E element, final int count) {
    return Multisets.immutableEntry(element, count);
  }

  private static <E> Entry<E> control(E element, int count) {
    return HashMultiset.create(Collections.nCopies(count, element)).entrySet().iterator().next();
  }

  public void testToString() {
    assertEquals("foo", entry("foo", 1).toString());
    assertEquals("bar x 2", entry("bar", 2).toString());
  }

  public void testToStringNull() {
    assertEquals("null", entry(NE, 1).toString());
    assertEquals("null x 2", entry(NE, 2).toString());
  }

  public void testEquals() {
    assertEquals(control("foo", 1), entry("foo", 1));
    assertEquals(control("bar", 2), entry("bar", 2));
    assertFalse(control("foo", 1).equals(entry("foo", 2)));
    assertFalse(entry("foo", 1).equals(control("bar", 1)));
    assertFalse(entry("foo", 1).equals(new Object()));
    assertFalse(entry("foo", 1).equals(null));
  }

  public void testEqualsNull() {
    assertEquals(control(NE, 1), entry(NE, 1));
    assertFalse(control(NE, 1).equals(entry(NE, 2)));
    assertFalse(entry(NE, 1).equals(control("bar", 1)));
    assertFalse(entry(NE, 1).equals(new Object()));
    assertFalse(entry(NE, 1).equals(null));
  }

  public void testHashCode() {
    assertEquals(control("foo", 1).hashCode(), entry("foo", 1).hashCode());
    assertEquals(control("bar", 2).hashCode(), entry("bar", 2).hashCode());
  }

  public void testHashCodeNull() {
    assertEquals(control(NE, 1).hashCode(), entry(NE, 1).hashCode());
  }

  public void testNegativeCount() {
    try {
      entry("foo", -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}

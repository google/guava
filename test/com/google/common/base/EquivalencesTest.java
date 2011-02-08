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

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import java.util.List;

/**
 * Unit test for {@link Equivalences}.
 * 
 * @author Kurt Alfred Kluever
 */
@GwtCompatible
public class EquivalencesTest extends TestCase {

  private static final Object OBJECT = (Integer) 42;

  public void testEqualsEquivalent() {
    assertTrue(Equivalences.equals().equivalent(null, null));
    assertTrue(Equivalences.equals().equivalent(OBJECT, OBJECT));
    assertTrue(Equivalences.equals().equivalent(((Integer) 42), OBJECT));
    assertTrue(Equivalences.equals().equivalent(OBJECT, ((Integer) 42)));
    assertFalse(Equivalences.equals().equivalent(OBJECT, null));
    assertFalse(Equivalences.equals().equivalent(null, OBJECT));
  }

  public void testEqualsHash() {
    assertEquals(OBJECT.hashCode(), Equivalences.equals().hash(OBJECT));
    assertEquals(0, Equivalences.equals().hash(null));
  }

  public void testIdentityEquivalent() {
    assertTrue(Equivalences.identity().equivalent(null, null));
    assertTrue(Equivalences.identity().equivalent(OBJECT, OBJECT));
    assertTrue(Equivalences.identity().equivalent(12L, 12L));
    assertFalse(Equivalences.identity().equivalent(OBJECT, null));
    assertFalse(Equivalences.identity().equivalent(null, OBJECT));
    assertFalse(Equivalences.identity().equivalent(12L, new Long(12L)));
    assertFalse(Equivalences.identity().equivalent(new Long(12L), 12L));
  }

  public void testIdentityHash() {
    assertEquals(System.identityHashCode(OBJECT), Equivalences.identity().hash(OBJECT));
    assertEquals(0, Equivalences.identity().hash(null));
  }

  public void testPairwiseEquivalent_equivalent() {
    Equivalence<Iterable<String>> pairwise = Equivalences.pairwise(Equivalences.equals());
    List<String> empty = ImmutableList.of();
    List<String> a = ImmutableList.of("a");
    List<String> b = ImmutableList.of("b");
    List<String> ab = ImmutableList.of("a", "b");

    assertTrue(pairwise.equivalent(empty, empty));
    assertTrue(pairwise.equivalent(a, a));
    assertTrue(pairwise.equivalent(b, b));
    assertTrue(pairwise.equivalent(ab, ab));
  }

  public void testPairwiseEquivalent_nonEquivalent() {
    Equivalence<Iterable<String>> pairwise = Equivalences.pairwise(Equivalences.equals());
    List<String> empty = ImmutableList.of();
    List<String> a = ImmutableList.of("a");
    List<String> b = ImmutableList.of("b");
    List<String> ab = ImmutableList.of("a", "b");

    assertFalse(pairwise.equivalent(empty, ab));
    assertFalse(pairwise.equivalent(a, ab));
    assertFalse(pairwise.equivalent(b, ab));

    assertFalse(pairwise.equivalent(a, b));
    assertFalse(pairwise.equivalent(b, a));

    assertFalse(pairwise.equivalent(ab, empty));
    assertFalse(pairwise.equivalent(ab, a));
    assertFalse(pairwise.equivalent(ab, b));
  }

  public void testPairwiseEquivalent_null() {
    Equivalence<Iterable<String>> pairwise = Equivalences.pairwise(Equivalences.equals());
    List<String> empty = ImmutableList.of();
    List<String> a = ImmutableList.of("a");

    assertTrue(pairwise.equivalent(null, null));
    assertFalse(pairwise.equivalent(null, empty));
    assertFalse(pairwise.equivalent(empty, null));
    assertFalse(pairwise.equivalent(null, a));
    assertFalse(pairwise.equivalent(a, null));
  }

  public void testPairwiseHash() {
    Equivalence<Iterable<String>> pairwise = Equivalences.pairwise(Equivalences.equals());

    assertEquals(0, pairwise.hash(null));
    assertEquals(pairwise.hash(ImmutableList.of("a")), pairwise.hash(ImmutableSet.of("a")));
    assertEquals(pairwise.hash(ImmutableList.of("a", "b", "c")),
        pairwise.hash(Lists.newArrayList("a", "b", "c")));
  }
}

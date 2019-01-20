/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ADD;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Ignore;

/**
 * Tests for {@code Multiset.add}.
 *
 * @author Jared Levy
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class MultisetAddTester<E> extends AbstractMultisetTester<E> {
  @CollectionFeature.Require(absent = SUPPORTS_ADD)
  public void testAddUnsupported() {
    try {
      getMultiset().add(e0());
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddMeansAddOne() {
    int originalCount = getMultiset().count(e0());
    assertTrue(getMultiset().add(e0()));
    assertEquals(originalCount + 1, getMultiset().count(e0()));
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddOccurrencesZero() {
    int originalCount = getMultiset().count(e0());
    assertEquals("old count", originalCount, getMultiset().add(e0(), 0));
    expectUnchanged();
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddOccurrences() {
    int originalCount = getMultiset().count(e0());
    assertEquals("old count", originalCount, getMultiset().add(e0(), 2));
    assertEquals("old count", originalCount + 2, getMultiset().count(e0()));
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddSeveralTimes() {
    int originalCount = getMultiset().count(e0());
    assertEquals(originalCount, getMultiset().add(e0(), 2));
    assertTrue(getMultiset().add(e0()));
    assertEquals(originalCount + 3, getMultiset().add(e0(), 1));
    assertEquals(originalCount + 4, getMultiset().count(e0()));
  }

  @CollectionFeature.Require(absent = SUPPORTS_ADD)
  public void testAddOccurrences_unsupported() {
    try {
      getMultiset().add(e0(), 2);
      fail("unsupported multiset.add(E, int) didn't throw exception");
    } catch (UnsupportedOperationException required) {
    }
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddOccurrencesNegative() {
    try {
      getMultiset().add(e0(), -1);
      fail("multiset.add(E, -1) didn't throw an exception");
    } catch (IllegalArgumentException required) {
    }
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddTooMany() {
    getMultiset().add(e3(), Integer.MAX_VALUE);
    try {
      getMultiset().add(e3());
      fail();
    } catch (IllegalArgumentException expected) {
    }
    assertEquals(Integer.MAX_VALUE, getMultiset().count(e3()));
    assertEquals(Integer.MAX_VALUE, getMultiset().size());
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddAll_emptySet() {
    assertFalse(getMultiset().addAll(Collections.<E>emptySet()));
    expectUnchanged();
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddAll_emptyMultiset() {
    assertFalse(getMultiset().addAll(getSubjectGenerator().create()));
    expectUnchanged();
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddAll_nonEmptyList() {
    assertTrue(getMultiset().addAll(Arrays.asList(e3(), e4(), e3())));
    expectAdded(e3(), e4(), e3());
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testAddAll_nonEmptyMultiset() {
    assertTrue(getMultiset().addAll(getSubjectGenerator().create(e3(), e4(), e3())));
    expectAdded(e3(), e4(), e3());
  }
}

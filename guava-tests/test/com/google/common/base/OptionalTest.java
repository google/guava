/*
 * Copyright (C) 2011 The Guava Authors
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

import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Unit test for {@link Optional}.
 *
 * @author Kurt Alfred Kluever
 */
@GwtCompatible(emulated = true)
public final class OptionalTest extends TestCase {
  public void testAbsent() {
    Optional<String> optionalName = Optional.absent();
    assertFalse(optionalName.isPresent());
  }

  public void testOf() {
    assertEquals("training", Optional.of("training").get());
  }

  public void testOf_null() {
    try {
      Optional.of(null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testFromNullable() {
    Optional<String> optionalName = Optional.fromNullable("bob");
    assertEquals("bob", optionalName.get());
  }

  public void testFromNullable_null() {
    // not promised by spec, but easier to test
    assertSame(Optional.absent(), Optional.fromNullable(null));
  }

  public void testIsPresent_no() {
    assertFalse(Optional.absent().isPresent());
  }

  public void testIsPresent_yes() {
    assertTrue(Optional.of("training").isPresent());
  }

  public void testGet_absent() {
    Optional<String> optional = Optional.absent();
    try {
      optional.get();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testGet_present() {
    assertEquals("training", Optional.of("training").get());
  }

  public void testOr_T_present() {
    assertEquals("a", Optional.of("a").or("default"));
  }

  public void testOr_T_absent() {
    assertEquals("default", Optional.absent().or("default"));
  }

  public void testOr_Supplier_present() {
    assertEquals("a", Optional.of("a").or(Suppliers.ofInstance("fallback")));
  }

  public void testOr_Supplier_absent() {
    assertEquals("fallback", Optional.absent().or(Suppliers.ofInstance("fallback")));
  }

  public void testOr_NullSupplier_absent() {
    Supplier<Object> nullSupplier = Suppliers.ofInstance(null);
    Optional<Object> absentOptional = Optional.absent();
    try {
      absentOptional.or(nullSupplier);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testOr_Optional_present() {
    assertEquals(Optional.of("a"), Optional.of("a").or(Optional.of("fallback")));
  }

  public void testOr_Optional_absent() {
    assertEquals(Optional.of("fallback"), Optional.absent().or(Optional.of("fallback")));
  }

  public void testOrNull_present() {
    assertEquals("a", Optional.of("a").orNull());
  }

  public void testOrNull_absent() {
    assertNull(Optional.absent().orNull());
  }
  
  public void testAsSet_present() {
    Set<String> expected = Collections.singleton("a");
    assertEquals(expected, Optional.of("a").asSet());
  }
  
  public void testAsSet_absent() {
    assertTrue("Returned set should be empty", Optional.absent().asSet().isEmpty());
  }
  
  public void testAsSet_presentIsImmutable() {
    Set<String> presentAsSet = Optional.of("a").asSet();
    try {
      presentAsSet.add("b");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testAsSet_absentIsImmutable() {
    Set<Object> absentAsSet = Optional.absent().asSet();
    try {
      absentAsSet.add("foo");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  // TODO(kevinb): use EqualsTester

  public void testEqualsAndHashCode_absent() {
    assertEquals(Optional.<String>absent(), Optional.<Integer>absent());
    assertEquals(Optional.absent().hashCode(), Optional.absent().hashCode());
  }

  public void testEqualsAndHashCode_present() {
    assertEquals(Optional.of("training"), Optional.of("training"));
    assertFalse(Optional.of("a").equals(Optional.of("b")));
    assertFalse(Optional.of("a").equals(Optional.absent()));
    assertEquals(Optional.of("training").hashCode(), Optional.of("training").hashCode());
  }

  public void testToString_absent() {
    assertEquals("Optional.absent()", Optional.absent().toString());
  }

  public void testToString_present() {
    assertEquals("Optional.of(training)", Optional.of("training").toString());
  }

  public void testPresentInstances_allPresent() {
    List<Optional<String>> optionals =
        ImmutableList.of(Optional.of("a"), Optional.of("b"), Optional.of("c"));
    ASSERT.that(Optional.presentInstances(optionals)).hasContentsInOrder("a", "b", "c");
  }
  
  public void testPresentInstances_allAbsent() {
    List<Optional<Object>> optionals =
        ImmutableList.of(Optional.absent(), Optional.absent());
    ASSERT.that(Optional.presentInstances(optionals)).isEmpty();
  }
  
  public void testPresentInstances_somePresent() {
    List<Optional<String>> optionals =
        ImmutableList.of(Optional.of("a"), Optional.<String>absent(), Optional.of("c"));
    ASSERT.that(Optional.presentInstances(optionals)).hasContentsInOrder("a", "c");
  }
  
  public void testPresentInstances_callingIteratorTwice() {
    List<Optional<String>> optionals =
        ImmutableList.of(Optional.of("a"), Optional.<String>absent(), Optional.of("c"));
    Iterable<String> onlyPresent = Optional.presentInstances(optionals);
    ASSERT.that(onlyPresent).hasContentsInOrder("a", "c");
    ASSERT.that(onlyPresent).hasContentsInOrder("a", "c");
  }

  @GwtIncompatible("SerializableTester")
  public void testSerialization() {
    SerializableTester.reserializeAndAssert(Optional.absent());
    SerializableTester.reserializeAndAssert(Optional.of("foo"));
  }

  @GwtIncompatible("NullPointerTester")
  public void testNullPointers() throws Exception {
    NullPointerTester npTester = new NullPointerTester();
    npTester.testAllPublicConstructors(Optional.class);
    npTester.testAllPublicStaticMethods(Optional.class);
    npTester.testAllPublicInstanceMethods(Optional.absent());
    npTester.testAllPublicInstanceMethods(Optional.of("training"));
  }
}

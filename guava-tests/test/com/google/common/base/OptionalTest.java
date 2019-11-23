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

import static com.google.common.testing.SerializableTester.reserialize;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;

/**
 * Unit test for {@link Optional}.
 *
 * @author Kurt Alfred Kluever
 */
@GwtCompatible(emulated = true)
public final class OptionalTest extends TestCase {
  public void testToJavaUtil_static() {
    assertNull(Optional.toJavaUtil(null));
    assertEquals(java.util.Optional.empty(), Optional.toJavaUtil(Optional.absent()));
    assertEquals(java.util.Optional.of("abc"), Optional.toJavaUtil(Optional.of("abc")));
  }

  public void testToJavaUtil_instance() {
    assertEquals(java.util.Optional.empty(), Optional.absent().toJavaUtil());
    assertEquals(java.util.Optional.of("abc"), Optional.of("abc").toJavaUtil());
  }

  public void testFromJavaUtil() {
    assertNull(Optional.fromJavaUtil(null));
    assertEquals(Optional.absent(), Optional.fromJavaUtil(java.util.Optional.empty()));
    assertEquals(Optional.of("abc"), Optional.fromJavaUtil(java.util.Optional.of("abc")));
  }

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

  public void testOr_supplier_present() {
    assertEquals("a", Optional.of("a").or(Suppliers.ofInstance("fallback")));
  }

  public void testOr_supplier_absent() {
    assertEquals("fallback", Optional.absent().or(Suppliers.ofInstance("fallback")));
  }

  public void testOr_nullSupplier_absent() {
    Supplier<Object> nullSupplier = Suppliers.ofInstance(null);
    Optional<Object> absentOptional = Optional.absent();
    try {
      absentOptional.or(nullSupplier);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testOr_nullSupplier_present() {
    Supplier<String> nullSupplier = Suppliers.ofInstance(null);
    assertEquals("a", Optional.of("a").or(nullSupplier));
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

  public void testTransform_absent() {
    assertEquals(Optional.absent(), Optional.absent().transform(Functions.identity()));
    assertEquals(Optional.absent(), Optional.absent().transform(Functions.toStringFunction()));
  }

  public void testTransform_presentIdentity() {
    assertEquals(Optional.of("a"), Optional.of("a").transform(Functions.identity()));
  }

  public void testTransform_presentToString() {
    assertEquals(Optional.of("42"), Optional.of(42).transform(Functions.toStringFunction()));
  }

  public void testTransform_present_functionReturnsNull() {
    try {
      Optional<String> unused =
          Optional.of("a")
              .transform(
                  new Function<String, String>() {
                    @Override
                    public String apply(String input) {
                      return null;
                    }
                  });
      fail("Should throw if Function returns null.");
    } catch (NullPointerException expected) {
    }
  }

  public void testTransform_absent_functionReturnsNull() {
    assertEquals(
        Optional.absent(),
        Optional.absent()
            .transform(
                new Function<Object, Object>() {
                  @Override
                  public Object apply(Object input) {
                    return null;
                  }
                }));
  }

  public void testEqualsAndHashCode() {
    new EqualsTester()
        .addEqualityGroup(Optional.absent(), reserialize(Optional.absent()))
        .addEqualityGroup(Optional.of(new Long(5)), reserialize(Optional.of(new Long(5))))
        .addEqualityGroup(Optional.of(new Long(42)), reserialize(Optional.of(new Long(42))))
        .testEquals();
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
    assertThat(Optional.presentInstances(optionals)).containsExactly("a", "b", "c").inOrder();
  }

  public void testPresentInstances_allAbsent() {
    List<Optional<Object>> optionals = ImmutableList.of(Optional.absent(), Optional.absent());
    assertThat(Optional.presentInstances(optionals)).isEmpty();
  }

  public void testPresentInstances_somePresent() {
    List<Optional<String>> optionals =
        ImmutableList.of(Optional.of("a"), Optional.<String>absent(), Optional.of("c"));
    assertThat(Optional.presentInstances(optionals)).containsExactly("a", "c").inOrder();
  }

  public void testPresentInstances_callingIteratorTwice() {
    List<Optional<String>> optionals =
        ImmutableList.of(Optional.of("a"), Optional.<String>absent(), Optional.of("c"));
    Iterable<String> onlyPresent = Optional.presentInstances(optionals);
    assertThat(onlyPresent).containsExactly("a", "c").inOrder();
    assertThat(onlyPresent).containsExactly("a", "c").inOrder();
  }

  public void testPresentInstances_wildcards() {
    List<Optional<? extends Number>> optionals =
        ImmutableList.<Optional<? extends Number>>of(Optional.<Double>absent(), Optional.of(2));
    Iterable<Number> onlyPresent = Optional.presentInstances(optionals);
    assertThat(onlyPresent).containsExactly(2);
  }

  private static Optional<Integer> getSomeOptionalInt() {
    return Optional.of(1);
  }

  private static FluentIterable<? extends Number> getSomeNumbers() {
    return FluentIterable.from(ImmutableList.<Number>of());
  }

  /*
   * The following tests demonstrate the shortcomings of or() and test that the casting workaround
   * mentioned in the method Javadoc does in fact compile.
   */

  @SuppressWarnings("unused") // compilation test
  public void testSampleCodeError1() {
    Optional<Integer> optionalInt = getSomeOptionalInt();
    // Number value = optionalInt.or(0.5); // error
  }

  @SuppressWarnings("unused") // compilation test
  public void testSampleCodeError2() {
    FluentIterable<? extends Number> numbers = getSomeNumbers();
    Optional<? extends Number> first = numbers.first();
    // Number value = first.or(0.5); // error
  }

  @SuppressWarnings("unused") // compilation test
  public void testSampleCodeFine1() {
    Optional<Number> optionalInt = Optional.of((Number) 1);
    Number value = optionalInt.or(0.5); // fine
  }

  @SuppressWarnings("unused") // compilation test
  public void testSampleCodeFine2() {
    FluentIterable<? extends Number> numbers = getSomeNumbers();

    // Sadly, the following is what users will have to do in some circumstances.

    @SuppressWarnings("unchecked") // safe covariant cast
    Optional<Number> first = (Optional<Number>) numbers.first();
    Number value = first.or(0.5); // fine
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    NullPointerTester npTester = new NullPointerTester();
    npTester.testAllPublicConstructors(Optional.class);
    npTester.testAllPublicStaticMethods(Optional.class);
    npTester.testAllPublicInstanceMethods(Optional.absent());
    npTester.testAllPublicInstanceMethods(Optional.of("training"));
  }
}

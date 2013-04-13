/*
 * Copyright (C) 2005 The Guava Authors
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.testing.EqualsTester;

import junit.framework.TestCase;

import java.io.Serializable;
import java.util.Map;

/**
 * Tests for {@link Functions}.
 *
 * @author Mike Bostock
 * @author Vlad Patryshev
 */
@GwtCompatible(emulated = true)
public class FunctionsTest extends TestCase {

  public void testIdentity_same() {
    Function<String, String> identity = Functions.identity();
    assertNull(identity.apply(null));
    assertSame("foo", identity.apply("foo"));
  }

  public void testIdentity_notSame() {
    Function<Long, Long> identity = Functions.identity();
    assertNotSame(new Long(135135L), identity.apply(new Long(135135L)));
  }

  public void testToStringFunction_apply() {
    assertEquals("3", Functions.toStringFunction().apply(3));
    assertEquals("hiya", Functions.toStringFunction().apply("hiya"));
    assertEquals("I'm a string",
        Functions.toStringFunction().apply(
            new Object() {
              @Override public String toString() {
                return "I'm a string";
              }
            }));
    try {
      Functions.toStringFunction().apply(null);
      fail("expected NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  public void testForMapWithoutDefault() {
    Map<String, Integer> map = Maps.newHashMap();
    map.put("One", 1);
    map.put("Three", 3);
    map.put("Null", null);
    Function<String, Integer> function = Functions.forMap(map);

    assertEquals(1, function.apply("One").intValue());
    assertEquals(3, function.apply("Three").intValue());
    assertNull(function.apply("Null"));

    try {
      function.apply("Two");
      fail();
    } catch (IllegalArgumentException expected) {
    }

    new EqualsTester()
        .addEqualityGroup(function, Functions.forMap(map))
        .addEqualityGroup(Functions.forMap(map, 42))
        .testEquals();
  }

  public void testForMapWithDefault() {
    Map<String, Integer> map = Maps.newHashMap();
    map.put("One", 1);
    map.put("Three", 3);
    map.put("Null", null);
    Function<String, Integer> function = Functions.forMap(map, 42);

    assertEquals(1, function.apply("One").intValue());
    assertEquals(42, function.apply("Two").intValue());
    assertEquals(3, function.apply("Three").intValue());
    assertNull(function.apply("Null"));

    new EqualsTester()
        .addEqualityGroup(function, Functions.forMap(map, 42))
        .addEqualityGroup(Functions.forMap(map))
        .addEqualityGroup(Functions.forMap(map, null))
        .addEqualityGroup(Functions.forMap(map, 43))
        .testEquals();
  }

  public void testForMapWithDefault_null() {
    ImmutableMap<String, Integer> map = ImmutableMap.of("One", 1);
    Function<String, Integer> function = Functions.forMap(map, null);

    assertEquals((Integer) 1, function.apply("One"));
    assertNull(function.apply("Two"));

    // check basic sanity of equals and hashCode
    new EqualsTester()
        .addEqualityGroup(function)
        .addEqualityGroup(Functions.forMap(map, 1))
        .testEquals();
  }

  public void testForMapWildCardWithDefault() {
    Map<String, Integer> map = Maps.newHashMap();
    map.put("One", 1);
    map.put("Three", 3);
    Number number = Double.valueOf(42);
    Function<String, Number> function = Functions.forMap(map, number);

    assertEquals(1, function.apply("One").intValue());
    assertEquals(number, function.apply("Two"));
    assertEquals(3L, function.apply("Three").longValue());
  }

  public void testComposition() {
    Map<String, Integer> mJapaneseToInteger = Maps.newHashMap();
    mJapaneseToInteger.put("Ichi", 1);
    mJapaneseToInteger.put("Ni", 2);
    mJapaneseToInteger.put("San", 3);
    Function<String, Integer> japaneseToInteger =
        Functions.forMap(mJapaneseToInteger);

    Map<Integer, String> mIntegerToSpanish = Maps.newHashMap();
    mIntegerToSpanish.put(1, "Uno");
    mIntegerToSpanish.put(3, "Tres");
    mIntegerToSpanish.put(4, "Cuatro");
    Function<Integer, String> integerToSpanish =
        Functions.forMap(mIntegerToSpanish);

    Function<String, String> japaneseToSpanish =
        Functions.compose(integerToSpanish, japaneseToInteger);

    assertEquals("Uno", japaneseToSpanish.apply("Ichi"));
    try {
      japaneseToSpanish.apply("Ni");
      fail();
    } catch (IllegalArgumentException e) {
    }
    assertEquals("Tres", japaneseToSpanish.apply("San"));
    try {
      japaneseToSpanish.apply("Shi");
      fail();
    } catch (IllegalArgumentException e) {
    }

    new EqualsTester()
        .addEqualityGroup(
            japaneseToSpanish,
            Functions.compose(integerToSpanish, japaneseToInteger))
        .addEqualityGroup(japaneseToInteger)
        .addEqualityGroup(integerToSpanish)
        .addEqualityGroup(
            Functions.compose(japaneseToInteger, integerToSpanish))
        .testEquals();
  }

  public void testCompositionWildcard() {
    Map<String, Integer> mapJapaneseToInteger = Maps.newHashMap();
    Function<String, Integer> japaneseToInteger =
        Functions.forMap(mapJapaneseToInteger);

    Function<Object, String> numberToSpanish = Functions.constant("Yo no se");

    Function<String, String> japaneseToSpanish =
        Functions.compose(numberToSpanish, japaneseToInteger);
  }

  private static class HashCodeFunction implements Function<Object, Integer> {
    @Override
    public Integer apply(Object o) {
      return (o == null) ? 0 : o.hashCode();
    }
  }

  public void testComposeOfFunctionsIsAssociative() {
    Map<Float, String> m = ImmutableMap.of(
        4.0f, "A", 3.0f, "B", 2.0f, "C", 1.0f, "D");
    Function<? super Integer, Boolean> h = Functions.constant(Boolean.TRUE);
    Function<? super String, Integer> g = new HashCodeFunction();
    Function<Float, String> f = Functions.forMap(m, "F");

    Function<Float, Boolean> c1 = Functions.compose(Functions.compose(h, g), f);
    Function<Float, Boolean> c2 = Functions.compose(h, Functions.compose(g, f));

    // Might be nice (eventually) to have:
    //     assertEquals(c1, c2);

    // But for now, settle for this:
    assertEquals(c1.hashCode(), c2.hashCode());

    assertEquals(c1.apply(1.0f), c2.apply(1.0f));
    assertEquals(c1.apply(5.0f), c2.apply(5.0f));
  }

  public void testComposeOfPredicateAndFunctionIsAssociative() {
    Map<Float, String> m = ImmutableMap.of(
        4.0f, "A", 3.0f, "B", 2.0f, "C", 1.0f, "D");
    Predicate<? super Integer> h = Predicates.equalTo(42);
    Function<? super String, Integer> g = new HashCodeFunction();
    Function<Float, String> f = Functions.forMap(m, "F");

    Predicate<Float> p1 = Predicates.compose(Predicates.compose(h, g), f);
    Predicate<Float> p2 = Predicates.compose(h, Functions.compose(g, f));

    // Might be nice (eventually) to have:
    //     assertEquals(p1, p2);

    // But for now, settle for this:
    assertEquals(p1.hashCode(), p2.hashCode());

    assertEquals(p1.apply(1.0f), p2.apply(1.0f));
    assertEquals(p1.apply(5.0f), p2.apply(5.0f));
  }

  public void testForPredicate() {
    Function<Object, Boolean> alwaysTrue =
        Functions.forPredicate(Predicates.alwaysTrue());
    Function<Object, Boolean> alwaysFalse =
        Functions.forPredicate(Predicates.alwaysFalse());

    assertTrue(alwaysTrue.apply(0));
    assertFalse(alwaysFalse.apply(0));

    new EqualsTester()
        .addEqualityGroup(
            alwaysTrue, Functions.forPredicate(Predicates.alwaysTrue()))
        .addEqualityGroup(alwaysFalse)
        .addEqualityGroup(Functions.identity())
        .testEquals();
  }

  public void testConstant() {
    Function<Object, Object> f = Functions.<Object>constant("correct");
    assertEquals("correct", f.apply(new Object()));
    assertEquals("correct", f.apply(null));

    Function<Object, String> g = Functions.constant(null);
    assertEquals(null, g.apply(2));
    assertEquals(null, g.apply(null));

    new EqualsTester()
        .addEqualityGroup(f, Functions.constant("correct"))
        .addEqualityGroup(Functions.constant("incorrect"))
        .addEqualityGroup(Functions.toStringFunction())
        .addEqualityGroup(g)
        .testEquals();

    new EqualsTester()
        .addEqualityGroup(g, Functions.constant(null))
        .addEqualityGroup(Functions.constant("incorrect"))
        .addEqualityGroup(Functions.toStringFunction())
        .addEqualityGroup(f)
        .testEquals();
  }

  private static class CountingSupplier
      implements Supplier<Integer>, Serializable {

    private static final long serialVersionUID = 0;

    private int value;

    @Override
    public Integer get() {
      return ++value;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof CountingSupplier) {
        return this.value == ((CountingSupplier) obj).value;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return value;
    }
  }

  public void testForSupplier() {
    Supplier<Integer> supplier = new CountingSupplier();
    Function<Object, Integer> function = Functions.forSupplier(supplier);

    assertEquals(1, (int) function.apply(null));
    assertEquals(2, (int) function.apply("foo"));

    new EqualsTester()
        .addEqualityGroup(function, Functions.forSupplier(supplier))
        .addEqualityGroup(Functions.forSupplier(new CountingSupplier()))
        .addEqualityGroup(Functions.forSupplier(Suppliers.ofInstance(12)))
        .addEqualityGroup(Functions.toStringFunction())
        .testEquals();
  }

}


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

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.base.Predicates.or;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.ClassSanityTester;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Unit test for {@link Predicates}.
 *
 * @author Kevin Bourrillion
 */
@NullMarked
@GwtCompatible
public class PredicatesTest extends TestCase {
  private static final Predicate<@Nullable Integer> TRUE = Predicates.alwaysTrue();
  private static final Predicate<@Nullable Integer> FALSE = Predicates.alwaysFalse();
  private static final Predicate<@Nullable Integer> NEVER_REACHED =
      unused -> {
        throw new AssertionFailedError("This predicate should never have been evaluated");
      };

  /** Instantiable predicate with reasonable hashCode() and equals() methods. */
  static class IsOdd implements Predicate<@Nullable Integer>, Serializable {
    @GwtIncompatible @J2ktIncompatible     private static final long serialVersionUID = 0x150ddL;

    @Override
    public boolean apply(@Nullable Integer i) {
      return (i.intValue() & 1) == 1;
    }

    @Override
    public int hashCode() {
      return 0x150dd;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return obj instanceof IsOdd;
    }

    @Override
    public String toString() {
      return "IsOdd";
    }
  }

  /**
   * Generates a new Predicate per call.
   *
   * <p>Creating a new Predicate each time helps catch cases where code is using {@code x == y}
   * instead of {@code x.equals(y)}.
   */
  private static IsOdd isOdd() {
    return new IsOdd();
  }

  /*
   * Tests for Predicates.alwaysTrue().
   */

  public void testAlwaysTrue_apply() {
    assertEvalsToTrue(Predicates.alwaysTrue());
  }

  public void testAlwaysTrue_equality() throws Exception {
    new EqualsTester()
        .addEqualityGroup(TRUE, Predicates.alwaysTrue())
        .addEqualityGroup(isOdd())
        .addEqualityGroup(Predicates.alwaysFalse())
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testAlwaysTrue_serialization() {
    checkSerialization(Predicates.alwaysTrue());
  }

  /*
   * Tests for Predicates.alwaysFalse().
   */

  public void testAlwaysFalse_apply() throws Exception {
    assertEvalsToFalse(Predicates.alwaysFalse());
  }

  public void testAlwaysFalse_equality() throws Exception {
    new EqualsTester()
        .addEqualityGroup(FALSE, Predicates.alwaysFalse())
        .addEqualityGroup(isOdd())
        .addEqualityGroup(Predicates.alwaysTrue())
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testAlwaysFalse_serialization() {
    checkSerialization(Predicates.alwaysFalse());
  }

  /*
   * Tests for Predicates.not(predicate).
   */

  public void testNot_apply() {
    assertEvalsToTrue(not(FALSE));
    assertEvalsToFalse(not(TRUE));
    assertEvalsLikeOdd(not(not(isOdd())));
  }

  public void testNot_equality() {
    new EqualsTester()
        .addEqualityGroup(not(isOdd()), not(isOdd()))
        .addEqualityGroup(not(TRUE))
        .addEqualityGroup(isOdd())
        .testEquals();
  }

  public void testNot_equalityForNotOfKnownValues() {
    new EqualsTester()
        .addEqualityGroup(TRUE, Predicates.alwaysTrue())
        .addEqualityGroup(FALSE)
        .addEqualityGroup(not(TRUE))
        .testEquals();

    new EqualsTester()
        .addEqualityGroup(FALSE, Predicates.alwaysFalse())
        .addEqualityGroup(TRUE)
        .addEqualityGroup(not(FALSE))
        .testEquals();

    new EqualsTester()
        .addEqualityGroup(Predicates.isNull(), Predicates.isNull())
        .addEqualityGroup(notNull())
        .addEqualityGroup(not(Predicates.isNull()))
        .testEquals();

    new EqualsTester()
        .addEqualityGroup(notNull(), notNull())
        .addEqualityGroup(Predicates.isNull())
        .addEqualityGroup(not(notNull()))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testNot_serialization() {
    checkSerialization(not(isOdd()));
  }

  /*
   * Tests for all the different flavors of Predicates.and().
   */

  public void testAnd_applyNoArgs() {
    assertEvalsToTrue(and());
  }

  public void testAnd_equalityNoArgs() {
    new EqualsTester()
        .addEqualityGroup(and(), and())
        .addEqualityGroup(and(FALSE))
        .addEqualityGroup(or())
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testAnd_serializationNoArgs() {
    checkSerialization(and());
  }

  public void testAnd_applyOneArg() {
    assertEvalsLikeOdd(and(isOdd()));
  }

  public void testAnd_equalityOneArg() {
    Object[] notEqualObjects = {and(NEVER_REACHED, FALSE)};
    new EqualsTester()
        .addEqualityGroup(and(NEVER_REACHED), and(NEVER_REACHED))
        .addEqualityGroup(notEqualObjects)
        .addEqualityGroup(and(isOdd()))
        .addEqualityGroup(and())
        .addEqualityGroup(or(NEVER_REACHED))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testAnd_serializationOneArg() {
    checkSerialization(and(isOdd()));
  }

  public void testAnd_applyBinary() {
    assertEvalsLikeOdd(and(isOdd(), TRUE));
    assertEvalsLikeOdd(and(TRUE, isOdd()));
    assertEvalsToFalse(and(FALSE, NEVER_REACHED));
  }

  public void testAnd_equalityBinary() {
    new EqualsTester()
        .addEqualityGroup(and(TRUE, NEVER_REACHED), and(TRUE, NEVER_REACHED))
        .addEqualityGroup(and(NEVER_REACHED, TRUE))
        .addEqualityGroup(and(TRUE))
        .addEqualityGroup(or(TRUE, NEVER_REACHED))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testAnd_serializationBinary() {
    checkSerialization(and(TRUE, isOdd()));
  }

  public void testAnd_applyTernary() {
    assertEvalsLikeOdd(and(isOdd(), TRUE, TRUE));
    assertEvalsLikeOdd(and(TRUE, isOdd(), TRUE));
    assertEvalsLikeOdd(and(TRUE, TRUE, isOdd()));
    assertEvalsToFalse(and(TRUE, FALSE, NEVER_REACHED));
  }

  public void testAnd_equalityTernary() {
    new EqualsTester()
        .addEqualityGroup(and(TRUE, isOdd(), NEVER_REACHED), and(TRUE, isOdd(), NEVER_REACHED))
        .addEqualityGroup(and(isOdd(), NEVER_REACHED, TRUE))
        .addEqualityGroup(and(TRUE))
        .addEqualityGroup(or(TRUE, isOdd(), NEVER_REACHED))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testAnd_serializationTernary() {
    checkSerialization(and(TRUE, isOdd(), FALSE));
  }

  public void testAnd_applyIterable() {
    Collection<Predicate<@Nullable Integer>> empty = asList();
    assertEvalsToTrue(and(empty));
    assertEvalsLikeOdd(and(asList(isOdd())));
    assertEvalsLikeOdd(and(asList(TRUE, isOdd())));
    assertEvalsToFalse(and(asList(FALSE, NEVER_REACHED)));
  }

  public void testAnd_equalityIterable() {
    new EqualsTester()
        .addEqualityGroup(
            and(asList(TRUE, NEVER_REACHED)),
            and(asList(TRUE, NEVER_REACHED)),
            and(TRUE, NEVER_REACHED))
        .addEqualityGroup(and(FALSE, NEVER_REACHED))
        .addEqualityGroup(or(TRUE, NEVER_REACHED))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testAnd_serializationIterable() {
    checkSerialization(and(asList(TRUE, FALSE)));
  }

  public void testAnd_arrayDefensivelyCopied() {
    @SuppressWarnings("unchecked") // generic arrays
    Predicate<Object>[] array = (Predicate<Object>[]) new Predicate<?>[] {Predicates.alwaysFalse()};
    Predicate<Object> predicate = and(array);
    assertFalse(predicate.apply(1));
    array[0] = Predicates.alwaysTrue();
    assertFalse(predicate.apply(1));
  }

  public void testAnd_listDefensivelyCopied() {
    List<Predicate<Object>> list = new ArrayList<>();
    Predicate<Object> predicate = and(list);
    assertTrue(predicate.apply(1));
    list.add(Predicates.alwaysFalse());
    assertTrue(predicate.apply(1));
  }

  public void testAnd_iterableDefensivelyCopied() {
    List<Predicate<Object>> list = new ArrayList<>();
    Iterable<Predicate<Object>> iterable =
        new Iterable<Predicate<Object>>() {
          @Override
          public Iterator<Predicate<Object>> iterator() {
            return list.iterator();
          }
        };
    Predicate<Object> predicate = and(iterable);
    assertTrue(predicate.apply(1));
    list.add(Predicates.alwaysFalse());
    assertTrue(predicate.apply(1));
  }

  /*
   * Tests for all the different flavors of Predicates.or().
   */

  public void testOr_applyNoArgs() {
    assertEvalsToFalse(or());
  }

  public void testOr_equalityNoArgs() {
    new EqualsTester()
        .addEqualityGroup(or(), or())
        .addEqualityGroup(or(TRUE))
        .addEqualityGroup(and())
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testOr_serializationNoArgs() {
    checkSerialization(or());
  }

  public void testOr_applyOneArg() {
    assertEvalsToTrue(or(TRUE));
    assertEvalsToFalse(or(FALSE));
  }

  public void testOr_equalityOneArg() {
    new EqualsTester()
        .addEqualityGroup(or(NEVER_REACHED), or(NEVER_REACHED))
        .addEqualityGroup(or(NEVER_REACHED, TRUE))
        .addEqualityGroup(or(TRUE))
        .addEqualityGroup(or())
        .addEqualityGroup(and(NEVER_REACHED))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testOr_serializationOneArg() {
    checkSerialization(or(isOdd()));
  }

  public void testOr_applyBinary() {
    Predicate<@Nullable Integer> falseOrFalse = or(FALSE, FALSE);
    Predicate<@Nullable Integer> falseOrTrue = or(FALSE, TRUE);
    Predicate<@Nullable Integer> trueOrAnything = or(TRUE, NEVER_REACHED);

    assertEvalsToFalse(falseOrFalse);
    assertEvalsToTrue(falseOrTrue);
    assertEvalsToTrue(trueOrAnything);
  }

  public void testOr_equalityBinary() {
    new EqualsTester()
        .addEqualityGroup(or(FALSE, NEVER_REACHED), or(FALSE, NEVER_REACHED))
        .addEqualityGroup(or(NEVER_REACHED, FALSE))
        .addEqualityGroup(or(TRUE))
        .addEqualityGroup(and(FALSE, NEVER_REACHED))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testOr_serializationBinary() {
    checkSerialization(or(isOdd(), TRUE));
  }

  public void testOr_applyTernary() {
    assertEvalsLikeOdd(or(isOdd(), FALSE, FALSE));
    assertEvalsLikeOdd(or(FALSE, isOdd(), FALSE));
    assertEvalsLikeOdd(or(FALSE, FALSE, isOdd()));
    assertEvalsToTrue(or(FALSE, TRUE, NEVER_REACHED));
  }

  public void testOr_equalityTernary() {
    new EqualsTester()
        .addEqualityGroup(or(FALSE, NEVER_REACHED, TRUE), or(FALSE, NEVER_REACHED, TRUE))
        .addEqualityGroup(or(TRUE, NEVER_REACHED, FALSE))
        .addEqualityGroup(or(TRUE))
        .addEqualityGroup(and(FALSE, NEVER_REACHED, TRUE))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testOr_serializationTernary() {
    checkSerialization(or(FALSE, isOdd(), TRUE));
  }

  public void testOr_applyIterable() {
    Predicate<@Nullable Integer> vacuouslyFalse = or(ImmutableList.of());
    Predicate<@Nullable Integer> troo = or(ImmutableList.of(TRUE));
    Predicate<@Nullable Integer> trueAndFalse = or(ImmutableList.of(TRUE, FALSE));

    assertEvalsToFalse(vacuouslyFalse);
    assertEvalsToTrue(troo);
    assertEvalsToTrue(trueAndFalse);
  }

  public void testOr_equalityIterable() {
    new EqualsTester()
        .addEqualityGroup(
            or(asList(FALSE, NEVER_REACHED)),
            or(asList(FALSE, NEVER_REACHED)),
            or(FALSE, NEVER_REACHED))
        .addEqualityGroup(or(TRUE, NEVER_REACHED))
        .addEqualityGroup(and(FALSE, NEVER_REACHED))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testOr_serializationIterable() {
    Predicate<Integer> pre = or(asList(TRUE, FALSE));
    Predicate<Integer> post = reserializeAndAssert(pre);
    assertEquals(pre.apply(0), post.apply(0));
  }

  public void testOr_arrayDefensivelyCopied() {
    @SuppressWarnings("unchecked") // generic arrays
    Predicate<Object>[] array = (Predicate<Object>[]) new Predicate<?>[] {Predicates.alwaysFalse()};
    Predicate<Object> predicate = or(array);
    assertFalse(predicate.apply(1));
    array[0] = Predicates.alwaysTrue();
    assertFalse(predicate.apply(1));
  }

  public void testOr_listDefensivelyCopied() {
    List<Predicate<Object>> list = new ArrayList<>();
    Predicate<Object> predicate = or(list);
    assertFalse(predicate.apply(1));
    list.add(Predicates.alwaysTrue());
    assertFalse(predicate.apply(1));
  }

  public void testOr_iterableDefensivelyCopied() {
    List<Predicate<Object>> list = new ArrayList<>();
    Iterable<Predicate<Object>> iterable =
        new Iterable<Predicate<Object>>() {
          @Override
          public Iterator<Predicate<Object>> iterator() {
            return list.iterator();
          }
        };
    Predicate<Object> predicate = or(iterable);
    assertFalse(predicate.apply(1));
    list.add(Predicates.alwaysTrue());
    assertFalse(predicate.apply(1));
  }

  /*
   * Tests for Predicates.equalTo(x).
   */

  public void testIsEqualTo_apply() {
    Predicate<@Nullable Integer> isOne = equalTo(1);

    assertTrue(isOne.apply(1));
    assertFalse(isOne.apply(2));
    assertFalse(isOne.apply(null));
  }

  public void testIsEqualTo_equality() {
    new EqualsTester()
        .addEqualityGroup(equalTo(1), equalTo(1))
        .addEqualityGroup(equalTo(2))
        .addEqualityGroup(Predicates.<@Nullable Integer>equalTo(null))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testIsEqualTo_serialization() {
    checkSerialization(equalTo(1));
  }

  public void testIsEqualToNull_apply() {
    Predicate<@Nullable Integer> isNull = equalTo(null);
    assertTrue(isNull.apply(null));
    assertFalse(isNull.apply(1));
  }

  public void testIsEqualToNull_equality() {
    new EqualsTester()
        .addEqualityGroup(
            Predicates.<@Nullable Integer>equalTo(null),
            Predicates.<@Nullable Integer>equalTo(null))
        .addEqualityGroup(equalTo(1))
        .addEqualityGroup(equalTo("null"))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testIsEqualToNull_serialization() {
    checkSerialization(equalTo(null));
  }

  /**
   * Tests for Predicates.instanceOf(x). TODO: Fix the comment style after fixing annotation
   * stripper to remove comments properly. Currently, all tests before the comments are removed as
   * well.
   */
  @GwtIncompatible // Predicates.instanceOf
  public void testIsInstanceOf_apply() {
    Predicate<@Nullable Object> isInteger = instanceOf(Integer.class);

    assertTrue(isInteger.apply(1));
    assertFalse(isInteger.apply(2.0f));
    assertFalse(isInteger.apply(""));
    assertFalse(isInteger.apply(null));
  }

  @GwtIncompatible // Predicates.instanceOf
  public void testIsInstanceOf_subclass() {
    Predicate<@Nullable Object> isNumber = instanceOf(Number.class);

    assertTrue(isNumber.apply(1));
    assertTrue(isNumber.apply(2.0f));
    assertFalse(isNumber.apply(""));
    assertFalse(isNumber.apply(null));
  }

  @GwtIncompatible // Predicates.instanceOf
  public void testIsInstanceOf_interface() {
    Predicate<@Nullable Object> isComparable = instanceOf(Comparable.class);

    assertTrue(isComparable.apply(1));
    assertTrue(isComparable.apply(2.0f));
    assertTrue(isComparable.apply(""));
    assertFalse(isComparable.apply(null));
  }

  @GwtIncompatible // Predicates.instanceOf
  public void testIsInstanceOf_equality() {
    new EqualsTester()
        .addEqualityGroup(instanceOf(Integer.class), instanceOf(Integer.class))
        .addEqualityGroup(instanceOf(Number.class))
        .addEqualityGroup(instanceOf(Float.class))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // Predicates.instanceOf, SerializableTester
  public void testIsInstanceOf_serialization() {
    checkSerialization(instanceOf(Integer.class));
  }

  @J2ktIncompatible
  @GwtIncompatible // Predicates.subtypeOf
  public void testSubtypeOf_apply() {
    Predicate<Class<?>> isInteger = Predicates.subtypeOf(Integer.class);

    assertTrue(isInteger.apply(Integer.class));
    assertFalse(isInteger.apply(Float.class));

    assertThrows(NullPointerException.class, () -> isInteger.apply(null));
  }

  @J2ktIncompatible
  @GwtIncompatible // Predicates.subtypeOf
  public void testSubtypeOf_subclass() {
    Predicate<Class<?>> isNumber = Predicates.subtypeOf(Number.class);

    assertTrue(isNumber.apply(Integer.class));
    assertTrue(isNumber.apply(Float.class));
  }

  @J2ktIncompatible
  @GwtIncompatible // Predicates.subtypeOf
  public void testSubtypeOf_interface() {
    Predicate<Class<?>> isComparable = Predicates.subtypeOf(Comparable.class);

    assertTrue(isComparable.apply(Integer.class));
    assertTrue(isComparable.apply(Float.class));
  }

  @J2ktIncompatible
  @GwtIncompatible // Predicates.subtypeOf
  public void testSubtypeOf_equality() {
    new EqualsTester()
        .addEqualityGroup(Predicates.subtypeOf(Integer.class))
        .addEqualityGroup(Predicates.subtypeOf(Number.class))
        .addEqualityGroup(Predicates.subtypeOf(Float.class))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // Predicates.subtypeOf, SerializableTester
  public void testSubtypeOf_serialization() {
    Predicate<Class<?>> predicate = Predicates.subtypeOf(Integer.class);
    Predicate<Class<?>> reserialized = reserializeAndAssert(predicate);

    assertEvalsLike(predicate, reserialized, Integer.class);
    assertEvalsLike(predicate, reserialized, Float.class);
    assertEvalsLike(predicate, reserialized, null);
  }

  /*
   * Tests for Predicates.isNull()
   */

  public void testIsNull_apply() {
    Predicate<@Nullable Integer> isNull = Predicates.isNull();
    assertTrue(isNull.apply(null));
    assertFalse(isNull.apply(1));
  }

  public void testIsNull_equality() {
    new EqualsTester()
        .addEqualityGroup(Predicates.isNull(), Predicates.isNull())
        .addEqualityGroup(notNull())
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testIsNull_serialization() {
    Predicate<String> pre = Predicates.isNull();
    Predicate<String> post = reserializeAndAssert(pre);
    assertEquals(pre.apply("foo"), post.apply("foo"));
    assertEquals(pre.apply(null), post.apply(null));
  }

  public void testNotNull_apply() {
    Predicate<@Nullable Integer> notNull = notNull();
    assertFalse(notNull.apply(null));
    assertTrue(notNull.apply(1));
  }

  public void testNotNull_equality() {
    new EqualsTester()
        .addEqualityGroup(notNull(), notNull())
        .addEqualityGroup(Predicates.isNull())
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testNotNull_serialization() {
    checkSerialization(notNull());
  }

  public void testIn_apply() {
    Collection<Integer> nums = asList(1, 5);
    Predicate<@Nullable Integer> isOneOrFive = Predicates.in(nums);

    assertTrue(isOneOrFive.apply(1));
    assertTrue(isOneOrFive.apply(5));
    assertFalse(isOneOrFive.apply(3));
    assertFalse(isOneOrFive.apply(null));
  }

  public void testIn_equality() {
    Collection<Integer> nums = ImmutableSet.of(1, 5);
    Collection<Integer> sameOrder = ImmutableSet.of(1, 5);
    Collection<Integer> differentOrder = ImmutableSet.of(5, 1);
    Collection<Integer> differentNums = ImmutableSet.of(1, 3, 5);

    new EqualsTester()
        .addEqualityGroup(
            Predicates.in(nums),
            Predicates.in(nums),
            Predicates.in(sameOrder),
            Predicates.in(differentOrder))
        .addEqualityGroup(Predicates.in(differentNums))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testIn_serialization() {
    checkSerialization(Predicates.in(asList(1, 2, 3, null)));
  }

  public void testIn_handlesNullPointerException() {
    class CollectionThatThrowsNullPointerException<T> extends ArrayList<T> {
      @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 1L;

      @Override
      public boolean contains(@Nullable Object element) {
        Preconditions.checkNotNull(element);
        return super.contains(element);
      }
    }
    Collection<Integer> nums = new CollectionThatThrowsNullPointerException<>();
    Predicate<@Nullable Integer> isFalse = Predicates.in(nums);
    assertFalse(isFalse.apply(null));
  }

  public void testIn_handlesClassCastException() {
    class CollectionThatThrowsClassCastException<T> extends ArrayList<T> {
      @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 1L;

      @Override
      public boolean contains(@Nullable Object element) {
        throw new ClassCastException("");
      }
    }
    Collection<Integer> nums = new CollectionThatThrowsClassCastException<>();
    nums.add(3);
    Predicate<Integer> isThree = Predicates.in(nums);
    assertFalse(isThree.apply(3));
  }

  /*
   * Tests that compilation will work when applying explicit types.
   */
  @SuppressWarnings("unused") // compilation test
  public void testIn_compilesWithExplicitSupertype() {
    Collection<Number> nums = ImmutableSet.of();
    Predicate<Number> p1 = Predicates.in(nums);
    Predicate<Object> p2 = Predicates.in(nums);
    // The next two lines are not expected to compile.
    // Predicate<Integer> p3 = Predicates.in(nums);
    // Predicate<Integer> p4 = Predicates.<Integer>in(nums);
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointerExceptions() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Predicates.class);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testCascadingSerialization() throws Exception {
    // Eclipse says Predicate<Integer>; javac says Predicate<Object>.
    Predicate<? super Integer> nasty =
        not(
            and(
                or(
                    equalTo((Object) 1),
                    equalTo(null),
                    Predicates.alwaysFalse(),
                    Predicates.alwaysTrue(),
                    Predicates.isNull(),
                    notNull(),
                    Predicates.in(asList(1)))));
    assertEvalsToFalse(nasty);

    Predicate<? super Integer> stillNasty = reserializeAndAssert(nasty);

    assertEvalsToFalse(stillNasty);
  }

  // enum singleton pattern
  private enum TrimStringFunction implements Function<String, String> {
    INSTANCE;

    @Override
    public String apply(String string) {
      return whitespace().trimFrom(string);
    }
  }

  public void testCompose() {
    Function<String, String> trim = TrimStringFunction.INSTANCE;
    Predicate<String> equalsFoo = equalTo("Foo");
    Predicate<String> equalsBar = equalTo("Bar");
    Predicate<String> trimEqualsFoo = Predicates.compose(equalsFoo, trim);
    Function<String, String> identity = Functions.identity();

    assertTrue(trimEqualsFoo.apply("Foo"));
    assertTrue(trimEqualsFoo.apply("   Foo   "));
    assertFalse(trimEqualsFoo.apply("Foo-b-que"));

    new EqualsTester()
        .addEqualityGroup(trimEqualsFoo, Predicates.compose(equalsFoo, trim))
        .addEqualityGroup(equalsFoo)
        .addEqualityGroup(trim)
        .addEqualityGroup(Predicates.compose(equalsFoo, identity))
        .addEqualityGroup(Predicates.compose(equalsBar, trim))
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testComposeSerialization() {
    Function<String, String> trim = TrimStringFunction.INSTANCE;
    Predicate<String> equalsFoo = equalTo("Foo");
    Predicate<String> trimEqualsFoo = Predicates.compose(equalsFoo, trim);
    reserializeAndAssert(trimEqualsFoo);
  }

  /**
   * Tests for Predicates.contains(Pattern) and .containsPattern(String). We assume the regex level
   * works, so there are only trivial tests of that aspect. TODO: Fix comment style once annotation
   * stripper is fixed.
   */
  @GwtIncompatible // Predicates.containsPattern
  public void testContainsPattern_apply() {
    Predicate<CharSequence> isFoobar = Predicates.containsPattern("^Fo.*o.*bar$");
    assertTrue(isFoobar.apply("Foxyzoabcbar"));
    assertFalse(isFoobar.apply("Foobarx"));
  }

  @GwtIncompatible // Predicates.containsPattern
  public void testContains_apply() {
    Predicate<CharSequence> isFoobar = Predicates.contains(Pattern.compile("^Fo.*o.*bar$"));

    assertTrue(isFoobar.apply("Foxyzoabcbar"));
    assertFalse(isFoobar.apply("Foobarx"));
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testContainsPattern_nulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    Predicate<CharSequence> isWooString = Predicates.containsPattern("Woo");

    tester.testAllPublicInstanceMethods(isWooString);
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testContains_nulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    Predicate<CharSequence> isWooPattern = Predicates.contains(Pattern.compile("Woo"));

    tester.testAllPublicInstanceMethods(isWooPattern);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testContainsPattern_serialization() {
    Predicate<CharSequence> pre = Predicates.containsPattern("foo");
    Predicate<CharSequence> post = reserializeAndAssert(pre);
    assertEquals(pre.apply("foo"), post.apply("foo"));
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testContains_equals() {
    new EqualsTester()
        .addEqualityGroup(
            Predicates.contains(Pattern.compile("foo")), Predicates.containsPattern("foo"))
        .addEqualityGroup(Predicates.contains(Pattern.compile("foo", Pattern.CASE_INSENSITIVE)))
        .addEqualityGroup(Predicates.containsPattern("bar"))
        .testEquals();
  }

  public void assertEqualHashCode(
      Predicate<? super @Nullable Integer> expected, Predicate<? super @Nullable Integer> actual) {
    assertEquals(actual + " should hash like " + expected, expected.hashCode(), actual.hashCode());
  }

  public void testHashCodeForBooleanOperations() {
    Predicate<@Nullable Integer> p1 = Predicates.isNull();
    Predicate<@Nullable Integer> p2 = isOdd();

    // Make sure that hash codes are not computed per-instance.
    assertEqualHashCode(not(p1), not(p1));

    assertEqualHashCode(and(p1, p2), and(p1, p2));

    assertEqualHashCode(or(p1, p2), or(p1, p2));

    // While not a contractual requirement, we'd like the hash codes for ands
    // & ors of the same predicates to not collide.
    assertTrue(and(p1, p2).hashCode() != or(p1, p2).hashCode());
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  public void testNulls() throws Exception {
    new ClassSanityTester().forAllPublicStaticMethods(Predicates.class).testNulls();
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  @AndroidIncompatible // TODO(cpovirk): ClassNotFoundException: com.google.common.base.Function
  public void testEqualsAndSerializable() throws Exception {
    new ClassSanityTester().forAllPublicStaticMethods(Predicates.class).testEqualsAndSerializable();
  }

  private static void assertEvalsToTrue(Predicate<? super @Nullable Integer> predicate) {
    assertTrue(predicate.apply(0));
    assertTrue(predicate.apply(1));
    assertTrue(predicate.apply(null));
  }

  private static void assertEvalsToFalse(Predicate<? super @Nullable Integer> predicate) {
    assertFalse(predicate.apply(0));
    assertFalse(predicate.apply(1));
    assertFalse(predicate.apply(null));
  }

  private static void assertEvalsLikeOdd(Predicate<? super @Nullable Integer> predicate) {
    assertEvalsLike(isOdd(), predicate);
  }

  private static void assertEvalsLike(
      Predicate<? super @Nullable Integer> expected, Predicate<? super @Nullable Integer> actual) {
    assertEvalsLike(expected, actual, 0);
    assertEvalsLike(expected, actual, 1);
    PredicatesTest.<@Nullable Integer>assertEvalsLike(expected, actual, null);
  }

  private static <T extends @Nullable Object> void assertEvalsLike(
      Predicate<? super T> expected, Predicate<? super T> actual, T input) {
    Boolean expectedResult = null;
    RuntimeException expectedRuntimeException = null;
    try {
      expectedResult = expected.apply(input);
    } catch (RuntimeException e) {
      expectedRuntimeException = e;
    }

    Boolean actualResult = null;
    RuntimeException actualRuntimeException = null;
    try {
      actualResult = actual.apply(input);
    } catch (RuntimeException e) {
      actualRuntimeException = e;
    }

    assertThat(actualResult).isEqualTo(expectedResult);
    if (expectedRuntimeException != null) {
      assertThat(actualRuntimeException).isNotNull();
      assertThat(actualRuntimeException.getClass()).isEqualTo(expectedRuntimeException.getClass());
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  private static void checkSerialization(Predicate<? super @Nullable Integer> predicate) {
    Predicate<? super @Nullable Integer> reserialized = reserializeAndAssert(predicate);
    assertEvalsLike(predicate, reserialized);
  }
}

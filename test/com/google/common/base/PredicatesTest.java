/*
 * Copyright (C) 2005 Google Inc.
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
import com.google.common.collect.ImmutableSet;
import com.google.testing.util.EqualsTester;
import com.google.testing.util.NullPointerTester;
import com.google.testing.util.SerializableTester;

import junit.framework.TestCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Unit test for {@link Predicates}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class PredicatesTest extends TestCase {
  private static final Predicate<Integer> TRUE = Predicates.alwaysTrue();
  private static final Predicate<Integer> FALSE = Predicates.alwaysFalse();
  private static final Predicate<Integer> NEVER_REACHED =
      new Predicate<Integer>() {
    public boolean apply(Integer i) {
      fail("This predicate should never have been evaluated");
      return false;
    }
  };

  /** Instantiable predicate with reasonable hashCode() and equals() methods. */
  static class IsOdd implements Predicate<Integer>, Serializable {
    private static final long serialVersionUID = 0x150ddL;
    public boolean apply(Integer i) {
      return (i.intValue() & 1) == 1;
    }
    @Override public int hashCode() {
      return 0x150dd;
    }
    @Override public boolean equals(Object obj) {
      return obj instanceof IsOdd;
    }
    @Override public String toString() {
      return "IsOdd";
    }
  }

  /**
   * Generates a new Predicate per call.
   *
   * <p>Creating a new Predicate each time helps catch cases where code is
   * using {@code x == y} instead of {@code x.equals(y)}.
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
    new EqualsTester(TRUE)
        .addEqualObject(Predicates.alwaysTrue())
        .addNotEqualObject(isOdd())
        .addNotEqualObject(Predicates.alwaysFalse())
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
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
    new EqualsTester(FALSE)
        .addEqualObject(Predicates.alwaysFalse())
        .addNotEqualObject(isOdd())
        .addNotEqualObject(Predicates.alwaysTrue())
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  public void testAlwaysFalse_serialization() {
    checkSerialization(Predicates.alwaysFalse());
  }

  /*
   * Tests for Predicates.not(predicate).
   */

  public void testNot_apply() {
    assertEvalsToTrue(Predicates.not(FALSE));
    assertEvalsToFalse(Predicates.not(TRUE));
    assertEvalsLikeOdd(Predicates.not(Predicates.not(isOdd())));
  }

  public void testNot_equality() {
    new EqualsTester(Predicates.not(isOdd()))
        .addEqualObject(Predicates.not(isOdd()))
        .addNotEqualObject(Predicates.not(TRUE))
        .addNotEqualObject(isOdd())
        .testEquals();
  }

  public void testNot_equalityForNotOfKnownValues() {
    /* Would be nice to have .addEqualObject(Predicates.not(FALSE)). */
    new EqualsTester(TRUE)
        .addEqualObject(Predicates.alwaysTrue())
        .addNotEqualObject(FALSE)
        .addNotEqualObject(Predicates.not(TRUE))
        .testEquals();

    /* Would be nice to have .addEqualObject(Predicates.not(TRUE)). */
    new EqualsTester(FALSE)
        .addEqualObject(Predicates.alwaysFalse())
        .addNotEqualObject(TRUE)
        .addNotEqualObject(Predicates.not(FALSE))
        .testEquals();

    /* Would be nice to have .addEqualObject(Predicates.not(notNull())). */
    new EqualsTester(Predicates.isNull())
        .addEqualObject(Predicates.isNull())
        .addNotEqualObject(Predicates.notNull())
        .addNotEqualObject(Predicates.not(Predicates.isNull()))
        .testEquals();

    /* Would be nice to have .addEqualObject(Predicates.not(isNull())). */
    new EqualsTester(Predicates.notNull())
        .addEqualObject(Predicates.notNull())
        .addNotEqualObject(Predicates.isNull())
        .addNotEqualObject(Predicates.not(Predicates.notNull()))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  public void testNot_serialization() {
    checkSerialization(Predicates.not(isOdd()));
  }

  /*
   * Tests for all the different flavors of Predicates.and().
   */

  @SuppressWarnings("unchecked")
  public void testAnd_applyNoArgs() {
    assertEvalsToTrue(Predicates.and());
  }

  @SuppressWarnings("unchecked")
  public void testAnd_equalityNoArgs() {
    new EqualsTester(Predicates.and())
        .addEqualObject(Predicates.and())
        .addNotEqualObject(Predicates.and(FALSE))
        .addNotEqualObject(Predicates.or())
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  @SuppressWarnings("unchecked")
  public void testAnd_serializationNoArgs() {
    checkSerialization(Predicates.and());
  }

  @SuppressWarnings("unchecked")
  public void testAnd_applyOneArg() {
    assertEvalsLikeOdd(Predicates.and(isOdd()));
  }

  @SuppressWarnings("unchecked")
  public void testAnd_equalityOneArg() {
    new EqualsTester(Predicates.and(NEVER_REACHED))
        .addEqualObject(Predicates.and(NEVER_REACHED))
        .addNotEqualObject(Predicates.and(NEVER_REACHED, FALSE))
        .addNotEqualObject(Predicates.and(isOdd()))
        .addNotEqualObject(Predicates.and())
        .addNotEqualObject(Predicates.or(NEVER_REACHED))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  @SuppressWarnings("unchecked")
  public void testAnd_serializationOneArg() {
    checkSerialization(Predicates.and(isOdd()));
  }

  public void testAnd_applyBinary() {
    assertEvalsLikeOdd(Predicates.and(isOdd(), TRUE));
    assertEvalsLikeOdd(Predicates.and(TRUE, isOdd()));
    assertEvalsToFalse(Predicates.and(FALSE, NEVER_REACHED));
  }

  @SuppressWarnings("unchecked")
  public void testAnd_equalityBinary() {
    new EqualsTester(Predicates.and(TRUE, NEVER_REACHED))
        .addEqualObject(Predicates.and(TRUE, NEVER_REACHED))
        .addNotEqualObject(Predicates.and(NEVER_REACHED, TRUE))
        .addNotEqualObject(Predicates.and(TRUE))
        .addNotEqualObject(Predicates.or(TRUE, NEVER_REACHED))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  public void testAnd_serializationBinary() {
    checkSerialization(Predicates.and(TRUE, isOdd()));
  }

  @SuppressWarnings("unchecked")
  public void testAnd_applyTernary() {
    assertEvalsLikeOdd(Predicates.and(isOdd(), TRUE, TRUE));
    assertEvalsLikeOdd(Predicates.and(TRUE, isOdd(), TRUE));
    assertEvalsLikeOdd(Predicates.and(TRUE, TRUE, isOdd()));
    assertEvalsToFalse(Predicates.and(TRUE, FALSE, NEVER_REACHED));
  }

  @SuppressWarnings("unchecked")
  public void testAnd_equalityTernary() {
    new EqualsTester(Predicates.and(TRUE, isOdd(), NEVER_REACHED))
        .addEqualObject(Predicates.and(TRUE, isOdd(), NEVER_REACHED))
        .addNotEqualObject(Predicates.and(isOdd(), NEVER_REACHED, TRUE))
        .addNotEqualObject(Predicates.and(TRUE))
        .addNotEqualObject(Predicates.or(TRUE, isOdd(), NEVER_REACHED))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  @SuppressWarnings("unchecked")
  public void testAnd_serializationTernary() {
    checkSerialization(Predicates.and(TRUE, isOdd(), FALSE));
  }

  @SuppressWarnings("unchecked")
  public void testAnd_applyIterable() {
    Collection<Predicate<Integer>> empty = Arrays.asList();
    assertEvalsToTrue(Predicates.and(empty));
    assertEvalsLikeOdd(Predicates.and(Arrays.asList(isOdd())));
    assertEvalsLikeOdd(Predicates.and(Arrays.asList(TRUE, isOdd())));
    assertEvalsToFalse(Predicates.and(Arrays.asList(FALSE, NEVER_REACHED)));
  }

  @SuppressWarnings("unchecked")
  public void testAnd_equalityIterable() {
    new EqualsTester(Predicates.and(Arrays.asList(TRUE, NEVER_REACHED)))
        .addEqualObject(Predicates.and(Arrays.asList(TRUE, NEVER_REACHED)))
        .addEqualObject(Predicates.and(TRUE, NEVER_REACHED))
        .addNotEqualObject(Predicates.and(FALSE, NEVER_REACHED))
        .addNotEqualObject(Predicates.or(TRUE, NEVER_REACHED))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  @SuppressWarnings("unchecked")
  public void testAnd_serializationIterable() {
    checkSerialization(Predicates.and(Arrays.asList(TRUE, FALSE)));
  }

  @SuppressWarnings("unchecked")
  public void testAnd_arrayDefensivelyCopied() {
    Predicate[] array = {Predicates.alwaysFalse()};
    Predicate<Object> predicate = Predicates.and(array);
    assertFalse(predicate.apply(1));
    array[0] = Predicates.alwaysTrue();
    assertFalse(predicate.apply(1));
  }

  @SuppressWarnings("unchecked")
  public void testAnd_listDefensivelyCopied() {
    List list = new ArrayList<Predicate>();
    Predicate<Object> predicate = Predicates.and(list);
    assertTrue(predicate.apply(1));
    list.add(Predicates.alwaysFalse());
    assertTrue(predicate.apply(1));
  }

  @SuppressWarnings("unchecked")
  public void testAnd_iterableDefensivelyCopied() {
    final List list = new ArrayList<Predicate>();
    Iterable iterable = new Iterable<Predicate>() {
      public Iterator<Predicate> iterator() {
        return list.iterator();
      }
    };
    Predicate<Object> predicate = Predicates.and(iterable);
    assertTrue(predicate.apply(1));
    list.add(Predicates.alwaysFalse());
    assertTrue(predicate.apply(1));
  }

  /*
   * Tests for all the different flavors of Predicates.or().
   */

  @SuppressWarnings("unchecked")
  public void testOr_applyNoArgs() {
    assertEvalsToFalse(Predicates.or());
  }

  @SuppressWarnings("unchecked")
  public void testOr_equalityNoArgs() {
    new EqualsTester(Predicates.or())
        .addEqualObject(Predicates.or())
        .addNotEqualObject(Predicates.or(TRUE))
        .addNotEqualObject(Predicates.and())
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  @SuppressWarnings("unchecked")
  public void testOr_serializationNoArgs() {
    checkSerialization(Predicates.or());
  }

  @SuppressWarnings("unchecked")
  public void testOr_applyOneArg() {
    assertEvalsToTrue(Predicates.or(TRUE));
    assertEvalsToFalse(Predicates.or(FALSE));
  }

  @SuppressWarnings("unchecked")
  public void testOr_equalityOneArg() {
    new EqualsTester(Predicates.or(NEVER_REACHED))
        .addEqualObject(Predicates.or(NEVER_REACHED))
        .addNotEqualObject(Predicates.or(NEVER_REACHED, TRUE))
        .addNotEqualObject(Predicates.or(TRUE))
        .addNotEqualObject(Predicates.or())
        .addNotEqualObject(Predicates.and(NEVER_REACHED))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  @SuppressWarnings("unchecked")
  public void testOr_serializationOneArg() {
    checkSerialization(Predicates.or(isOdd()));
  }

  public void testOr_applyBinary() {
    Predicate<Integer> falseOrFalse = Predicates.or(FALSE, FALSE);
    Predicate<Integer> falseOrTrue = Predicates.or(FALSE, TRUE);
    Predicate<Integer> trueOrAnything = Predicates.or(TRUE, NEVER_REACHED);

    assertEvalsToFalse(falseOrFalse);
    assertEvalsToTrue(falseOrTrue);
    assertEvalsToTrue(trueOrAnything);
  }

  @SuppressWarnings("unchecked")
  public void testOr_equalityBinary() {
    new EqualsTester(Predicates.or(FALSE, NEVER_REACHED))
    .addEqualObject(Predicates.or(FALSE, NEVER_REACHED))
    .addNotEqualObject(Predicates.or(NEVER_REACHED, FALSE))
    .addNotEqualObject(Predicates.or(TRUE))
    .addNotEqualObject(Predicates.and(FALSE, NEVER_REACHED))
    .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  public void testOr_serializationBinary() {
    checkSerialization(Predicates.or(isOdd(), TRUE));
  }

  @SuppressWarnings("unchecked")
  public void testOr_applyTernary() {
    assertEvalsLikeOdd(Predicates.or(isOdd(), FALSE, FALSE));
    assertEvalsLikeOdd(Predicates.or(FALSE, isOdd(), FALSE));
    assertEvalsLikeOdd(Predicates.or(FALSE, FALSE, isOdd()));
    assertEvalsToTrue(Predicates.or(FALSE, TRUE, NEVER_REACHED));
  }

  @SuppressWarnings("unchecked")
  public void testOr_equalityTernary() {
    new EqualsTester(Predicates.or(FALSE, NEVER_REACHED, TRUE))
        .addEqualObject(Predicates.or(FALSE, NEVER_REACHED, TRUE))
        .addNotEqualObject(Predicates.or(TRUE, NEVER_REACHED, FALSE))
        .addNotEqualObject(Predicates.or(TRUE))
        .addNotEqualObject(Predicates.and(FALSE, NEVER_REACHED, TRUE))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  @SuppressWarnings("unchecked")
  public void testOr_serializationTernary() {
    checkSerialization(Predicates.or(FALSE, isOdd(), TRUE));
  }

  @SuppressWarnings("unchecked")
  public void testOr_applyIterable() {
    Predicate<Integer> vacuouslyFalse =
        Predicates.or(Collections.<Predicate<Integer>>emptyList());
    Predicate<Integer> troo = Predicates.or(Collections.singletonList(TRUE));
    /*
     * newLinkedList() takes varargs. TRUE and FALSE are both instances of
     * Predicate<Integer>, so the call is safe.
     */
    Predicate<Integer> trueAndFalse = Predicates.or(Arrays.asList(TRUE, FALSE));

    assertEvalsToFalse(vacuouslyFalse);
    assertEvalsToTrue(troo);
    assertEvalsToTrue(trueAndFalse);
  }

  @SuppressWarnings("unchecked")
  public void testOr_equalityIterable() {
    new EqualsTester(Predicates.or(Arrays.asList(FALSE, NEVER_REACHED)))
        .addEqualObject(Predicates.or(Arrays.asList(FALSE, NEVER_REACHED)))
        .addEqualObject(Predicates.or(FALSE, NEVER_REACHED))
        .addNotEqualObject(Predicates.or(TRUE, NEVER_REACHED))
        .addNotEqualObject(Predicates.and(FALSE, NEVER_REACHED))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  @SuppressWarnings("unchecked")
  public void testOr_serializationIterable() {
    Predicate<Integer> pre = Predicates.or(Arrays.asList(TRUE, FALSE));
    Predicate<Integer> post = SerializableTester.reserializeAndAssert(pre);
    assertEquals(pre.apply(0), post.apply(0));
  }

  @SuppressWarnings("unchecked")
  public void testOr_arrayDefensivelyCopied() {
    Predicate[] array = {Predicates.alwaysFalse()};
    Predicate<Object> predicate = Predicates.or(array);
    assertFalse(predicate.apply(1));
    array[0] = Predicates.alwaysTrue();
    assertFalse(predicate.apply(1));
  }

  @SuppressWarnings("unchecked")
  public void testOr_listDefensivelyCopied() {
    List list = new ArrayList<Predicate>();
    Predicate<Object> predicate = Predicates.or(list);
    assertFalse(predicate.apply(1));
    list.add(Predicates.alwaysTrue());
    assertFalse(predicate.apply(1));
  }

  @SuppressWarnings("unchecked")
  public void testOr_iterableDefensivelyCopied() {
    final List list = new ArrayList<Predicate>();
    Iterable iterable = new Iterable<Predicate>() {
      public Iterator<Predicate> iterator() {
        return list.iterator();
      }
    };
    Predicate<Object> predicate = Predicates.or(iterable);
    assertFalse(predicate.apply(1));
    list.add(Predicates.alwaysTrue());
    assertFalse(predicate.apply(1));
  }

  /*
   * Tests for Predicates.equalTo(x).
   */

  public void testIsEqualTo_apply() {
    Predicate<Integer> isOne = Predicates.equalTo(1);

    assertTrue(isOne.apply(1));
    assertFalse(isOne.apply(2));
    assertFalse(isOne.apply(null));
  }

  public void testIsEqualTo_equality() {
    new EqualsTester(Predicates.equalTo(1))
        .addEqualObject(Predicates.equalTo(1))
        .addNotEqualObject(Predicates.equalTo(2))
        .addNotEqualObject(Predicates.equalTo(null))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  public void testIsEqualTo_serialization() {
    checkSerialization(Predicates.equalTo(1));
  }

  public void testIsEqualToNull_apply() {
    Predicate<Integer> isNull = Predicates.equalTo(null);
    assertTrue(isNull.apply(null));
    assertFalse(isNull.apply(1));
  }

  public void testIsEqualToNull_equality() {
    new EqualsTester(Predicates.equalTo(null))
        .addEqualObject(Predicates.equalTo(null))
        .addNotEqualObject(Predicates.equalTo(1))
        .addNotEqualObject(Predicates.equalTo("null"))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  public void testIsEqualToNull_serialization() {
    checkSerialization(Predicates.equalTo(null));
  }

  /**
   * Tests for Predicates.instanceOf(x).
   * TODO: Fix the comment style after fixing annotation stripper to remove
   * comments properly.  Currently, all tests before the comments are removed
   * as well.
   */

  @GwtIncompatible("Predicates.instanceOf")
  public void testIsInstanceOf_apply() {
    Predicate<Object> isInteger = Predicates.instanceOf(Integer.class);

    assertTrue(isInteger.apply(1));
    assertFalse(isInteger.apply(2.0f));
    assertFalse(isInteger.apply(""));
    assertFalse(isInteger.apply(null));
  }

  @GwtIncompatible("Predicates.instanceOf")
  public void testIsInstanceOf_subclass() {
    Predicate<Object> isNumber = Predicates.instanceOf(Number.class);

    assertTrue(isNumber.apply(1));
    assertTrue(isNumber.apply(2.0f));
    assertFalse(isNumber.apply(""));
    assertFalse(isNumber.apply(null));
  }

  @GwtIncompatible("Predicates.instanceOf")
  public void testIsInstanceOf_interface() {
    Predicate<Object> isComparable = Predicates.instanceOf(Comparable.class);

    assertTrue(isComparable.apply(1));
    assertTrue(isComparable.apply(2.0f));
    assertTrue(isComparable.apply(""));
    assertFalse(isComparable.apply(null));
  }

  @GwtIncompatible("Predicates.instanceOf")
  public void testIsInstanceOf_equality() {
    new EqualsTester(Predicates.instanceOf(Integer.class))
        .addEqualObject(Predicates.instanceOf(Integer.class))
        .addNotEqualObject(Predicates.instanceOf(Number.class))
        .addNotEqualObject(Predicates.instanceOf(Float.class))
        .testEquals();
  }

  @GwtIncompatible("Predicates.instanceOf, SerializableTester")
  public void testIsInstanceOf_serialization() {
    checkSerialization(Predicates.instanceOf(Integer.class));
  }

  /*
   * Tests for Predicates.isNull()
   */

  public void testIsNull_apply() {
    Predicate<Integer> isNull = Predicates.isNull();
    assertTrue(isNull.apply(null));
    assertFalse(isNull.apply(1));
  }

  public void testIsNull_equality() {
    new EqualsTester(Predicates.isNull())
        .addEqualObject(Predicates.isNull())
        .addNotEqualObject(Predicates.notNull())
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  public void testIsNull_serialization() {
    Predicate<String> pre = Predicates.isNull();
    Predicate<String> post = SerializableTester.reserializeAndAssert(pre);
    assertEquals(pre.apply("foo"), post.apply("foo"));
    assertEquals(pre.apply(null), post.apply(null));
  }

  public void testNotNull_apply() {
    Predicate<Integer> notNull = Predicates.notNull();
    assertFalse(notNull.apply(null));
    assertTrue(notNull.apply(1));
  }

  public void testNotNull_equality() {
    new EqualsTester(Predicates.notNull())
        .addEqualObject(Predicates.notNull())
        .addNotEqualObject(Predicates.isNull())
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  public void testNotNull_serialization() {
    checkSerialization(Predicates.notNull());
  }

  public void testIn_apply() {
    Collection<Integer> nums = Arrays.asList(1, 5);
    Predicate<Integer> isOneOrFive = Predicates.in(nums);

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

    new EqualsTester(Predicates.in(nums))
        .addEqualObject(Predicates.in(nums))
        .addEqualObject(Predicates.in(sameOrder))
        .addEqualObject(Predicates.in(differentOrder))
        .addNotEqualObject(Predicates.in(differentNums))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  public void testIn_serialization() {
    checkSerialization(Predicates.in(Arrays.asList(1, 2, 3, null)));
  }

  public void testIn_handlesNullPointerException() {
    class CollectionThatThrowsNPE<T> extends ArrayList<T> {
      private static final long serialVersionUID = 1L;

      @Override public boolean contains(Object element) {
        Preconditions.checkNotNull(element);
        return super.contains(element);
      }
    }
    Collection<Integer> nums = new CollectionThatThrowsNPE<Integer>();
    Predicate<Integer> isFalse = Predicates.in(nums);
    assertFalse(isFalse.apply(null));
  }

  public void testIn_handlesClassCastException() {
    class CollectionThatThrowsCCE<T> extends ArrayList<T> {
      private static final long serialVersionUID = 1L;

      @Override public boolean contains(Object element) {
        throw new ClassCastException("");
      }
    }
    Collection<Integer> nums = new CollectionThatThrowsCCE<Integer>();
    nums.add(3);
    Predicate<Integer> isThree = Predicates.in(nums);
    assertFalse(isThree.apply(3));
  }

  /*
   * Tests that compilation will work when applying explicit types.
   */
  public void testIn_compilesWithExplicitSupertype() {
    Collection<Number> nums = ImmutableSet.of();
    Predicate<Number> p1 = Predicates.in(nums);
    Predicate<Object> p2 = Predicates.<Object>in(nums);
    // The next two lines are not expected to compile.
    // Predicate<Integer> p3 = Predicates.in(nums);
    // Predicate<Integer> p4 = Predicates.<Integer>in(nums);
  }

  @GwtIncompatible("NullPointerTester")
  public void testNullPointerExceptions() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Predicates.class);
  }

  @SuppressWarnings("unchecked") // varargs
  @GwtIncompatible("SerializbleTester")
  public void testCascadingSerialization() throws Exception {
    // Eclipse says Predicate<Integer>; javac says Predicate<Object>.
    Predicate<? super Integer> nasty = Predicates.not(Predicates.and(
        Predicates.or(
            Predicates.equalTo((Object) 1), Predicates.equalTo(null),
            Predicates.alwaysFalse(), Predicates.alwaysTrue(),
            Predicates.isNull(), Predicates.notNull(),
            Predicates.in(Arrays.asList(1)))));
    assertEvalsToFalse(nasty);

    Predicate<? super Integer> stillNasty =
        SerializableTester.reserializeAndAssert(nasty);

    assertEvalsToFalse(stillNasty);
  }

  // enum singleton pattern
  private enum TrimStringFunction implements Function<String, String> {
    INSTANCE;

    public String apply(String string) {
      return string.trim();
    }
  }

  public void testCompose() {
    Function<String, String> trim = TrimStringFunction.INSTANCE;
    Predicate<String> equalsFoo = Predicates.equalTo("Foo");
    Predicate<String> equalsBar = Predicates.equalTo("Bar");
    Predicate<String> trimEqualsFoo = Predicates.compose(equalsFoo, trim);
    Function<String, String> identity = Functions.identity();

    assertTrue(trimEqualsFoo.apply("Foo"));
    assertTrue(trimEqualsFoo.apply("   Foo   "));
    assertFalse(trimEqualsFoo.apply("Foo-b-que"));

    new EqualsTester(trimEqualsFoo)
        .addEqualObject(Predicates.compose(equalsFoo, trim))
        .addNotEqualObject(equalsFoo)
        .addNotEqualObject(trim)
        .addNotEqualObject(Predicates.compose(equalsFoo, identity))
        .addNotEqualObject(Predicates.compose(equalsBar, trim))
        .testEquals();
  }

  @GwtIncompatible("SerializableTester")
  public void testComposeSerialization() {
    Function<String, String> trim = TrimStringFunction.INSTANCE;
    Predicate<String> equalsFoo = Predicates.equalTo("Foo");
    Predicate<String> trimEqualsFoo = Predicates.compose(equalsFoo, trim);
    SerializableTester.reserializeAndAssert(trimEqualsFoo);
  }

  /**
   * Tests for Predicates.contains(Pattern) and .containsPattern(String).
   * We assume the regex level works, so there are only trivial tests of that
   * aspect.
   * TODO: Fix comment style once annotation stripper is fixed.
   */

  @GwtIncompatible("Predicates.containsPattern")
  public void testContainsPattern_apply() {
    Predicate<CharSequence> isFoobar =
        Predicates.containsPattern("^Fo.*o.*bar$");
    assertTrue(isFoobar.apply("Foxyzoabcbar"));
    assertFalse(isFoobar.apply("Foobarx"));
  }

  @GwtIncompatible("Predicates.containsPattern")
  public void testContains_apply() {
    Predicate<CharSequence> isFoobar =
        Predicates.contains(Pattern.compile("^Fo.*o.*bar$"));

    assertTrue(isFoobar.apply("Foxyzoabcbar"));
    assertFalse(isFoobar.apply("Foobarx"));
  }

  @GwtIncompatible("NullPointerTester")
  public void testContainsPattern_nulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    Predicate<CharSequence> isWooString = Predicates.containsPattern("Woo");

    tester.testAllPublicInstanceMethods(isWooString);
  }

  @GwtIncompatible("NullPointerTester")
  public void testContains_nulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    Predicate<CharSequence> isWooPattern =
        Predicates.contains(Pattern.compile("Woo"));

    tester.testAllPublicInstanceMethods(isWooPattern);
  }

  @GwtIncompatible("SerializableTester")
  public void testContainsPattern_serialization() {
    Predicate<CharSequence> pre = Predicates.containsPattern("foo");
    Predicate<CharSequence> post = SerializableTester.reserializeAndAssert(pre);
    assertEquals(pre.apply("foo"), post.apply("foo"));
  }

  @GwtIncompatible("java.util.regex.Pattern")
  public void testContains_equals() {
    new EqualsTester()
        .addEqualityGroup(
            Predicates.contains(Pattern.compile("foo")),
            Predicates.containsPattern("foo"))
        .addEqualityGroup(
            Predicates.contains(
                Pattern.compile("foo", Pattern.CASE_INSENSITIVE)))
        .addEqualityGroup(
            Predicates.containsPattern("bar"))
        .testEquals();
      }

  public void checkConsistency(
      Predicate<? super Integer> expected, Predicate<? super Integer> actual) {
    assertEvalsLike(expected, actual);
    assertEquals(actual.toString() + " should hash like " + expected.toString(),
        expected.hashCode(), actual.hashCode());
  }

  public void testHashCodeForBooleanOperationsIsConsistentWithBooleanLogic() {
    /*
     * This isn't a "requirement" yet, as much as it's a "nice to have for
     * future design."
     *
     * Maybe it will be possible to eventually have these predicates do
     * certain simplifying logical transformations to simpler equivalent
     * forms.  If so, this checks that the hash codes have been chosen in such
     * a way that the fundamental building-block operations:
     *
     *    alwaysTrue()
     *    alwaysFalse()
     *    not(p)
     *    and(p1, p2)
     *    or(p1, p2)
     *
     * have hashCode() calculations that coincidentally cause equivalent logical
     * expressions to have equivalent hashCode() values.
     */
    Predicate<Integer> p1 = Predicates.isNull();
    Predicate<Integer> p2 = isOdd();
    Predicate<Integer> p3 = new Predicate<Integer>() {
      public boolean apply(Integer i) {
        return (Integer.bitCount(i) & 1) == 1;
      }
      @Override public String toString() {
        return "oddBitCount";
      }
    };

    checkConsistency(
        p1,
        Predicates.not(Predicates.not(p1)));

    checkConsistency(
        Predicates.and(Predicates.not(p1), Predicates.not(p2)),
        Predicates.not(Predicates.or(p1, p2)));

    checkConsistency(
        Predicates.or(Predicates.not(p1), Predicates.not(p2)),
        Predicates.not(Predicates.and(p1, p2)));

    checkConsistency(
        Predicates.and(Predicates.and(p1, p2), p3),
        Predicates.and(p1, Predicates.and(p2, p3)));

    checkConsistency(
        Predicates.or(Predicates.or(p1, p2), p3),
        Predicates.or(p1, Predicates.or(p2, p3)));

    checkConsistency(
        Predicates.or(Predicates.and(p1, p2), p3),
        Predicates.and(Predicates.or(p1, p3), Predicates.or(p2, p3)));

    checkConsistency(
        Predicates.and(Predicates.or(p1, p2), p3),
        Predicates.or(Predicates.and(p1, p3), Predicates.and(p2, p3)));

    /*
     * Now that alwaysFalse and alwaysTrue follow the enum singleton pattern,
     * the following tests can't call checkConsistency to compare the hash
     * codes.
     */

    assertEvalsLike(
        Predicates.alwaysTrue(),
        Predicates.not(Predicates.alwaysFalse()));

    assertEvalsLike(
        Predicates.alwaysFalse(),
        Predicates.not(Predicates.alwaysTrue()));

    assertEvalsLike(
        Predicates.alwaysFalse(),
        Predicates.and(p1, Predicates.not(p1)));

    assertEvalsLike(
        Predicates.alwaysTrue(),
        Predicates.or(p1, Predicates.not(p1)));
  }

  private static void assertEvalsToTrue(Predicate<? super Integer> predicate) {
    assertTrue(predicate.apply(0));
    assertTrue(predicate.apply(1));
    assertTrue(predicate.apply(null));
  }

  private static void assertEvalsToFalse(Predicate<? super Integer> predicate) {
    assertFalse(predicate.apply(0));
    assertFalse(predicate.apply(1));
    assertFalse(predicate.apply(null));
  }

  private static void assertEvalsLikeOdd(Predicate<? super Integer> predicate) {
    assertEvalsLike(isOdd(), predicate);
  }

  private static void assertEvalsLike(
      Predicate<? super Integer> expected,
      Predicate<? super Integer> actual) {
    assertEvalsLike(expected, actual, 0);
    assertEvalsLike(expected, actual, 1);
    assertEvalsLike(expected, actual, null);
  }

  private static void assertEvalsLike(
      Predicate<? super Integer> expected,
      Predicate<? super Integer> actual,
      Integer input) {
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

    assertEquals(expectedResult, actualResult);
    if (expectedRuntimeException != null) {
      assertNotNull(actualRuntimeException);
      assertEquals(
          expectedRuntimeException.getClass(),
          actualRuntimeException.getClass());
    }
  }

  @GwtIncompatible("SerializableTester")
  private static void checkSerialization(Predicate<? super Integer> predicate) {
    Predicate<? super Integer> reserialized =
        SerializableTester.reserializeAndAssert(predicate);
    assertEvalsLike(predicate, reserialized);
  }
}

/*
 * Copyright (C) 2010 The Guava Authors
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
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Equivalence.Wrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.EquivalenceTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link Equivalence}.
 *
 * @author Jige Yu
 */
@ElementTypesAreNonnullByDefault
@GwtCompatible(emulated = true)
public class EquivalenceTest extends TestCase {
  public void testPairwiseEquivalent() {
    EquivalenceTester.of(Equivalence.equals().<String>pairwise())
        .addEquivalenceGroup(ImmutableList.<String>of())
        .addEquivalenceGroup(ImmutableList.of("a"))
        .addEquivalenceGroup(ImmutableList.of("b"))
        .addEquivalenceGroup(ImmutableList.of("a", "b"), ImmutableList.of("a", "b"))
        .test();
  }

  public void testPairwiseEquivalent_equals() {
    new EqualsTester()
        .addEqualityGroup(Equivalence.equals().pairwise(), Equivalence.equals().pairwise())
        .addEqualityGroup(Equivalence.identity().pairwise())
        .testEquals();
  }

  private enum LengthFunction implements Function<String, Integer> {
    INSTANCE;

    @Override
    public Integer apply(String input) {
      return input.length();
    }
  }

  private static final Equivalence<String> LENGTH_EQUIVALENCE =
      Equivalence.equals().onResultOf(LengthFunction.INSTANCE);

  public void testWrap() {
    new EqualsTester()
        .addEqualityGroup(
            LENGTH_EQUIVALENCE.wrap("hello"),
            LENGTH_EQUIVALENCE.wrap("hello"),
            LENGTH_EQUIVALENCE.wrap("world"))
        .addEqualityGroup(LENGTH_EQUIVALENCE.wrap("hi"), LENGTH_EQUIVALENCE.wrap("yo"))
        .addEqualityGroup(
            LENGTH_EQUIVALENCE.<@Nullable String>wrap(null),
            LENGTH_EQUIVALENCE.<@Nullable String>wrap(null))
        .addEqualityGroup(Equivalence.equals().wrap("hello"))
        .addEqualityGroup(Equivalence.equals().<@Nullable Object>wrap(null))
        .testEquals();
  }

  public void testWrap_get() {
    String test = "test";
    Wrapper<String> wrapper = LENGTH_EQUIVALENCE.wrap(test);
    assertSame(test, wrapper.get());
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testSerialization() {
    SerializableTester.reserializeAndAssert(LENGTH_EQUIVALENCE.wrap("hello"));
    SerializableTester.reserializeAndAssert(Equivalence.equals());
    SerializableTester.reserializeAndAssert(Equivalence.identity());
  }

  private static class IntValue {
    private final int value;

    IntValue(int value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "value = " + value;
    }
  }

  public void testOnResultOf() {
    EquivalenceTester.of(Equivalence.equals().onResultOf(Functions.toStringFunction()))
        .addEquivalenceGroup(new IntValue(1), new IntValue(1))
        .addEquivalenceGroup(new IntValue(2))
        .test();
  }

  public void testOnResultOf_equals() {
    new EqualsTester()
        .addEqualityGroup(
            Equivalence.identity().onResultOf(Functions.toStringFunction()),
            Equivalence.identity().onResultOf(Functions.toStringFunction()))
        .addEqualityGroup(Equivalence.equals().onResultOf(Functions.toStringFunction()))
        .addEqualityGroup(Equivalence.identity().onResultOf(Functions.identity()))
        .testEquals();
  }

  public void testEquivalentTo() {
    Predicate<@Nullable Object> equalTo1 = Equivalence.equals().equivalentTo("1");
    assertTrue(equalTo1.apply("1"));
    assertFalse(equalTo1.apply("2"));
    assertFalse(equalTo1.apply(null));
    Predicate<@Nullable Object> isNull = Equivalence.equals().equivalentTo(null);
    assertFalse(isNull.apply("1"));
    assertFalse(isNull.apply("2"));
    assertTrue(isNull.apply(null));

    new EqualsTester()
        .addEqualityGroup(equalTo1, Equivalence.equals().equivalentTo("1"))
        .addEqualityGroup(isNull)
        .addEqualityGroup(Equivalence.identity().equivalentTo("1"))
        .testEquals();
  }

  /*
   * We use large numbers to avoid the integer cache. Normally, we'd accomplish that merely by using
   * `new Integer` (as we do) instead of `Integer.valueOf`. However, under J2KT, `new Integer`
   * gets translated back to `Integer.valueOf` because that is the only thing J2KT can support. And
   * anyway, it's nice to avoid `Integer.valueOf` because the Android toolchain optimizes multiple
   * `Integer.valueOf` calls into one! So we stick with the deprecated `Integer` constructor.
   */

  public void testEqualsEquivalent() {
    EquivalenceTester.of(Equivalence.equals())
        .addEquivalenceGroup(new Integer(42_000_000), 42_000_000)
        .addEquivalenceGroup("a")
        .test();
  }

  public void testIdentityEquivalent() {
    EquivalenceTester.of(Equivalence.identity())
        .addEquivalenceGroup(new Integer(42_000_000))
        .addEquivalenceGroup(new Integer(42_000_000))
        .addEquivalenceGroup("a")
        .test();
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(Equivalence.equals(), Equivalence.equals())
        .addEqualityGroup(Equivalence.identity(), Equivalence.identity())
        .testEquals();
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNulls() throws NoSuchMethodException {
    NullPointerTester tester = new NullPointerTester();
    // Necessary until JDK15:
    // https://bugs.openjdk.org/browse/JDK-8202469
    tester.ignore(Equivalence.class.getMethod("wrap", Object.class));

    tester.testAllPublicStaticMethods(Equivalence.class);
    tester.testAllPublicInstanceMethods(Equivalence.equals());
    tester.testAllPublicInstanceMethods(Equivalence.identity());
  }
}

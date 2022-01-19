package com.google.common.collect;

import com.google.common.collect.AndroidAccessToCollectorsTest.MyEnum;
import com.google.common.testing.CollectorTester;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import junit.framework.TestCase;

public final class AndroidAccessToCollectorsToImmutableEnumSetTest extends TestCase {

  private static Collector<MyEnum, ?, ImmutableSet<MyEnum>> collector() {
    return AndroidAccessToCollectors.toImmutableEnumSet();
  }

  private static final BiPredicate<ImmutableSet<MyEnum>, ImmutableSet<MyEnum>> EQUIVALENCE =
      AndroidAccessToCollectorsTest.onResultOf(ImmutableSet::asList);

  public void testToImmutableEnumSetTest() {
    CollectorTester.of(collector(), EQUIVALENCE)
        .expectCollects(ImmutableSet.of())
        .expectCollects(ImmutableSet.of(MyEnum.A), MyEnum.A)
        .expectCollects(ImmutableSet.of(MyEnum.A, MyEnum.B), MyEnum.A, MyEnum.B)
        .expectCollects(
            ImmutableSet.of(MyEnum.A, MyEnum.B, MyEnum.C, MyEnum.D),
            MyEnum.A,
            MyEnum.B,
            MyEnum.A,
            MyEnum.C,
            MyEnum.C,
            MyEnum.A,
            MyEnum.D);
  }
}

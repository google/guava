/*
 * Copyright (C) 2017 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.primitives;

import static com.google.common.primitives.TestPlatform.reduceIterationsIfGwt;
import static com.google.common.testing.SerializableTester.reserialize;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.testing.EqualsTester;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.DoubleStream;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/** @author Kevin Bourrillion */
@GwtCompatible(emulated = true)
public class ImmutableDoubleArrayTest extends TestCase {
  // Test all creation paths very lazily: by assuming asList() works

  public void testOf0() {
    assertThat(ImmutableDoubleArray.of().asList()).isEmpty();
  }

  public void testOf1() {
    assertThat(ImmutableDoubleArray.of(0).asList()).containsExactly(0.0);
  }

  public void testOf2() {
    assertThat(ImmutableDoubleArray.of(0, 1).asList()).containsExactly(0.0, 1.0).inOrder();
  }

  public void testOf3() {
    assertThat(ImmutableDoubleArray.of(0, 1, 3).asList()).containsExactly(0.0, 1.0, 3.0).inOrder();
  }

  public void testOf4() {
    assertThat(ImmutableDoubleArray.of(0, 1, 3, 6).asList())
        .containsExactly(0.0, 1.0, 3.0, 6.0)
        .inOrder();
  }

  public void testOf5() {
    assertThat(ImmutableDoubleArray.of(0, 1, 3, 6, 10).asList())
        .containsExactly(0.0, 1.0, 3.0, 6.0, 10.0)
        .inOrder();
  }

  public void testOf6() {
    assertThat(ImmutableDoubleArray.of(0, 1, 3, 6, 10, 15).asList())
        .containsExactly(0.0, 1.0, 3.0, 6.0, 10.0, 15.0)
        .inOrder();
  }

  public void testOf7() {
    assertThat(ImmutableDoubleArray.of(0, 1, 3, 6, 10, 15, 21).asList())
        .containsExactly(0.0, 1.0, 3.0, 6.0, 10.0, 15.0, 21.0)
        .inOrder();
  }

  public void testCopyOf_array_empty() {
    /*
     * We don't guarantee the same-as property, so we aren't obligated to test it. However, it's
     * useful in testing - when two things are the same then one can't have bugs the other doesn't.
     */
    assertThat(ImmutableDoubleArray.copyOf(new double[0]))
        .isSameInstanceAs(ImmutableDoubleArray.of());
  }

  public void testCopyOf_array_nonempty() {
    double[] array = new double[] {0, 1, 3};
    ImmutableDoubleArray iia = ImmutableDoubleArray.copyOf(array);
    array[2] = 2;
    assertThat(iia.asList()).containsExactly(0.0, 1.0, 3.0).inOrder();
  }

  public void testCopyOf_iterable_notCollection_empty() {
    Iterable<Double> iterable = iterable(Collections.<Double>emptySet());
    assertThat(ImmutableDoubleArray.copyOf(iterable)).isSameInstanceAs(ImmutableDoubleArray.of());
  }

  public void testCopyOf_iterable_notCollection_nonempty() {
    List<Double> list = Arrays.asList(0.0, 1.0, 3.0);
    ImmutableDoubleArray iia = ImmutableDoubleArray.copyOf(iterable(list));
    list.set(2, 2.0);
    assertThat(iia.asList()).containsExactly(0.0, 1.0, 3.0).inOrder();
  }

  public void testCopyOf_iterable_collection_empty() {
    Iterable<Double> iterable = Collections.emptySet();
    assertThat(ImmutableDoubleArray.copyOf(iterable)).isSameInstanceAs(ImmutableDoubleArray.of());
  }

  public void testCopyOf_iterable_collection_nonempty() {
    List<Double> list = Arrays.asList(0.0, 1.0, 3.0);
    ImmutableDoubleArray iia = ImmutableDoubleArray.copyOf((Iterable<Double>) list);
    list.set(2, 2.0);
    assertThat(iia.asList()).containsExactly(0.0, 1.0, 3.0).inOrder();
  }

  public void testCopyOf_collection_empty() {
    Collection<Double> iterable = Collections.emptySet();
    assertThat(ImmutableDoubleArray.copyOf(iterable)).isSameInstanceAs(ImmutableDoubleArray.of());
  }

  public void testCopyOf_collection_nonempty() {
    List<Double> list = Arrays.asList(0.0, 1.0, 3.0);
    ImmutableDoubleArray iia = ImmutableDoubleArray.copyOf(list);
    list.set(2, 2.0);
    assertThat(iia.asList()).containsExactly(0.0, 1.0, 3.0).inOrder();
  }

  public void testCopyOf_stream() {
    assertThat(ImmutableDoubleArray.copyOf(DoubleStream.empty()))
        .isSameInstanceAs(ImmutableDoubleArray.of());
    assertThat(ImmutableDoubleArray.copyOf(DoubleStream.of(0, 1, 3)).asList())
        .containsExactly(0.0, 1.0, 3.0)
        .inOrder();
  }

  public void testBuilder_presize_zero() {
    ImmutableDoubleArray.Builder builder = ImmutableDoubleArray.builder(0);
    builder.add(5.0);
    ImmutableDoubleArray array = builder.build();
    assertThat(array.asList()).containsExactly(5.0);
  }

  public void testBuilder_presize_negative() {
    try {
      ImmutableDoubleArray.builder(-1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  /**
   * If there's a bug in builder growth, we wouldn't know how to expose it. So, brute force the hell
   * out of it for a while and see what happens.
   */
  public void testBuilder_bruteForce() {
    for (int i = 0; i < reduceIterationsIfGwt(100); i++) {
      ImmutableDoubleArray.Builder builder = ImmutableDoubleArray.builder(RANDOM.nextInt(20));
      AtomicInteger counter = new AtomicInteger(0);
      while (counter.get() < 1000) {
        BuilderOp op = BuilderOp.randomOp();
        op.doIt(builder, counter);
      }
      ImmutableDoubleArray iia = builder.build();
      for (int j = 0; j < iia.length(); j++) {
        assertThat(iia.get(j)).isEqualTo((double) j);
      }
    }
  }

  private enum BuilderOp {
    ADD_ONE {
      @Override
      void doIt(ImmutableDoubleArray.Builder builder, AtomicInteger counter) {
        builder.add(counter.getAndIncrement());
      }
    },
    ADD_ARRAY {
      @Override
      void doIt(ImmutableDoubleArray.Builder builder, AtomicInteger counter) {
        double[] array = new double[RANDOM.nextInt(10)];
        for (int i = 0; i < array.length; i++) {
          array[i] = counter.getAndIncrement();
        }
        builder.addAll(array);
      }
    },
    ADD_COLLECTION {
      @Override
      void doIt(ImmutableDoubleArray.Builder builder, AtomicInteger counter) {
        List<Double> list = new ArrayList<>();
        double num = RANDOM.nextInt(10);
        for (int i = 0; i < num; i++) {
          list.add((double) counter.getAndIncrement());
        }
        builder.addAll(list);
      }
    },
    ADD_ITERABLE {
      @Override
      void doIt(ImmutableDoubleArray.Builder builder, AtomicInteger counter) {
        List<Double> list = new ArrayList<>();
        double num = RANDOM.nextInt(10);
        for (int i = 0; i < num; i++) {
          list.add((double) counter.getAndIncrement());
        }
        builder.addAll(iterable(list));
      }
    },
    ADD_STREAM {
      @Override
      void doIt(ImmutableDoubleArray.Builder builder, AtomicInteger counter) {
        double[] array = new double[RANDOM.nextInt(10)];
        for (int i = 0; i < array.length; i++) {
          array[i] = counter.getAndIncrement();
        }
        builder.addAll(Arrays.stream(array));
      }
    },
    ADD_IIA {
      @Override
      void doIt(ImmutableDoubleArray.Builder builder, AtomicInteger counter) {
        double[] array = new double[RANDOM.nextInt(10)];
        for (int i = 0; i < array.length; i++) {
          array[i] = counter.getAndIncrement();
        }
        builder.addAll(ImmutableDoubleArray.copyOf(array));
      }
    },
    ADD_LARGER_ARRAY {
      @Override
      void doIt(ImmutableDoubleArray.Builder builder, AtomicInteger counter) {
        double[] array = new double[RANDOM.nextInt(200) + 200];
        for (int i = 0; i < array.length; i++) {
          array[i] = counter.getAndIncrement();
        }
        builder.addAll(array);
      }
    },
    ;

    static final BuilderOp[] values = values();

    static BuilderOp randomOp() {
      return values[RANDOM.nextInt(values.length)];
    }

    abstract void doIt(ImmutableDoubleArray.Builder builder, AtomicInteger counter);
  }

  private static final Random RANDOM = new Random(42);

  public void testLength() {
    assertThat(ImmutableDoubleArray.of().length()).isEqualTo(0);
    assertThat(ImmutableDoubleArray.of(0).length()).isEqualTo(1);
    assertThat(ImmutableDoubleArray.of(0, 1, 3).length()).isEqualTo(3);
    assertThat(ImmutableDoubleArray.of(0, 1, 3).subArray(1, 1).length()).isEqualTo(0);
    assertThat(ImmutableDoubleArray.of(0, 1, 3).subArray(1, 2).length()).isEqualTo(1);
  }

  public void testIsEmpty() {
    assertThat(ImmutableDoubleArray.of().isEmpty()).isTrue();
    assertThat(ImmutableDoubleArray.of(0).isEmpty()).isFalse();
    assertThat(ImmutableDoubleArray.of(0, 1, 3).isEmpty()).isFalse();
    assertThat(ImmutableDoubleArray.of(0, 1, 3).subArray(1, 1).isEmpty()).isTrue();
    assertThat(ImmutableDoubleArray.of(0, 1, 3).subArray(1, 2).isEmpty()).isFalse();
  }

  public void testGet_good() {
    ImmutableDoubleArray iia = ImmutableDoubleArray.of(0, 1, 3);
    assertThat(iia.get(0)).isEqualTo(0.0);
    assertThat(iia.get(2)).isEqualTo(3.0);
    assertThat(iia.subArray(1, 3).get(1)).isEqualTo(3.0);
  }

  public void testGet_bad() {
    ImmutableDoubleArray iia = ImmutableDoubleArray.of(0, 1, 3);
    try {
      iia.get(-1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      iia.get(3);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }

    iia = iia.subArray(1, 2);
    try {
      iia.get(-1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  public void testIndexOf() {
    ImmutableDoubleArray iia = ImmutableDoubleArray.of(1, 1, 2, 3, 5, 8);
    assertThat(iia.indexOf(1)).isEqualTo(0);
    assertThat(iia.indexOf(8)).isEqualTo(5);
    assertThat(iia.indexOf(4)).isEqualTo(-1);
    assertThat(ImmutableDoubleArray.of(13).indexOf(13)).isEqualTo(0);
    assertThat(ImmutableDoubleArray.of().indexOf(21)).isEqualTo(-1);
    assertThat(iia.subArray(1, 5).indexOf(1)).isEqualTo(0);
  }

  public void testIndexOf_specialValues() {
    ImmutableDoubleArray iia =
        ImmutableDoubleArray.of(-0.0, 0.0, Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NaN);
    assertThat(iia.indexOf(-0.0)).isEqualTo(0);
    assertThat(iia.indexOf(0.0)).isEqualTo(1);
    assertThat(iia.indexOf(Double.MAX_VALUE)).isEqualTo(2);
    assertThat(iia.indexOf(Double.POSITIVE_INFINITY)).isEqualTo(3);
    assertThat(iia.indexOf(Double.NaN)).isEqualTo(4);
  }

  public void testLastIndexOf() {
    ImmutableDoubleArray iia = ImmutableDoubleArray.of(1, 1, 2, 3, 5, 8);
    assertThat(iia.lastIndexOf(1)).isEqualTo(1);
    assertThat(iia.lastIndexOf(8)).isEqualTo(5);
    assertThat(iia.lastIndexOf(4)).isEqualTo(-1);
    assertThat(ImmutableDoubleArray.of(13).lastIndexOf(13)).isEqualTo(0);
    assertThat(ImmutableDoubleArray.of().lastIndexOf(21)).isEqualTo(-1);
    assertThat(iia.subArray(1, 5).lastIndexOf(1)).isEqualTo(0);
  }

  public void testContains() {
    ImmutableDoubleArray iia = ImmutableDoubleArray.of(1, 1, 2, 3, 5, 8);
    assertThat(iia.contains(1)).isTrue();
    assertThat(iia.contains(8)).isTrue();
    assertThat(iia.contains(4)).isFalse();
    assertThat(ImmutableDoubleArray.of(13).contains(13)).isTrue();
    assertThat(ImmutableDoubleArray.of().contains(21)).isFalse();
    assertThat(iia.subArray(1, 5).contains(1)).isTrue();
  }

  public void testForEach() {
    ImmutableDoubleArray.of().forEach(i -> fail());
    ImmutableDoubleArray.of(0, 1, 3).subArray(1, 1).forEach(i -> fail());

    AtomicInteger count = new AtomicInteger(0);
    ImmutableDoubleArray.of(0, 1, 2, 3)
        .forEach(i -> assertThat(i).isEqualTo((double) count.getAndIncrement()));
    assertThat(count.get()).isEqualTo(4);
  }

  public void testStream() {
    ImmutableDoubleArray.of().stream().forEach(i -> fail());
    ImmutableDoubleArray.of(0, 1, 3).subArray(1, 1).stream().forEach(i -> fail());
    assertThat(ImmutableDoubleArray.of(0, 1, 3).stream().toArray())
        .isEqualTo(new double[] {0, 1, 3});
  }

  public void testSubArray() {
    ImmutableDoubleArray iia0 = ImmutableDoubleArray.of();
    ImmutableDoubleArray iia1 = ImmutableDoubleArray.of(5);
    ImmutableDoubleArray iia3 = ImmutableDoubleArray.of(5, 25, 125);

    assertThat(iia0.subArray(0, 0)).isSameInstanceAs(ImmutableDoubleArray.of());
    assertThat(iia1.subArray(0, 0)).isSameInstanceAs(ImmutableDoubleArray.of());
    assertThat(iia1.subArray(1, 1)).isSameInstanceAs(ImmutableDoubleArray.of());
    assertThat(iia1.subArray(0, 1).asList()).containsExactly(5.0);
    assertThat(iia3.subArray(0, 2).asList()).containsExactly(5.0, 25.0).inOrder();
    assertThat(iia3.subArray(1, 3).asList()).containsExactly(25.0, 125.0).inOrder();

    try {
      iia3.subArray(-1, 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      iia3.subArray(1, 4);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  /*
   * Whenever an implementation uses `instanceof` on a parameter instance, the test has to know that
   * (so much for "black box") and try instances that both do and don't pass the check. The "don't"
   * half of that is more awkward to arrange...
   */
  private static <T> Iterable<T> iterable(final Collection<T> collection) {
    // return collection::iterator;
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return collection.iterator();
      }
    };
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(ImmutableDoubleArray.of())
        .addEqualityGroup(
            ImmutableDoubleArray.of(1, 2),
            reserialize(ImmutableDoubleArray.of(1, 2)),
            ImmutableDoubleArray.of(0, 1, 2, 3).subArray(1, 3))
        .addEqualityGroup(ImmutableDoubleArray.of(1, 3))
        .addEqualityGroup(ImmutableDoubleArray.of(1, 2, 3))
        .testEquals();
  }

  /**
   * This is probably a weird and hacky way to test what we're really trying to test, but hey, it
   * caught a bug.
   */
  public void testTrimmed() {
    ImmutableDoubleArray iia = ImmutableDoubleArray.of(0, 1, 3);
    assertDoesntActuallyTrim(iia);
    assertDoesntActuallyTrim(iia.subArray(0, 3));
    assertActuallyTrims(iia.subArray(0, 2));
    assertActuallyTrims(iia.subArray(1, 3));

    ImmutableDoubleArray rightSized = ImmutableDoubleArray.builder(3).add(0).add(1).add(3).build();
    assertDoesntActuallyTrim(rightSized);

    ImmutableDoubleArray overSized = ImmutableDoubleArray.builder(3).add(0).add(1).build();
    assertActuallyTrims(overSized);

    ImmutableDoubleArray underSized = ImmutableDoubleArray.builder(2).add(0).add(1).add(3).build();
    assertActuallyTrims(underSized);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testSerialization() {
    assertThat(reserialize(ImmutableDoubleArray.of())).isSameInstanceAs(ImmutableDoubleArray.of());
    assertThat(reserialize(ImmutableDoubleArray.of(0, 1).subArray(1, 1)))
        .isSameInstanceAs(ImmutableDoubleArray.of());

    ImmutableDoubleArray iia = ImmutableDoubleArray.of(0, 1, 3, 6).subArray(1, 3);
    ImmutableDoubleArray iia2 = reserialize(iia);
    assertThat(iia2).isEqualTo(iia);
    assertDoesntActuallyTrim(iia2);
  }

  private static void assertActuallyTrims(ImmutableDoubleArray iia) {
    ImmutableDoubleArray trimmed = iia.trimmed();
    assertThat(trimmed).isNotSameInstanceAs(iia);

    // Yes, this is apparently how you check array equality in Truth
    assertThat(trimmed.toArray()).isEqualTo(iia.toArray());
  }

  private static void assertDoesntActuallyTrim(ImmutableDoubleArray iia) {
    assertThat(iia.trimmed()).isSameInstanceAs(iia);
  }

  @J2ktIncompatible
  @GwtIncompatible // suite
  public static Test suite() {
    List<ListTestSuiteBuilder<Double>> builders =
        ImmutableList.of(
            ListTestSuiteBuilder.using(new ImmutableDoubleArrayAsListGenerator())
                .named("ImmutableDoubleArray.asList"),
            ListTestSuiteBuilder.using(new ImmutableDoubleArrayHeadSubListAsListGenerator())
                .named("ImmutableDoubleArray.asList, head subList"),
            ListTestSuiteBuilder.using(new ImmutableDoubleArrayTailSubListAsListGenerator())
                .named("ImmutableDoubleArray.asList, tail subList"),
            ListTestSuiteBuilder.using(new ImmutableDoubleArrayMiddleSubListAsListGenerator())
                .named("ImmutableDoubleArray.asList, middle subList"));

    TestSuite suite = new TestSuite();
    for (ListTestSuiteBuilder<Double> builder : builders) {
      suite.addTest(
          builder
              .withFeatures(
                  CollectionSize.ZERO,
                  CollectionSize.ONE,
                  CollectionSize.SEVERAL,
                  CollectionFeature.ALLOWS_NULL_QUERIES,
                  CollectionFeature.RESTRICTS_ELEMENTS,
                  CollectionFeature.KNOWN_ORDER,
                  CollectionFeature.SERIALIZABLE_INCLUDING_VIEWS)
              .createTestSuite());
    }
    suite.addTestSuite(ImmutableDoubleArrayTest.class);
    return suite;
  }

  @J2ktIncompatible
  @GwtIncompatible // used only from suite
  private static ImmutableDoubleArray makeArray(Double[] values) {
    return ImmutableDoubleArray.copyOf(Arrays.asList(values));
  }

  // Test generators.  To let the GWT test suite generator access them, they need to be public named
  // classes with a public default constructor (not that we run these suites under GWT yet).

  @J2ktIncompatible
  @GwtIncompatible // used only from suite
  public static final class ImmutableDoubleArrayAsListGenerator extends TestDoubleListGenerator {
    @Override
    protected List<Double> create(Double[] elements) {
      return makeArray(elements).asList();
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // used only from suite
  public static final class ImmutableDoubleArrayHeadSubListAsListGenerator
      extends TestDoubleListGenerator {
    @Override
    protected List<Double> create(Double[] elements) {
      Double[] suffix = {Double.MIN_VALUE, Double.MAX_VALUE};
      Double[] all = concat(elements, suffix);
      return makeArray(all).subArray(0, elements.length).asList();
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // used only from suite
  public static final class ImmutableDoubleArrayTailSubListAsListGenerator
      extends TestDoubleListGenerator {
    @Override
    protected List<Double> create(Double[] elements) {
      Double[] prefix = {86.0, 99.0};
      Double[] all = concat(prefix, elements);
      return makeArray(all).subArray(2, elements.length + 2).asList();
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // used only from suite
  public static final class ImmutableDoubleArrayMiddleSubListAsListGenerator
      extends TestDoubleListGenerator {
    @Override
    protected List<Double> create(Double[] elements) {
      Double[] prefix = {Double.MIN_VALUE, Double.MAX_VALUE};
      Double[] suffix = {86.0, 99.0};
      Double[] all = concat(concat(prefix, elements), suffix);
      return makeArray(all).subArray(2, elements.length + 2).asList();
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // used only from suite
  private static Double[] concat(Double[] a, Double[] b) {
    return ObjectArrays.concat(a, b, Double.class);
  }

  @J2ktIncompatible
  @GwtIncompatible // used only from suite
  public abstract static class TestDoubleListGenerator implements TestListGenerator<Double> {
    @Override
    public SampleElements<Double> samples() {
      return new SampleDoubles();
    }

    @Override
    public List<Double> create(Object... elements) {
      Double[] array = new Double[elements.length];
      int i = 0;
      for (Object e : elements) {
        array[i++] = (Double) e;
      }
      return create(array);
    }

    /**
     * Creates a new collection containing the given elements; implement this method instead of
     * {@link #create(Object...)}.
     */
    protected abstract List<Double> create(Double[] elements);

    @Override
    public Double[] createArray(int length) {
      return new Double[length];
    }

    /** Returns the original element list, unchanged. */
    @Override
    public List<Double> order(List<Double> insertionOrder) {
      return insertionOrder;
    }
  }

  @J2ktIncompatible
  @GwtIncompatible // used only from suite
  public static class SampleDoubles extends SampleElements<Double> {
    public SampleDoubles() {
      super(-0.0, Long.MAX_VALUE * 3.0, Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NaN);
    }
  }
}

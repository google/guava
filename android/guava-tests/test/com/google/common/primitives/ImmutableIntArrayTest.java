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
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/** @author Kevin Bourrillion */
@GwtCompatible(emulated = true)
public class ImmutableIntArrayTest extends TestCase {
  // Test all creation paths very lazily: by assuming asList() works

  public void testOf0() {
    assertThat(ImmutableIntArray.of().asList()).isEmpty();
  }

  public void testOf1() {
    assertThat(ImmutableIntArray.of(0).asList()).containsExactly(0);
  }

  public void testOf2() {
    assertThat(ImmutableIntArray.of(0, 1).asList()).containsExactly(0, 1).inOrder();
  }

  public void testOf3() {
    assertThat(ImmutableIntArray.of(0, 1, 3).asList()).containsExactly(0, 1, 3).inOrder();
  }

  public void testOf4() {
    assertThat(ImmutableIntArray.of(0, 1, 3, 6).asList()).containsExactly(0, 1, 3, 6).inOrder();
  }

  public void testOf5() {
    assertThat(ImmutableIntArray.of(0, 1, 3, 6, 10).asList())
        .containsExactly(0, 1, 3, 6, 10)
        .inOrder();
  }

  public void testOf6() {
    assertThat(ImmutableIntArray.of(0, 1, 3, 6, 10, 15).asList())
        .containsExactly(0, 1, 3, 6, 10, 15)
        .inOrder();
  }

  public void testOf7() {
    assertThat(ImmutableIntArray.of(0, 1, 3, 6, 10, 15, 21).asList())
        .containsExactly(0, 1, 3, 6, 10, 15, 21)
        .inOrder();
  }

  public void testCopyOf_array_empty() {
    /*
     * We don't guarantee the same-as property, so we aren't obligated to test it. However, it's
     * useful in testing - when two things are the same then one can't have bugs the other doesn't.
     */
    assertThat(ImmutableIntArray.copyOf(new int[0])).isSameInstanceAs(ImmutableIntArray.of());
  }

  public void testCopyOf_array_nonempty() {
    int[] array = new int[] {0, 1, 3};
    ImmutableIntArray iia = ImmutableIntArray.copyOf(array);
    array[2] = 2;
    assertThat(iia.asList()).containsExactly(0, 1, 3).inOrder();
  }

  public void testCopyOf_iterable_notCollection_empty() {
    Iterable<Integer> iterable = iterable(Collections.<Integer>emptySet());
    assertThat(ImmutableIntArray.copyOf(iterable)).isSameInstanceAs(ImmutableIntArray.of());
  }

  public void testCopyOf_iterable_notCollection_nonempty() {
    List<Integer> list = Arrays.asList(0, 1, 3);
    ImmutableIntArray iia = ImmutableIntArray.copyOf(iterable(list));
    list.set(2, 2);
    assertThat(iia.asList()).containsExactly(0, 1, 3).inOrder();
  }

  public void testCopyOf_iterable_collection_empty() {
    Iterable<Integer> iterable = Collections.emptySet();
    assertThat(ImmutableIntArray.copyOf(iterable)).isSameInstanceAs(ImmutableIntArray.of());
  }

  public void testCopyOf_iterable_collection_nonempty() {
    List<Integer> list = Arrays.asList(0, 1, 3);
    ImmutableIntArray iia = ImmutableIntArray.copyOf((Iterable<Integer>) list);
    list.set(2, 2);
    assertThat(iia.asList()).containsExactly(0, 1, 3).inOrder();
  }

  public void testCopyOf_collection_empty() {
    Collection<Integer> iterable = Collections.emptySet();
    assertThat(ImmutableIntArray.copyOf(iterable)).isSameInstanceAs(ImmutableIntArray.of());
  }

  public void testCopyOf_collection_nonempty() {
    List<Integer> list = Arrays.asList(0, 1, 3);
    ImmutableIntArray iia = ImmutableIntArray.copyOf(list);
    list.set(2, 2);
    assertThat(iia.asList()).containsExactly(0, 1, 3).inOrder();
  }

  public void testBuilder_presize_zero() {
    ImmutableIntArray.Builder builder = ImmutableIntArray.builder(0);
    builder.add(5);
    ImmutableIntArray array = builder.build();
    assertThat(array.asList()).containsExactly(5);
  }

  public void testBuilder_presize_negative() {
    try {
      ImmutableIntArray.builder(-1);
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
      ImmutableIntArray.Builder builder = ImmutableIntArray.builder(RANDOM.nextInt(20));
      AtomicInteger counter = new AtomicInteger(0);
      while (counter.get() < 1000) {
        BuilderOp op = BuilderOp.randomOp();
        op.doIt(builder, counter);
      }
      ImmutableIntArray iia = builder.build();
      for (int j = 0; j < iia.length(); j++) {
        assertThat(iia.get(j)).isEqualTo(j);
      }
    }
  }

  private enum BuilderOp {
    ADD_ONE {
      @Override
      void doIt(ImmutableIntArray.Builder builder, AtomicInteger counter) {
        builder.add(counter.getAndIncrement());
      }
    },
    ADD_ARRAY {
      @Override
      void doIt(ImmutableIntArray.Builder builder, AtomicInteger counter) {
        int[] array = new int[RANDOM.nextInt(10)];
        for (int i = 0; i < array.length; i++) {
          array[i] = counter.getAndIncrement();
        }
        builder.addAll(array);
      }
    },
    ADD_COLLECTION {
      @Override
      void doIt(ImmutableIntArray.Builder builder, AtomicInteger counter) {
        List<Integer> list = new ArrayList<>();
        int num = RANDOM.nextInt(10);
        for (int i = 0; i < num; i++) {
          list.add(counter.getAndIncrement());
        }
        builder.addAll(list);
      }
    },
    ADD_ITERABLE {
      @Override
      void doIt(ImmutableIntArray.Builder builder, AtomicInteger counter) {
        List<Integer> list = new ArrayList<>();
        int num = RANDOM.nextInt(10);
        for (int i = 0; i < num; i++) {
          list.add(counter.getAndIncrement());
        }
        builder.addAll(iterable(list));
      }
    },
    ADD_IIA {
      @Override
      void doIt(ImmutableIntArray.Builder builder, AtomicInteger counter) {
        int[] array = new int[RANDOM.nextInt(10)];
        for (int i = 0; i < array.length; i++) {
          array[i] = counter.getAndIncrement();
        }
        builder.addAll(ImmutableIntArray.copyOf(array));
      }
    },
    ADD_LARGER_ARRAY {
      @Override
      void doIt(ImmutableIntArray.Builder builder, AtomicInteger counter) {
        int[] array = new int[RANDOM.nextInt(200) + 200];
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

    abstract void doIt(ImmutableIntArray.Builder builder, AtomicInteger counter);
  }

  private static final Random RANDOM = new Random(42);

  public void testLength() {
    assertThat(ImmutableIntArray.of().length()).isEqualTo(0);
    assertThat(ImmutableIntArray.of(0).length()).isEqualTo(1);
    assertThat(ImmutableIntArray.of(0, 1, 3).length()).isEqualTo(3);
    assertThat(ImmutableIntArray.of(0, 1, 3).subArray(1, 1).length()).isEqualTo(0);
    assertThat(ImmutableIntArray.of(0, 1, 3).subArray(1, 2).length()).isEqualTo(1);
  }

  public void testIsEmpty() {
    assertThat(ImmutableIntArray.of().isEmpty()).isTrue();
    assertThat(ImmutableIntArray.of(0).isEmpty()).isFalse();
    assertThat(ImmutableIntArray.of(0, 1, 3).isEmpty()).isFalse();
    assertThat(ImmutableIntArray.of(0, 1, 3).subArray(1, 1).isEmpty()).isTrue();
    assertThat(ImmutableIntArray.of(0, 1, 3).subArray(1, 2).isEmpty()).isFalse();
  }

  public void testGet_good() {
    ImmutableIntArray iia = ImmutableIntArray.of(0, 1, 3);
    assertThat(iia.get(0)).isEqualTo(0);
    assertThat(iia.get(2)).isEqualTo(3);
    assertThat(iia.subArray(1, 3).get(1)).isEqualTo(3);
  }

  public void testGet_bad() {
    ImmutableIntArray iia = ImmutableIntArray.of(0, 1, 3);
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
    ImmutableIntArray iia = ImmutableIntArray.of(1, 1, 2, 3, 5, 8);
    assertThat(iia.indexOf(1)).isEqualTo(0);
    assertThat(iia.indexOf(8)).isEqualTo(5);
    assertThat(iia.indexOf(4)).isEqualTo(-1);
    assertThat(ImmutableIntArray.of(13).indexOf(13)).isEqualTo(0);
    assertThat(ImmutableIntArray.of().indexOf(21)).isEqualTo(-1);
    assertThat(iia.subArray(1, 5).indexOf(1)).isEqualTo(0);
  }

  public void testLastIndexOf() {
    ImmutableIntArray iia = ImmutableIntArray.of(1, 1, 2, 3, 5, 8);
    assertThat(iia.lastIndexOf(1)).isEqualTo(1);
    assertThat(iia.lastIndexOf(8)).isEqualTo(5);
    assertThat(iia.lastIndexOf(4)).isEqualTo(-1);
    assertThat(ImmutableIntArray.of(13).lastIndexOf(13)).isEqualTo(0);
    assertThat(ImmutableIntArray.of().lastIndexOf(21)).isEqualTo(-1);
    assertThat(iia.subArray(1, 5).lastIndexOf(1)).isEqualTo(0);
  }

  public void testContains() {
    ImmutableIntArray iia = ImmutableIntArray.of(1, 1, 2, 3, 5, 8);
    assertThat(iia.contains(1)).isTrue();
    assertThat(iia.contains(8)).isTrue();
    assertThat(iia.contains(4)).isFalse();
    assertThat(ImmutableIntArray.of(13).contains(13)).isTrue();
    assertThat(ImmutableIntArray.of().contains(21)).isFalse();
    assertThat(iia.subArray(1, 5).contains(1)).isTrue();
  }

  public void testSubArray() {
    ImmutableIntArray iia0 = ImmutableIntArray.of();
    ImmutableIntArray iia1 = ImmutableIntArray.of(5);
    ImmutableIntArray iia3 = ImmutableIntArray.of(5, 25, 125);

    assertThat(iia0.subArray(0, 0)).isSameInstanceAs(ImmutableIntArray.of());
    assertThat(iia1.subArray(0, 0)).isSameInstanceAs(ImmutableIntArray.of());
    assertThat(iia1.subArray(1, 1)).isSameInstanceAs(ImmutableIntArray.of());
    assertThat(iia1.subArray(0, 1).asList()).containsExactly(5);
    assertThat(iia3.subArray(0, 2).asList()).containsExactly(5, 25).inOrder();
    assertThat(iia3.subArray(1, 3).asList()).containsExactly(25, 125).inOrder();

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
        .addEqualityGroup(ImmutableIntArray.of())
        .addEqualityGroup(
            ImmutableIntArray.of(1, 2),
            reserialize(ImmutableIntArray.of(1, 2)),
            ImmutableIntArray.of(0, 1, 2, 3).subArray(1, 3))
        .addEqualityGroup(ImmutableIntArray.of(1, 3))
        .addEqualityGroup(ImmutableIntArray.of(1, 2, 3))
        .testEquals();
  }

  /**
   * This is probably a weird and hacky way to test what we're really trying to test, but hey, it
   * caught a bug.
   */
  public void testTrimmed() {
    ImmutableIntArray iia = ImmutableIntArray.of(0, 1, 3);
    assertDoesntActuallyTrim(iia);
    assertDoesntActuallyTrim(iia.subArray(0, 3));
    assertActuallyTrims(iia.subArray(0, 2));
    assertActuallyTrims(iia.subArray(1, 3));

    ImmutableIntArray rightSized = ImmutableIntArray.builder(3).add(0).add(1).add(3).build();
    assertDoesntActuallyTrim(rightSized);

    ImmutableIntArray overSized = ImmutableIntArray.builder(3).add(0).add(1).build();
    assertActuallyTrims(overSized);

    ImmutableIntArray underSized = ImmutableIntArray.builder(2).add(0).add(1).add(3).build();
    assertActuallyTrims(underSized);
  }

  @GwtIncompatible // SerializableTester
  public void testSerialization() {
    assertThat(reserialize(ImmutableIntArray.of())).isSameInstanceAs(ImmutableIntArray.of());
    assertThat(reserialize(ImmutableIntArray.of(0, 1).subArray(1, 1)))
        .isSameInstanceAs(ImmutableIntArray.of());

    ImmutableIntArray iia = ImmutableIntArray.of(0, 1, 3, 6).subArray(1, 3);
    ImmutableIntArray iia2 = reserialize(iia);
    assertThat(iia2).isEqualTo(iia);
    assertDoesntActuallyTrim(iia2);
  }

  private static void assertActuallyTrims(ImmutableIntArray iia) {
    ImmutableIntArray trimmed = iia.trimmed();
    assertThat(trimmed).isNotSameInstanceAs(iia);

    // Yes, this is apparently how you check array equality in Truth
    assertThat(trimmed.toArray()).isEqualTo(iia.toArray());
  }

  private static void assertDoesntActuallyTrim(ImmutableIntArray iia) {
    assertThat(iia.trimmed()).isSameInstanceAs(iia);
  }

  @GwtIncompatible // suite
  public static Test suite() {
    List<ListTestSuiteBuilder<Integer>> builders =
        ImmutableList.of(
            ListTestSuiteBuilder.using(new ImmutableIntArrayAsListGenerator())
                .named("ImmutableIntArray.asList"),
            ListTestSuiteBuilder.using(new ImmutableIntArrayHeadSubListAsListGenerator())
                .named("ImmutableIntArray.asList, head subList"),
            ListTestSuiteBuilder.using(new ImmutableIntArrayTailSubListAsListGenerator())
                .named("ImmutableIntArray.asList, tail subList"),
            ListTestSuiteBuilder.using(new ImmutableIntArrayMiddleSubListAsListGenerator())
                .named("ImmutableIntArray.asList, middle subList"));

    TestSuite suite = new TestSuite();
    for (ListTestSuiteBuilder<Integer> builder : builders) {
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
    suite.addTestSuite(ImmutableIntArrayTest.class);
    return suite;
  }

  @GwtIncompatible // used only from suite
  private static ImmutableIntArray makeArray(Integer[] values) {
    return ImmutableIntArray.copyOf(Arrays.asList(values));
  }

  // Test generators.  To let the GWT test suite generator access them, they need to be public named
  // classes with a public default constructor (not that we run these suites under GWT yet).

  @GwtIncompatible // used only from suite
  public static final class ImmutableIntArrayAsListGenerator extends TestIntegerListGenerator {
    @Override
    protected List<Integer> create(Integer[] elements) {
      return makeArray(elements).asList();
    }
  }

  @GwtIncompatible // used only from suite
  public static final class ImmutableIntArrayHeadSubListAsListGenerator
      extends TestIntegerListGenerator {
    @Override
    protected List<Integer> create(Integer[] elements) {
      Integer[] suffix = {Integer.MIN_VALUE, Integer.MAX_VALUE};
      Integer[] all = concat(elements, suffix);
      return makeArray(all).subArray(0, elements.length).asList();
    }
  }

  @GwtIncompatible // used only from suite
  public static final class ImmutableIntArrayTailSubListAsListGenerator
      extends TestIntegerListGenerator {
    @Override
    protected List<Integer> create(Integer[] elements) {
      Integer[] prefix = {86, 99};
      Integer[] all = concat(prefix, elements);
      return makeArray(all).subArray(2, elements.length + 2).asList();
    }
  }

  @GwtIncompatible // used only from suite
  public static final class ImmutableIntArrayMiddleSubListAsListGenerator
      extends TestIntegerListGenerator {
    @Override
    protected List<Integer> create(Integer[] elements) {
      Integer[] prefix = {Integer.MIN_VALUE, Integer.MAX_VALUE};
      Integer[] suffix = {86, 99};
      Integer[] all = concat(concat(prefix, elements), suffix);
      return makeArray(all).subArray(2, elements.length + 2).asList();
    }
  }

  @GwtIncompatible // used only from suite
  private static Integer[] concat(Integer[] a, Integer[] b) {
    return ObjectArrays.concat(a, b, Integer.class);
  }

  @GwtIncompatible // used only from suite
  public abstract static class TestIntegerListGenerator implements TestListGenerator<Integer> {
    @Override
    public SampleElements<Integer> samples() {
      return new SampleIntegers();
    }

    @Override
    public List<Integer> create(Object... elements) {
      Integer[] array = new Integer[elements.length];
      int i = 0;
      for (Object e : elements) {
        array[i++] = (Integer) e;
      }
      return create(array);
    }

    /**
     * Creates a new collection containing the given elements; implement this method instead of
     * {@link #create(Object...)}.
     */
    protected abstract List<Integer> create(Integer[] elements);

    @Override
    public Integer[] createArray(int length) {
      return new Integer[length];
    }

    /** Returns the original element list, unchanged. */
    @Override
    public List<Integer> order(List<Integer> insertionOrder) {
      return insertionOrder;
    }
  }

  @GwtIncompatible // used only from suite
  public static class SampleIntegers extends SampleElements<Integer> {
    public SampleIntegers() {
      super(1, 3, 6, 10, 15);
    }
  }
}

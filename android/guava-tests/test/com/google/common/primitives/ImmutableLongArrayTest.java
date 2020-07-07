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
import java.util.concurrent.atomic.AtomicLong;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/** @author Kevin Bourrillion */
@GwtCompatible(emulated = true)
public class ImmutableLongArrayTest extends TestCase {
  // Test all creation paths very lazily: by assuming asList() works

  public void testOf0() {
    assertThat(ImmutableLongArray.of().asList()).isEmpty();
  }

  public void testOf1() {
    assertThat(ImmutableLongArray.of(0).asList()).containsExactly(0L);
  }

  public void testOf2() {
    assertThat(ImmutableLongArray.of(0, 1).asList()).containsExactly(0L, 1L).inOrder();
  }

  public void testOf3() {
    assertThat(ImmutableLongArray.of(0, 1, 3).asList()).containsExactly(0L, 1L, 3L).inOrder();
  }

  public void testOf4() {
    assertThat(ImmutableLongArray.of(0, 1, 3, 6).asList())
        .containsExactly(0L, 1L, 3L, 6L)
        .inOrder();
  }

  public void testOf5() {
    assertThat(ImmutableLongArray.of(0, 1, 3, 6, 10).asList())
        .containsExactly(0L, 1L, 3L, 6L, 10L)
        .inOrder();
  }

  public void testOf6() {
    assertThat(ImmutableLongArray.of(0, 1, 3, 6, 10, 15).asList())
        .containsExactly(0L, 1L, 3L, 6L, 10L, 15L)
        .inOrder();
  }

  public void testOf7() {
    assertThat(ImmutableLongArray.of(0, 1, 3, 6, 10, 15, 21).asList())
        .containsExactly(0L, 1L, 3L, 6L, 10L, 15L, 21L)
        .inOrder();
  }

  public void testCopyOf_array_empty() {
    /*
     * We don't guarantee the same-as property, so we aren't obligated to test it. However, it's
     * useful in testing - when two things are the same then one can't have bugs the other doesn't.
     */
    assertThat(ImmutableLongArray.copyOf(new long[0])).isSameInstanceAs(ImmutableLongArray.of());
  }

  public void testCopyOf_array_nonempty() {
    long[] array = new long[] {0, 1, 3};
    ImmutableLongArray iia = ImmutableLongArray.copyOf(array);
    array[2] = 2;
    assertThat(iia.asList()).containsExactly(0L, 1L, 3L).inOrder();
  }

  public void testCopyOf_iterable_notCollection_empty() {
    Iterable<Long> iterable = iterable(Collections.<Long>emptySet());
    assertThat(ImmutableLongArray.copyOf(iterable)).isSameInstanceAs(ImmutableLongArray.of());
  }

  public void testCopyOf_iterable_notCollection_nonempty() {
    List<Long> list = Arrays.asList(0L, 1L, 3L);
    ImmutableLongArray iia = ImmutableLongArray.copyOf(iterable(list));
    list.set(2, 2L);
    assertThat(iia.asList()).containsExactly(0L, 1L, 3L).inOrder();
  }

  public void testCopyOf_iterable_collection_empty() {
    Iterable<Long> iterable = Collections.emptySet();
    assertThat(ImmutableLongArray.copyOf(iterable)).isSameInstanceAs(ImmutableLongArray.of());
  }

  public void testCopyOf_iterable_collection_nonempty() {
    List<Long> list = Arrays.asList(0L, 1L, 3L);
    ImmutableLongArray iia = ImmutableLongArray.copyOf((Iterable<Long>) list);
    list.set(2, 2L);
    assertThat(iia.asList()).containsExactly(0L, 1L, 3L).inOrder();
  }

  public void testCopyOf_collection_empty() {
    Collection<Long> iterable = Collections.emptySet();
    assertThat(ImmutableLongArray.copyOf(iterable)).isSameInstanceAs(ImmutableLongArray.of());
  }

  public void testCopyOf_collection_nonempty() {
    List<Long> list = Arrays.asList(0L, 1L, 3L);
    ImmutableLongArray iia = ImmutableLongArray.copyOf(list);
    list.set(2, 2L);
    assertThat(iia.asList()).containsExactly(0L, 1L, 3L).inOrder();
  }

  public void testBuilder_presize_zero() {
    ImmutableLongArray.Builder builder = ImmutableLongArray.builder(0);
    builder.add(5L);
    ImmutableLongArray array = builder.build();
    assertThat(array.asList()).containsExactly(5L);
  }

  public void testBuilder_presize_negative() {
    try {
      ImmutableLongArray.builder(-1);
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
      ImmutableLongArray.Builder builder = ImmutableLongArray.builder(RANDOM.nextInt(20));
      AtomicLong counter = new AtomicLong(0);
      while (counter.get() < 1000) {
        BuilderOp op = BuilderOp.randomOp();
        op.doIt(builder, counter);
      }
      ImmutableLongArray iia = builder.build();
      for (int j = 0; j < iia.length(); j++) {
        assertThat(iia.get(j)).isEqualTo((long) j);
      }
    }
  }

  private enum BuilderOp {
    ADD_ONE {
      @Override
      void doIt(ImmutableLongArray.Builder builder, AtomicLong counter) {
        builder.add(counter.getAndIncrement());
      }
    },
    ADD_ARRAY {
      @Override
      void doIt(ImmutableLongArray.Builder builder, AtomicLong counter) {
        long[] array = new long[RANDOM.nextInt(10)];
        for (int i = 0; i < array.length; i++) {
          array[i] = counter.getAndIncrement();
        }
        builder.addAll(array);
      }
    },
    ADD_COLLECTION {
      @Override
      void doIt(ImmutableLongArray.Builder builder, AtomicLong counter) {
        List<Long> list = new ArrayList<>();
        long num = RANDOM.nextInt(10);
        for (int i = 0; i < num; i++) {
          list.add(counter.getAndIncrement());
        }
        builder.addAll(list);
      }
    },
    ADD_ITERABLE {
      @Override
      void doIt(ImmutableLongArray.Builder builder, AtomicLong counter) {
        List<Long> list = new ArrayList<>();
        long num = RANDOM.nextInt(10);
        for (int i = 0; i < num; i++) {
          list.add(counter.getAndIncrement());
        }
        builder.addAll(iterable(list));
      }
    },
    ADD_IIA {
      @Override
      void doIt(ImmutableLongArray.Builder builder, AtomicLong counter) {
        long[] array = new long[RANDOM.nextInt(10)];
        for (int i = 0; i < array.length; i++) {
          array[i] = counter.getAndIncrement();
        }
        builder.addAll(ImmutableLongArray.copyOf(array));
      }
    },
    ADD_LARGER_ARRAY {
      @Override
      void doIt(ImmutableLongArray.Builder builder, AtomicLong counter) {
        long[] array = new long[RANDOM.nextInt(200) + 200];
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

    abstract void doIt(ImmutableLongArray.Builder builder, AtomicLong counter);
  }

  private static final Random RANDOM = new Random(42);

  public void testLength() {
    assertThat(ImmutableLongArray.of().length()).isEqualTo(0);
    assertThat(ImmutableLongArray.of(0).length()).isEqualTo(1);
    assertThat(ImmutableLongArray.of(0, 1, 3).length()).isEqualTo(3);
    assertThat(ImmutableLongArray.of(0, 1, 3).subArray(1, 1).length()).isEqualTo(0);
    assertThat(ImmutableLongArray.of(0, 1, 3).subArray(1, 2).length()).isEqualTo(1);
  }

  public void testIsEmpty() {
    assertThat(ImmutableLongArray.of().isEmpty()).isTrue();
    assertThat(ImmutableLongArray.of(0).isEmpty()).isFalse();
    assertThat(ImmutableLongArray.of(0, 1, 3).isEmpty()).isFalse();
    assertThat(ImmutableLongArray.of(0, 1, 3).subArray(1, 1).isEmpty()).isTrue();
    assertThat(ImmutableLongArray.of(0, 1, 3).subArray(1, 2).isEmpty()).isFalse();
  }

  public void testGet_good() {
    ImmutableLongArray iia = ImmutableLongArray.of(0, 1, 3);
    assertThat(iia.get(0)).isEqualTo(0L);
    assertThat(iia.get(2)).isEqualTo(3L);
    assertThat(iia.subArray(1, 3).get(1)).isEqualTo(3L);
  }

  public void testGet_bad() {
    ImmutableLongArray iia = ImmutableLongArray.of(0, 1, 3);
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
    ImmutableLongArray iia = ImmutableLongArray.of(1, 1, 2, 3, 5, 8);
    assertThat(iia.indexOf(1)).isEqualTo(0);
    assertThat(iia.indexOf(8)).isEqualTo(5);
    assertThat(iia.indexOf(4)).isEqualTo(-1);
    assertThat(ImmutableLongArray.of(13).indexOf(13)).isEqualTo(0);
    assertThat(ImmutableLongArray.of().indexOf(21)).isEqualTo(-1);
    assertThat(iia.subArray(1, 5).indexOf(1)).isEqualTo(0);
  }

  public void testLastIndexOf() {
    ImmutableLongArray iia = ImmutableLongArray.of(1, 1, 2, 3, 5, 8);
    assertThat(iia.lastIndexOf(1)).isEqualTo(1);
    assertThat(iia.lastIndexOf(8)).isEqualTo(5);
    assertThat(iia.lastIndexOf(4)).isEqualTo(-1);
    assertThat(ImmutableLongArray.of(13).lastIndexOf(13)).isEqualTo(0);
    assertThat(ImmutableLongArray.of().lastIndexOf(21)).isEqualTo(-1);
    assertThat(iia.subArray(1, 5).lastIndexOf(1)).isEqualTo(0);
  }

  public void testContains() {
    ImmutableLongArray iia = ImmutableLongArray.of(1, 1, 2, 3, 5, 8);
    assertThat(iia.contains(1)).isTrue();
    assertThat(iia.contains(8)).isTrue();
    assertThat(iia.contains(4)).isFalse();
    assertThat(ImmutableLongArray.of(13).contains(13)).isTrue();
    assertThat(ImmutableLongArray.of().contains(21)).isFalse();
    assertThat(iia.subArray(1, 5).contains(1)).isTrue();
  }

  public void testSubArray() {
    ImmutableLongArray iia0 = ImmutableLongArray.of();
    ImmutableLongArray iia1 = ImmutableLongArray.of(5);
    ImmutableLongArray iia3 = ImmutableLongArray.of(5, 25, 125);

    assertThat(iia0.subArray(0, 0)).isSameInstanceAs(ImmutableLongArray.of());
    assertThat(iia1.subArray(0, 0)).isSameInstanceAs(ImmutableLongArray.of());
    assertThat(iia1.subArray(1, 1)).isSameInstanceAs(ImmutableLongArray.of());
    assertThat(iia1.subArray(0, 1).asList()).containsExactly(5L);
    assertThat(iia3.subArray(0, 2).asList()).containsExactly(5L, 25L).inOrder();
    assertThat(iia3.subArray(1, 3).asList()).containsExactly(25L, 125L).inOrder();

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
        .addEqualityGroup(ImmutableLongArray.of())
        .addEqualityGroup(
            ImmutableLongArray.of(1, 2),
            reserialize(ImmutableLongArray.of(1, 2)),
            ImmutableLongArray.of(0, 1, 2, 3).subArray(1, 3))
        .addEqualityGroup(ImmutableLongArray.of(1, 3))
        .addEqualityGroup(ImmutableLongArray.of(1, 2, 3))
        .testEquals();
  }

  /**
   * This is probably a weird and hacky way to test what we're really trying to test, but hey, it
   * caught a bug.
   */
  public void testTrimmed() {
    ImmutableLongArray iia = ImmutableLongArray.of(0, 1, 3);
    assertDoesntActuallyTrim(iia);
    assertDoesntActuallyTrim(iia.subArray(0, 3));
    assertActuallyTrims(iia.subArray(0, 2));
    assertActuallyTrims(iia.subArray(1, 3));

    ImmutableLongArray rightSized = ImmutableLongArray.builder(3).add(0).add(1).add(3).build();
    assertDoesntActuallyTrim(rightSized);

    ImmutableLongArray overSized = ImmutableLongArray.builder(3).add(0).add(1).build();
    assertActuallyTrims(overSized);

    ImmutableLongArray underSized = ImmutableLongArray.builder(2).add(0).add(1).add(3).build();
    assertActuallyTrims(underSized);
  }

  @GwtIncompatible // SerializableTester
  public void testSerialization() {
    assertThat(reserialize(ImmutableLongArray.of())).isSameInstanceAs(ImmutableLongArray.of());
    assertThat(reserialize(ImmutableLongArray.of(0, 1).subArray(1, 1)))
        .isSameInstanceAs(ImmutableLongArray.of());

    ImmutableLongArray iia = ImmutableLongArray.of(0, 1, 3, 6).subArray(1, 3);
    ImmutableLongArray iia2 = reserialize(iia);
    assertThat(iia2).isEqualTo(iia);
    assertDoesntActuallyTrim(iia2);
  }

  private static void assertActuallyTrims(ImmutableLongArray iia) {
    ImmutableLongArray trimmed = iia.trimmed();
    assertThat(trimmed).isNotSameInstanceAs(iia);

    // Yes, this is apparently how you check array equality in Truth
    assertThat(trimmed.toArray()).isEqualTo(iia.toArray());
  }

  private static void assertDoesntActuallyTrim(ImmutableLongArray iia) {
    assertThat(iia.trimmed()).isSameInstanceAs(iia);
  }

  @GwtIncompatible // suite
  public static Test suite() {
    List<ListTestSuiteBuilder<Long>> builders =
        ImmutableList.of(
            ListTestSuiteBuilder.using(new ImmutableLongArrayAsListGenerator())
                .named("ImmutableLongArray.asList"),
            ListTestSuiteBuilder.using(new ImmutableLongArrayHeadSubListAsListGenerator())
                .named("ImmutableLongArray.asList, head subList"),
            ListTestSuiteBuilder.using(new ImmutableLongArrayTailSubListAsListGenerator())
                .named("ImmutableLongArray.asList, tail subList"),
            ListTestSuiteBuilder.using(new ImmutableLongArrayMiddleSubListAsListGenerator())
                .named("ImmutableLongArray.asList, middle subList"));

    TestSuite suite = new TestSuite();
    for (ListTestSuiteBuilder<Long> builder : builders) {
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
    suite.addTestSuite(ImmutableLongArrayTest.class);
    return suite;
  }

  @GwtIncompatible // used only from suite
  private static ImmutableLongArray makeArray(Long[] values) {
    return ImmutableLongArray.copyOf(Arrays.asList(values));
  }

  // Test generators.  To let the GWT test suite generator access them, they need to be public named
  // classes with a public default constructor (not that we run these suites under GWT yet).

  @GwtIncompatible // used only from suite
  public static final class ImmutableLongArrayAsListGenerator extends TestLongListGenerator {
    @Override
    protected List<Long> create(Long[] elements) {
      return makeArray(elements).asList();
    }
  }

  @GwtIncompatible // used only from suite
  public static final class ImmutableLongArrayHeadSubListAsListGenerator
      extends TestLongListGenerator {
    @Override
    protected List<Long> create(Long[] elements) {
      Long[] suffix = {Long.MIN_VALUE, Long.MAX_VALUE};
      Long[] all = concat(elements, suffix);
      return makeArray(all).subArray(0, elements.length).asList();
    }
  }

  @GwtIncompatible // used only from suite
  public static final class ImmutableLongArrayTailSubListAsListGenerator
      extends TestLongListGenerator {
    @Override
    protected List<Long> create(Long[] elements) {
      Long[] prefix = {86L, 99L};
      Long[] all = concat(prefix, elements);
      return makeArray(all).subArray(2, elements.length + 2).asList();
    }
  }

  @GwtIncompatible // used only from suite
  public static final class ImmutableLongArrayMiddleSubListAsListGenerator
      extends TestLongListGenerator {
    @Override
    protected List<Long> create(Long[] elements) {
      Long[] prefix = {Long.MIN_VALUE, Long.MAX_VALUE};
      Long[] suffix = {86L, 99L};
      Long[] all = concat(concat(prefix, elements), suffix);
      return makeArray(all).subArray(2, elements.length + 2).asList();
    }
  }

  @GwtIncompatible // used only from suite
  private static Long[] concat(Long[] a, Long[] b) {
    return ObjectArrays.concat(a, b, Long.class);
  }

  @GwtIncompatible // used only from suite
  public abstract static class TestLongListGenerator implements TestListGenerator<Long> {
    @Override
    public SampleElements<Long> samples() {
      return new SampleLongs();
    }

    @Override
    public List<Long> create(Object... elements) {
      Long[] array = new Long[elements.length];
      int i = 0;
      for (Object e : elements) {
        array[i++] = (Long) e;
      }
      return create(array);
    }

    /**
     * Creates a new collection containing the given elements; implement this method instead of
     * {@link #create(Object...)}.
     */
    protected abstract List<Long> create(Long[] elements);

    @Override
    public Long[] createArray(int length) {
      return new Long[length];
    }

    /** Returns the original element list, unchanged. */
    @Override
    public List<Long> order(List<Long> insertionOrder) {
      return insertionOrder;
    }
  }

  @GwtIncompatible // used only from suite
  public static class SampleLongs extends SampleElements<Long> {
    public SampleLongs() {
      super(1L << 31, 1L << 33, 1L << 36, 1L << 40, 1L << 45);
    }
  }
}

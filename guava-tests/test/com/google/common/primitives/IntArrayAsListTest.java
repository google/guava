/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.primitives;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite covering {@link Ints#asList(int[])}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("cast") // redundant casts are intentional and harmless
public class IntArrayAsListTest extends TestCase {

  private static List<Integer> asList(Integer[] values) {
    int[] temp = new int[values.length];
    for (int i = 0; i < values.length; i++) {
      temp[i] = checkNotNull(values[i]); // checkNotNull for GWT (do not optimize).
    }
    return Ints.asList(temp);
  }

  @J2ktIncompatible
  @GwtIncompatible // suite
  public static Test suite() {
    List<ListTestSuiteBuilder<Integer>> builders =
        ImmutableList.of(
            ListTestSuiteBuilder.using(new IntsAsListGenerator()).named("Ints.asList"),
            ListTestSuiteBuilder.using(new IntsAsListHeadSubListGenerator())
                .named("Ints.asList, head subList"),
            ListTestSuiteBuilder.using(new IntsAsListTailSubListGenerator())
                .named("Ints.asList, tail subList"),
            ListTestSuiteBuilder.using(new IntsAsListMiddleSubListGenerator())
                .named("Ints.asList, middle subList"));

    TestSuite suite = new TestSuite();
    for (ListTestSuiteBuilder<Integer> builder : builders) {
      suite.addTest(
          builder
              .withFeatures(
                  CollectionSize.ONE,
                  CollectionSize.SEVERAL,
                  CollectionFeature.RESTRICTS_ELEMENTS,
                  ListFeature.SUPPORTS_SET)
              .createTestSuite());
    }
    return suite;
  }

  // Test generators.  To let the GWT test suite generator access them, they need to be
  // public named classes with a public default constructor.

  public static final class IntsAsListGenerator extends TestIntegerListGenerator {
    @Override
    protected List<Integer> create(Integer[] elements) {
      return asList(elements);
    }
  }

  public static final class IntsAsListHeadSubListGenerator extends TestIntegerListGenerator {
    @Override
    protected List<Integer> create(Integer[] elements) {
      Integer[] suffix = {Integer.MIN_VALUE, Integer.MAX_VALUE};
      Integer[] all = concat(elements, suffix);
      return asList(all).subList(0, elements.length);
    }
  }

  public static final class IntsAsListTailSubListGenerator extends TestIntegerListGenerator {
    @Override
    protected List<Integer> create(Integer[] elements) {
      Integer[] prefix = {(int) 86, (int) 99};
      Integer[] all = concat(prefix, elements);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  public static final class IntsAsListMiddleSubListGenerator extends TestIntegerListGenerator {
    @Override
    protected List<Integer> create(Integer[] elements) {
      Integer[] prefix = {Integer.MIN_VALUE, Integer.MAX_VALUE};
      Integer[] suffix = {(int) 86, (int) 99};
      Integer[] all = concat(concat(prefix, elements), suffix);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  private static Integer[] concat(Integer[] left, Integer[] right) {
    Integer[] result = new Integer[left.length + right.length];
    System.arraycopy(left, 0, result, 0, left.length);
    System.arraycopy(right, 0, result, left.length, right.length);
    return result;
  }

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

  public static class SampleIntegers extends SampleElements<Integer> {
    public SampleIntegers() {
      super((int) 0, (int) 1, (int) 2, (int) 3, (int) 4);
    }
  }
}

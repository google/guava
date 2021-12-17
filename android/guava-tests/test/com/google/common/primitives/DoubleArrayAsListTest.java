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
 * Test suite covering {@link Doubles#asList(double[])}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class DoubleArrayAsListTest extends TestCase {

  private static List<Double> asList(Double[] values) {
    double[] temp = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      temp[i] = checkNotNull(values[i]); // checkNotNull for GWT (do not optimize).
    }
    return Doubles.asList(temp);
  }

  @GwtIncompatible // suite
  public static Test suite() {
    List<ListTestSuiteBuilder<Double>> builders =
        ImmutableList.of(
            ListTestSuiteBuilder.using(new DoublesAsListGenerator()).named("Doubles.asList"),
            ListTestSuiteBuilder.using(new DoublsAsListHeadSubListGenerator())
                .named("Doubles.asList, head subList"),
            ListTestSuiteBuilder.using(new DoublesAsListTailSubListGenerator())
                .named("Doubles.asList, tail subList"),
            ListTestSuiteBuilder.using(new DoublesAsListMiddleSubListGenerator())
                .named("Doubles.asList, middle subList"));

    TestSuite suite = new TestSuite();
    for (ListTestSuiteBuilder<Double> builder : builders) {
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

  public static final class DoublesAsListGenerator extends TestDoubleListGenerator {
    @Override
    protected List<Double> create(Double[] elements) {
      return asList(elements);
    }
  }

  public static final class DoublsAsListHeadSubListGenerator extends TestDoubleListGenerator {
    @Override
    protected List<Double> create(Double[] elements) {
      Double[] suffix = {Double.MIN_VALUE, Double.MAX_VALUE};
      Double[] all = concat(elements, suffix);
      return asList(all).subList(0, elements.length);
    }
  }

  public static final class DoublesAsListTailSubListGenerator extends TestDoubleListGenerator {
    @Override
    protected List<Double> create(Double[] elements) {
      Double[] prefix = {(double) 86, (double) 99};
      Double[] all = concat(prefix, elements);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  public static final class DoublesAsListMiddleSubListGenerator extends TestDoubleListGenerator {
    @Override
    protected List<Double> create(Double[] elements) {
      Double[] prefix = {Double.MIN_VALUE, Double.MAX_VALUE};
      Double[] suffix = {(double) 86, (double) 99};
      Double[] all = concat(concat(prefix, elements), suffix);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  private static Double[] concat(Double[] left, Double[] right) {
    Double[] result = new Double[left.length + right.length];
    System.arraycopy(left, 0, result, 0, left.length);
    System.arraycopy(right, 0, result, left.length, right.length);
    return result;
  }

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

  public static class SampleDoubles extends SampleElements<Double> {
    public SampleDoubles() {
      super((double) 0, (double) 1, (double) 2, (double) 3, (double) 4);
    }
  }
}

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
 * Test suite covering {@link Floats#asList(float[])})}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class FloatArrayAsListTest extends TestCase {

  private static List<Float> asList(Float[] values) {
    float[] temp = new float[values.length];
    for (int i = 0; i < values.length; i++) {
      temp[i] = checkNotNull(values[i]); // checkNotNull for GWT (do not optimize).
    }
    return Floats.asList(temp);
  }

  @J2ktIncompatible
  @GwtIncompatible // suite
  public static Test suite() {
    List<ListTestSuiteBuilder<Float>> builders =
        ImmutableList.of(
            ListTestSuiteBuilder.using(new FloatsAsListGenerator()).named("Floats.asList"),
            ListTestSuiteBuilder.using(new FloatsAsListHeadSubListGenerator())
                .named("Floats.asList, head subList"),
            ListTestSuiteBuilder.using(new FloatsAsListTailSubListGenerator())
                .named("Floats.asList, tail subList"),
            ListTestSuiteBuilder.using(new FloatsAsListMiddleSubListGenerator())
                .named("Floats.asList, middle subList"));

    TestSuite suite = new TestSuite();
    for (ListTestSuiteBuilder<Float> builder : builders) {
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

  public static final class FloatsAsListGenerator extends TestFloatListGenerator {
    @Override
    protected List<Float> create(Float[] elements) {
      return asList(elements);
    }
  }

  public static final class FloatsAsListHeadSubListGenerator extends TestFloatListGenerator {
    @Override
    protected List<Float> create(Float[] elements) {
      Float[] suffix = {Float.MIN_VALUE, Float.MAX_VALUE};
      Float[] all = concat(elements, suffix);
      return asList(all).subList(0, elements.length);
    }
  }

  public static final class FloatsAsListTailSubListGenerator extends TestFloatListGenerator {
    @Override
    protected List<Float> create(Float[] elements) {
      Float[] prefix = {(float) 86, (float) 99};
      Float[] all = concat(prefix, elements);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  public static final class FloatsAsListMiddleSubListGenerator extends TestFloatListGenerator {
    @Override
    protected List<Float> create(Float[] elements) {
      Float[] prefix = {Float.MIN_VALUE, Float.MAX_VALUE};
      Float[] suffix = {(float) 86, (float) 99};
      Float[] all = concat(concat(prefix, elements), suffix);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  private static Float[] concat(Float[] left, Float[] right) {
    Float[] result = new Float[left.length + right.length];
    System.arraycopy(left, 0, result, 0, left.length);
    System.arraycopy(right, 0, result, left.length, right.length);
    return result;
  }

  public abstract static class TestFloatListGenerator implements TestListGenerator<Float> {
    @Override
    public SampleElements<Float> samples() {
      return new SampleFloats();
    }

    @Override
    public List<Float> create(Object... elements) {
      Float[] array = new Float[elements.length];
      int i = 0;
      for (Object e : elements) {
        array[i++] = (Float) e;
      }
      return create(array);
    }

    /**
     * Creates a new collection containing the given elements; implement this method instead of
     * {@link #create(Object...)}.
     */
    protected abstract List<Float> create(Float[] elements);

    @Override
    public Float[] createArray(int length) {
      return new Float[length];
    }

    /** Returns the original element list, unchanged. */
    @Override
    public List<Float> order(List<Float> insertionOrder) {
      return insertionOrder;
    }
  }

  public static class SampleFloats extends SampleElements<Float> {
    public SampleFloats() {
      super((float) 0, (float) 1, (float) 2, (float) 3, (float) 4);
    }
  }
}

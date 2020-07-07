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
 * Test suite covering {@link Shorts#asList(short[])}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class ShortArrayAsListTest extends TestCase {

  private static List<Short> asList(Short[] values) {
    short[] temp = new short[values.length];
    for (short i = 0; i < values.length; i++) {
      temp[i] = checkNotNull(values[i]); // checkNotNull for GWT (do not optimize).
    }
    return Shorts.asList(temp);
  }

  @GwtIncompatible // suite
  public static Test suite() {
    List<ListTestSuiteBuilder<Short>> builders =
        ImmutableList.of(
            ListTestSuiteBuilder.using(new ShortsAsListGenerator()).named("Shorts.asList"),
            ListTestSuiteBuilder.using(new ShortsAsListHeadSubListGenerator())
                .named("Shorts.asList, head subList"),
            ListTestSuiteBuilder.using(new ShortsAsListTailSubListGenerator())
                .named("Shorts.asList, tail subList"),
            ListTestSuiteBuilder.using(new ShortsAsListMiddleSubListGenerator())
                .named("Shorts.asList, middle subList"));

    TestSuite suite = new TestSuite();
    for (ListTestSuiteBuilder<Short> builder : builders) {
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

  public static final class ShortsAsListGenerator extends TestShortListGenerator {
    @Override
    protected List<Short> create(Short[] elements) {
      return asList(elements);
    }
  }

  public static final class ShortsAsListHeadSubListGenerator extends TestShortListGenerator {
    @Override
    protected List<Short> create(Short[] elements) {
      Short[] suffix = {Short.MIN_VALUE, Short.MAX_VALUE};
      Short[] all = concat(elements, suffix);
      return asList(all).subList(0, elements.length);
    }
  }

  public static final class ShortsAsListTailSubListGenerator extends TestShortListGenerator {
    @Override
    protected List<Short> create(Short[] elements) {
      Short[] prefix = {(short) 86, (short) 99};
      Short[] all = concat(prefix, elements);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  public static final class ShortsAsListMiddleSubListGenerator extends TestShortListGenerator {
    @Override
    protected List<Short> create(Short[] elements) {
      Short[] prefix = {Short.MIN_VALUE, Short.MAX_VALUE};
      Short[] suffix = {(short) 86, (short) 99};
      Short[] all = concat(concat(prefix, elements), suffix);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  private static Short[] concat(Short[] left, Short[] right) {
    Short[] result = new Short[left.length + right.length];
    System.arraycopy(left, 0, result, 0, left.length);
    System.arraycopy(right, 0, result, left.length, right.length);
    return result;
  }

  public abstract static class TestShortListGenerator implements TestListGenerator<Short> {
    @Override
    public SampleElements<Short> samples() {
      return new SampleShorts();
    }

    @Override
    public List<Short> create(Object... elements) {
      Short[] array = new Short[elements.length];
      short i = 0;
      for (Object e : elements) {
        array[i++] = (Short) e;
      }
      return create(array);
    }

    /**
     * Creates a new collection containing the given elements; implement this method instead of
     * {@link #create(Object...)}.
     */
    protected abstract List<Short> create(Short[] elements);

    @Override
    public Short[] createArray(int length) {
      return new Short[length];
    }

    /** Returns the original element list, unchanged. */
    @Override
    public List<Short> order(List<Short> insertionOrder) {
      return insertionOrder;
    }
  }

  public static class SampleShorts extends SampleElements<Short> {
    public SampleShorts() {
      super((short) 0, (short) 1, (short) 2, (short) 3, (short) 4);
    }
  }
}

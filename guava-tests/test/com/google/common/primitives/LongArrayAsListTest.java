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
 * Test suite covering {@link Longs#asList(long[])}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class LongArrayAsListTest extends TestCase {

  private static List<Long> asList(Long[] values) {
    long[] temp = new long[values.length];
    for (int i = 0; i < values.length; i++) {
      temp[i] = checkNotNull(values[i]); // checkNotNull for GWT (do not optimize).
    }
    return Longs.asList(temp);
  }

  @GwtIncompatible // suite
  public static Test suite() {
    List<ListTestSuiteBuilder<Long>> builders =
        ImmutableList.of(
            ListTestSuiteBuilder.using(new LongsAsListGenerator()).named("Longs.asList"),
            ListTestSuiteBuilder.using(new LongsAsListHeadSubListGenerator())
                .named("Longs.asList, head subList"),
            ListTestSuiteBuilder.using(new LongsAsListTailSubListGenerator())
                .named("Longs.asList, tail subList"),
            ListTestSuiteBuilder.using(new LongsAsListMiddleSubListGenerator())
                .named("Longs.asList, middle subList"));

    TestSuite suite = new TestSuite();
    for (ListTestSuiteBuilder<Long> builder : builders) {
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

  public static final class LongsAsListGenerator extends TestLongListGenerator {
    @Override
    protected List<Long> create(Long[] elements) {
      return asList(elements);
    }
  }

  public static final class LongsAsListHeadSubListGenerator extends TestLongListGenerator {
    @Override
    protected List<Long> create(Long[] elements) {
      Long[] suffix = {Long.MIN_VALUE, Long.MAX_VALUE};
      Long[] all = concat(elements, suffix);
      return asList(all).subList(0, elements.length);
    }
  }

  public static final class LongsAsListTailSubListGenerator extends TestLongListGenerator {
    @Override
    protected List<Long> create(Long[] elements) {
      Long[] prefix = {(long) 86, (long) 99};
      Long[] all = concat(prefix, elements);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  public static final class LongsAsListMiddleSubListGenerator extends TestLongListGenerator {
    @Override
    protected List<Long> create(Long[] elements) {
      Long[] prefix = {Long.MIN_VALUE, Long.MAX_VALUE};
      Long[] suffix = {(long) 86, (long) 99};
      Long[] all = concat(concat(prefix, elements), suffix);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  private static Long[] concat(Long[] left, Long[] right) {
    Long[] result = new Long[left.length + right.length];
    System.arraycopy(left, 0, result, 0, left.length);
    System.arraycopy(right, 0, result, left.length, right.length);
    return result;
  }

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

  public static class SampleLongs extends SampleElements<Long> {
    public SampleLongs() {
      super((long) 0, (long) 1, (long) 2, (long) 3, (long) 4);
    }
  }
}

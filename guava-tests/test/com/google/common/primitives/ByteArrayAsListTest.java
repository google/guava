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
 * Test suite covering {@link Bytes#asList(byte[])}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class ByteArrayAsListTest extends TestCase {

  private static List<Byte> asList(Byte[] values) {
    byte[] temp = new byte[values.length];
    for (int i = 0; i < values.length; i++) {
      temp[i] = checkNotNull(values[i]); // checkNotNull for GWT (do not optimize).
    }
    return Bytes.asList(temp);
  }

  @GwtIncompatible // suite
  public static Test suite() {
    List<ListTestSuiteBuilder<Byte>> builders =
        ImmutableList.of(
            ListTestSuiteBuilder.using(new BytesAsListGenerator()).named("Bytes.asList"),
            ListTestSuiteBuilder.using(new BytesAsListHeadSubListGenerator())
                .named("Bytes.asList, head subList"),
            ListTestSuiteBuilder.using(new BytesAsListTailSubListGenerator())
                .named("Bytes.asList, tail subList"),
            ListTestSuiteBuilder.using(new BytesAsListMiddleSubListGenerator())
                .named("Bytes.asList, middle subList"));

    TestSuite suite = new TestSuite();
    for (ListTestSuiteBuilder<Byte> builder : builders) {
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

  public static final class BytesAsListGenerator extends TestByteListGenerator {
    @Override
    protected List<Byte> create(Byte[] elements) {
      return asList(elements);
    }
  }

  public static final class BytesAsListHeadSubListGenerator extends TestByteListGenerator {
    @Override
    protected List<Byte> create(Byte[] elements) {
      Byte[] suffix = {Byte.MIN_VALUE, Byte.MAX_VALUE};
      Byte[] all = concat(elements, suffix);
      return asList(all).subList(0, elements.length);
    }
  }

  public static final class BytesAsListTailSubListGenerator extends TestByteListGenerator {
    @Override
    protected List<Byte> create(Byte[] elements) {
      Byte[] prefix = {(byte) 86, (byte) 99};
      Byte[] all = concat(prefix, elements);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  public static final class BytesAsListMiddleSubListGenerator extends TestByteListGenerator {
    @Override
    protected List<Byte> create(Byte[] elements) {
      Byte[] prefix = {Byte.MIN_VALUE, Byte.MAX_VALUE};
      Byte[] suffix = {(byte) 86, (byte) 99};
      Byte[] all = concat(concat(prefix, elements), suffix);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  private static Byte[] concat(Byte[] left, Byte[] right) {
    Byte[] result = new Byte[left.length + right.length];
    System.arraycopy(left, 0, result, 0, left.length);
    System.arraycopy(right, 0, result, left.length, right.length);
    return result;
  }

  public abstract static class TestByteListGenerator implements TestListGenerator<Byte> {
    @Override
    public SampleElements<Byte> samples() {
      return new SampleBytes();
    }

    @Override
    public List<Byte> create(Object... elements) {
      Byte[] array = new Byte[elements.length];
      int i = 0;
      for (Object e : elements) {
        array[i++] = (Byte) e;
      }
      return create(array);
    }

    /**
     * Creates a new collection containing the given elements; implement this method instead of
     * {@link #create(Object...)}.
     */
    protected abstract List<Byte> create(Byte[] elements);

    @Override
    public Byte[] createArray(int length) {
      return new Byte[length];
    }

    /** Returns the original element list, unchanged. */
    @Override
    public List<Byte> order(List<Byte> insertionOrder) {
      return insertionOrder;
    }
  }

  public static class SampleBytes extends SampleElements<Byte> {
    public SampleBytes() {
      super((byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4);
    }
  }
}

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
 * Test suite covering {@link Chars#asList(char[])}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class CharArrayAsListTest extends TestCase {

  private static List<Character> asList(Character[] values) {
    char[] temp = new char[values.length];
    for (int i = 0; i < values.length; i++) {
      temp[i] = checkNotNull(values[i]); // checkNotNull for GWT (do not optimize).
    }
    return Chars.asList(temp);
  }

  @J2ktIncompatible
  @GwtIncompatible // suite
  public static Test suite() {
    List<ListTestSuiteBuilder<Character>> builders =
        ImmutableList.of(
            ListTestSuiteBuilder.using(new CharsAsListGenerator()).named("Chars.asList"),
            ListTestSuiteBuilder.using(new CharsAsListHeadSubListGenerator())
                .named("Chars.asList, head subList"),
            ListTestSuiteBuilder.using(new CharsAsListTailSubListGenerator())
                .named("Chars.asList, tail subList"),
            ListTestSuiteBuilder.using(new CharsAsListMiddleSubListGenerator())
                .named("Chars.asList, middle subList"));

    TestSuite suite = new TestSuite();
    for (ListTestSuiteBuilder<Character> builder : builders) {
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

  public static final class CharsAsListGenerator extends TestCharListGenerator {
    @Override
    protected List<Character> create(Character[] elements) {
      return asList(elements);
    }
  }

  public static final class CharsAsListHeadSubListGenerator extends TestCharListGenerator {
    @Override
    protected List<Character> create(Character[] elements) {
      Character[] suffix = {Character.MIN_VALUE, Character.MAX_VALUE};
      Character[] all = concat(elements, suffix);
      return asList(all).subList(0, elements.length);
    }
  }

  public static final class CharsAsListTailSubListGenerator extends TestCharListGenerator {
    @Override
    protected List<Character> create(Character[] elements) {
      Character[] prefix = {(char) 86, (char) 99};
      Character[] all = concat(prefix, elements);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  public static final class CharsAsListMiddleSubListGenerator extends TestCharListGenerator {
    @Override
    protected List<Character> create(Character[] elements) {
      Character[] prefix = {Character.MIN_VALUE, Character.MAX_VALUE};
      Character[] suffix = {(char) 86, (char) 99};
      Character[] all = concat(concat(prefix, elements), suffix);
      return asList(all).subList(2, elements.length + 2);
    }
  }

  private static Character[] concat(Character[] left, Character[] right) {
    Character[] result = new Character[left.length + right.length];
    System.arraycopy(left, 0, result, 0, left.length);
    System.arraycopy(right, 0, result, left.length, right.length);
    return result;
  }

  public abstract static class TestCharListGenerator implements TestListGenerator<Character> {
    @Override
    public SampleElements<Character> samples() {
      return new SampleChars();
    }

    @Override
    public List<Character> create(Object... elements) {
      Character[] array = new Character[elements.length];
      int i = 0;
      for (Object e : elements) {
        array[i++] = (Character) e;
      }
      return create(array);
    }

    /**
     * Creates a new collection containing the given elements; implement this method instead of
     * {@link #create(Object...)}.
     */
    protected abstract List<Character> create(Character[] elements);

    @Override
    public Character[] createArray(int length) {
      return new Character[length];
    }

    /** Returns the original element list, unchanged. */
    @Override
    public List<Character> order(List<Character> insertionOrder) {
      return insertionOrder;
    }
  }

  public static class SampleChars extends SampleElements<Character> {
    public SampleChars() {
      super((char) 0, (char) 1, (char) 2, (char) 3, (char) 4);
    }
  }
}

/*
 * Copyright (C) 2017 The Guava Authors
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

package com.google.common.collect;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/** Tests the package level *impl methods directly using various types of lists. */
@GwtCompatible(emulated = true)
public class ListsImplTest extends TestCase {

  /** Describes how a list is modifiable */
  public enum Modifiability {
    NONE, // immutable lists
    BY_ELEMENT, // elements can change (set), but not structure
    DIRECT_ONLY, // Element can be added and removed only via direct calls, not through iterators
    ALL // Elements can be added and removed as well as modified.
  }

  /** Handles the creation of lists needed for the tests */
  public abstract static class ListExample {

    private final String name;
    private final Modifiability modifiability;

    protected ListExample(String name, Modifiability modifiability) {
      this.name = name;
      this.modifiability = modifiability;
    }

    /** Gets the name of the example */
    public String getName() {
      return name;
    }

    /** Creates a new list with the given contents. */
    public abstract <T> List<T> createList(Class<T> listType, Collection<? extends T> contents);

    /** The modifiability of this list example. */
    public Modifiability modifiability() {
      return modifiability;
    }
  }

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(createExampleSuite(new ArrayListExample("ArrayList")));
    suite.addTest(createExampleSuite(new LinkedListExample("LinkedList")));
    suite.addTest(createExampleSuite(new ArraysAsListExample("Arrays.asList")));
    suite.addTest(createExampleSuite(new ImmutableListExample("ImmutableList")));
    suite.addTest(createExampleSuite(new CopyOnWriteListExample("CopyOnWriteArrayList")));
    suite.addTestSuite(ListsImplTest.class);
    return suite;
  }

  @GwtIncompatible // suite sub call
  private static TestSuite createExampleSuite(ListExample example) {
    TestSuite resultSuite = new TestSuite(ListsImplTest.class);
    for (Enumeration<Test> testEnum = resultSuite.tests(); testEnum.hasMoreElements(); ) {
      ListsImplTest test = (ListsImplTest) testEnum.nextElement();
      test.example = example;
    }
    return resultSuite;
  }

  private ListExample example;

  private ListExample getExample() {
    // because sometimes one version with a null example is created.
    return example == null ? new ImmutableListExample("test") : example;
  }

  @GwtIncompatible // not used under GWT, and super.getName() is not available under J2CL
  @Override
  public String getName() {
    return example == null ? super.getName() : buildTestName();
  }

  @GwtIncompatible // not used under GWT, and super.getName() is not available under J2CL
  private String buildTestName() {
    return super.getName() + ":" + example.getName();
  }

  public void testHashCodeImpl() {
    List<Integer> base = createList(Integer.class, 1, 2, 2);
    List<Integer> copy = createList(Integer.class, 1, 2, 2);
    List<Integer> outOfOrder = createList(Integer.class, 2, 2, 1);
    List<Integer> diffValue = createList(Integer.class, 1, 2, 4);
    List<Integer> diffLength = createList(Integer.class, 1, 2);
    List<Integer> empty = createList(Integer.class);

    assertThat(Lists.hashCodeImpl(base)).isEqualTo(Lists.hashCodeImpl(copy));

    assertThat(Lists.hashCodeImpl(base)).isNotEqualTo(Lists.hashCodeImpl(outOfOrder));
    assertThat(Lists.hashCodeImpl(base)).isNotEqualTo(Lists.hashCodeImpl(diffValue));
    assertThat(Lists.hashCodeImpl(base)).isNotEqualTo(Lists.hashCodeImpl(diffLength));
    assertThat(Lists.hashCodeImpl(base)).isNotEqualTo(Lists.hashCodeImpl(empty));
  }

  public void testEqualsImpl() {
    List<Integer> base = createList(Integer.class, 1, 2, 2);
    List<Integer> copy = createList(Integer.class, 1, 2, 2);
    ImmutableList<Integer> otherType = ImmutableList.of(1, 2, 2);
    List<Integer> outOfOrder = createList(Integer.class, 2, 2, 1);
    List<Integer> diffValue = createList(Integer.class, 1, 2, 3);
    List<Integer> diffLength = createList(Integer.class, 1, 2);
    List<Integer> empty = createList(Integer.class);

    assertThat(Lists.equalsImpl(base, copy)).isTrue();
    assertThat(Lists.equalsImpl(base, otherType)).isTrue();

    List<Object> unEqualItems =
        Arrays.asList(outOfOrder, diffValue, diffLength, empty, null, new Object());
    for (Object other : unEqualItems) {
      assertWithMessage("%s", other).that(Lists.equalsImpl(base, other)).isFalse();
    }
  }

  public void testAddAllImpl() {
    if (getExample().modifiability() != Modifiability.ALL) {
      return;
    }
    List<String> toTest = createList(String.class);

    List<Iterable<String>> toAdd =
        ImmutableList.of(
            (Iterable<String>) Collections.singleton("A"),
            Collections.<String>emptyList(),
            ImmutableList.of("A", "B", "C"),
            ImmutableList.of("D", "E"));
    List<Integer> indexes = ImmutableList.of(0, 0, 1, 3);
    List<List<String>> expected =
        ImmutableList.of(
            Collections.singletonList("A"),
            ImmutableList.of("A"),
            ImmutableList.of("A", "A", "B", "C"),
            ImmutableList.of("A", "A", "D", "E", "B", "C"));

    String format = "Adding %s at %s";
    for (int i = 0; i < toAdd.size(); i++) {
      int index = indexes.get(i);
      Iterable<String> iterableToAdd = toAdd.get(i);
      boolean expectedChanged = iterableToAdd.iterator().hasNext();
      assertWithMessage(format, iterableToAdd, index)
          .that(Lists.addAllImpl(toTest, index, iterableToAdd))
          .isEqualTo(expectedChanged);
      assertWithMessage(format, iterableToAdd, index)
          .that(toTest)
          .containsExactlyElementsIn(expected.get(i));
    }
  }

  public void testIndexOfImpl_nonNull() {
    List<Integer> toTest = createList(Integer.class, 5, 2, -1, 2, 1, 10, 5);
    int[] expected = {0, 1, 2, 1, 4, 5, 0};
    checkIndexOf(toTest, expected);
  }

  public void testIndexOfImpl_null() {
    List<String> toTest;
    try {
      toTest = createList(String.class, null, "A", "B", null, "C", null);
    } catch (NullPointerException e) {
      // example cannot handle nulls, test invalid
      return;
    }
    int[] expected = {0, 1, 2, 0, 4, 0};
    checkIndexOf(toTest, expected);
  }

  public void testLastIndexOfImpl_nonNull() {
    List<Integer> toTest = createList(Integer.class, 1, 5, 6, 10, 1, 3, 2, 1, 6);
    int[] expected = {7, 1, 8, 3, 7, 5, 6, 7, 8};
    checkLastIndexOf(toTest, expected);
  }

  public void testLastIndexOfImpl_null() {
    List<String> toTest;
    try {
      toTest = createList(String.class, null, "A", "B", null, "C", "B");
    } catch (NullPointerException e) {
      // example cannot handle nulls, test invalid
      return;
    }
    int[] expected = {3, 1, 5, 3, 4, 5};
    checkLastIndexOf(toTest, expected);
  }

  private void checkIndexOf(List<?> toTest, int[] expected) {
    int index = 0;
    for (Object obj : toTest) {
      String name = "toTest[" + index + "] (" + obj + ")";
      assertWithMessage(name).that(Lists.indexOfImpl(toTest, obj)).isEqualTo(expected[index]);
      index++;
    }
  }

  private void checkLastIndexOf(List<?> toTest, int[] expected) {
    int index = 0;
    for (Object obj : toTest) {
      String name = "toTest[" + index + "] (" + obj + ")";
      assertWithMessage(name).that(Lists.lastIndexOfImpl(toTest, obj)).isEqualTo(expected[index]);
      index++;
    }
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  private final <T> List<T> createList(Class<T> listType, T... contents) {
    return getExample().createList(listType, Arrays.asList(contents));
  }

  private static final class ArrayListExample extends ListExample {

    protected ArrayListExample(String name) {
      super(name, Modifiability.ALL);
    }

    @Override
    public <T> List<T> createList(Class<T> listType, Collection<? extends T> contents) {
      return new ArrayList<>(contents);
    }
  }

  private static final class LinkedListExample extends ListExample {

    protected LinkedListExample(String name) {
      super(name, Modifiability.ALL);
    }

    @Override
    public <T> List<T> createList(Class<T> listType, Collection<? extends T> contents) {
      return new LinkedList<>(contents);
    }
  }

  @GwtIncompatible // Iterables.toArray
  private static final class ArraysAsListExample extends ListExample {

    protected ArraysAsListExample(String name) {
      super(name, Modifiability.BY_ELEMENT);
    }

    @Override
    public <T> List<T> createList(Class<T> listType, Collection<? extends T> contents) {
      @SuppressWarnings("unchecked") // safe by contract
      T[] array = Iterables.toArray(contents, listType);
      return Arrays.asList(array);
    }
  }

  private static final class ImmutableListExample extends ListExample {

    protected ImmutableListExample(String name) {
      super(name, Modifiability.NONE);
    }

    @Override
    public <T> List<T> createList(Class<T> listType, Collection<? extends T> contents) {
      return ImmutableList.copyOf(contents);
    }
  }

  @GwtIncompatible // CopyOnWriteArrayList
  private static final class CopyOnWriteListExample extends ListExample {

    protected CopyOnWriteListExample(String name) {
      super(name, Modifiability.DIRECT_ONLY);
    }

    @Override
    public <T> List<T> createList(Class<T> listType, Collection<? extends T> contents) {
      return new CopyOnWriteArrayList<>(contents);
    }
  }
}

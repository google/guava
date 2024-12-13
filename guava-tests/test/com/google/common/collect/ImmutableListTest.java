/*
 * Copyright (C) 2007 The Guava Authors
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

import static com.google.common.collect.Iterators.emptyIterator;
import static com.google.common.collect.Iterators.singletonIterator;
import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.collect.testing.Helpers.misleadingSizeCollection;
import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_QUERIES;
import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.MinimalIterable;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.ListGenerators.BuilderAddAllListGenerator;
import com.google.common.collect.testing.google.ListGenerators.BuilderReversedListGenerator;
import com.google.common.collect.testing.google.ListGenerators.ImmutableListHeadSubListGenerator;
import com.google.common.collect.testing.google.ListGenerators.ImmutableListMiddleSubListGenerator;
import com.google.common.collect.testing.google.ListGenerators.ImmutableListOfGenerator;
import com.google.common.collect.testing.google.ListGenerators.ImmutableListTailSubListGenerator;
import com.google.common.collect.testing.google.ListGenerators.UnhashableElementsImmutableListGenerator;
import com.google.common.collect.testing.testers.ListHashCodeTester;
import com.google.common.testing.CollectorTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unit test for {@link ImmutableList}.
 *
 * @author Kevin Bourrillion
 * @author George van den Driessche
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public class ImmutableListTest extends TestCase {

  @J2ktIncompatible
  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableListOfGenerator())
            .named("ImmutableList")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new BuilderAddAllListGenerator())
            .named("ImmutableList, built with Builder.add")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new BuilderAddAllListGenerator())
            .named("ImmutableList, built with Builder.addAll")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new BuilderReversedListGenerator())
            .named("ImmutableList, reversed")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableListHeadSubListGenerator())
            .named("ImmutableList, head subList")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableListTailSubListGenerator())
            .named("ImmutableList, tail subList")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new ImmutableListMiddleSubListGenerator())
            .named("ImmutableList, middle subList")
            .withFeatures(CollectionSize.ANY, SERIALIZABLE, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    suite.addTest(
        ListTestSuiteBuilder.using(new UnhashableElementsImmutableListGenerator())
            .suppressing(ListHashCodeTester.getHashCodeMethod())
            .named("ImmutableList, unhashable values")
            .withFeatures(CollectionSize.ANY, ALLOWS_NULL_QUERIES)
            .createTestSuite());
    return suite;
  }

  // Creation tests

  public void testCreation_noArgs() {
    List<String> list = ImmutableList.of();
    assertEquals(emptyList(), list);
  }

  public void testCreation_oneElement() {
    List<String> list = ImmutableList.of("a");
    assertEquals(singletonList("a"), list);
  }

  public void testCreation_twoElements() {
    List<String> list = ImmutableList.of("a", "b");
    assertEquals(Lists.newArrayList("a", "b"), list);
  }

  public void testCreation_threeElements() {
    List<String> list = ImmutableList.of("a", "b", "c");
    assertEquals(Lists.newArrayList("a", "b", "c"), list);
  }

  public void testCreation_fourElements() {
    List<String> list = ImmutableList.of("a", "b", "c", "d");
    assertEquals(Lists.newArrayList("a", "b", "c", "d"), list);
  }

  public void testCreation_fiveElements() {
    List<String> list = ImmutableList.of("a", "b", "c", "d", "e");
    assertEquals(Lists.newArrayList("a", "b", "c", "d", "e"), list);
  }

  public void testCreation_sixElements() {
    List<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f");
    assertEquals(Lists.newArrayList("a", "b", "c", "d", "e", "f"), list);
  }

  public void testCreation_sevenElements() {
    List<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g");
    assertEquals(Lists.newArrayList("a", "b", "c", "d", "e", "f", "g"), list);
  }

  public void testCreation_eightElements() {
    List<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h");
    assertEquals(Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h"), list);
  }

  public void testCreation_nineElements() {
    List<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
    assertEquals(Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i"), list);
  }

  public void testCreation_tenElements() {
    List<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
    assertEquals(Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"), list);
  }

  public void testCreation_elevenElements() {
    List<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");
    assertEquals(Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"), list);
  }

  // Varargs versions

  public void testCreation_twelveElements() {
    List<String> list =
        ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l");
    assertEquals(
        Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"), list);
  }

  public void testCreation_thirteenElements() {
    List<String> list =
        ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m");
    assertEquals(
        Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m"), list);
  }

  public void testCreation_fourteenElements() {
    List<String> list =
        ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n");
    assertEquals(
        Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n"),
        list);
  }

  public void testCreation_singletonNull() {
    assertThrows(NullPointerException.class, () -> ImmutableList.of((String) null));
  }

  public void testCreation_withNull() {
    assertThrows(NullPointerException.class, () -> ImmutableList.of("a", null, "b"));
  }

  public void testCreation_generic() {
    List<String> a = ImmutableList.of("a");
    // only verify that there is no compile warning
    ImmutableList<List<String>> unused = ImmutableList.of(a, a);
  }

  public void testCreation_arrayOfArray() {
    String[] array = new String[] {"a"};
    List<String[]> list = ImmutableList.<String[]>of(array);
    assertEquals(singletonList(array), list);
  }

  public void testCopyOf_emptyArray() {
    String[] array = new String[0];
    List<String> list = ImmutableList.copyOf(array);
    assertEquals(emptyList(), list);
  }

  public void testCopyOf_arrayOfOneElement() {
    String[] array = new String[] {"a"};
    List<String> list = ImmutableList.copyOf(array);
    assertEquals(singletonList("a"), list);
  }

  public void testCopyOf_nullArray() {
    assertThrows(NullPointerException.class, () -> ImmutableList.copyOf((String[]) null));
  }

  public void testCopyOf_arrayContainingOnlyNull() {
    @Nullable String[] array = new @Nullable String[] {null};
    assertThrows(NullPointerException.class, () -> ImmutableList.copyOf((String[]) array));
  }

  public void testCopyOf_collection_empty() {
    // "<String>" is required to work around a javac 1.5 bug.
    Collection<String> c = MinimalCollection.<String>of();
    List<String> list = ImmutableList.copyOf(c);
    assertEquals(emptyList(), list);
  }

  public void testCopyOf_collection_oneElement() {
    Collection<String> c = MinimalCollection.of("a");
    List<String> list = ImmutableList.copyOf(c);
    assertEquals(singletonList("a"), list);
  }

  public void testCopyOf_collection_general() {
    Collection<String> c = MinimalCollection.of("a", "b", "a");
    List<String> list = ImmutableList.copyOf(c);
    assertEquals(asList("a", "b", "a"), list);
    List<String> mutableList = asList("a", "b");
    list = ImmutableList.copyOf(mutableList);
    mutableList.set(0, "c");
    assertEquals(asList("a", "b"), list);
  }

  public void testCopyOf_collectionContainingNull() {
    Collection<@Nullable String> c = MinimalCollection.of("a", null, "b");
    assertThrows(NullPointerException.class, () -> ImmutableList.copyOf((Collection<String>) c));
  }

  public void testCopyOf_iterator_empty() {
    Iterator<String> iterator = emptyIterator();
    List<String> list = ImmutableList.copyOf(iterator);
    assertEquals(emptyList(), list);
  }

  public void testCopyOf_iterator_oneElement() {
    Iterator<String> iterator = singletonIterator("a");
    List<String> list = ImmutableList.copyOf(iterator);
    assertEquals(singletonList("a"), list);
  }

  public void testCopyOf_iterator_general() {
    Iterator<String> iterator = asList("a", "b", "a").iterator();
    List<String> list = ImmutableList.copyOf(iterator);
    assertEquals(asList("a", "b", "a"), list);
  }

  public void testCopyOf_iteratorContainingNull() {
    Iterator<@Nullable String> iterator =
        Arrays.<@Nullable String>asList("a", null, "b").iterator();
    assertThrows(
        NullPointerException.class, () -> ImmutableList.copyOf((Iterator<String>) iterator));
  }

  public void testCopyOf_iteratorNull() {
    assertThrows(NullPointerException.class, () -> ImmutableList.copyOf((Iterator<String>) null));
  }

  public void testCopyOf_concurrentlyMutating() {
    List<String> sample = Lists.newArrayList("a", "b", "c");
    for (int delta : new int[] {-1, 0, 1}) {
      for (int i = 0; i < sample.size(); i++) {
        Collection<String> misleading = misleadingSizeCollection(delta);
        List<String> expected = sample.subList(0, i);
        misleading.addAll(expected);
        assertEquals(expected, ImmutableList.copyOf(misleading));
        assertEquals(expected, ImmutableList.copyOf((Iterable<String>) misleading));
      }
    }
  }

  private static class CountingIterable implements Iterable<String> {
    int count = 0;

    @Override
    public Iterator<String> iterator() {
      count++;
      return asList("a", "b", "a").iterator();
    }
  }

  public void testCopyOf_plainIterable() {
    CountingIterable iterable = new CountingIterable();
    List<String> list = ImmutableList.copyOf(iterable);
    assertEquals(asList("a", "b", "a"), list);
  }

  public void testCopyOf_plainIterable_iteratesOnce() {
    CountingIterable iterable = new CountingIterable();
    ImmutableList<String> unused = ImmutableList.copyOf(iterable);
    assertEquals(1, iterable.count);
  }

  public void testCopyOf_shortcut_empty() {
    Collection<String> c = ImmutableList.of();
    assertSame(c, ImmutableList.copyOf(c));
  }

  public void testCopyOf_shortcut_singleton() {
    Collection<String> c = ImmutableList.of("a");
    assertSame(c, ImmutableList.copyOf(c));
  }

  public void testCopyOf_shortcut_immutableList() {
    Collection<String> c = ImmutableList.of("a", "b", "c");
    assertSame(c, ImmutableList.copyOf(c));
  }

  public void testBuilderAddArrayHandlesNulls() {
    @Nullable String[] elements = new @Nullable String[] {"a", null, "b"};
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    assertThrows(NullPointerException.class, () -> builder.add((String[]) elements));
    ImmutableList<String> result = builder.build();

    /*
     * Maybe it rejects all elements, or maybe it adds "a" before failing.
     * Either way is fine with us.
     */
    if (result.isEmpty()) {
      return;
    }
    assertTrue(ImmutableList.of("a").equals(result));
    assertEquals(1, result.size());
  }

  public void testBuilderAddCollectionHandlesNulls() {
    List<@Nullable String> elements = asList("a", null, "b");
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    assertThrows(NullPointerException.class, () -> builder.addAll((List<String>) elements));
    ImmutableList<String> result = builder.build();
    assertEquals(ImmutableList.of("a"), result);
    assertEquals(1, result.size());
  }

  public void testSortedCopyOf_natural() {
    Collection<Integer> c = MinimalCollection.of(4, 16, 10, -1, 5);
    ImmutableList<Integer> list = ImmutableList.sortedCopyOf(c);
    assertEquals(asList(-1, 4, 5, 10, 16), list);
  }

  public void testSortedCopyOf_natural_empty() {
    Collection<Integer> c = MinimalCollection.of();
    ImmutableList<Integer> list = ImmutableList.sortedCopyOf(c);
    assertEquals(asList(), list);
  }

  public void testSortedCopyOf_natural_singleton() {
    Collection<Integer> c = MinimalCollection.of(100);
    ImmutableList<Integer> list = ImmutableList.sortedCopyOf(c);
    assertEquals(asList(100), list);
  }

  public void testSortedCopyOf_natural_containsNull() {
    Collection<@Nullable Integer> c = MinimalCollection.of(1, 3, null, 2);
    assertThrows(
        NullPointerException.class, () -> ImmutableList.sortedCopyOf((Collection<Integer>) c));
  }

  public void testSortedCopyOf() {
    Collection<String> c = MinimalCollection.of("a", "b", "A", "c");
    List<String> list = ImmutableList.sortedCopyOf(String.CASE_INSENSITIVE_ORDER, c);
    assertEquals(asList("a", "A", "b", "c"), list);
  }

  public void testSortedCopyOf_empty() {
    Collection<String> c = MinimalCollection.of();
    List<String> list = ImmutableList.sortedCopyOf(String.CASE_INSENSITIVE_ORDER, c);
    assertEquals(asList(), list);
  }

  public void testSortedCopyOf_singleton() {
    Collection<String> c = MinimalCollection.of("a");
    List<String> list = ImmutableList.sortedCopyOf(String.CASE_INSENSITIVE_ORDER, c);
    assertEquals(asList("a"), list);
  }

  public void testSortedCopyOf_containsNull() {
    Collection<@Nullable String> c = MinimalCollection.of("a", "b", "A", null, "c");
    assertThrows(
        NullPointerException.class,
        () -> ImmutableList.sortedCopyOf(String.CASE_INSENSITIVE_ORDER, (Collection<String>) c));
  }

  public void testToImmutableList() {
    CollectorTester.of(ImmutableList.<String>toImmutableList())
        .expectCollects(ImmutableList.of("a", "b", "c", "d"), "a", "b", "c", "d");
  }

  // Basic tests

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(ImmutableList.class);
    tester.testAllPublicInstanceMethods(ImmutableList.of(1, 2, 3));
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testSerialization_empty() {
    Collection<String> c = ImmutableList.of();
    assertSame(c, SerializableTester.reserialize(c));
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testSerialization_singleton() {
    Collection<String> c = ImmutableList.of("a");
    SerializableTester.reserializeAndAssert(c);
  }

  @J2ktIncompatible
  @GwtIncompatible // SerializableTester
  public void testSerialization_multiple() {
    Collection<String> c = ImmutableList.of("a", "b", "c");
    SerializableTester.reserializeAndAssert(c);
  }

  public void testEquals_immutableList() {
    Collection<String> c = ImmutableList.of("a", "b", "c");
    assertTrue(c.equals(ImmutableList.of("a", "b", "c")));
    assertFalse(c.equals(ImmutableList.of("a", "c", "b")));
    assertFalse(c.equals(ImmutableList.of("a", "b")));
    assertFalse(c.equals(ImmutableList.of("a", "b", "c", "d")));
  }

  public void testBuilderAdd() {
    ImmutableList<String> list =
        new ImmutableList.Builder<String>().add("a").add("b").add("a").add("c").build();
    assertEquals(asList("a", "b", "a", "c"), list);
  }

  @GwtIncompatible("Builder impl")
  public void testBuilderForceCopy() {
    ImmutableList.Builder<Integer> builder = ImmutableList.builder();
    Object[] prevArray = null;
    for (int i = 0; i < 10; i++) {
      builder.add(i);
      assertNotSame(builder.contents, prevArray);
      prevArray = builder.contents;
      ImmutableList<Integer> unused = builder.build();
    }
  }

  @GwtIncompatible
  public void testBuilderExactlySizedReusesArray() {
    ImmutableList.Builder<Integer> builder = ImmutableList.builderWithExpectedSize(10);
    Object[] builderArray = builder.contents;
    for (int i = 0; i < 10; i++) {
      builder.add(i);
    }
    Object[] builderArrayAfterAdds = builder.contents;
    RegularImmutableList<Integer> list = (RegularImmutableList<Integer>) builder.build();
    Object[] listInternalArray = list.array;
    assertSame(builderArray, builderArrayAfterAdds);
    assertSame(builderArray, listInternalArray);
  }

  public void testBuilderAdd_varargs() {
    ImmutableList<String> list =
        new ImmutableList.Builder<String>().add("a", "b", "a", "c").build();
    assertEquals(asList("a", "b", "a", "c"), list);
  }

  public void testBuilderAddAll_iterable() {
    List<String> a = asList("a", "b");
    List<String> b = asList("c", "d");
    ImmutableList<String> list = new ImmutableList.Builder<String>().addAll(a).addAll(b).build();
    assertEquals(asList("a", "b", "c", "d"), list);
    b.set(0, "f");
    assertEquals(asList("a", "b", "c", "d"), list);
  }

  public void testBuilderAddAll_iterator() {
    List<String> a = asList("a", "b");
    List<String> b = asList("c", "d");
    ImmutableList<String> list =
        new ImmutableList.Builder<String>().addAll(a.iterator()).addAll(b.iterator()).build();
    assertEquals(asList("a", "b", "c", "d"), list);
    b.set(0, "f");
    assertEquals(asList("a", "b", "c", "d"), list);
  }

  public void testComplexBuilder() {
    List<Integer> colorElem = asList(0x00, 0x33, 0x66, 0x99, 0xCC, 0xFF);
    ImmutableList.Builder<Integer> webSafeColorsBuilder = ImmutableList.builder();
    for (Integer red : colorElem) {
      for (Integer green : colorElem) {
        for (Integer blue : colorElem) {
          webSafeColorsBuilder.add((red << 16) + (green << 8) + blue);
        }
      }
    }
    ImmutableList<Integer> webSafeColors = webSafeColorsBuilder.build();
    assertEquals(216, webSafeColors.size());
    Integer[] webSafeColorArray = webSafeColors.toArray(new Integer[webSafeColors.size()]);
    assertEquals(0x000000, (int) webSafeColorArray[0]);
    assertEquals(0x000033, (int) webSafeColorArray[1]);
    assertEquals(0x000066, (int) webSafeColorArray[2]);
    assertEquals(0x003300, (int) webSafeColorArray[6]);
    assertEquals(0x330000, (int) webSafeColorArray[36]);
    assertEquals(0x000066, (int) webSafeColors.get(2));
    assertEquals(0x003300, (int) webSafeColors.get(6));
    ImmutableList<Integer> addedColor = webSafeColorsBuilder.add(0x00BFFF).build();
    assertEquals(
        "Modifying the builder should not have changed any already" + " built sets",
        216,
        webSafeColors.size());
    assertEquals("the new array should be one bigger than webSafeColors", 217, addedColor.size());
    Integer[] appendColorArray = addedColor.toArray(new Integer[addedColor.size()]);
    assertEquals(0x00BFFF, (int) appendColorArray[216]);
  }

  public void testBuilderAddHandlesNullsCorrectly() {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    assertThrows(NullPointerException.class, () -> builder.add((String) null));

    assertThrows(NullPointerException.class, () -> builder.add((String[]) null));

    assertThrows(NullPointerException.class, () -> builder.add("a", null, "b"));
  }

  public void testBuilderAddAllHandlesNullsCorrectly() {
    {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
      assertThrows(NullPointerException.class, () -> builder.addAll((Iterable<String>) null));
    }

    {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      assertThrows(NullPointerException.class, () -> builder.addAll((Iterator<String>) null));
    }

    {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
    List<@Nullable String> listWithNulls = asList("a", null, "b");
      assertThrows(NullPointerException.class, () -> builder.addAll((List<String>) listWithNulls));
    }

    {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
    Iterator<@Nullable String> iteratorWithNulls =
        Arrays.<@Nullable String>asList("a", null, "b").iterator();
      assertThrows(
          NullPointerException.class, () -> builder.addAll((Iterator<String>) iteratorWithNulls));
    }

    {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
    Iterable<@Nullable String> iterableWithNulls = MinimalIterable.of("a", null, "b");
      assertThrows(
          NullPointerException.class, () -> builder.addAll((Iterable<String>) iterableWithNulls));
    }
  }

  public void testAsList() {
    ImmutableList<String> list = ImmutableList.of("a", "b");
    assertSame(list, list.asList());
  }

  @SuppressWarnings("ModifiedButNotUsed")
  @GwtIncompatible // actually allocates nCopies
  @J2ktIncompatible // actually allocates nCopies
  public void testAddOverflowCollection() {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (int i = 0; i < 100; i++) {
      builder.add("a");
    }
    IllegalArgumentException expected =
        assertThrows(
            IllegalArgumentException.class,
            () -> builder.addAll(nCopies(Integer.MAX_VALUE - 50, "a")));
    assertThat(expected).hasMessageThat().contains("cannot store more than MAX_VALUE elements");
  }
}

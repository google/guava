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

package com.google.common.collect;

import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.IteratorTester;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.MinimalIterable;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;

/**
 * Base class for {@link ImmutableSet} and {@link ImmutableSortedSet} tests.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public abstract class AbstractImmutableSetTest extends TestCase {

  protected abstract <E extends Comparable<? super E>> Set<E> of();

  protected abstract <E extends Comparable<? super E>> Set<E> of(E e);

  protected abstract <E extends Comparable<? super E>> Set<E> of(E e1, E e2);

  protected abstract <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3);

  protected abstract <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3, E e4);

  protected abstract <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3, E e4, E e5);

  @SuppressWarnings("unchecked")
  protected abstract <E extends Comparable<? super E>> Set<E> of(
      E e1, E e2, E e3, E e4, E e5, E e6, E... rest);

  protected abstract <E extends Comparable<? super E>> Set<E> copyOf(E[] elements);

  protected abstract <E extends Comparable<? super E>> Set<E> copyOf(
      Collection<? extends E> elements);

  protected abstract <E extends Comparable<? super E>> Set<E> copyOf(
      Iterable<? extends E> elements);

  protected abstract <E extends Comparable<? super E>> Set<E> copyOf(
      Iterator<? extends E> elements);

  public void testCreation_noArgs() {
    Set<String> set = of();
    assertEquals(Collections.<String>emptySet(), set);
    assertSame(of(), set);
  }

  public void testCreation_oneElement() {
    Set<String> set = of("a");
    assertEquals(Collections.singleton("a"), set);
  }

  public void testCreation_twoElements() {
    Set<String> set = of("a", "b");
    assertEquals(Sets.newHashSet("a", "b"), set);
  }

  public void testCreation_threeElements() {
    Set<String> set = of("a", "b", "c");
    assertEquals(Sets.newHashSet("a", "b", "c"), set);
  }

  public void testCreation_fourElements() {
    Set<String> set = of("a", "b", "c", "d");
    assertEquals(Sets.newHashSet("a", "b", "c", "d"), set);
  }

  public void testCreation_fiveElements() {
    Set<String> set = of("a", "b", "c", "d", "e");
    assertEquals(Sets.newHashSet("a", "b", "c", "d", "e"), set);
  }

  public void testCreation_sixElements() {
    Set<String> set = of("a", "b", "c", "d", "e", "f");
    assertEquals(Sets.newHashSet("a", "b", "c", "d", "e", "f"), set);
  }

  public void testCreation_sevenElements() {
    Set<String> set = of("a", "b", "c", "d", "e", "f", "g");
    assertEquals(Sets.newHashSet("a", "b", "c", "d", "e", "f", "g"), set);
  }

  public void testCreation_eightElements() {
    Set<String> set = of("a", "b", "c", "d", "e", "f", "g", "h");
    assertEquals(Sets.newHashSet("a", "b", "c", "d", "e", "f", "g", "h"), set);
  }

  public void testCopyOf_emptyArray() {
    String[] array = new String[0];
    Set<String> set = copyOf(array);
    assertEquals(Collections.<String>emptySet(), set);
    assertSame(of(), set);
  }

  public void testCopyOf_arrayOfOneElement() {
    String[] array = new String[] {"a"};
    Set<String> set = copyOf(array);
    assertEquals(Collections.singleton("a"), set);
  }

  public void testCopyOf_nullArray() {
    try {
      copyOf((String[]) null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testCopyOf_arrayContainingOnlyNull() {
    String[] array = new String[] {null};
    try {
      copyOf(array);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testCopyOf_collection_empty() {
    // "<String>" is required to work around a javac 1.5 bug.
    Collection<String> c = MinimalCollection.<String>of();
    Set<String> set = copyOf(c);
    assertEquals(Collections.<String>emptySet(), set);
    assertSame(of(), set);
  }

  public void testCopyOf_collection_oneElement() {
    Collection<String> c = MinimalCollection.of("a");
    Set<String> set = copyOf(c);
    assertEquals(Collections.singleton("a"), set);
  }

  public void testCopyOf_collection_oneElementRepeated() {
    Collection<String> c = MinimalCollection.of("a", "a", "a");
    Set<String> set = copyOf(c);
    assertEquals(Collections.singleton("a"), set);
  }

  public void testCopyOf_collection_general() {
    Collection<String> c = MinimalCollection.of("a", "b", "a");
    Set<String> set = copyOf(c);
    assertEquals(2, set.size());
    assertTrue(set.contains("a"));
    assertTrue(set.contains("b"));
  }

  public void testCopyOf_collectionContainingNull() {
    Collection<String> c = MinimalCollection.of("a", null, "b");
    try {
      copyOf(c);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  enum TestEnum {
    A,
    B,
    C,
    D
  }

  public void testCopyOf_collection_enumSet() {
    Collection<TestEnum> c = EnumSet.of(TestEnum.A, TestEnum.B, TestEnum.D);
    Set<TestEnum> set = copyOf(c);
    assertEquals(3, set.size());
    assertEquals(c, set);
  }

  public void testCopyOf_iterator_empty() {
    Iterator<String> iterator = Iterators.emptyIterator();
    Set<String> set = copyOf(iterator);
    assertEquals(Collections.<String>emptySet(), set);
    assertSame(of(), set);
  }

  public void testCopyOf_iterator_oneElement() {
    Iterator<String> iterator = Iterators.singletonIterator("a");
    Set<String> set = copyOf(iterator);
    assertEquals(Collections.singleton("a"), set);
  }

  public void testCopyOf_iterator_oneElementRepeated() {
    Iterator<String> iterator = Iterators.forArray("a", "a", "a");
    Set<String> set = copyOf(iterator);
    assertEquals(Collections.singleton("a"), set);
  }

  public void testCopyOf_iterator_general() {
    Iterator<String> iterator = Iterators.forArray("a", "b", "a");
    Set<String> set = copyOf(iterator);
    assertEquals(2, set.size());
    assertTrue(set.contains("a"));
    assertTrue(set.contains("b"));
  }

  public void testCopyOf_iteratorContainingNull() {
    Iterator<String> c = Iterators.forArray("a", null, "b");
    try {
      copyOf(c);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  private static class CountingIterable implements Iterable<String> {
    int count = 0;

    @Override
    public Iterator<String> iterator() {
      count++;
      return Iterators.forArray("a", "b", "a");
    }
  }

  public void testCopyOf_plainIterable() {
    CountingIterable iterable = new CountingIterable();
    Set<String> set = copyOf(iterable);
    assertEquals(2, set.size());
    assertTrue(set.contains("a"));
    assertTrue(set.contains("b"));
  }

  public void testCopyOf_plainIterable_iteratesOnce() {
    CountingIterable iterable = new CountingIterable();
    Set<String> unused = copyOf(iterable);
    assertEquals(1, iterable.count);
  }

  public void testCopyOf_shortcut_empty() {
    Collection<String> c = of();
    assertEquals(Collections.<String>emptySet(), copyOf(c));
    assertSame(c, copyOf(c));
  }

  public void testCopyOf_shortcut_singleton() {
    Collection<String> c = of("a");
    assertEquals(Collections.singleton("a"), copyOf(c));
    assertSame(c, copyOf(c));
  }

  public void testCopyOf_shortcut_sameType() {
    Collection<String> c = of("a", "b", "c");
    assertSame(c, copyOf(c));
  }

  public void testToString() {
    Set<String> set = of("a", "b", "c", "d", "e", "f", "g");
    assertEquals("[a, b, c, d, e, f, g]", set.toString());
  }

  @GwtIncompatible // slow (~40s)
  public void testIterator_oneElement() {
    new IteratorTester<String>(
        5, UNMODIFIABLE, Collections.singleton("a"), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<String> newTargetIterator() {
        return of("a").iterator();
      }
    }.test();
  }

  @GwtIncompatible // slow (~30s)
  public void testIterator_general() {
    new IteratorTester<String>(
        5, UNMODIFIABLE, asList("a", "b", "c"), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<String> newTargetIterator() {
        return of("a", "b", "c").iterator();
      }
    }.test();
  }

  public void testContainsAll_sameType() {
    Collection<String> c = of("a", "b", "c");
    assertFalse(c.containsAll(of("a", "b", "c", "d")));
    assertFalse(c.containsAll(of("a", "d")));
    assertTrue(c.containsAll(of("a", "c")));
    assertTrue(c.containsAll(of("a", "b", "c")));
  }

  public void testEquals_sameType() {
    Collection<String> c = of("a", "b", "c");
    assertTrue(c.equals(of("a", "b", "c")));
    assertFalse(c.equals(of("a", "b", "d")));
  }

  abstract <E extends Comparable<E>> ImmutableSet.Builder<E> builder();

  public void testBuilderWithNonDuplicateElements() {
    ImmutableSet<String> set =
        this.<String>builder()
            .add("a")
            .add("b", "c")
            .add("d", "e", "f")
            .add("g", "h", "i", "j")
            .build();
    assertThat(set).containsExactly("a", "b", "c", "d", "e", "f", "g", "h", "i", "j").inOrder();
  }

  public void testReuseBuilderWithNonDuplicateElements() {
    ImmutableSet.Builder<String> builder = this.<String>builder().add("a").add("b");
    assertThat(builder.build()).containsExactly("a", "b").inOrder();
    builder.add("c", "d");
    assertThat(builder.build()).containsExactly("a", "b", "c", "d").inOrder();
  }

  public void testBuilderWithDuplicateElements() {
    ImmutableSet<String> set =
        this.<String>builder()
            .add("a")
            .add("a", "a")
            .add("a", "a", "a")
            .add("a", "a", "a", "a")
            .build();
    assertTrue(set.contains("a"));
    assertFalse(set.contains("b"));
    assertEquals(1, set.size());
  }

  public void testReuseBuilderWithDuplicateElements() {
    ImmutableSet.Builder<String> builder = this.<String>builder().add("a").add("a", "a").add("b");
    assertThat(builder.build()).containsExactly("a", "b").inOrder();
    builder.add("a", "b", "c", "c");
    assertThat(builder.build()).containsExactly("a", "b", "c").inOrder();
  }

  public void testBuilderAddAll() {
    List<String> a = asList("a", "b", "c");
    List<String> b = asList("c", "d", "e");
    ImmutableSet<String> set = this.<String>builder().addAll(a).addAll(b).build();
    assertThat(set).containsExactly("a", "b", "c", "d", "e").inOrder();
  }

  static final int LAST_COLOR_ADDED = 0x00BFFF;

  public void testComplexBuilder() {
    List<Integer> colorElem = asList(0x00, 0x33, 0x66, 0x99, 0xCC, 0xFF);
    // javac won't compile this without "this.<Integer>"
    ImmutableSet.Builder<Integer> webSafeColorsBuilder = this.<Integer>builder();
    for (Integer red : colorElem) {
      for (Integer green : colorElem) {
        for (Integer blue : colorElem) {
          webSafeColorsBuilder.add((red << 16) + (green << 8) + blue);
        }
      }
    }
    ImmutableSet<Integer> webSafeColors = webSafeColorsBuilder.build();
    assertEquals(216, webSafeColors.size());
    Integer[] webSafeColorArray = webSafeColors.toArray(new Integer[webSafeColors.size()]);
    assertEquals(0x000000, (int) webSafeColorArray[0]);
    assertEquals(0x000033, (int) webSafeColorArray[1]);
    assertEquals(0x000066, (int) webSafeColorArray[2]);
    assertEquals(0x003300, (int) webSafeColorArray[6]);
    assertEquals(0x330000, (int) webSafeColorArray[36]);
    ImmutableSet<Integer> addedColor = webSafeColorsBuilder.add(LAST_COLOR_ADDED).build();
    assertEquals(
        "Modifying the builder should not have changed any already built sets",
        216,
        webSafeColors.size());
    assertEquals("the new array should be one bigger than webSafeColors", 217, addedColor.size());
    Integer[] appendColorArray = addedColor.toArray(new Integer[addedColor.size()]);
    assertEquals(getComplexBuilderSetLastElement(), (int) appendColorArray[216]);
  }

  abstract int getComplexBuilderSetLastElement();

  public void testBuilderAddHandlesNullsCorrectly() {
    ImmutableSet.Builder<String> builder = this.<String>builder();
    try {
      builder.add((String) null);
      fail("expected NullPointerException"); // COV_NF_LINE
    } catch (NullPointerException expected) {
    }

    builder = this.<String>builder();
    try {
      builder.add((String[]) null);
      fail("expected NullPointerException"); // COV_NF_LINE
    } catch (NullPointerException expected) {
    }

    builder = this.<String>builder();
    try {
      builder.add("a", (String) null);
      fail("expected NullPointerException"); // COV_NF_LINE
    } catch (NullPointerException expected) {
    }

    builder = this.<String>builder();
    try {
      builder.add("a", "b", (String) null);
      fail("expected NullPointerException"); // COV_NF_LINE
    } catch (NullPointerException expected) {
    }

    builder = this.<String>builder();
    try {
      builder.add("a", "b", "c", null);
      fail("expected NullPointerException"); // COV_NF_LINE
    } catch (NullPointerException expected) {
    }

    builder = this.<String>builder();
    try {
      builder.add("a", "b", null, "c");
      fail("expected NullPointerException"); // COV_NF_LINE
    } catch (NullPointerException expected) {
    }
  }

  public void testBuilderAddAllHandlesNullsCorrectly() {
    ImmutableSet.Builder<String> builder = this.<String>builder();
    try {
      builder.addAll((Iterable<String>) null);
      fail("expected NullPointerException"); // COV_NF_LINE
    } catch (NullPointerException expected) {
    }

    try {
      builder.addAll((Iterator<String>) null);
      fail("expected NullPointerException"); // COV_NF_LINE
    } catch (NullPointerException expected) {
    }

    builder = this.<String>builder();
    List<String> listWithNulls = asList("a", null, "b");
    try {
      builder.addAll(listWithNulls);
      fail("expected NullPointerException"); // COV_NF_LINE
    } catch (NullPointerException expected) {
    }

    Iterable<String> iterableWithNulls = MinimalIterable.of("a", null, "b");
    try {
      builder.addAll(iterableWithNulls);
      fail("expected NullPointerException"); // COV_NF_LINE
    } catch (NullPointerException expected) {
    }
  }

  /**
   * Verify thread safety by using a collection whose size() may be inconsistent with the actual
   * number of elements. Tests using this method might fail in GWT because the GWT emulations might
   * count on size() during copy. It is safe to do so in GWT because javascript is single-threaded.
   */
  // TODO(benyu): turn this into a test once all copyOf(Collection) are
  // thread-safe
  @GwtIncompatible // GWT is single threaded
  void verifyThreadSafe() {
    List<String> sample = Lists.newArrayList("a", "b", "c");
    for (int delta : new int[] {-1, 0, 1}) {
      for (int i = 0; i < sample.size(); i++) {
        Collection<String> misleading = Helpers.misleadingSizeCollection(delta);
        List<String> expected = sample.subList(0, i);
        misleading.addAll(expected);
        assertEquals(
            "delta: " + delta + " sample size: " + i,
            Sets.newHashSet(expected),
            copyOf(misleading));
      }
    }
  }
}

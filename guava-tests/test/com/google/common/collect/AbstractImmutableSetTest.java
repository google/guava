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

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterators.emptyIterator;
import static com.google.common.collect.Iterators.singletonIterator;
import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.testing.IteratorFeature.UNMODIFIABLE;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Strings;
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
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base class for {@link ImmutableSet} and {@link ImmutableSortedSet} tests.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public abstract class AbstractImmutableSetTest extends TestCase {

  protected abstract <E extends Comparable<? super E>> Set<E> of();

  protected abstract <E extends Comparable<? super E>> Set<E> of(E e);

  protected abstract <E extends Comparable<? super E>> Set<E> of(E e1, E e2);

  protected abstract <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3);

  protected abstract <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3, E e4);

  protected abstract <E extends Comparable<? super E>> Set<E> of(E e1, E e2, E e3, E e4, E e5);

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
    assertSame(this.<String>of(), set);
  }

  public void testCreation_oneElement() {
    Set<String> set = of("a");
    assertEquals(singleton("a"), set);
  }

  public void testCreation_twoElements() {
    Set<String> set = of("a", "b");
    assertEquals(newHashSet("a", "b"), set);
  }

  public void testCreation_threeElements() {
    Set<String> set = of("a", "b", "c");
    assertEquals(newHashSet("a", "b", "c"), set);
  }

  public void testCreation_fourElements() {
    Set<String> set = of("a", "b", "c", "d");
    assertEquals(newHashSet("a", "b", "c", "d"), set);
  }

  public void testCreation_fiveElements() {
    Set<String> set = of("a", "b", "c", "d", "e");
    assertEquals(newHashSet("a", "b", "c", "d", "e"), set);
  }

  public void testCreation_sixElements() {
    Set<String> set = of("a", "b", "c", "d", "e", "f");
    assertEquals(newHashSet("a", "b", "c", "d", "e", "f"), set);
  }

  public void testCreation_sevenElements() {
    Set<String> set = of("a", "b", "c", "d", "e", "f", "g");
    assertEquals(newHashSet("a", "b", "c", "d", "e", "f", "g"), set);
  }

  public void testCreation_eightElements() {
    Set<String> set = of("a", "b", "c", "d", "e", "f", "g", "h");
    assertEquals(newHashSet("a", "b", "c", "d", "e", "f", "g", "h"), set);
  }

  public void testCopyOf_emptyArray() {
    String[] array = new String[0];
    Set<String> set = copyOf(array);
    assertEquals(Collections.<String>emptySet(), set);
    assertSame(this.<String>of(), set);
  }

  public void testCopyOf_arrayOfOneElement() {
    String[] array = new String[] {"a"};
    Set<String> set = copyOf(array);
    assertEquals(singleton("a"), set);
  }

  public void testCopyOf_nullArray() {
    assertThrows(NullPointerException.class, () -> copyOf((String[]) null));
  }

  public void testCopyOf_arrayContainingOnlyNull() {
    @Nullable String[] array = new @Nullable String[] {null};
    assertThrows(NullPointerException.class, () -> copyOf((String[]) array));
  }

  public void testCopyOf_collection_empty() {
    // "<String>" is required to work around a javac 1.5 bug.
    Collection<String> c = MinimalCollection.<String>of();
    Set<String> set = copyOf(c);
    assertEquals(Collections.<String>emptySet(), set);
    assertSame(this.<String>of(), set);
  }

  public void testCopyOf_collection_oneElement() {
    Collection<String> c = MinimalCollection.of("a");
    Set<String> set = copyOf(c);
    assertEquals(singleton("a"), set);
  }

  public void testCopyOf_collection_oneElementRepeated() {
    Collection<String> c = MinimalCollection.of("a", "a", "a");
    Set<String> set = copyOf(c);
    assertEquals(singleton("a"), set);
  }

  public void testCopyOf_collection_general() {
    Collection<String> c = MinimalCollection.of("a", "b", "a");
    Set<String> set = copyOf(c);
    assertEquals(2, set.size());
    assertTrue(set.contains("a"));
    assertTrue(set.contains("b"));
  }

  public void testCopyOf_collectionContainingNull() {
    Collection<@Nullable String> c = MinimalCollection.of("a", null, "b");
    assertThrows(NullPointerException.class, () -> copyOf((Collection<String>) c));
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
    Iterator<String> iterator = emptyIterator();
    Set<String> set = copyOf(iterator);
    assertEquals(Collections.<String>emptySet(), set);
    assertSame(this.<String>of(), set);
  }

  public void testCopyOf_iterator_oneElement() {
    Iterator<String> iterator = singletonIterator("a");
    Set<String> set = copyOf(iterator);
    assertEquals(singleton("a"), set);
  }

  public void testCopyOf_iterator_oneElementRepeated() {
    Iterator<String> iterator = Iterators.forArray("a", "a", "a");
    Set<String> set = copyOf(iterator);
    assertEquals(singleton("a"), set);
  }

  public void testCopyOf_iterator_general() {
    Iterator<String> iterator = Iterators.forArray("a", "b", "a");
    Set<String> set = copyOf(iterator);
    assertEquals(2, set.size());
    assertTrue(set.contains("a"));
    assertTrue(set.contains("b"));
  }

  public void testCopyOf_iteratorContainingNull() {
    Iterator<@Nullable String> c = Iterators.forArray("a", null, "b");
    assertThrows(NullPointerException.class, () -> copyOf((Iterator<String>) c));
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
    assertEquals(singleton("a"), copyOf(c));
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
        5, UNMODIFIABLE, singleton("a"), IteratorTester.KnownOrder.KNOWN_ORDER) {
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
    {
    ImmutableSet.Builder<String> builder = this.<String>builder();
      assertThrows(NullPointerException.class, () -> builder.add((String) null));
    }

    {
      ImmutableSet.Builder<String> builder = this.<String>builder();
      assertThrows(NullPointerException.class, () -> builder.add((String[]) null));
    }

    {
      ImmutableSet.Builder<String> builder = this.<String>builder();
      assertThrows(NullPointerException.class, () -> builder.add("a", (String) null));
    }

    {
      ImmutableSet.Builder<String> builder = this.<String>builder();
      assertThrows(NullPointerException.class, () -> builder.add("a", "b", (String) null));
    }

    {
      ImmutableSet.Builder<String> builder = this.<String>builder();
      assertThrows(NullPointerException.class, () -> builder.add("a", "b", "c", null));
    }

    {
      ImmutableSet.Builder<String> builder = this.<String>builder();
      assertThrows(NullPointerException.class, () -> builder.add("a", "b", null, "c"));
    }
  }

  public void testBuilderAddAllHandlesNullsCorrectly() {
    {
    ImmutableSet.Builder<String> builder = this.<String>builder();
      assertThrows(NullPointerException.class, () -> builder.addAll((Iterable<String>) null));
    }

    {
      ImmutableSet.Builder<String> builder = this.<String>builder();
      assertThrows(NullPointerException.class, () -> builder.addAll((Iterator<String>) null));
    }

    {
      ImmutableSet.Builder<String> builder = this.<String>builder();
    List<@Nullable String> listWithNulls = asList("a", null, "b");
      assertThrows(NullPointerException.class, () -> builder.addAll((List<String>) listWithNulls));
    }

    {
      ImmutableSet.Builder<String> builder = this.<String>builder();
    Iterable<@Nullable String> iterableWithNulls = MinimalIterable.of("a", null, "b");
      assertThrows(
          NullPointerException.class, () -> builder.addAll((Iterable<String>) iterableWithNulls));
    }
  }

  /**
   * Verify thread safety by using a collection whose size() may be inconsistent with the actual
   * number of elements and whose elements may change over time.
   *
   * <p>This test might fail in GWT because the GWT emulations might count on the input collection
   * not to change during the copy. It is safe to do so in GWT because javascript is
   * single-threaded.
   */
  @GwtIncompatible // GWT is single threaded
  public void testCopyOf_threadSafe() {
    /*
     * The actual collections that we pass as inputs will be wrappers around these, so
     * ImmutableSet.copyOf won't short-circuit because it won't see an ImmutableSet input.
     */
    ImmutableList<ImmutableSet<String>> distinctCandidatesByAscendingSize =
        ImmutableList.of(
            ImmutableSet.of(),
            ImmutableSet.of("a"),
            ImmutableSet.of("b", "a"),
            ImmutableSet.of("c", "b", "a"),
            ImmutableSet.of("d", "c", "b", "a"));
    for (boolean byAscendingSize : new boolean[] {true, false}) {
      Iterable<ImmutableSet<String>> infiniteSets =
          Iterables.cycle(
              byAscendingSize
                  ? distinctCandidatesByAscendingSize
                  : Lists.reverse(distinctCandidatesByAscendingSize));
      for (int startIndex = 0;
          startIndex < distinctCandidatesByAscendingSize.size();
          startIndex++) {
        Iterable<ImmutableSet<String>> infiniteSetsFromStartIndex =
            Iterables.skip(infiniteSets, startIndex);
        for (boolean inputIsSet : new boolean[] {true, false}) {
          Collection<String> input =
              inputIsSet
                  ? new MutatedOnQuerySet<>(infiniteSetsFromStartIndex)
                  : new MutatedOnQueryList<>(
                      transform(infiniteSetsFromStartIndex, ImmutableList::copyOf));
          Set<String> immutableCopy;
          try {
            immutableCopy = copyOf(input);
          } catch (RuntimeException e) {
            throw new RuntimeException(
                Strings.lenientFormat(
                    "byAscendingSize %s, startIndex %s, inputIsSet %s",
                    byAscendingSize, startIndex, inputIsSet),
                e);
          }
          /*
           * TODO(cpovirk): Check that the values match one of candidates that
           * MutatedOnQuery*.delegate() actually returned during this test?
           */
          assertWithMessage(
                  "byAscendingSize %s, startIndex %s, inputIsSet %s",
                  byAscendingSize, startIndex, inputIsSet)
              .that(immutableCopy)
              .isIn(distinctCandidatesByAscendingSize);
        }
      }
    }
  }

  private static final class MutatedOnQuerySet<E> extends ForwardingSet<E> {
    final Iterator<ImmutableSet<E>> infiniteCandidates;

    MutatedOnQuerySet(Iterable<ImmutableSet<E>> infiniteCandidates) {
      this.infiniteCandidates = infiniteCandidates.iterator();
    }

    @Override
    protected Set<E> delegate() {
      return infiniteCandidates.next();
    }
  }

  private static final class MutatedOnQueryList<E> extends ForwardingList<E> {
    final Iterator<ImmutableList<E>> infiniteCandidates;

    MutatedOnQueryList(Iterable<ImmutableList<E>> infiniteCandidates) {
      this.infiniteCandidates = infiniteCandidates.iterator();
    }

    @Override
    protected List<E> delegate() {
      return infiniteCandidates.next();
    }
  }
}

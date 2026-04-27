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

package com.google.common.base;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.testing.NullPointerTester;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Unit test for {@link Joiner}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
@NullMarked
public class JoinerTest extends TestCase {
  private static final Joiner J = Joiner.on("-");

  // <Integer> needed to prevent warning :(
  private static final Iterable<Integer> iterable = Arrays.asList();
  private static final Iterable<Integer> iterable1 = asList(1);
  private static final Iterable<Integer> iterable12 = asList(1, 2);
  private static final Iterable<Integer> iterable123 = asList(1, 2, 3);
  private static final Iterable<@Nullable Integer> iterableNull = asList((Integer) null);
  private static final Iterable<@Nullable Integer> iterableNullNull = asList((Integer) null, null);
  private static final Iterable<@Nullable Integer> iterableNull1 = asList(null, 1);
  private static final Iterable<@Nullable Integer> iterable1Null = asList(1, null);
  private static final Iterable<@Nullable Integer> iterable1Null2 = asList(1, null, 2);
  private static final Iterable<@Nullable Integer> iterableFourNulls =
      asList((Integer) null, null, null, null);

  /*
   * Both of these fields *are* immutable/constant. They don't use the type ImmutableList because
   * they need to behave slightly differently.
   */
  @SuppressWarnings("ConstantCaseForConstants")
  private static final List<Integer> UNDERREPORTING_SIZE_LIST;

  @SuppressWarnings("ConstantCaseForConstants")
  private static final List<Integer> OVERREPORTING_SIZE_LIST;

  static {
    List<Integer> collection123 = asList(1, 2, 3);
    UNDERREPORTING_SIZE_LIST = unmodifiableList(new MisleadingSizeList<>(collection123, -1));
    OVERREPORTING_SIZE_LIST = unmodifiableList(new MisleadingSizeList<>(collection123, 1));
  }

  /*
   * c.g.c.collect.testing.Helpers.misleadingSizeList has a broken Iterator, so we can't use it. (I
   * mean, ideally we'd fix it....) Also, we specifically need a List so that we trigger the fast
   * path in join(Iterable).
   */
  private static final class MisleadingSizeList<E extends @Nullable Object>
      extends ForwardingList<E> {
    final List<E> delegate;
    final int delta;

    MisleadingSizeList(List<E> delegate, int delta) {
      this.delegate = delegate;
      this.delta = delta;
    }

    @Override
    protected List<E> delegate() {
      return delegate;
    }

    @Override
    public int size() {
      return delegate.size() + delta;
    }
  }

  @SuppressWarnings("JoinIterableIterator") // explicitly testing iterator overload, too
  public void testNoSpecialNullBehavior() {
    checkNoOutput(J, iterable);
    checkResult(J, iterable1, "1");
    checkResult(J, iterable12, "1-2");
    checkResult(J, iterable123, "1-2-3");
    checkResult(J, UNDERREPORTING_SIZE_LIST, "1-2-3");
    checkResult(J, OVERREPORTING_SIZE_LIST, "1-2-3");

    assertThrows(NullPointerException.class, () -> J.join(iterableNull));
    assertThrows(NullPointerException.class, () -> J.join(iterable1Null2));

    assertThrows(NullPointerException.class, () -> J.join(iterableNull.iterator()));
    assertThrows(NullPointerException.class, () -> J.join(iterable1Null2.iterator()));
  }

  public void testOnCharOverride() {
    Joiner onChar = Joiner.on('-');
    checkNoOutput(onChar, iterable);
    checkResult(onChar, iterable1, "1");
    checkResult(onChar, iterable12, "1-2");
    checkResult(onChar, iterable123, "1-2-3");
    checkResult(J, UNDERREPORTING_SIZE_LIST, "1-2-3");
    checkResult(J, OVERREPORTING_SIZE_LIST, "1-2-3");
  }

  public void testSkipNulls() {
    Joiner skipNulls = J.skipNulls();
    checkNoOutput(skipNulls, iterable);
    checkNoOutput(skipNulls, iterableNull);
    checkNoOutput(skipNulls, iterableNullNull);
    checkNoOutput(skipNulls, iterableFourNulls);
    checkResult(skipNulls, iterable1, "1");
    checkResult(skipNulls, iterable12, "1-2");
    checkResult(skipNulls, iterable123, "1-2-3");
    checkResult(J, UNDERREPORTING_SIZE_LIST, "1-2-3");
    checkResult(J, OVERREPORTING_SIZE_LIST, "1-2-3");
    checkResult(skipNulls, iterableNull1, "1");
    checkResult(skipNulls, iterable1Null, "1");
    checkResult(skipNulls, iterable1Null2, "1-2");
  }

  public void testUseForNull() {
    Joiner zeroForNull = J.useForNull("0");
    checkNoOutput(zeroForNull, iterable);
    checkResult(zeroForNull, iterable1, "1");
    checkResult(zeroForNull, iterable12, "1-2");
    checkResult(zeroForNull, iterable123, "1-2-3");
    checkResult(J, UNDERREPORTING_SIZE_LIST, "1-2-3");
    checkResult(J, OVERREPORTING_SIZE_LIST, "1-2-3");
    checkResult(zeroForNull, iterableNull, "0");
    checkResult(zeroForNull, iterableNullNull, "0-0");
    checkResult(zeroForNull, iterableNull1, "0-1");
    checkResult(zeroForNull, iterable1Null, "1-0");
    checkResult(zeroForNull, iterable1Null2, "1-0-2");
    checkResult(zeroForNull, iterableFourNulls, "0-0-0-0");
  }

  @SuppressWarnings("AppendIterableIterator") // We intentionally test the Iterator overload.
  private static void checkNoOutput(Joiner joiner, Iterable<? extends @Nullable Integer> set) {
    assertThat(joiner.join(set)).isEqualTo("");
    assertThat(joiner.join(set.iterator())).isEqualTo("");

    @Nullable Integer[] array = newArrayList(set).toArray(new @Nullable Integer[0]);
    assertThat(joiner.join(array)).isEqualTo("");

    StringBuilder sb1FromIterable = new StringBuilder();
    assertThat(joiner.appendTo(sb1FromIterable, set)).isSameInstanceAs(sb1FromIterable);
    assertEquals(0, sb1FromIterable.length());

    StringBuilder sb1FromIterator = new StringBuilder();
    assertThat(joiner.appendTo(sb1FromIterator, set)).isSameInstanceAs(sb1FromIterator);
    assertEquals(0, sb1FromIterator.length());

    StringBuilder sb2 = new StringBuilder();
    assertThat(joiner.appendTo(sb2, array)).isSameInstanceAs(sb2);
    assertEquals(0, sb2.length());

    try {
      joiner.appendTo(NASTY_APPENDABLE, set);
    } catch (IOException e) {
      throw new AssertionError(e);
    }

    try {
      joiner.appendTo(NASTY_APPENDABLE, set.iterator());
    } catch (IOException e) {
      throw new AssertionError(e);
    }

    try {
      joiner.appendTo(NASTY_APPENDABLE, array);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private static final Appendable NASTY_APPENDABLE =
      new Appendable() {
        @Override
        public Appendable append(@Nullable CharSequence csq) throws IOException {
          throw new IOException();
        }

        @Override
        public Appendable append(@Nullable CharSequence csq, int start, int end)
            throws IOException {
          throw new IOException();
        }

        @Override
        public Appendable append(char c) throws IOException {
          throw new IOException();
        }
      };

  private static void checkResult(
      Joiner joiner, Iterable<? extends @Nullable Integer> parts, String expected) {
    assertThat(joiner.join(parts)).isEqualTo(expected);
    assertThat(joiner.join(parts.iterator())).isEqualTo(expected);

    StringBuilder sb1FromIterable = new StringBuilder().append('x');
    joiner.appendTo(sb1FromIterable, parts);
    assertThat(sb1FromIterable.toString()).isEqualTo("x" + expected);

    StringBuilder sb1FromIterator = new StringBuilder().append('x');
    joiner.appendTo(sb1FromIterator, parts.iterator());
    assertThat(sb1FromIterator.toString()).isEqualTo("x" + expected);

    // The use of iterator() works around J2KT b/381065164.
    @Nullable Integer[] partsArray =
        newArrayList(parts.iterator()).toArray(new @Nullable Integer[0]);
    assertThat(joiner.join(partsArray)).isEqualTo(expected);

    StringBuilder sb2 = new StringBuilder().append('x');
    joiner.appendTo(sb2, partsArray);
    assertThat(sb2.toString()).isEqualTo("x" + expected);

    int num = partsArray.length - 2;
    if (num >= 0) {
      @Nullable Object[] rest = new @Nullable Object[num];
      for (int i = 0; i < num; i++) {
        rest[i] = partsArray[i + 2];
      }

      assertThat(joiner.join(partsArray[0], partsArray[1], rest)).isEqualTo(expected);

      StringBuilder sb3 = new StringBuilder().append('x');
      joiner.appendTo(sb3, partsArray[0], partsArray[1], rest);
      assertThat(sb3.toString()).isEqualTo("x" + expected);
    }
  }

  public void test_useForNull_skipNulls() {
    Joiner j = Joiner.on("x").useForNull("y");
    assertThrows(UnsupportedOperationException.class, j::skipNulls);
  }

  public void test_skipNulls_useForNull() {
    Joiner j = Joiner.on("x").skipNulls();
    assertThrows(UnsupportedOperationException.class, () -> j.useForNull("y"));
  }

  public void test_useForNull_twice() {
    Joiner j = Joiner.on("x").useForNull("y");
    assertThrows(UnsupportedOperationException.class, () -> j.useForNull("y"));
  }

  public void testMap() {
    MapJoiner j = Joiner.on(';').withKeyValueSeparator(':');
    assertThat(j.join(ImmutableMap.of())).isEqualTo("");
    assertThat(j.join(ImmutableMap.of("", ""))).isEqualTo(":");

    Map<@Nullable String, @Nullable String> mapWithNulls = new LinkedHashMap<>();
    mapWithNulls.put("a", null);
    mapWithNulls.put(null, "b");

    assertThrows(NullPointerException.class, () -> j.join(mapWithNulls));

    assertThat(j.useForNull("00").join(mapWithNulls)).isEqualTo("a:00;00:b");

    StringBuilder sb = new StringBuilder();
    j.appendTo(sb, ImmutableMap.of(1, 2, 3, 4, 5, 6));
    assertThat(sb.toString()).isEqualTo("1:2;3:4;5:6");
  }

  public void testEntries() {
    MapJoiner j = Joiner.on(";").withKeyValueSeparator(":");
    assertThat(j.join(ImmutableMultimap.of().entries())).isEqualTo("");
    assertThat(j.join(ImmutableMultimap.of().entries().iterator())).isEqualTo("");
    assertThat(j.join(ImmutableMultimap.of("", "").entries())).isEqualTo(":");
    assertThat(j.join(ImmutableMultimap.of("", "").entries().iterator())).isEqualTo(":");
    assertThat(j.join(ImmutableMultimap.of("1", "a", "1", "b").entries())).isEqualTo("1:a;1:b");
    assertThat(j.join(ImmutableMultimap.of("1", "a", "1", "b").entries().iterator()))
        .isEqualTo("1:a;1:b");

    Map<@Nullable String, @Nullable String> mapWithNulls = new LinkedHashMap<>();
    mapWithNulls.put("a", null);
    mapWithNulls.put(null, "b");
    Set<Entry<@Nullable String, @Nullable String>> entriesWithNulls = mapWithNulls.entrySet();

    assertThrows(NullPointerException.class, () -> j.join(entriesWithNulls));

    assertThrows(NullPointerException.class, () -> j.join(entriesWithNulls.iterator()));

    assertThat(j.useForNull("00").join(entriesWithNulls)).isEqualTo("a:00;00:b");
    assertThat(j.useForNull("00").join(entriesWithNulls.iterator())).isEqualTo("a:00;00:b");

    StringBuilder sb1 = new StringBuilder();
    j.appendTo(sb1, ImmutableMultimap.of(1, 2, 3, 4, 5, 6, 1, 3, 5, 10).entries());
    assertThat(sb1.toString()).isEqualTo("1:2;1:3;3:4;5:6;5:10");

    StringBuilder sb2 = new StringBuilder();
    j.appendTo(sb2, ImmutableMultimap.of(1, 2, 3, 4, 5, 6, 1, 3, 5, 10).entries().iterator());
    assertThat(sb2.toString()).isEqualTo("1:2;1:3;3:4;5:6;5:10");
  }

  public void test_skipNulls_onMap() {
    Joiner j = Joiner.on(",").skipNulls();
    assertThrows(UnsupportedOperationException.class, () -> j.withKeyValueSeparator("/"));
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Joiner.class);
    tester.testInstanceMethods(Joiner.on(","), NullPointerTester.Visibility.PACKAGE);
    tester.testInstanceMethods(Joiner.on(",").skipNulls(), NullPointerTester.Visibility.PACKAGE);
    tester.testInstanceMethods(
        Joiner.on(",").useForNull("x"), NullPointerTester.Visibility.PACKAGE);
    tester.testInstanceMethods(
        Joiner.on(",").withKeyValueSeparator("="), NullPointerTester.Visibility.PACKAGE);
  }
}

/*
 * Copyright (C) 2026 The Guava Authors
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

import static com.google.common.collect.MoreGatherers.distinctBy;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtIncompatible;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

/// Tests for `MoreGatherers`.
@GwtIncompatible
@NullMarked
public class MoreGatherersTest {

  @Test
  public void testDistinctBy_example() {
    ImmutableList<String> input = ImmutableList.of("foo", "bar", "quux", "womble");
    List<String> output = input.stream().gather(distinctBy(String::length)).toList();
    assertThat(output).containsExactly("foo", "quux", "womble").inOrder();
  }

  @Test
  public void testDistinctBy_empty() {
    List<String> output = Stream.<String>empty().gather(distinctBy(String::length)).toList();
    assertThat(output).isEmpty();
  }

  @Test
  public void testDistinctBy_allDistinct() {
    ImmutableList<String> input = ImmutableList.of("a", "bb", "ccc", "dddd");
    List<String> output = input.stream().gather(distinctBy(String::length)).toList();
    assertThat(output).containsExactly("a", "bb", "ccc", "dddd").inOrder();
  }

  @Test
  public void testDistinctBy_allSame() {
    ImmutableList<String> input = ImmutableList.of("a", "b", "c", "d");
    List<String> output = input.stream().gather(distinctBy(String::length)).toList();
    assertThat(output).containsExactly("a");
  }

  @Test
  public void testDistinctBy_identity() {
    ImmutableList<String> input = ImmutableList.of("a", "b", "a", "c", "b");
    List<String> output = input.stream().gather(distinctBy(Function.identity())).toList();
    assertThat(output).containsExactly("a", "b", "c").inOrder();
  }

  @Test
  public void testDistinctBy_nullKeys() {
    ImmutableList<String> input = ImmutableList.of("a", "b", "c");
    List<String> output = input.stream().gather(distinctBy(s -> null)).toList();
    assertThat(output).containsExactly("a");
  }

  @Test
  public void testDistinctBy_customObjects() {
    record Item(int key, String value) {}
    ImmutableList<Item> input =
        ImmutableList.of(
            new Item(1, "one-a"),
            new Item(2, "two-a"),
            new Item(1, "one-b"),
            new Item(3, "three-a"),
            new Item(2, "two-b"));
    List<Item> output = input.stream().gather(distinctBy(Item::key)).toList();
    assertThat(output)
        .containsExactly(new Item(1, "one-a"), new Item(2, "two-a"), new Item(3, "three-a"))
        .inOrder();
  }

  @Test
  public void testDistinctBy_shortCircuit() {
    AtomicInteger seenCount = new AtomicInteger();
    ImmutableList<String> input = ImmutableList.of("a", "bb", "ccc", "dddd", "eeeee");
    List<String> output =
        input.stream()
            .peek(e -> seenCount.incrementAndGet())
            .gather(distinctBy(String::length))
            .limit(2)
            .toList();
    assertThat(output).containsExactly("a", "bb").inOrder();
    assertThat(seenCount.get()).isEqualTo(2);
  }

  @Test
  public void testDistinctBy_superTypeFunction() {
    ImmutableList<String> input = ImmutableList.of("foo", "bar", "quux", "womble");
    List<String> output = input.stream().gather(distinctBy(CharSequence::length)).toList();
    assertThat(output).containsExactly("foo", "quux", "womble").inOrder();
  }

  @Test
  public void testDistinctBy_nullElements() {
    List<@Nullable String> input = Arrays.asList("foo", null, "bar", null, "quux");
    List<@Nullable String> output =
        input.stream().gather(distinctBy(s -> s == null ? 0 : s.length())).toList();
    assertThat(output).containsExactly("foo", null, "quux").inOrder();
  }
}

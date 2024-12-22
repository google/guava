/*
 * Copyright (C) 2016 The Guava Authors
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

import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.common.collect.MoreCollectors.toOptional;
import static com.google.common.collect.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import junit.framework.TestCase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests for {@code MoreCollectors}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@NullMarked
public class MoreCollectorsTest extends TestCase {
  public void testToOptionalEmpty() {
    assertThat(Stream.empty().collect(toOptional())).isEmpty();
  }

  public void testToOptionalSingleton() {
    assertThat(Stream.of(1).collect(toOptional())).hasValue(1);
  }

  public void testToOptionalNull() {
    Stream<@Nullable Object> stream = Stream.of((Object) null);
    assertThrows(NullPointerException.class, () -> stream.collect(toOptional()));
  }

  public void testToOptionalMultiple() {
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2).collect(toOptional()));
    assertThat(expected).hasMessageThat().contains("1, 2");
  }

  public void testToOptionalMultipleWithNull() {
    assertThrows(NullPointerException.class, () -> Stream.of(1, null).collect(toOptional()));
  }

  public void testToOptionalMany() {
    IllegalArgumentException expected =
        assertThrows(
            IllegalArgumentException.class,
            () -> Stream.of(1, 2, 3, 4, 5, 6).collect(toOptional()));
    assertThat(expected).hasMessageThat().contains("1, 2, 3, 4, 5, ...");
  }

  public void testOnlyElement() {
    assertThrows(NoSuchElementException.class, () -> Stream.empty().collect(onlyElement()));
  }

  public void testOnlyElementSingleton() {
    assertThat(Stream.of(1).collect(onlyElement())).isEqualTo(1);
  }

  public void testOnlyElementNull() {
    assertThat(Stream.<@Nullable Object>of((Object) null).collect(onlyElement())).isNull();
  }

  public void testOnlyElementMultiple() {
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2).collect(onlyElement()));
    assertThat(expected).hasMessageThat().contains("1, 2");
  }

  public void testOnlyElementMany() {
    IllegalArgumentException expected =
        assertThrows(
            IllegalArgumentException.class,
            () -> Stream.of(1, 2, 3, 4, 5, 6).collect(onlyElement()));
    assertThat(expected).hasMessageThat().contains("1, 2, 3, 4, 5, ...");
  }
}

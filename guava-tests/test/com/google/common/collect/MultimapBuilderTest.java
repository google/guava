/*
 * Copyright (C) 2013 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.MultimapBuilder.MultimapBuilderWithKeys;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.RoundingMode;
import java.util.SortedMap;
import java.util.SortedSet;
import junit.framework.TestCase;

/**
 * Tests for {@link MultimapBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
public class MultimapBuilderTest extends TestCase {

  @GwtIncompatible // doesn't build without explicit type parameters on build() methods
  public void testGenerics() {
    ListMultimap<String, Integer> a = MultimapBuilder.hashKeys().arrayListValues().build();
    SortedSetMultimap<String, Integer> b = MultimapBuilder.linkedHashKeys().treeSetValues().build();
    SetMultimap<String, Integer> c =
        MultimapBuilder.treeKeys(String.CASE_INSENSITIVE_ORDER).hashSetValues().build();
  }

  public void testGenerics_gwtCompatible() {
    ListMultimap<String, Integer> a =
        MultimapBuilder.hashKeys().arrayListValues().<String, Integer>build();
    SortedSetMultimap<String, Integer> b =
        MultimapBuilder.linkedHashKeys().treeSetValues().<String, Integer>build();
    SetMultimap<String, Integer> c =
        MultimapBuilder.treeKeys(String.CASE_INSENSITIVE_ORDER)
            .hashSetValues()
            .<String, Integer>build();
  }

  @GwtIncompatible // doesn't build without explicit type parameters on build() methods
  public void testTreeKeys() {
    ListMultimap<String, Integer> multimap = MultimapBuilder.treeKeys().arrayListValues().build();
    assertTrue(multimap.keySet() instanceof SortedSet);
    assertTrue(multimap.asMap() instanceof SortedMap);
  }

  public void testTreeKeys_gwtCompatible() {
    ListMultimap<String, Integer> multimap =
        MultimapBuilder.treeKeys().arrayListValues().<String, Integer>build();
    assertTrue(multimap.keySet() instanceof SortedSet);
    assertTrue(multimap.asMap() instanceof SortedMap);
  }

  @GwtIncompatible // serialization
  public void testSerialization() throws Exception {
    for (MultimapBuilderWithKeys<?> builderWithKeys :
        ImmutableList.of(
            MultimapBuilder.hashKeys(),
            MultimapBuilder.linkedHashKeys(),
            MultimapBuilder.treeKeys(),
            MultimapBuilder.enumKeys(RoundingMode.class))) {
      for (MultimapBuilder<?, ?> builder :
          ImmutableList.of(
              builderWithKeys.arrayListValues(),
              builderWithKeys.linkedListValues(),
              builderWithKeys.hashSetValues(),
              builderWithKeys.linkedHashSetValues(),
              builderWithKeys.treeSetValues(),
              builderWithKeys.enumSetValues(RoundingMode.class))) {
        /*
         * Temporarily inlining SerializableTester here for obscure internal reasons.
         */
        reserializeAndAssert(builder.build());
      }
    }
  }

  @GwtIncompatible // serialization
  private static void reserializeAndAssert(Object object) throws Exception {
    Object copy = reserialize(object);
    assertEquals(object, copy);
    assertEquals(object.getClass(), copy.getClass());
  }

  @GwtIncompatible // serialization
  private static Object reserialize(Object object) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    new ObjectOutputStream(bytes).writeObject(object);
    return new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
  }
}

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

import static java.util.Arrays.asList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.AnEnum;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.MultisetFeature;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestEnumMultisetGenerator;
import com.google.common.testing.ClassSanityTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for an {@link EnumMultiset}.
 *
 * @author Jared Levy
 */
@GwtCompatible(emulated = true)
public class EnumMultisetTest extends TestCase {

  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(
        MultisetTestSuiteBuilder.using(enumMultisetGenerator())
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.GENERAL_PURPOSE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.ALLOWS_NULL_QUERIES,
                MultisetFeature.ENTRIES_ARE_VIEWS)
            .named("EnumMultiset")
            .createTestSuite());
    suite.addTestSuite(EnumMultisetTest.class);
    return suite;
  }

  private static TestEnumMultisetGenerator enumMultisetGenerator() {
    return new TestEnumMultisetGenerator() {
      @Override
      protected Multiset<AnEnum> create(AnEnum[] elements) {
        return (elements.length == 0)
            ? EnumMultiset.create(AnEnum.class)
            : EnumMultiset.create(asList(elements));
      }
    };
  }

  private enum Color {
    BLUE,
    RED,
    YELLOW,
    GREEN,
    WHITE
  }

  private enum Gender {
    MALE,
    FEMALE
  }

  public void testClassCreate() {
    Multiset<Color> ms = EnumMultiset.create(Color.class);
    ms.add(Color.RED);
    ms.add(Color.YELLOW);
    ms.add(Color.RED);
    assertEquals(0, ms.count(Color.BLUE));
    assertEquals(1, ms.count(Color.YELLOW));
    assertEquals(2, ms.count(Color.RED));
  }

  public void testCollectionCreate() {
    Multiset<Color> ms = EnumMultiset.create(asList(Color.RED, Color.YELLOW, Color.RED));
    assertEquals(0, ms.count(Color.BLUE));
    assertEquals(1, ms.count(Color.YELLOW));
    assertEquals(2, ms.count(Color.RED));
  }

  public void testIllegalCreate() {
    Collection<Color> empty = EnumSet.noneOf(Color.class);
    try {
      EnumMultiset.create(empty);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testCreateEmptyWithClass() {
    Multiset<Color> ms = EnumMultiset.create(ImmutableList.<Color>of(), Color.class);
    ms.add(Color.RED);
  }

  public void testCreateEmptyWithoutClassFails() {
    try {
      EnumMultiset.create(ImmutableList.<Color>of());
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testToString() {
    Multiset<Color> ms = EnumMultiset.create(Color.class);
    ms.add(Color.BLUE, 3);
    ms.add(Color.YELLOW, 1);
    ms.add(Color.RED, 2);
    assertEquals("[BLUE x 3, RED x 2, YELLOW]", ms.toString());
  }

  @GwtIncompatible // SerializableTester
  public void testSerializable() {
    Multiset<Color> ms = EnumMultiset.create(asList(Color.RED, Color.YELLOW, Color.RED));
    assertEquals(ms, SerializableTester.reserialize(ms));
  }

  public void testEntrySet() {
    Multiset<Color> ms = EnumMultiset.create(Color.class);
    ms.add(Color.BLUE, 3);
    ms.add(Color.YELLOW, 1);
    ms.add(Color.RED, 2);

    Set<Object> uniqueEntries = Sets.newIdentityHashSet();
    uniqueEntries.addAll(ms.entrySet());
    assertEquals(3, uniqueEntries.size());
  }

  // Wrapper of EnumMultiset factory methods, because we need to skip create(Class).
  // create(Enum1.class) is equal to create(Enum2.class) but testEquals() expects otherwise.
  // For the same reason, we need to skip create(Iterable, Class).
  private static class EnumMultisetFactory {
    public static <E extends Enum<E>> EnumMultiset<E> create(Iterable<E> elements) {
      return EnumMultiset.create(elements);
    }
  }

  @GwtIncompatible // reflection
  public void testEquals() throws Exception {
    new ClassSanityTester()
        .setDistinctValues(Class.class, Color.class, Gender.class)
        .setDistinctValues(Enum.class, Color.BLUE, Color.RED)
        .forAllPublicStaticMethods(EnumMultisetFactory.class)
        .testEquals();
  }

  @GwtIncompatible // reflection
  public void testNulls() throws Exception {
    new NullPointerTester()
        .setDefault(Class.class, Color.class)
        .setDefault(Iterable.class, EnumSet.allOf(Color.class))
        .testAllPublicStaticMethods(EnumMultiset.class);
  }
}

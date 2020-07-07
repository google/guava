/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.testing;

import com.google.common.base.CharMatcher;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Ticker;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.collect.TreeMultiset;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.google.common.reflect.TypeToken;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import junit.framework.TestCase;

/**
 * Tests for {@link FreshValueGenerator}.
 *
 * @author Ben Yu
 */
public class FreshValueGeneratorTest extends TestCase {

  @AndroidIncompatible // problem with equality of Type objects?
  public void testFreshInstance() {
    assertFreshInstances(
        String.class,
        CharSequence.class,
        Appendable.class,
        StringBuffer.class,
        StringBuilder.class,
        Pattern.class,
        MatchResult.class,
        Number.class,
        int.class,
        Integer.class,
        long.class,
        Long.class,
        short.class,
        Short.class,
        byte.class,
        Byte.class,
        boolean.class,
        Boolean.class,
        char.class,
        Character.class,
        int[].class,
        Object[].class,
        UnsignedInteger.class,
        UnsignedLong.class,
        BigInteger.class,
        BigDecimal.class,
        Throwable.class,
        Error.class,
        Exception.class,
        RuntimeException.class,
        Charset.class,
        Locale.class,
        Currency.class,
        List.class,
        Entry.class,
        Object.class,
        Equivalence.class,
        Predicate.class,
        Function.class,
        Comparable.class,
        Comparator.class,
        Ordering.class,
        Class.class,
        Type.class,
        TypeToken.class,
        TimeUnit.class,
        Ticker.class,
        Joiner.class,
        Splitter.class,
        CharMatcher.class,
        InputStream.class,
        ByteArrayInputStream.class,
        Reader.class,
        Readable.class,
        StringReader.class,
        OutputStream.class,
        ByteArrayOutputStream.class,
        Writer.class,
        StringWriter.class,
        File.class,
        Buffer.class,
        ByteBuffer.class,
        CharBuffer.class,
        ShortBuffer.class,
        IntBuffer.class,
        LongBuffer.class,
        FloatBuffer.class,
        DoubleBuffer.class,
        String[].class,
        Object[].class,
        int[].class);
  }

  public void testStringArray() {
    FreshValueGenerator generator = new FreshValueGenerator();
    String[] a1 = generator.generateFresh(String[].class);
    String[] a2 = generator.generateFresh(String[].class);
    assertFalse(a1[0].equals(a2[0]));
  }

  public void testPrimitiveArray() {
    FreshValueGenerator generator = new FreshValueGenerator();
    int[] a1 = generator.generateFresh(int[].class);
    int[] a2 = generator.generateFresh(int[].class);
    assertTrue(a1[0] != a2[0]);
  }

  public void testRange() {
    assertFreshInstance(new TypeToken<Range<String>>() {});
  }

  public void testImmutableList() {
    assertFreshInstance(new TypeToken<ImmutableList<String>>() {});
  }

  public void testImmutableSet() {
    assertFreshInstance(new TypeToken<ImmutableSet<String>>() {});
  }

  public void testImmutableSortedSet() {
    assertFreshInstance(new TypeToken<ImmutableSortedSet<String>>() {});
  }

  public void testImmutableMultiset() {
    assertFreshInstance(new TypeToken<ImmutableSortedSet<String>>() {});
    assertNotInstantiable(new TypeToken<ImmutableMultiset<EmptyEnum>>() {});
  }

  public void testImmutableCollection() {
    assertFreshInstance(new TypeToken<ImmutableCollection<String>>() {});
    assertNotInstantiable(new TypeToken<ImmutableCollection<EmptyEnum>>() {});
  }

  public void testImmutableMap() {
    assertFreshInstance(new TypeToken<ImmutableMap<String, Integer>>() {});
  }

  public void testImmutableSortedMap() {
    assertFreshInstance(new TypeToken<ImmutableSortedMap<String, Integer>>() {});
  }

  public void testImmutableMultimap() {
    assertFreshInstance(new TypeToken<ImmutableMultimap<String, Integer>>() {});
    assertNotInstantiable(new TypeToken<ImmutableMultimap<EmptyEnum, String>>() {});
  }

  public void testImmutableListMultimap() {
    assertFreshInstance(new TypeToken<ImmutableListMultimap<String, Integer>>() {});
  }

  public void testImmutableSetMultimap() {
    assertFreshInstance(new TypeToken<ImmutableSetMultimap<String, Integer>>() {});
  }

  public void testImmutableBiMap() {
    assertFreshInstance(new TypeToken<ImmutableBiMap<String, Integer>>() {});
  }

  public void testImmutableTable() {
    assertFreshInstance(new TypeToken<ImmutableTable<String, Integer, ImmutableList<String>>>() {});
  }

  public void testList() {
    assertFreshInstance(new TypeToken<List<String>>() {});
    assertNotInstantiable(new TypeToken<List<EmptyEnum>>() {});
  }

  public void testArrayList() {
    assertFreshInstance(new TypeToken<ArrayList<String>>() {});
    assertNotInstantiable(new TypeToken<ArrayList<EmptyEnum>>() {});
  }

  public void testLinkedList() {
    assertFreshInstance(new TypeToken<LinkedList<String>>() {});
  }

  public void testSet() {
    assertFreshInstance(new TypeToken<Set<String>>() {});
    assertNotInstantiable(new TypeToken<Set<EmptyEnum>>() {});
  }

  public void testHashSet() {
    assertFreshInstance(new TypeToken<HashSet<String>>() {});
  }

  public void testLinkedHashSet() {
    assertFreshInstance(new TypeToken<LinkedHashSet<String>>() {});
  }

  public void testTreeSet() {
    assertFreshInstance(new TypeToken<TreeSet<String>>() {});
  }

  public void testSortedSet() {
    assertFreshInstance(new TypeToken<SortedSet<String>>() {});
  }

  public void testNavigableSet() {
    assertFreshInstance(new TypeToken<NavigableSet<String>>() {});
  }

  public void testMultiset() {
    assertFreshInstance(new TypeToken<Multiset<String>>() {});
  }

  public void testSortedMultiset() {
    assertFreshInstance(new TypeToken<SortedMultiset<String>>() {});
  }

  public void testHashMultiset() {
    assertFreshInstance(new TypeToken<HashMultiset<String>>() {});
  }

  public void testLinkedHashMultiset() {
    assertFreshInstance(new TypeToken<LinkedHashMultiset<String>>() {});
  }

  public void testTreeMultiset() {
    assertFreshInstance(new TypeToken<TreeMultiset<String>>() {});
  }

  public void testImmutableSortedMultiset() {
    assertFreshInstance(new TypeToken<ImmutableSortedMultiset<String>>() {});
  }

  public void testCollection() {
    assertFreshInstance(new TypeToken<Collection<String>>() {});
  }

  public void testIterable() {
    assertFreshInstance(new TypeToken<Iterable<String>>() {});
  }

  public void testMap() {
    assertFreshInstance(new TypeToken<Map<String, ?>>() {});
  }

  public void testHashMap() {
    assertFreshInstance(new TypeToken<HashMap<String, ?>>() {});
  }

  public void testLinkedHashMap() {
    assertFreshInstance(new TypeToken<LinkedHashMap<String, ?>>() {});
  }

  public void testTreeMap() {
    assertFreshInstance(new TypeToken<TreeMap<String, ?>>() {});
  }

  public void testSortedMap() {
    assertFreshInstance(new TypeToken<SortedMap<?, String>>() {});
  }

  public void testNavigableMap() {
    assertFreshInstance(new TypeToken<NavigableMap<?, ?>>() {});
  }

  public void testConcurrentMap() {
    assertFreshInstance(new TypeToken<ConcurrentMap<String, ?>>() {});
    assertCanGenerateOnly(
        new TypeToken<ConcurrentMap<EmptyEnum, String>>() {}, Maps.newConcurrentMap());
  }

  public void testMultimap() {
    assertFreshInstance(new TypeToken<Multimap<String, ?>>() {});
  }

  public void testHashMultimap() {
    assertFreshInstance(new TypeToken<HashMultimap<String, ?>>() {});
  }

  public void testLinkedHashMultimap() {
    assertFreshInstance(new TypeToken<LinkedHashMultimap<String, ?>>() {});
  }

  public void testListMultimap() {
    assertFreshInstance(new TypeToken<ListMultimap<String, ?>>() {});
  }

  public void testArrayListMultimap() {
    assertFreshInstance(new TypeToken<ArrayListMultimap<String, ?>>() {});
  }

  public void testSetMultimap() {
    assertFreshInstance(new TypeToken<SetMultimap<String, ?>>() {});
  }

  public void testBiMap() {
    assertFreshInstance(new TypeToken<BiMap<String, ?>>() {});
    assertNotInstantiable(new TypeToken<BiMap<EmptyEnum, String>>() {});
  }

  public void testHashBiMap() {
    assertFreshInstance(new TypeToken<HashBiMap<String, ?>>() {});
  }

  public void testTable() {
    assertFreshInstance(new TypeToken<Table<String, ?, ?>>() {});
    assertNotInstantiable(new TypeToken<Table<EmptyEnum, String, Integer>>() {});
  }

  public void testHashBasedTable() {
    assertFreshInstance(new TypeToken<HashBasedTable<String, ?, ?>>() {});
  }

  public void testRowSortedTable() {
    assertFreshInstance(new TypeToken<RowSortedTable<String, ?, ?>>() {});
  }

  public void testTreeBasedTable() {
    assertFreshInstance(new TypeToken<TreeBasedTable<String, ?, ?>>() {});
  }

  public void testObject() {
    assertEquals(
        new FreshValueGenerator().generateFresh(String.class),
        new FreshValueGenerator().generateFresh(Object.class));
  }

  public void testEnums() {
    assertEqualInstance(EmptyEnum.class, null);
    assertEqualInstance(OneConstantEnum.class, OneConstantEnum.CONSTANT1);
    assertFreshInstance(TwoConstantEnum.class, 2);
    assertFreshInstance(new TypeToken<com.google.common.base.Optional<OneConstantEnum>>() {}, 2);
    assertFreshInstance(new TypeToken<List<OneConstantEnum>>() {}, 1);
    assertFreshInstance(new TypeToken<List<TwoConstantEnum>>() {}, 2);
  }

  @AndroidIncompatible // problem with equality of Type objects?
  public void testGoogleOptional() {
    FreshValueGenerator generator = new FreshValueGenerator();
    assertEquals(
        com.google.common.base.Optional.absent(),
        generator.generateFresh(new TypeToken<com.google.common.base.Optional<String>>() {}));
    assertEquals(
        com.google.common.base.Optional.of("2"),
        generator.generateFresh(new TypeToken<com.google.common.base.Optional<String>>() {}));
    // Test that the first generated instance for different cgcb.Optional<T> is always absent().
    // Having generated cgcb.Optional<String> instances doesn't prevent absent() from being
    // generated for other cgcb.Optional types.
    assertEquals(
        com.google.common.base.Optional.absent(),
        generator.generateFresh(
            new TypeToken<com.google.common.base.Optional<OneConstantEnum>>() {}));
    assertEquals(
        com.google.common.base.Optional.of(OneConstantEnum.CONSTANT1),
        generator.generateFresh(
            new TypeToken<com.google.common.base.Optional<OneConstantEnum>>() {}));
  }

  @AndroidIncompatible
  public void testJavaOptional() {
    FreshValueGenerator generator = new FreshValueGenerator();
    assertEquals(Optional.empty(), generator.generateFresh(new TypeToken<Optional<String>>() {}));
    assertEquals(Optional.of("2"), generator.generateFresh(new TypeToken<Optional<String>>() {}));
    // Test that the first generated instance for different Optional<T> is always empty(). Having
    // generated Optional<String> instances doesn't prevent empty() from being generated for other
    // Optional types.
    assertEquals(
        Optional.empty(), generator.generateFresh(new TypeToken<Optional<OneConstantEnum>>() {}));
    assertEquals(
        Optional.of(OneConstantEnum.CONSTANT1),
        generator.generateFresh(new TypeToken<Optional<OneConstantEnum>>() {}));
  }

  public void testOptionalInt() {
    assertFreshInstance(new TypeToken<OptionalInt>() {});
  }

  public void testOptionalLong() {
    assertFreshInstance(new TypeToken<OptionalLong>() {});
  }

  public void testOptionalDouble() {
    assertFreshInstance(new TypeToken<OptionalDouble>() {});
  }

  public void testAddSampleInstances_twoInstances() {
    FreshValueGenerator generator = new FreshValueGenerator();
    generator.addSampleInstances(String.class, ImmutableList.of("a", "b"));
    assertEquals("a", generator.generateFresh(String.class));
    assertEquals("b", generator.generateFresh(String.class));
    assertEquals("a", generator.generateFresh(String.class));
  }

  public void testAddSampleInstances_oneInstance() {
    FreshValueGenerator generator = new FreshValueGenerator();
    generator.addSampleInstances(String.class, ImmutableList.of("a"));
    assertEquals("a", generator.generateFresh(String.class));
    assertEquals("a", generator.generateFresh(String.class));
  }

  public void testAddSampleInstances_noInstance() {
    FreshValueGenerator generator = new FreshValueGenerator();
    generator.addSampleInstances(String.class, ImmutableList.<String>of());
    assertEquals(
        new FreshValueGenerator().generateFresh(String.class),
        generator.generateFresh(String.class));
  }

  public void testFreshCurrency() {
    FreshValueGenerator generator = new FreshValueGenerator();
    // repeat a few times to make sure we don't stumble upon a bad Locale
    assertNotNull(generator.generateFresh(Currency.class));
    assertNotNull(generator.generateFresh(Currency.class));
    assertNotNull(generator.generateFresh(Currency.class));
  }

  public void testNulls() throws Exception {
    new ClassSanityTester()
        .setDefault(Method.class, FreshValueGeneratorTest.class.getDeclaredMethod("testNulls"))
        .testNulls(FreshValueGenerator.class);
  }

  private static void assertFreshInstances(Class<?>... types) {
    for (Class<?> type : types) {
      assertFreshInstance(type, 2);
    }
  }

  private static void assertFreshInstance(TypeToken<?> type) {
    assertFreshInstance(type, 3);
  }

  private static void assertFreshInstance(Class<?> type, int instances) {
    assertFreshInstance(TypeToken.of(type), instances);
  }

  private static void assertFreshInstance(TypeToken<?> type, int instances) {
    FreshValueGenerator generator = new FreshValueGenerator();
    EqualsTester tester = new EqualsTester();
    for (int i = 0; i < instances; i++) {
      tester.addEqualityGroup(generator.generateFresh(type));
    }
    tester.testEquals();
  }

  private static <T> void assertEqualInstance(Class<T> type, T value) {
    FreshValueGenerator generator = new FreshValueGenerator();
    assertEquals(value, generator.generateFresh(type));
    assertEquals(value, generator.generateFresh(type));
  }

  private enum EmptyEnum {}

  private enum OneConstantEnum {
    CONSTANT1
  }

  private enum TwoConstantEnum {
    CONSTANT1,
    CONSTANT2
  }

  private static void assertCanGenerateOnly(TypeToken<?> type, Object expected) {
    FreshValueGenerator generator = new FreshValueGenerator();
    assertValueAndTypeEquals(expected, generator.generateFresh(type));
    assertNull(generator.generateFresh(type));
  }

  private static void assertNotInstantiable(TypeToken<?> type) {
    assertNull(new FreshValueGenerator().generateFresh(type));
  }

  private static void assertValueAndTypeEquals(Object expected, Object actual) {
    assertEquals(expected, actual);
    assertEquals(expected.getClass(), actual.getClass());
  }
}

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
import com.google.common.base.Optional;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.collect.TreeMultiset;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.google.common.reflect.TypeToken;

import junit.framework.TestCase;

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
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Tests for {@link FreshValueGenerator}.
 *
 * @author Ben Yu
 */
public class FreshValueGeneratorTest extends TestCase {

  public void testFreshInstance() {
    assertFreshInstances(
        String.class, CharSequence.class,
        Appendable.class, StringBuffer.class, StringBuilder.class,
        Pattern.class, MatchResult.class,
        Number.class, int.class, Integer.class,
        long.class, Long.class,
        short.class, Short.class,
        byte.class, Byte.class,
        boolean.class, Boolean.class,
        char.class, Character.class,
        int[].class, Object[].class,
        UnsignedInteger.class, UnsignedLong.class,
        BigInteger.class, BigDecimal.class,
        Throwable.class, Error.class, Exception.class, RuntimeException.class,
        Charset.class, Locale.class, Currency.class,
        List.class, Map.Entry.class,
        Object.class,
        Equivalence.class, Predicate.class, Function.class,
        Comparable.class, Comparator.class, Ordering.class,
        Class.class, Type.class, TypeToken.class,
        TimeUnit.class, Ticker.class,
        Joiner.class, Splitter.class, CharMatcher.class,
        InputStream.class, ByteArrayInputStream.class,
        Reader.class, Readable.class, StringReader.class,
        OutputStream.class, ByteArrayOutputStream.class,
        Writer.class, StringWriter.class, File.class,
        Buffer.class, ByteBuffer.class, CharBuffer.class,
        ShortBuffer.class, IntBuffer.class, LongBuffer.class,
        FloatBuffer.class, DoubleBuffer.class,
        String[].class, Object[].class, int[].class);
  }

  public void testStringArray() {
    FreshValueGenerator generator = new FreshValueGenerator();
    String[] a1 = generator.generate(String[].class);
    String[] a2 = generator.generate(String[].class);
    assertFalse(a1[0].equals(a2[0]));
  }

  public void testPrimitiveArray() {
    FreshValueGenerator generator = new FreshValueGenerator();
    int[] a1 = generator.generate(int[].class);
    int[] a2 = generator.generate(int[].class);
    assertTrue(a1[0] != a2[0]);
  }

  public void testRange() {
    assertFreshInstance(new TypeToken<Range<String>>() {});
  }

  public void testImmutableList() {
    assertFreshInstance(new TypeToken<ImmutableList<String>>() {});
    assertValueAndTypeEquals(ImmutableList.of(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableList<String>>() {}));
    assertValueAndTypeEquals(ImmutableList.of(new FreshValueGenerator().generate(int.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableList<Integer>>() {}));
    assertValueAndTypeEquals(ImmutableList.of(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableList<?>>() {}));
  }

  public void testImmutableSet() {
    assertFreshInstance(new TypeToken<ImmutableSet<String>>() {});
    assertValueAndTypeEquals(ImmutableSet.of(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableSet<String>>() {}));
    assertValueAndTypeEquals(ImmutableSet.of(new FreshValueGenerator().generate(Number.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableSet<Number>>() {}));
    assertValueAndTypeEquals(ImmutableSet.of(new FreshValueGenerator().generate(Number.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableSet<? extends Number>>() {}));
  }

  public void testImmutableSortedSet() {
    assertFreshInstance(new TypeToken<ImmutableSortedSet<String>>() {});
    assertValueAndTypeEquals(
        ImmutableSortedSet.of(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableSortedSet<String>>() {}));
  }

  public void testImmutableMultiset() {
    assertFreshInstance(new TypeToken<ImmutableSortedSet<String>>() {});
    assertValueAndTypeEquals(ImmutableMultiset.of(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableMultiset<String>>() {}));
    assertValueAndTypeEquals(ImmutableMultiset.of(new FreshValueGenerator().generate(Number.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableMultiset<Number>>() {}));
    assertNotInstantiable(new TypeToken<ImmutableMultiset<EmptyEnum>>() {});
  }

  public void testImmutableCollection() {
    assertFreshInstance(new TypeToken<ImmutableCollection<String>>() {});
    assertValueAndTypeEquals(ImmutableList.of(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableCollection<String>>() {}));
    assertNotInstantiable(new TypeToken<ImmutableCollection<EmptyEnum>>() {});
  }

  public void testImmutableMap() {
    assertFreshInstance(new TypeToken<ImmutableMap<String, Integer>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    assertValueAndTypeEquals(
        ImmutableMap.of(generator.generate(String.class), generator.generate(int.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableMap<String, Integer>>() {}));
  }

  public void testImmutableSortedMap() {
    assertFreshInstance(new TypeToken<ImmutableSortedMap<String, Integer>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    assertValueAndTypeEquals(
        ImmutableSortedMap.of(generator.generate(String.class), generator.generate(int.class)),
        new FreshValueGenerator().generate(
            new TypeToken<ImmutableSortedMap<String, Integer>>() {}));
  }

  public void testImmutableMultimap() {
    assertFreshInstance(new TypeToken<ImmutableMultimap<String, Integer>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    assertValueAndTypeEquals(
        ImmutableMultimap.of(generator.generate(String.class), generator.generate(int.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableMultimap<String, Integer>>() {}));
    assertNotInstantiable(new TypeToken<ImmutableMultimap<EmptyEnum, String>>() {});
  }

  public void testImmutableListMultimap() {
    assertFreshInstance(new TypeToken<ImmutableListMultimap<String, Integer>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    assertValueAndTypeEquals(
        ImmutableListMultimap.of(generator.generate(String.class), generator.generate(int.class)),
        new FreshValueGenerator().generate(
            new TypeToken<ImmutableListMultimap<String, Integer>>() {}));
  }

  public void testImmutableSetMultimap() {
    assertFreshInstance(new TypeToken<ImmutableSetMultimap<String, Integer>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    assertValueAndTypeEquals(
        ImmutableSetMultimap.of(generator.generate(String.class), generator.generate(int.class)),
        new FreshValueGenerator().generate(
            new TypeToken<ImmutableSetMultimap<String, Integer>>() {}));
  }

  public void testImmutableBiMap() {
    assertFreshInstance(new TypeToken<ImmutableBiMap<String, Integer>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    assertValueAndTypeEquals(
        ImmutableBiMap.of(generator.generate(String.class), generator.generate(int.class)),
        new FreshValueGenerator().generate(
            new TypeToken<ImmutableBiMap<String, Integer>>() {}));
  }

  public void testImmutableTable() {
    assertFreshInstance(new TypeToken<ImmutableTable<String, Integer, ImmutableList<String>>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    assertValueAndTypeEquals(
        ImmutableTable.of(
            generator.generate(String.class), generator.generate(int.class),
            generator.generate(new TypeToken<ImmutableList<String>>() {})),
        new FreshValueGenerator().generate(
            new TypeToken<ImmutableTable<String, Integer, ImmutableList<String>>>() {}));
  }

  public void testList() {
    assertFreshInstance(new TypeToken<List<String>>() {});
    assertValueAndTypeEquals(Lists.newArrayList(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<List<String>>() {}));
    assertValueAndTypeEquals(Lists.newArrayList(new FreshValueGenerator().generate(int.class)),
        new FreshValueGenerator().generate(new TypeToken<List<Integer>>() {}));
    assertValueAndTypeEquals(Lists.newArrayList(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<List<?>>() {}));
    assertNotInstantiable(new TypeToken<List<EmptyEnum>>() {});
  }

  public void testArrayList() {
    assertFreshInstance(new TypeToken<ArrayList<String>>() {});
    assertValueAndTypeEquals(Lists.newArrayList(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<ArrayList<String>>() {}));
    assertValueAndTypeEquals(Lists.newArrayList(new FreshValueGenerator().generate(int.class)),
        new FreshValueGenerator().generate(new TypeToken<ArrayList<Integer>>() {}));
    assertValueAndTypeEquals(Lists.newArrayList(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<ArrayList<?>>() {}));
    assertNotInstantiable(new TypeToken<ArrayList<EmptyEnum>>() {});
  }

  public void testLinkedList() {
    assertFreshInstance(new TypeToken<LinkedList<String>>() {});
    assertValueAndTypeEquals(newLinkedList(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<LinkedList<String>>() {}));
  }

  public void testSet() {
    assertFreshInstance(new TypeToken<Set<String>>() {});
    assertValueAndTypeEquals(
        newLinkedHashSet(new FreshValueGenerator().generate(Number.class)),
        new FreshValueGenerator().generate(new TypeToken<Set<? extends Number>>() {}));
    assertNotInstantiable(new TypeToken<Set<EmptyEnum>>() {});
  }

  public void testHashSet() {
    assertFreshInstance(new TypeToken<HashSet<String>>() {});
    assertValueAndTypeEquals(
        newLinkedHashSet(new FreshValueGenerator().generate(Number.class)),
        new FreshValueGenerator().generate(new TypeToken<HashSet<? extends Number>>() {}));
  }

  public void testLinkedHashSet() {
    assertFreshInstance(new TypeToken<LinkedHashSet<String>>() {});
    assertValueAndTypeEquals(
        newLinkedHashSet(new FreshValueGenerator().generate(Number.class)),
        new FreshValueGenerator().generate(new TypeToken<LinkedHashSet<? extends Number>>() {}));
  }

  public void testTreeSet() {
    assertFreshInstance(new TypeToken<TreeSet<String>>() {});
    TreeSet<String> expected = Sets.newTreeSet();
    expected.add(new FreshValueGenerator().generate(String.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<TreeSet<? extends CharSequence>>() {}));
    assertNotInstantiable(new TypeToken<TreeSet<EmptyEnum>>() {});
  }

  public void testSortedSet() {
    assertFreshInstance(new TypeToken<SortedSet<String>>() {});
    TreeSet<String> expected = Sets.newTreeSet();
    expected.add(new FreshValueGenerator().generate(String.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<SortedSet<String>>() {}));
    assertNotInstantiable(new TypeToken<SortedSet<EmptyEnum>>() {});
  }

  public void testMultiset() {
    assertFreshInstance(new TypeToken<Multiset<String>>() {});
    Multiset<String> expected = HashMultiset.create();
    expected.add(new FreshValueGenerator().generate(String.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<Multiset<String>>() {}));
    assertNotInstantiable(new TypeToken<Multiset<EmptyEnum>>() {});
  }

  public void testSortedMultiset() {
    assertFreshInstance(new TypeToken<SortedMultiset<String>>() {});
    SortedMultiset<String> expected = TreeMultiset.create();
    expected.add(new FreshValueGenerator().generate(String.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<SortedMultiset<String>>() {}));
    assertNotInstantiable(new TypeToken<Multiset<EmptyEnum>>() {});
  }

  public void testHashMultiset() {
    assertFreshInstance(new TypeToken<HashMultiset<String>>() {});
    HashMultiset<String> expected = HashMultiset.create();
    expected.add(new FreshValueGenerator().generate(String.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<HashMultiset<String>>() {}));
  }

  public void testLinkedHashMultiset() {
    assertFreshInstance(new TypeToken<LinkedHashMultiset<String>>() {});
    LinkedHashMultiset<String> expected = LinkedHashMultiset.create();
    expected.add(new FreshValueGenerator().generate(String.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<LinkedHashMultiset<String>>() {}));
  }

  public void testTreeMultiset() {
    assertFreshInstance(new TypeToken<TreeMultiset<String>>() {});
    TreeMultiset<String> expected = TreeMultiset.create();
    expected.add(new FreshValueGenerator().generate(String.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<TreeMultiset<String>>() {}));
  }

  public void testImmutableSortedMultiset() {
    assertFreshInstance(new TypeToken<ImmutableSortedMultiset<String>>() {});
    assertValueAndTypeEquals(
        ImmutableSortedMultiset.of(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<ImmutableSortedMultiset<String>>() {}));
    assertNotInstantiable(new TypeToken<Multiset<EmptyEnum>>() {});
  }

  public void testCollection() {
    assertFreshInstance(new TypeToken<Collection<String>>() {});
    assertValueAndTypeEquals(Lists.newArrayList(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<Collection<String>>() {}));
    assertNotInstantiable(new TypeToken<Collection<EmptyEnum>>() {});
  }

  public void testIterable() {
    assertFreshInstance(new TypeToken<Iterable<String>>() {});
    assertValueAndTypeEquals(Lists.newArrayList(new FreshValueGenerator().generate(String.class)),
        new FreshValueGenerator().generate(new TypeToken<Iterable<String>>() {}));
    assertNotInstantiable(new TypeToken<Iterable<EmptyEnum>>() {});
  }

  public void testMap() {
    assertFreshInstance(new TypeToken<Map<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    Map<String, Integer> expected = Maps.newLinkedHashMap();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<Map<String, Integer>>() {}));
    assertNotInstantiable(new TypeToken<Map<EmptyEnum, String>>() {});
  }

  public void testHashMap() {
    assertFreshInstance(new TypeToken<HashMap<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    HashMap<String, Integer> expected = Maps.newLinkedHashMap();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<HashMap<String, Integer>>() {}));
  }

  public void testLinkedHashMap() {
    assertFreshInstance(new TypeToken<LinkedHashMap<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    LinkedHashMap<String, Integer> expected = Maps.newLinkedHashMap();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<LinkedHashMap<String, Integer>>() {}));
  }

  public void testTreeMap() {
    assertFreshInstance(new TypeToken<TreeMap<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    TreeMap<String, Integer> expected = Maps.newTreeMap();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<TreeMap<String, Integer>>() {}));
    assertNotInstantiable(new TypeToken<LinkedHashSet<EmptyEnum>>() {});
  }

  public void testSortedMap() {
    assertFreshInstance(new TypeToken<SortedMap<?, String>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    TreeMap<String, Integer> expected = Maps.newTreeMap();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(
            new TypeToken<SortedMap<String, Integer>>() {}));
    assertNotInstantiable(new TypeToken<SortedMap<EmptyEnum, String>>() {});
  }

  public void testConcurrentMap() {
    assertFreshInstance(new TypeToken<ConcurrentMap<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    ConcurrentMap<String, Integer> expected = Maps.newConcurrentMap();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<ConcurrentMap<String, Integer>>() {}));
    assertNotInstantiable(new TypeToken<ConcurrentMap<EmptyEnum, String>>() {});
  }

  public void testMultimap() {
    assertFreshInstance(new TypeToken<Multimap<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    Multimap<String, Integer> expected = ArrayListMultimap.create();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<Multimap<String, Integer>>() {}));
    assertNotInstantiable(new TypeToken<Multimap<EmptyEnum, String>>() {});
  }

  public void testHashMultimap() {
    assertFreshInstance(new TypeToken<HashMultimap<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    HashMultimap<String, Integer> expected = HashMultimap.create();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(new TypeToken<HashMultimap<String, Integer>>() {}));
  }

  public void testLinkedHashMultimap() {
    assertFreshInstance(new TypeToken<LinkedHashMultimap<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    LinkedHashMultimap<String, Integer> expected = LinkedHashMultimap.create();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(
            new TypeToken<LinkedHashMultimap<String, Integer>>() {}));
  }

  public void testListMultimap() {
    assertFreshInstance(new TypeToken<ListMultimap<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    ListMultimap<String, Integer> expected = ArrayListMultimap.create();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(
            new TypeToken<ListMultimap<String, Integer>>() {}));
    assertNotInstantiable(new TypeToken<ListMultimap<EmptyEnum, String>>() {});
  }

  public void testArrayListMultimap() {
    assertFreshInstance(new TypeToken<ArrayListMultimap<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    ArrayListMultimap<String, Integer> expected = ArrayListMultimap.create();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(
            new TypeToken<ArrayListMultimap<String, Integer>>() {}));
  }

  public void testSetMultimap() {
    assertFreshInstance(new TypeToken<SetMultimap<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    SetMultimap<String, Integer> expected = LinkedHashMultimap.create();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(
            new TypeToken<SetMultimap<String, Integer>>() {}));
    assertNotInstantiable(new TypeToken<SetMultimap<EmptyEnum, String>>() {});
  }

  public void testBiMap() {
    assertFreshInstance(new TypeToken<BiMap<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    BiMap<String, Integer> expected = HashBiMap.create();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(
            new TypeToken<BiMap<String, Integer>>() {}));
    assertNotInstantiable(new TypeToken<BiMap<EmptyEnum, String>>() {});
  }

  public void testHashBiMap() {
    assertFreshInstance(new TypeToken<HashBiMap<String, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    HashBiMap<String, Integer> expected = HashBiMap.create();
    expected.put(generator.generate(String.class), generator.generate(int.class));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(
            new TypeToken<HashBiMap<String, Integer>>() {}));
  }

  public void testTable() {
    assertFreshInstance(new TypeToken<Table<String, ?, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    Table<String, Integer, Object> expected = HashBasedTable.create();
    expected.put(generator.generate(String.class), generator.generate(int.class),
            generator.generate(new TypeToken<List<String>>() {}));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(
            new TypeToken<Table<String, Integer, List<String>>>() {}));
    assertNotInstantiable(new TypeToken<Table<EmptyEnum, String, Integer>>() {});
  }

  public void testHashBasedTable() {
    assertFreshInstance(new TypeToken<HashBasedTable<String, ?, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    HashBasedTable<String, Integer, Object> expected = HashBasedTable.create();
    expected.put(generator.generate(String.class), generator.generate(int.class),
            generator.generate(new TypeToken<List<String>>() {}));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(
            new TypeToken<HashBasedTable<String, Integer, List<String>>>() {}));
  }

  public void testRowSortedTable() {
    assertFreshInstance(new TypeToken<RowSortedTable<String, ?, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    RowSortedTable<String, Integer, Object> expected = TreeBasedTable.create();
    expected.put(generator.generate(String.class), generator.generate(int.class),
            generator.generate(new TypeToken<List<String>>() {}));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(
            new TypeToken<RowSortedTable<String, Integer, List<String>>>() {}));
    assertNotInstantiable(new TypeToken<RowSortedTable<EmptyEnum, String, Integer>>() {});
  }

  public void testTreeBasedTable() {
    assertFreshInstance(new TypeToken<TreeBasedTable<String, ?, ?>>() {});
    FreshValueGenerator generator = new FreshValueGenerator();
    TreeBasedTable<String, Integer, Object> expected = TreeBasedTable.create();
    expected.put(generator.generate(String.class), generator.generate(int.class),
            generator.generate(new TypeToken<List<String>>() {}));
    assertValueAndTypeEquals(expected,
        new FreshValueGenerator().generate(
            new TypeToken<TreeBasedTable<String, Integer, List<String>>>() {}));
  }

  public void testObject() {
    assertEquals(new FreshValueGenerator().generate(String.class),
        new FreshValueGenerator().generate(Object.class));
  }

  public void testEnums() {
    assertEqualInstance(EmptyEnum.class, null);
    assertEqualInstance(OneConstantEnum.class, OneConstantEnum.CONSTANT1);
    assertFreshInstance(TwoConstantEnum.class);
    assertFreshInstance(new TypeToken<Optional<OneConstantEnum>>() {});
  }

  public void testOptional() {
    FreshValueGenerator generator = new FreshValueGenerator();
    assertEquals(Optional.absent(), generator.generate(new TypeToken<Optional<String>>() {}));
    assertEquals(Optional.of("1"), generator.generate(new TypeToken<Optional<String>>() {}));
    // Test that the first generated instance for different Optional<T> is always absent().
    // Having generated Optional<String> instances doesn't prevent absent() from being generated for
    // other Optional types.
    assertEquals(Optional.absent(),
        generator.generate(new TypeToken<Optional<OneConstantEnum>>() {}));
    assertEquals(Optional.of(OneConstantEnum.CONSTANT1),
        generator.generate(new TypeToken<Optional<OneConstantEnum>>() {}));
  }

  public void testAddSampleInstances_twoInstances() {
    FreshValueGenerator generator = new FreshValueGenerator();
    generator.addSampleInstances(String.class, ImmutableList.of("a", "b"));
    assertEquals("a", generator.generate(String.class));
    assertEquals("b", generator.generate(String.class));
    assertEquals("a", generator.generate(String.class));
  }

  public void testAddSampleInstances_oneInstance() {
    FreshValueGenerator generator = new FreshValueGenerator();
    generator.addSampleInstances(String.class, ImmutableList.of("a"));
    assertEquals("a", generator.generate(String.class));
    assertEquals("a", generator.generate(String.class));
  }

  public void testAddSampleInstances_noInstance() {
    FreshValueGenerator generator = new FreshValueGenerator();
    generator.addSampleInstances(String.class, ImmutableList.<String>of());
    assertEquals(new FreshValueGenerator().generate(String.class),
        generator.generate(String.class));
  }

  public void testFreshCurrency() {
    FreshValueGenerator generator = new FreshValueGenerator();
    // repeat a few times to make sure we don't stumble upon a bad Locale
    assertNotNull(generator.generate(Currency.class));
    assertNotNull(generator.generate(Currency.class));
    assertNotNull(generator.generate(Currency.class));
  }

  public void testNulls() throws Exception {
    new ClassSanityTester()
        .setDefault(Method.class, FreshValueGeneratorTest.class.getDeclaredMethod("testNulls"))
        .testNulls(FreshValueGenerator.class);
  }

  private static void assertFreshInstances(Class<?>... types) {
    for (Class<?> type : types) {
      assertFreshInstance(type);
    }
  }

  private static void assertFreshInstance(TypeToken<?> type) {
    FreshValueGenerator generator = new FreshValueGenerator();
    Object value1 = generator.generate(type);
    Object value2 = generator.generate(type);
    assertNotNull("Null returned for " + type, value1);
    assertFalse("Equal instance " + value1 + " returned for " + type, value1.equals(value2));
  }

  private static <T> void assertFreshInstance(Class<T> type) {
    assertFreshInstance(TypeToken.of(type));
  }

  private static <T> void assertEqualInstance(Class<T> type, T value) {
    FreshValueGenerator generator = new FreshValueGenerator();
    assertEquals(value, generator.generate(type));
    assertEquals(value, generator.generate(type));
  }

  private enum EmptyEnum {}

  private enum OneConstantEnum {
    CONSTANT1
  }

  private enum TwoConstantEnum {
    CONSTANT1, CONSTANT2
  }

  private static void assertValueAndTypeEquals(Object expected, Object actual) {
    assertEquals(expected, actual);
    assertEquals(expected.getClass(), actual.getClass());
  }

  private static void assertNotInstantiable(TypeToken<?> type) {
    assertNull(new FreshValueGenerator().generate(type));
  }

  private static <E> LinkedHashSet<E> newLinkedHashSet(E element) {
    LinkedHashSet<E> set = Sets.newLinkedHashSet();
    set.add(element);
    return set;
  }

  private static <E> LinkedList<E> newLinkedList(E element) {
    LinkedList<E> list = Lists.newLinkedList();
    list.add(element);
    return list;
  }
}

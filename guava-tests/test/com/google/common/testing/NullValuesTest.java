// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.testing;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Unit test for {@link NullValues}.
 *
 * @author benyu@google.com (Ben Yu)
 */
public class NullValuesTest extends TestCase {

  public void testGet_primitives() {
    assertNull(NullValues.get(void.class));
    assertNull(NullValues.get(Void.class));
    assertEquals(Boolean.FALSE, NullValues.get(boolean.class));
    assertEquals(Boolean.FALSE, NullValues.get(Boolean.class));
    assertEquals(Character.valueOf('\0'), NullValues.get(char.class));
    assertEquals(Character.valueOf('\0'), NullValues.get(Character.class));
    assertEquals(Byte.valueOf((byte) 0), NullValues.get(byte.class));
    assertEquals(Byte.valueOf((byte) 0), NullValues.get(Byte.class));
    assertEquals(Short.valueOf((short) 0), NullValues.get(short.class));
    assertEquals(Short.valueOf((short) 0), NullValues.get(Short.class));
    assertEquals(Integer.valueOf(0), NullValues.get(int.class));
    assertEquals(Integer.valueOf(0), NullValues.get(Integer.class));
    assertEquals(Long.valueOf(0), NullValues.get(long.class));
    assertEquals(Long.valueOf(0), NullValues.get(Long.class));
    assertEquals(Float.valueOf(0), NullValues.get(float.class));
    assertEquals(Float.valueOf(0), NullValues.get(Float.class));
    assertEquals(Double.valueOf(0), NullValues.get(double.class));
    assertEquals(Double.valueOf(0), NullValues.get(Double.class));
    assertEquals("", NullValues.get(String.class));
    assertEquals("", NullValues.get(CharSequence.class));
    assertEquals(TimeUnit.SECONDS, NullValues.get(TimeUnit.class));
    assertNotNull(NullValues.get(Object.class));
    assertEquals(Pattern.compile("").pattern(), NullValues.get(Pattern.class).pattern());
    assertEquals(0, NullValues.get(Number.class));
  }

  public void testGet_collections() {
    assertEquals(Iterators.emptyIterator(), NullValues.get(Iterator.class));
    assertFalse(NullValues.get(ListIterator.class).hasNext());
    assertEquals(ImmutableSet.of(), NullValues.get(Iterable.class));
    assertEquals(ImmutableSet.of(), NullValues.get(Set.class));
    assertEquals(ImmutableSet.of(), NullValues.get(ImmutableSet.class));
    assertEquals(ImmutableSortedSet.of(), NullValues.get(SortedSet.class));
    assertEquals(ImmutableSortedSet.of(), NullValues.get(ImmutableSortedSet.class));
    assertEquals(ImmutableList.of(), NullValues.get(Collection.class));
    assertEquals(ImmutableList.of(), NullValues.get(ImmutableCollection.class));
    assertEquals(ImmutableList.of(), NullValues.get(List.class));
    assertEquals(ImmutableList.of(), NullValues.get(ImmutableList.class));
    assertEquals(ImmutableMap.of(), NullValues.get(Map.class));
    assertEquals(ImmutableMap.of(), NullValues.get(ImmutableMap.class));
    assertEquals(ImmutableSortedMap.of(), NullValues.get(SortedMap.class));
    assertEquals(ImmutableSortedMap.of(), NullValues.get(ImmutableSortedMap.class));
    assertEquals(ImmutableMultiset.of(), NullValues.get(Multiset.class));
    assertEquals(ImmutableMultiset.of(), NullValues.get(ImmutableMultiset.class));
    assertEquals(ImmutableMultimap.of(), NullValues.get(Multimap.class));
    assertEquals(ImmutableMultimap.of(), NullValues.get(ImmutableMultimap.class));
    assertEquals(ImmutableTable.of(), NullValues.get(Table.class));
    assertEquals(ImmutableTable.of(), NullValues.get(ImmutableTable.class));
  }

  @SuppressWarnings("unchecked") // functor classes have no type parameters
  public void testGet_functors() {
    assertEquals(0, NullValues.get(Comparator.class).compare("abc", 123));
    assertTrue(NullValues.get(Predicate.class).apply("abc"));
  }

  public void testGet_comparable() {
    @SuppressWarnings("unchecked") // The null value can compare with any Object
    Comparable<Object> comparable = NullValues.get(Comparable.class);
    assertEquals(0, comparable.compareTo(comparable));
    assertTrue(comparable.compareTo("") > 0);
    try {
      comparable.compareTo(null);
      fail();
    } catch (NullPointerException expected) {}
  }

  public void testGet_array() {
    assertEquals(0, NullValues.get(int[].class).length);
    assertEquals(0, NullValues.get(Object[].class).length);
    assertEquals(0, NullValues.get(String[].class).length);
  }

  public void testGet_enum() {
    assertNull(NullValues.get(EmptyEnum.class));
    assertEquals(Direction.UP, NullValues.get(Direction.class));
  }

  public void testGet_interface() {
    assertNull(NullValues.get(SomeInterface.class));
  }

  public void testGet_class() {
    assertNull(NullValues.get(SomeAbstractClass.class));
    assertNull(NullValues.get(WithPrivateConstructor.class));
    assertNull(NullValues.get(NoDefaultConstructor.class));
    assertNull(NullValues.get(WithExceptionalConstructor.class));
    assertNull(NullValues.get(NonPublicClass.class));
  }

  public void testGet_mutable() {
    assertEquals(0, NullValues.get(ArrayList.class).size());
    assertEquals(0, NullValues.get(HashMap.class).size());
    assertEquals("", NullValues.get(Appendable.class).toString());
    assertEquals("", NullValues.get(StringBuilder.class).toString());
    assertEquals("", NullValues.get(StringBuffer.class).toString());
    assertFreshInstanceReturned(
        ArrayList.class, HashMap.class, Appendable.class, StringBuilder.class, StringBuffer.class);
  }

  private static void assertFreshInstanceReturned(Class<?>... mutableClasses) {
    for (Class<?> mutableClass : mutableClasses) {
      assertNotSame(NullValues.get(mutableClass), NullValues.get(mutableClass));
    }
  }

  private enum EmptyEnum {}

  private enum Direction {
    UP, DOWN
  }

  public interface SomeInterface {}

  public static abstract class SomeAbstractClass {
    public SomeAbstractClass() {}
  }

  static class NonPublicClass {
    public NonPublicClass() {}
  }
  
  private static class WithPrivateConstructor {}
  
  public static class NoDefaultConstructor {
    public NoDefaultConstructor(int i) {}
  }

  public static class WithExceptionalConstructor {
    public WithExceptionalConstructor() {
      throw new RuntimeException();
    }
  }
}

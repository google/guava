// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.testing;

import com.google.common.annotations.Beta;
import com.google.common.base.Defaults;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
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
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.google.common.primitives.Primitives;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Can provide an appropriate "null value" for a wide variety of types.
 *
 * @author kevinb@google.com (Kevin Bourrilllion)
 * @author benyu@google.com (Jige Yu)
 * @since 12.0
 */
@Beta
public final class NullValues {

  // Compare by toString() to satisfy 2 properties:
  // 1. compareTo(null) should throw NullPointerException
  // 2. the order is deterministic and easy to understand, for debugging purpose.
  private static final Comparable<Object> BY_TO_STRING = new Comparable<Object>() {
    @Override public int compareTo(Object o) {
      return toString().compareTo(o.toString());
    }
    @Override public String toString() {
      return "BY_TO_STRING";
    }
  };

  // Always equal is a valid total ordering. And it works for any Object.
  private static final Ordering<Object> ALWAYS_EQUAL = new Ordering<Object>() {
    @Override public int compare(Object o1, Object o2) {
      return 0;
    }
    @Override public String toString() {
      return "ALWAYS_EQUAL";
    }
  };

  private static final ClassToInstanceMap<Object> DEFAULTS = ImmutableClassToInstanceMap.builder()
      .put(Object.class, new Object())
      .put(CharSequence.class, "")
      .put(String.class, "")
      .put(Number.class, 0)
      .put(Pattern.class, Pattern.compile(""))
      .put(TimeUnit.class, TimeUnit.SECONDS)
      // All collections are immutable empty. So safe for any type parameter.
      .put(Collection.class, ImmutableList.of())
      .put(Iterable.class, ImmutableSet.of())
      .put(Iterator.class, Iterators.emptyIterator())
      .put(ListIterator.class, ImmutableList.of().listIterator())
      .put(List.class, ImmutableList.of())
      .put(ImmutableList.class, ImmutableList.of())
      .put(Set.class, ImmutableSet.of())
      .put(ImmutableSet.class, ImmutableSet.of())
      .put(SortedSet.class, ImmutableSortedSet.of())
      .put(ImmutableSortedSet.class, ImmutableSortedSet.of())
      .put(ImmutableCollection.class, ImmutableList.of())
      .put(Map.class, ImmutableMap.of())
      .put(ImmutableMap.class, ImmutableMap.of())
      .put(SortedMap.class, ImmutableSortedMap.of())
      .put(ImmutableSortedMap.class, ImmutableSortedMap.of())
      .put(Multimap.class, ImmutableMultimap.of())
      .put(ImmutableMultimap.class, ImmutableMultimap.of())
      .put(Multiset.class, ImmutableMultiset.of())
      .put(ImmutableMultiset.class, ImmutableMultiset.of())
      .put(Table.class, ImmutableTable.of())
      .put(ImmutableTable.class, ImmutableTable.of())
      .put(Comparable.class, BY_TO_STRING)
      .put(Comparator.class, ALWAYS_EQUAL)
      .put(Ordering.class, ALWAYS_EQUAL)
      .put(Predicate.class, Predicates.alwaysTrue())
      .build();

  private static final Logger logger = Logger.getLogger(NullValues.class.getName());
  
  /**
   * Returns an 'empty' value for {@code type} as the null value, or {@code null} if empty-ness is
   * unknown for the type.
   */
  @Nullable public static <T> T get(Class<T> type) {
    T defaultValue = DEFAULTS.getInstance(type);
    if (defaultValue != null) {
      return defaultValue;
    }
    if (type.isEnum()) {
      T[] enumConstants = type.getEnumConstants();
      return (enumConstants.length == 0)
          ? null
          : enumConstants[0];
    }
    if (type.isArray()) {
      return createEmptyArray(type);
    }
    if (type == Appendable.class) {
      return type.cast(new StringBuilder());
    }
    T jvmDefault = Defaults.defaultValue(Primitives.unwrap(type));
    if (jvmDefault != null) {
      return jvmDefault;
    }
    if (Modifier.isAbstract(type.getModifiers()) || !Modifier.isPublic(type.getModifiers())) {
      return null;
    }
    final Constructor<T> constructor;
    try {
      constructor = type.getConstructor();
    } catch (NoSuchMethodException e) {
      return null;
    }
    constructor.setAccessible(true); // accessibility check is too slow
    try {
      return constructor.newInstance();
    } catch (InstantiationException impossible) {
      throw new AssertionError(impossible);
    } catch (IllegalAccessException impossible) {
      throw new AssertionError(impossible);
    } catch (InvocationTargetException e) {
      logger.log(Level.WARNING, "Exception while invoking default constructor.", e.getCause());
      return null;
    }
  }

  @SuppressWarnings("unchecked") // same component type means same array type
  private static <T> T createEmptyArray(Class<T> arrayType) {
    return (T) Array.newInstance(arrayType.getComponentType(), 0);
  }

  private NullValues() {}
}

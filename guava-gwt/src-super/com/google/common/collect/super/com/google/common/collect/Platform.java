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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps.EntryTransformer;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Minimal GWT emulation of {@code com.google.common.collect.Platform}.
 *
 * <p><strong>This .java file should never be consumed by javac.</strong>
 *
 * @author Hayward Chan
 */
final class Platform {

  static <T> T[] newArray(T[] reference, int length) {
    T[] clone = Arrays.copyOf(reference, 0);
    resizeArray(clone, length);
    return clone;
  }

  private static void resizeArray(Object array, int newSize) {
    ((NativeArray) array).setLength(newSize);
  }

  // TODO(user): Move this logic to a utility class.
  @JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
  private interface NativeArray {
    @JsProperty
    void setLength(int length);
  }

  static MapMaker tryWeakKeys(MapMaker mapMaker) {
    return mapMaker;
  }

  static <K, V1, V2> SortedMap<K, V2> mapsTransformEntriesSortedMap(
      SortedMap<K, V1> fromMap,
      EntryTransformer<? super K, ? super V1, V2> transformer) {
    return Maps.transformEntriesIgnoreNavigable(fromMap, transformer);
  }

  static <K, V> SortedMap<K, V> mapsAsMapSortedSet(
      SortedSet<K> set, Function<? super K, V> function) {
    return Maps.asMapSortedIgnoreNavigable(set, function);
  }

  static <E> SortedSet<E> setsFilterSortedSet(
      SortedSet<E> unfiltered, Predicate<? super E> predicate) {
    return Sets.filterSortedIgnoreNavigable(unfiltered, predicate);
  }

  static <K, V> SortedMap<K, V> mapsFilterSortedMap(
      SortedMap<K, V> unfiltered, Predicate<? super Map.Entry<K, V>> predicate) {
    return Maps.filterSortedIgnoreNavigable(unfiltered, predicate);
  }

  static <E> Deque<E> newFastestDeque(int ignored) {
    return new LinkedList<E>();
  }

  private Platform() {}
}

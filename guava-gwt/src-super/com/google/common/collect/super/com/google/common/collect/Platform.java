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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Minimal GWT emulation of {@code com.google.common.collect.Platform}.
 *
 * <p><strong>This .java file should never be consumed by javac.</strong>
 *
 * @author Hayward Chan
 */
final class Platform {
  static <K, V> Map<K, V> newHashMapWithExpectedSize(int expectedSize) {
    return Maps.newHashMapWithExpectedSize(expectedSize);
  }

  static <K, V> Map<K, V> newLinkedHashMapWithExpectedSize(int expectedSize) {
    return Maps.newLinkedHashMapWithExpectedSize(expectedSize);
  }

  static <E> Set<E> newHashSetWithExpectedSize(int expectedSize) {
    return Sets.newHashSetWithExpectedSize(expectedSize);
  }

  static <E> Set<E> newLinkedHashSetWithExpectedSize(int expectedSize) {
    return Sets.newLinkedHashSetWithExpectedSize(expectedSize);
  }

  /**
   * Returns the platform preferred map implementation that preserves insertion order when used only
   * for insertions.
   */
  static <K, V> Map<K, V> preservesInsertionOrderOnPutsMap() {
    return Maps.newLinkedHashMap();
  }

  /**
   * Returns the platform preferred set implementation that preserves insertion order when used only
   * for insertions.
   */
  static <E> Set<E> preservesInsertionOrderOnAddsSet() {
    return Sets.newLinkedHashSet();
  }

  static <T> T[] newArray(T[] reference, int length) {
    T[] clone = Arrays.copyOf(reference, 0);
    resizeArray(clone, length);
    return clone;
  }

  private static void resizeArray(Object array, int newSize) {
    ((NativeArray) array).setLength(newSize);
  }

  /** Equivalent to Arrays.copyOfRange(source, from, to, arrayOfType.getClass()). */
  static <T> T[] copy(Object[] source, int from, int to, T[] arrayOfType) {
    T[] result = newArray(arrayOfType, to - from);
    System.arraycopy(source, from, result, 0, to - from);
    return result;
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

  static int reduceIterationsIfGwt(int iterations) {
    return iterations / 10;
  }

  static int reduceExponentIfGwt(int exponent) {
    return exponent / 2;
  }

  /*
   * We will eventually disable GWT-RPC on the server side, but we'll leave it nominally enabled on
   * the client side. There's little practical difference: If it's disabled on the server, it won't
   * work. It's just a matter of how quickly it fails. I'm not sure if failing on the client would
   * be better or not, but it's harder: GWT's System.getProperty reads from a different property
   * list than Java's, so anyone who needs to reenable GWT-RPC in an emergency would have to figure
   * out how to set both properties. It's easier to have to set only one, and it might as well be
   * the Java property, since Guava already reads another Java property.
   */
  static void checkGwtRpcEnabled() {}

  private Platform() {}
}

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

  static int reduceIterationsIfGwt(int iterations) {
    return iterations / 10;
  }

  static int reduceExponentIfGwt(int exponent) {
    return exponent / 2;
  }

  private Platform() {}
}

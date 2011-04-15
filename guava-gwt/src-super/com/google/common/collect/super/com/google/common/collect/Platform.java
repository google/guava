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

/**
 * Minimal GWT emulation of {@code com.google.common.collect.Platform}.
 *
 * <p><strong>This .java file should never be consumed by javac.</strong>
 *
 * @author Hayward Chan
 */
class Platform {

  static <T> T[] clone(T[] array) {
    return GwtPlatform.clone(array);
  }

  // TODO: Fix System.arraycopy in GWT so that it isn't necessary.
  static void unsafeArrayCopy(
      Object[] src, int srcPos, Object[] dest, int destPos, int length) {
    for (int i = 0; i < length; i++) {
      dest[destPos + i] = src[srcPos + i];
    }
  }

  static <T> T[] newArray(Class<T> type, int length) {
    throw new UnsupportedOperationException(
        "Platform.newArray is not supported in GWT yet.");
  }

  static <T> T[] newArray(T[] reference, int length) {
    return GwtPlatform.newArray(reference, length);
  }

  static MapMaker tryWeakKeys(MapMaker mapMaker) {
    return mapMaker;
  }
}

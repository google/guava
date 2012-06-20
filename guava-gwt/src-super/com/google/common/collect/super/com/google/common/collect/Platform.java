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

import com.google.common.collect.Maps.EntryTransformer;

import java.util.SortedMap;

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

  static <T> T[] newArray(T[] reference, int length) {
    return GwtPlatform.newArray(reference, length);
  }

  static MapMaker tryWeakKeys(MapMaker mapMaker) {
    return mapMaker;
  }

  static <K, V1, V2> SortedMap<K, V2> mapsTransformEntriesSortedMap(
      SortedMap<K, V1> fromMap,
      EntryTransformer<? super K, ? super V1, V2> transformer) {
    return Maps.transformEntriesIgnoreNavigable(fromMap, transformer);
  }
}

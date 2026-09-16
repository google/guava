/*
 * Copyright (C) 2026 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect.testing.testers;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.AbstractMapTester;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;

@GwtIncompatible
public class ConcurrentMapSpliteratorTester<K, V> extends AbstractMapTester<K, V> {

  public void testKeySetSpliterator() {
    Spliterator<K> spliterator = getMap().keySet().spliterator();
    assertTrue(
        "keySet spliterator should be CONCURRENT",
        spliterator.hasCharacteristics(Spliterator.CONCURRENT));
    assertFalse(
        "keySet spliterator should not be SIZED",
        spliterator.hasCharacteristics(Spliterator.SIZED));
    testStreamToList(getMap().keySet().stream());
  }

  public void testValuesSpliterator() {
    Spliterator<V> spliterator = getMap().values().spliterator();
    assertTrue(
        "values spliterator should be CONCURRENT",
        spliterator.hasCharacteristics(Spliterator.CONCURRENT));
    assertFalse(
        "values spliterator should not be SIZED",
        spliterator.hasCharacteristics(Spliterator.SIZED));
    testStreamToList(getMap().values().stream());
  }

  public void testEntrySetSpliterator() {
    Spliterator<java.util.Map.Entry<K, V>> spliterator = getMap().entrySet().spliterator();
    assertTrue(
        "entrySet spliterator should be CONCURRENT",
        spliterator.hasCharacteristics(Spliterator.CONCURRENT));
    assertFalse(
        "entrySet spliterator should not be SIZED",
        spliterator.hasCharacteristics(Spliterator.SIZED));
    testStreamToList(getMap().entrySet().stream());
  }

  private void testStreamToList(Stream<?> stream) {
    try {
      Method toList = Stream.class.getMethod("toList");
      List<?> list = (List<?>) toList.invoke(stream);
      assertEquals(getNumElements(), list.size());
    } catch (NoSuchMethodException e) {
      // Stream.toList() not available, just check toArray
      assertEquals(getNumElements(), stream.toArray().length);
    } catch (Exception e) {
      fail("Stream.toList() threw exception: " + e.getCause());
    }
  }
}

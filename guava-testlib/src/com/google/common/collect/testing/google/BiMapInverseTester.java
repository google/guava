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

package com.google.common.collect.testing.google;

import static com.google.common.collect.testing.features.CollectionFeature.SERIALIZABLE;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.BiMap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.testing.SerializableTester;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.junit.Ignore;

/**
 * Tests for the {@code inverse} view of a BiMap.
 *
 * <p>This assumes that {@code bimap.inverse().inverse() == bimap}, which is not technically
 * required but is fulfilled by all current implementations.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class BiMapInverseTester<K, V> extends AbstractBiMapTester<K, V> {

  public void testInverseSame() {
    assertSame(getMap(), getMap().inverse().inverse());
  }

  @CollectionFeature.Require(SERIALIZABLE)
  public void testInverseSerialization() {
    BiMapPair<K, V> pair = new BiMapPair<>(getMap());
    BiMapPair<K, V> copy = SerializableTester.reserialize(pair);
    assertEquals(pair.forward, copy.forward);
    assertEquals(pair.backward, copy.backward);
    assertSame(copy.backward, copy.forward.inverse());
    assertSame(copy.forward, copy.backward.inverse());
  }

  private static class BiMapPair<K, V> implements Serializable {
    final BiMap<K, V> forward;
    final BiMap<V, K> backward;

    BiMapPair(BiMap<K, V> original) {
      this.forward = original;
      this.backward = original.inverse();
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns {@link Method} instances for the tests that assume that the inverse will be the same
   * after serialization.
   */
  @GwtIncompatible // reflection
  public static List<Method> getInverseSameAfterSerializingMethods() {
    return Collections.singletonList(getMethod("testInverseSerialization"));
  }

  @GwtIncompatible // reflection
  private static Method getMethod(String methodName) {
    return Helpers.getMethod(BiMapInverseTester.class, methodName);
  }
}

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
import static com.google.common.testing.SerializableTester.reserialize;
import static java.util.Collections.singletonList;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.BiMap;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Tests for the {@code inverse} view of a BiMap.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class BiMapInverseTester<K, V> extends AbstractBiMapTester<K, V> {

  // View caching is not required but was historically provided by some of our implementations.
  public void testInverseSame() {
    assertSame(getMap(), getMap().inverse().inverse());
  }

  // View caching is not required but was historically provided by some of our implementations.
  @CollectionFeature.Require(SERIALIZABLE)
  public void testInverseSerialization() {
    BiMapPair<K, V> pair = new BiMapPair<>(getMap());
    BiMapPair<K, V> copy = reserialize(pair);
    assertEquals(pair.forward, copy.forward);
    assertEquals(pair.backward, copy.backward);
    assertSame(copy.backward, copy.forward.inverse());
    assertSame(copy.forward, copy.backward.inverse());
  }

  /**
   * @since 33.7.0
   */
  public void testInverseEquals() {
    assertEquals(getMap(), getMap().inverse().inverse());
  }

  /**
   * @since 33.7.0
   */
  @CollectionFeature.Require(SERIALIZABLE)
  public void testInverseSerializationEquals() {
    BiMapPair<K, V> pair = new BiMapPair<>(getMap());
    BiMapPair<K, V> copy = reserialize(pair);
    assertEquals(pair.forward, copy.forward);
    assertEquals(pair.backward, copy.backward);
    assertEquals(copy.backward, copy.forward.inverse());
    assertEquals(copy.forward, copy.backward.inverse());
  }

  private static final class BiMapPair<K, V> implements Serializable {
    final BiMap<K, V> forward;
    final BiMap<V, K> backward;

    BiMapPair(BiMap<K, V> original) {
      this.forward = original;
      this.backward = original.inverse();
    }

    @GwtIncompatible @J2ktIncompatible private static final long serialVersionUID = 0;
  }

  /**
   * Returns {@link Method} instance for {@link #testInverseSame()} so that tests can suppress it
   * with {@code FeatureSpecificTestSuiteBuilder.suppressing()}.
   *
   * @since 33.7.0
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static Method getInverseSameMethod() {
    return getMethod("testInverseSame");
  }

  /**
   * Returns {@link Method} instances for the tests that assume that the inverse will be the same
   * after serialization.
   */
  @J2ktIncompatible
  @GwtIncompatible // reflection
  public static List<Method> getInverseSameAfterSerializingMethods() {
    return singletonList(getMethod("testInverseSerialization"));
  }

  @J2ktIncompatible
  @GwtIncompatible // reflection
  private static Method getMethod(String methodName) {
    return Helpers.getMethod(BiMapInverseTester.class, methodName);
  }
}

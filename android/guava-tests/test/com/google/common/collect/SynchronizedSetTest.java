/*
 * Copyright (C) 2007 The Guava Authors
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


import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for {@code Synchronized#set}.
 *
 * @author Mike Bostock
 */
@NullUnmarked
@AndroidIncompatible // test-suite builders
public class SynchronizedSetTest extends TestCase {

  public static final Object MUTEX = new Object[0]; // something Serializable

  public static Test suite() {
    return SetTestSuiteBuilder.using(
            new TestStringSetGenerator() {
              @Override
              protected Set<String> create(String[] elements) {
                LockHeldAssertingSet<String> inner =
                    new LockHeldAssertingSet<>(new HashSet<String>(), MUTEX);
                Set<String> outer = Synchronized.set(inner, inner.mutex);
                Collections.addAll(outer, elements);
                return outer;
              }
            })
        .named("Synchronized.set")
        .withFeatures(
            CollectionFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionSize.ANY,
            CollectionFeature.SERIALIZABLE)
        .createTestSuite();
  }
}

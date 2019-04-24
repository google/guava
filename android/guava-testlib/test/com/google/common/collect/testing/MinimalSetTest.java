/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.collect.testing;

import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Unit test for {@link MinimalSet}.
 *
 * @author Regina O'Dell
 */
public class MinimalSetTest extends TestCase {
  public static Test suite() {
    return SetTestSuiteBuilder.using(
            new TestStringSetGenerator() {
              @Override
              protected Set<String> create(String[] elements) {
                return MinimalSet.of(elements);
              }
            })
        .named("MinimalSet")
        .withFeatures(
            CollectionFeature.ALLOWS_NULL_VALUES, CollectionFeature.NONE, CollectionSize.ANY)
        .createTestSuite();
  }
}

/*
 * Copyright (C) 2010 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import junit.framework.Test;
import junit.framework.TestCase;

import java.util.List;

@GwtCompatible
public class TransformedImmutableListTest extends TestCase {
  @GwtIncompatible("suite")
  public static Test suite() {
    return ListTestSuiteBuilder.using(new TestStringListGenerator() {

      @SuppressWarnings("serial")
      @Override protected List<String> create(String[] elements) {
        return new TransformedImmutableList<String, String>(
            ImmutableList.copyOf(elements)) {

          @Override String transform(String str) {
            return str;
          }
        };
      }
    }).named("TransformedImmutableList identity").withFeatures(
        CollectionSize.ANY,
        CollectionFeature.ALLOWS_NULL_QUERIES).createTestSuite();
  }
}

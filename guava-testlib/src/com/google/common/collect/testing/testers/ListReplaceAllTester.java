/*
 * Copyright (C) 2015 The Guava Authors
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

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.features.CollectionSize.ZERO;
import static com.google.common.collect.testing.features.ListFeature.SUPPORTS_SET;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import java.util.Collections;
import java.util.List;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@link List#replaceAll}. Can't be invoked directly; please see
 * {@link com.google.common.collect.testing.ListTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class ListReplaceAllTester<E> extends AbstractListTester<E> {
  @ListFeature.Require(SUPPORTS_SET)
  public void testReplaceAll() {
    getList().replaceAll(e -> samples.e3());
    expectContents(Collections.nCopies(getNumElements(), samples.e3()));
  }

  @ListFeature.Require(SUPPORTS_SET)
  public void testReplaceAll_changesSome() {
    getList().replaceAll(e -> e.equals(samples.e0()) ? samples.e3() : e);
    E[] expected = createSamplesArray();
    for (int i = 0; i < expected.length; i++) {
      if (expected[i].equals(samples.e0())) {
        expected[i] = samples.e3();
      }
    }
    expectContents(expected);
  }

  @CollectionSize.Require(absent = ZERO)
  @ListFeature.Require(absent = SUPPORTS_SET)
  public void testReplaceAll_unsupported() {
    try {
      getList().replaceAll(e -> e);
      fail("replaceAll() should throw UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
    }
    expectUnchanged();
  }
}

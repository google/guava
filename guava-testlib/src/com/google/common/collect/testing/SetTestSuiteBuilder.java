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

package com.google.common.collect.testing;

import com.google.common.collect.testing.testers.SetAddAllTester;
import com.google.common.collect.testing.testers.SetAddTester;
import com.google.common.collect.testing.testers.SetCreationTester;
import com.google.common.collect.testing.testers.SetEqualsTester;
import com.google.common.collect.testing.testers.SetHashCodeTester;
import com.google.common.collect.testing.testers.SetRemoveTester;

import java.util.List;

/**
 * Creates, based on your criteria, a JUnit test suite that exhaustively tests
 * a Set implementation.
 *
 * @author George van den Driessche
 */
public class SetTestSuiteBuilder<E>
    extends AbstractCollectionTestSuiteBuilder<SetTestSuiteBuilder<E>, E> {
  public static <E> SetTestSuiteBuilder<E> using(
      TestSetGenerator<E> generator) {
    return new SetTestSuiteBuilder<E>().usingGenerator(generator);
  }

  @Override protected List<Class<? extends AbstractTester>> getTesters() {
    List<Class<? extends AbstractTester>> testers
        = Helpers.copyToList(super.getTesters());

    testers.add(SetAddAllTester.class);
    testers.add(SetAddTester.class);
    testers.add(SetCreationTester.class);
    testers.add(SetHashCodeTester.class);
    testers.add(SetEqualsTester.class);
    testers.add(SetRemoveTester.class);
    // SetRemoveAllTester doesn't exist because, Sets not permitting
    // duplicate elements, there are no tests for Set.removeAll() that aren't
    // covered by CollectionRemoveAllTester.
    return testers;
  }
}

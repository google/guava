/*
 * Copyright (C) 2011 The Guava Authors
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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Ignore;

/** Support class added to a suite as part of {@link FeatureSpecificTestSuiteBuilderTest}. */
/*
 * @Ignore affects the Android test runner (and only the Android test runner): It respects JUnit 4
 * annotations even on JUnit 3 tests.
 *
 * TODO(b/225350400): Remove @Ignore, which doesn't seem like it should be necessary and probably
 * soon won't be.
 */
@SuppressWarnings("JUnit4ClassUsedInJUnit3")
@Ignore
public final class MyTester extends AbstractTester<@Nullable Void> {
  static int timesTestClassWasRun = 0;

  public void testNothing() {
    timesTestClassWasRun++;
  }
}

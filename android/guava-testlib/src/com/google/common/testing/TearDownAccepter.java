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

package com.google.common.testing;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.DoNotMock;

/**
 * Any object which can accept registrations of {@link TearDown} instances.
 *
 * @author Kevin Bourrillion
 * @since 10.0
 */
@DoNotMock("Implement with a lambda")
@GwtCompatible
@ElementTypesAreNonnullByDefault
public interface TearDownAccepter {
  /**
   * Registers a TearDown implementor which will be run after the test proper.
   *
   * <p>In JUnit4 language, that means as an {@code @After}.
   *
   * <p>In JUnit3 language, that means during the {@link junit.framework.TestCase#tearDown()} step.
   */
  void addTearDown(TearDown tearDown);
}

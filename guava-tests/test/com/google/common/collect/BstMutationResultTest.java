/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.collect.BstTesting.defaultNullPointerTester;

import com.google.common.annotations.GwtIncompatible;

import junit.framework.TestCase;

/**
 * Tests for {@code BstMutationResult}.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible("NullPointerTester")
public class BstMutationResultTest extends TestCase {
  public void testNullPointers() throws Exception {
    defaultNullPointerTester().testAllPublicStaticMethods(BstMutationResult.class);
  }
}

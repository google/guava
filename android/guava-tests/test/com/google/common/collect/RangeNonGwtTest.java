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

package com.google.common.collect;

import com.google.common.testing.NullPointerTester;
import junit.framework.TestCase;

/**
 * Test cases for {@link Range} which cannot run as GWT tests.
 *
 * @author Gregory Kick
 * @see RangeTest
 */
public class RangeNonGwtTest extends TestCase {

  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();

    tester.testAllPublicStaticMethods(Range.class);
    tester.testAllPublicStaticMethods(Range.class);

    tester.testAllPublicInstanceMethods(Range.all());
    tester.testAllPublicInstanceMethods(Range.open(1, 3));
  }
}

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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Suite of tests for OpenJdk 6 tests. The existence of this class is a hack because the
 * suitebuilder won't pick up the suites directly in the other classes because they don't extend
 * TestCase. Ergh.
 *
 * @author Kevin Bourrillion
 */
public class OpenJdk6Tests extends TestCase {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(OpenJdk6SetTests.suite());
    suite.addTest(OpenJdk6ListTests.suite());
    suite.addTest(OpenJdk6QueueTests.suite());
    suite.addTest(OpenJdk6MapTests.suite());
    return suite;
  }
}

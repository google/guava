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

import static com.google.common.collect.testing.testers.CollectionCreationTester.getCreateWithNullUnsupportedMethod;
import static com.google.common.collect.testing.testers.MapCreationTester.getCreateWithNullKeyUnsupportedMethod;
import static com.google.common.collect.testing.testers.MapEntrySetTester.getContainsEntryWithIncomparableKeyMethod;
import static com.google.common.collect.testing.testers.MapEntrySetTester.getContainsEntryWithIncomparableValueMethod;
import static com.google.common.collect.testing.testers.MapPutAllTester.getPutAllNullKeyUnsupportedMethod;
import static com.google.common.collect.testing.testers.MapPutTester.getPutNullKeyUnsupportedMethod;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Tests the {@link Map} implementations of {@link java.util}, suppressing
 * tests that trip known OpenJDK 6 bugs.
 *
 * @author Kevin Bourrillion
 */
public class OpenJdk6MapTests extends TestsForMapsInJavaUtil {
  public static Test suite() {
    return new OpenJdk6MapTests().allTests();
  }

  @Override protected Collection<Method> suppressForTreeMapNatural() {
    return Arrays.asList(
        getPutNullKeyUnsupportedMethod(),
        getPutAllNullKeyUnsupportedMethod(),
        getCreateWithNullKeyUnsupportedMethod(),
        getCreateWithNullUnsupportedMethod(), // for keySet
        getContainsEntryWithIncomparableKeyMethod(),
        getContainsEntryWithIncomparableValueMethod()); 
  }

  @Override
  protected Collection<Method> suppressForConcurrentSkipListMap() {
    return Arrays.asList(
        getContainsEntryWithIncomparableKeyMethod(),
        getContainsEntryWithIncomparableValueMethod()); 
  }

  @Override public Test testsForEnumMap() {
    // Do nothing.
    // TODO: work around the reused-entry problem
    // http://bugs.sun.com/view_bug.do?bug_id=6312706
    return new TestSuite();
  }
}

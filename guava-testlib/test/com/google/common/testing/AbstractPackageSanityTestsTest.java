/*
 * Copyright (C) 2012 The Guava Authors
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

import static org.junit.contrib.truth.Truth.ASSERT;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link AbstractPackageSanityTests}.
 *
 * @author Ben Yu
 */
public class AbstractPackageSanityTestsTest extends TestCase {

  public void testFindClassesToTest_testClass() {
    ASSERT.that(findClassesToTest(ImmutableList.of(EmptyTest.class)))
        .isEmpty();
    ASSERT.that(findClassesToTest(ImmutableList.of(EmptyTests.class)))
        .isEmpty();
    ASSERT.that(findClassesToTest(ImmutableList.of(EmptyTestCase.class)))
        .isEmpty();
    ASSERT.that(findClassesToTest(ImmutableList.of(EmptyTestSuite.class)))
        .isEmpty();
  }

  public void testFindClassesToTest_noCorrespondingTestClass() {
    ASSERT.that(findClassesToTest(ImmutableList.of(Foo.class)))
        .hasContentsInOrder(Foo.class);
    ASSERT.that(findClassesToTest(ImmutableList.of(Foo.class, Foo2Test.class)))
        .hasContentsInOrder(Foo.class);
  }

  public void testFindClassesToTest_withCorrespondingTestClassButNotExplicitlyTested() {
    ASSERT.that(findClassesToTest(ImmutableList.of(Foo.class, FooTest.class), "testNotThere"))
        .hasContentsInOrder(Foo.class);
    ASSERT.that(findClassesToTest(ImmutableList.of(Foo.class, FooTest.class), "testNotPublic"))
        .hasContentsInOrder(Foo.class);
  }

  public void testFindClassesToTest_withCorrespondingTestClassAndExplicitlyTested() {
    ImmutableList<Class<? extends Object>> classes = ImmutableList.of(Foo.class, FooTest.class);
    ASSERT.that(findClassesToTest(classes, "testPublic"))
        .isEmpty();
    ASSERT.that(findClassesToTest(classes, "testNotThere", "testPublic"))
        .isEmpty();
  }

  public void testFindClassesToTest_withCorrespondingTestClass_noTestName() {
    ASSERT.that(findClassesToTest(ImmutableList.of(Foo.class, FooTest.class)))
        .hasContentsInOrder(Foo.class);
  }

  private static class EmptyTestCase {}

  private static class EmptyTest {}

  private static class EmptyTests {}

  private static class EmptyTestSuite {}

  private static class Foo {}

  private static class FooTest {
    @SuppressWarnings("unused") // accessed reflectively
    public void testPublic() {}
    @SuppressWarnings("unused") // accessed reflectively
    void testNotPublic() {}
  }

  // Shouldn't be mistaken as Foo's test
  private static class Foo2Test {
    @SuppressWarnings("unused") // accessed reflectively
    public void testPublic() {}
  }

  private static List<Class<?>> findClassesToTest(
      Iterable<? extends Class<?>> classes, String... explicitTestNames) {
    return AbstractPackageSanityTests.findClassesToTest(classes, Arrays.asList(explicitTestNames));
  }
}

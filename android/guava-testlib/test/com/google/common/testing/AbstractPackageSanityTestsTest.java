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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

/**
 * Unit tests for {@link AbstractPackageSanityTests}.
 *
 * @author Ben Yu
 */
public class AbstractPackageSanityTestsTest extends TestCase {
  /*
   * This is a public type so that the Android test runner can create an instance directly as it
   * insists upon doing. It then runs the test, which behaves exactly like this package's existing
   * PackageSanityTests. (The test would run on the JVM, too, if not for the suppression below, and
   * that would be a problem because it violates small-test rules. Note that we strip the
   * suppression externally, but it's OK because we don't enforce test-size rules there.)
   *
   * We'd just use PackageSanityTests directly, saving us from needing this separate type, but we're
   * currently skipping MediumTests on Android, and we skip them by not making them present at
   * runtime at all. I could just make _this_ test a MediumTest, but then it wouldn't run on
   * Android.... The right long-term fix is probably to get MediumTests running under Android by
   * default and then suppress them strategically as needed.
   */
  public static final class ConcretePackageSanityTests extends AbstractPackageSanityTests {}

  private final AbstractPackageSanityTests sanityTests = new ConcretePackageSanityTests();

  public void testFindClassesToTest_testClass() {
    assertThat(findClassesToTest(ImmutableList.of(EmptyTest.class))).isEmpty();
    assertThat(findClassesToTest(ImmutableList.of(EmptyTests.class))).isEmpty();
    assertThat(findClassesToTest(ImmutableList.of(EmptyTestCase.class))).isEmpty();
    assertThat(findClassesToTest(ImmutableList.of(EmptyTestSuite.class))).isEmpty();
  }

  public void testFindClassesToTest_noCorrespondingTestClass() {
    assertThat(findClassesToTest(ImmutableList.of(Foo.class))).containsExactly(Foo.class);
    assertThat(findClassesToTest(ImmutableList.of(Foo.class, Foo2Test.class)))
        .containsExactly(Foo.class);
  }

  public void testFindClassesToTest_publicApiOnly() {
    sanityTests.publicApiOnly();
    assertThat(findClassesToTest(ImmutableList.of(Foo.class))).isEmpty();
    assertThat(findClassesToTest(ImmutableList.of(PublicFoo.class))).contains(PublicFoo.class);
  }

  public void testFindClassesToTest_ignoreClasses() {
    sanityTests.ignoreClasses(Predicates.<Object>equalTo(PublicFoo.class));
    assertThat(findClassesToTest(ImmutableList.of(PublicFoo.class))).isEmpty();
    assertThat(findClassesToTest(ImmutableList.of(Foo.class))).contains(Foo.class);
  }

  public void testFindClassesToTest_ignoreUnderscores() {
    assertThat(findClassesToTest(ImmutableList.of(Foo.class, Foo_Bar.class)))
        .containsExactly(Foo.class, Foo_Bar.class);
    sanityTests.ignoreClasses(AbstractPackageSanityTests.UNDERSCORE_IN_NAME);
    assertThat(findClassesToTest(ImmutableList.of(Foo.class, Foo_Bar.class)))
        .containsExactly(Foo.class);
  }

  public void testFindClassesToTest_withCorrespondingTestClassButNotExplicitlyTested() {
    assertThat(findClassesToTest(ImmutableList.of(Foo.class, FooTest.class), "testNotThere"))
        .containsExactly(Foo.class);
    assertThat(findClassesToTest(ImmutableList.of(Foo.class, FooTest.class), "testNotPublic"))
        .containsExactly(Foo.class);
  }

  public void testFindClassesToTest_withCorrespondingTestClassAndExplicitlyTested() {
    ImmutableList<Class<?>> classes = ImmutableList.of(Foo.class, FooTest.class);
    assertThat(findClassesToTest(classes, "testPublic")).isEmpty();
    assertThat(findClassesToTest(classes, "testNotThere", "testPublic")).isEmpty();
  }

  public void testFindClassesToTest_withCorrespondingTestClass_noTestName() {
    assertThat(findClassesToTest(ImmutableList.of(Foo.class, FooTest.class)))
        .containsExactly(Foo.class);
  }

  static class EmptyTestCase {}

  static class EmptyTest {}

  static class EmptyTests {}

  static class EmptyTestSuite {}

  static class Foo {}

  static class Foo_Bar {}

  public static class PublicFoo {}

  static class FooTest {
    @SuppressWarnings("unused") // accessed reflectively
    public void testPublic() {}

    @SuppressWarnings("unused") // accessed reflectively
    void testNotPublic() {}
  }

  // Shouldn't be mistaken as Foo's test
  static class Foo2Test {
    @SuppressWarnings("unused") // accessed reflectively
    public void testPublic() {}
  }

  private List<Class<?>> findClassesToTest(
      Iterable<? extends Class<?>> classes, String... explicitTestNames) {
    return sanityTests.findClassesToTest(classes, Arrays.asList(explicitTestNames));
  }
}

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

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This abstract base class for testers allows the framework to inject needed information after
 * JUnit constructs the instances.
 *
 * <p>This class is emulated in GWT.
 *
 * @param <G> the type of the test generator required by this tester. An instance of G should
 *     somehow provide an instance of the class under test, plus any other information required to
 *     parameterize the test.
 * @author George van den Driessche
 */
@GwtCompatible(emulated = true)
@ElementTypesAreNonnullByDefault
public class AbstractTester<G> extends TestCase {
  private G subjectGenerator;
  private String suiteName;
  private @Nullable Runnable setUp;
  private @Nullable Runnable tearDown;

  // public so that it can be referenced in generated GWT tests.
  @Override
  public void setUp() throws Exception {
    if (setUp != null) {
      setUp.run();
    }
  }

  // public so that it can be referenced in generated GWT tests.
  @Override
  public void tearDown() throws Exception {
    if (tearDown != null) {
      tearDown.run();
    }
  }

  // public so that it can be referenced in generated GWT tests.
  public final void init(
      G subjectGenerator, String suiteName, @Nullable Runnable setUp, @Nullable Runnable tearDown) {
    this.subjectGenerator = subjectGenerator;
    this.suiteName = suiteName;
    this.setUp = setUp;
    this.tearDown = tearDown;
  }

  // public so that it can be referenced in generated GWT tests.
  public final void init(G subjectGenerator, String suiteName) {
    init(subjectGenerator, suiteName, null, null);
  }

  public G getSubjectGenerator() {
    return subjectGenerator;
  }

  /** Returns the name of the test method invoked by this test instance. */
  @J2ktIncompatible
  @GwtIncompatible // not used under GWT, and super.getName() is not available under J2CL
  public final String getTestMethodName() {
    return super.getName();
  }

  @J2ktIncompatible
  @GwtIncompatible // not used under GWT, and super.getName() is not available under J2CL
  @Override
  public String getName() {
    return Platform.format("%s[%s]", super.getName(), suiteName);
  }

  /**
   * Asserts that the given object is non-null, with a better failure message than {@link
   * TestCase#assertNull(String, Object)}.
   *
   * <p>The {@link TestCase} version (which is from JUnit 3) produces a failure message that does
   * not include the value of the object.
   *
   * @since NEXT
   */
  public static void assertNull(String message, Object object) {
    assertEquals(message, null, object);
  }
}

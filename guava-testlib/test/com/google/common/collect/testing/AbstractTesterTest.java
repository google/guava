/*
 * Copyright (C) 2026 The Guava Authors
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import org.jspecify.annotations.Nullable;

/** Tests the diagnostic context attached to generated test failures. */
@GwtIncompatible // JUnit 3 lifecycle
@J2ktIncompatible // JUnit 3 lifecycle
public final class AbstractTesterTest extends TestCase {
  public void testFailureKeepsIdentityAndExistingContext() {
    AssertionError failure = new AssertionError("original failure");
    Throwable previous = new IllegalStateException("previous context");
    failure.addSuppressed(previous);
    FailingTester tester = new FailingTester(failure);
    tester.init(new NamedGenerator(), "user suite");
    AssertionError actual = assertThrows(AssertionError.class, tester::runBare);
    assertSame(failure, actual);
    assertSame(previous, actual.getSuppressed()[0]);
    assertThat(actual.getSuppressed()).hasLength(2);
    assertThat(actual.getSuppressed()[1].getMessage()).contains("user suite");
    assertThat(actual.getSuppressed()[1].getMessage()).contains(NamedGenerator.class.getName());
    assertThat(actual.getSuppressed()[1].getStackTrace()).isEmpty();
  }

  public void testUnwrapsDerivedAndSizeGenerators() {
    NamedGenerator original = new NamedGenerator();
    FailingTester tester = new FailingTester(new AssertionError("failure"));
    tester.init(
        new WrappedGenerator(new OneSizeGenerator<>(original, CollectionSize.SEVERAL)), "suite");
    AssertionError failure = assertThrows(AssertionError.class, tester::runBare);
    assertThat(failure.getSuppressed()).hasLength(1);
    assertThat(failure.getSuppressed()[0].getMessage()).contains(NamedGenerator.class.getName());
  }

  public void testCyclicGeneratorDoesNotMaskFailure() {
    FailingTester tester = new FailingTester(new AssertionError("failure"));
    WrappedGenerator cyclic =
        new WrappedGenerator(new OneSizeGenerator<>(new NamedGenerator(), CollectionSize.SEVERAL));
    cyclic.inner = cyclic;
    tester.init(cyclic, "suite");
    AssertionError failure = assertThrows(AssertionError.class, tester::runBare);
    assertThat(failure.getSuppressed()).hasLength(1);
    assertThat(failure.getSuppressed()[0].getMessage()).contains(WrappedGenerator.class.getName());
  }

  public void testUninitializedTesterDoesNotMaskFailure() {
    AssertionError failure = new AssertionError("failure");
    FailingTester tester = new FailingTester(failure);
    assertSame(failure, assertThrows(AssertionError.class, tester::runBare));
    assertThat(failure.getSuppressed()).isEmpty();
  }

  public void testDiagnosticFailureDoesNotMaskAssertion() {
    AssertionError original = new AssertionError("original failure");
    FailingTester tester = new FailingTester(original);
    WrappedGenerator generator =
        new WrappedGenerator(new OneSizeGenerator<>(new NamedGenerator(), CollectionSize.SEVERAL));
    generator.throwOnUnwrap = true;
    tester.init(generator, "suite");
    assertSame(original, assertThrows(AssertionError.class, tester::runBare));
    assertThat(original.getSuppressed()).hasLength(1);
    assertThat(original.getSuppressed()[0]).isInstanceOf(IllegalStateException.class);
  }

  public void testNonAssertionFailureIsUnchanged() {
    IllegalStateException original = new IllegalStateException("failure");
    FailingTester tester = new FailingTester(original);
    tester.init(new NamedGenerator(), "suite");
    assertSame(original, assertThrows(IllegalStateException.class, tester::runBare));
    assertThat(original.getSuppressed()).isEmpty();
  }

  public void testDiagnosticAssertionDoesNotMaskOriginal() {
    AssertionError original = new AssertionError("original failure");
    FailingTester tester = new FailingTester(original);
    WrappedGenerator generator = new WrappedGenerator(null);
    generator.errorOnUnwrap = new AssertionError("metadata failure");
    tester.init(generator, "suite");
    assertSame(original, assertThrows(AssertionError.class, tester::runBare));
    assertSame(generator.errorOnUnwrap, original.getSuppressed()[0]);
  }

  public void testDiagnosticCannotSuppressOriginalIntoItself() {
    AssertionError original = new AssertionError("original failure");
    FailingTester tester = new FailingTester(original);
    WrappedGenerator generator = new WrappedGenerator(null);
    generator.errorOnUnwrap = original;
    tester.init(generator, "suite");
    assertSame(original, assertThrows(AssertionError.class, tester::runBare));
    assertThat(original.getSuppressed()).isEmpty();
  }

  public void testNullInnerGeneratorStillProvidesContext() {
    FailingTester tester = new FailingTester(new AssertionError("failure"));
    tester.init(new WrappedGenerator(null), "suite");
    AssertionError failure = assertThrows(AssertionError.class, tester::runBare);
    assertThat(failure.getSuppressed()).hasLength(1);
    assertThat(failure.getSuppressed()[0].getMessage()).contains(WrappedGenerator.class.getName());
  }

  public void testFreshWrapperChainHasBoundedTraversal() {
    int[] calls = {0};
    FailingTester tester = new FailingTester(new AssertionError("failure"));
    tester.init(new FreshWrapper(calls), "suite");
    AssertionError failure = assertThrows(AssertionError.class, tester::runBare);
    assertTrue(calls[0] <= 32);
    assertThat(failure.getSuppressed()).hasLength(1);
    assertThat(failure.getSuppressed()[0].getMessage()).contains(FreshWrapper.class.getName());
  }

  public void testSuccessfulTestStillRunsTearDown() throws Throwable {
    boolean[] tornDown = {false};
    FailingTester tester = new FailingTester(null);
    tester.init(new NamedGenerator(), "suite", null, () -> tornDown[0] = true);
    tester.runBare();
    assertTrue(tornDown[0]);
  }

  private static final class FailingTester extends AbstractTester<Object> {
    private final @Nullable Throwable failure;

    FailingTester(@Nullable Throwable failure) {
      this.failure = failure;
    }

    @Override
    protected void runTest() throws Throwable {
      if (failure != null) {
        throw failure;
      }
    }
  }

  private static final class NamedGenerator extends TestStringListGenerator {
    @Override
    protected List<String> create(String[] elements) {
      return Arrays.asList(elements);
    }
  }

  private static final class WrappedGenerator
      implements DerivedGenerator, TestSubjectGenerator<Object> {
    @Nullable TestSubjectGenerator<?> inner;
    boolean throwOnUnwrap;
    @Nullable Error errorOnUnwrap;

    WrappedGenerator(@Nullable TestSubjectGenerator<?> inner) {
      this.inner = inner;
    }

    @Override
    public TestSubjectGenerator<?> getInnerGenerator() {
      if (throwOnUnwrap) {
        throw new IllegalStateException("bad generator");
      }
      if (errorOnUnwrap != null) {
        throw errorOnUnwrap;
      }
      return inner;
    }

    @Override
    public Object createTestSubject() {
      if (inner == null) {
        throw new IllegalStateException("no generator");
      }
      return inner.createTestSubject();
    }
  }

  private static final class FreshWrapper
      implements DerivedGenerator, TestSubjectGenerator<Object> {
    private final int[] calls;

    FreshWrapper(int[] calls) {
      this.calls = calls;
    }

    @Override
    public TestSubjectGenerator<?> getInnerGenerator() {
      if (++calls[0] > 100) {
        throw new IllegalStateException("unbounded traversal");
      }
      return new FreshWrapper(calls);
    }

    @Override
    public Object createTestSubject() {
      return new Object();
    }
  }
}

/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.base;

import static com.google.common.base.ReflectionFreeAssertThrows.assertThrows;
import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;
import static com.google.common.base.Throwables.getCausalChain;
import static com.google.common.base.Throwables.getCauseAs;
import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.base.Throwables.lazyStackTrace;
import static com.google.common.base.Throwables.lazyStackTraceIsLazy;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.base.Throwables.propagateIfInstanceOf;
import static com.google.common.base.Throwables.propagateIfPossible;
import static com.google.common.base.Throwables.throwIfInstanceOf;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static com.google.common.truth.Truth.assertThat;
import static java.util.regex.Pattern.quote;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.TestExceptions.SomeChainingException;
import com.google.common.base.TestExceptions.SomeCheckedException;
import com.google.common.base.TestExceptions.SomeError;
import com.google.common.base.TestExceptions.SomeOtherCheckedException;
import com.google.common.base.TestExceptions.SomeUncheckedException;
import com.google.common.base.TestExceptions.YetAnotherCheckedException;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import com.google.common.testing.NullPointerTester;
import java.util.List;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Unit test for {@link Throwables}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("deprecation") // tests of numerous deprecated methods
@NullUnmarked
public class ThrowablesTest extends TestCase {
  // We're testing that the method is in fact equivalent to throwing the exception directly.
  @SuppressWarnings("ThrowIfUncheckedKnownUnchecked")
  public void testThrowIfUnchecked_unchecked() {
    assertThrows(
        SomeUncheckedException.class, () -> throwIfUnchecked(new SomeUncheckedException()));
  }

  // We're testing that the method is in fact equivalent to throwing the exception directly.
  @SuppressWarnings("ThrowIfUncheckedKnownUnchecked")
  public void testThrowIfUnchecked_error() {
    assertThrows(SomeError.class, () -> throwIfUnchecked(new SomeError()));
  }

  @SuppressWarnings("ThrowIfUncheckedKnownChecked")
  public void testThrowIfUnchecked_checked() {
    throwIfUnchecked(new SomeCheckedException());
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfPossible
  // We're testing that the method is in fact equivalent to throwing the exception directly.
  @SuppressWarnings("ThrowIfUncheckedKnownUnchecked")
  public void testPropagateIfPossible_noneDeclared_unchecked() {
    assertThrows(
        SomeUncheckedException.class, () -> propagateIfPossible(new SomeUncheckedException()));
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfPossible
  @SuppressWarnings("ThrowIfUncheckedKnownChecked")
  public void testPropagateIfPossible_noneDeclared_checked() {
    propagateIfPossible(new SomeCheckedException());
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfPossible(Throwable, Class)
  public void testPropagateIfPossible_oneDeclared_unchecked() {
    assertThrows(
        SomeUncheckedException.class,
        () -> propagateIfPossible(new SomeUncheckedException(), SomeCheckedException.class));
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfPossible(Throwable, Class)
  public void testPropagateIfPossible_oneDeclared_checkedSame() {
    assertThrows(
        SomeCheckedException.class,
        () -> propagateIfPossible(new SomeCheckedException(), SomeCheckedException.class));
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfPossible(Throwable, Class)
  public void testPropagateIfPossible_oneDeclared_checkedDifferent() throws SomeCheckedException {
    propagateIfPossible(new SomeOtherCheckedException(), SomeCheckedException.class);
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfPossible(Throwable, Class, Class)
  public void testPropagateIfPossible_twoDeclared_unchecked() {
    assertThrows(
        SomeUncheckedException.class,
        () ->
            propagateIfPossible(
                new SomeUncheckedException(),
                SomeCheckedException.class,
                SomeOtherCheckedException.class));
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfPossible(Throwable, Class, Class)
  public void testPropagateIfPossible_twoDeclared_firstSame() {
    assertThrows(
        SomeCheckedException.class,
        () ->
            propagateIfPossible(
                new SomeCheckedException(),
                SomeCheckedException.class,
                SomeOtherCheckedException.class));
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfPossible(Throwable, Class, Class)
  public void testPropagateIfPossible_twoDeclared_secondSame() {
    assertThrows(
        SomeOtherCheckedException.class,
        () ->
            propagateIfPossible(
                new SomeOtherCheckedException(),
                SomeCheckedException.class,
                SomeOtherCheckedException.class));
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfPossible(Throwable, Class, Class)
  public void testPropagateIfPossible_twoDeclared_neitherSame()
      throws SomeCheckedException, SomeOtherCheckedException {
    propagateIfPossible(
        new YetAnotherCheckedException(),
        SomeCheckedException.class,
        SomeOtherCheckedException.class);
  }

  // I guess it's technically a bug that ThrowIfUncheckedKnownUnchecked fires here.
  @SuppressWarnings("ThrowIfUncheckedKnownUnchecked")
  public void testThrowIfUnchecked_null() {
    assertThrows(NullPointerException.class, () -> throwIfUnchecked(null));
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfPossible
  // I guess it's technically a bug that ThrowIfUncheckedKnownUnchecked fires here.
  @SuppressWarnings("ThrowIfUncheckedKnownUnchecked")
  public void testPropageIfPossible_null() {
    propagateIfPossible(null);
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfPossible(Throwable, Class)
  public void testPropageIfPossible_oneDeclared_null() throws SomeCheckedException {
    propagateIfPossible(null, SomeCheckedException.class);
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfPossible(Throwable, Class, Class)
  public void testPropageIfPossible_twoDeclared_null()
      throws SomeCheckedException, SomeOtherCheckedException {
    propagateIfPossible(null, SomeCheckedException.class, SomeOtherCheckedException.class);
  }

  @J2ktIncompatible
  @GwtIncompatible // propagate
  public void testPropagate_noneDeclared_unchecked() {
    assertThrows(SomeUncheckedException.class, () -> propagate(new SomeUncheckedException()));
  }

  @J2ktIncompatible
  @GwtIncompatible // propagate
  public void testPropagate_noneDeclared_error() {
    assertThrows(SomeError.class, () -> propagate(new SomeError()));
  }

  @J2ktIncompatible
  @GwtIncompatible // propagate
  public void testPropagate_noneDeclared_checked() {
    RuntimeException expected =
        assertThrows(RuntimeException.class, () -> propagate(new SomeCheckedException()));
    assertThat(expected).hasCauseThat().isInstanceOf(SomeCheckedException.class);
  }

  @GwtIncompatible // throwIfInstanceOf
  public void testThrowIfInstanceOf_unchecked() throws SomeCheckedException {
    throwIfInstanceOf(new SomeUncheckedException(), SomeCheckedException.class);
  }

  @GwtIncompatible // throwIfInstanceOf
  public void testThrowIfInstanceOf_checkedDifferent() throws SomeCheckedException {
    throwIfInstanceOf(new SomeOtherCheckedException(), SomeCheckedException.class);
  }

  @GwtIncompatible // throwIfInstanceOf
  public void testThrowIfInstanceOf_checkedSame() {
    assertThrows(
        SomeCheckedException.class,
        () -> throwIfInstanceOf(new SomeCheckedException(), SomeCheckedException.class));
  }

  @GwtIncompatible // throwIfInstanceOf
  public void testThrowIfInstanceOf_checkedSubclass() {
    assertThrows(
        SomeCheckedException.class,
        () -> throwIfInstanceOf(new SomeCheckedException() {}, SomeCheckedException.class));
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfInstanceOf
  public void testPropagateIfInstanceOf_checkedSame() {
    assertThrows(
        SomeCheckedException.class,
        () -> propagateIfInstanceOf(new SomeCheckedException(), SomeCheckedException.class));
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfInstanceOf
  public void testPropagateIfInstanceOf_unchecked() throws SomeCheckedException {
    propagateIfInstanceOf(new SomeUncheckedException(), SomeCheckedException.class);
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfInstanceOf
  public void testPropagateIfInstanceOf_checkedDifferent() throws SomeCheckedException {
    propagateIfInstanceOf(new SomeOtherCheckedException(), SomeCheckedException.class);
  }

  @GwtIncompatible // throwIfInstanceOf
  public void testThrowIfInstanceOf_null() {
    assertThrows(
        NullPointerException.class, () -> throwIfInstanceOf(null, SomeCheckedException.class));
  }

  @J2ktIncompatible
  @GwtIncompatible // propagateIfInstanceOf
  public void testPropageIfInstanceOf_null() throws SomeCheckedException {
    propagateIfInstanceOf(null, SomeCheckedException.class);
  }

  public void testGetRootCause_noCause() {
    SomeCheckedException exception = new SomeCheckedException();
    assertSame(exception, getRootCause(exception));
  }

  public void testGetRootCause_singleWrapped() {
    SomeCheckedException cause = new SomeCheckedException();
    SomeChainingException exception = new SomeChainingException(cause);
    assertSame(cause, getRootCause(exception));
  }

  public void testGetRootCause_doubleWrapped() {
    SomeCheckedException cause = new SomeCheckedException();
    SomeChainingException exception = new SomeChainingException(new SomeChainingException(cause));
    assertSame(cause, getRootCause(exception));
  }

  public void testGetRootCause_loop() {
    Exception cause = new Exception();
    Exception exception = new Exception(cause);
    cause.initCause(exception);
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> getRootCause(cause));
    assertThat(expected).hasCauseThat().isSameInstanceAs(cause);
  }

  @J2ktIncompatible // Format does not match
  @GwtIncompatible // getStackTraceAsString(Throwable)
  public void testGetStackTraceAsString() {
    class StackTraceException extends Exception {
      StackTraceException(String message) {
        super(message);
      }
    }

    StackTraceException e = new StackTraceException("my message");

    String firstLine = quote(e.getClass().getName() + ": " + e.getMessage());
    String secondLine = "\\s*at " + ThrowablesTest.class.getName() + "\\..*";
    String moreLines = "(?:.*" + System.lineSeparator() + "?)*";
    String expected =
        firstLine + System.lineSeparator() + secondLine + System.lineSeparator() + moreLines;
    assertThat(getStackTraceAsString(e)).matches(expected);
  }

  public void testGetCausalChain() {
    SomeUncheckedException sue = new SomeUncheckedException();
    IllegalArgumentException iae = new IllegalArgumentException(sue);
    RuntimeException re = new RuntimeException(iae);
    IllegalStateException ex = new IllegalStateException(re);

    assertThat(getCausalChain(ex)).containsExactly(ex, re, iae, sue).inOrder();
    assertSame(sue, Iterables.getOnlyElement(getCausalChain(sue)));

    List<Throwable> causes = getCausalChain(ex);
    assertThrows(UnsupportedOperationException.class, () -> causes.add(new RuntimeException()));
  }

  public void testGetCasualChainNull() {
    assertThrows(NullPointerException.class, () -> getCausalChain(null));
  }

  public void testGetCasualChainLoop() {
    Exception cause = new Exception();
    Exception exception = new Exception(cause);
    cause.initCause(exception);
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> getCausalChain(cause));
    assertThat(expected).hasCauseThat().isSameInstanceAs(cause);
  }

  @GwtIncompatible // getCauseAs(Throwable, Class)
  public void testGetCauseAs() {
    SomeCheckedException cause = new SomeCheckedException();
    SomeChainingException thrown = new SomeChainingException(cause);

    assertThat(thrown).hasCauseThat().isSameInstanceAs(cause);
    assertThat(getCauseAs(thrown, SomeCheckedException.class)).isSameInstanceAs(cause);
    assertThat(getCauseAs(thrown, Exception.class)).isSameInstanceAs(cause);

    ClassCastException expected =
        assertThrows(
            ClassCastException.class, () -> getCauseAs(thrown, IllegalStateException.class));
    assertThat(expected).hasCauseThat().isSameInstanceAs(thrown);
  }

  @AndroidIncompatible // No getJavaLangAccess in Android (at least not in the version we use).
  @J2ktIncompatible
  @GwtIncompatible // lazyStackTraceIsLazy()
  public void testLazyStackTraceWorksInProd() {
    // TODO(b/64442212): Remove this guard once lazyStackTrace() works in Java 9+.
    Integer javaVersion = Ints.tryParse(JAVA_SPECIFICATION_VERSION.value());
    if (javaVersion != null && javaVersion >= 9) {
      return;
    }
    // Obviously this isn't guaranteed in every environment, but it works well enough for now:
    assertTrue(lazyStackTraceIsLazy());
  }

  @J2ktIncompatible
  @GwtIncompatible // lazyStackTrace(Throwable)
  public void testLazyStackTrace() {
    Exception e = new Exception();
    StackTraceElement[] originalStackTrace = e.getStackTrace();

    assertThat(lazyStackTrace(e)).containsExactly((Object[]) originalStackTrace).inOrder();

    assertThrows(UnsupportedOperationException.class, () -> lazyStackTrace(e).set(0, null));

    // Now we test a property that holds only for the lazy implementation.

    if (!lazyStackTraceIsLazy()) {
      return;
    }

    e.setStackTrace(new StackTraceElement[0]);
    assertThat(lazyStackTrace(e)).containsExactly((Object[]) originalStackTrace).inOrder();
  }

  @J2ktIncompatible
  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    new NullPointerTester().testAllPublicStaticMethods(Throwables.class);
  }
}

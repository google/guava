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

package com.google.common.testing.anotherpackage;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Ordering;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.google.common.testing.ForwardingWrapperTester;
import com.google.common.testing.NullPointerTester;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import junit.framework.AssertionFailedError;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

/**
 * Tests for {@link ForwardingWrapperTester}. Live in a different package to detect reflection
 * access issues, if any.
 *
 * @author Ben Yu
 */
public class ForwardingWrapperTesterTest {

  private final ForwardingWrapperTester tester = new ForwardingWrapperTester();

  @Test
  public void goodForwarder() {
    tester.testForwarding(Arithmetic.class, ForwardingArithmetic::new);
    tester.testForwarding(ParameterTypesDifferent.class, ParameterTypesDifferentForwarder::new);
  }

  @Test
  public void voidMethodForwarding() {
    tester.testForwarding(Runnable.class, ForwardingRunnable::new);
  }

  @Test
  public void toStringForwarding() {
    tester.testForwarding(
        Runnable.class,
        runnable ->
            new ForwardingRunnable(runnable) {
              @Override
              public String toString() {
                return runnable.toString();
              }
            });
  }

  @Test
  public void failsToForwardToString() {
    assertFailure(
        Runnable.class,
        runnable ->
            new ForwardingRunnable(runnable) {
              @Override
              public String toString() {
                return "";
              }
            },
        "toString()");
  }

  @Test
  public void failsToForwardHashCode() {
    tester.includingEquals();
    assertFailure(
        Runnable.class,
        runnable ->
            new ForwardingRunnable(runnable) {

              @SuppressWarnings("EqualsHashCode")
              @Override
              public boolean equals(@Nullable Object o) {
                if (o instanceof ForwardingRunnable) {
                  ForwardingRunnable that = (ForwardingRunnable) o;
                  return runnable.equals(that.runnable);
                }
                return false;
              }
            },
        "Runnable");
  }

  @Test
  public void equalsAndHashCodeForwarded() {
    tester.includingEquals();
    tester.testForwarding(
        Runnable.class,
        runnable ->
            new ForwardingRunnable(runnable) {
              @Override
              public boolean equals(@Nullable Object o) {
                if (o instanceof ForwardingRunnable) {
                  ForwardingRunnable that = (ForwardingRunnable) o;
                  return runnable.equals(that.runnable);
                }
                return false;
              }

              @Override
              public int hashCode() {
                return runnable.hashCode();
              }
            });
  }

  @Test
  public void failsToForwardEquals() {
    tester.includingEquals();
    assertFailure(
        Runnable.class,
        runnable ->
            new ForwardingRunnable(runnable) {
              @Override
              public int hashCode() {
                return runnable.hashCode();
              }
            },
        "Runnable");
  }

  @Test
  public void failsToForward() {
    assertFailure(
        Runnable.class,
        runnable ->
            new ForwardingRunnable(runnable) {
              @Override
              public void run() {}
            },
        "run()",
        "Failed to forward");
  }

  @Test
  public void redundantForwarding() {
    assertFailure(
        Runnable.class,
        runnable ->
            () -> {
              runnable.run();
              runnable.run();
            },
        "run()",
        "invoked more than once");
  }

  @Test
  public void failsToForwardParameters() {
    assertFailure(Adder.class, FailsToForwardParameters::new, "add(", "Parameter #0");
  }

  @Test
  public void forwardsToTheWrongMethod() {
    assertFailure(Arithmetic.class, ForwardsToTheWrongMethod::new, "minus");
  }

  @Test
  public void failsToForwardReturnValue() {
    assertFailure(Adder.class, FailsToForwardReturnValue::new, "add(", "Return value");
  }

  @Test
  public void failsToPropagateException() {
    assertFailure(Adder.class, FailsToPropagateException::new, "add(", "exception");
  }

  @Test
  public void notInterfaceType() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ForwardingWrapperTester().testForwarding(String.class, Functions.identity()));
  }

  @Test
  public void nulls() {
    new NullPointerTester()
        .setDefault(Class.class, Runnable.class)
        .testAllPublicInstanceMethods(new ForwardingWrapperTester());
  }

  private <T> void assertFailure(
      Class<T> interfaceType,
      Function<T, ? extends T> wrapperFunction,
      String... expectedMessages) {
    AssertionFailedError expected =
        assertThrows(
            AssertionFailedError.class,
            () -> tester.testForwarding(interfaceType, wrapperFunction));
    for (String message : expectedMessages) {
      assertThat(expected).hasMessageThat().contains(message);
    }
  }

  private static class ForwardingRunnable implements Runnable {

    private final Runnable runnable;

    ForwardingRunnable(Runnable runnable) {
      this.runnable = runnable;
    }

    @Override
    public void run() {
      runnable.run();
    }

    @Override
    public String toString() {
      return runnable.toString();
    }
  }

  private interface Adder {
    int add(int a, int b);
  }

  private static final class ForwardingArithmetic implements Arithmetic {
    private final Arithmetic arithmetic;

    ForwardingArithmetic(Arithmetic arithmetic) {
      this.arithmetic = arithmetic;
    }

    @Override
    public int add(int a, int b) {
      return arithmetic.add(a, b);
    }

    @Override
    public int minus(int a, int b) {
      return arithmetic.minus(a, b);
    }

    @Override
    public String toString() {
      return arithmetic.toString();
    }
  }

  private static final class FailsToForwardParameters implements Adder {
    private final Adder adder;

    FailsToForwardParameters(Adder adder) {
      this.adder = adder;
    }

    @Override
    public int add(int a, int b) {
      return adder.add(b, a);
    }

    @Override
    public String toString() {
      return adder.toString();
    }
  }

  private static final class FailsToForwardReturnValue implements Adder {
    private final Adder adder;

    FailsToForwardReturnValue(Adder adder) {
      this.adder = adder;
    }

    @Override
    public int add(int a, int b) {
      return adder.add(a, b) + 1;
    }

    @Override
    public String toString() {
      return adder.toString();
    }
  }

  private static final class FailsToPropagateException implements Adder {
    private final Adder adder;

    FailsToPropagateException(Adder adder) {
      this.adder = adder;
    }

    @Override
    @SuppressWarnings("CatchingUnchecked") // sneaky checked exception
    public int add(int a, int b) {
      try {
        return adder.add(a, b);
      } catch (Exception e) { // sneaky checked exception
        // swallow!
        return 0;
      }
    }

    @Override
    public String toString() {
      return adder.toString();
    }
  }

  private interface Arithmetic extends Adder {
    int minus(int a, int b);
  }

  private static final class ForwardsToTheWrongMethod implements Arithmetic {
    private final Arithmetic arithmetic;

    ForwardsToTheWrongMethod(Arithmetic arithmetic) {
      this.arithmetic = arithmetic;
    }

    @Override
    public int minus(int a, int b) { // bad!
      return arithmetic.add(a, b);
    }

    @Override
    public int add(int a, int b) {
      return arithmetic.add(a, b);
    }

    @Override
    public String toString() {
      return arithmetic.toString();
    }
  }

  private interface ParameterTypesDifferent {
    void foo(
        String s,
        Runnable r,
        Number n,
        Iterable<?> it,
        boolean b,
        Equivalence<String> eq,
        Exception e,
        InputStream in,
        Comparable<?> c,
        Ordering<Integer> ord,
        Charset charset,
        TimeUnit unit,
        Class<?> cls,
        Joiner joiner,
        Pattern pattern,
        UnsignedInteger ui,
        UnsignedLong ul,
        StringBuilder sb,
        Predicate<?> pred,
        Function<?, ?> func,
        Object obj);
  }

  private static class ParameterTypesDifferentForwarder implements ParameterTypesDifferent {
    private final ParameterTypesDifferent delegate;

    ParameterTypesDifferentForwarder(ParameterTypesDifferent delegate) {
      this.delegate = delegate;
    }

    @Override
    public void foo(
        String s,
        Runnable r,
        Number n,
        Iterable<?> it,
        boolean b,
        Equivalence<String> eq,
        Exception e,
        InputStream in,
        Comparable<?> c,
        Ordering<Integer> ord,
        Charset charset,
        TimeUnit unit,
        Class<?> cls,
        Joiner joiner,
        Pattern pattern,
        UnsignedInteger ui,
        UnsignedLong ul,
        StringBuilder sb,
        Predicate<?> pred,
        Function<?, ?> func,
        Object obj) {
      delegate.foo(
          s, r, n, it, b, eq, e, in, c, ord, charset, unit, cls, joiner, pattern, ui, ul, sb, pred,
          func, obj);
    }

    @Override
    public String toString() {
      return delegate.toString();
    }
  }

  @Test
  public void covariantReturn() {
    new ForwardingWrapperTester().testForwarding(Sub.class, ForwardingSub::new);
  }

  interface Base {
    CharSequence getId();
  }

  interface Sub extends Base {
    @Override
    String getId();
  }

  private static final class ForwardingSub implements Sub {
    private final Sub delegate;

    ForwardingSub(Sub delegate) {
      this.delegate = delegate;
    }

    @Override
    public String getId() {
      return delegate.getId();
    }

    @Override
    public String toString() {
      return delegate.toString();
    }
  }

  private interface Equals {
    @Override
    boolean equals(@Nullable Object obj);

    @Override
    int hashCode();

    @Override
    String toString();
  }

  private static class NoDelegateToEquals implements Equals {

    private final Equals delegate;

    NoDelegateToEquals(Equals delegate) {
      this.delegate = delegate;
    }

    @Override
    public String toString() {
      return delegate.toString();
    }
  }

  @Test
  public void explicitEqualsAndHashCodeNotDelegatedByDefault() {
    new ForwardingWrapperTester().testForwarding(Equals.class, NoDelegateToEquals::new);
  }

  @Test
  public void explicitEqualsAndHashCodeDelegatedWhenExplicitlyAsked() {
    try {
      new ForwardingWrapperTester()
          .includingEquals()
          .testForwarding(Equals.class, NoDelegateToEquals::new);
    } catch (AssertionFailedError expected) {
      return;
    }
    fail("Should have failed");
  }

  /** An interface for the 2 ways that a chaining call might be defined. */
  private interface ChainingCalls {
    // A method that is defined to 'return this'
    @CanIgnoreReturnValue
    ChainingCalls chainingCall();

    // A method that just happens to return a ChainingCalls object
    ChainingCalls nonChainingCall();
  }

  private static final class ForwardingChainingCalls implements ChainingCalls {
    final ChainingCalls delegate;

    ForwardingChainingCalls(ChainingCalls delegate) {
      this.delegate = delegate;
    }

    @CanIgnoreReturnValue
    @Override
    public ForwardingChainingCalls chainingCall() {
      delegate.chainingCall();
      return this;
    }

    @Override
    public ChainingCalls nonChainingCall() {
      return delegate.nonChainingCall();
    }

    @Override
    public String toString() {
      return delegate.toString();
    }
  }

  @Test
  public void chainingCalls() {
    tester.testForwarding(ChainingCalls.class, ForwardingChainingCalls::new);
  }
}

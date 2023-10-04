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
import junit.framework.TestCase;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tests for {@link ForwardingWrapperTester}. Live in a different package to detect reflection
 * access issues, if any.
 *
 * @author Ben Yu
 */
public class ForwardingWrapperTesterTest extends TestCase {

  private final ForwardingWrapperTester tester = new ForwardingWrapperTester();

  public void testGoodForwarder() {
    tester.testForwarding(
        Arithmetic.class,
        new Function<Arithmetic, Arithmetic>() {
          @Override
          public Arithmetic apply(Arithmetic arithmetic) {
            return new ForwardingArithmetic(arithmetic);
          }
        });
    tester.testForwarding(
        ParameterTypesDifferent.class,
        new Function<ParameterTypesDifferent, ParameterTypesDifferent>() {
          @Override
          public ParameterTypesDifferent apply(ParameterTypesDifferent delegate) {
            return new ParameterTypesDifferentForwarder(delegate);
          }
        });
  }

  public void testVoidMethodForwarding() {
    tester.testForwarding(
        Runnable.class,
        new Function<Runnable, Runnable>() {
          @Override
          public Runnable apply(final Runnable runnable) {
            return new ForwardingRunnable(runnable);
          }
        });
  }

  public void testToStringForwarding() {
    tester.testForwarding(
        Runnable.class,
        new Function<Runnable, Runnable>() {
          @Override
          public Runnable apply(final Runnable runnable) {
            return new ForwardingRunnable(runnable) {
              @Override
              public String toString() {
                return runnable.toString();
              }
            };
          }
        });
  }

  public void testFailsToForwardToString() {
    assertFailure(
        Runnable.class,
        new Function<Runnable, Runnable>() {
          @Override
          public Runnable apply(final Runnable runnable) {
            return new ForwardingRunnable(runnable) {
              @Override
              public String toString() {
                return "";
              }
            };
          }
        },
        "toString()");
  }

  public void testFailsToForwardHashCode() {
    tester.includingEquals();
    assertFailure(
        Runnable.class,
        new Function<Runnable, Runnable>() {
          @Override
          public Runnable apply(final Runnable runnable) {
            return new ForwardingRunnable(runnable) {

              @SuppressWarnings("EqualsHashCode")
              @Override
              public boolean equals(@Nullable Object o) {
                if (o instanceof ForwardingRunnable) {
                  ForwardingRunnable that = (ForwardingRunnable) o;
                  return runnable.equals(that.runnable);
                }
                return false;
              }
            };
          }
        },
        "Runnable");
  }

  public void testEqualsAndHashCodeForwarded() {
    tester.includingEquals();
    tester.testForwarding(
        Runnable.class,
        new Function<Runnable, Runnable>() {
          @Override
          public Runnable apply(final Runnable runnable) {
            return new ForwardingRunnable(runnable) {
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
            };
          }
        });
  }

  public void testFailsToForwardEquals() {
    tester.includingEquals();
    assertFailure(
        Runnable.class,
        new Function<Runnable, Runnable>() {
          @Override
          public Runnable apply(final Runnable runnable) {
            return new ForwardingRunnable(runnable) {
              @Override
              public int hashCode() {
                return runnable.hashCode();
              }
            };
          }
        },
        "Runnable");
  }

  public void testFailsToForward() {
    assertFailure(
        Runnable.class,
        new Function<Runnable, Runnable>() {
          @Override
          public Runnable apply(Runnable runnable) {
            return new ForwardingRunnable(runnable) {
              @Override
              public void run() {}
            };
          }
        },
        "run()",
        "Failed to forward");
  }

  public void testRedundantForwarding() {
    assertFailure(
        Runnable.class,
        new Function<Runnable, Runnable>() {
          @Override
          public Runnable apply(final Runnable runnable) {
            return new Runnable() {
              @Override
              public void run() {
                runnable.run();
                runnable.run();
              }
            };
          }
        },
        "run()",
        "invoked more than once");
  }

  public void testFailsToForwardParameters() {
    assertFailure(
        Adder.class,
        new Function<Adder, Adder>() {
          @Override
          public Adder apply(Adder adder) {
            return new FailsToForwardParameters(adder);
          }
        },
        "add(",
        "Parameter #0");
  }

  public void testForwardsToTheWrongMethod() {
    assertFailure(
        Arithmetic.class,
        new Function<Arithmetic, Arithmetic>() {
          @Override
          public Arithmetic apply(Arithmetic adder) {
            return new ForwardsToTheWrongMethod(adder);
          }
        },
        "minus");
  }

  public void testFailsToForwardReturnValue() {
    assertFailure(
        Adder.class,
        new Function<Adder, Adder>() {
          @Override
          public Adder apply(Adder adder) {
            return new FailsToForwardReturnValue(adder);
          }
        },
        "add(",
        "Return value");
  }

  public void testFailsToPropagateException() {
    assertFailure(
        Adder.class,
        new Function<Adder, Adder>() {
          @Override
          public Adder apply(Adder adder) {
            return new FailsToPropagateException(adder);
          }
        },
        "add(",
        "exception");
  }

  public void testNotInterfaceType() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ForwardingWrapperTester()
                .testForwarding(String.class, Functions.<String>identity()));
  }

  public void testNulls() {
    new NullPointerTester()
        .setDefault(Class.class, Runnable.class)
        .testAllPublicInstanceMethods(new ForwardingWrapperTester());
  }

  private <T> void assertFailure(
      Class<T> interfaceType,
      Function<T, ? extends T> wrapperFunction,
      String... expectedMessages) {
    try {
      tester.testForwarding(interfaceType, wrapperFunction);
    } catch (AssertionFailedError expected) {
      for (String message : expectedMessages) {
        assertThat(expected.getMessage()).contains(message);
      }
      return;
    }
    fail("expected failure not reported");
  }

  private class ForwardingRunnable implements Runnable {

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

  private static class ForwardingArithmetic implements Arithmetic {
    private final Arithmetic arithmetic;

    public ForwardingArithmetic(Arithmetic arithmetic) {
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

  private static class FailsToForwardParameters implements Adder {
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

  private static class FailsToForwardReturnValue implements Adder {
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

  private static class FailsToPropagateException implements Adder {
    private final Adder adder;

    FailsToPropagateException(Adder adder) {
      this.adder = adder;
    }

    @Override
    public int add(int a, int b) {
      try {
        return adder.add(a, b);
      } catch (Exception e) {
        // swallow!
        return 0;
      }
    }

    @Override
    public String toString() {
      return adder.toString();
    }
  }

  public interface Arithmetic extends Adder {
    int minus(int a, int b);
  }

  private static class ForwardsToTheWrongMethod implements Arithmetic {
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

    public ParameterTypesDifferentForwarder(ParameterTypesDifferent delegate) {
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

  public void testCovariantReturn() {
    new ForwardingWrapperTester()
        .testForwarding(
            Sub.class,
            new Function<Sub, Sub>() {
              @Override
              public Sub apply(Sub sub) {
                return new ForwardingSub(sub);
              }
            });
  }

  interface Base {
    CharSequence getId();
  }

  interface Sub extends Base {
    @Override
    String getId();
  }

  private static class ForwardingSub implements Sub {
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

    private static Function<Equals, Equals> WRAPPER =
        new Function<Equals, Equals>() {
          @Override
          public NoDelegateToEquals apply(Equals delegate) {
            return new NoDelegateToEquals(delegate);
          }
        };

    private final Equals delegate;

    NoDelegateToEquals(Equals delegate) {
      this.delegate = delegate;
    }

    @Override
    public String toString() {
      return delegate.toString();
    }
  }

  public void testExplicitEqualsAndHashCodeNotDelegatedByDefault() {
    new ForwardingWrapperTester().testForwarding(Equals.class, NoDelegateToEquals.WRAPPER);
  }

  public void testExplicitEqualsAndHashCodeDelegatedWhenExplicitlyAsked() {
    try {
      new ForwardingWrapperTester()
          .includingEquals()
          .testForwarding(Equals.class, NoDelegateToEquals.WRAPPER);
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

  private static class ForwardingChainingCalls implements ChainingCalls {
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

  public void testChainingCalls() {
    tester.testForwarding(
        ChainingCalls.class,
        new Function<ChainingCalls, ChainingCalls>() {
          @Override
          public ChainingCalls apply(ChainingCalls delegate) {
            return new ForwardingChainingCalls(delegate);
          }
        });
  }
}

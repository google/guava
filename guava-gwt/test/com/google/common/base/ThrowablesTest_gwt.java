/*
 * Copyright (C) 2016 The Guava Authors
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

import com.google.common.collect.Iterables;

import java.util.List;

import static com.google.common.base.Throwables.throwIfUnchecked;
import static java.util.Arrays.asList;

public class ThrowablesTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
  @Override public String getModuleName() {
    return "com.google.common.base.testModule";
  }

  public void testThrowIfUnchecked_Unchecked() {
    try {
      throwIfUnchecked(new SomeUncheckedException());
      fail();
    } catch (SomeUncheckedException expected) {
    }
  }

  public void testThrowIfUnchecked_Error() {
    try {
      throwIfUnchecked(new SomeError());
      fail();
    } catch (SomeError expected) {
    }
  }

  public void testThrowIfUnchecked_Checked() {
    throwIfUnchecked(new SomeCheckedException());
  }

  public void testPropagateIfPossible_NoneDeclared_NoneThrown() {
    Sample sample = new Sample() {
      @Override public void noneDeclared() {
        try {
          methodThatDoesntThrowAnything();
        } catch (Throwable t) {
          Throwables.propagateIfPossible(t);
          throw new SomeChainingException(t);
        }
      }
    };

    // Expect no exception to be thrown
    sample.noneDeclared();
  }

  public void testPropagateIfPossible_NoneDeclared_UncheckedThrown() {
    Sample sample = new Sample() {
      @Override public void noneDeclared() {
        try {
          methodThatThrowsUnchecked();
        } catch (Throwable t) {
          Throwables.propagateIfPossible(t);
          throw new SomeChainingException(t);
        }
      }
    };

    // Expect the unchecked exception to propagate as-is
    try {
      sample.noneDeclared();
      fail();
    } catch (SomeUncheckedException expected) {
    }
  }

  public void testPropagateIfPossible_NoneDeclared_UndeclaredThrown() {
    Sample sample = new Sample() {
      @Override public void noneDeclared() {
        try {
          methodThatThrowsUndeclaredChecked();
        } catch (Throwable t) {
          Throwables.propagateIfPossible(t);
          throw new SomeChainingException(t);
        }
      }
    };

    // Expect the undeclared exception to have been chained inside another
    try {
      sample.noneDeclared();
      fail();
    } catch (SomeChainingException expected) {
    }
  }

  public void testThrowIfUnchecked_null() throws SomeCheckedException {
    try {
      throwIfUnchecked(null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testPropageIfPossible_null() throws SomeCheckedException {
    Throwables.propagateIfPossible(null);
  }

  public void testPropagate_NoneDeclared_NoneThrown() {
    Sample sample = new Sample() {
      @Override public void noneDeclared() {
        try {
          methodThatDoesntThrowAnything();
        } catch (Throwable t) {
          throw Throwables.propagate(t);
        }
      }
    };

    // Expect no exception to be thrown
    sample.noneDeclared();
  }

  public void testPropagate_NoneDeclared_UncheckedThrown() {
    Sample sample = new Sample() {
      @Override public void noneDeclared() {
        try {
          methodThatThrowsUnchecked();
        } catch (Throwable t) {
          throw Throwables.propagate(t);
        }
      }
    };

    // Expect the unchecked exception to propagate as-is
    try {
      sample.noneDeclared();
      fail();
    } catch (SomeUncheckedException expected) {
    }
  }

  public void testPropagate_NoneDeclared_ErrorThrown() {
    Sample sample = new Sample() {
      @Override public void noneDeclared() {
        try {
          methodThatThrowsError();
        } catch (Throwable t) {
          throw Throwables.propagate(t);
        }
      }
    };

    // Expect the error to propagate as-is
    try {
      sample.noneDeclared();
      fail();
    } catch (SomeError expected) {
    }
  }

  public void testPropagate_NoneDeclared_CheckedThrown() {
    Sample sample = new Sample() {
      @Override public void noneDeclared() {
        try {
          methodThatThrowsChecked();
        } catch (Throwable t) {
          throw Throwables.propagate(t);
        }
      }
    };

    // Expect the undeclared exception to have been chained inside another
    try {
      sample.noneDeclared();
      fail();
    } catch (RuntimeException expected) {
      assertTrue(expected.getCause() instanceof SomeCheckedException);
    }
  }

  public void testGetRootCause_NoCause() {
    SomeCheckedException exception = new SomeCheckedException();
    assertSame(exception, Throwables.getRootCause(exception));
  }

  public void testGetRootCause_SingleWrapped() {
    SomeCheckedException cause = new SomeCheckedException();
    SomeChainingException exception = new SomeChainingException(cause);
    assertSame(cause, Throwables.getRootCause(exception));
  }

  public void testGetRootCause_DoubleWrapped() {
    SomeCheckedException cause = new SomeCheckedException();
    SomeChainingException exception =
        new SomeChainingException(new SomeChainingException(cause));
    assertSame(cause, Throwables.getRootCause(exception));
  }

  private static class SomeError extends Error {}
  private static class SomeCheckedException extends Exception {}
  private static class SomeOtherCheckedException extends Exception {}
  private static class SomeUncheckedException extends RuntimeException {}
  private static class SomeUndeclaredCheckedException extends Exception {}
  private static class SomeChainingException extends RuntimeException {
    public SomeChainingException(Throwable cause) {
      super(cause);
    }
  }

  static class Sample {
    void noneDeclared() {}
    void oneDeclared() throws SomeCheckedException {}
    void twoDeclared() throws SomeCheckedException, SomeOtherCheckedException {}
  }

  static void methodThatDoesntThrowAnything() {}
  static void methodThatThrowsError() {
    throw new SomeError();
  }
  static void methodThatThrowsUnchecked() {
    throw new SomeUncheckedException();
  }
  static void methodThatThrowsChecked() throws SomeCheckedException {
    throw new SomeCheckedException();
  }
  static void methodThatThrowsOtherChecked() throws SomeOtherCheckedException {
    throw new SomeOtherCheckedException();
  }
  static void methodThatThrowsUndeclaredChecked()
      throws SomeUndeclaredCheckedException {
    throw new SomeUndeclaredCheckedException();
  }

  public void testGetCausalChain() {
    SomeUncheckedException sue = new SomeUncheckedException();
    IllegalArgumentException iae = new IllegalArgumentException(sue);
    RuntimeException re = new RuntimeException(iae);
    IllegalStateException ex = new IllegalStateException(re);

    assertEquals(asList(ex, re, iae, sue), Throwables.getCausalChain(ex));
    assertSame(sue, Iterables.getOnlyElement(Throwables.getCausalChain(sue)));

    List<Throwable> causes = Throwables.getCausalChain(ex);
    try {
      causes.add(new RuntimeException());
      fail("List should be unmodifiable");
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testGetCasualChainNull() {
    try {
      Throwables.getCausalChain(null);
      fail("Should have throw NPE");
    } catch (NullPointerException expected) {
    }
  }
}

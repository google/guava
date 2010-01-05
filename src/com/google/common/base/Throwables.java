/*
 * Copyright (C) 2007 Google Inc.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Static utility methods pertaining to instances of {@link Throwable}.
 *
 * @author Kevin Bourrillion
 * @author Ben Yu
 * @since 2009.09.15 <b>tentative</b>
 */
public final class Throwables {
  private Throwables() {}

  /**
   * Propagates {@code throwable} exactly as-is, if and only if it is an
   * instance of {@code declaredType}.  Example usage:
   * <pre>
   *   try {
   *     someMethodThatCouldThrowAnything();
   *   } catch (IKnowWhatToDoWithThisException e) {
   *     handle(e);
   *   } catch (Throwable t) {
   *     Throwables.propagateIfInstanceOf(t, IOException.class);
   *     Throwables.propagateIfInstanceOf(t, SQLException.class);
   *     throw Throwables.propagate(t);
   *   }
   * </pre>
   */
  public static <X extends Throwable> void propagateIfInstanceOf(
      Throwable throwable, Class<X> declaredType) throws X {
    if (declaredType.isInstance(throwable)) {
      throw declaredType.cast(throwable);
    }
  }

  /**
   * Propagates {@code throwable} exactly as-is, if and only if it is an
   * instance of {@link RuntimeException} or {@link Error}.  Example usage:
   * <pre>
   *   try {
   *     someMethodThatCouldThrowAnything();
   *   } catch (IKnowWhatToDoWithThisException e) {
   *     handle(e);
   *   } catch (Throwable t) {
   *     Throwables.propagateIfPossible(t);
   *     throw new RuntimeException("unexpected", t);
   *   }
   * </pre>
   */
  public static void propagateIfPossible(Throwable throwable) {
    propagateIfInstanceOf(throwable, Error.class);
    propagateIfInstanceOf(throwable, RuntimeException.class);
  }

  /**
   * Propagates {@code throwable} exactly as-is, if and only if it is an
   * instance of {@link RuntimeException}, {@link Error}, or
   * {@code declaredType}. Example usage:
   * <pre>
   *   try {
   *     someMethodThatCouldThrowAnything();
   *   } catch (IKnowWhatToDoWithThisException e) {
   *     handle(e);
   *   } catch (Throwable t) {
   *     Throwables.propagateIfPossible(t, OtherException.class);
   *     throw new RuntimeException("unexpected", t);
   *   }
   * </pre>
   *
   * @param throwable the Throwable to possibly propagate
   * @param declaredType the single checked exception type declared by the
   *     calling method
   */
  public static <X extends Throwable> void propagateIfPossible(
      Throwable throwable, Class<X> declaredType) throws X {
    propagateIfInstanceOf(throwable, declaredType);
    propagateIfPossible(throwable);
  }

  /**
   * Propagates {@code throwable} exactly as-is, if and only if it is an
   * instance of {@link RuntimeException}, {@link Error}, {@code aDeclaredType},
   * or {@code anotherDeclaredType}.  In the unlikely case that you have three
   * or more declared checked exception types, you can handle them all by
   * invoking these methods repeatedly. See usage example in
   * {@link #propagateIfPossible(Throwable, Class)}.
   *
   * @param throwable the Throwable to possibly propagate
   * @param aDeclaredType any checked exception type declared by the calling
   *     method
   * @param anotherDeclaredType any other checked exception type declared by the
   *     calling method
   */
  public static <X1 extends Throwable, X2 extends Throwable> void
      propagateIfPossible(Throwable throwable, Class<X1> aDeclaredType,
          Class<X2> anotherDeclaredType) throws X1, X2 {
    propagateIfInstanceOf(throwable, aDeclaredType);
    propagateIfPossible(throwable, anotherDeclaredType);
  }

  /**
   * Propagates {@code throwable} as-is if it is an instance of
   * {@link RuntimeException} or {@link Error}, or else as a last resort, wraps
   * it in a {@code RuntimeException} then propagates.
   * <p>
   * This method always throws an exception. The {@code RuntimeException} return
   * type is only for client code to make Java type system happy in case a
   * return value is required by the enclosing method. Example usage:
   * <pre>
   *   T doSomething() {
   *     try {
   *       return someMethodThatCouldThrowAnything();
   *     } catch (IKnowWhatToDoWithThisException e) {
   *       return handle(e);
   *     } catch (Throwable t) {
   *       throw Throwables.propagate(t);
   *     }
   *   }
   * </pre>
   *
   * @param throwable the Throwable to propagate
   * @return nothing will ever be returned
   */
  public static RuntimeException propagate(Throwable throwable) {
    propagateIfPossible(throwable);
    throw new RuntimeException(throwable);
  }

  /**
   * Returns the innermost cause of {@code throwable}. The first throwable in a
   * chain provides context from when the error or exception was initially
   * detected. Example usage:
   * <pre>
   *   assertEquals("Unable to assign a customer id",
   *       Throwables.getRootCause(e).getMessage());
   * </pre>
   */
  public static Throwable getRootCause(Throwable throwable) {
    Throwable cause;
    while ((cause = throwable.getCause()) != null) {
      throwable = cause;
    }
    return throwable;
  }

  /**
   * Gets a {@code Throwable} cause chain as a list.  The first entry in the
   * list will be {@code throwable} followed by its cause hierarchy.  Note
   * that this is a snapshot of the cause chain and will not reflect
   * any subsequent changes to the cause chain.
   *
   * <p>Here's an example of how it can be used to find specific types
   * of exceptions in the cause chain:
   *
   * <pre>
   * Iterables.filter(Throwables.getCausalChain(e), IOException.class));
   * </pre>
   *
   * @param throwable the non-null {@code Throwable} to extract causes from
   * @return an unmodifiable list containing the cause chain starting with
   *     {@code throwable}
   */
  public static List<Throwable> getCausalChain(Throwable throwable) {
    Preconditions.checkNotNull(throwable);
    List<Throwable> causes = new ArrayList<Throwable>(4);
    while (throwable != null) {
      causes.add(throwable);
      throwable = throwable.getCause();
    }
    return Collections.unmodifiableList(causes);
  }

  /**
   * Returns a string containing the result of
   * {@link Throwable#toString() toString()}, followed by the full, recursive
   * stack trace of {@code throwable}. Note that you probably should not be
   * parsing the resulting string; if you need programmatic access to the stack
   * frames, you can call {@link Throwable#getStackTrace()}.
   */
  public static String getStackTraceAsString(Throwable throwable) {
    StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }

  /**
   * Rethrows the cause exception of a given throwable, discarding the original
   * throwable. Optionally, the stack frames of the cause and the outer
   * exception are combined and the stack trace of the cause is set to this
   * combined trace. If there is no cause the original exception is rethrown
   * unchanged in all cases.
   *
   * @param exception the exception from which to extract the cause
   * @param combineStackTraces if true the stack trace of the cause will be
   *     replaced by the concatenation of the trace from the exception and the
   *     trace from the cause.
   */
  public static Exception throwCause(Exception exception, boolean combineStackTraces)
      throws Exception {
    Throwable cause = exception.getCause();
    if (cause == null) {
      throw exception;
    }
    if (combineStackTraces) {
      StackTraceElement[] causeTrace = cause.getStackTrace();
      StackTraceElement[] outerTrace = exception.getStackTrace();
      StackTraceElement[] combined = new StackTraceElement[causeTrace.length + outerTrace.length];
      System.arraycopy(causeTrace, 0, combined, 0, causeTrace.length);
      System.arraycopy(outerTrace, 0, combined, causeTrace.length, outerTrace.length);
      cause.setStackTrace(combined);
    }
    if (cause instanceof Exception) {
      throw (Exception) cause;
    }
    if (cause instanceof Error) {
      throw (Error) cause;
    }
    // The cause is a weird kind of Throwable, so throw the outer exception
    throw exception;
  }
}

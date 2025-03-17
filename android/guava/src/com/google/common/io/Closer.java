/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.io;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.throwIfInstanceOf;
import static com.google.common.base.Throwables.throwIfUnchecked;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Closeable} that collects {@code Closeable} resources and closes them all when it is
 * {@linkplain #close closed}. This was intended to approximately emulate the behavior of Java 7's
 * <a href="http://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html"
 * >try-with-resources</a> statement in JDK6-compatible code. Code using this should be
 * approximately equivalent in behavior to the same code written with try-with-resources.
 *
 * <p>This class is intended to be used in the following pattern:
 *
 * {@snippet :
 * Closer closer = Closer.create();
 * try {
 *   InputStream in = closer.register(openInputStream());
 *   OutputStream out = closer.register(openOutputStream());
 *   // do stuff
 * } catch (Throwable e) {
 *   // ensure that any checked exception types other than IOException that could be thrown are
 *   // provided here, e.g. throw closer.rethrow(e, CheckedException.class);
 *   throw closer.rethrow(e);
 * } finally {
 *   closer.close();
 * }
 * }
 *
 * <p>Note that this try-catch-finally block is not equivalent to a try-catch-finally block using
 * try-with-resources. To get the equivalent of that, you must wrap the above code in <i>another</i>
 * try block in order to catch any exception that may be thrown (including from the call to {@code
 * close()}).
 *
 * <p>This pattern ensures the following:
 *
 * <ul>
 *   <li>Each {@code Closeable} resource that is successfully registered will be closed later.
 *   <li>If a {@code Throwable} is thrown in the try block, no exceptions that occur when attempting
 *       to close resources will be thrown from the finally block. The throwable from the try block
 *       will be thrown.
 *   <li>If no exceptions or errors were thrown in the try block, the <i>first</i> exception thrown
 *       by an attempt to close a resource will be thrown.
 *   <li>Any exception caught when attempting to close a resource that is <i>not</i> thrown (because
 *       another exception is already being thrown) is <i>suppressed</i>.
 * </ul>
 *
 * <p>An exception that is suppressed is added to the exception that <i>will</i> be thrown using
 * {@code Throwable.addSuppressed(Throwable)}.
 *
 * @author Colin Decker
 * @since 14.0
 */
// Coffee's for {@link Closer closers} only.
@J2ktIncompatible
@GwtIncompatible
public final class Closer implements Closeable {
  /** Creates a new {@link Closer}. */
  public static Closer create() {
    return new Closer(SUPPRESSING_SUPPRESSOR);
  }

  @VisibleForTesting final Suppressor suppressor;

  // only need space for 2 elements in most cases, so try to use the smallest array possible
  private final Deque<Closeable> stack = new ArrayDeque<>(4);
  private @Nullable Throwable thrown;

  @VisibleForTesting
  Closer(Suppressor suppressor) {
    this.suppressor = checkNotNull(suppressor); // checkNotNull to satisfy null tests
  }

  /**
   * Registers the given {@code closeable} to be closed when this {@code Closer} is {@linkplain
   * #close closed}.
   *
   * @return the given {@code closeable}
   */
  // close. this word no longer has any meaning to me.
  @CanIgnoreReturnValue
  @ParametricNullness
  public <C extends @Nullable Closeable> C register(@ParametricNullness C closeable) {
    if (closeable != null) {
      stack.addFirst(closeable);
    }

    return closeable;
  }

  /**
   * Stores the given throwable and rethrows it. It will be rethrown as is if it is an {@code
   * IOException}, {@code RuntimeException} or {@code Error}. Otherwise, it will be rethrown wrapped
   * in a {@code RuntimeException}. <b>Note:</b> Be sure to declare all of the checked exception
   * types your try block can throw when calling an overload of this method so as to avoid losing
   * the original exception type.
   *
   * <p>This method always throws, and as such should be called as {@code throw closer.rethrow(e);}
   * to ensure the compiler knows that it will throw.
   *
   * @return this method does not return; it always throws
   * @throws IOException when the given throwable is an IOException
   */
  public RuntimeException rethrow(Throwable e) throws IOException {
    checkNotNull(e);
    thrown = e;
    throwIfInstanceOf(e, IOException.class);
    throwIfUnchecked(e);
    throw new RuntimeException(e);
  }

  /**
   * Stores the given throwable and rethrows it. It will be rethrown as is if it is an {@code
   * IOException}, {@code RuntimeException}, {@code Error} or a checked exception of the given type.
   * Otherwise, it will be rethrown wrapped in a {@code RuntimeException}. <b>Note:</b> Be sure to
   * declare all of the checked exception types your try block can throw when calling an overload of
   * this method so as to avoid losing the original exception type.
   *
   * <p>This method always throws, and as such should be called as {@code throw closer.rethrow(e,
   * ...);} to ensure the compiler knows that it will throw.
   *
   * @return this method does not return; it always throws
   * @throws IOException when the given throwable is an IOException
   * @throws X when the given throwable is of the declared type X
   */
  public <X extends Exception> RuntimeException rethrow(Throwable e, Class<X> declaredType)
      throws IOException, X {
    checkNotNull(e);
    thrown = e;
    throwIfInstanceOf(e, IOException.class);
    throwIfInstanceOf(e, declaredType);
    throwIfUnchecked(e);
    throw new RuntimeException(e);
  }

  /**
   * Stores the given throwable and rethrows it. It will be rethrown as is if it is an {@code
   * IOException}, {@code RuntimeException}, {@code Error} or a checked exception of either of the
   * given types. Otherwise, it will be rethrown wrapped in a {@code RuntimeException}. <b>Note:</b>
   * Be sure to declare all of the checked exception types your try block can throw when calling an
   * overload of this method so as to avoid losing the original exception type.
   *
   * <p>This method always throws, and as such should be called as {@code throw closer.rethrow(e,
   * ...);} to ensure the compiler knows that it will throw.
   *
   * @return this method does not return; it always throws
   * @throws IOException when the given throwable is an IOException
   * @throws X1 when the given throwable is of the declared type X1
   * @throws X2 when the given throwable is of the declared type X2
   */
  public <X1 extends Exception, X2 extends Exception> RuntimeException rethrow(
      Throwable e, Class<X1> declaredType1, Class<X2> declaredType2) throws IOException, X1, X2 {
    checkNotNull(e);
    thrown = e;
    throwIfInstanceOf(e, IOException.class);
    throwIfInstanceOf(e, declaredType1);
    throwIfInstanceOf(e, declaredType2);
    throwIfUnchecked(e);
    throw new RuntimeException(e);
  }

  /**
   * Closes all {@code Closeable} instances that have been added to this {@code Closer}. If an
   * exception was thrown in the try block and passed to one of the {@code exceptionThrown} methods,
   * any exceptions thrown when attempting to close a closeable will be suppressed. Otherwise, the
   * <i>first</i> exception to be thrown from an attempt to close a closeable will be thrown and any
   * additional exceptions that are thrown after that will be suppressed.
   */
  @Override
  public void close() throws IOException {
    Throwable throwable = thrown;

    // close closeables in LIFO order
    while (!stack.isEmpty()) {
      Closeable closeable = stack.removeFirst();
      try {
        closeable.close();
      } catch (Throwable e) {
        if (throwable == null) {
          throwable = e;
        } else {
          suppressor.suppress(closeable, throwable, e);
        }
      }
    }

    if (thrown == null && throwable != null) {
      throwIfInstanceOf(throwable, IOException.class);
      throwIfUnchecked(throwable);
      throw new AssertionError(throwable); // not possible
    }
  }

  /** Suppression strategy interface. */
  @VisibleForTesting
  interface Suppressor {
    /**
     * Suppresses the given exception ({@code suppressed}) which was thrown when attempting to close
     * the given closeable. {@code thrown} is the exception that is actually being thrown from the
     * method. Implementations of this method should not throw under any circumstances.
     */
    void suppress(Closeable closeable, Throwable thrown, Throwable suppressed);
  }

  /**
   * Suppresses exceptions by adding them to the exception that will be thrown using the
   * addSuppressed(Throwable) mechanism.
   */
  private static final Suppressor SUPPRESSING_SUPPRESSOR =
      (closeable, thrown, suppressed) -> {
        // ensure no exceptions from addSuppressed
        if (thrown == suppressed) {
          return;
        }
        try {
          thrown.addSuppressed(suppressed);
        } catch (Throwable e) {
          /*
           * A Throwable is very unlikely, but we really don't want to throw from a Suppressor, so
           * we catch everything. (Any Exception is either a RuntimeException or
           * sneaky checked exception.) With no better options, we log anything to the same
           * place as Closeables logs.
           */
          Closeables.logger.log(
              Level.WARNING, "Suppressing exception thrown when closing " + closeable, suppressed);
        }
      };
}

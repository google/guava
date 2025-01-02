/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.base;

import static com.google.common.base.Strings.lenientFormat;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jspecify.annotations.Nullable;

/**
 * Static convenience methods that serve the same purpose as Java language <a
 * href="https://docs.oracle.com/javase/8/docs/technotes/guides/language/assert.html">assertions</a>,
 * except that they are always enabled. These methods should be used instead of Java assertions
 * whenever there is a chance the check may fail "in real life". Example:
 *
 * <pre>{@code
 * Bill bill = remoteService.getLastUnpaidBill();
 *
 * // In case bug 12345 happens again we'd rather just die
 * Verify.verify(bill.status() == Status.UNPAID,
 *     "Unexpected bill status: %s", bill.status());
 * }</pre>
 *
 * <h3>Comparison to alternatives</h3>
 *
 * <p><b>Note:</b> In some cases the differences explained below can be subtle. When it's unclear
 * which approach to use, <b>don't worry</b> too much about it; just pick something that seems
 * reasonable and it will be fine.
 *
 * <ul>
 *   <li>If checking whether the <i>caller</i> has violated your method or constructor's contract
 *       (such as by passing an invalid argument), use the utilities of the {@link Preconditions}
 *       class instead.
 *   <li>If checking an <i>impossible</i> condition (which <i>cannot</i> happen unless your own
 *       class or its <i>trusted</i> dependencies is badly broken), this is what ordinary Java
 *       assertions are for. Note that assertions are not enabled by default; they are essentially
 *       considered "compiled comments."
 *   <li>An explicit {@code if/throw} (as illustrated below) is always acceptable; we still
 *       recommend using our {@link VerifyException} exception type. Throwing a plain {@link
 *       RuntimeException} is frowned upon.
 *   <li>Use of {@link java.util.Objects#requireNonNull(Object)} is generally discouraged, since
 *       {@link #verifyNotNull(Object)} and {@link Preconditions#checkNotNull(Object)} perform the
 *       same function with more clarity.
 * </ul>
 *
 * <h3>Warning about performance</h3>
 *
 * <p>Remember that parameter values for message construction must all be computed eagerly, and
 * autoboxing and varargs array creation may happen as well, even when the verification succeeds and
 * the message ends up unneeded. Performance-sensitive verification checks should continue to use
 * usual form:
 *
 * <pre>{@code
 * Bill bill = remoteService.getLastUnpaidBill();
 * if (bill.status() != Status.UNPAID) {
 *   throw new VerifyException("Unexpected bill status: " + bill.status());
 * }
 * }</pre>
 *
 * <h3>Only {@code %s} is supported</h3>
 *
 * <p>As with {@link Preconditions}, {@code Verify} uses {@link Strings#lenientFormat} to format
 * error message template strings. This only supports the {@code "%s"} specifier, not the full range
 * of {@link java.util.Formatter} specifiers. However, note that if the number of arguments does not
 * match the number of occurrences of {@code "%s"} in the format string, {@code Verify} will still
 * behave as expected, and will still include all argument values in the error message; the message
 * will simply not be formatted exactly as intended.
 *
 * <h3>More information</h3>
 *
 * See <a href="https://github.com/google/guava/wiki/ConditionalFailuresExplained">Conditional
 * failures explained</a> in the Guava User Guide for advice on when this class should be used.
 *
 * @since 17.0
 */
@GwtCompatible
public final class Verify {
  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with no
   * message otherwise.
   *
   * @throws VerifyException if {@code expression} is {@code false}
   * @see Preconditions#checkState Preconditions.checkState()
   */
  public static void verify(boolean expression) {
    if (!expression) {
      throw new VerifyException();
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * @param expression a boolean expression
   * @param errorMessageTemplate a template for the exception message should the check fail. The
   *     message is formed by replacing each {@code %s} placeholder in the template with an
   *     argument. These are matched by position - the first {@code %s} gets {@code
   *     errorMessageArgs[0]}, etc. Unmatched arguments will be appended to the formatted message in
   *     square braces. Unmatched placeholders will be left as-is.
   * @param errorMessageArgs the arguments to be substituted into the message template. Arguments
   *     are converted to strings using {@link String#valueOf(Object)}.
   * @throws VerifyException if {@code expression} is {@code false}
   * @see Preconditions#checkState Preconditions.checkState()
   */
  public static void verify(
      boolean expression,
      String errorMessageTemplate,
      @Nullable Object @Nullable ... errorMessageArgs) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, errorMessageArgs));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, char p1) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, int p1) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, long p1) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, @Nullable Object p1) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, char p1, char p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, int p1, char p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, long p1, char p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(
      boolean expression, String errorMessageTemplate, @Nullable Object p1, char p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, char p1, int p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, int p1, int p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, long p1, int p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(
      boolean expression, String errorMessageTemplate, @Nullable Object p1, int p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, char p1, long p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, int p1, long p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(boolean expression, String errorMessageTemplate, long p1, long p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(
      boolean expression, String errorMessageTemplate, @Nullable Object p1, long p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(
      boolean expression, String errorMessageTemplate, char p1, @Nullable Object p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(
      boolean expression, String errorMessageTemplate, int p1, @Nullable Object p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(
      boolean expression, String errorMessageTemplate, long p1, @Nullable Object p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(
      boolean expression, String errorMessageTemplate, @Nullable Object p1, @Nullable Object p2) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(
      boolean expression,
      String errorMessageTemplate,
      @Nullable Object p1,
      @Nullable Object p2,
      @Nullable Object p3) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2, p3));
    }
  }

  /**
   * Ensures that {@code expression} is {@code true}, throwing a {@code VerifyException} with a
   * custom message otherwise.
   *
   * <p>See {@link #verify(boolean, String, Object...)} for details.
   *
   * @since 23.1 (varargs overload since 17.0)
   */
  public static void verify(
      boolean expression,
      String errorMessageTemplate,
      @Nullable Object p1,
      @Nullable Object p2,
      @Nullable Object p3,
      @Nullable Object p4) {
    if (!expression) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, p1, p2, p3, p4));
    }
  }

  /*
   * For a discussion of the signature of verifyNotNull, see the discussion above
   * Preconditions.checkNotNull.
   *
   * (verifyNotNull has many fewer "problem" callers, so we could try to be stricter. On the other
   * hand, verifyNotNull arguably has more reason to accept nullable arguments in the first
   * place....)
   */

  /**
   * Ensures that {@code reference} is non-null, throwing a {@code VerifyException} with a default
   * message otherwise.
   *
   * @return {@code reference}, guaranteed to be non-null, for convenience
   * @throws VerifyException if {@code reference} is {@code null}
   * @see Preconditions#checkNotNull Preconditions.checkNotNull()
   */
  @CanIgnoreReturnValue
  public static <T> T verifyNotNull(@Nullable T reference) {
    return verifyNotNull(reference, "expected a non-null reference");
  }

  /**
   * Ensures that {@code reference} is non-null, throwing a {@code VerifyException} with a custom
   * message otherwise.
   *
   * @param errorMessageTemplate a template for the exception message should the check fail. The
   *     message is formed by replacing each {@code %s} placeholder in the template with an
   *     argument. These are matched by position - the first {@code %s} gets {@code
   *     errorMessageArgs[0]}, etc. Unmatched arguments will be appended to the formatted message in
   *     square braces. Unmatched placeholders will be left as-is.
   * @param errorMessageArgs the arguments to be substituted into the message template. Arguments
   *     are converted to strings using {@link String#valueOf(Object)}.
   * @return {@code reference}, guaranteed to be non-null, for convenience
   * @throws VerifyException if {@code reference} is {@code null}
   * @see Preconditions#checkNotNull Preconditions.checkNotNull()
   */
  @CanIgnoreReturnValue
  public static <T> T verifyNotNull(
      @Nullable T reference,
      String errorMessageTemplate,
      @Nullable Object @Nullable ... errorMessageArgs) {
    if (reference == null) {
      throw new VerifyException(lenientFormat(errorMessageTemplate, errorMessageArgs));
    }
    return reference;
  }

  // TODO(kevinb): consider <T> T verifySingleton(Iterable<T>) to take over for
  // Iterables.getOnlyElement()

  private Verify() {}
}

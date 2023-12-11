/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.util.Comparator;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A utility for performing a chained comparison statement. <b>Note:</b> Java 8+ users should
 * generally prefer the methods in {@link Comparator}; see <a href="#java8">below</a>.
 *
 * <p>Example usage of {@code ComparisonChain}:
 *
 * <pre>{@code
 * public int compareTo(Foo that) {
 *   return ComparisonChain.start()
 *       .compare(this.aString, that.aString)
 *       .compare(this.anInt, that.anInt)
 *       .compare(this.anEnum, that.anEnum, Ordering.natural().nullsLast())
 *       .result();
 * }
 * }</pre>
 *
 * <p>The value of this expression will have the same sign as the <i>first nonzero</i> comparison
 * result in the chain, or will be zero if every comparison result was zero.
 *
 * <p><b>Note:</b> {@code ComparisonChain} instances are <b>immutable</b>. For this utility to work
 * correctly, calls must be chained as illustrated above.
 *
 * <p>Performance note: Even though the {@code ComparisonChain} caller always invokes its {@code
 * compare} methods unconditionally, the {@code ComparisonChain} implementation stops calling its
 * inputs' {@link Comparable#compareTo compareTo} and {@link Comparator#compare compare} methods as
 * soon as one of them returns a nonzero result. This optimization is typically important only in
 * the presence of expensive {@code compareTo} and {@code compare} implementations.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CommonObjectUtilitiesExplained#comparecompareto">{@code
 * ComparisonChain}</a>.
 *
 * <h4 id="java8">Java 8+ equivalents</h4>
 *
 * If you are using Java version 8 or greater, you should generally use the static methods in {@link
 * Comparator} instead of {@code ComparisonChain}. The example above can be implemented like this:
 *
 * <pre>{@code
 * import static java.util.Comparator.comparing;
 * import static java.util.Comparator.nullsLast;
 * import static java.util.Comparator.naturalOrder;
 *
 * ...
 *   private static final Comparator<Foo> COMPARATOR =
 *       comparing((Foo foo) -> foo.aString)
 *           .thenComparing(foo -> foo.anInt)
 *           .thenComparing(foo -> foo.anEnum, nullsLast(naturalOrder()));}
 *
 *   {@code @Override}{@code
 *   public int compareTo(Foo that) {
 *     return COMPARATOR.compare(this, that);
 *   }
 * }</pre>
 *
 * <p>With method references it is more succinct: {@code comparing(Foo::aString)} for example.
 *
 * <p>Using {@link Comparator} avoids certain types of bugs, for example when you meant to write
 * {@code .compare(a.foo, b.foo)} but you actually wrote {@code .compare(a.foo, a.foo)} or {@code
 * .compare(a.foo, b.bar)}. {@code ComparisonChain} also has a potential performance problem that
 * {@code Comparator} doesn't: it evaluates all the parameters of all the {@code .compare} calls,
 * even when the result of the comparison is already known from previous {@code .compare} calls.
 * That can be expensive.
 *
 * @author Mark Davis
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public abstract class ComparisonChain {
  private ComparisonChain() {}

  /** Begins a new chained comparison statement. See example in the class documentation. */
  public static ComparisonChain start() {
    return ACTIVE;
  }

  private static final ComparisonChain ACTIVE =
      new ComparisonChain() {
        @SuppressWarnings("unchecked") // unsafe; see discussion on supertype
        @Override
        public ComparisonChain compare(Comparable<?> left, Comparable<?> right) {
          return classify(((Comparable<Object>) left).compareTo(right));
        }

        @Override
        public <T extends @Nullable Object> ComparisonChain compare(
            @ParametricNullness T left, @ParametricNullness T right, Comparator<T> comparator) {
          return classify(comparator.compare(left, right));
        }

        @Override
        public ComparisonChain compare(int left, int right) {
          return classify(Ints.compare(left, right));
        }

        @Override
        public ComparisonChain compare(long left, long right) {
          return classify(Longs.compare(left, right));
        }

        @Override
        public ComparisonChain compare(float left, float right) {
          return classify(Float.compare(left, right));
        }

        @Override
        public ComparisonChain compare(double left, double right) {
          return classify(Double.compare(left, right));
        }

        @Override
        public ComparisonChain compareTrueFirst(boolean left, boolean right) {
          return classify(Booleans.compare(right, left)); // reversed
        }

        @Override
        public ComparisonChain compareFalseFirst(boolean left, boolean right) {
          return classify(Booleans.compare(left, right));
        }

        ComparisonChain classify(int result) {
          return (result < 0) ? LESS : (result > 0) ? GREATER : ACTIVE;
        }

        @Override
        public int result() {
          return 0;
        }
      };

  private static final ComparisonChain LESS = new InactiveComparisonChain(-1);

  private static final ComparisonChain GREATER = new InactiveComparisonChain(1);

  private static final class InactiveComparisonChain extends ComparisonChain {
    final int result;

    InactiveComparisonChain(int result) {
      this.result = result;
    }

    @Override
    public ComparisonChain compare(Comparable<?> left, Comparable<?> right) {
      return this;
    }

    @Override
    public <T extends @Nullable Object> ComparisonChain compare(
        @ParametricNullness T left, @ParametricNullness T right, Comparator<T> comparator) {
      return this;
    }

    @Override
    public ComparisonChain compare(int left, int right) {
      return this;
    }

    @Override
    public ComparisonChain compare(long left, long right) {
      return this;
    }

    @Override
    public ComparisonChain compare(float left, float right) {
      return this;
    }

    @Override
    public ComparisonChain compare(double left, double right) {
      return this;
    }

    @Override
    public ComparisonChain compareTrueFirst(boolean left, boolean right) {
      return this;
    }

    @Override
    public ComparisonChain compareFalseFirst(boolean left, boolean right) {
      return this;
    }

    @Override
    public int result() {
      return result;
    }
  }

  /**
   * Compares two comparable objects as specified by {@link Comparable#compareTo}, <i>if</i> the
   * result of this comparison chain has not already been determined.
   *
   * <p>This method is declared to accept any 2 {@code Comparable} objects, even if they are not <a
   * href="https://docs.oracle.com/javase/tutorial/collections/interfaces/order.html">mutually
   * comparable</a>. If you pass objects that are not mutually comparable, this method may throw an
   * exception. (The reason for this decision is lost to time, but the reason <i>might</i> be that
   * we wanted to support legacy classes that implement the raw type {@code Comparable} (instead of
   * implementing {@code Comparable<Foo>}) without producing warnings. If so, we would prefer today
   * to produce warnings in that case, and we may change this method to do so in the future. Support
   * for raw {@code Comparable} types in Guava in general is tracked as <a
   * href="https://github.com/google/guava/issues/989">#989</a>.)
   *
   * @throws ClassCastException if the parameters are not mutually comparable
   */
  public abstract ComparisonChain compare(Comparable<?> left, Comparable<?> right);

  /**
   * Compares two objects using a comparator, <i>if</i> the result of this comparison chain has not
   * already been determined.
   */
  public abstract <T extends @Nullable Object> ComparisonChain compare(
      @ParametricNullness T left, @ParametricNullness T right, Comparator<T> comparator);

  /**
   * Compares two {@code int} values as specified by {@link Ints#compare}, <i>if</i> the result of
   * this comparison chain has not already been determined.
   */
  public abstract ComparisonChain compare(int left, int right);

  /**
   * Compares two {@code long} values as specified by {@link Longs#compare}, <i>if</i> the result of
   * this comparison chain has not already been determined.
   */
  public abstract ComparisonChain compare(long left, long right);

  /**
   * Compares two {@code float} values as specified by {@link Float#compare}, <i>if</i> the result
   * of this comparison chain has not already been determined.
   */
  public abstract ComparisonChain compare(float left, float right);

  /**
   * Compares two {@code double} values as specified by {@link Double#compare}, <i>if</i> the result
   * of this comparison chain has not already been determined.
   */
  public abstract ComparisonChain compare(double left, double right);

  /**
   * Discouraged synonym for {@link #compareFalseFirst}.
   *
   * @deprecated Use {@link #compareFalseFirst}; or, if the parameters passed are being either
   *     negated or reversed, undo the negation or reversal and use {@link #compareTrueFirst}.
   * @since 19.0
   */
  @Deprecated
  public final ComparisonChain compare(Boolean left, Boolean right) {
    return compareFalseFirst(left, right);
  }

  /**
   * Compares two {@code boolean} values, considering {@code true} to be less than {@code false},
   * <i>if</i> the result of this comparison chain has not already been determined.
   *
   * <p>Java 8+ users: you can get the equivalent from {@link Booleans#trueFirst()}. For example:
   *
   * <pre>
   * Comparator.comparing(Foo::isBar, {@link Booleans#trueFirst()})
   * </pre>
   *
   * @since 12.0
   */
  public abstract ComparisonChain compareTrueFirst(boolean left, boolean right);

  /**
   * Compares two {@code boolean} values, considering {@code false} to be less than {@code true},
   * <i>if</i> the result of this comparison chain has not already been determined.
   *
   * <p>Java 8+ users: you can get the equivalent from {@link Booleans#falseFirst()}. For example:
   *
   * <pre>
   * Comparator.comparing(Foo::isBar, {@link Booleans#falseFirst()})
   * </pre>
   *
   * @since 12.0 (present as {@code compare} since 2.0)
   */
  public abstract ComparisonChain compareFalseFirst(boolean left, boolean right);

  /**
   * Ends this comparison chain and returns its result: a value having the same sign as the first
   * nonzero comparison result in the chain, or zero if every result was zero.
   */
  public abstract int result();
}

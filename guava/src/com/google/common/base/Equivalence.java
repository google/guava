/*
 * Copyright (C) 2010 The Guava Authors
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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * A strategy for determining whether two instances are considered equivalent. Examples of
 * equivalences are the {@link Equivalences#identity() identity equivalence} and {@link
 * Equivalences#equals equals equivalence}.
 *
 * @author Bob Lee
 * @author Ben Yu
 * @since Guava release 10 (<a href="http://code.google.com/p/guava-libraries/wiki/Compatibility"
 *        >mostly source-compatible</a> since Guava release 04)
 */
@Beta
@GwtCompatible
public abstract class Equivalence<T> {
  /**
   * Constructor for use by subclasses.
   */
  protected Equivalence() {}

  /**
   * Returns {@code true} if the given objects are considered equivalent.
   *
   * <p>The {@code equivalent} method implements an equivalence relation on object references:
   *
   * <ul>
   * <li>It is <i>reflexive</i>: for any reference {@code x}, including null, {@code
   *     equivalent(x, x)} should return {@code true}.
   * <li>It is <i>symmetric</i>: for any references {@code x} and {@code y}, {@code
   *     equivalent(x, y) == equivalent(y, x)}.
   * <li>It is <i>transitive</i>: for any references {@code x}, {@code y}, and {@code z}, if
   *     {@code equivalent(x, y)} returns {@code true} and {@code equivalent(y, z)} returns {@code
   *     true}, then {@code equivalent(x, z)} should return {@code true}.
   * <li>It is <i>consistent</i>: for any references {@code x} and {@code y}, multiple invocations
   *     of {@code equivalent(x, y)} consistently return {@code true} or consistently return {@code
   *     false} (provided that neither {@code x} nor {@code y} is modified).
   * </ul>
   */
  public abstract boolean equivalent(@Nullable T a, @Nullable T b);

  /**
   * Returns a hash code for {@code object}.
   *
   * <p>The {@code hash} must have the following properties:
   * <ul>
   * <li>It is <i>consistent</i>: for any reference {@code x}, multiple invocations of
   *     {@code hash(x}} consistently return the same value provided {@code x} remains unchanged
   *     according to the definition of the equivalence. The hash need not remain consistent from
   *     one execution of an application to another execution of the same application.
   * <li>It is <i>distributable accross equivalence</i>: for any references {@code x} and {@code y},
   *     if {@code equivalent(x, y)}, then {@code hash(x) == hash(y)}. It is <i>not</i> necessary
   *     that the hash be distributable accorss <i>inequivalence</i>. If {@code equivalence(x, y)}
   *     is false, {@code hash(x) == hash(y)} may still be true.
   * <li>{@code hash(null)} is {@code 0}.
   * </ul>
   */
  public abstract int hash(@Nullable T t);

  /**
   * Returns a new equivalence relation for {@code F} which evaluates equivalence by first applying
   * {@code function} to the argument, then evaluating using {@code this}. That is, for any pair of
   * non-null objects {@code x} and {@code y}, {@code
   * equivalence.onResultOf(function).equivalent(a, b)} is true if and only if {@code
   * equivalence.equivalent(function.apply(a), function.apply(b))} is true.
   *
   * <p>For example: <pre>   {@code
   *
   *    Equivalence<Person> SAME_AGE = Equivalences.equals().onResultOf(GET_PERSON_AGE);
   * }</pre>
   * 
   * <p>{@code function} will never be invoked with a null value.
   * 
   * <p>Note that {@code function} must be consistent according to {@code this} equivalence
   * relation. That is, invoking {@link Function#apply} multiple times for a given value must return
   * equivalent results.
   * For example, {@code Equivalences.identity().onResultOf(Functions.toStringFunction())} is broken
   * because it's not guaranteed that {@link Object#toString}) always returns the same string
   * instance.
   * 
   * @since Guava release 10
   */
  @Beta
  public final <F> Equivalence<F> onResultOf(Function<F, ? extends T> function) {
    return new FunctionalEquivalence<F, T>(function, this);
  }
  
  /**
   * Returns a wrapper of {@code reference} that implements
   * {@link EquivalenceWrapper#equals(Object) Object.equals()} such that
   * {@code wrap(this, a).equals(wrap(this, b))} if and only if {@code this.equivalent(a, b)}.
   * 
   * @since Guava release 10
   */
  @Beta
  public final <S extends T> EquivalenceWrapper<S> wrap(@Nullable S reference) {
    return new EquivalenceWrapper<S>(this, reference);
  }

  /**
   * Returns an equivalence over iterables based on the equivalence of their elements.  More
   * specifically, two iterables are considered equivalent if they both contain the same number of
   * elements, and each pair of corresponding elements is equivalent according to
   * {@code this}.  Null iterables are equivalent to one another.
   * 
   * <p>Note that this method performs a similar function for equivalences as {@link
   * com.google.common.collect.Ordering#lexicographical} does for orderings.
   *
   * @since Guava release 10
   */
  @Beta
  @GwtCompatible(serializable = true)
  public final <S extends T> Equivalence<Iterable<S>> pairwise() {
    // Ideally, the returned equivalence would support Iterable<? extends T>. However,
    // the need for this is so rare that it's not worth making callers deal with the ugly wildcard.
    return new PairwiseEquivalence<S>(this);
  }
  
  /**
   * Returns a predicate that evaluates to true if and only if the input is
   * equivalent to {@code target} according to this equivalence relation.
   * 
   * @since Guava release 10
   */
  @Beta
  public final Predicate<T> equivalentTo(@Nullable T target) {
    return new EquivalentToPredicate<T>(this, target);
  }

  private static final class EquivalentToPredicate<T> implements Predicate<T>, Serializable {

    private final Equivalence<T> equivalence;
    @Nullable private final T target;

    EquivalentToPredicate(Equivalence<T> equivalence, @Nullable T target) {
      this.equivalence = checkNotNull(equivalence);
      this.target = target;
    }

    @Override public boolean apply(@Nullable T input) {
      return equivalence.equivalent(input, target);
    }

    @Override public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj instanceof EquivalentToPredicate) {
        EquivalentToPredicate<?> that = (EquivalentToPredicate<?>) obj;
        return equivalence.equals(that.equivalence)
            && Objects.equal(target, that.target);
      }
      return false;
    }

    @Override public int hashCode() {
      return Objects.hashCode(equivalence, target);
    }

    @Override public String toString() {
      return equivalence + ".equivalentTo(" + target + ")";
    }

    private static final long serialVersionUID = 0;
  }
}

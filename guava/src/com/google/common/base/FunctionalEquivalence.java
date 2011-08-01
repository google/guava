// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.base;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * Equivalence applied on functional result.
 *
 * @author Bob Lee
 * @since Guava release 10
 */
@Beta
@GwtCompatible
final class FunctionalEquivalence<F, T> extends Equivalence<F>
    implements Serializable {

  private static final long serialVersionUID = 0;

  private final Function<F, ? extends T> function;
  private final Equivalence<T> resultEquivalence;

  FunctionalEquivalence(
      Function<F, ? extends T> function, Equivalence<T> resultEquivalence) {
    this.function = checkNotNull(function);
    this.resultEquivalence = checkNotNull(resultEquivalence);
  }

  @Override protected boolean doEquivalent(F a, F b) {
    return resultEquivalence.equivalent(function.apply(a), function.apply(b));
  }

  @Override protected int doHash(F a) {
    return resultEquivalence.hash(function.apply(a));
  }

  @Override public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof FunctionalEquivalence) {
      FunctionalEquivalence<?, ?> that = (FunctionalEquivalence<?, ?>) obj;
      return function.equals(that.function)
          && resultEquivalence.equals(that.resultEquivalence);
    }
    return false;
  }

  @Override public int hashCode() {
    return Objects.hashCode(function, resultEquivalence);
  }

  @Override public String toString() {
    return resultEquivalence + ".onResultOf(" + function + ")";
  }
}

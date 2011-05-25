// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.base;

import com.google.common.annotations.GwtCompatible;

import javax.annotation.Nullable;

/**
 * Helper class to implement the contract around nulls.
 * 
 * @author benyu@google.com (Jige Yu)
 */
// TODO(benyu): Pull up into Equivalence and expose it as SPI.
@GwtCompatible
abstract class AbstractEquivalence<T> extends Equivalence<T> {

  @Override public final boolean equivalent(@Nullable T a, @Nullable T b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    return equivalentNonNull(a, b);
  }

  @Override public final int hash(@Nullable T t) {
    if (t == null) {
      return 0;
    }
    return hashNonNull(t);
  }

  protected abstract boolean equivalentNonNull(T a, T b);
  protected abstract int hashNonNull(T t);
}

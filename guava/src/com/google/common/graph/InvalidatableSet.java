package com.google.common.graph;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Supplier;
import com.google.common.collect.ForwardingSet;
import java.util.Set;

/**
 * A subclass of `ForwardingSet` that throws `IllegalStateException` on invocation of any method
 * (except `hashCode` and `equals`) if the provided `Supplier` returns false.
 */
final class InvalidatableSet<E> extends ForwardingSet<E> {
  private final Supplier<Boolean> validator;
  private final Set<E> delegate;
  private final String errorMessage;

  public static final <E> InvalidatableSet<E> of(Set<E> delegate, Supplier<Boolean> validator) {
    return new InvalidatableSet<>(checkNotNull(delegate), checkNotNull(validator), null);
  }

  public static final <E> InvalidatableSet<E> of(
      Set<E> delegate, Supplier<Boolean> validator, String errorMessage) {
    return new InvalidatableSet<>(checkNotNull(delegate), checkNotNull(validator), errorMessage);
  }

  @Override
  protected Set<E> delegate() {
    validate();
    return delegate;
  }

  private InvalidatableSet(Set<E> delegate, Supplier<Boolean> validator, String errorMessage) {
    this.delegate = delegate;
    this.validator = validator;
    this.errorMessage = errorMessage;
  }

  // Override hashCode() to access delegate directly (so that it doesn't trigger the validate() call
  // via delegate()); it seems inappropriate to throw ISE on this method.
  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  private void validate() {
    if (errorMessage == null) {
      checkState(validator.get());
    } else {
      checkState(validator.get(), errorMessage);
    }
  }
}

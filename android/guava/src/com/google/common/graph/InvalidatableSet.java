package com.google.common.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Supplier;
import com.google.common.collect.ForwardingSet;
import java.util.Set;

/**
 * A subclass of {@link ForwardingSet} that throws {@link IllegalStateException} on invocation of
 * any method (except {@link #hashCode}) if the provided {@link Supplier} returns false. Once the
 * set is invalidated, {@link #hashCode} returns 0.
 */
final class InvalidatableSet<E> extends ForwardingSet<E> {
  private final Supplier<Boolean> validator;
  private final Set<E> delegate;
  private final Supplier<String> errorMessage;

  static <E> InvalidatableSet<E> of(
      Set<E> delegate, Supplier<Boolean> validator, Supplier<String> errorMessage) {
    return new InvalidatableSet<>(
        checkNotNull(delegate), checkNotNull(validator), checkNotNull(errorMessage));
  }

  @Override
  protected Set<E> delegate() {
    validate();
    return delegate;
  }

  private InvalidatableSet(
      Set<E> delegate, Supplier<Boolean> validator, Supplier<String> errorMessage) {
    this.delegate = delegate;
    this.validator = validator;
    this.errorMessage = errorMessage;
  }

  // Override hashCode() to access delegate directly (so that it doesn't trigger the validate() call
  // via delegate()); it seems inappropriate to throw ISE on this method. Once the set has been
  // invalidated, though, we don't touch the delegate at all: computing its hash code may itself
  // throw (e.g., if it is a view that looks up a node that has since been removed).
  @Override
  public int hashCode() {
    return validator.get() ? delegate.hashCode() : 0;
  }

  private void validate() {
    // Don't use checkState(), because we don't want the overhead of generating the error message
    // unless it's actually going to be used; validate() is called for all set method calls, so it
    // needs to be fast.
    // (We could instead generate the message once, when the set is created, but zero is better.)
    if (!validator.get()) {
      throw new IllegalStateException(errorMessage.get());
    }
  }
}

package com.google.common.util.concurrent;

/**
 * A function which computes a value and can throw an {@link InterruptedException}.
 *
 * @author Ram Anvesh Reddy
 */
abstract class InterruptibleCallableWithoutTimeout<V> implements InterruptibleCallable<V> {
  abstract V call() throws InterruptedException;

  @Override
  public final V call(TimeDuration timeDuration) throws InterruptedException {
    return call();
  }
}

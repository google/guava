package com.google.common.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * A function which computes a value and can throw an {@link InterruptedException}.
 *
 * @author Ram Anvesh Reddy
 */
abstract class InterruptibleCallableWithTimeout<V> implements InterruptibleCallable<V> {
  abstract V call(long timeout, TimeUnit unit) throws InterruptedException;

  @Override
  public final V call(TimeDuration timeDuration) throws InterruptedException {
    return call(timeDuration.getDuration(), timeDuration.getUnit());
  }
}
package com.google.common.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * A function which computes a value and can throw an {@link InterruptedException}.
 *
 * @author Ram Anvesh Reddy
 */
abstract class InterruptibleRunnableWithTimeout implements InterruptibleCallable<Void> {
  abstract void run(long timeout, TimeUnit unit) throws InterruptedException;

  @Override
  public final Void call(TimeDuration timeDuration) throws InterruptedException {
    run(timeDuration.getDuration(), timeDuration.getUnit());
    return null;
  }
}

package com.google.common.util.concurrent;

/**
 * A function which computes a value and can throw an {@link InterruptedException}.
 *
 * @author Ram Anvesh Reddy
 */
abstract class InterruptibleRunnableWithoutTimeout implements InterruptibleCallable<Void> {
  abstract void run() throws InterruptedException;

  @Override
  public final Void call(TimeDuration timeDuration) throws InterruptedException {
    run();
    return null;
  }
}

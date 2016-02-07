package com.google.common.util.concurrent;

/**
 *
 * A function which computes a value and can throw an {@link InterruptedException}.
 *
 * @author Ram Anvesh Reddy
 */
interface InterruptibleCallable<V> {

  V call(TimeDuration timeDuration) throws InterruptedException;
}

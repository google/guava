/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.testing;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.base.Ticker;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A Ticker whose value can be advanced programmatically in test.
 *
 * <p>The ticker can be configured so that the time is incremented whenever {@link #read} is called:
 * see {@link #setAutoIncrementStep}.
 *
 * <p>This class is thread-safe.
 *
 * @author Jige Yu
 * @since 10.0
 */
@ElementTypesAreNonnullByDefault
@GwtCompatible
public class FakeTicker extends Ticker {

  private final AtomicLong nanos = new AtomicLong();
  private volatile long autoIncrementStepNanos;

  /** Advances the ticker value by {@code time} in {@code timeUnit}. */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  @CanIgnoreReturnValue
  public FakeTicker advance(long time, TimeUnit timeUnit) {
    return advance(timeUnit.toNanos(time));
  }

  /** Advances the ticker value by {@code nanoseconds}. */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  @CanIgnoreReturnValue
  public FakeTicker advance(long nanoseconds) {
    nanos.addAndGet(nanoseconds);
    return this;
  }

  /**
   * Advances the ticker value by {@code duration}.
   *
   * @since 33.1.0 (but since 28.0 in the JRE <a
   *     href="https://github.com/google/guava#guava-google-core-libraries-for-java">flavor</a>)
   */
  @GwtIncompatible
  @J2ktIncompatible
  @CanIgnoreReturnValue
  @SuppressWarnings("Java7ApiChecker") // guava-android can rely on library desugaring now.
  @IgnoreJRERequirement // TODO: b/288085449 - Remove this once we use library-desugaring scents.
  @Beta // TODO: b/288085449 - Remove @Beta after we're sure that Java 8 APIs are safe for Android
  public FakeTicker advance(Duration duration) {
    return advance(duration.toNanos());
  }

  /**
   * Sets the increment applied to the ticker whenever it is queried.
   *
   * <p>The default behavior is to auto increment by zero. i.e: The ticker is left unchanged when
   * queried.
   */
  @SuppressWarnings("GoodTime") // should accept a java.time.Duration
  @CanIgnoreReturnValue
  public FakeTicker setAutoIncrementStep(long autoIncrementStep, TimeUnit timeUnit) {
    checkArgument(autoIncrementStep >= 0, "May not auto-increment by a negative amount");
    this.autoIncrementStepNanos = timeUnit.toNanos(autoIncrementStep);
    return this;
  }

  /**
   * Sets the increment applied to the ticker whenever it is queried.
   *
   * <p>The default behavior is to auto increment by zero. i.e: The ticker is left unchanged when
   * queried.
   *
   * @since 33.1.0 (but since 28.0 in the JRE <a
   *     href="https://github.com/google/guava#guava-google-core-libraries-for-java">flavor</a>)
   */
  @GwtIncompatible
  @J2ktIncompatible
  @CanIgnoreReturnValue
  @SuppressWarnings("Java7ApiChecker") // guava-android can rely on library desugaring now.
  @IgnoreJRERequirement // TODO: b/288085449 - Remove this once we use library-desugaring scents.
  @Beta // TODO: b/288085449 - Remove @Beta after we're sure that Java 8 APIs are safe for Android
  public FakeTicker setAutoIncrementStep(Duration autoIncrementStep) {
    return setAutoIncrementStep(autoIncrementStep.toNanos(), TimeUnit.NANOSECONDS);
  }

  @Override
  public long read() {
    return nanos.getAndAdd(autoIncrementStepNanos);
  }
}

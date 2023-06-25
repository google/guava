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

package com.google.common.base;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.J2ktIncompatible;
import com.google.common.testing.FakeTicker;
import java.time.Duration;
import junit.framework.TestCase;

/** Unit test for the {@code java.time} support in {@link Stopwatch}. */
@J2ktIncompatible
@GwtIncompatible
public class StopwatchJavaTimeTest extends TestCase {
  private final FakeTicker ticker = new FakeTicker();
  private final Stopwatch stopwatch = new Stopwatch(ticker);

  public void testElapsed_duration() {
    stopwatch.start();
    ticker.advance(999999);
    assertEquals(Duration.ofNanos(999999), stopwatch.elapsed());
    ticker.advance(1);
    assertEquals(Duration.ofMillis(1), stopwatch.elapsed());
  }
}

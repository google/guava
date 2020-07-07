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

import com.google.caliper.Benchmark;
import java.util.concurrent.TimeUnit;

/**
 * Simple benchmark: create, start, read. This does not currently report the most useful result
 * because it's ambiguous to what extent the stopwatch benchmark is being affected by GC.
 *
 * @author Kevin Bourrillion
 */
public class StopwatchBenchmark {
  @Benchmark
  long stopwatch(int reps) {
    long total = 0;
    for (int i = 0; i < reps; i++) {
      Stopwatch s = Stopwatch.createStarted();
      // here is where you would do something
      total += s.elapsed(TimeUnit.NANOSECONDS);
    }
    return total;
  }

  @Benchmark
  long manual(int reps) {
    long total = 0;
    for (int i = 0; i < reps; i++) {
      long start = System.nanoTime();
      // here is where you would do something
      total += (System.nanoTime() - start);
    }
    return total;
  }
}

/*
 * Copyright (C) 2013 The Guava Authors
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

import static com.google.common.base.Throwables.lazyStackTrace;
import static java.util.Arrays.asList;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.SkipThisScenarioException;
import java.util.List;

/**
 * Quick and dirty benchmark of {@link Throwables#lazyStackTrace(Throwable)}. We benchmark a "caller
 * finder" implementation that might be used in a logging framework.
 */
public class LazyStackTraceBenchmark {
  @Param({"20", "200", "2000"})
  int stackDepth;

  @Param({"-1", "3", "15"})
  int breakAt;

  int recursionCount;

  private static final Object duh = new Object();

  @Param Mode mode;

  enum Mode {
    LAZY_STACK_TRACE {
      @Override
      List<StackTraceElement> getStackTrace(Throwable t) {
        return lazyStackTrace(t);
      }
    },
    GET_STACK_TRACE {
      @Override
      List<StackTraceElement> getStackTrace(Throwable t) {
        return asList(t.getStackTrace());
      }
    };

    boolean timeIt(int reps, int breakAt) {
      boolean dummy = false;
      for (int i = 0; i < reps; i++) {
        int f = 0;
        Throwable t = new Throwable();
        for (StackTraceElement ste : getStackTrace(t)) {
          dummy |= ste == duh;
          if (f++ == breakAt) {
            break;
          }
        }
      }
      return dummy;
    }

    abstract List<StackTraceElement> getStackTrace(Throwable t);
  }

  @BeforeExperiment
  public void doBefore() {
    recursionCount = stackDepth - new Throwable().getStackTrace().length - 1;
    if (recursionCount < 0) {
      throw new SkipThisScenarioException();
    }
  }

  @Benchmark
  public boolean timeFindCaller(int reps) {
    return timeFindCaller(reps, recursionCount);
  }

  private boolean timeFindCaller(int reps, int recurse) {
    return recurse > 0 ? timeFindCaller(reps, recurse - 1) : mode.timeIt(reps, breakAt);
  }
}

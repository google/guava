/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.eventbus;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;

/**
 * Benchmark for {@link EventBus}.
 *
 * @author Eric Fellheimer
 */
public class EventBusBenchmark {

  private EventBus eventBus;

  @BeforeExperiment
  void setUp() {
    eventBus = new EventBus("for benchmarking purposes");
    eventBus.register(this);
  }

  @Benchmark
  void postStrings(int reps) {
    for (int i = 0; i < reps; i++) {
      eventBus.post("hello there");
    }
  }

  @Subscribe
  public void handleStrings(String string) {
    // Nothing to do here.
  }
}

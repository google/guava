/*
 * Copyright (C) 2010 The Guava Authors
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

package com.google.common.util.concurrent;

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

import java.lang.reflect.Constructor;
import java.util.concurrent.BlockingQueue;

/**
 * Benchmarks for {@link Monitor}.
 * 
 * @author Justin T. Sampson
 */
public class MonitorBenchmark extends SimpleBenchmark {
  
  @Param({"10", "100", "1000"}) int capacity;
  @Param({"Array", "Priority"}) String queueType;
  @Param boolean useMonitor;
  
  private BlockingQueue<String> queue;
  private String[] strings;

  @Override
  @SuppressWarnings("unchecked")
  protected void setUp() throws Exception {
    String prefix =
        (useMonitor ? "com.google.common.util.concurrent.MonitorBased" : "java.util.concurrent.");
    String className = prefix + queueType + "BlockingQueue";
    Constructor<?> constructor = Class.forName(className).getConstructor(int.class);
    queue = (BlockingQueue<String>) constructor.newInstance(capacity);
    
    strings = new String[capacity];
    for (int i = 0; i < capacity; i++) {
      strings[i] = String.valueOf(Math.random());
    }
  }

  public void timeAddsAndRemoves(int reps) {
    int capacity = this.capacity;
    BlockingQueue<String> queue = this.queue;
    String[] strings = this.strings;
    for (int i = 0; i < reps; i++) {
      for (int j = 0; j < capacity; j++) {
        queue.add(strings[j]);
      }
      for (int j = 0; j < capacity; j++) {
        queue.remove();
      }
    }
  }

  public static void main(String[] args) {
    Runner.main(MonitorBenchmark.class, args);
  }

}

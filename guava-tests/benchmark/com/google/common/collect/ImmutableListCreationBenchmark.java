/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.collect;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.runner.CaliperMain;

import java.util.List;

/**
 * Benchmark for various ways to create an {@code ImmutableList}.
 *
 * @author Louis Wasserman
 */
public class ImmutableListCreationBenchmark extends Benchmark {

  @Param({"10", "1000", "1000000"})
  int size;

  private static final Object OBJECT = new Object();

  public int timeBuilderAdd(int reps) {
    int size = this.size;
    int dummy = 0;
    for (int rep = 0; rep < reps; rep++) {
      ImmutableList.Builder<Object> builder = ImmutableList.builder();
      for (int i = 0; i < size; i++) {
        builder.add(OBJECT);
      }
      dummy += builder.build().size();
    }
    return dummy;
  }

  public int timePreSizedBuilderAdd(int reps) {
    int size = this.size;
    int dummy = 0;
    for (int rep = 0; rep < reps; rep++) {
      ImmutableList.Builder<Object> builder = new ImmutableList.Builder<Object>(size);
      for (int i = 0; i < size; i++) {
        builder.add(OBJECT);
      }
      dummy += builder.build().size();
    }
    return dummy;
  }

  public int timeCopyArrayList(int reps) {
    int size = this.size;
    int dummy = 0;
    for (int rep = 0; rep < reps; rep++) {
      List<Object> builder = Lists.newArrayList();
      for (int i = 0; i < size; i++) {
        builder.add(OBJECT);
      }
      dummy += ImmutableList.copyOf(builder).size();
    }
    return dummy;
  }

  public int timeCopyPreSizedArrayList(int reps) {
    int size = this.size;
    int tmp = 0;
    for (int rep = 0; rep < reps; rep++) {
      List<Object> builder = Lists.newArrayListWithCapacity(size);
      for (int i = 0; i < size; i++) {
        builder.add(OBJECT);
      }
      tmp += ImmutableList.copyOf(builder).size();
    }
    return tmp;
  }

  public static void main(String[] args) {
    CaliperMain.main(ImmutableListCreationBenchmark.class, args);
  }
}

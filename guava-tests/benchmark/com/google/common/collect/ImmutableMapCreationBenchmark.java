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

package com.google.common.collect;

import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;

/**
 * A Caliper benchmark used to track Builder performance.  Currently supports:
 * ImmutableMap's copyOf and addAll methods.
 *
 * @author Paul Lindner
 */
public class ImmutableMapCreationBenchmark extends SimpleBenchmark {

  @Param({"0", "1", "5", "50", "500", "5000", "50000"}) private int size;

  /** Holds testdata that is inserted into the Builder, populated by setUp() */
  Map<String, String> testData = Maps.newHashMap();

  /** A map we insert into, in class scope to keep the JVM from optimizing local references */
  ImmutableMap<String, String> testMap;

  // Preinitialize these to keep them out of the inner benchmark loop
  public static final Map<String, String> SINGLETON_MAP = Collections.singletonMap("1", "1");
  public static final Map<String, String> EMPTY_MAP = Collections.emptyMap();

  @Override
  public void setUp() {
    if (size == 0) {
      testData = EMPTY_MAP;
    } else if (size == 1) {
      testData = SINGLETON_MAP;
    } else {
      testData = Maps.newHashMap();
      for (int i = 0; i < size; i++) {
        String number = Integer.toString(i);
        testData.put(number, number);
      }
    }
  }

  /**
   * Test performance of ImmutableMap.copyOf()
   *
   * @param reps repetitions (used by Caliper)
   * @return a dummy string to ensure that the JVM does not optimize our code
   */
  public String timeCopyOf(int reps) {
    for (int i = 0; i < reps; i++) {
      testMap = ImmutableMap.copyOf(testData);
    }
    return testMap.get("");
  }

  /**
   * Test performance of the ImmutableMap.Builder putAll method.
   *
   * @param reps repetitions (used by Caliper)
   * @return a dummy string to ensure that the JVM does not optimize our code
   */
  public String timeBuilder(int reps) {
    for (int i = 0; i < reps; i++) {
      ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
      testMap = builder.putAll(testData).build();
    }
    return testMap.get("");
  }

  public static void main(String[] args) {
    Runner.main(ImmutableMapCreationBenchmark.class, args);
  }
}

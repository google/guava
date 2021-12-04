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

package com.google.common.base;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.util.Arrays;
import java.util.Collections;

/**
 * Some microbenchmarks for the {@link MoreObjects.ToStringHelper} class.
 *
 * @author Osvaldo Doederlein
 */
public class ToStringHelperBenchmark {

  @Param({"0", "1", "5"})
  int dataSize;

  @Param({"false", "true"})
  boolean omitNulls;

  enum Dataset {
    SMALL {
      void addEntries(MoreObjects.ToStringHelper helper) {
        helper
            .add(SHORT_NAME, 10)
            .addValue(10L)
            .add(SHORT_NAME, 3.14f)
            .addValue(3.14d)
            .add(LONG_NAME, false)
            .add(LONG_NAME, LONG_NAME);
      }
    },
    CONDITIONAL {
      void addEntries(MoreObjects.ToStringHelper helper) {
        helper
            .add(SHORT_NAME, "x")
            .add(LONG_NAME, "y")
            .add(SHORT_NAME, null)
            .add(LONG_NAME, null)
            .addValue("z")
            .addValue("")
            .addValue(null)
            .add(SHORT_NAME, Arrays.asList("A"))
            .add(LONG_NAME, Arrays.asList("B"))
            .add(SHORT_NAME, Arrays.asList())
            .add(LONG_NAME, Arrays.asList())
            .addValue(Arrays.asList("C"))
            .addValue(Arrays.asList())
            .add(SHORT_NAME, Collections.singletonMap("k1", "v1"))
            .add(LONG_NAME, Collections.singletonMap("k2", "v2"))
            .addValue(Collections.singletonMap("k3", "v3"))
            .addValue(Collections.emptyMap())
            .addValue(null)
            .add(SHORT_NAME, java.util.Optional.of("1"))
            .add(LONG_NAME, java.util.Optional.of("1"))
            .add(SHORT_NAME, java.util.Optional.empty())
            .add(LONG_NAME, java.util.Optional.empty())
            .add(SHORT_NAME, Optional.of("2"))
            .add(SHORT_NAME, Optional.absent())
            .addValue(null)
            .add(SHORT_NAME, new int[] {1})
            .add(LONG_NAME, new int[] {2})
            .addValue(new int[] {3})
            .addValue(new int[] {})
            .addValue(null);
      }
    },
    UNCONDITIONAL {
      void addEntries(MoreObjects.ToStringHelper helper) {
        helper
            .add(SHORT_NAME, false)
            .add(LONG_NAME, false)
            .addValue(true)
            .add(SHORT_NAME, (byte) 1)
            .add(LONG_NAME, (byte) 2)
            .addValue((byte) 3)
            .add(SHORT_NAME, 'A')
            .add(LONG_NAME, 'B')
            .addValue('C')
            .add(SHORT_NAME, (short) 4)
            .add(LONG_NAME, (short) 5)
            .addValue((short) 6)
            .add(SHORT_NAME, 7)
            .add(LONG_NAME, 8)
            .addValue(9)
            .add(SHORT_NAME, 10L)
            .add(LONG_NAME, 11L)
            .addValue(12L)
            .add(SHORT_NAME, 13.0f)
            .add(LONG_NAME, 14.0f)
            .addValue(15.0f);
      }
    };

    void addEntries(MoreObjects.ToStringHelper helper) {}
  }

  @Param Dataset dataset;

  private static final String SHORT_NAME = "userId";
  private static final String LONG_NAME = "fluxCapacitorFailureRate95Percentile";

  private MoreObjects.ToStringHelper newHelper() {
    MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper("klass");
    if (omitNulls) {
      helper = helper.omitNullValues();
    }
    return helper;
  }

  @Benchmark
  int toString(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; i++) {
      MoreObjects.ToStringHelper helper = newHelper();
      for (int j = 0; j < dataSize; ++j) {
        dataset.addEntries(helper);
      }
      dummy ^= helper.toString().hashCode();
    }
    return dummy;
  }

  // When omitEmptyValues() is released, remove this method and add a new @Param "omitEmptyValues".
}

/*
 * Copyright (C) 2019 The Guava Authors
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

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.common.math.IntMath;
import java.math.RoundingMode;

/** Benchmark of implementations of {@link ImmutableSet#hashFloodingDetected(Object[])}. */
public class ImmutableSetHashFloodingDetectionBenchmark {
  private static final int TEST_CASES = 0x100;

  @Param({"10", "100", "1000", "10000"})
  int size;

  @Param Impl impl;

  private static final Object[][] tables = new Object[TEST_CASES][];

  @BeforeExperiment
  public void setUp() {
    int tableSize = ImmutableSet.chooseTableSize(size);
    int mask = tableSize - 1;
    for (int i = 0; i < TEST_CASES; i++) {
      tables[i] = new Object[tableSize];
      for (int j = 0; j < size; j++) {
        Object o = new Object();
        for (int k = o.hashCode(); ; k++) {
          int index = k & mask;
          if (tables[i][index] == null) {
            tables[i][index] = o;
            break;
          }
        }
      }
    }
  }

  enum Impl {
    EXHAUSTIVE {
      int maxRunBeforeFallback(int tableSize) {
        return 12 * IntMath.log2(tableSize, RoundingMode.UNNECESSARY);
      }

      @Override
      boolean hashFloodingDetected(Object[] hashTable) {
        int maxRunBeforeFallback = maxRunBeforeFallback(hashTable.length);

        // Test for a run wrapping around the end of the table, then check for runs in the middle.
        int endOfStartRun;
        for (endOfStartRun = 0; endOfStartRun < hashTable.length; ) {
          if (hashTable[endOfStartRun] == null) {
            break;
          }
          endOfStartRun++;
          if (endOfStartRun > maxRunBeforeFallback) {
            return true;
          }
        }
        int startOfEndRun;
        for (startOfEndRun = hashTable.length - 1; startOfEndRun > endOfStartRun; startOfEndRun--) {
          if (hashTable[startOfEndRun] == null) {
            break;
          }
          if (endOfStartRun + (hashTable.length - 1 - startOfEndRun) > maxRunBeforeFallback) {
            return true;
          }
        }
        for (int i = endOfStartRun + 1; i < startOfEndRun; i++) {
          for (int runLength = 0; i < startOfEndRun && hashTable[i] != null; i++) {
            runLength++;
            if (runLength > maxRunBeforeFallback) {
              return true;
            }
          }
        }
        return false;
      }
    },
    SEPARATE_RANGES {
      int maxRunBeforeFallback(int tableSize) {
        return 13 * IntMath.log2(tableSize, RoundingMode.UNNECESSARY);
      }

      @Override
      boolean hashFloodingDetected(Object[] hashTable) {
        int maxRunBeforeFallback = maxRunBeforeFallback(hashTable.length);

        // Test for a run wrapping around the end of the table, then check for runs in the middle.
        int endOfStartRun;
        for (endOfStartRun = 0; endOfStartRun < hashTable.length; ) {
          if (hashTable[endOfStartRun] == null) {
            break;
          }
          endOfStartRun++;
          if (endOfStartRun > maxRunBeforeFallback) {
            return true;
          }
        }
        int startOfEndRun;
        for (startOfEndRun = hashTable.length - 1; startOfEndRun > endOfStartRun; startOfEndRun--) {
          if (hashTable[startOfEndRun] == null) {
            break;
          }
          if (endOfStartRun + (hashTable.length - 1 - startOfEndRun) > maxRunBeforeFallback) {
            return true;
          }
        }

        // If this part returns true, there is definitely a run of size maxRunBeforeFallback/2.
        // If this part returns false, there are definitely no runs of size >= maxRunBeforeFallback.
        int testBlockSize = maxRunBeforeFallback / 2;
        for (int i = endOfStartRun + 1; i + testBlockSize <= startOfEndRun; i += testBlockSize) {
          boolean runGood = false;
          for (int j = 0; j < testBlockSize; j++) {
            if (hashTable[i + j] == null) {
              runGood = true;
              break;
            }
          }
          if (!runGood) {
            return true;
          }
        }
        return false;
      }
    },
    SKIPPING {
      int maxRunBeforeFallback(int tableSize) {
        return 13 * IntMath.log2(tableSize, RoundingMode.UNNECESSARY);
      }

      @Override
      boolean hashFloodingDetected(Object[] hashTable) {
        int maxRunBeforeFallback = maxRunBeforeFallback(hashTable.length);
        int mask = hashTable.length - 1;

        // Invariant: all elements at indices in [knownRunStart, knownRunEnd) are nonnull.
        // If knownRunStart == knownRunEnd, this is vacuously true.
        // When knownRunEnd exceeds hashTable.length, it "wraps", detecting runs around the end
        // of the table.
        int knownRunStart = 0;
        int knownRunEnd = 0;

        outerLoop:
        while (knownRunStart < hashTable.length) {
          if (knownRunStart == knownRunEnd && hashTable[knownRunStart] == null) {
            if (hashTable[(knownRunStart + maxRunBeforeFallback - 1) & mask] == null) {
              // There are only maxRunBeforeFallback - 1 elements between here and there,
              // so even if they were all nonnull, we wouldn't detect a hash flood.  Therefore,
              // we can skip them all.
              knownRunStart += maxRunBeforeFallback;
            } else {
              knownRunStart++; // the only case in which maxRunEnd doesn't increase by mRBF
              // happens about f * (1-f) for f = DESIRED_LOAD_FACTOR, so around 21% of the time
            }
            knownRunEnd = knownRunStart;
          } else {
            for (int j = knownRunStart + maxRunBeforeFallback - 1; j >= knownRunEnd; j--) {
              if (hashTable[j & mask] == null) {
                knownRunEnd = knownRunStart + maxRunBeforeFallback;
                knownRunStart = j + 1;
                continue outerLoop;
              }
            }
            return true;
          }
        }
        return false;
      }
    };

    abstract boolean hashFloodingDetected(Object[] array);
  }

  @Benchmark
  public int detect(int reps) {
    int count = 0;
    for (int i = 0; i < reps; i++) {
      if (impl.hashFloodingDetected(tables[i & 0xFF])) {
        count++;
      }
    }
    return count;
  }
}

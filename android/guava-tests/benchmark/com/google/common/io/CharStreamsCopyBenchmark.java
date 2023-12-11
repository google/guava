/*
 * Copyright (C) 2017 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.io;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.VmOptions;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.util.Random;

/**
 * Benchmarks for {@link CharStreams#copy}.
 *
 * <p>{@link CharStreams#copy} has type specific optimizations for various common Appendable and
 * Reader implementations, this compares the performance of the different options.
 */
// These benchmarks allocate a lot of data so use a large heap
@VmOptions({"-Xms12g", "-Xmx12g", "-d64"})
public class CharStreamsCopyBenchmark {
  enum CopyStrategy {
    OLD {
      @Override
      long copy(Readable from, Appendable to) throws IOException {
        CharBuffer buf = CharStreams.createBuffer();
        long total = 0;
        while (from.read(buf) != -1) {
          ((Buffer) buf).flip();
          to.append(buf);
          total += buf.remaining();
          ((Buffer) buf).clear();
        }
        return total;
      }
    },
    NEW {
      @Override
      long copy(Readable from, Appendable to) throws IOException {
        return CharStreams.copy(from, to);
      }
    };

    abstract long copy(Readable from, Appendable to) throws IOException;
  }

  enum TargetSupplier {
    STRING_WRITER {
      @Override
      Appendable get(int sz) {
        return new StringWriter(sz);
      }
    },
    STRING_BUILDER {
      @Override
      Appendable get(int sz) {
        return new StringBuilder(sz);
      }
    };

    abstract Appendable get(int sz);
  }

  @Param CopyStrategy strategy;
  @Param TargetSupplier target;

  @Param({"10", "1024", "1048576"})
  int size;

  String data;

  @BeforeExperiment
  public void setUp() {
    // precalculate some random strings of ascii characters.
    StringBuilder sb = new StringBuilder();
    Random random = new Random(0xdeadbeef); // for unpredictable but reproducible behavior
    sb.ensureCapacity(size);
    for (int k = 0; k < size; k++) {
      // [9-127) includes all ascii non-control characters
      sb.append((char) (random.nextInt(127 - 9) + 9));
    }
    data = sb.toString();
  }

  @Benchmark
  public long timeCopy(int reps) throws IOException {
    long r = 0;
    final String localData = data;
    final TargetSupplier localTarget = target;
    final CopyStrategy localStrategy = strategy;
    for (int i = 0; i < reps; i++) {
      Appendable appendable = localTarget.get(localData.length());
      r += localStrategy.copy(new StringReader(localData), appendable);
    }
    return r;
  }
}

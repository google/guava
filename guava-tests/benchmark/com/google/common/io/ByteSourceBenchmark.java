/*
 * Copyright (C) 2015 The Guava Authors
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * Benchmark for {@code ByteSource} performance.
 */
public class ByteSourceBenchmark {
    @Param({"10", "100", "1000", "10000", "100000"})
    int n;

    private ByteSource byteSource;
    private ByteSource byteSourceCopy;

    @BeforeExperiment
    public void setUp() {
        Random rng = new Random();

        byte[] bytes = new byte[n];

        rng.nextBytes(bytes);

        byteSource = ByteSource.wrap(bytes);
        byteSourceCopy = ByteSource.wrap(Arrays.copyOf(bytes, bytes.length));
    }

    @Benchmark
    public void contentEquals(int reps) throws IOException {
        for (int i = 0; i < reps; i++) {
            byteSource.contentEquals(byteSourceCopy);
        }
    }
}

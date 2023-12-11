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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Benchmark for {@code ByteStreams} performance.
 */
public class ByteStreamsBenchmark {
    @Param({"10", "100", "10000"})
    int n;

    private byte[] randomData;
    private ByteArrayInputStream byteArrayInputStream;
    private ByteArrayOutputStream byteArrayOutputStream;

    @BeforeExperiment
    public void setUp() {
        Random rng = new Random();
        randomData = new byte[n];
        rng.nextBytes(randomData);
        byteArrayInputStream = new ByteArrayInputStream(randomData);
        byteArrayOutputStream = new ByteArrayOutputStream(randomData.length);
    }

    @Benchmark
    public void copy(int reps) throws IOException {
        for (int i = 0; i < reps; i++) {
            ByteStreams.copy(byteArrayInputStream, byteArrayOutputStream);
            byteArrayInputStream.reset();
            byteArrayOutputStream.reset();
        }
    }

    @Benchmark
    public void toByteArray(int reps) throws IOException {
        for (int i = 0; i < reps; i++) {
            ByteStreams.toByteArray(byteArrayInputStream);
            byteArrayInputStream.reset();
        }
    }

    @Benchmark
    public void toByteArrayWithExpectedSize(int reps) throws IOException {
        for (int i = 0; i < reps; i++) {
            ByteStreams.toByteArray(byteArrayInputStream, randomData.length);
            byteArrayInputStream.reset();
        }
    }

    @Benchmark
    public void skipFully(int reps) throws IOException {
        for (int i = 0; i < reps; i++) {
            ByteStreams.skipFully(byteArrayInputStream, randomData.length);
            byteArrayInputStream.reset();
        }
    }

    @Benchmark
    public void limitOneQuarter(int reps) throws IOException {
        for (int i = 0; i < reps; i++) {
            ByteStreams.limit(byteArrayInputStream, Math.round(randomData.length * 0.25));
            byteArrayInputStream.reset();
        }
    }

    @Benchmark
    public void limitHalf(int reps) throws IOException {
        for (int i = 0; i < reps; i++) {
            ByteStreams.limit(byteArrayInputStream, Math.round(randomData.length * 0.5));
            byteArrayInputStream.reset();
        }
    }

    @Benchmark
    public void limitThreeQuarter(int reps) throws IOException {
        for (int i = 0; i < reps; i++) {
            ByteStreams.limit(byteArrayInputStream, Math.round(randomData.length * 0.75));
            byteArrayInputStream.reset();
        }
    }

    @Benchmark
    public void readBytes(int reps) throws IOException {
        for (int i = 0; i < reps; i++) {
            ByteStreams.readBytes(
                    byteArrayInputStream,
                    new ByteProcessor<Object>() {
                        @Override
                        public boolean processBytes(byte[] buf, int off, int len) throws IOException {
                            return true;
                        }

                        @Override
                        public Object getResult() {
                            return null;
                        }
                    }
            );
            byteArrayInputStream.reset();
        }
    }
}

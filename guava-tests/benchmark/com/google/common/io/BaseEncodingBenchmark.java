/*
 * Copyright (C) 2014 The Guava Authors
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Random;

/** Benchmark for {@code BaseEncoding} performance. */
public class BaseEncodingBenchmark {
  private static final int INPUTS_COUNT = 0x1000;
  private static final int INPUTS_MASK = 0xFFF;

  enum EncodingOption {
    BASE64(BaseEncoding.base64()),
    BASE64_URL(BaseEncoding.base64Url()),
    BASE32(BaseEncoding.base32()),
    BASE32_HEX(BaseEncoding.base32Hex()),
    BASE16(BaseEncoding.base16());

    final BaseEncoding encoding;

    EncodingOption(BaseEncoding encoding) {
      this.encoding = encoding;
    }
  }

  @Param EncodingOption encoding;

  @Param({"10", "100", "10000"})
  int n;

  private final byte[][] encodingInputs = new byte[INPUTS_COUNT][];
  private final String[] decodingInputs = new String[INPUTS_COUNT];

  @BeforeExperiment
  public void setUp() {
    Random rng = new Random();
    for (int i = 0; i < encodingInputs.length; i++) {
      encodingInputs[i] = new byte[n];
      rng.nextBytes(encodingInputs[i]);
      decodingInputs[i] = encoding.encoding.encode(encodingInputs[i]);
    }
  }

  @Benchmark
  public int encode(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      tmp += System.identityHashCode(encoding.encoding.encode(encodingInputs[i & INPUTS_MASK]));
    }
    return tmp;
  }

  @Benchmark
  public int decode(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      tmp += System.identityHashCode(encoding.encoding.decode(decodingInputs[i & INPUTS_MASK]));
    }
    return tmp;
  }

  @Benchmark
  public int encodingStream(int reps) throws IOException {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      StringWriter target = new StringWriter(2 * n);
      OutputStream encodingStream = encoding.encoding.encodingStream(target);
      encodingStream.write(encodingInputs[i & INPUTS_MASK]);
      encodingStream.close();
      tmp += target.getBuffer().length();
    }
    return tmp;
  }

  @Benchmark
  public int decodingStream(int reps) throws IOException {
    int tmp = 0;
    byte[] target = new byte[n];
    for (int i = 0; i < reps; i++) {
      StringReader source = new StringReader(decodingInputs[i & INPUTS_MASK]);
      InputStream decodingStream = encoding.encoding.decodingStream(source);
      decodingStream.read(target);
      decodingStream.close();
      tmp += target[0];
    }
    return tmp;
  }
}

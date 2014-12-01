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

import java.util.Random;

/**
 * Benchmark for {@code BaseEncoding} performance.
 */
public class BaseEncodingBenchmark {
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
  
  @Param
  EncodingOption encoding;
  
  @Param({"10", "100", "10000"})
  int n;
  
  private final byte[][] inputs = new byte[0x1000][];

  @BeforeExperiment
  public void setUp() {
    Random rng = new Random();
    for (int i = 0; i < inputs.length; i++) {
      inputs[i] = new byte[n];
      rng.nextBytes(inputs[i]);
    }
  }
  
  @Benchmark public int encode(int reps) {
    int tmp = 0;
    for (int i = 0; i < reps; i++) {
      tmp += System.identityHashCode(encoding.encoding.encode(inputs[i & 0xFFF]));
    }
    return tmp;
  }
}

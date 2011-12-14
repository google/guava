/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.hash;

import static com.google.common.hash.Hashing.murmur3_32;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashTestUtils.HashFn;

import junit.framework.TestCase;

/**
 * Tests for Murmur3Hash32.
 */
public class Murmur3Hash32Test extends TestCase {
  public void testParanoid() {
    HashFn hf = new HashFn() {
      @Override public byte[] hash(byte[] input, int seed) {
        Hasher hasher = murmur3_32(seed).newHasher();
        Funnels.byteArrayFunnel().funnel(input, hasher);
        return hasher.hash().asBytes();
      }
    };
    // the magic number comes from:
    // http://code.google.com/p/smhasher/source/browse/trunk/main.cpp, #72
    HashTestUtils.verifyHashFunction(hf, 32, 0xB0F57EE3);
  }
}

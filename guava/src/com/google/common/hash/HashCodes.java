/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.hash;

/**
 * Static factories for {@link HashCode} instances.
 * 
 * @author andreou@google.com (Dimitris Andreou)
 */
final class HashCodes {
  private HashCodes() { }
  
  /**
   * Creates a 32-bit {@code HashCode}, of which the bytes will form the passed int, interpreted 
   * in little endian order.
   */
  static HashCode fromInt(int hash) {
    return new IntHashCode(hash);
  }
  
  private static class IntHashCode extends HashCode {
    final int hash;
    
    IntHashCode(int hash) {
      this.hash = hash;
    }

    @Override public int bits() {
      return 32;
    }

    @Override public byte[] asBytes() {
      return new byte[] {
          (byte) hash,
          (byte) (hash >> 8),
          (byte) (hash >> 16),
          (byte) (hash >> 24)};
    }
    
    @Override public int asInt() {
      return hash;
    }

    @Override public long asLong() {
      throw new IllegalStateException("this HashCode only has 32 bits; cannot create a long");
    }
  }
  
  /**
   * Creates a 64-bit {@code HashCode}, of which the bytes will form the passed long, interpreted 
   * in little endian order.
   */
  static HashCode fromLong(long hash) {
    return new LongHashCode(hash);
  }
  
  private static class LongHashCode extends HashCode {
    final long hash;
    
    LongHashCode(long hash) {
      this.hash = hash;
    }

    @Override public int bits() {
      return 64;
    }

    @Override public byte[] asBytes() {
      return new byte[] {
          (byte) hash,
          (byte) (hash >> 8),
          (byte) (hash >> 16),
          (byte) (hash >> 24),
          (byte) (hash >> 32),
          (byte) (hash >> 40),
          (byte) (hash >> 48),
          (byte) (hash >> 56)};
    }

    @Override public int asInt() {
      return (int) hash;
    }

    @Override public long asLong() {
      return hash;
    }
  }
  
  /**
   * Creates a {@code HashCode} from a byte array. The array is <i>not</i> copied defensively, 
   * so it must be handed-off so as to preserve the immutability contract of {@code HashCode}.
   * The array must be at least of length 4 (not checked). 
   */
  static HashCode fromBytes(byte[] bytes) {
    return new BytesHashCode(bytes);
  }
  
  private static class BytesHashCode extends HashCode {
    final byte[] bytes;
    
    BytesHashCode(byte[] bytes) {
      this.bytes = bytes;
    }

    @Override public int bits() {
      return bytes.length * 8;
    }

    @Override public byte[] asBytes() {
      return bytes.clone();
    }

    @Override public int asInt() {
      return (bytes[0] & 0xFF)
          | ((bytes[1] & 0xFF) << 8)
          | ((bytes[2] & 0xFF) << 16)
          | ((bytes[3] & 0xFF) << 24);
    }

    @Override public long asLong() {
      if (bytes.length < 8) {
        // Checking this to throw the correct type of exception
        throw new IllegalStateException("Not enough bytes");
      }
      return (bytes[0] & 0xFFL)
          | ((bytes[1] & 0xFFL) << 8)
          | ((bytes[2] & 0xFFL) << 16)
          | ((bytes[3] & 0xFFL) << 24)
          | ((bytes[4] & 0xFFL) << 32)
          | ((bytes[5] & 0xFFL) << 40)
          | ((bytes[6] & 0xFFL) << 48)
          | ((bytes[7] & 0xFFL) << 56);
    }
  }
}

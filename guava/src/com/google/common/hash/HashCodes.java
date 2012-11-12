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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.Beta;
import com.google.common.primitives.UnsignedInts;

import java.io.Serializable;

/**
 * Static factories for creating {@link HashCode} instances; most users should never have to use
 * this. All returned instances are {@link Serializable}.
 *
 * @author Dimitris Andreou
 * @since 12.0
 */
@Beta
public final class HashCodes {
  private HashCodes() {}

  /**
   * Creates a 32-bit {@code HashCode}, of which the bytes will form the passed int, interpreted 
   * in little endian order.
   */
  public static HashCode fromInt(int hash) {
    return new IntHashCode(hash);
  }
  
  private static final class IntHashCode extends HashCode implements Serializable {
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

    @Override
    public long padToLong() {
      return UnsignedInts.toLong(hash);
    }

    private static final long serialVersionUID = 0;
  }
  
  /**
   * Creates a 64-bit {@code HashCode}, of which the bytes will form the passed long, interpreted 
   * in little endian order.
   */
  public static HashCode fromLong(long hash) {
    return new LongHashCode(hash);
  }
  
  private static final class LongHashCode extends HashCode implements Serializable {
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

    @Override
    public long padToLong() {
      return hash;
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Creates a {@code HashCode} from a byte array. The array is defensively copied to preserve
   * the immutability contract of {@code HashCode}. The array cannot be empty.
   */
  public static HashCode fromBytes(byte[] bytes) {
    checkArgument(bytes.length >= 1, "A HashCode must contain at least 1 byte.");
    return fromBytesNoCopy(bytes.clone());
  }

  /**
   * Creates a {@code HashCode} from a byte array. The array is <i>not</i> copied defensively, 
   * so it must be handed-off so as to preserve the immutability contract of {@code HashCode}.
   */
  static HashCode fromBytesNoCopy(byte[] bytes) {
    return new BytesHashCode(bytes);
  }
  
  private static final class BytesHashCode extends HashCode implements Serializable {
    final byte[] bytes;
    
    BytesHashCode(byte[] bytes) {
      this.bytes = checkNotNull(bytes);
    }

    @Override public int bits() {
      return bytes.length * 8;
    }

    @Override public byte[] asBytes() {
      return bytes.clone();
    }

    @Override public int asInt() {
      checkState(bytes.length >= 4,
          "HashCode#asInt() requires >= 4 bytes (it only has %s bytes).", bytes.length);
      return (bytes[0] & 0xFF)
          | ((bytes[1] & 0xFF) << 8)
          | ((bytes[2] & 0xFF) << 16)
          | ((bytes[3] & 0xFF) << 24);
    }

    @Override public long asLong() {
      checkState(bytes.length >= 8,
          "HashCode#asLong() requires >= 8 bytes (it only has %s bytes).", bytes.length);
      return (bytes[0] & 0xFFL)
          | ((bytes[1] & 0xFFL) << 8)
          | ((bytes[2] & 0xFFL) << 16)
          | ((bytes[3] & 0xFFL) << 24)
          | ((bytes[4] & 0xFFL) << 32)
          | ((bytes[5] & 0xFFL) << 40)
          | ((bytes[6] & 0xFFL) << 48)
          | ((bytes[7] & 0xFFL) << 56);
    }

    @Override
    public long padToLong() {
      return (bytes.length < 8) ? UnsignedInts.toLong(asInt()) : asLong();
    }

    @Override
    public int hashCode() {
      if (bytes.length >= 4) {
        return asInt();
      } else {
        int val = (bytes[0] & 0xFF);
        for (int i = 1; i < bytes.length; i++) {
          val |= ((bytes[i] & 0xFF) << (i * 8));
        }
        return val;
      }
    }

    private static final long serialVersionUID = 0;
  }
}

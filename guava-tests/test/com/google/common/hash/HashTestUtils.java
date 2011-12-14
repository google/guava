// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.hash;

import com.google.common.primitives.Ints;

import org.junit.Assert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

/**
 * @author andreou@google.com (Dimitris Andreou)
 */
class HashTestUtils {
  private HashTestUtils() {}

  /**
   * Converts a string, which should contain only ascii-representable characters, to a byte[].
   */
  static byte[] ascii(String string) {
    byte[] bytes = new byte[string.length()];
    for (int i = 0; i < string.length(); i++) {
      bytes[i] = (byte) string.charAt(i);
    }
    return bytes;
  }

  /**
   * Returns a byte array representation for a sequence of longs, in big-endian order. 
   */
  static byte[] toBytes(ByteOrder bo, long... longs) {
    ByteBuffer bb = ByteBuffer.wrap(new byte[longs.length * 8]).order(bo);
    for (long x : longs) {
      bb.putLong(x);
    }
    return bb.array();
  }

  interface HashFn {
    byte[] hash(byte[] input, int seed);
  }

  static void verifyHashFunction(HashFn hashFunction, int hashbits, int expected) {
    int hashBytes = hashbits / 8;

    byte[] key = new byte[256];
    byte[] hashes = new byte[hashBytes * 256];

    // Hash keys of the form {}, {0}, {0,1}, {0,1,2}... up to N=255,using 256-N as the seed
    for (int i = 0; i < 256; i++) {
      key[i] = (byte) i;
      int seed = 256 - i;
      byte[] hash = hashFunction.hash(Arrays.copyOf(key, i), seed);
      System.arraycopy(hash, 0, hashes, i * hashBytes, hash.length);
    }
    
    // Then hash the result array
    byte[] result = hashFunction.hash(hashes, 0);
    
    // interpreted in little-endian order.
    int verification = Integer.reverseBytes(Ints.fromByteArray(result)); 

    if (expected != verification) {
      throw new AssertionError("Expected: " + Integer.toHexString(expected)
          + " got: " + Integer.toHexString(verification));
    }
  }
  
  static void assertEqualHashes(byte[] expectedHash, byte[] actualHash) {
    if (!Arrays.equals(expectedHash, actualHash)) {
      Assert.fail(String.format("Should be: %x, was %x", expectedHash, actualHash));
    }
  }
  
  static final Funnel<Object> BAD_FUNNEL = new Funnel<Object>() {
    @Override public void funnel(Object object, Sink byteSink) {
      byteSink.putInt(object.hashCode());
    }
  };
  
  static enum RandomHasherAction {
    PUT_BOOLEAN() {
      @Override void performAction(Random random, Iterable<? extends Sink> sinks) {
        boolean value = random.nextBoolean();
        for (Sink sink : sinks) {
          sink.putBoolean(value);
        }
      }
    },
    PUT_BYTE() {
      @Override void performAction(Random random, Iterable<? extends Sink> sinks) {
        int value = random.nextInt();
        for (Sink sink : sinks) {
          sink.putByte((byte) value);
        }
      }
    },
    PUT_SHORT() {
      @Override void performAction(Random random, Iterable<? extends Sink> sinks) {
        short value = (short) random.nextInt();
        for (Sink sink : sinks) {
          sink.putShort(value);
        }
      }
    },
    PUT_CHAR() {
      @Override void performAction(Random random, Iterable<? extends Sink> sinks) {
        char value = (char) random.nextInt();
        for (Sink sink : sinks) {
          sink.putChar(value);
        }
      }
    },
    PUT_INT() {
      @Override void performAction(Random random, Iterable<? extends Sink> sinks) {
        int value = random.nextInt();
        for (Sink sink : sinks) {
          sink.putInt(value);
        }
      }
    },
    PUT_LONG() {
      @Override void performAction(Random random, Iterable<? extends Sink> sinks) {
        long value = random.nextLong();
        for (Sink sink : sinks) {
          sink.putLong(value);
        }
      }
    },
    PUT_FLOAT() {
      @Override void performAction(Random random, Iterable<? extends Sink> sinks) {
        float value = random.nextFloat();
        for (Sink sink : sinks) {
          sink.putFloat(value);
        }
      }
    },
    PUT_DOUBLE() {
      @Override void performAction(Random random, Iterable<? extends Sink> sinks) {
        double value = random.nextDouble();
        for (Sink sink : sinks) {
          sink.putDouble(value);
        }
      }
    },
    PUT_BYTES() {
      @Override void performAction(Random random, Iterable<? extends Sink> sinks) {
        byte[] value = new byte[random.nextInt(128)];
        random.nextBytes(value);
        for (Sink sink : sinks) {
          sink.putBytes(value);
        }
      }
    },
    PUT_BYTES_INT_INT() {
      @Override void performAction(Random random, Iterable<? extends Sink> sinks) {
        byte[] value = new byte[random.nextInt(128)];
        random.nextBytes(value);
        int off = random.nextInt(value.length + 1);
        int len = random.nextInt(value.length - off + 1);
        for (Sink sink : sinks) {
          sink.putBytes(value);
        }
      }
    };
    
    abstract void performAction(Random random, Iterable<? extends Sink> sinks);
    
    private static final RandomHasherAction[] actions = values();
    
    static RandomHasherAction pickAtRandom(Random random) {
      return actions[random.nextInt(actions.length)];
    }
  }
}

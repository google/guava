// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.hash;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashTestUtils.RandomHasherAction;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Tests for AbstractNonStreamingHashFunction.
 */
public class AbstractNonStreamingHashFunctionTest extends TestCase {
  /**
   * Constructs two trivial HashFunctions (output := input), one streaming and one non-streaming,
   * and checks that their results are identical, no matter which newHasher version we used.
   */
  public void test() {
    List<Hasher> hashers = ImmutableList.of( 
        new StreamingVersion().newHasher(),
        new StreamingVersion().newHasher(52),
        new NonStreamingVersion().newHasher(),
        new NonStreamingVersion().newHasher(123));
    Random random = new Random(0);
    for (int i = 0; i < 200; i++) {
      RandomHasherAction.pickAtRandom(random).performAction(random, hashers);
    }
    HashCode[] codes = new HashCode[hashers.size()];
    for (int i = 0; i < hashers.size(); i++) {
      codes[i] = hashers.get(i).hash();
    }
    for (int i = 1; i < codes.length; i++) {
      assertEquals(codes[i - 1], codes[i]);
    }
  }
  
  static class StreamingVersion extends AbstractStreamingHashFunction {
    @Override
    public int bits() {
      return 32;
    }

    @Override
    public Hasher newHasher() {
      return new AbstractStreamingHasher(4, 4) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        @Override
        HashCode makeHash() {
          return HashCodes.fromBytes(out.toByteArray());
        }

        @Override
        protected void process(ByteBuffer bb) {
          while (bb.hasRemaining()) {
            out.write(bb.get());
          }
        }

        @Override
        protected void processRemaining(ByteBuffer bb) {
          while (bb.hasRemaining()) {
            out.write(bb.get());
          }
        }
      };
    }
  }
  
  static class NonStreamingVersion extends AbstractNonStreamingHashFunction {
    @Override
    public int bits() {
      return 32;
    }

    @Override
    public HashCode hashBytes(byte[] input) {
      return HashCodes.fromBytes(input);
    }

    @Override
    public HashCode hashBytes(byte[] input, int off, int len) {
      return HashCodes.fromBytes(Arrays.copyOfRange(input, off, off + len));
    }
    
    @Override
    public HashCode hashString(CharSequence input) {
      throw new UnsupportedOperationException();
    }
    
    @Override
    public HashCode hashString(CharSequence input, Charset charset) {
      throw new UnsupportedOperationException();
    }

    @Override
    public HashCode hashLong(long input) {
      throw new UnsupportedOperationException();
    }
  }
}

// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.hash;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.hash.AbstractStreamingHashFunction.AbstractStreamingHasher;
import com.google.common.hash.HashTestUtils.RandomHasherAction;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Tests for AbstractHashSink.
 * 
 * @author andreou@google.com (Dimitris Andreou)
 */
public class AbstractStreamingHasherTest extends TestCase {
  /** Test we get the HashCode that is created by the sink. Later we ignore the result */
  public void testSanity() { 
    Sink sink = new Sink(4);
    assertEquals(0xDeadBeef, sink.makeHash().asInt());
  }
  
  public void testBytes() {
    Sink sink = new Sink(4); // byte order insignificant here
    byte[] expected = { 1, 2, 3, 4, 5, 6, 7, 8 };
    sink.putByte((byte) 1);
    sink.putBytes(new byte[] { 2, 3, 4, 5, 6 });
    sink.putByte((byte) 7);
    sink.putBytes(new byte[] { });
    sink.putBytes(new byte[] { 8 });
    sink.hash();
    sink.assertInvariants(8);
    sink.assertBytes(expected);
  }
  
  public void testShort() {
    Sink sink = new Sink(4);
    sink.putShort((short) 0x0201);
    sink.hash();
    sink.assertInvariants(2);
    sink.assertBytes(new byte[] { 1, 2, 0, 0 }); // padded with zeros
  }
  
  public void testInt() {
    Sink sink = new Sink(4);
    sink.putInt(0x04030201);
    sink.hash();
    sink.assertInvariants(4);
    sink.assertBytes(new byte[] { 1, 2, 3, 4 });
  }
  
  public void testLong() {
    Sink sink = new Sink(8);
    sink.putLong(0x0807060504030201L);
    sink.hash();
    sink.assertInvariants(8);
    sink.assertBytes(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
  }
  
  public void testChar() {
    Sink sink = new Sink(4);
    sink.putChar((char) 0x0201);
    sink.hash();
    sink.assertInvariants(2);
    sink.assertBytes(new byte[] { 1, 2, 0, 0  }); // padded with zeros
  }
  
  public void testFloat() {
    Sink sink = new Sink(4);
    sink.putFloat(Float.intBitsToFloat(0x04030201));
    sink.hash();
    sink.assertInvariants(4);
    sink.assertBytes(new byte[] { 1, 2, 3, 4 });
  }
  
  public void testDouble() {
    Sink sink = new Sink(8);
    sink.putDouble(Double.longBitsToDouble(0x0807060504030201L));
    sink.hash();
    sink.assertInvariants(8);
    sink.assertBytes(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 });
  }
  
  public void testCorrectExceptions() {
    Sink sink = new Sink(4);
    try {
      sink.putBytes(new byte[8], -1, 4);
      fail();
    } catch (IndexOutOfBoundsException ok) {}
    try {
      sink.putBytes(new byte[8], 0, 16);
      fail();
    } catch (IndexOutOfBoundsException ok) {}
    try {
      sink.putBytes(new byte[8], 0, -1);
      fail();
    } catch (IndexOutOfBoundsException ok) {}
  }
  
  /**
   * This test creates a long random sequence of inputs, then a lot of differently configured
   * sinks process it; all should produce the same answer, the only difference should be the
   * number of process()/processRemaining() invocations, due to alignment.  
   */
  public void testExhaustive() throws Exception {
    Random random = new Random(0); // will iteratively make more debuggable, each time it breaks
    for (int totalInsertions = 0; totalInsertions < 200; totalInsertions++) {
      
      List<Sink> sinks = Lists.newArrayList();
      for (int chunkSize = 4; chunkSize <= 32; chunkSize++) {
        for (int bufferSize = chunkSize; bufferSize <= chunkSize * 4; bufferSize += chunkSize) {
          // yes, that's a lot of sinks!
          sinks.add(new Sink(chunkSize, bufferSize));
          // For convenience, testing only with big endianness, to match DataOutputStream.
          // I regard highly unlikely that both the little endianness tests above and this one
          // passes, and there is still a little endianness bug lurking around. 
        }
      }
      
      Control control = new Control();
      Hasher controlSink = control.newHasher(1024);
      
      Iterable<Hasher> sinksAndControl = Iterables.concat(
          sinks, Collections.singleton(controlSink));
      for (int insertion = 0; insertion < totalInsertions; insertion++) {
        RandomHasherAction.pickAtRandom(random).performAction(random, sinksAndControl); 
      }
      for (Sink sink : sinks) {
        sink.hash();
      }
      
      byte[] expected = controlSink.hash().asBytes();
      for (Sink sink : sinks) {
        sink.assertInvariants(expected.length);
        sink.assertBytes(expected);
      }
    }
  }
  
  private static class Sink extends AbstractStreamingHasher {
    final int chunkSize;
    final int bufferSize;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    int processCalled = 0;
    boolean remainingCalled = false;
    
    Sink(int chunkSize, int bufferSize) {
      super(chunkSize, bufferSize);
      this.chunkSize = chunkSize;
      this.bufferSize = bufferSize;
    }

    Sink(int chunkSize) {
      super(chunkSize);
      this.chunkSize = chunkSize;
      this.bufferSize = chunkSize;
    }

    @Override HashCode makeHash() {
      return HashCodes.fromInt(0xDeadBeef);
    }

    @Override protected void process(ByteBuffer bb) {
      processCalled++;
      assertEquals(ByteOrder.LITTLE_ENDIAN, bb.order());
      assertTrue(bb.remaining() >= chunkSize);
      for (int i = 0; i < chunkSize; i++) {
        out.write(bb.get());
      }
    }
    
    @Override protected void processRemaining(ByteBuffer bb) {
      assertFalse(remainingCalled);
      remainingCalled = true;
      assertEquals(ByteOrder.LITTLE_ENDIAN, bb.order());
      assertTrue(bb.remaining() > 0);
      assertTrue(bb.remaining() < bufferSize);
      int before = processCalled;
      super.processRemaining(bb);
      int after = processCalled;
      assertEquals(before + 1, after); // default implementation pads and calls process()
      processCalled--; // don't count the tail invocation (makes tests a bit more understandable)
    }
    
    // ensures that the number of invocations looks sane
    void assertInvariants(int expectedBytes) {
      // we should have seen as many bytes as the next multiple of chunk after expectedBytes - 1 
      assertEquals(out.toByteArray().length, ceilToMultiple(expectedBytes, chunkSize));
      assertEquals(expectedBytes / chunkSize, processCalled);
      assertEquals(expectedBytes % chunkSize != 0, remainingCalled);
    }
    
    // returns the minimum x such as x >= a && (x % b) == 0
    private static int ceilToMultiple(int a, int b) {
      int remainder = a % b;
      return remainder == 0 ? a : a + b - remainder;
    }
    
    void assertBytes(byte[] expected) {
      byte[] got = out.toByteArray();
      for (int i = 0; i < expected.length; i++) {
        assertEquals(expected[i], got[i]);
      }
    }
  }
  
  private static class Control extends AbstractNonStreamingHashFunction {
    @Override
    public HashCode hashBytes(byte[] input) {
      return HashCodes.fromBytes(input);
    }
    
    @Override
    public HashCode hashBytes(byte[] input, int off, int len) {
      return hashBytes(Arrays.copyOfRange(input, off, off + len));
    }

    @Override
    public int bits() {
      throw new UnsupportedOperationException();
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

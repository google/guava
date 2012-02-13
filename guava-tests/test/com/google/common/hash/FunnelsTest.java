// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.hash;

import com.google.common.hash.AbstractStreamingHashFunction.AbstractStreamingHasher;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.nio.ByteBuffer;

/**
 * Tests for HashExtractors.
 * 
 * @author andreou@google.com (Dimitris Andreou)
 */
public class FunnelsTest extends TestCase {
  public void testForBytes() {
    PrimitiveSink bytePrimitiveSink = EasyMock.createMock(PrimitiveSink.class);

    EasyMock.expect(bytePrimitiveSink.putBytes(EasyMock.aryEq(new byte[] { 4, 3, 2, 1})))
        .andReturn(bytePrimitiveSink).once();
    EasyMock.replay(bytePrimitiveSink);

    Funnels.byteArrayFunnel().funnel(new byte[]{4, 3, 2, 1}, bytePrimitiveSink);

    EasyMock.verify(bytePrimitiveSink);
  }

  public void testForBytes_null() {
    assertNullsThrowException(Funnels.byteArrayFunnel());
  }

  public void testForStrings() {

    PrimitiveSink bytePrimitiveSink = EasyMock.createMock(PrimitiveSink.class);
    
    EasyMock.expect(bytePrimitiveSink.putString("test")).andReturn(bytePrimitiveSink).once();
    EasyMock.replay(bytePrimitiveSink);
    
    Funnels.stringFunnel().funnel("test", bytePrimitiveSink);
    
    EasyMock.verify(bytePrimitiveSink);
  }
  
  public void testForStrings_null() {
    assertNullsThrowException(Funnels.stringFunnel());
  }
  
  private static void assertNullsThrowException(Funnel<?> funnel) {
    PrimitiveSink bytePrimitiveSink = new AbstractStreamingHasher(4, 4) {
      @Override HashCode makeHash() { throw new UnsupportedOperationException(); }

      @Override protected void process(ByteBuffer bb) {
        while (bb.hasRemaining()) {
          bb.get();
        }
      }
    };
    try {
      funnel.funnel(null, bytePrimitiveSink);
      fail();
    } catch (NullPointerException ok) {}
  }
}

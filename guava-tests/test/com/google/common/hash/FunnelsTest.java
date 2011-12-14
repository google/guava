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
    Sink byteSink = EasyMock.createMock(Sink.class);

    EasyMock.expect(byteSink.putBytes(EasyMock.aryEq(new byte[] { 4, 3, 2, 1})))
        .andReturn(byteSink).once();
    EasyMock.replay(byteSink);

    Funnels.byteArrayFunnel().funnel(new byte[]{4, 3, 2, 1}, byteSink);

    EasyMock.verify(byteSink);
  }

  public void testForBytes_null() {
    assertNullsThrowException(Funnels.byteArrayFunnel());
  }

  public void testForStrings() {

    Sink byteSink = EasyMock.createMock(Sink.class);
    
    EasyMock.expect(byteSink.putString("test")).andReturn(byteSink).once();
    EasyMock.replay(byteSink);
    
    Funnels.stringFunnel().funnel("test", byteSink);
    
    EasyMock.verify(byteSink);
  }
  
  public void testForStrings_null() {
    assertNullsThrowException(Funnels.stringFunnel());
  }
  
  private static void assertNullsThrowException(Funnel<?> funnel) {
    Sink byteSink = new AbstractStreamingHasher(4, 4) {
      @Override HashCode makeHash() { throw new UnsupportedOperationException(); }

      @Override protected void process(ByteBuffer bb) {
        while (bb.hasRemaining()) {
          bb.get();
        }
      }
    };
    try {
      funnel.funnel(null, byteSink);
      fail();
    } catch (NullPointerException ok) {}
  }
}

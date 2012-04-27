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

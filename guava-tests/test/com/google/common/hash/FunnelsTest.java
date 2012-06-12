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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.hash.AbstractStreamingHashFunction.AbstractStreamingHasher;

import junit.framework.TestCase;

import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Tests for HashExtractors.
 *
 * @author Dimitris Andreou
 */
public class FunnelsTest extends TestCase {
  public void testForBytes() {
    PrimitiveSink bytePrimitiveSink = mock(PrimitiveSink.class);
    Funnels.byteArrayFunnel().funnel(new byte[] { 4, 3, 2, 1 }, bytePrimitiveSink);
    verify(bytePrimitiveSink).putBytes(new byte[] { 4, 3, 2, 1 });
  }

  public void testForBytes_null() {
    assertNullsThrowException(Funnels.byteArrayFunnel());
  }

  public void testForStrings() {
    PrimitiveSink bytePrimitiveSink = mock(PrimitiveSink.class);
    Funnels.stringFunnel().funnel("test", bytePrimitiveSink);
    verify(bytePrimitiveSink).putString("test");
  }

  public void testForStrings_null() {
    assertNullsThrowException(Funnels.stringFunnel());
  }

  public void testForInts() {
    Integer value = 1234;
    PrimitiveSink bytePrimitiveSink = mock(PrimitiveSink.class);
    Funnels.integerFunnel().funnel(value, bytePrimitiveSink);
    verify(bytePrimitiveSink).putInt(1234);
  }

  public void testForInts_null() {
    assertNullsThrowException(Funnels.integerFunnel());
  }

  public void testForLongs() {
    Long value = 1234L;
    PrimitiveSink bytePrimitiveSink = mock(PrimitiveSink.class);
    Funnels.longFunnel().funnel(value, bytePrimitiveSink);
    verify(bytePrimitiveSink).putLong(1234);
  }

  public void testForLongs_null() {
    assertNullsThrowException(Funnels.longFunnel());
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
  
  public void testAsOutputStream() throws Exception {
    PrimitiveSink sink = mock(PrimitiveSink.class);
    OutputStream out = Funnels.asOutputStream(sink);
    byte[] bytes = { 1, 2, 3, 4 };
    out.write(255);
    out.write(bytes);
    out.write(bytes, 1, 2);
    verify(sink).putByte((byte) 255);
    verify(sink).putBytes(bytes);
    verify(sink).putBytes(bytes, 1, 2);
  }
}

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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.base.Charsets;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import junit.framework.TestCase;
import org.mockito.InOrder;

/**
 * Tests for HashExtractors.
 *
 * @author Dimitris Andreou
 */
public class FunnelsTest extends TestCase {
  public void testForBytes() {
    PrimitiveSink primitiveSink = mock(PrimitiveSink.class);
    Funnels.byteArrayFunnel().funnel(new byte[] {4, 3, 2, 1}, primitiveSink);
    verify(primitiveSink).putBytes(new byte[] {4, 3, 2, 1});
  }

  public void testForBytes_null() {
    assertNullsThrowException(Funnels.byteArrayFunnel());
  }

  public void testForStrings() {
    PrimitiveSink primitiveSink = mock(PrimitiveSink.class);
    Funnels.unencodedCharsFunnel().funnel("test", primitiveSink);
    verify(primitiveSink).putUnencodedChars("test");
  }

  public void testForStrings_null() {
    assertNullsThrowException(Funnels.unencodedCharsFunnel());
  }

  public void testForStringsCharset() {
    for (Charset charset : Charset.availableCharsets().values()) {
      PrimitiveSink primitiveSink = mock(PrimitiveSink.class);
      Funnels.stringFunnel(charset).funnel("test", primitiveSink);
      verify(primitiveSink).putString("test", charset);
    }
  }

  public void testForStringsCharset_null() {
    for (Charset charset : Charset.availableCharsets().values()) {
      assertNullsThrowException(Funnels.stringFunnel(charset));
    }
  }

  public void testForInts() {
    Integer value = 1234;
    PrimitiveSink primitiveSink = mock(PrimitiveSink.class);
    Funnels.integerFunnel().funnel(value, primitiveSink);
    verify(primitiveSink).putInt(1234);
  }

  public void testForInts_null() {
    assertNullsThrowException(Funnels.integerFunnel());
  }

  public void testForLongs() {
    Long value = 1234L;
    PrimitiveSink primitiveSink = mock(PrimitiveSink.class);
    Funnels.longFunnel().funnel(value, primitiveSink);
    verify(primitiveSink).putLong(1234);
  }

  public void testForLongs_null() {
    assertNullsThrowException(Funnels.longFunnel());
  }

  public void testSequential() {
    // go/do-not-mock-common-types-lsc
    @SuppressWarnings({"unchecked", "DoNotMock"})
    Funnel<Object> elementFunnel = mock(Funnel.class);
    PrimitiveSink primitiveSink = mock(PrimitiveSink.class);
    Funnel<Iterable<?>> sequential = Funnels.sequentialFunnel(elementFunnel);
    sequential.funnel(Arrays.asList("foo", "bar", "baz", "quux"), primitiveSink);
    InOrder inOrder = inOrder(elementFunnel);
    inOrder.verify(elementFunnel).funnel("foo", primitiveSink);
    inOrder.verify(elementFunnel).funnel("bar", primitiveSink);
    inOrder.verify(elementFunnel).funnel("baz", primitiveSink);
    inOrder.verify(elementFunnel).funnel("quux", primitiveSink);
  }

  private static void assertNullsThrowException(Funnel<?> funnel) {
    PrimitiveSink primitiveSink =
        new AbstractStreamingHasher(4, 4) {
          @Override
          protected HashCode makeHash() {
            throw new UnsupportedOperationException();
          }

          @Override
          protected void process(ByteBuffer bb) {
            while (bb.hasRemaining()) {
              bb.get();
            }
          }
        };
    try {
      funnel.funnel(null, primitiveSink);
      fail();
    } catch (NullPointerException ok) {
    }
  }

  public void testAsOutputStream() throws Exception {
    PrimitiveSink sink = mock(PrimitiveSink.class);
    OutputStream out = Funnels.asOutputStream(sink);
    byte[] bytes = {1, 2, 3, 4};
    out.write(255);
    out.write(bytes);
    out.write(bytes, 1, 2);
    verify(sink).putByte((byte) 255);
    verify(sink).putBytes(bytes);
    verify(sink).putBytes(bytes, 1, 2);
  }

  public void testSerialization() {
    assertSame(
        Funnels.byteArrayFunnel(), SerializableTester.reserialize(Funnels.byteArrayFunnel()));
    assertSame(Funnels.integerFunnel(), SerializableTester.reserialize(Funnels.integerFunnel()));
    assertSame(Funnels.longFunnel(), SerializableTester.reserialize(Funnels.longFunnel()));
    assertSame(
        Funnels.unencodedCharsFunnel(),
        SerializableTester.reserialize(Funnels.unencodedCharsFunnel()));
    assertEquals(
        Funnels.sequentialFunnel(Funnels.integerFunnel()),
        SerializableTester.reserialize(Funnels.sequentialFunnel(Funnels.integerFunnel())));
    assertEquals(
        Funnels.stringFunnel(Charsets.US_ASCII),
        SerializableTester.reserialize(Funnels.stringFunnel(Charsets.US_ASCII)));
  }

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(Funnels.byteArrayFunnel())
        .addEqualityGroup(Funnels.integerFunnel())
        .addEqualityGroup(Funnels.longFunnel())
        .addEqualityGroup(Funnels.unencodedCharsFunnel())
        .addEqualityGroup(Funnels.stringFunnel(Charsets.UTF_8))
        .addEqualityGroup(Funnels.stringFunnel(Charsets.US_ASCII))
        .addEqualityGroup(
            Funnels.sequentialFunnel(Funnels.integerFunnel()),
            SerializableTester.reserialize(Funnels.sequentialFunnel(Funnels.integerFunnel())))
        .addEqualityGroup(Funnels.sequentialFunnel(Funnels.longFunnel()))
        .testEquals();
  }
}

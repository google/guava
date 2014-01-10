/*
 * Copyright (C) 2013 The Guava Authors
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

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;

import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.io.ByteArrayInputStream;

/**
 * Tests for {@link HashingInputStream}.
 *
 * @author Qian Huang
 */
public class HashingInputStreamTest extends TestCase {
  private Hasher hasher;
  private HashFunction hashFunction;
  private static final byte[] testBytes = new byte[] {'y', 'a', 'm', 's'};
  private ByteArrayInputStream buffer;

  @Override protected void setUp() throws Exception {
    super.setUp();
    hasher = EasyMock.createMock(Hasher.class);
    hashFunction = EasyMock.createMock(HashFunction.class);
    buffer = new ByteArrayInputStream(testBytes);

    EasyMock.expect(hashFunction.newHasher()).andReturn(hasher).once();
    EasyMock.replay(hashFunction);
  }

  public void testRead_putSingleByte() throws Exception {
    EasyMock.expect(hasher.putByte((byte) 'y')).andReturn(hasher).once();
    EasyMock.replay(hasher);
    HashingInputStream in = new HashingInputStream(hashFunction, buffer);

    int b = in.read();
    assertEquals('y', b);

    EasyMock.verify(hashFunction);
    EasyMock.verify(hasher);
  }

  public void testRead_putByteArray() throws Exception {
    EasyMock.expect(hasher.putBytes(aryEq(testBytes), eq(0), eq(testBytes.length)))
        .andReturn(hasher).once();
    EasyMock.replay(hasher);
    HashingInputStream in = new HashingInputStream(hashFunction, buffer);

    byte[] buf = new byte[4];
    int numOfByteRead = in.read(buf, 0, buf.length);
    assertEquals(4, numOfByteRead);
    for (int i = 0; i < testBytes.length; i++) {
      assertEquals(testBytes[i], buf[i]);
    }

    EasyMock.verify(hashFunction);
    EasyMock.verify(hasher);
  }

  public void testRead_putByteArrayAtPos() throws Exception {
    EasyMock.expect(hasher.putBytes(aryEq(new byte[] {'y', 'a', 'm'}), eq(0), eq(3)))
        .andReturn(hasher).once();
    EasyMock.replay(hasher);
    HashingInputStream in = new HashingInputStream(hashFunction, buffer);

    byte[] buf = new byte[3];
    int numOfByteRead = in.read(buf, 0, 3);
    assertEquals(3, numOfByteRead);
    for (int i = 0; i < numOfByteRead; i++) {
      assertEquals(testBytes[i], buf[i]);
    }

    EasyMock.verify(hashFunction);
    EasyMock.verify(hasher);
  }

  public void testRead_putByteArrayOutOfBound() throws Exception {
    byte[] buf = new byte[100];
    byte[] expectedBytes = buf.clone();
    System.arraycopy(testBytes, 0, expectedBytes, 0, testBytes.length);

    EasyMock.expect(hasher.putBytes(aryEq(expectedBytes), eq(0), eq(4)))
        .andReturn(hasher).once();
    EasyMock.replay(hasher);
    HashingInputStream in = new HashingInputStream(hashFunction, buffer);

    int numOfByteRead = in.read(buf, 0, 100);
    assertEquals(4, numOfByteRead);
    for (int i = 0; i < numOfByteRead; i++) {
      assertEquals(testBytes[i], buf[i]);
    }

    EasyMock.verify(hashFunction);
    EasyMock.verify(hasher);
  }

  public void testHash_hashesCorrectly() throws Exception {
    HashCode expectedHash = Hashing.md5().hashBytes(testBytes);
    HashingInputStream in = new HashingInputStream(Hashing.md5(), buffer);

    byte[] buf = new byte[4];
    int numOfByteRead = in.read(buf, 0, buf.length);
    assertEquals(4, numOfByteRead);

    assertEquals(expectedHash, in.hash());
  }

  public void testHash_hashesCorrectlyReadOutOfBound() throws Exception {
    HashCode expectedHash = Hashing.md5().hashBytes(testBytes);
    HashingInputStream in = new HashingInputStream(Hashing.md5(), buffer);

    byte[] buf = new byte[100];
    int numOfByteRead = in.read(buf, 0, buf.length);
    assertEquals(-1, in.read()); // additional read
    assertEquals(4, numOfByteRead);

    assertEquals(expectedHash, in.hash());
  }

  public void testHash_hashesCorrectlyForSkipping() throws Exception {
    HashCode expectedHash = Hashing.md5().hashBytes(new byte[] {'m', 's'});
    HashingInputStream in = new HashingInputStream(Hashing.md5(), buffer);

    long numOfByteSkipped = in.skip(2);
    assertEquals(2, numOfByteSkipped);

    byte[] buf = new byte[4];
    int numOfByteRead = in.read(buf, 0, buf.length);
    assertEquals(2, numOfByteRead);

    assertEquals(expectedHash, in.hash());
  }

  public void testChecksForNull() throws Exception {
    NullPointerTester tester = new NullPointerTester();

    tester.testAllPublicInstanceMethods(new HashingInputStream(Hashing.md5(), buffer));
    tester.testAllPublicStaticMethods(HashingInputStream.class);
    tester.testAllPublicConstructors(HashingInputStream.class);
  }
}

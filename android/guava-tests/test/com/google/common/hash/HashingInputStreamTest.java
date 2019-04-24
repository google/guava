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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.testing.NullPointerTester;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import junit.framework.TestCase;

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

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    hasher = mock(Hasher.class);
    hashFunction = mock(HashFunction.class);
    buffer = new ByteArrayInputStream(testBytes);

    when(hashFunction.newHasher()).thenReturn(hasher);
  }

  public void testRead_putSingleByte() throws Exception {
    HashingInputStream in = new HashingInputStream(hashFunction, buffer);

    int b = in.read();
    assertEquals('y', b);

    verify(hasher).putByte((byte) 'y');
    verify(hashFunction).newHasher();
    verifyNoMoreInteractions(hashFunction, hasher);
  }

  public void testRead_putByteArray() throws Exception {
    HashingInputStream in = new HashingInputStream(hashFunction, buffer);

    byte[] buf = new byte[4];
    int numOfByteRead = in.read(buf, 0, buf.length);
    assertEquals(4, numOfByteRead);
    for (int i = 0; i < testBytes.length; i++) {
      assertEquals(testBytes[i], buf[i]);
    }

    verify(hasher).putBytes(testBytes, 0, testBytes.length);
    verify(hashFunction).newHasher();
    verifyNoMoreInteractions(hashFunction, hasher);
  }

  public void testRead_putByteArrayAtPos() throws Exception {
    HashingInputStream in = new HashingInputStream(hashFunction, buffer);

    byte[] buf = new byte[3];
    int numOfByteRead = in.read(buf, 0, 3);
    assertEquals(3, numOfByteRead);
    for (int i = 0; i < numOfByteRead; i++) {
      assertEquals(testBytes[i], buf[i]);
    }

    verify(hasher).putBytes(Arrays.copyOf(testBytes, 3), 0, 3);
    verify(hashFunction).newHasher();
    verifyNoMoreInteractions(hashFunction, hasher);
  }

  public void testRead_putByteArrayOutOfBound() throws Exception {
    byte[] buf = new byte[100];
    byte[] expectedBytes = buf.clone();
    System.arraycopy(testBytes, 0, expectedBytes, 0, testBytes.length);

    HashingInputStream in = new HashingInputStream(hashFunction, buffer);

    int numOfByteRead = in.read(buf, 0, 100);
    assertEquals(4, numOfByteRead);
    for (int i = 0; i < numOfByteRead; i++) {
      assertEquals(testBytes[i], buf[i]);
    }

    verify(hasher).putBytes(expectedBytes, 0, 4);
    verify(hashFunction).newHasher();
    verifyNoMoreInteractions(hashFunction, hasher);
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

/*
 * Copyright (C) 2011 The Guava Authors
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

import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.io.ByteArrayOutputStream;

/**
 * Tests for {@link HashingOutputStream}.
 *
 * @author Nick Piepmeier
 */
public class HashingOutputStreamTest extends TestCase {
  private Hasher hasher;
  private HashFunction hashFunction;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  @Override protected void setUp() throws Exception {
    super.setUp();
    hasher = EasyMock.createMock(Hasher.class);
    hashFunction = EasyMock.createMock(HashFunction.class);

    EasyMock.expect(hashFunction.newHasher()).andReturn(hasher).once();
    EasyMock.replay(hashFunction);
  }

  public void testWrite_putSingleByte() throws Exception {
    int b = 'q';
    EasyMock.expect(hasher.putByte((byte) b)).andReturn(hasher).once();
    EasyMock.replay(hasher);
    HashingOutputStream out = new HashingOutputStream(hashFunction, buffer);

    out.write(b);

    EasyMock.verify(hashFunction);
    EasyMock.verify(hasher);
  }

  public void testWrite_putByteArray() throws Exception {
    byte[] buf = new byte[] {'y', 'a', 'm', 's'};
    EasyMock.expect(hasher.putBytes(buf, 0, buf.length)).andReturn(hasher).once();
    EasyMock.replay(hasher);
    HashingOutputStream out = new HashingOutputStream(hashFunction, buffer);

    out.write(buf);

    EasyMock.verify(hashFunction);
    EasyMock.verify(hasher);
  }

  public void testWrite_putByteArrayAtPos() throws Exception {
    byte[] buf = new byte[] {'y', 'a', 'm', 's'};
    EasyMock.expect(hasher.putBytes(buf, 0, 3)).andReturn(hasher).once();
    EasyMock.replay(hasher);
    HashingOutputStream out = new HashingOutputStream(hashFunction, buffer);

    out.write(buf, 0, 3);

    EasyMock.verify(hashFunction);
    EasyMock.verify(hasher);
  }

  public void testHash_hashesCorrectly() throws Exception {
    byte[] buf = new byte[] {'y', 'a', 'm', 's'};
    HashCode expectedHash = Hashing.md5().hashBytes(buf);
    HashingOutputStream out = new HashingOutputStream(Hashing.md5(), buffer);

    out.write(buf);

    assertEquals(expectedHash, out.hash());
  }

  public void testChecksForNull() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(
        new HashingOutputStream(Hashing.md5(), new ByteArrayOutputStream()));
    tester.testAllPublicStaticMethods(HashingOutputStream.class);
    tester.testAllPublicConstructors(HashingOutputStream.class);
  }
}

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.testing.NullPointerTester;
import java.io.ByteArrayOutputStream;
import junit.framework.TestCase;

/**
 * Tests for {@link HashingOutputStream}.
 *
 * @author Nick Piepmeier
 */
public class HashingOutputStreamTest extends TestCase {
  private Hasher hasher;
  private HashFunction hashFunction;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    hasher = mock(Hasher.class);
    hashFunction = mock(HashFunction.class);

    when(hashFunction.newHasher()).thenReturn(hasher);
  }

  public void testWrite_putSingleByte() throws Exception {
    int b = 'q';
    HashingOutputStream out = new HashingOutputStream(hashFunction, buffer);
    out.write(b);

    verify(hashFunction).newHasher();
    verify(hasher).putByte((byte) b);
    verifyNoMoreInteractions(hashFunction, hasher);
  }

  public void testWrite_putByteArray() throws Exception {
    byte[] buf = new byte[] {'y', 'a', 'm', 's'};
    HashingOutputStream out = new HashingOutputStream(hashFunction, buffer);
    out.write(buf);

    verify(hashFunction).newHasher();
    verify(hasher).putBytes(buf, 0, buf.length);
    verifyNoMoreInteractions(hashFunction, hasher);
  }

  public void testWrite_putByteArrayAtPos() throws Exception {
    byte[] buf = new byte[] {'y', 'a', 'm', 's'};
    HashingOutputStream out = new HashingOutputStream(hashFunction, buffer);
    out.write(buf, 0, 3);

    verify(hashFunction).newHasher();
    verify(hasher).putBytes(buf, 0, 3);
    verifyNoMoreInteractions(hashFunction, hasher);
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

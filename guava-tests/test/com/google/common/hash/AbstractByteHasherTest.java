/*
 * Copyright (C) 2012 The Guava Authors
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

import static com.google.common.base.Charsets.UTF_16LE;
import static org.junit.Assert.assertArrayEquals;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import junit.framework.TestCase;

/**
 * Tests for AbstractByteHasher.
 *
 * @author Colin Decker
 */
public class AbstractByteHasherTest extends TestCase {

  public void testBytes() {
    TestHasher hasher = new TestHasher(); // byte order insignificant here
    byte[] expected = {1, 2, 3, 4, 5, 6, 7, 8};
    hasher.putByte((byte) 1);
    hasher.putBytes(new byte[] {2, 3, 4, 5, 6});
    hasher.putByte((byte) 7);
    hasher.putBytes(new byte[] {});
    hasher.putBytes(new byte[] {8});
    hasher.assertBytes(expected);
  }

  public void testShort() {
    TestHasher hasher = new TestHasher();
    hasher.putShort((short) 0x0201);
    hasher.assertBytes(new byte[] {1, 2});
  }

  public void testInt() {
    TestHasher hasher = new TestHasher();
    hasher.putInt(0x04030201);
    hasher.assertBytes(new byte[] {1, 2, 3, 4});
  }

  public void testLong() {
    TestHasher hasher = new TestHasher();
    hasher.putLong(0x0807060504030201L);
    hasher.assertBytes(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
  }

  public void testChar() {
    TestHasher hasher = new TestHasher();
    hasher.putChar((char) 0x0201);
    hasher.assertBytes(new byte[] {1, 2});
  }

  public void testString() {
    Random random = new Random();
    for (int i = 0; i < 100; i++) {
      byte[] bytes = new byte[64];
      random.nextBytes(bytes);
      String s = new String(bytes, UTF_16LE); // so all random strings are valid
      assertEquals(
          new TestHasher().putUnencodedChars(s).hash(),
          new TestHasher().putBytes(s.getBytes(UTF_16LE)).hash());
      assertEquals(
          new TestHasher().putUnencodedChars(s).hash(),
          new TestHasher().putString(s, UTF_16LE).hash());
    }
  }

  public void testFloat() {
    TestHasher hasher = new TestHasher();
    hasher.putFloat(Float.intBitsToFloat(0x04030201));
    hasher.assertBytes(new byte[] {1, 2, 3, 4});
  }

  public void testDouble() {
    TestHasher hasher = new TestHasher();
    hasher.putDouble(Double.longBitsToDouble(0x0807060504030201L));
    hasher.assertBytes(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
  }

  public void testCorrectExceptions() {
    TestHasher hasher = new TestHasher();
    try {
      hasher.putBytes(new byte[8], -1, 4);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      hasher.putBytes(new byte[8], 0, 16);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      hasher.putBytes(new byte[8], 0, -1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
  }

  @CanIgnoreReturnValue
  private class TestHasher extends AbstractByteHasher {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @Override
    protected void update(byte b) {
      out.write(b);
    }

    @Override
    protected void update(byte[] b, int off, int len) {
      out.write(b, off, len);
    }

    byte[] bytes() {
      return out.toByteArray();
    }

    void assertBytes(byte[] expected) {
      assertArrayEquals(expected, bytes());
    }

    @Override
    public HashCode hash() {
      return HashCode.fromBytesNoCopy(bytes());
    }
  }
}

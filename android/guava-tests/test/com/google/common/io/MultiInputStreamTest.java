/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.io;

import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Test class for {@link MultiInputStream}.
 *
 * @author Chris Nokleberg
 */
public class MultiInputStreamTest extends IoTestCase {

  public void testJoin() throws Exception {
    joinHelper(0);
    joinHelper(1);
    joinHelper(0, 0, 0);
    joinHelper(10, 20);
    joinHelper(10, 0, 20);
    joinHelper(0, 10, 20);
    joinHelper(10, 20, 0);
    joinHelper(10, 20, 1);
    joinHelper(1, 1, 1, 1, 1, 1, 1, 1);
    joinHelper(1, 0, 1, 0, 1, 0, 1, 0);
  }

  public void testOnlyOneOpen() throws Exception {
    final ByteSource source = newByteSource(0, 50);
    final int[] counter = new int[1];
    ByteSource checker =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            if (counter[0]++ != 0) {
              throw new IllegalStateException("More than one source open");
            }
            return new FilterInputStream(source.openStream()) {
              @Override
              public void close() throws IOException {
                super.close();
                counter[0]--;
              }
            };
          }
        };
    byte[] result = ByteSource.concat(checker, checker, checker).read();
    assertEquals(150, result.length);
  }

  private void joinHelper(Integer... spans) throws Exception {
    List<ByteSource> sources = Lists.newArrayList();
    int start = 0;
    for (Integer span : spans) {
      sources.add(newByteSource(start, span));
      start += span;
    }
    ByteSource joined = ByteSource.concat(sources);
    assertTrue(newByteSource(0, start).contentEquals(joined));
  }

  public void testReadSingleByte() throws Exception {
    ByteSource source = newByteSource(0, 10);
    ByteSource joined = ByteSource.concat(source, source);
    assertEquals(20, joined.size());
    InputStream in = joined.openStream();
    assertFalse(in.markSupported());
    assertEquals(10, in.available());
    int total = 0;
    while (in.read() != -1) {
      total++;
    }
    assertEquals(0, in.available());
    assertEquals(20, total);
  }

  @SuppressWarnings("CheckReturnValue") // these calls to skip always return 0
  public void testSkip() throws Exception {
    MultiInputStream multi =
        new MultiInputStream(
            Collections.singleton(
                    new ByteSource() {
                      @Override
                      public InputStream openStream() {
                        return new ByteArrayInputStream(newPreFilledByteArray(0, 50)) {
                          @Override
                          public long skip(long n) {
                            return 0;
                          }
                        };
                      }
                    })
                .iterator());
    assertEquals(0, multi.skip(-1));
    assertEquals(0, multi.skip(-1));
    assertEquals(0, multi.skip(0));
    ByteStreams.skipFully(multi, 20);
    assertEquals(20, multi.read());
  }

  public void testReadSingle_noStackOverflow() throws IOException {
    // https://github.com/google/guava/issues/2996
    // no data, just testing that there's no StackOverflowException
    assertEquals(-1, tenMillionEmptySources().read());
  }

  public void testReadArray_noStackOverflow() throws IOException {
    // https://github.com/google/guava/issues/2996
    // no data, just testing that there's no StackOverflowException
    assertEquals(-1, tenMillionEmptySources().read(new byte[1]));
  }

  private static MultiInputStream tenMillionEmptySources() throws IOException {
    return new MultiInputStream(Collections.nCopies(10_000_000, ByteSource.empty()).iterator());
  }

  private static ByteSource newByteSource(final int start, final int size) {
    return new ByteSource() {
      @Override
      public InputStream openStream() {
        return new ByteArrayInputStream(newPreFilledByteArray(start, size));
      }
    };
  }
}

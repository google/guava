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
    final InputSupplier<InputStream> supplier = newByteSupplier(0, 50);
    final int[] counter = new int[1];
    InputSupplier<InputStream> checker = new InputSupplier<InputStream>() {
      @Override
      public InputStream getInput() throws IOException {
        if (counter[0]++ != 0) {
          throw new IllegalStateException("More than one supplier open");
        }
        return new FilterInputStream(supplier.getInput()) {
          @Override public void close() throws IOException {
            super.close();
            counter[0]--;
          }
        };
      }
    };
    @SuppressWarnings("unchecked")
    byte[] result = ByteStreams.toByteArray(
        ByteStreams.join(checker, checker, checker));
    assertEquals(150, result.length);
  }

  private void joinHelper(Integer... spans) throws Exception {
    List<InputSupplier<InputStream>> suppliers = Lists.newArrayList();
    int start = 0;
    for (Integer span : spans) {
      suppliers.add(newByteSupplier(start, span));
      start += span;
    }
    InputSupplier<InputStream> joined = ByteStreams.join(suppliers);
    assertTrue(ByteStreams.equal(newByteSupplier(0, start), joined));
  }

  public void testReadSingleByte() throws Exception {
    InputSupplier<InputStream> supplier = newByteSupplier(0, 10);
    @SuppressWarnings("unchecked")
    InputSupplier<InputStream> joined = ByteStreams.join(supplier, supplier);
    assertEquals(20, ByteStreams.length(joined));
    InputStream in = joined.getInput();
    assertFalse(in.markSupported());
    assertEquals(10, in.available());
    int total = 0;
    while (in.read() != -1) {
      total++;
    }
    assertEquals(0, in.available());
    assertEquals(20, total);
  }

  public void testSkip() throws Exception {
    MultiInputStream multi = new MultiInputStream(
        Collections.singleton(new InputSupplier<InputStream>() {
          @Override
          public InputStream getInput() {
            return new ByteArrayInputStream(newPreFilledByteArray(0, 50)) {
              @Override public long skip(long n) {
                return 0;
              }
            };
          }
        }).iterator());
    multi.skip(-1);
    multi.skip(-1);
    multi.skip(0);
    ByteStreams.skipFully(multi, 20);
    assertEquals(20, multi.read());
  }

  private static InputSupplier<InputStream> newByteSupplier(final int start, final int size) {
    return new InputSupplier<InputStream>() {
      @Override
      public InputStream getInput() {
        return new ByteArrayInputStream(newPreFilledByteArray(start, size));
      }
    };
  }
}

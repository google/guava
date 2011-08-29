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

import junit.framework.TestCase;

/**
 * Base test case class for I/O tests.
 *
 * @author Chris Nokleberg
 */
public abstract class IoTestCase extends TestCase {

  static final String I18N
      = "\u00CE\u00F1\u0163\u00E9\u0072\u00F1\u00E5\u0163\u00EE\u00F6"
      + "\u00F1\u00E5\u013C\u00EE\u017E\u00E5\u0163\u00EE\u00F6\u00F1";

  static final String ASCII
      = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      + "[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

  /** Returns a byte array of length size that has values 0 .. size - 1. */
  protected static byte[] newPreFilledByteArray(int size) {
    return newPreFilledByteArray(0, size);
  }

  /**
   * Returns a byte array of length size that has values
   *    offset .. offset + size - 1.
   */
  protected static byte[] newPreFilledByteArray(int offset, int size) {
    byte[] array = new byte[size];
    for (int i = 0; i < size; i++) {
      array[i] = (byte) (offset + i);
    }
    return array;
  }
}

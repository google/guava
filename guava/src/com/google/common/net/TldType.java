/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.net;

import com.google.common.annotations.GwtCompatible;

/**
 * Specifies the type of a top-level domain definition.
 */
@GwtCompatible
enum TldType {

  /** private definition of a top-level domain */
  PRIVATE(':', ','),
  /** ICANN definition of a top-level domain */
  ICANN('!', '?');

  /** The character used for an inner node in the trie encoding */
  private final char innerNodeCode;

  /** The character used for a leaf node in the trie encoding */
  private final char leafNodeCode;

  private TldType(char innerNodeCode, char leafNodeCode) {
    this.innerNodeCode = innerNodeCode;
    this.leafNodeCode = leafNodeCode;
  }

  public char getLeafNodeCode() {
    return leafNodeCode;
  }

  public char getInnerNodeCode() {
    return innerNodeCode;
  }

  /** Returns a TldType of the right type according to the given code */
  public static TldType fromCode(char code) {
    for (TldType value : values()) {
      if (value.getInnerNodeCode() == code || value.getLeafNodeCode() == code) {
        return value;
      }
    }
    throw new IllegalArgumentException("No enum corresponding to given code: " + code);
  }

  public static TldType fromIsPrivate(boolean isPrivate) {
    return isPrivate ? PRIVATE : ICANN;
  }
}

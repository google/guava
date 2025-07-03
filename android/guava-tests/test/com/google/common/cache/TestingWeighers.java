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

package com.google.common.cache;

import org.jspecify.annotations.NullUnmarked;

/**
 * Utility {@link Weigher} implementations intended for use in testing.
 *
 * @author Charles Fry
 */
@NullUnmarked
public class TestingWeighers {

  /** Returns a {@link Weigher} that returns the given {@code constant} for every request. */
  static Weigher<Object, Object> constantWeigher(int constant) {
    return new ConstantWeigher(constant);
  }

  /** Returns a {@link Weigher} that uses the integer key as the weight. */
  static Weigher<Integer, Object> intKeyWeigher() {
    return new IntKeyWeigher();
  }

  /** Returns a {@link Weigher} that uses the integer value as the weight. */
  static Weigher<Object, Integer> intValueWeigher() {
    return new IntValueWeigher();
  }

  static final class ConstantWeigher implements Weigher<Object, Object> {
    private final int constant;

    ConstantWeigher(int constant) {
      this.constant = constant;
    }

    @Override
    public int weigh(Object key, Object value) {
      return constant;
    }
  }

  static final class IntKeyWeigher implements Weigher<Integer, Object> {
    @Override
    public int weigh(Integer key, Object value) {
      return key;
    }
  }

  static final class IntValueWeigher implements Weigher<Object, Integer> {
    @Override
    public int weigh(Object key, Integer value) {
      return value;
    }
  }
}

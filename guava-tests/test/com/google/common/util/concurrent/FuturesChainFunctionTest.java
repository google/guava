/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.util.concurrent;

import com.google.common.base.Function;

/**
 * Unit tests for {@link Futures#chain(ListenableFuture, Function)}.
 *
 * @author Nishant Thakkar
 */
public class FuturesChainFunctionTest extends AbstractFuturesChainTest {
  @Override ListenableFuture<String> chain(
      ListenableFuture<Integer> inputFuture) {
    return Futures.chain(inputFuture, new ChainingFunction());
  }

  private class ChainingFunction
      implements Function<Integer, ListenableFuture<String>> {
    @Override
    public ListenableFuture<String> apply(Integer input) {
      return FuturesChainFunctionTest.this.apply(input);
    }
  }
}

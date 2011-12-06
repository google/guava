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

/**
 * Unit tests for {@link Futures#transform(ListenableFuture, AsyncFunction)}.
 *
 * @author Nishant Thakkar
 */
public class FuturesChainAsyncFunctionTest extends AbstractFuturesChainTest {
  @Override ListenableFuture<String> chain(
      ListenableFuture<Integer> inputFuture) {
    return Futures.transform(inputFuture, new AsyncFunctionFunction());
  }

  private class AsyncFunctionFunction
      implements AsyncFunction<Integer, String> {
    @Override
    public ListenableFuture<String> apply(Integer input) {
      return FuturesChainAsyncFunctionTest.this.apply(input);
    }
  }
}

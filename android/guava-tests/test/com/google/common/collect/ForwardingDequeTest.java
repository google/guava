/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.testing.ForwardingWrapperTester;
import java.util.Deque;
import junit.framework.TestCase;

/**
 * Tests for {@code ForwardingDeque}.
 *
 * @author Kurt Alfred Kluever
 */
public class ForwardingDequeTest extends TestCase {

  @SuppressWarnings("rawtypes")
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            Deque.class,
            new Function<Deque, Deque>() {
              @Override
              public Deque apply(Deque delegate) {
                return wrap(delegate);
              }
            });
  }

  private static <T> Deque<T> wrap(final Deque<T> delegate) {
    return new ForwardingDeque<T>() {
      @Override
      protected Deque<T> delegate() {
        return delegate;
      }
    };
  }
}

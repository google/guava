/*
 * Copyright (C) 2010 The Guava Authors
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
import com.google.common.testing.EqualsTester;
import com.google.common.testing.ForwardingWrapperTester;
import junit.framework.TestCase;

/**
 * Unit test for {@link ForwardingListMultimap}.
 *
 * @author Kurt Alfred Kluever
 */
public class ForwardingListMultimapTest extends TestCase {

  @SuppressWarnings("rawtypes")
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            ListMultimap.class,
            new Function<ListMultimap, ListMultimap<?, ?>>() {
              @Override
              public ListMultimap<?, ?> apply(ListMultimap delegate) {
                return wrap((ListMultimap<?, ?>) delegate);
              }
            });
  }

  public void testEquals() {
    ListMultimap<Integer, String> map1 = ImmutableListMultimap.of(1, "one");
    ListMultimap<Integer, String> map2 = ImmutableListMultimap.of(2, "two");
    new EqualsTester()
        .addEqualityGroup(map1, wrap(map1), wrap(map1))
        .addEqualityGroup(map2, wrap(map2))
        .testEquals();
  }

  private static <K, V> ListMultimap<K, V> wrap(final ListMultimap<K, V> delegate) {
    return new ForwardingListMultimap<K, V>() {
      @Override
      protected ListMultimap<K, V> delegate() {
        return delegate;
      }
    };
  }
}

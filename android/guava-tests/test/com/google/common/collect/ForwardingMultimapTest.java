/*
 * Copyright (C) 2009 The Guava Authors
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
 * Unit test for {@link ForwardingMultimap}.
 *
 * @author Hayward Chan
 */
public class ForwardingMultimapTest extends TestCase {

  @SuppressWarnings("rawtypes")
  public void testForwarding() {
    new ForwardingWrapperTester()
        .testForwarding(
            Multimap.class,
            new Function<Multimap, Multimap>() {
              @Override
              public Multimap apply(Multimap delegate) {
                return wrap(delegate);
              }
            });
  }

  public void testEquals() {
    Multimap<Integer, String> map1 = ImmutableMultimap.of(1, "one");
    Multimap<Integer, String> map2 = ImmutableMultimap.of(2, "two");
    new EqualsTester()
        .addEqualityGroup(map1, wrap(map1), wrap(map1))
        .addEqualityGroup(map2, wrap(map2))
        .testEquals();
  }

  private static <K, V> Multimap<K, V> wrap(final Multimap<K, V> delegate) {
    return new ForwardingMultimap<K, V>() {
      @Override
      protected Multimap<K, V> delegate() {
        return delegate;
      }
    };
  }
}

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

package com.google.common.collect;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit test for {@link ConcurrentHashMultiset} behavior when backed by
 * {@link ConcurrentSkipListMap}. User code cannot create such a multiset, since
 * the constructor we use here is package-private, but
 * {@link ConcurrentHashMultiset} should work regardless of the type of its
 * backing map, and it's possible that this test could catch a bug that the
 * standard test doesn't.
 * 
 * @author Chris Povirk
 * @author Jared Levy
 */
public class ConcurrentHashMultisetWithCslmTest
    extends AbstractConcurrentHashMultisetTest {
  @Override protected <E> Multiset<E> create() {
    return new ConcurrentHashMultiset<E>(
        new ConcurrentSkipListMap<E, AtomicInteger>());
  }
}

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

/**
 * Unit test for {@link ForwardingSortedSetMultimap}.
 *
 * @author Kurt Alfred Kluever
 */
public class ForwardingSortedSetMultimapTest extends ForwardingSetMultimapTest {

  @Override public void setUp() throws Exception {
    super.setUp();
    /*
     * Class parameters must be raw, so we can't create a proxy with generic
     * type arguments. The created proxy only records calls and returns null, so
     * the type is irrelevant at runtime.
     */
    @SuppressWarnings("unchecked")
    final SortedSetMultimap<String, Boolean> multimap =
        createProxyInstance(SortedSetMultimap.class);
    forward = new ForwardingSortedSetMultimap<String, Boolean>() {
      @Override protected SortedSetMultimap<String, Boolean> delegate() {
        return multimap;
      }
    };
  }
}

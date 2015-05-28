/*
 * Copyright (C) 2015 The Guava Authors
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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtCompatible;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation helper for {@link TestConcurrentMapGenerator} for use with maps of strings.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public abstract class TestStringConcurrentMapGenerator 
    extends TestStringMapGenerator implements TestConcurrentMapGenerator<String, String> {
  @Override
  public ConcurrentMap<String, String> create(Object... entries) {
    return (ConcurrentMap<String, String>) super.create(entries);
  }

  @Override
  protected abstract ConcurrentMap<String, String> create(Entry<String, String>[] entries);

}
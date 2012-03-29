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

package com.google.common.collect.testing;

/**
 * A generator that relies on a preexisting generator for most of its work. For example, a derived
 * iterator generator may delegate the work of creating the underlying collection to an inner
 * collection generator.
 *
 * @author Chris Povirk
 */
public interface DerivedGenerator {
  TestSubjectGenerator<?> getInnerGenerator();
}

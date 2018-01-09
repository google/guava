/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect.testing.testers;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import java.util.Set;
import org.junit.Ignore;

/** @author George van den Driessche */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class AbstractSetTester<E> extends AbstractCollectionTester<E> {
  /*
   * Previously we had a field named set that was initialized to the value of
   * collection in setUp(), but that caused problems when a tester changed the
   * value of set or collection but not both.
   */
  protected final Set<E> getSet() {
    return (Set<E>) collection;
  }
}

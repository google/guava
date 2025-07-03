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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtCompatible;
import java.util.Iterator;

/**
 * Adapts a test iterable generator to give a TestIteratorGenerator.
 *
 * @author George van den Driessche
 */
@GwtCompatible
public final class DerivedTestIteratorGenerator<E>
    implements TestIteratorGenerator<E>, DerivedGenerator {
  private final TestSubjectGenerator<? extends Iterable<E>> collectionGenerator;

  public DerivedTestIteratorGenerator(
      TestSubjectGenerator<? extends Iterable<E>> collectionGenerator) {
    this.collectionGenerator = collectionGenerator;
  }

  @Override
  public TestSubjectGenerator<? extends Iterable<E>> getInnerGenerator() {
    return collectionGenerator;
  }

  @Override
  public Iterator<E> get() {
    return collectionGenerator.createTestSubject().iterator();
  }
}

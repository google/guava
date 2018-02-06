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

package com.google.common.collect.testing;

import com.google.common.annotations.GwtCompatible;
import java.util.Collection;
import org.junit.Ignore;

/**
 * Base class for collection testers.
 *
 * @param <E> the element type of the collection to be tested.
 * @author Kevin Bourrillion
 */
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public abstract class AbstractCollectionTester<E>
    extends AbstractContainerTester<Collection<E>, E> {

  // TODO: replace this with an accessor.
  protected Collection<E> collection;

  @Override
  protected Collection<E> actualContents() {
    return collection;
  }

  // TODO: dispose of this once collection is encapsulated.
  @Override
  protected Collection<E> resetContainer(Collection<E> newContents) {
    collection = super.resetContainer(newContents);
    return collection;
  }

  /** @see AbstractContainerTester#resetContainer() */
  protected void resetCollection() {
    resetContainer();
  }

  /** @return an array of the proper size with {@code null} inserted into the middle element. */
  protected E[] createArrayWithNullElement() {
    E[] array = createSamplesArray();
    array[getNullLocation()] = null;
    return array;
  }

  protected void initCollectionWithNullElement() {
    E[] array = createArrayWithNullElement();
    resetContainer(getSubjectGenerator().create(array));
  }

  /**
   * Equivalent to {@link #expectMissing(Object[]) expectMissing}{@code (null)} except that the call
   * to {@code contains(null)} is permitted to throw a {@code NullPointerException}.
   *
   * @param message message to use upon assertion failure
   */
  protected void expectNullMissingWhenNullUnsupported(String message) {
    try {
      assertFalse(message, actualContents().contains(null));
    } catch (NullPointerException tolerated) {
      // Tolerated
    }
  }
}

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
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * A utility similar to {@link IteratorTester} for testing a {@link ListIterator} against a known
 * good reference implementation. As with {@code IteratorTester}, a concrete subclass must provide
 * target iterators on demand. It also requires three additional constructor parameters: {@code
 * elementsToInsert}, the elements to be passed to {@code set()} and {@code add()} calls; {@code
 * features}, the features supported by the iterator; and {@code expectedElements}, the elements the
 * iterator should return in order.
 *
 * <p>The items in {@code elementsToInsert} will be repeated if {@code steps} is larger than the
 * number of provided elements.
 *
 * @author Chris Povirk
 */
@GwtCompatible
public abstract class ListIteratorTester<E> extends AbstractIteratorTester<E, ListIterator<E>> {
  protected ListIteratorTester(
      int steps,
      Iterable<E> elementsToInsert,
      Iterable<? extends IteratorFeature> features,
      Iterable<E> expectedElements,
      int startIndex) {
    super(steps, elementsToInsert, features, expectedElements, KnownOrder.KNOWN_ORDER, startIndex);
  }

  @Override
  protected final Iterable<? extends Stimulus<E, ? super ListIterator<E>>> getStimulusValues() {
    List<Stimulus<E, ? super ListIterator<E>>> list = new ArrayList<>();
    Helpers.addAll(list, iteratorStimuli());
    Helpers.addAll(list, listIteratorStimuli());
    return list;
  }

  @Override
  protected abstract ListIterator<E> newTargetIterator();
}

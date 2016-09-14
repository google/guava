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
import com.google.common.collect.testing.features.CollectionSize;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Generator for collection of a particular size.
 *
 * @author George van den Driessche
 */
@GwtCompatible
public final class OneSizeGenerator<T, E> implements OneSizeTestContainerGenerator<T, E> {
  private final TestContainerGenerator<T, E> generator;
  private final CollectionSize collectionSize;

  public OneSizeGenerator(TestContainerGenerator<T, E> generator, CollectionSize collectionSize) {
    this.generator = generator;
    this.collectionSize = collectionSize;
  }

  @Override
  public TestContainerGenerator<T, E> getInnerGenerator() {
    return generator;
  }

  @Override
  public SampleElements<E> samples() {
    return generator.samples();
  }

  @Override
  public T create(Object... elements) {
    return generator.create(elements);
  }

  @Override
  public E[] createArray(int length) {
    return generator.createArray(length);
  }

  @Override
  public T createTestSubject() {
    Collection<E> elements = getSampleElements(getCollectionSize().getNumElements());
    return generator.create(elements.toArray());
  }

  @Override
  public Collection<E> getSampleElements(int howMany) {
    SampleElements<E> samples = samples();
    @SuppressWarnings("unchecked")
    List<E> allSampleElements =
        Arrays.asList(samples.e0(), samples.e1(), samples.e2(), samples.e3(), samples.e4());
    return new ArrayList<E>(allSampleElements.subList(0, howMany));
  }

  @Override
  public CollectionSize getCollectionSize() {
    return collectionSize;
  }

  @Override
  public Iterable<E> order(List<E> insertionOrder) {
    return generator.order(insertionOrder);
  }
}

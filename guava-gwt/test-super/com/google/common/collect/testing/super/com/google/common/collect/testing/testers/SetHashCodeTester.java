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

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import java.util.Collection;

/**
 * Tests {@link java.util.Set#hashCode}.
 *
 * @author George van den Driessche
 */
@GwtCompatible(emulated = true)
public class SetHashCodeTester<E> extends AbstractSetTester<E> {
  public void testHashCode() {
    int expectedHashCode = 0;
    for (E element : getSampleElements()) {
      expectedHashCode += ((element == null) ? 0 : element.hashCode());
    }
    assertEquals(
        "A Set's hashCode() should be the sum of those of its elements.",
        expectedHashCode, getSet().hashCode());
  }

  @CollectionSize.Require(absent = CollectionSize.ZERO)
  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  public void testHashCode_containingNull() {
    Collection<E> elements = getSampleElements(getNumElements() - 1);
    int expectedHashCode = 0;
    for (E element : elements) {
      expectedHashCode += ((element == null) ? 0 : element.hashCode());
    }

    elements.add(null);
    collection = getSubjectGenerator().create(elements.toArray());
    assertEquals(
        "A Set's hashCode() should be the sum of those of its elements (with "
            + "a null element counting as having a hash of zero).",
        expectedHashCode, getSet().hashCode());
  }
}


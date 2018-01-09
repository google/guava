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
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.ONE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Ignore;

/**
 * A generic JUnit test which tests {@code retainAll} operations on a collection. Can't be invoked
 * directly; please see {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Chris Povirk
 */
@SuppressWarnings("unchecked") // too many "unchecked generic array creations"
@GwtCompatible
@Ignore // Affects only Android test runner, which respects JUnit 4 annotations on JUnit 3 tests.
public class CollectionRetainAllTester<E> extends AbstractCollectionTester<E> {

  /** A collection of elements to retain, along with a description for use in failure messages. */
  private class Target {
    private final Collection<E> toRetain;
    private final String description;

    private Target(Collection<E> toRetain, String description) {
      this.toRetain = toRetain;
      this.description = description;
    }

    @Override
    public String toString() {
      return description;
    }
  }

  private Target empty;
  private Target disjoint;
  private Target superset;
  private Target nonEmptyProperSubset;
  private Target sameElements;
  private Target partialOverlap;
  private Target containsDuplicates;
  private Target nullSingleton;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    empty = new Target(emptyCollection(), "empty");
    /*
     * We test that nullSingleton.retainAll(disjointList) does NOT throw a
     * NullPointerException when disjointList does not, so we can't use
     * MinimalCollection, which throws NullPointerException on calls to
     * contains(null).
     */
    List<E> disjointList = Arrays.asList(e3(), e4());
    disjoint = new Target(disjointList, "disjoint");
    superset = new Target(MinimalCollection.of(e0(), e1(), e2(), e3(), e4()), "superset");
    nonEmptyProperSubset = new Target(MinimalCollection.of(e1()), "subset");
    sameElements = new Target(Arrays.asList(createSamplesArray()), "sameElements");
    containsDuplicates =
        new Target(MinimalCollection.of(e0(), e0(), e3(), e3()), "containsDuplicates");
    partialOverlap = new Target(MinimalCollection.of(e2(), e3()), "partialOverlap");
    nullSingleton = new Target(Collections.<E>singleton(null), "nullSingleton");
  }

  // retainAll(empty)

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testRetainAll_emptyPreviouslyEmpty() {
    expectReturnsFalse(empty);
    expectUnchanged();
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testRetainAll_emptyPreviouslyEmptyUnsupported() {
    expectReturnsFalseOrThrows(empty);
    expectUnchanged();
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRetainAll_emptyPreviouslyNonEmpty() {
    expectReturnsTrue(empty);
    expectContents();
    expectMissing(e0(), e1(), e2());
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRetainAll_emptyPreviouslyNonEmptyUnsupported() {
    expectThrows(empty);
    expectUnchanged();
  }

  // retainAll(disjoint)

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testRetainAll_disjointPreviouslyEmpty() {
    expectReturnsFalse(disjoint);
    expectUnchanged();
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testRetainAll_disjointPreviouslyEmptyUnsupported() {
    expectReturnsFalseOrThrows(disjoint);
    expectUnchanged();
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRetainAll_disjointPreviouslyNonEmpty() {
    expectReturnsTrue(disjoint);
    expectContents();
    expectMissing(e0(), e1(), e2());
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRetainAll_disjointPreviouslyNonEmptyUnsupported() {
    expectThrows(disjoint);
    expectUnchanged();
  }

  // retainAll(superset)

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRetainAll_superset() {
    expectReturnsFalse(superset);
    expectUnchanged();
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  public void testRetainAll_supersetUnsupported() {
    expectReturnsFalseOrThrows(superset);
    expectUnchanged();
  }

  // retainAll(subset)

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testRetainAll_subset() {
    expectReturnsTrue(nonEmptyProperSubset);
    expectContents(nonEmptyProperSubset.toRetain);
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testRetainAll_subsetUnsupported() {
    expectThrows(nonEmptyProperSubset);
    expectUnchanged();
  }

  // retainAll(sameElements)

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testRetainAll_sameElements() {
    expectReturnsFalse(sameElements);
    expectUnchanged();
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  public void testRetainAll_sameElementsUnsupported() {
    expectReturnsFalseOrThrows(sameElements);
    expectUnchanged();
  }

  // retainAll(partialOverlap)

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testRetainAll_partialOverlap() {
    expectReturnsTrue(partialOverlap);
    expectContents(e2());
  }

  @CollectionFeature.Require(absent = SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testRetainAll_partialOverlapUnsupported() {
    expectThrows(partialOverlap);
    expectUnchanged();
  }

  // retainAll(containsDuplicates)

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ONE)
  public void testRetainAll_containsDuplicatesSizeOne() {
    expectReturnsFalse(containsDuplicates);
    expectContents(e0());
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testRetainAll_containsDuplicatesSizeSeveral() {
    expectReturnsTrue(containsDuplicates);
    expectContents(e0());
  }

  // retainAll(nullSingleton)

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testRetainAll_nullSingletonPreviouslyEmpty() {
    expectReturnsFalse(nullSingleton);
    expectUnchanged();
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRetainAll_nullSingletonPreviouslyNonEmpty() {
    expectReturnsTrue(nullSingleton);
    expectContents();
  }

  @CollectionFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(ONE)
  public void testRetainAll_nullSingletonPreviouslySingletonWithNull() {
    initCollectionWithNullElement();
    expectReturnsFalse(nullSingleton);
    expectContents(createArrayWithNullElement());
  }

  @CollectionFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = {ZERO, ONE})
  public void testRetainAll_nullSingletonPreviouslySeveralWithNull() {
    initCollectionWithNullElement();
    expectReturnsTrue(nullSingleton);
    expectContents(nullSingleton.toRetain);
  }

  // nullSingleton.retainAll()

  @CollectionFeature.Require({SUPPORTS_REMOVE, ALLOWS_NULL_VALUES})
  @CollectionSize.Require(absent = ZERO)
  public void testRetainAll_containsNonNullWithNull() {
    initCollectionWithNullElement();
    expectReturnsTrue(disjoint);
    expectContents();
  }

  // retainAll(null)

  /*
   * AbstractCollection fails the retainAll(null) test when the subject
   * collection is empty, but we'd still like to test retainAll(null) when we
   * can. We split the test into empty and non-empty cases. This allows us to
   * suppress only the former.
   */

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(ZERO)
  public void testRetainAll_nullCollectionReferenceEmptySubject() {
    try {
      collection.retainAll(null);
      // Returning successfully is not ideal, but tolerated.
    } catch (NullPointerException tolerated) {
    }
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  @CollectionSize.Require(absent = ZERO)
  public void testRetainAll_nullCollectionReferenceNonEmptySubject() {
    try {
      collection.retainAll(null);
      fail("retainAll(null) should throw NullPointerException");
    } catch (NullPointerException expected) {
    }
  }

  private void expectReturnsTrue(Target target) {
    String message = Platform.format("retainAll(%s) should return true", target);
    assertTrue(message, collection.retainAll(target.toRetain));
  }

  private void expectReturnsFalse(Target target) {
    String message = Platform.format("retainAll(%s) should return false", target);
    assertFalse(message, collection.retainAll(target.toRetain));
  }

  private void expectThrows(Target target) {
    try {
      collection.retainAll(target.toRetain);
      String message = Platform.format("retainAll(%s) should throw", target);
      fail(message);
    } catch (UnsupportedOperationException expected) {
    }
  }

  private void expectReturnsFalseOrThrows(Target target) {
    String message = Platform.format("retainAll(%s) should return false or throw", target);
    try {
      assertFalse(message, collection.retainAll(target.toRetain));
    } catch (UnsupportedOperationException tolerated) {
    }
  }
}

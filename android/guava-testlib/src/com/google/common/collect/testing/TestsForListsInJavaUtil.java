/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.collect.testing.testers.ListListIteratorTester.getListIteratorFullyModifiableMethod;
import static com.google.common.collect.testing.testers.ListSubListTester.getSubListOriginalListSetAffectsSubListLargeListMethod;
import static com.google.common.collect.testing.testers.ListSubListTester.getSubListOriginalListSetAffectsSubListMethod;
import static com.google.common.collect.testing.testers.ListSubListTester.getSubListSubListRemoveAffectsOriginalLargeListMethod;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Generates a test suite covering the {@link List} implementations in the {@link java.util}
 * package. Can be subclassed to specify tests that should be suppressed.
 *
 * @author Kevin Bourrillion
 */
@GwtIncompatible
public class TestsForListsInJavaUtil {
  public static Test suite() {
    return new TestsForListsInJavaUtil().allTests();
  }

  public Test allTests() {
    TestSuite suite = new TestSuite("java.util Lists");
    suite.addTest(testsForEmptyList());
    suite.addTest(testsForSingletonList());
    suite.addTest(testsForArraysAsList());
    suite.addTest(testsForArrayList());
    suite.addTest(testsForLinkedList());
    suite.addTest(testsForCopyOnWriteArrayList());
    suite.addTest(testsForUnmodifiableList());
    suite.addTest(testsForCheckedList());
    suite.addTest(testsForAbstractList());
    suite.addTest(testsForAbstractSequentialList());
    suite.addTest(testsForVector());
    return suite;
  }

  protected Collection<Method> suppressForEmptyList() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForSingletonList() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForArraysAsList() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForArrayList() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForLinkedList() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForCopyOnWriteArrayList() {
    return Arrays.asList(
        getSubListOriginalListSetAffectsSubListMethod(),
        getSubListOriginalListSetAffectsSubListLargeListMethod(),
        getSubListSubListRemoveAffectsOriginalLargeListMethod(),
        getListIteratorFullyModifiableMethod());
  }

  protected Collection<Method> suppressForUnmodifiableList() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForCheckedList() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForAbstractList() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForAbstractSequentialList() {
    return Collections.emptySet();
  }

  protected Collection<Method> suppressForVector() {
    return Collections.emptySet();
  }

  public Test testsForEmptyList() {
    return ListTestSuiteBuilder.using(
            new TestStringListGenerator() {
              @Override
              public List<String> create(String[] elements) {
                return Collections.emptyList();
              }
            })
        .named("emptyList")
        .withFeatures(CollectionFeature.SERIALIZABLE, CollectionSize.ZERO)
        .suppressing(suppressForEmptyList())
        .createTestSuite();
  }

  public Test testsForSingletonList() {
    return ListTestSuiteBuilder.using(
            new TestStringListGenerator() {
              @Override
              public List<String> create(String[] elements) {
                return Collections.singletonList(elements[0]);
              }
            })
        .named("singletonList")
        .withFeatures(
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionSize.ONE)
        .suppressing(suppressForSingletonList())
        .createTestSuite();
  }

  public Test testsForArraysAsList() {
    return ListTestSuiteBuilder.using(
            new TestStringListGenerator() {
              @Override
              public List<String> create(String[] elements) {
                return Arrays.asList(elements.clone());
              }
            })
        .named("Arrays.asList")
        .withFeatures(
            ListFeature.SUPPORTS_SET,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionSize.ANY)
        .suppressing(suppressForArraysAsList())
        .createTestSuite();
  }

  public Test testsForArrayList() {
    return ListTestSuiteBuilder.using(
            new TestStringListGenerator() {
              @Override
              public List<String> create(String[] elements) {
                return new ArrayList<>(MinimalCollection.of(elements));
              }
            })
        .named("ArrayList")
        .withFeatures(
            ListFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionSize.ANY)
        .suppressing(suppressForArrayList())
        .createTestSuite();
  }

  public Test testsForLinkedList() {
    return ListTestSuiteBuilder.using(
            new TestStringListGenerator() {
              @Override
              public List<String> create(String[] elements) {
                return new LinkedList<>(MinimalCollection.of(elements));
              }
            })
        .named("LinkedList")
        .withFeatures(
            ListFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionSize.ANY)
        .suppressing(suppressForLinkedList())
        .createTestSuite();
  }

  public Test testsForCopyOnWriteArrayList() {
    return ListTestSuiteBuilder.using(
            new TestStringListGenerator() {
              @Override
              public List<String> create(String[] elements) {
                return new CopyOnWriteArrayList<>(MinimalCollection.of(elements));
              }
            })
        .named("CopyOnWriteArrayList")
        .withFeatures(
            ListFeature.SUPPORTS_ADD_WITH_INDEX,
            ListFeature.SUPPORTS_REMOVE_WITH_INDEX,
            ListFeature.SUPPORTS_SET,
            CollectionFeature.SUPPORTS_ADD,
            CollectionFeature.SUPPORTS_REMOVE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionSize.ANY)
        .suppressing(suppressForCopyOnWriteArrayList())
        .createTestSuite();
  }

  public Test testsForUnmodifiableList() {
    return ListTestSuiteBuilder.using(
            new TestStringListGenerator() {
              @Override
              public List<String> create(String[] elements) {
                List<String> innerList = new ArrayList<>();
                Collections.addAll(innerList, elements);
                return Collections.unmodifiableList(innerList);
              }
            })
        .named("unmodifiableList/ArrayList")
        .withFeatures(
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionSize.ANY)
        .suppressing(suppressForUnmodifiableList())
        .createTestSuite();
  }

  public Test testsForCheckedList() {
    return ListTestSuiteBuilder.using(
            new TestStringListGenerator() {
              @Override
              public List<String> create(String[] elements) {
                List<String> innerList = new ArrayList<>();
                Collections.addAll(innerList, elements);
                return Collections.checkedList(innerList, String.class);
              }
            })
        .named("checkedList/ArrayList")
        .withFeatures(
            ListFeature.GENERAL_PURPOSE,
            CollectionFeature.SERIALIZABLE,
            CollectionFeature.RESTRICTS_ELEMENTS,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionSize.ANY)
        .suppressing(suppressForCheckedList())
        .createTestSuite();
  }

  public Test testsForAbstractList() {
    return ListTestSuiteBuilder.using(
            new TestStringListGenerator() {
              @Override
              protected List<String> create(final String[] elements) {
                return new AbstractList<String>() {
                  @Override
                  public int size() {
                    return elements.length;
                  }

                  @Override
                  public String get(int index) {
                    return elements[index];
                  }
                };
              }
            })
        .named("AbstractList")
        .withFeatures(
            CollectionFeature.NONE, CollectionFeature.ALLOWS_NULL_VALUES, CollectionSize.ANY)
        .suppressing(suppressForAbstractList())
        .createTestSuite();
  }

  public Test testsForAbstractSequentialList() {
    return ListTestSuiteBuilder.using(
            new TestStringListGenerator() {
              @Override
              protected List<String> create(final String[] elements) {
                // For this test we trust ArrayList works
                final List<String> list = new ArrayList<>();
                Collections.addAll(list, elements);
                return new AbstractSequentialList<String>() {
                  @Override
                  public int size() {
                    return list.size();
                  }

                  @Override
                  public ListIterator<String> listIterator(int index) {
                    return list.listIterator(index);
                  }
                };
              }
            })
        .named("AbstractSequentialList")
        .withFeatures(
            ListFeature.GENERAL_PURPOSE, CollectionFeature.ALLOWS_NULL_VALUES, CollectionSize.ANY)
        .suppressing(suppressForAbstractSequentialList())
        .createTestSuite();
  }

  private Test testsForVector() {
    return ListTestSuiteBuilder.using(
            new TestStringListGenerator() {
              @Override
              protected List<String> create(String[] elements) {
                return new Vector<>(MinimalCollection.of(elements));
              }
            })
        .named("Vector")
        .withFeatures(
            ListFeature.GENERAL_PURPOSE,
            CollectionFeature.ALLOWS_NULL_VALUES,
            CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
            CollectionFeature.SERIALIZABLE,
            CollectionSize.ANY)
        .createTestSuite();
  }
}

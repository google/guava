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
package com.google.common.collect;
public class ImmutableListTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.collect.testModule";
}
public void testAsList__BasicTests() throws Exception {
  com.google.common.collect.ImmutableListTest.BasicTests testCase = new com.google.common.collect.ImmutableListTest.BasicTests();
  testCase.testAsList();
}

public void testBuilderAdd__BasicTests() throws Exception {
  com.google.common.collect.ImmutableListTest.BasicTests testCase = new com.google.common.collect.ImmutableListTest.BasicTests();
  testCase.testBuilderAdd();
}

public void testBuilderAddAllHandlesNullsCorrectly__BasicTests() throws Exception {
  com.google.common.collect.ImmutableListTest.BasicTests testCase = new com.google.common.collect.ImmutableListTest.BasicTests();
  testCase.testBuilderAddAllHandlesNullsCorrectly();
}

public void testBuilderAddAll_iterable__BasicTests() throws Exception {
  com.google.common.collect.ImmutableListTest.BasicTests testCase = new com.google.common.collect.ImmutableListTest.BasicTests();
  testCase.testBuilderAddAll_iterable();
}

public void testBuilderAddAll_iterator__BasicTests() throws Exception {
  com.google.common.collect.ImmutableListTest.BasicTests testCase = new com.google.common.collect.ImmutableListTest.BasicTests();
  testCase.testBuilderAddAll_iterator();
}

public void testBuilderAddHandlesNullsCorrectly__BasicTests() throws Exception {
  com.google.common.collect.ImmutableListTest.BasicTests testCase = new com.google.common.collect.ImmutableListTest.BasicTests();
  testCase.testBuilderAddHandlesNullsCorrectly();
}

public void testBuilderAdd_varargs__BasicTests() throws Exception {
  com.google.common.collect.ImmutableListTest.BasicTests testCase = new com.google.common.collect.ImmutableListTest.BasicTests();
  testCase.testBuilderAdd_varargs();
}

public void testComplexBuilder__BasicTests() throws Exception {
  com.google.common.collect.ImmutableListTest.BasicTests testCase = new com.google.common.collect.ImmutableListTest.BasicTests();
  testCase.testComplexBuilder();
}

public void testEquals_immutableList__BasicTests() throws Exception {
  com.google.common.collect.ImmutableListTest.BasicTests testCase = new com.google.common.collect.ImmutableListTest.BasicTests();
  testCase.testEquals_immutableList();
}

public void testBuilderAddArrayHandlesNulls__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testBuilderAddArrayHandlesNulls();
}

public void testBuilderAddCollectionHandlesNulls__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testBuilderAddCollectionHandlesNulls();
}

public void testCopyOf_arrayContainingOnlyNull__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_arrayContainingOnlyNull();
}

public void testCopyOf_arrayOfOneElement__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_arrayOfOneElement();
}

public void testCopyOf_collectionContainingNull__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_collectionContainingNull();
}

public void testCopyOf_collection_empty__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_collection_empty();
}

public void testCopyOf_collection_general__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_collection_general();
}

public void testCopyOf_collection_oneElement__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_collection_oneElement();
}

public void testCopyOf_concurrentlyMutating__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_concurrentlyMutating();
}

public void testCopyOf_emptyArray__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_emptyArray();
}

public void testCopyOf_iteratorContainingNull__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_iteratorContainingNull();
}

public void testCopyOf_iteratorNull__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_iteratorNull();
}

public void testCopyOf_iterator_empty__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_iterator_empty();
}

public void testCopyOf_iterator_general__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_iterator_general();
}

public void testCopyOf_iterator_oneElement__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_iterator_oneElement();
}

public void testCopyOf_nullArray__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_nullArray();
}

public void testCopyOf_plainIterable__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_plainIterable();
}

public void testCopyOf_plainIterable_iteratesOnce__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_plainIterable_iteratesOnce();
}

public void testCopyOf_shortcut_empty__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_shortcut_empty();
}

public void testCopyOf_shortcut_immutableList__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_shortcut_immutableList();
}

public void testCopyOf_shortcut_singleton__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCopyOf_shortcut_singleton();
}

public void testCreation_arrayOfArray__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_arrayOfArray();
}

public void testCreation_eightElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_eightElements();
}

public void testCreation_elevenElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_elevenElements();
}

public void testCreation_fiveElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_fiveElements();
}

public void testCreation_fourElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_fourElements();
}

public void testCreation_fourteenElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_fourteenElements();
}

public void testCreation_generic__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_generic();
}

public void testCreation_nineElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_nineElements();
}

public void testCreation_noArgs__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_noArgs();
}

public void testCreation_oneElement__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_oneElement();
}

public void testCreation_sevenElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_sevenElements();
}

public void testCreation_singletonNull__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_singletonNull();
}

public void testCreation_sixElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_sixElements();
}

public void testCreation_tenElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_tenElements();
}

public void testCreation_thirteenElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_thirteenElements();
}

public void testCreation_threeElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_threeElements();
}

public void testCreation_twelveElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_twelveElements();
}

public void testCreation_twoElements__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_twoElements();
}

public void testCreation_withNull__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testCreation_withNull();
}

public void testSortedCopyOf__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testSortedCopyOf();
}

public void testSortedCopyOf_containsNull__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testSortedCopyOf_containsNull();
}

public void testSortedCopyOf_empty__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testSortedCopyOf_empty();
}

public void testSortedCopyOf_natural__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testSortedCopyOf_natural();
}

public void testSortedCopyOf_natural_containsNull__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testSortedCopyOf_natural_containsNull();
}

public void testSortedCopyOf_natural_empty__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testSortedCopyOf_natural_empty();
}

public void testSortedCopyOf_natural_singleton__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testSortedCopyOf_natural_singleton();
}

public void testSortedCopyOf_singleton__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testSortedCopyOf_singleton();
}

public void testToImmutableList__CreationTests() throws Exception {
  com.google.common.collect.ImmutableListTest.CreationTests testCase = new com.google.common.collect.ImmutableListTest.CreationTests();
  testCase.testToImmutableList();
}
}

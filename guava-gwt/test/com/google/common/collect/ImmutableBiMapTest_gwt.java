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
public class ImmutableBiMapTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.collect.testModule";
}
public void testNoop() throws Exception {
  com.google.common.collect.ImmutableBiMapTest testCase = new com.google.common.collect.ImmutableBiMapTest();
  testCase.testNoop();
}

public void testDoubleInverse__BiMapSpecificTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.BiMapSpecificTests testCase = new com.google.common.collect.ImmutableBiMapTest.BiMapSpecificTests();
  testCase.testDoubleInverse();
}

public void testForcePut__BiMapSpecificTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.BiMapSpecificTests testCase = new com.google.common.collect.ImmutableBiMapTest.BiMapSpecificTests();
  testCase.testForcePut();
}

public void testKeySet__BiMapSpecificTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.BiMapSpecificTests testCase = new com.google.common.collect.ImmutableBiMapTest.BiMapSpecificTests();
  testCase.testKeySet();
}

public void testValues__BiMapSpecificTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.BiMapSpecificTests testCase = new com.google.common.collect.ImmutableBiMapTest.BiMapSpecificTests();
  testCase.testValues();
}

public void testBuilder__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testBuilder();
}

public void testBuilderPutAll__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testBuilderPutAll();
}

public void testBuilderPutAllWithEmptyMap__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testBuilderPutAllWithEmptyMap();
}

public void testBuilderPutNullKey__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testBuilderPutNullKey();
}

public void testBuilderPutNullKeyViaPutAll__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testBuilderPutNullKeyViaPutAll();
}

public void testBuilderPutNullValue__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testBuilderPutNullValue();
}

public void testBuilderPutNullValueViaPutAll__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testBuilderPutNullValueViaPutAll();
}

public void testBuilderReuse__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testBuilderReuse();
}

public void testBuilder_orderEntriesByValue__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testBuilder_orderEntriesByValue();
}

public void testBuilder_orderEntriesByValueAfterExactSizeBuild__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testBuilder_orderEntriesByValueAfterExactSizeBuild();
}

public void testBuilder_orderEntriesByValue_usedTwiceFails__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testBuilder_orderEntriesByValue_usedTwiceFails();
}

public void testBuilder_withImmutableEntry__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testBuilder_withImmutableEntry();
}

public void testCopyOf__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testCopyOf();
}

public void testCopyOfEmptyMap__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testCopyOfEmptyMap();
}

public void testCopyOfSingletonMap__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testCopyOfSingletonMap();
}

public void testDuplicateValues__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testDuplicateValues();
}

public void testEmpty__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testEmpty();
}

public void testEmptyBuilder__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testEmptyBuilder();
}

public void testFromHashMap__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testFromHashMap();
}

public void testFromImmutableMap__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testFromImmutableMap();
}

public void testOf__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testOf();
}

public void testOfNullKey__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testOfNullKey();
}

public void testOfNullValue__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testOfNullValue();
}

public void testOfWithDuplicateKey__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testOfWithDuplicateKey();
}

public void testPuttingTheSameKeyTwiceThrowsOnBuild__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testPuttingTheSameKeyTwiceThrowsOnBuild();
}

public void testSingletonBuilder__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testSingletonBuilder();
}

public void testToImmutableBiMap__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testToImmutableBiMap();
}

public void testToImmutableBiMap_exceptionOnDuplicateKey__CreationTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.CreationTests testCase = new com.google.common.collect.ImmutableBiMapTest.CreationTests();
  testCase.testToImmutableBiMap_exceptionOnDuplicateKey();
}

public void testClear__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testClear();
}

public void testContainsKey__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testContainsKey();
}

public void testContainsValue__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testContainsValue();
}

public void testEntrySet__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySet();
}

public void testEntrySetAddAndAddAll__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetAddAndAddAll();
}

public void testEntrySetClear__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetClear();
}

public void testEntrySetContainsEntryIncompatibleKey__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetContainsEntryIncompatibleKey();
}

public void testEntrySetContainsEntryNullKeyMissing__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetContainsEntryNullKeyMissing();
}

public void testEntrySetContainsEntryNullKeyPresent__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetContainsEntryNullKeyPresent();
}

public void testEntrySetForEmptyMap__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetForEmptyMap();
}

public void testEntrySetIteratorRemove__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetIteratorRemove();
}

public void testEntrySetRemove__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetRemove();
}

public void testEntrySetRemoveAll__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetRemoveAll();
}

public void testEntrySetRemoveAllNullFromEmpty__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetRemoveAllNullFromEmpty();
}

public void testEntrySetRemoveDifferentValue__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetRemoveDifferentValue();
}

public void testEntrySetRemoveMissingKey__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetRemoveMissingKey();
}

public void testEntrySetRemoveNullKeyMissing__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetRemoveNullKeyMissing();
}

public void testEntrySetRemoveNullKeyPresent__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetRemoveNullKeyPresent();
}

public void testEntrySetRetainAll__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetRetainAll();
}

public void testEntrySetRetainAllNullFromEmpty__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetRetainAllNullFromEmpty();
}

public void testEntrySetSetValue__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetSetValue();
}

public void testEntrySetSetValueSameValue__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEntrySetSetValueSameValue();
}

public void testEqualsForEmptyMap__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEqualsForEmptyMap();
}

public void testEqualsForEqualMap__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEqualsForEqualMap();
}

public void testEqualsForLargerMap__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEqualsForLargerMap();
}

public void testEqualsForSmallerMap__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testEqualsForSmallerMap();
}

public void testGet__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testGet();
}

public void testGetForEmptyMap__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testGetForEmptyMap();
}

public void testGetNull__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testGetNull();
}

public void testHashCode__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testHashCode();
}

public void testHashCodeForEmptyMap__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testHashCodeForEmptyMap();
}

public void testKeySetClear__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testKeySetClear();
}

public void testKeySetRemove__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testKeySetRemove();
}

public void testKeySetRemoveAll__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testKeySetRemoveAll();
}

public void testKeySetRemoveAllNullFromEmpty__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testKeySetRemoveAllNullFromEmpty();
}

public void testKeySetRetainAll__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testKeySetRetainAll();
}

public void testKeySetRetainAllNullFromEmpty__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testKeySetRetainAllNullFromEmpty();
}

public void testPutAllExistingKey__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testPutAllExistingKey();
}

public void testPutAllNewKey__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testPutAllNewKey();
}

public void testPutExistingKey__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testPutExistingKey();
}

public void testPutNewKey__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testPutNewKey();
}

public void testPutNullKey__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testPutNullKey();
}

public void testPutNullValue__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testPutNullValue();
}

public void testPutNullValueForExistingKey__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testPutNullValueForExistingKey();
}

public void testRemove__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testRemove();
}

public void testRemoveMissingKey__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testRemoveMissingKey();
}

public void testSize__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testSize();
}

public void testValues__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testValues();
}

public void testValuesClear__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testValuesClear();
}

public void testValuesIteratorRemove__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testValuesIteratorRemove();
}

public void testValuesRemove__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testValuesRemove();
}

public void testValuesRemoveAll__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testValuesRemoveAll();
}

public void testValuesRemoveAllNullFromEmpty__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testValuesRemoveAllNullFromEmpty();
}

public void testValuesRemoveMissing__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testValuesRemoveMissing();
}

public void testValuesRetainAll__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testValuesRetainAll();
}

public void testValuesRetainAllNullFromEmpty__InverseMapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.InverseMapTests testCase = new com.google.common.collect.ImmutableBiMapTest.InverseMapTests();
  testCase.testValuesRetainAllNullFromEmpty();
}

public void testClear__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testClear();
}

public void testContainsKey__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testContainsKey();
}

public void testContainsValue__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testContainsValue();
}

public void testEntrySet__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySet();
}

public void testEntrySetAddAndAddAll__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetAddAndAddAll();
}

public void testEntrySetClear__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetClear();
}

public void testEntrySetContainsEntryIncompatibleKey__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetContainsEntryIncompatibleKey();
}

public void testEntrySetContainsEntryNullKeyMissing__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetContainsEntryNullKeyMissing();
}

public void testEntrySetContainsEntryNullKeyPresent__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetContainsEntryNullKeyPresent();
}

public void testEntrySetForEmptyMap__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetForEmptyMap();
}

public void testEntrySetIteratorRemove__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetIteratorRemove();
}

public void testEntrySetRemove__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetRemove();
}

public void testEntrySetRemoveAll__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetRemoveAll();
}

public void testEntrySetRemoveAllNullFromEmpty__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetRemoveAllNullFromEmpty();
}

public void testEntrySetRemoveDifferentValue__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetRemoveDifferentValue();
}

public void testEntrySetRemoveMissingKey__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetRemoveMissingKey();
}

public void testEntrySetRemoveNullKeyMissing__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetRemoveNullKeyMissing();
}

public void testEntrySetRemoveNullKeyPresent__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetRemoveNullKeyPresent();
}

public void testEntrySetRetainAll__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetRetainAll();
}

public void testEntrySetRetainAllNullFromEmpty__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetRetainAllNullFromEmpty();
}

public void testEntrySetSetValue__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetSetValue();
}

public void testEntrySetSetValueSameValue__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEntrySetSetValueSameValue();
}

public void testEqualsForEmptyMap__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEqualsForEmptyMap();
}

public void testEqualsForEqualMap__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEqualsForEqualMap();
}

public void testEqualsForLargerMap__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEqualsForLargerMap();
}

public void testEqualsForSmallerMap__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testEqualsForSmallerMap();
}

public void testGet__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testGet();
}

public void testGetForEmptyMap__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testGetForEmptyMap();
}

public void testGetNull__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testGetNull();
}

public void testHashCode__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testHashCode();
}

public void testHashCodeForEmptyMap__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testHashCodeForEmptyMap();
}

public void testKeySetClear__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testKeySetClear();
}

public void testKeySetRemove__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testKeySetRemove();
}

public void testKeySetRemoveAll__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testKeySetRemoveAll();
}

public void testKeySetRemoveAllNullFromEmpty__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testKeySetRemoveAllNullFromEmpty();
}

public void testKeySetRetainAll__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testKeySetRetainAll();
}

public void testKeySetRetainAllNullFromEmpty__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testKeySetRetainAllNullFromEmpty();
}

public void testPutAllExistingKey__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testPutAllExistingKey();
}

public void testPutAllNewKey__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testPutAllNewKey();
}

public void testPutExistingKey__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testPutExistingKey();
}

public void testPutNewKey__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testPutNewKey();
}

public void testPutNullKey__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testPutNullKey();
}

public void testPutNullValue__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testPutNullValue();
}

public void testPutNullValueForExistingKey__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testPutNullValueForExistingKey();
}

public void testRemove__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testRemove();
}

public void testRemoveMissingKey__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testRemoveMissingKey();
}

public void testSize__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testSize();
}

public void testValues__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testValues();
}

public void testValuesClear__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testValuesClear();
}

public void testValuesIteratorRemove__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testValuesIteratorRemove();
}

public void testValuesRemove__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testValuesRemove();
}

public void testValuesRemoveAll__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testValuesRemoveAll();
}

public void testValuesRemoveAllNullFromEmpty__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testValuesRemoveAllNullFromEmpty();
}

public void testValuesRemoveMissing__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testValuesRemoveMissing();
}

public void testValuesRetainAll__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testValuesRetainAll();
}

public void testValuesRetainAllNullFromEmpty__MapTests() throws Exception {
  com.google.common.collect.ImmutableBiMapTest.MapTests testCase = new com.google.common.collect.ImmutableBiMapTest.MapTests();
  testCase.testValuesRetainAllNullFromEmpty();
}
}

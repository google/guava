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
public class ImmutableEnumMapTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.collect.testModule";
}
public void testEmptyImmutableEnumMap() throws Exception {
  com.google.common.collect.ImmutableEnumMapTest testCase = new com.google.common.collect.ImmutableEnumMapTest();
  testCase.testEmptyImmutableEnumMap();
}

public void testImmutableEnumMapOrdering() throws Exception {
  com.google.common.collect.ImmutableEnumMapTest testCase = new com.google.common.collect.ImmutableEnumMapTest();
  testCase.testImmutableEnumMapOrdering();
}

public void testIteratesOnce() throws Exception {
  com.google.common.collect.ImmutableEnumMapTest testCase = new com.google.common.collect.ImmutableEnumMapTest();
  testCase.testIteratesOnce();
}

public void testToImmutableEnumMap() throws Exception {
  com.google.common.collect.ImmutableEnumMapTest testCase = new com.google.common.collect.ImmutableEnumMapTest();
  testCase.testToImmutableEnumMap();
}

public void testToImmutableMapMerging() throws Exception {
  com.google.common.collect.ImmutableEnumMapTest testCase = new com.google.common.collect.ImmutableEnumMapTest();
  testCase.testToImmutableMapMerging();
}

public void testToImmutableMap_exceptionOnDuplicateKey() throws Exception {
  com.google.common.collect.ImmutableEnumMapTest testCase = new com.google.common.collect.ImmutableEnumMapTest();
  testCase.testToImmutableMap_exceptionOnDuplicateKey();
}
}

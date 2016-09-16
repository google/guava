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
public class MapConstraintsTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.collect.testModule";
}
public void testConstrainedMapIllegal() throws Exception {
  com.google.common.collect.MapConstraintsTest testCase = new com.google.common.collect.MapConstraintsTest();
  testCase.testConstrainedMapIllegal();
}

public void testConstrainedMapLegal() throws Exception {
  com.google.common.collect.MapConstraintsTest testCase = new com.google.common.collect.MapConstraintsTest();
  testCase.testConstrainedMapLegal();
}

public void testConstrainedTypePreservingList() throws Exception {
  com.google.common.collect.MapConstraintsTest testCase = new com.google.common.collect.MapConstraintsTest();
  testCase.testConstrainedTypePreservingList();
}

public void testConstrainedTypePreservingRandomAccessList() throws Exception {
  com.google.common.collect.MapConstraintsTest testCase = new com.google.common.collect.MapConstraintsTest();
  testCase.testConstrainedTypePreservingRandomAccessList();
}

public void testMapEntrySetContainsNefariousEntry() throws Exception {
  com.google.common.collect.MapConstraintsTest testCase = new com.google.common.collect.MapConstraintsTest();
  testCase.testMapEntrySetContainsNefariousEntry();
}

public void testMapEntrySetToArray() throws Exception {
  com.google.common.collect.MapConstraintsTest testCase = new com.google.common.collect.MapConstraintsTest();
  testCase.testMapEntrySetToArray();
}

public void testNefariousMapPutAll() throws Exception {
  com.google.common.collect.MapConstraintsTest testCase = new com.google.common.collect.MapConstraintsTest();
  testCase.testNefariousMapPutAll();
}
}

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
public class HashMultimapTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.collect.testModule";
}
public void testCreate() throws Exception {
  com.google.common.collect.HashMultimapTest testCase = new com.google.common.collect.HashMultimapTest();
  testCase.testCreate();
}

public void testCreateFromIllegalSizes() throws Exception {
  com.google.common.collect.HashMultimapTest testCase = new com.google.common.collect.HashMultimapTest();
  testCase.testCreateFromIllegalSizes();
}

public void testCreateFromMultimap() throws Exception {
  com.google.common.collect.HashMultimapTest testCase = new com.google.common.collect.HashMultimapTest();
  testCase.testCreateFromMultimap();
}

public void testCreateFromSizes() throws Exception {
  com.google.common.collect.HashMultimapTest testCase = new com.google.common.collect.HashMultimapTest();
  testCase.testCreateFromSizes();
}

public void testEmptyMultimapsEqual() throws Exception {
  com.google.common.collect.HashMultimapTest testCase = new com.google.common.collect.HashMultimapTest();
  testCase.testEmptyMultimapsEqual();
}
}

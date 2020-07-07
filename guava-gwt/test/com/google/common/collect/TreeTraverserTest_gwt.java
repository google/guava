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
public class TreeTraverserTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.collect.testModule";
}
public void testBreadthOrder() throws Exception {
  com.google.common.collect.TreeTraverserTest testCase = new com.google.common.collect.TreeTraverserTest();
  testCase.testBreadthOrder();
}

public void testPostOrder() throws Exception {
  com.google.common.collect.TreeTraverserTest testCase = new com.google.common.collect.TreeTraverserTest();
  testCase.testPostOrder();
}

public void testPreOrder() throws Exception {
  com.google.common.collect.TreeTraverserTest testCase = new com.google.common.collect.TreeTraverserTest();
  testCase.testPreOrder();
}

public void testUsing() throws Exception {
  com.google.common.collect.TreeTraverserTest testCase = new com.google.common.collect.TreeTraverserTest();
  testCase.testUsing();
}
}

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
package com.google.common.testing;
public class EquivalenceTesterTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.testing.testModule";
}
public void testOf_NullPointerException() throws Exception {
  com.google.common.testing.EquivalenceTesterTest testCase = new com.google.common.testing.EquivalenceTesterTest();
  testCase.setUp();
  testCase.testOf_NullPointerException();
}

public void testTest() throws Exception {
  com.google.common.testing.EquivalenceTesterTest testCase = new com.google.common.testing.EquivalenceTesterTest();
  testCase.setUp();
  testCase.testTest();
}

public void testTest_NoData() throws Exception {
  com.google.common.testing.EquivalenceTesterTest testCase = new com.google.common.testing.EquivalenceTesterTest();
  testCase.setUp();
  testCase.testTest_NoData();
}

public void testTest_hash() throws Exception {
  com.google.common.testing.EquivalenceTesterTest testCase = new com.google.common.testing.EquivalenceTesterTest();
  testCase.setUp();
  testCase.testTest_hash();
}

public void testTest_inequivalence() throws Exception {
  com.google.common.testing.EquivalenceTesterTest testCase = new com.google.common.testing.EquivalenceTesterTest();
  testCase.setUp();
  testCase.testTest_inequivalence();
}

public void testTest_symmetric() throws Exception {
  com.google.common.testing.EquivalenceTesterTest testCase = new com.google.common.testing.EquivalenceTesterTest();
  testCase.setUp();
  testCase.testTest_symmetric();
}

public void testTest_trasitive() throws Exception {
  com.google.common.testing.EquivalenceTesterTest testCase = new com.google.common.testing.EquivalenceTesterTest();
  testCase.setUp();
  testCase.testTest_trasitive();
}
}

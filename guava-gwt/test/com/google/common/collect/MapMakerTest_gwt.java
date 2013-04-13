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
public class MapMakerTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.collect.testModule";
}
public void testComputerThatReturnsNull__ComputingTest() throws Exception {
  com.google.common.collect.MapMakerTest.ComputingTest testCase = new com.google.common.collect.MapMakerTest.ComputingTest();
  testCase.testComputerThatReturnsNull();
}

public void testRuntimeException__ComputingTest() throws Exception {
  com.google.common.collect.MapMakerTest.ComputingTest testCase = new com.google.common.collect.MapMakerTest.ComputingTest();
  testCase.testRuntimeException();
}

public void testRecursiveComputation__RecursiveComputationTest() throws Exception {
  com.google.common.collect.MapMakerTest.RecursiveComputationTest testCase = new com.google.common.collect.MapMakerTest.RecursiveComputationTest();
  testCase.testRecursiveComputation();
}

public void testPut_sizeIsZero__MaximumSizeTest() throws Exception {
  com.google.common.collect.MapMakerTest.MaximumSizeTest testCase = new com.google.common.collect.MapMakerTest.MaximumSizeTest();
  testCase.testPut_sizeIsZero();
}

public void testSizeBasedEviction__MaximumSizeTest() throws Exception {
  com.google.common.collect.MapMakerTest.MaximumSizeTest testCase = new com.google.common.collect.MapMakerTest.MaximumSizeTest();
  testCase.testSizeBasedEviction();
}

public void testExpiration_setTwice__MakerTest() throws Exception {
  com.google.common.collect.MapMakerTest.MakerTest testCase = new com.google.common.collect.MapMakerTest.MakerTest();
  testCase.testExpiration_setTwice();
}

public void testInitialCapacity_negative__MakerTest() throws Exception {
  com.google.common.collect.MapMakerTest.MakerTest testCase = new com.google.common.collect.MapMakerTest.MakerTest();
  testCase.testInitialCapacity_negative();
}

public void testMaximumSize_setTwice__MakerTest() throws Exception {
  com.google.common.collect.MapMakerTest.MakerTest testCase = new com.google.common.collect.MapMakerTest.MakerTest();
  testCase.testMaximumSize_setTwice();
}

public void testReturnsPlainConcurrentHashMapWhenPossible__MakerTest() throws Exception {
  com.google.common.collect.MapMakerTest.MakerTest testCase = new com.google.common.collect.MapMakerTest.MakerTest();
  testCase.testReturnsPlainConcurrentHashMapWhenPossible();
}
}

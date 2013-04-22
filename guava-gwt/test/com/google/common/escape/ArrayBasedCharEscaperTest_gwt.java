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
package com.google.common.escape;
public class ArrayBasedCharEscaperTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.escape.testModule";
}
public void testDeleteUnsafeChars() throws Exception {
  com.google.common.escape.ArrayBasedCharEscaperTest testCase = new com.google.common.escape.ArrayBasedCharEscaperTest();
  testCase.testDeleteUnsafeChars();
}

public void testReplacementPriority() throws Exception {
  com.google.common.escape.ArrayBasedCharEscaperTest testCase = new com.google.common.escape.ArrayBasedCharEscaperTest();
  testCase.testReplacementPriority();
}

public void testSafeRange() throws Exception {
  com.google.common.escape.ArrayBasedCharEscaperTest testCase = new com.google.common.escape.ArrayBasedCharEscaperTest();
  testCase.testSafeRange();
}

public void testSafeRange_maxLessThanMin() throws Exception {
  com.google.common.escape.ArrayBasedCharEscaperTest testCase = new com.google.common.escape.ArrayBasedCharEscaperTest();
  testCase.testSafeRange_maxLessThanMin();
}
}

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
public class EscapersTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.escape.testModule";
}
public void testAsUnicodeEscaper() throws Exception {
  com.google.common.escape.EscapersTest testCase = new com.google.common.escape.EscapersTest();
  testCase.testAsUnicodeEscaper();
}

public void testBuilderCreatesIndependentEscapers() throws Exception {
  com.google.common.escape.EscapersTest testCase = new com.google.common.escape.EscapersTest();
  testCase.testBuilderCreatesIndependentEscapers();
}

public void testBuilderInitialStateNoReplacement() throws Exception {
  com.google.common.escape.EscapersTest testCase = new com.google.common.escape.EscapersTest();
  testCase.testBuilderInitialStateNoReplacement();
}

public void testBuilderInitialStateNoneUnsafe() throws Exception {
  com.google.common.escape.EscapersTest testCase = new com.google.common.escape.EscapersTest();
  testCase.testBuilderInitialStateNoneUnsafe();
}

public void testBuilderRetainsState() throws Exception {
  com.google.common.escape.EscapersTest testCase = new com.google.common.escape.EscapersTest();
  testCase.testBuilderRetainsState();
}

public void testNullEscaper() throws Exception {
  com.google.common.escape.EscapersTest testCase = new com.google.common.escape.EscapersTest();
  testCase.testNullEscaper();
}
}

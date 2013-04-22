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
public class UnicodeEscaperTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.escape.testModule";
}
public void testBadStrings() throws Exception {
  com.google.common.escape.UnicodeEscaperTest testCase = new com.google.common.escape.UnicodeEscaperTest();
  testCase.testBadStrings();
}

public void testCodePointAt_IndexOutOfBoundsException() throws Exception {
  com.google.common.escape.UnicodeEscaperTest testCase = new com.google.common.escape.UnicodeEscaperTest();
  testCase.testCodePointAt_IndexOutOfBoundsException();
}

public void testFalsePositivesForNextEscapedIndex() throws Exception {
  com.google.common.escape.UnicodeEscaperTest testCase = new com.google.common.escape.UnicodeEscaperTest();
  testCase.testFalsePositivesForNextEscapedIndex();
}

public void testGrowBuffer() throws Exception {
  com.google.common.escape.UnicodeEscaperTest testCase = new com.google.common.escape.UnicodeEscaperTest();
  testCase.testGrowBuffer();
}

public void testNopEscaper() throws Exception {
  com.google.common.escape.UnicodeEscaperTest testCase = new com.google.common.escape.UnicodeEscaperTest();
  testCase.testNopEscaper();
}

public void testNullInput() throws Exception {
  com.google.common.escape.UnicodeEscaperTest testCase = new com.google.common.escape.UnicodeEscaperTest();
  testCase.testNullInput();
}

public void testSimpleEscaper() throws Exception {
  com.google.common.escape.UnicodeEscaperTest testCase = new com.google.common.escape.UnicodeEscaperTest();
  testCase.testSimpleEscaper();
}

public void testSurrogatePairs() throws Exception {
  com.google.common.escape.UnicodeEscaperTest testCase = new com.google.common.escape.UnicodeEscaperTest();
  testCase.testSurrogatePairs();
}

public void testTrailingHighSurrogate() throws Exception {
  com.google.common.escape.UnicodeEscaperTest testCase = new com.google.common.escape.UnicodeEscaperTest();
  testCase.testTrailingHighSurrogate();
}
}

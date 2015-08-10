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
package com.google.common.base;
public class Utf8Test_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.base.testModule";
}
public void testEncodedLength_invalidStrings() throws Exception {
  com.google.common.base.Utf8Test testCase = new com.google.common.base.Utf8Test();
  testCase.testEncodedLength_invalidStrings();
}

public void testEncodedLength_validStrings() throws Exception {
  com.google.common.base.Utf8Test testCase = new com.google.common.base.Utf8Test();
  testCase.testEncodedLength_validStrings();
}

public void testEncodedLength_validStrings2() throws Exception {
  com.google.common.base.Utf8Test testCase = new com.google.common.base.Utf8Test();
  testCase.testEncodedLength_validStrings2();
}

public void testIsWellFormed_4BytesSamples() throws Exception {
  com.google.common.base.Utf8Test testCase = new com.google.common.base.Utf8Test();
  testCase.testIsWellFormed_4BytesSamples();
}

public void testShardsHaveExpectedRoundTrippables() throws Exception {
  com.google.common.base.Utf8Test testCase = new com.google.common.base.Utf8Test();
  testCase.testShardsHaveExpectedRoundTrippables();
}

public void testSomeSequences() throws Exception {
  com.google.common.base.Utf8Test testCase = new com.google.common.base.Utf8Test();
  testCase.testSomeSequences();
}
}

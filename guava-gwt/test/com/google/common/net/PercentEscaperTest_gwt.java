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
package com.google.common.net;
public class PercentEscaperTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.net.testModule";
}
public void testBadArguments_badchars() throws Exception {
  com.google.common.net.PercentEscaperTest testCase = new com.google.common.net.PercentEscaperTest();
  testCase.testBadArguments_badchars();
}

public void testBadArguments_null() throws Exception {
  com.google.common.net.PercentEscaperTest testCase = new com.google.common.net.PercentEscaperTest();
  testCase.testBadArguments_null();
}

public void testBadArguments_plusforspace() throws Exception {
  com.google.common.net.PercentEscaperTest testCase = new com.google.common.net.PercentEscaperTest();
  testCase.testBadArguments_plusforspace();
}

public void testCustomEscaper() throws Exception {
  com.google.common.net.PercentEscaperTest testCase = new com.google.common.net.PercentEscaperTest();
  testCase.testCustomEscaper();
}

public void testCustomEscaper_withpercent() throws Exception {
  com.google.common.net.PercentEscaperTest testCase = new com.google.common.net.PercentEscaperTest();
  testCase.testCustomEscaper_withpercent();
}

public void testPlusForSpace() throws Exception {
  com.google.common.net.PercentEscaperTest testCase = new com.google.common.net.PercentEscaperTest();
  testCase.testPlusForSpace();
}

public void testSimpleEscaper() throws Exception {
  com.google.common.net.PercentEscaperTest testCase = new com.google.common.net.PercentEscaperTest();
  testCase.testSimpleEscaper();
}
}

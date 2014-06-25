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
public class EnumsTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.base.testModule";
}
public void testGetIfPresent() throws Exception {
  com.google.common.base.EnumsTest testCase = new com.google.common.base.EnumsTest();
  testCase.testGetIfPresent();
}

public void testGetIfPresent_caseSensitive() throws Exception {
  com.google.common.base.EnumsTest testCase = new com.google.common.base.EnumsTest();
  testCase.testGetIfPresent_caseSensitive();
}

public void testGetIfPresent_whenNoMatchingConstant() throws Exception {
  com.google.common.base.EnumsTest testCase = new com.google.common.base.EnumsTest();
  testCase.testGetIfPresent_whenNoMatchingConstant();
}

public void testStringConverter_convert() throws Exception {
  com.google.common.base.EnumsTest testCase = new com.google.common.base.EnumsTest();
  testCase.testStringConverter_convert();
}

public void testStringConverter_convertError() throws Exception {
  com.google.common.base.EnumsTest testCase = new com.google.common.base.EnumsTest();
  testCase.testStringConverter_convertError();
}

public void testStringConverter_nullConversions() throws Exception {
  com.google.common.base.EnumsTest testCase = new com.google.common.base.EnumsTest();
  testCase.testStringConverter_nullConversions();
}

public void testStringConverter_reverse() throws Exception {
  com.google.common.base.EnumsTest testCase = new com.google.common.base.EnumsTest();
  testCase.testStringConverter_reverse();
}

public void testStringConverter_serialization() throws Exception {
  com.google.common.base.EnumsTest testCase = new com.google.common.base.EnumsTest();
  testCase.testStringConverter_serialization();
}
}

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
public class HostAndPortTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.net.testModule";
}
public void testFromHost() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testFromHost();
}

public void testFromParts() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testFromParts();
}

public void testFromStringBadDefaultPort() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testFromStringBadDefaultPort();
}

public void testFromStringBadPort() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testFromStringBadPort();
}

public void testFromStringParseableNonsense() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testFromStringParseableNonsense();
}

public void testFromStringUnparseableNonsense() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testFromStringUnparseableNonsense();
}

public void testFromStringUnusedDefaultPort() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testFromStringUnusedDefaultPort();
}

public void testFromStringWellFormed() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testFromStringWellFormed();
}

public void testGetPortOrDefault() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testGetPortOrDefault();
}

public void testHashCodeAndEquals() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testHashCodeAndEquals();
}

public void testRequireBracketsForIPv6() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testRequireBracketsForIPv6();
}

public void testSerialization() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testSerialization();
}

public void testToString() throws Exception {
  com.google.common.net.HostAndPortTest testCase = new com.google.common.net.HostAndPortTest();
  testCase.testToString();
}
}

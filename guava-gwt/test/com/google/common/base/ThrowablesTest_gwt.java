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
public class ThrowablesTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.base.testModule";
}
public void testGetCasualChainLoop() throws Exception {
  com.google.common.base.ThrowablesTest testCase = new com.google.common.base.ThrowablesTest();
  testCase.testGetCasualChainLoop();
}

public void testGetCasualChainNull() throws Exception {
  com.google.common.base.ThrowablesTest testCase = new com.google.common.base.ThrowablesTest();
  testCase.testGetCasualChainNull();
}

public void testGetCausalChain() throws Exception {
  com.google.common.base.ThrowablesTest testCase = new com.google.common.base.ThrowablesTest();
  testCase.testGetCausalChain();
}

public void testGetRootCause_DoubleWrapped() throws Exception {
  com.google.common.base.ThrowablesTest testCase = new com.google.common.base.ThrowablesTest();
  testCase.testGetRootCause_DoubleWrapped();
}

public void testGetRootCause_Loop() throws Exception {
  com.google.common.base.ThrowablesTest testCase = new com.google.common.base.ThrowablesTest();
  testCase.testGetRootCause_Loop();
}

public void testGetRootCause_NoCause() throws Exception {
  com.google.common.base.ThrowablesTest testCase = new com.google.common.base.ThrowablesTest();
  testCase.testGetRootCause_NoCause();
}

public void testGetRootCause_SingleWrapped() throws Exception {
  com.google.common.base.ThrowablesTest testCase = new com.google.common.base.ThrowablesTest();
  testCase.testGetRootCause_SingleWrapped();
}

public void testThrowIfUnchecked_Checked() throws Exception {
  com.google.common.base.ThrowablesTest testCase = new com.google.common.base.ThrowablesTest();
  testCase.testThrowIfUnchecked_Checked();
}

public void testThrowIfUnchecked_Error() throws Exception {
  com.google.common.base.ThrowablesTest testCase = new com.google.common.base.ThrowablesTest();
  testCase.testThrowIfUnchecked_Error();
}

public void testThrowIfUnchecked_Unchecked() throws Exception {
  com.google.common.base.ThrowablesTest testCase = new com.google.common.base.ThrowablesTest();
  testCase.testThrowIfUnchecked_Unchecked();
}

public void testThrowIfUnchecked_null() throws Exception {
  com.google.common.base.ThrowablesTest testCase = new com.google.common.base.ThrowablesTest();
  testCase.testThrowIfUnchecked_null();
}
}

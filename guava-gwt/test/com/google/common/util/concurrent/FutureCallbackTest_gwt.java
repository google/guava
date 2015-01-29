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
package com.google.common.util.concurrent;
public class FutureCallbackTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.util.concurrent.testModule";
}
public void testCancel() throws Exception {
  com.google.common.util.concurrent.FutureCallbackTest testCase = new com.google.common.util.concurrent.FutureCallbackTest();
  testCase.testCancel();
}

public void testExecutorSuccess() throws Exception {
  com.google.common.util.concurrent.FutureCallbackTest testCase = new com.google.common.util.concurrent.FutureCallbackTest();
  testCase.testExecutorSuccess();
}

public void testRuntimeExeceptionFromGet() throws Exception {
  com.google.common.util.concurrent.FutureCallbackTest testCase = new com.google.common.util.concurrent.FutureCallbackTest();
  testCase.testRuntimeExeceptionFromGet();
}

public void testSameThreadExecutionException() throws Exception {
  com.google.common.util.concurrent.FutureCallbackTest testCase = new com.google.common.util.concurrent.FutureCallbackTest();
  testCase.testSameThreadExecutionException();
}

public void testSameThreadSuccess() throws Exception {
  com.google.common.util.concurrent.FutureCallbackTest testCase = new com.google.common.util.concurrent.FutureCallbackTest();
  testCase.testSameThreadSuccess();
}

public void testThrowErrorFromGet() throws Exception {
  com.google.common.util.concurrent.FutureCallbackTest testCase = new com.google.common.util.concurrent.FutureCallbackTest();
  testCase.testThrowErrorFromGet();
}

public void testWildcardFuture() throws Exception {
  com.google.common.util.concurrent.FutureCallbackTest testCase = new com.google.common.util.concurrent.FutureCallbackTest();
  testCase.testWildcardFuture();
}
}

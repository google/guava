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
package com.google.common.xml;
public class XmlEscapersTest_gwt extends com.google.gwt.junit.client.GWTTestCase {
@Override public String getModuleName() {
  return "com.google.common.xml.testModule";
}
public void testXmlAttributeEscaper() throws Exception {
  com.google.common.xml.XmlEscapersTest testCase = new com.google.common.xml.XmlEscapersTest();
  testCase.testXmlAttributeEscaper();
}

public void testXmlContentEscaper() throws Exception {
  com.google.common.xml.XmlEscapersTest testCase = new com.google.common.xml.XmlEscapersTest();
  testCase.testXmlContentEscaper();
}
}

/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.io;

import junit.framework.TestCase;

/**
 * Unit tests for Files.simplifyPath().
 *
 * @author Pablo Bellver
 */
public class CleanPathTest extends TestCase {

  public void testSimplify() {
    assertEquals("", Files.simplifyPath("."));
  }

  public void testSimplify1() {
    assertEquals(".", Files.simplifyPath(""));
    assertEquals(" ", Files.simplifyPath(" "));
  }

  public void testSimplify2() {
    assertEquals("x", Files.simplifyPath("x"));
  }

  public void testSimplify3() {
    assertEquals("/a/b/c/d", Files.simplifyPath("/a/b/c/d"));
  }

  public void testSimplify4() {
    assertEquals("/a/b/c/d", Files.simplifyPath("/a/b/c/d/"));
  }

  public void testSimplify5() {
    assertEquals("/a/b", Files.simplifyPath("/a//b"));
  }

  public void testSimplify6() {
    assertEquals("/a/b", Files.simplifyPath("//a//b/"));
  }

  public void testSimplify7() {
    assertEquals("/", Files.simplifyPath("/.."));
  }

  public void testSimplify8() {
    assertEquals("/", Files.simplifyPath("/././././"));
  }

  public void testSimplify9() {
    assertEquals("/a", Files.simplifyPath("/a/b/.."));
  }

  public void testSimplify10() {
    assertEquals("/", Files.simplifyPath("/a/b/../../.."));
  }

  public void testSimplify11() {
    assertEquals("/", Files.simplifyPath("//a//b/..////../..//"));
  }

  public void testSimplify12() {
    assertEquals("/x", Files.simplifyPath("//a//../x//"));
  }
  
  public void testSimplify13() {
    assertEquals("../c", Files.simplifyPath("a/b/../../../c"));
  }
  
  public void testSimplifyDotDot() {
    assertEquals("..", Files.simplifyPath(".."));
  }

  public void testSimplifyDotDotSlash() {
    assertEquals("..", Files.simplifyPath("../"));
    assertEquals("..", Files.simplifyPath("a/../.."));
    assertEquals("..", Files.simplifyPath("a/../../"));
  }
  
  public void testSimplifyDotDots() {
    assertEquals("../..", Files.simplifyPath("a/../../.."));
    assertEquals("../../..", Files.simplifyPath("a/../../../.."));
  }
  
  // b/4558855
  public void testMadbotsBug() {
    assertEquals("../this", Files.simplifyPath("../this"));
    assertEquals("../this/is/ok", Files.simplifyPath("../this/is/ok"));
    assertEquals("../ok", Files.simplifyPath("../this/../ok"));
  }
  
  // https://code.google.com/p/guava-libraries/issues/detail?id=705
  public void test705() {
    assertEquals("../b", Files.simplifyPath("x/../../b"));
    assertEquals("b", Files.simplifyPath("x/../b"));
  }
}

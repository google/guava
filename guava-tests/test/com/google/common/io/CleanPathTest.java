// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.common.io;

import junit.framework.TestCase;

/**
 * Unit tests for Files.simplifyPath().
 *
 * Note: ported from file/base/cleanpath_unittest.cc
 *
 * @author pablob@google.com
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
  }
}

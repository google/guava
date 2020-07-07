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

import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.CharMatcher.breakingWhitespace;
import static com.google.common.base.CharMatcher.forPredicate;
import static com.google.common.base.CharMatcher.inRange;
import static com.google.common.base.CharMatcher.is;
import static com.google.common.base.CharMatcher.isNot;
import static com.google.common.base.CharMatcher.noneOf;
import static com.google.common.base.CharMatcher.whitespace;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Sets;
import com.google.common.testing.NullPointerTester;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Unit test for {@link CharMatcher}.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
public class CharMatcherTest extends TestCase {

  @GwtIncompatible // NullPointerTester
  public void testStaticNullPointers() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(CharMatcher.class);
    tester.testAllPublicInstanceMethods(CharMatcher.any());
    tester.testAllPublicInstanceMethods(CharMatcher.anyOf("abc"));
  }

  private static final CharMatcher WHATEVER =
      new CharMatcher() {
        @Override
        public boolean matches(char c) {
          throw new AssertionFailedError("You weren't supposed to actually invoke me!");
        }
      };

  public void testAnyAndNone_logicalOps() throws Exception {
    // These are testing behavior that's never promised by the API, but since
    // we're lucky enough that these do pass, it saves us from having to write
    // more excruciating tests! Hooray!

    assertSame(CharMatcher.any(), CharMatcher.none().negate());
    assertSame(CharMatcher.none(), CharMatcher.any().negate());

    assertSame(WHATEVER, CharMatcher.any().and(WHATEVER));
    assertSame(CharMatcher.any(), CharMatcher.any().or(WHATEVER));

    assertSame(CharMatcher.none(), CharMatcher.none().and(WHATEVER));
    assertSame(WHATEVER, CharMatcher.none().or(WHATEVER));
  }

  // The rest of the behavior of ANY and DEFAULT will be covered in the tests for
  // the text processing methods below.

  public void testWhitespaceBreakingWhitespaceSubset() throws Exception {
    for (int c = 0; c <= Character.MAX_VALUE; c++) {
      if (breakingWhitespace().matches((char) c)) {
        assertTrue(Integer.toHexString(c), whitespace().matches((char) c));
      }
    }
  }

  // The next tests require ICU4J and have, at least for now, been sliced out
  // of the open-source view of the tests.

  @GwtIncompatible // Character.isISOControl
  public void testJavaIsoControl() {
    for (int c = 0; c <= Character.MAX_VALUE; c++) {
      assertEquals(
          "" + c, Character.isISOControl(c), CharMatcher.javaIsoControl().matches((char) c));
    }
  }

  // Omitting tests for the rest of the JAVA_* constants as these are defined
  // as extremely straightforward pass-throughs to the JDK methods.

  // We're testing the is(), isNot(), anyOf(), noneOf() and inRange() methods
  // below by testing their text-processing methods.

  // The organization of this test class is unusual, as it's not done by
  // method, but by overall "scenario". Also, the variety of actual tests we
  // do borders on absurd overkill. Better safe than sorry, though?

  @GwtIncompatible // java.util.BitSet
  public void testSetBits() {
    doTestSetBits(CharMatcher.any());
    doTestSetBits(CharMatcher.none());
    doTestSetBits(is('a'));
    doTestSetBits(isNot('a'));
    doTestSetBits(anyOf(""));
    doTestSetBits(anyOf("x"));
    doTestSetBits(anyOf("xy"));
    doTestSetBits(anyOf("CharMatcher"));
    doTestSetBits(noneOf("CharMatcher"));
    doTestSetBits(inRange('n', 'q'));
    doTestSetBits(forPredicate(Predicates.equalTo('c')));
    doTestSetBits(CharMatcher.ascii());
    doTestSetBits(CharMatcher.digit());
    doTestSetBits(CharMatcher.invisible());
    doTestSetBits(CharMatcher.whitespace());
    doTestSetBits(inRange('A', 'Z').and(inRange('F', 'K').negate()));
  }

  @GwtIncompatible // java.util.BitSet
  private void doTestSetBits(CharMatcher matcher) {
    BitSet bitset = new BitSet();
    matcher.setBits(bitset);
    for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
      assertEquals(matcher.matches((char) i), bitset.get(i));
    }
  }

  public void testEmpty() throws Exception {
    doTestEmpty(CharMatcher.any());
    doTestEmpty(CharMatcher.none());
    doTestEmpty(is('a'));
    doTestEmpty(isNot('a'));
    doTestEmpty(anyOf(""));
    doTestEmpty(anyOf("x"));
    doTestEmpty(anyOf("xy"));
    doTestEmpty(anyOf("CharMatcher"));
    doTestEmpty(noneOf("CharMatcher"));
    doTestEmpty(inRange('n', 'q'));
    doTestEmpty(forPredicate(Predicates.equalTo('c')));
  }

  @GwtIncompatible // NullPointerTester
  public void testNull() throws Exception {
    doTestNull(CharMatcher.any());
    doTestNull(CharMatcher.none());
    doTestNull(is('a'));
    doTestNull(isNot('a'));
    doTestNull(anyOf(""));
    doTestNull(anyOf("x"));
    doTestNull(anyOf("xy"));
    doTestNull(anyOf("CharMatcher"));
    doTestNull(noneOf("CharMatcher"));
    doTestNull(inRange('n', 'q'));
    doTestNull(forPredicate(Predicates.equalTo('c')));
  }

  private void doTestEmpty(CharMatcher matcher) throws Exception {
    reallyTestEmpty(matcher);
    reallyTestEmpty(matcher.negate());
    reallyTestEmpty(matcher.precomputed());
  }

  private void reallyTestEmpty(CharMatcher matcher) throws Exception {
    assertEquals(-1, matcher.indexIn(""));
    assertEquals(-1, matcher.indexIn("", 0));
    try {
      matcher.indexIn("", 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      matcher.indexIn("", -1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    assertEquals(-1, matcher.lastIndexIn(""));
    assertFalse(matcher.matchesAnyOf(""));
    assertTrue(matcher.matchesAllOf(""));
    assertTrue(matcher.matchesNoneOf(""));
    assertEquals("", matcher.removeFrom(""));
    assertEquals("", matcher.replaceFrom("", 'z'));
    assertEquals("", matcher.replaceFrom("", "ZZ"));
    assertEquals("", matcher.trimFrom(""));
    assertEquals(0, matcher.countIn(""));
  }

  @GwtIncompatible // NullPointerTester
  private static void doTestNull(CharMatcher matcher) throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(matcher);
  }

  public void testNoMatches() {
    doTestNoMatches(CharMatcher.none(), "blah");
    doTestNoMatches(is('a'), "bcde");
    doTestNoMatches(isNot('a'), "aaaa");
    doTestNoMatches(anyOf(""), "abcd");
    doTestNoMatches(anyOf("x"), "abcd");
    doTestNoMatches(anyOf("xy"), "abcd");
    doTestNoMatches(anyOf("CharMatcher"), "zxqy");
    doTestNoMatches(noneOf("CharMatcher"), "ChMa");
    doTestNoMatches(inRange('p', 'x'), "mom");
    doTestNoMatches(forPredicate(Predicates.equalTo('c')), "abe");
    doTestNoMatches(inRange('A', 'Z').and(inRange('F', 'K').negate()), "F1a");
    doTestNoMatches(CharMatcher.digit(), "\tAz()");
    doTestNoMatches(CharMatcher.javaDigit(), "\tAz()");
    doTestNoMatches(CharMatcher.digit().and(CharMatcher.ascii()), "\tAz()");
    doTestNoMatches(CharMatcher.singleWidth(), "\u05bf\u3000");
  }

  private void doTestNoMatches(CharMatcher matcher, String s) {
    reallyTestNoMatches(matcher, s);
    reallyTestAllMatches(matcher.negate(), s);
    reallyTestNoMatches(matcher.precomputed(), s);
    reallyTestAllMatches(matcher.negate().precomputed(), s);
    reallyTestAllMatches(matcher.precomputed().negate(), s);
    reallyTestNoMatches(forPredicate(matcher), s);

    reallyTestNoMatches(matcher, new StringBuilder(s));
  }

  public void testAllMatches() {
    doTestAllMatches(CharMatcher.any(), "blah");
    doTestAllMatches(isNot('a'), "bcde");
    doTestAllMatches(is('a'), "aaaa");
    doTestAllMatches(noneOf("CharMatcher"), "zxqy");
    doTestAllMatches(anyOf("x"), "xxxx");
    doTestAllMatches(anyOf("xy"), "xyyx");
    doTestAllMatches(anyOf("CharMatcher"), "ChMa");
    doTestAllMatches(inRange('m', 'p'), "mom");
    doTestAllMatches(forPredicate(Predicates.equalTo('c')), "ccc");
    doTestAllMatches(CharMatcher.digit(), "0123456789\u0ED0\u1B59");
    doTestAllMatches(CharMatcher.javaDigit(), "0123456789");
    doTestAllMatches(CharMatcher.digit().and(CharMatcher.ascii()), "0123456789");
    doTestAllMatches(CharMatcher.singleWidth(), "\t0123ABCdef~\u00A0\u2111");
  }

  private void doTestAllMatches(CharMatcher matcher, String s) {
    reallyTestAllMatches(matcher, s);
    reallyTestNoMatches(matcher.negate(), s);
    reallyTestAllMatches(matcher.precomputed(), s);
    reallyTestNoMatches(matcher.negate().precomputed(), s);
    reallyTestNoMatches(matcher.precomputed().negate(), s);
    reallyTestAllMatches(forPredicate(matcher), s);

    reallyTestAllMatches(matcher, new StringBuilder(s));
  }

  private void reallyTestNoMatches(CharMatcher matcher, CharSequence s) {
    assertFalse(matcher.matches(s.charAt(0)));
    assertEquals(-1, matcher.indexIn(s));
    assertEquals(-1, matcher.indexIn(s, 0));
    assertEquals(-1, matcher.indexIn(s, 1));
    assertEquals(-1, matcher.indexIn(s, s.length()));
    try {
      matcher.indexIn(s, s.length() + 1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      matcher.indexIn(s, -1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    assertEquals(-1, matcher.lastIndexIn(s));
    assertFalse(matcher.matchesAnyOf(s));
    assertFalse(matcher.matchesAllOf(s));
    assertTrue(matcher.matchesNoneOf(s));

    assertEquals(s.toString(), matcher.removeFrom(s));
    assertEquals(s.toString(), matcher.replaceFrom(s, 'z'));
    assertEquals(s.toString(), matcher.replaceFrom(s, "ZZ"));
    assertEquals(s.toString(), matcher.trimFrom(s));
    assertEquals(0, matcher.countIn(s));
  }

  private void reallyTestAllMatches(CharMatcher matcher, CharSequence s) {
    assertTrue(matcher.matches(s.charAt(0)));
    assertEquals(0, matcher.indexIn(s));
    assertEquals(0, matcher.indexIn(s, 0));
    assertEquals(1, matcher.indexIn(s, 1));
    assertEquals(-1, matcher.indexIn(s, s.length()));
    assertEquals(s.length() - 1, matcher.lastIndexIn(s));
    assertTrue(matcher.matchesAnyOf(s));
    assertTrue(matcher.matchesAllOf(s));
    assertFalse(matcher.matchesNoneOf(s));
    assertEquals("", matcher.removeFrom(s));
    assertEquals(Strings.repeat("z", s.length()), matcher.replaceFrom(s, 'z'));
    assertEquals(Strings.repeat("ZZ", s.length()), matcher.replaceFrom(s, "ZZ"));
    assertEquals("", matcher.trimFrom(s));
    assertEquals(s.length(), matcher.countIn(s));
  }

  public void testGeneral() {
    doTestGeneral(is('a'), 'a', 'b');
    doTestGeneral(isNot('a'), 'b', 'a');
    doTestGeneral(anyOf("x"), 'x', 'z');
    doTestGeneral(anyOf("xy"), 'y', 'z');
    doTestGeneral(anyOf("CharMatcher"), 'C', 'z');
    doTestGeneral(noneOf("CharMatcher"), 'z', 'C');
    doTestGeneral(inRange('p', 'x'), 'q', 'z');
  }

  private void doTestGeneral(CharMatcher matcher, char match, char noMatch) {
    doTestOneCharMatch(matcher, "" + match);
    doTestOneCharNoMatch(matcher, "" + noMatch);
    doTestMatchThenNoMatch(matcher, "" + match + noMatch);
    doTestNoMatchThenMatch(matcher, "" + noMatch + match);
  }

  private void doTestOneCharMatch(CharMatcher matcher, String s) {
    reallyTestOneCharMatch(matcher, s);
    reallyTestOneCharNoMatch(matcher.negate(), s);
    reallyTestOneCharMatch(matcher.precomputed(), s);
    reallyTestOneCharNoMatch(matcher.negate().precomputed(), s);
    reallyTestOneCharNoMatch(matcher.precomputed().negate(), s);
  }

  private void doTestOneCharNoMatch(CharMatcher matcher, String s) {
    reallyTestOneCharNoMatch(matcher, s);
    reallyTestOneCharMatch(matcher.negate(), s);
    reallyTestOneCharNoMatch(matcher.precomputed(), s);
    reallyTestOneCharMatch(matcher.negate().precomputed(), s);
    reallyTestOneCharMatch(matcher.precomputed().negate(), s);
  }

  private void doTestMatchThenNoMatch(CharMatcher matcher, String s) {
    reallyTestMatchThenNoMatch(matcher, s);
    reallyTestNoMatchThenMatch(matcher.negate(), s);
    reallyTestMatchThenNoMatch(matcher.precomputed(), s);
    reallyTestNoMatchThenMatch(matcher.negate().precomputed(), s);
    reallyTestNoMatchThenMatch(matcher.precomputed().negate(), s);
  }

  private void doTestNoMatchThenMatch(CharMatcher matcher, String s) {
    reallyTestNoMatchThenMatch(matcher, s);
    reallyTestMatchThenNoMatch(matcher.negate(), s);
    reallyTestNoMatchThenMatch(matcher.precomputed(), s);
    reallyTestMatchThenNoMatch(matcher.negate().precomputed(), s);
    reallyTestMatchThenNoMatch(matcher.precomputed().negate(), s);
  }

  @SuppressWarnings("deprecation") // intentionally testing apply() method
  private void reallyTestOneCharMatch(CharMatcher matcher, String s) {
    assertTrue(matcher.matches(s.charAt(0)));
    assertTrue(matcher.apply(s.charAt(0)));
    assertEquals(0, matcher.indexIn(s));
    assertEquals(0, matcher.indexIn(s, 0));
    assertEquals(-1, matcher.indexIn(s, 1));
    assertEquals(0, matcher.lastIndexIn(s));
    assertTrue(matcher.matchesAnyOf(s));
    assertTrue(matcher.matchesAllOf(s));
    assertFalse(matcher.matchesNoneOf(s));
    assertEquals("", matcher.removeFrom(s));
    assertEquals("z", matcher.replaceFrom(s, 'z'));
    assertEquals("ZZ", matcher.replaceFrom(s, "ZZ"));
    assertEquals("", matcher.trimFrom(s));
    assertEquals(1, matcher.countIn(s));
  }

  @SuppressWarnings("deprecation") // intentionally testing apply() method
  private void reallyTestOneCharNoMatch(CharMatcher matcher, String s) {
    assertFalse(matcher.matches(s.charAt(0)));
    assertFalse(matcher.apply(s.charAt(0)));
    assertEquals(-1, matcher.indexIn(s));
    assertEquals(-1, matcher.indexIn(s, 0));
    assertEquals(-1, matcher.indexIn(s, 1));
    assertEquals(-1, matcher.lastIndexIn(s));
    assertFalse(matcher.matchesAnyOf(s));
    assertFalse(matcher.matchesAllOf(s));
    assertTrue(matcher.matchesNoneOf(s));

    assertSame(s, matcher.removeFrom(s));
    assertSame(s, matcher.replaceFrom(s, 'z'));
    assertSame(s, matcher.replaceFrom(s, "ZZ"));
    assertSame(s, matcher.trimFrom(s));
    assertSame(0, matcher.countIn(s));
  }

  private void reallyTestMatchThenNoMatch(CharMatcher matcher, String s) {
    assertEquals(0, matcher.indexIn(s));
    assertEquals(0, matcher.indexIn(s, 0));
    assertEquals(-1, matcher.indexIn(s, 1));
    assertEquals(-1, matcher.indexIn(s, 2));
    assertEquals(0, matcher.lastIndexIn(s));
    assertTrue(matcher.matchesAnyOf(s));
    assertFalse(matcher.matchesAllOf(s));
    assertFalse(matcher.matchesNoneOf(s));
    assertEquals(s.substring(1), matcher.removeFrom(s));
    assertEquals("z" + s.substring(1), matcher.replaceFrom(s, 'z'));
    assertEquals("ZZ" + s.substring(1), matcher.replaceFrom(s, "ZZ"));
    assertEquals(s.substring(1), matcher.trimFrom(s));
    assertEquals(1, matcher.countIn(s));
  }

  private void reallyTestNoMatchThenMatch(CharMatcher matcher, String s) {
    assertEquals(1, matcher.indexIn(s));
    assertEquals(1, matcher.indexIn(s, 0));
    assertEquals(1, matcher.indexIn(s, 1));
    assertEquals(-1, matcher.indexIn(s, 2));
    assertEquals(1, matcher.lastIndexIn(s));
    assertTrue(matcher.matchesAnyOf(s));
    assertFalse(matcher.matchesAllOf(s));
    assertFalse(matcher.matchesNoneOf(s));
    assertEquals(s.substring(0, 1), matcher.removeFrom(s));
    assertEquals(s.substring(0, 1) + "z", matcher.replaceFrom(s, 'z'));
    assertEquals(s.substring(0, 1) + "ZZ", matcher.replaceFrom(s, "ZZ"));
    assertEquals(s.substring(0, 1), matcher.trimFrom(s));
    assertEquals(1, matcher.countIn(s));
  }

  /**
   * Checks that expected is equals to out, and further, if in is equals to expected, then out is
   * successfully optimized to be identical to in, i.e. that "in" is simply returned.
   */
  private void assertEqualsSame(String expected, String in, String out) {
    if (expected.equals(in)) {
      assertSame(in, out);
    } else {
      assertEquals(expected, out);
    }
  }

  // Test collapse() a little differently than the rest, as we really want to
  // cover lots of different configurations of input text
  public void testCollapse() {
    // collapsing groups of '-' into '_' or '-'
    doTestCollapse("-", "_");
    doTestCollapse("x-", "x_");
    doTestCollapse("-x", "_x");
    doTestCollapse("--", "_");
    doTestCollapse("x--", "x_");
    doTestCollapse("--x", "_x");
    doTestCollapse("-x-", "_x_");
    doTestCollapse("x-x", "x_x");
    doTestCollapse("---", "_");
    doTestCollapse("--x-", "_x_");
    doTestCollapse("--xx", "_xx");
    doTestCollapse("-x--", "_x_");
    doTestCollapse("-x-x", "_x_x");
    doTestCollapse("-xx-", "_xx_");
    doTestCollapse("x--x", "x_x");
    doTestCollapse("x-x-", "x_x_");
    doTestCollapse("x-xx", "x_xx");
    doTestCollapse("x-x--xx---x----x", "x_x_xx_x_x");

    doTestCollapseWithNoChange("");
    doTestCollapseWithNoChange("x");
    doTestCollapseWithNoChange("xx");
  }

  private void doTestCollapse(String in, String out) {
    // Try a few different matchers which all match '-' and not 'x'
    // Try replacement chars that both do and do not change the value.
    for (char replacement : new char[] {'_', '-'}) {
      String expected = out.replace('_', replacement);
      assertEqualsSame(expected, in, is('-').collapseFrom(in, replacement));
      assertEqualsSame(expected, in, is('-').collapseFrom(in, replacement));
      assertEqualsSame(expected, in, is('-').or(is('#')).collapseFrom(in, replacement));
      assertEqualsSame(expected, in, isNot('x').collapseFrom(in, replacement));
      assertEqualsSame(expected, in, is('x').negate().collapseFrom(in, replacement));
      assertEqualsSame(expected, in, anyOf("-").collapseFrom(in, replacement));
      assertEqualsSame(expected, in, anyOf("-#").collapseFrom(in, replacement));
      assertEqualsSame(expected, in, anyOf("-#123").collapseFrom(in, replacement));
    }
  }

  private void doTestCollapseWithNoChange(String inout) {
    assertSame(inout, is('-').collapseFrom(inout, '_'));
    assertSame(inout, is('-').or(is('#')).collapseFrom(inout, '_'));
    assertSame(inout, isNot('x').collapseFrom(inout, '_'));
    assertSame(inout, is('x').negate().collapseFrom(inout, '_'));
    assertSame(inout, anyOf("-").collapseFrom(inout, '_'));
    assertSame(inout, anyOf("-#").collapseFrom(inout, '_'));
    assertSame(inout, anyOf("-#123").collapseFrom(inout, '_'));
    assertSame(inout, CharMatcher.none().collapseFrom(inout, '_'));
  }

  public void testCollapse_any() {
    assertEquals("", CharMatcher.any().collapseFrom("", '_'));
    assertEquals("_", CharMatcher.any().collapseFrom("a", '_'));
    assertEquals("_", CharMatcher.any().collapseFrom("ab", '_'));
    assertEquals("_", CharMatcher.any().collapseFrom("abcd", '_'));
  }

  public void testTrimFrom() {
    // trimming -
    doTestTrimFrom("-", "");
    doTestTrimFrom("x-", "x");
    doTestTrimFrom("-x", "x");
    doTestTrimFrom("--", "");
    doTestTrimFrom("x--", "x");
    doTestTrimFrom("--x", "x");
    doTestTrimFrom("-x-", "x");
    doTestTrimFrom("x-x", "x-x");
    doTestTrimFrom("---", "");
    doTestTrimFrom("--x-", "x");
    doTestTrimFrom("--xx", "xx");
    doTestTrimFrom("-x--", "x");
    doTestTrimFrom("-x-x", "x-x");
    doTestTrimFrom("-xx-", "xx");
    doTestTrimFrom("x--x", "x--x");
    doTestTrimFrom("x-x-", "x-x");
    doTestTrimFrom("x-xx", "x-xx");
    doTestTrimFrom("x-x--xx---x----x", "x-x--xx---x----x");
    // additional testing using the doc example
    assertEquals("cat", anyOf("ab").trimFrom("abacatbab"));
  }

  private void doTestTrimFrom(String in, String out) {
    // Try a few different matchers which all match '-' and not 'x'
    assertEquals(out, is('-').trimFrom(in));
    assertEquals(out, is('-').or(is('#')).trimFrom(in));
    assertEquals(out, isNot('x').trimFrom(in));
    assertEquals(out, is('x').negate().trimFrom(in));
    assertEquals(out, anyOf("-").trimFrom(in));
    assertEquals(out, anyOf("-#").trimFrom(in));
    assertEquals(out, anyOf("-#123").trimFrom(in));
  }

  public void testTrimLeadingFrom() {
    // trimming -
    doTestTrimLeadingFrom("-", "");
    doTestTrimLeadingFrom("x-", "x-");
    doTestTrimLeadingFrom("-x", "x");
    doTestTrimLeadingFrom("--", "");
    doTestTrimLeadingFrom("x--", "x--");
    doTestTrimLeadingFrom("--x", "x");
    doTestTrimLeadingFrom("-x-", "x-");
    doTestTrimLeadingFrom("x-x", "x-x");
    doTestTrimLeadingFrom("---", "");
    doTestTrimLeadingFrom("--x-", "x-");
    doTestTrimLeadingFrom("--xx", "xx");
    doTestTrimLeadingFrom("-x--", "x--");
    doTestTrimLeadingFrom("-x-x", "x-x");
    doTestTrimLeadingFrom("-xx-", "xx-");
    doTestTrimLeadingFrom("x--x", "x--x");
    doTestTrimLeadingFrom("x-x-", "x-x-");
    doTestTrimLeadingFrom("x-xx", "x-xx");
    doTestTrimLeadingFrom("x-x--xx---x----x", "x-x--xx---x----x");
    // additional testing using the doc example
    assertEquals("catbab", anyOf("ab").trimLeadingFrom("abacatbab"));
  }

  private void doTestTrimLeadingFrom(String in, String out) {
    // Try a few different matchers which all match '-' and not 'x'
    assertEquals(out, is('-').trimLeadingFrom(in));
    assertEquals(out, is('-').or(is('#')).trimLeadingFrom(in));
    assertEquals(out, isNot('x').trimLeadingFrom(in));
    assertEquals(out, is('x').negate().trimLeadingFrom(in));
    assertEquals(out, anyOf("-#").trimLeadingFrom(in));
    assertEquals(out, anyOf("-#123").trimLeadingFrom(in));
  }

  public void testTrimTrailingFrom() {
    // trimming -
    doTestTrimTrailingFrom("-", "");
    doTestTrimTrailingFrom("x-", "x");
    doTestTrimTrailingFrom("-x", "-x");
    doTestTrimTrailingFrom("--", "");
    doTestTrimTrailingFrom("x--", "x");
    doTestTrimTrailingFrom("--x", "--x");
    doTestTrimTrailingFrom("-x-", "-x");
    doTestTrimTrailingFrom("x-x", "x-x");
    doTestTrimTrailingFrom("---", "");
    doTestTrimTrailingFrom("--x-", "--x");
    doTestTrimTrailingFrom("--xx", "--xx");
    doTestTrimTrailingFrom("-x--", "-x");
    doTestTrimTrailingFrom("-x-x", "-x-x");
    doTestTrimTrailingFrom("-xx-", "-xx");
    doTestTrimTrailingFrom("x--x", "x--x");
    doTestTrimTrailingFrom("x-x-", "x-x");
    doTestTrimTrailingFrom("x-xx", "x-xx");
    doTestTrimTrailingFrom("x-x--xx---x----x", "x-x--xx---x----x");
    // additional testing using the doc example
    assertEquals("abacat", anyOf("ab").trimTrailingFrom("abacatbab"));
  }

  private void doTestTrimTrailingFrom(String in, String out) {
    // Try a few different matchers which all match '-' and not 'x'
    assertEquals(out, is('-').trimTrailingFrom(in));
    assertEquals(out, is('-').or(is('#')).trimTrailingFrom(in));
    assertEquals(out, isNot('x').trimTrailingFrom(in));
    assertEquals(out, is('x').negate().trimTrailingFrom(in));
    assertEquals(out, anyOf("-#").trimTrailingFrom(in));
    assertEquals(out, anyOf("-#123").trimTrailingFrom(in));
  }

  public void testTrimAndCollapse() {
    // collapsing groups of '-' into '_' or '-'
    doTestTrimAndCollapse("", "");
    doTestTrimAndCollapse("x", "x");
    doTestTrimAndCollapse("-", "");
    doTestTrimAndCollapse("x-", "x");
    doTestTrimAndCollapse("-x", "x");
    doTestTrimAndCollapse("--", "");
    doTestTrimAndCollapse("x--", "x");
    doTestTrimAndCollapse("--x", "x");
    doTestTrimAndCollapse("-x-", "x");
    doTestTrimAndCollapse("x-x", "x_x");
    doTestTrimAndCollapse("---", "");
    doTestTrimAndCollapse("--x-", "x");
    doTestTrimAndCollapse("--xx", "xx");
    doTestTrimAndCollapse("-x--", "x");
    doTestTrimAndCollapse("-x-x", "x_x");
    doTestTrimAndCollapse("-xx-", "xx");
    doTestTrimAndCollapse("x--x", "x_x");
    doTestTrimAndCollapse("x-x-", "x_x");
    doTestTrimAndCollapse("x-xx", "x_xx");
    doTestTrimAndCollapse("x-x--xx---x----x", "x_x_xx_x_x");
  }

  private void doTestTrimAndCollapse(String in, String out) {
    // Try a few different matchers which all match '-' and not 'x'
    for (char replacement : new char[] {'_', '-'}) {
      String expected = out.replace('_', replacement);
      assertEqualsSame(expected, in, is('-').trimAndCollapseFrom(in, replacement));
      assertEqualsSame(expected, in, is('-').or(is('#')).trimAndCollapseFrom(in, replacement));
      assertEqualsSame(expected, in, isNot('x').trimAndCollapseFrom(in, replacement));
      assertEqualsSame(expected, in, is('x').negate().trimAndCollapseFrom(in, replacement));
      assertEqualsSame(expected, in, anyOf("-").trimAndCollapseFrom(in, replacement));
      assertEqualsSame(expected, in, anyOf("-#").trimAndCollapseFrom(in, replacement));
      assertEqualsSame(expected, in, anyOf("-#123").trimAndCollapseFrom(in, replacement));
    }
  }

  public void testReplaceFrom() {
    assertEquals("yoho", is('a').replaceFrom("yaha", 'o'));
    assertEquals("yh", is('a').replaceFrom("yaha", ""));
    assertEquals("yoho", is('a').replaceFrom("yaha", "o"));
    assertEquals("yoohoo", is('a').replaceFrom("yaha", "oo"));
    assertEquals("12 &gt; 5", is('>').replaceFrom("12 > 5", "&gt;"));
  }

  public void testPrecomputedOptimizations() {
    // These are testing behavior that's never promised by the API.
    // Some matchers are so efficient that it is a waste of effort to
    // build a precomputed version.
    CharMatcher m1 = is('x');
    assertSame(m1, m1.precomputed());
    assertEquals(m1.toString(), m1.precomputed().toString());

    CharMatcher m2 = anyOf("Az");
    assertSame(m2, m2.precomputed());
    assertEquals(m2.toString(), m2.precomputed().toString());

    CharMatcher m3 = inRange('A', 'Z');
    assertSame(m3, m3.precomputed());
    assertEquals(m3.toString(), m3.precomputed().toString());

    assertSame(CharMatcher.none(), CharMatcher.none().precomputed());
    assertSame(CharMatcher.any(), CharMatcher.any().precomputed());
  }

  @GwtIncompatible // java.util.BitSet
  private static BitSet bitSet(String chars) {
    return bitSet(chars.toCharArray());
  }

  @GwtIncompatible // java.util.BitSet
  private static BitSet bitSet(char[] chars) {
    BitSet tmp = new BitSet();
    for (char c : chars) {
      tmp.set(c);
    }
    return tmp;
  }

  @GwtIncompatible // java.util.Random, java.util.BitSet
  public void testSmallCharMatcher() {
    CharMatcher len1 = SmallCharMatcher.from(bitSet("#"), "#");
    CharMatcher len2 = SmallCharMatcher.from(bitSet("ab"), "ab");
    CharMatcher len3 = SmallCharMatcher.from(bitSet("abc"), "abc");
    CharMatcher len4 = SmallCharMatcher.from(bitSet("abcd"), "abcd");
    assertTrue(len1.matches('#'));
    assertFalse(len1.matches('!'));
    assertTrue(len2.matches('a'));
    assertTrue(len2.matches('b'));
    for (char c = 'c'; c < 'z'; c++) {
      assertFalse(len2.matches(c));
    }
    assertTrue(len3.matches('a'));
    assertTrue(len3.matches('b'));
    assertTrue(len3.matches('c'));
    for (char c = 'd'; c < 'z'; c++) {
      assertFalse(len3.matches(c));
    }
    assertTrue(len4.matches('a'));
    assertTrue(len4.matches('b'));
    assertTrue(len4.matches('c'));
    assertTrue(len4.matches('d'));
    for (char c = 'e'; c < 'z'; c++) {
      assertFalse(len4.matches(c));
    }

    Random rand = new Random(1234);
    for (int testCase = 0; testCase < 100; testCase++) {
      char[] chars = randomChars(rand, rand.nextInt(63) + 1);
      CharMatcher m = SmallCharMatcher.from(bitSet(chars), new String(chars));
      checkExactMatches(m, chars);
    }
  }

  static void checkExactMatches(CharMatcher m, char[] chars) {
    Set<Character> positive = Sets.newHashSetWithExpectedSize(chars.length);
    for (char c : chars) {
      positive.add(c);
    }
    for (int c = 0; c <= Character.MAX_VALUE; c++) {
      assertFalse(positive.contains(new Character((char) c)) ^ m.matches((char) c));
    }
  }

  static char[] randomChars(Random rand, int size) {
    Set<Character> chars = new HashSet<>(size);
    for (int i = 0; i < size; i++) {
      char c;
      do {
        c = (char) rand.nextInt(Character.MAX_VALUE - Character.MIN_VALUE + 1);
      } while (chars.contains(c));
      chars.add(c);
    }
    char[] retValue = new char[chars.size()];
    int i = 0;
    for (char c : chars) {
      retValue[i++] = c;
    }
    Arrays.sort(retValue);
    return retValue;
  }

  public void testToString() {
    assertToStringWorks("CharMatcher.none()", CharMatcher.anyOf(""));
    assertToStringWorks("CharMatcher.is('\\u0031')", CharMatcher.anyOf("1"));
    assertToStringWorks("CharMatcher.isNot('\\u0031')", CharMatcher.isNot('1'));
    assertToStringWorks("CharMatcher.anyOf(\"\\u0031\\u0032\")", CharMatcher.anyOf("12"));
    assertToStringWorks("CharMatcher.anyOf(\"\\u0031\\u0032\\u0033\")", CharMatcher.anyOf("321"));
    assertToStringWorks("CharMatcher.inRange('\\u0031', '\\u0033')", CharMatcher.inRange('1', '3'));
  }

  private static void assertToStringWorks(String expected, CharMatcher matcher) {
    assertEquals(expected, matcher.toString());
    assertEquals(expected, matcher.precomputed().toString());
    assertEquals(expected, matcher.negate().negate().toString());
    assertEquals(expected, matcher.negate().precomputed().negate().toString());
    assertEquals(expected, matcher.negate().precomputed().negate().precomputed().toString());
  }
}

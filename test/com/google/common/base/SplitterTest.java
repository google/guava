/*
 * Copyright (C) 2009 Google Inc.
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

import com.google.common.collect.Lists;
import com.google.testing.util.NullPointerTester;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * @author Julien Silland
 */
public class SplitterTest extends TestCase {

  public void testSplitNullString() {
    try {
      Iterable<String> letters = Splitter.on(',').split(null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testCharacterSimpleSplit() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.on(',').split(simple);
    assertContentsInOrder(letters, "a", "b", "c");
  }

  public void testCharacterSimpleSplitWithNoDelimiter() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.on('.').split(simple);
    assertContentsInOrder(letters, "a,b,c");
  }

  public void testCharacterSplitWithDoubleDelimiter() {
    String doubled = "a,,b,c";
    Iterable<String> letters = Splitter.on(',').split(doubled);
    assertContentsInOrder(letters, "a", "", "b", "c");
  }

  public void testCharacterSplitWithDoubleDelimiterAndSpace() {
    String doubled = "a,, b,c";
    Iterable<String> letters = Splitter.on(',').split(doubled);
    assertContentsInOrder(letters, "a", "", " b", "c");
  }

  public void testCharacterSplitWithTrailingDelimiter() {
    String trailing = "a,b,c,";
    Iterable<String> letters = Splitter.on(',').split(trailing);
    assertContentsInOrder(letters, "a", "b", "c", "");
  }

  public void testCharacterSplitWithLeadingDelimiter() {
    String leading = ",a,b,c";
    Iterable<String> letters = Splitter.on(',').split(leading);
    assertContentsInOrder(letters, "", "a", "b", "c");
  }

  public void testCharacterSplitWithMulitpleLetters() {
    Iterable<String> testCharacteringMotto = Splitter.on('-').split(
        "Testing-rocks-Debugging-sucks");
    assertContentsInOrder(testCharacteringMotto,
        "Testing", "rocks", "Debugging", "sucks");
  }

  public void testCharacterSplitWithMatcherDelimiter() {
    Iterable<String> testCharacteringMotto = Splitter
        .on(CharMatcher.WHITESPACE)
        .split("Testing\nrocks\tDebugging sucks");
    assertContentsInOrder(testCharacteringMotto,
        "Testing", "rocks", "Debugging", "sucks");
  }

  public void testCharacterSplitWithDoubleDelimiterOmitEmptyStrings() {
    String doubled = "a..b.c";
    Iterable<String> letters = Splitter.on('.')
        .omitEmptyStrings().split(doubled);
    assertContentsInOrder(letters, "a", "b", "c");
  }

  public void testCharacterSplitEmptyToken() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on('.').trimResults()
        .split(emptyToken);
    assertContentsInOrder(letters, "a", "", "c");
  }

  public void testCharacterSplitEmptyTokenOmitEmptyStrings() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on('.')
        .omitEmptyStrings().trimResults().split(emptyToken);
    assertContentsInOrder(letters, "a", "c");
  }

  public void testCharacterSplitOnEmptyString() {
    Iterable<String> nothing = Splitter.on('.').split("");
    assertContentsInOrder(nothing, "");
  }

  public void testCharacterSplitOnEmptyStringOmitEmptyStrings() {
    assertFalse(
        Splitter.on('.').omitEmptyStrings().split("").iterator().hasNext());
  }

  public void testCharacterSplitOnOnlyDelimiter() {
    Iterable<String> blankblank = Splitter.on('.').split(".");
    assertContentsInOrder(blankblank, "", "");
  }

  public void testCharacterSplitOnOnlyDelimitersOmitEmptyStrings() {
    Iterable<String> empty = Splitter.on('.').omitEmptyStrings().split("...");
    assertContentsInOrder(empty);
  }

  public void testCharacterSplitWithTrim() {
    String jacksons = "arfo(Marlon)aorf, (Michael)orfa, afro(Jackie)orfa, "
        + "ofar(Jemaine), aff(Tito)";
    Iterable<String> family = Splitter.on(',')
        .trimResults(CharMatcher.anyOf("afro").or(CharMatcher.WHITESPACE))
        .split(jacksons);
    assertContentsInOrder(family,
        "(Marlon)", "(Michael)", "(Jackie)", "(Jemaine)", "(Tito)");
  }

  public void testStringSimpleSplit() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.on(",").split(simple);
    assertContentsInOrder(letters, "a", "b", "c");
  }

  public void testStringSimpleSplitWithNoDelimiter() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.on(".").split(simple);
    assertContentsInOrder(letters, "a,b,c");
  }

  public void testStringSplitWithDoubleDelimiter() {
    String doubled = "a,,b,c";
    Iterable<String> letters = Splitter.on(",").split(doubled);
    assertContentsInOrder(letters, "a", "", "b", "c");
  }

  public void testStringSplitWithDoubleDelimiterAndSpace() {
    String doubled = "a,, b,c";
    Iterable<String> letters = Splitter.on(",").split(doubled);
    assertContentsInOrder(letters, "a", "", " b", "c");
  }

  public void testStringSplitWithTrailingDelimiter() {
    String trailing = "a,b,c,";
    Iterable<String> letters = Splitter.on(",").split(trailing);
    assertContentsInOrder(letters, "a", "b", "c", "");
  }

  public void testStringSplitWithLeadingDelimiter() {
    String leading = ",a,b,c";
    Iterable<String> letters = Splitter.on(",").split(leading);
    assertContentsInOrder(letters, "", "a", "b", "c");
  }

  public void testStringSplitWithMultipleLetters() {
    Iterable<String> testStringingMotto = Splitter.on("-").split(
        "Testing-rocks-Debugging-sucks");
    assertContentsInOrder(testStringingMotto,
        "Testing", "rocks", "Debugging", "sucks");
  }

  public void testStringSplitWithDoubleDelimiterOmitEmptyStrings() {
    String doubled = "a..b.c";
    Iterable<String> letters = Splitter.on(".")
        .omitEmptyStrings().split(doubled);
    assertContentsInOrder(letters, "a", "b", "c");
  }

  public void testStringSplitEmptyToken() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on(".").trimResults()
        .split(emptyToken);
    assertContentsInOrder(letters, "a", "", "c");
  }

  public void testStringSplitEmptyTokenOmitEmptyStrings() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on(".")
        .omitEmptyStrings().trimResults().split(emptyToken);
    assertContentsInOrder(letters, "a", "c");
  }

  public void testStringSplitWithLongDelimiter() {
    String longDelimiter = "a, b, c";
    Iterable<String> letters = Splitter.on(", ").split(longDelimiter);
    assertContentsInOrder(letters, "a", "b", "c");
  }

  public void testStringSplitWithLongLeadingDelimiter() {
    String longDelimiter = ", a, b, c";
    Iterable<String> letters = Splitter.on(", ").split(longDelimiter);
    assertContentsInOrder(letters, "", "a", "b", "c");
  }

  public void testStringSplitWithLongTrailingDelimiter() {
    String longDelimiter = "a, b, c, ";
    Iterable<String> letters = Splitter.on(", ").split(longDelimiter);
    assertContentsInOrder(letters, "a", "b", "c", "");
  }

  public void testStringSplitWithDelimiterSubstringInValue() {
    String fourCommasAndFourSpaces = ",,,,    ";
    Iterable<String> threeCommasThenTreeSpaces = Splitter.on(", ").split(
        fourCommasAndFourSpaces);
    assertContentsInOrder(threeCommasThenTreeSpaces, ",,,", "   ");
  }

  public void testStringSplitWithEmptyString() {
    try {
      Splitter.on("");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testStringSplitOnEmptyString() {
    Iterable<String> notMuch = Splitter.on(".").split("");
    assertContentsInOrder(notMuch, "");
  }

  public void testStringSplitOnEmptyStringOmitEmptyString() {
    assertFalse(
        Splitter.on(".").omitEmptyStrings().split("").iterator().hasNext());
  }

  public void testStringSplitOnOnlyDelimiter() {
    Iterable<String> blankblank = Splitter.on(".").split(".");
    assertContentsInOrder(blankblank, "", "");
  }

  public void testStringSplitOnOnlyDelimitersOmitEmptyStrings() {
    Iterable<String> empty = Splitter.on(".").omitEmptyStrings().split("...");
    assertContentsInOrder(empty);
  }

  public void testStringSplitWithTrim() {
    String jacksons = "arfo(Marlon)aorf, (Michael)orfa, afro(Jackie)orfa, "
        + "ofar(Jemaine), aff(Tito)";
    Iterable<String> family = Splitter.on(",")
        .trimResults(CharMatcher.anyOf("afro").or(CharMatcher.WHITESPACE))
        .split(jacksons);
    assertContentsInOrder(family,
        "(Marlon)", "(Michael)", "(Jackie)", "(Jemaine)", "(Tito)");
  }

  public void testPatternSimpleSplit() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.onPattern(",").split(simple);
    assertContentsInOrder(letters, "a", "b", "c");
  }

  public void testPatternSimpleSplitWithNoDelimiter() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.onPattern("foo").split(simple);
    assertContentsInOrder(letters, "a,b,c");
  }

  public void testPatternSplitWithDoubleDelimiter() {
    String doubled = "a,,b,c";
    Iterable<String> letters = Splitter.onPattern(",").split(doubled);
    assertContentsInOrder(letters, "a", "", "b", "c");
  }

  public void testPatternSplitWithDoubleDelimiterAndSpace() {
    String doubled = "a,, b,c";
    Iterable<String> letters = Splitter.onPattern(",").split(doubled);
    assertContentsInOrder(letters, "a", "", " b", "c");
  }

  public void testPatternSplitWithTrailingDelimiter() {
    String trailing = "a,b,c,";
    Iterable<String> letters = Splitter.onPattern(",").split(trailing);
    assertContentsInOrder(letters, "a", "b", "c", "");
  }

  public void testPatternSplitWithLeadingDelimiter() {
    String leading = ",a,b,c";
    Iterable<String> letters = Splitter.onPattern(",").split(leading);
    assertContentsInOrder(letters, "", "a", "b", "c");
  }

  public void testPatternSplitWithMultipleLetters() {
    Iterable<String> testPatterningMotto = Splitter.onPattern("-").split(
        "Testing-rocks-Debugging-sucks");
    assertContentsInOrder(testPatterningMotto,
        "Testing", "rocks", "Debugging", "sucks");
  }

  private static final Pattern LITERAL_DOT_PATTERN = Pattern.compile("\\.");

  public void testPatternSplitWithDoubleDelimiterOmitEmptyStrings() {
    String doubled = "a..b.c";
    Iterable<String> letters = Splitter.on(LITERAL_DOT_PATTERN)
        .omitEmptyStrings().split(doubled);
    assertContentsInOrder(letters, "a", "b", "c");
  }

  public void testPatternSplitEmptyToken() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on(LITERAL_DOT_PATTERN).trimResults()
        .split(emptyToken);
    assertContentsInOrder(letters, "a", "", "c");
  }

  public void testPatternSplitEmptyTokenOmitEmptyStrings() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on(LITERAL_DOT_PATTERN)
        .omitEmptyStrings().trimResults().split(emptyToken);
    assertContentsInOrder(letters, "a", "c");
  }

  public void testPatternSplitOnOnlyDelimiter() {
    Iterable<String> blankblank = Splitter.on(LITERAL_DOT_PATTERN).split(".");
    assertContentsInOrder(blankblank, "", "");
  }

  public void testPatternSplitOnOnlyDelimitersOmitEmptyStrings() {
    Iterable<String> empty = Splitter.on(LITERAL_DOT_PATTERN).omitEmptyStrings()
        .split("...");
    assertContentsInOrder(empty);
  }

  public void testPatternSplitMatchingIsGreedy() {
    String longDelimiter = "a, b,   c";
    Iterable<String> letters = Splitter.on(Pattern.compile(",\\s*"))
        .split(longDelimiter);
    assertContentsInOrder(letters, "a", "b", "c");
  }

  public void testPatternSplitWithLongLeadingDelimiter() {
    String longDelimiter = ", a, b, c";
    Iterable<String> letters = Splitter.on(Pattern.compile(", "))
        .split(longDelimiter);
    assertContentsInOrder(letters, "", "a", "b", "c");
  }

  public void testPatternSplitWithLongTrailingDelimiter() {
    String longDelimiter = "a, b, c/ ";
    Iterable<String> letters = Splitter.on(Pattern.compile("[,/]\\s"))
        .split(longDelimiter);
    assertContentsInOrder(letters, "a", "b", "c", "");
  }

  public void testPatternSplitInvalidPattern() {
    try {
      Splitter.on(Pattern.compile("a*"));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testPatternSplitWithTrim() {
    String jacksons = "arfo(Marlon)aorf, (Michael)orfa, afro(Jackie)orfa, "
        + "ofar(Jemaine), aff(Tito)";
    Iterable<String> family = Splitter.on(Pattern.compile(","))
        .trimResults(CharMatcher.anyOf("afro").or(CharMatcher.WHITESPACE))
        .split(jacksons);
    assertContentsInOrder(family,
        "(Marlon)", "(Michael)", "(Jackie)", "(Jemaine)", "(Tito)");
  }

  public void testSplitterIterableIsUnmodifiable() {
    assertIteratorIsUnmodifiable(Splitter.on(',').split("a,b").iterator());
    assertIteratorIsUnmodifiable(Splitter.on(",").split("a,b").iterator());
    assertIteratorIsUnmodifiable(
        Splitter.on(Pattern.compile(",")).split("a,b").iterator());
  }
  
  private void assertIteratorIsUnmodifiable(Iterator<?> iterator) {
    iterator.next();
    try {
      iterator.remove();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testSplitterIterableIsLazy() {
    assertSplitterIterableIsLazy(Splitter.on(','));
    assertSplitterIterableIsLazy(Splitter.on(","));
    assertSplitterIterableIsLazy(Splitter.on(Pattern.compile(",")));
  }

  /**
   * This test really pushes the boundaries of what we support. In general the
   * splitter's behaviour is not well defined if the char sequence it's
   * splitting is mutated during iteration.
   */
  private void assertSplitterIterableIsLazy(Splitter splitter) {
    StringBuilder builder = new StringBuilder();
    Iterator<String> iterator = splitter.split(builder).iterator();

    builder.append("A,");
    assertEquals("A", iterator.next());
    builder.append("B,");
    assertEquals("B", iterator.next());
    builder.append("C");
    assertEquals("C", iterator.next());
    assertFalse(iterator.hasNext());
  }

  public void testAtEachSimpleSplit() {
    String simple = "abcde";
    Iterable<String> letters = Splitter.fixedLength(2).split(simple);
    assertContentsInOrder(letters, "ab", "cd", "e");
  }

  public void testAtEachSplitEqualChunkLength() {
    String simple = "abcdef";
    Iterable<String> letters = Splitter.fixedLength(2).split(simple);
    assertContentsInOrder(letters, "ab", "cd", "ef");
  }

  public void testAtEachSplitOnlyOneChunk() {
    String simple = "abc";
    Iterable<String> letters = Splitter.fixedLength(3).split(simple);
    assertContentsInOrder(letters, "abc");
  }

  public void testAtEachSplitSmallerString() {
    String simple = "ab";
    Iterable<String> letters = Splitter.fixedLength(3).split(simple);
    assertContentsInOrder(letters, "ab");
  }

  public void testAtEachSplitEmptyString() {
    String simple = "";
    Iterable<String> letters = Splitter.fixedLength(3).split(simple);
    assertContentsInOrder(letters, "");
  }

  public void testAtEachSplitEmptyStringWithOmitEmptyStrings() {
    assertFalse(Splitter.fixedLength(3).omitEmptyStrings().split("").iterator()
        .hasNext());
  }

  public void testAtEachSplitIntoChars() {
    String simple = "abcd";
    Iterable<String> letters = Splitter.fixedLength(1).split(simple);
    assertContentsInOrder(letters, "a", "b", "c", "d");
  }

  public void testAtEachSplitZeroChunkLen() {
    try {
      Splitter.fixedLength(0);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testAtEachSplitNegativeChunkLen() {
    try {
      Splitter.fixedLength(-1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testNullPointers() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Splitter.class);
    tester.testAllPublicInstanceMethods(Splitter.on(","));
    tester.testAllPublicInstanceMethods(Splitter.on(",").trimResults());
  }

  // TODO: use common one when we settle where that is...
  private void assertContentsInOrder(
      Iterable<String> actual, String... expected) {
    assertEquals(Arrays.asList(expected), Lists.newArrayList(actual));
  }
}

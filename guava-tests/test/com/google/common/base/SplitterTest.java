/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Splitter.MapSplitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.testing.NullPointerTester;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import junit.framework.TestCase;

/** @author Julien Silland */
@GwtCompatible(emulated = true)
public class SplitterTest extends TestCase {

  private static final Splitter COMMA_SPLITTER = Splitter.on(',');

  public void testSplitNullString() {
    try {
      COMMA_SPLITTER.split(null);
      fail();
    } catch (NullPointerException expected) {
    }
  }

  public void testCharacterSimpleSplit() {
    String simple = "a,b,c";
    Iterable<String> letters = COMMA_SPLITTER.split(simple);
    assertThat(letters).containsExactly("a", "b", "c").inOrder();
  }

  /**
   * All of the infrastructure of split and splitToString is identical, so we do one test of
   * splitToString. All other cases should be covered by testing of split.
   *
   * <p>TODO(user): It would be good to make all the relevant tests run on both split and
   * splitToString automatically.
   */
  public void testCharacterSimpleSplitToList() {
    String simple = "a,b,c";
    List<String> letters = COMMA_SPLITTER.splitToList(simple);
    assertThat(letters).containsExactly("a", "b", "c").inOrder();
  }

  public void testCharacterSimpleSplitToStream() {
    String simple = "a,b,c";
    List<String> letters = COMMA_SPLITTER.splitToStream(simple).collect(toImmutableList());
    assertThat(letters).containsExactly("a", "b", "c").inOrder();
  }

  public void testToString() {
    assertEquals("[]", COMMA_SPLITTER.split("").toString());
    assertEquals("[a, b, c]", COMMA_SPLITTER.split("a,b,c").toString());
    assertEquals("[yam, bam, jam, ham]", Splitter.on(", ").split("yam, bam, jam, ham").toString());
  }

  public void testCharacterSimpleSplitWithNoDelimiter() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.on('.').split(simple);
    assertThat(letters).containsExactly("a,b,c").inOrder();
  }

  public void testCharacterSplitWithDoubleDelimiter() {
    String doubled = "a,,b,c";
    Iterable<String> letters = COMMA_SPLITTER.split(doubled);
    assertThat(letters).containsExactly("a", "", "b", "c").inOrder();
  }

  public void testCharacterSplitWithDoubleDelimiterAndSpace() {
    String doubled = "a,, b,c";
    Iterable<String> letters = COMMA_SPLITTER.split(doubled);
    assertThat(letters).containsExactly("a", "", " b", "c").inOrder();
  }

  public void testCharacterSplitWithTrailingDelimiter() {
    String trailing = "a,b,c,";
    Iterable<String> letters = COMMA_SPLITTER.split(trailing);
    assertThat(letters).containsExactly("a", "b", "c", "").inOrder();
  }

  public void testCharacterSplitWithLeadingDelimiter() {
    String leading = ",a,b,c";
    Iterable<String> letters = COMMA_SPLITTER.split(leading);
    assertThat(letters).containsExactly("", "a", "b", "c").inOrder();
  }

  public void testCharacterSplitWithMultipleLetters() {
    Iterable<String> testCharacteringMotto =
        Splitter.on('-').split("Testing-rocks-Debugging-sucks");
    assertThat(testCharacteringMotto)
        .containsExactly("Testing", "rocks", "Debugging", "sucks")
        .inOrder();
  }

  public void testCharacterSplitWithMatcherDelimiter() {
    Iterable<String> testCharacteringMotto =
        Splitter.on(CharMatcher.whitespace()).split("Testing\nrocks\tDebugging sucks");
    assertThat(testCharacteringMotto)
        .containsExactly("Testing", "rocks", "Debugging", "sucks")
        .inOrder();
  }

  public void testCharacterSplitWithDoubleDelimiterOmitEmptyStrings() {
    String doubled = "a..b.c";
    Iterable<String> letters = Splitter.on('.').omitEmptyStrings().split(doubled);
    assertThat(letters).containsExactly("a", "b", "c").inOrder();
  }

  public void testCharacterSplitEmptyToken() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on('.').trimResults().split(emptyToken);
    assertThat(letters).containsExactly("a", "", "c").inOrder();
  }

  public void testCharacterSplitEmptyTokenOmitEmptyStrings() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on('.').omitEmptyStrings().trimResults().split(emptyToken);
    assertThat(letters).containsExactly("a", "c").inOrder();
  }

  public void testCharacterSplitOnEmptyString() {
    Iterable<String> nothing = Splitter.on('.').split("");
    assertThat(nothing).containsExactly("").inOrder();
  }

  public void testCharacterSplitOnEmptyStringOmitEmptyStrings() {
    assertThat(Splitter.on('.').omitEmptyStrings().split("")).isEmpty();
  }

  public void testCharacterSplitOnOnlyDelimiter() {
    Iterable<String> blankblank = Splitter.on('.').split(".");
    assertThat(blankblank).containsExactly("", "").inOrder();
  }

  public void testCharacterSplitOnOnlyDelimitersOmitEmptyStrings() {
    Iterable<String> empty = Splitter.on('.').omitEmptyStrings().split("...");
    assertThat(empty).isEmpty();
  }

  public void testCharacterSplitWithTrim() {
    String jacksons =
        "arfo(Marlon)aorf, (Michael)orfa, afro(Jackie)orfa, " + "ofar(Jemaine), aff(Tito)";
    Iterable<String> family =
        COMMA_SPLITTER
            .trimResults(CharMatcher.anyOf("afro").or(CharMatcher.whitespace()))
            .split(jacksons);
    assertThat(family)
        .containsExactly("(Marlon)", "(Michael)", "(Jackie)", "(Jemaine)", "(Tito)")
        .inOrder();
  }

  public void testStringSimpleSplit() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.on(",").split(simple);
    assertThat(letters).containsExactly("a", "b", "c").inOrder();
  }

  public void testStringSimpleSplitWithNoDelimiter() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.on(".").split(simple);
    assertThat(letters).containsExactly("a,b,c").inOrder();
  }

  public void testStringSplitWithDoubleDelimiter() {
    String doubled = "a,,b,c";
    Iterable<String> letters = Splitter.on(",").split(doubled);
    assertThat(letters).containsExactly("a", "", "b", "c").inOrder();
  }

  public void testStringSplitWithDoubleDelimiterAndSpace() {
    String doubled = "a,, b,c";
    Iterable<String> letters = Splitter.on(",").split(doubled);
    assertThat(letters).containsExactly("a", "", " b", "c").inOrder();
  }

  public void testStringSplitWithTrailingDelimiter() {
    String trailing = "a,b,c,";
    Iterable<String> letters = Splitter.on(",").split(trailing);
    assertThat(letters).containsExactly("a", "b", "c", "").inOrder();
  }

  public void testStringSplitWithLeadingDelimiter() {
    String leading = ",a,b,c";
    Iterable<String> letters = Splitter.on(",").split(leading);
    assertThat(letters).containsExactly("", "a", "b", "c").inOrder();
  }

  public void testStringSplitWithMultipleLetters() {
    Iterable<String> testStringingMotto = Splitter.on("-").split("Testing-rocks-Debugging-sucks");
    assertThat(testStringingMotto)
        .containsExactly("Testing", "rocks", "Debugging", "sucks")
        .inOrder();
  }

  public void testStringSplitWithDoubleDelimiterOmitEmptyStrings() {
    String doubled = "a..b.c";
    Iterable<String> letters = Splitter.on(".").omitEmptyStrings().split(doubled);
    assertThat(letters).containsExactly("a", "b", "c").inOrder();
  }

  public void testStringSplitEmptyToken() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on(".").trimResults().split(emptyToken);
    assertThat(letters).containsExactly("a", "", "c").inOrder();
  }

  public void testStringSplitEmptyTokenOmitEmptyStrings() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on(".").omitEmptyStrings().trimResults().split(emptyToken);
    assertThat(letters).containsExactly("a", "c").inOrder();
  }

  public void testStringSplitWithLongDelimiter() {
    String longDelimiter = "a, b, c";
    Iterable<String> letters = Splitter.on(", ").split(longDelimiter);
    assertThat(letters).containsExactly("a", "b", "c").inOrder();
  }

  public void testStringSplitWithLongLeadingDelimiter() {
    String longDelimiter = ", a, b, c";
    Iterable<String> letters = Splitter.on(", ").split(longDelimiter);
    assertThat(letters).containsExactly("", "a", "b", "c").inOrder();
  }

  public void testStringSplitWithLongTrailingDelimiter() {
    String longDelimiter = "a, b, c, ";
    Iterable<String> letters = Splitter.on(", ").split(longDelimiter);
    assertThat(letters).containsExactly("a", "b", "c", "").inOrder();
  }

  public void testStringSplitWithDelimiterSubstringInValue() {
    String fourCommasAndFourSpaces = ",,,,    ";
    Iterable<String> threeCommasThenThreeSpaces = Splitter.on(", ").split(fourCommasAndFourSpaces);
    assertThat(threeCommasThenThreeSpaces).containsExactly(",,,", "   ").inOrder();
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
    assertThat(notMuch).containsExactly("").inOrder();
  }

  public void testStringSplitOnEmptyStringOmitEmptyString() {
    assertThat(Splitter.on(".").omitEmptyStrings().split("")).isEmpty();
  }

  public void testStringSplitOnOnlyDelimiter() {
    Iterable<String> blankblank = Splitter.on(".").split(".");
    assertThat(blankblank).containsExactly("", "").inOrder();
  }

  public void testStringSplitOnOnlyDelimitersOmitEmptyStrings() {
    Iterable<String> empty = Splitter.on(".").omitEmptyStrings().split("...");
    assertThat(empty).isEmpty();
  }

  public void testStringSplitWithTrim() {
    String jacksons =
        "arfo(Marlon)aorf, (Michael)orfa, afro(Jackie)orfa, " + "ofar(Jemaine), aff(Tito)";
    Iterable<String> family =
        Splitter.on(",")
            .trimResults(CharMatcher.anyOf("afro").or(CharMatcher.whitespace()))
            .split(jacksons);
    assertThat(family)
        .containsExactly("(Marlon)", "(Michael)", "(Jackie)", "(Jemaine)", "(Tito)")
        .inOrder();
  }

  @GwtIncompatible // Splitter.onPattern
  public void testPatternSimpleSplit() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.onPattern(",").split(simple);
    assertThat(letters).containsExactly("a", "b", "c").inOrder();
  }

  @GwtIncompatible // Splitter.onPattern
  public void testPatternSimpleSplitWithNoDelimiter() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.onPattern("foo").split(simple);
    assertThat(letters).containsExactly("a,b,c").inOrder();
  }

  @GwtIncompatible // Splitter.onPattern
  public void testPatternSplitWithDoubleDelimiter() {
    String doubled = "a,,b,c";
    Iterable<String> letters = Splitter.onPattern(",").split(doubled);
    assertThat(letters).containsExactly("a", "", "b", "c").inOrder();
  }

  @GwtIncompatible // Splitter.onPattern
  public void testPatternSplitWithDoubleDelimiterAndSpace() {
    String doubled = "a,, b,c";
    Iterable<String> letters = Splitter.onPattern(",").split(doubled);
    assertThat(letters).containsExactly("a", "", " b", "c").inOrder();
  }

  @GwtIncompatible // Splitter.onPattern
  public void testPatternSplitWithTrailingDelimiter() {
    String trailing = "a,b,c,";
    Iterable<String> letters = Splitter.onPattern(",").split(trailing);
    assertThat(letters).containsExactly("a", "b", "c", "").inOrder();
  }

  @GwtIncompatible // Splitter.onPattern
  public void testPatternSplitWithLeadingDelimiter() {
    String leading = ",a,b,c";
    Iterable<String> letters = Splitter.onPattern(",").split(leading);
    assertThat(letters).containsExactly("", "a", "b", "c").inOrder();
  }

  // TODO(kevinb): the name of this method suggests it might not actually be testing what it
  // intends to be testing?
  @GwtIncompatible // Splitter.onPattern
  public void testPatternSplitWithMultipleLetters() {
    Iterable<String> testPatterningMotto =
        Splitter.onPattern("-").split("Testing-rocks-Debugging-sucks");
    assertThat(testPatterningMotto)
        .containsExactly("Testing", "rocks", "Debugging", "sucks")
        .inOrder();
  }

  @GwtIncompatible // java.util.regex.Pattern
  private static Pattern literalDotPattern() {
    return Pattern.compile("\\.");
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testPatternSplitWithDoubleDelimiterOmitEmptyStrings() {
    String doubled = "a..b.c";
    Iterable<String> letters = Splitter.on(literalDotPattern()).omitEmptyStrings().split(doubled);
    assertThat(letters).containsExactly("a", "b", "c").inOrder();
  }

  @GwtIncompatible // java.util.regex.Pattern
  @AndroidIncompatible // Bug in older versions of Android we test against, since fixed.
  public void testPatternSplitLookBehind() {
    if (!CommonPattern.isPcreLike()) {
      return;
    }
    String toSplit = ":foo::barbaz:";
    String regexPattern = "(?<=:)";
    Iterable<String> split = Splitter.onPattern(regexPattern).split(toSplit);
    assertThat(split).containsExactly(":", "foo:", ":", "barbaz:").inOrder();
    // splits into chunks ending in :
  }

  @GwtIncompatible // java.util.regex.Pattern
  @AndroidIncompatible // Bug in older versions of Android we test against, since fixed.
  public void testPatternSplitWordBoundary() {
    String string = "foo<bar>bletch";
    Iterable<String> words = Splitter.on(Pattern.compile("\\b")).split(string);
    assertThat(words).containsExactly("foo", "<", "bar", ">", "bletch").inOrder();
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testPatternSplitWordBoundary_singleCharInput() {
    String string = "f";
    Iterable<String> words = Splitter.on(Pattern.compile("\\b")).split(string);
    assertThat(words).containsExactly("f").inOrder();
  }

  @AndroidIncompatible // Apparently Gingerbread's regex API is buggy.
  @GwtIncompatible // java.util.regex.Pattern
  public void testPatternSplitWordBoundary_singleWordInput() {
    String string = "foo";
    Iterable<String> words = Splitter.on(Pattern.compile("\\b")).split(string);
    assertThat(words).containsExactly("foo").inOrder();
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testPatternSplitEmptyToken() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on(literalDotPattern()).trimResults().split(emptyToken);
    assertThat(letters).containsExactly("a", "", "c").inOrder();
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testPatternSplitEmptyTokenOmitEmptyStrings() {
    String emptyToken = "a. .c";
    Iterable<String> letters =
        Splitter.on(literalDotPattern()).omitEmptyStrings().trimResults().split(emptyToken);
    assertThat(letters).containsExactly("a", "c").inOrder();
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testPatternSplitOnOnlyDelimiter() {
    Iterable<String> blankblank = Splitter.on(literalDotPattern()).split(".");

    assertThat(blankblank).containsExactly("", "").inOrder();
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testPatternSplitOnOnlyDelimitersOmitEmptyStrings() {
    Iterable<String> empty = Splitter.on(literalDotPattern()).omitEmptyStrings().split("...");
    assertThat(empty).isEmpty();
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testPatternSplitMatchingIsGreedy() {
    String longDelimiter = "a, b,   c";
    Iterable<String> letters = Splitter.on(Pattern.compile(",\\s*")).split(longDelimiter);
    assertThat(letters).containsExactly("a", "b", "c").inOrder();
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testPatternSplitWithLongLeadingDelimiter() {
    String longDelimiter = ", a, b, c";
    Iterable<String> letters = Splitter.on(Pattern.compile(", ")).split(longDelimiter);
    assertThat(letters).containsExactly("", "a", "b", "c").inOrder();
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testPatternSplitWithLongTrailingDelimiter() {
    String longDelimiter = "a, b, c/ ";
    Iterable<String> letters = Splitter.on(Pattern.compile("[,/]\\s")).split(longDelimiter);
    assertThat(letters).containsExactly("a", "b", "c", "").inOrder();
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testPatternSplitInvalidPattern() {
    try {
      Splitter.on(Pattern.compile("a*"));
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testPatternSplitWithTrim() {
    String jacksons =
        "arfo(Marlon)aorf, (Michael)orfa, afro(Jackie)orfa, " + "ofar(Jemaine), aff(Tito)";
    Iterable<String> family =
        Splitter.on(Pattern.compile(","))
            .trimResults(CharMatcher.anyOf("afro").or(CharMatcher.whitespace()))
            .split(jacksons);
    assertThat(family)
        .containsExactly("(Marlon)", "(Michael)", "(Jackie)", "(Jemaine)", "(Tito)")
        .inOrder();
  }

  public void testSplitterIterableIsUnmodifiable_char() {
    assertIteratorIsUnmodifiable(COMMA_SPLITTER.split("a,b").iterator());
  }

  public void testSplitterIterableIsUnmodifiable_string() {
    assertIteratorIsUnmodifiable(Splitter.on(",").split("a,b").iterator());
  }

  @GwtIncompatible // java.util.regex.Pattern
  public void testSplitterIterableIsUnmodifiable_pattern() {
    assertIteratorIsUnmodifiable(Splitter.on(Pattern.compile(",")).split("a,b").iterator());
  }

  private void assertIteratorIsUnmodifiable(Iterator<?> iterator) {
    iterator.next();
    try {
      iterator.remove();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testSplitterIterableIsLazy_char() {
    assertSplitterIterableIsLazy(COMMA_SPLITTER);
  }

  public void testSplitterIterableIsLazy_string() {
    assertSplitterIterableIsLazy(Splitter.on(","));
  }

  @GwtIncompatible // java.util.regex.Pattern
  @AndroidIncompatible // not clear that j.u.r.Matcher promises to handle mutations during use
  public void testSplitterIterableIsLazy_pattern() {
    if (!CommonPattern.isPcreLike()) {
      return;
    }
    assertSplitterIterableIsLazy(Splitter.onPattern(","));
  }

  /**
   * This test really pushes the boundaries of what we support. In general the splitter's behaviour
   * is not well defined if the char sequence it's splitting is mutated during iteration.
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

  public void testFixedLengthSimpleSplit() {
    String simple = "abcde";
    Iterable<String> letters = Splitter.fixedLength(2).split(simple);
    assertThat(letters).containsExactly("ab", "cd", "e").inOrder();
  }

  public void testFixedLengthSplitEqualChunkLength() {
    String simple = "abcdef";
    Iterable<String> letters = Splitter.fixedLength(2).split(simple);
    assertThat(letters).containsExactly("ab", "cd", "ef").inOrder();
  }

  public void testFixedLengthSplitOnlyOneChunk() {
    String simple = "abc";
    Iterable<String> letters = Splitter.fixedLength(3).split(simple);
    assertThat(letters).containsExactly("abc").inOrder();
  }

  public void testFixedLengthSplitSmallerString() {
    String simple = "ab";
    Iterable<String> letters = Splitter.fixedLength(3).split(simple);
    assertThat(letters).containsExactly("ab").inOrder();
  }

  public void testFixedLengthSplitEmptyString() {
    String simple = "";
    Iterable<String> letters = Splitter.fixedLength(3).split(simple);
    assertThat(letters).containsExactly("").inOrder();
  }

  public void testFixedLengthSplitEmptyStringWithOmitEmptyStrings() {
    assertThat(Splitter.fixedLength(3).omitEmptyStrings().split("")).isEmpty();
  }

  public void testFixedLengthSplitIntoChars() {
    String simple = "abcd";
    Iterable<String> letters = Splitter.fixedLength(1).split(simple);
    assertThat(letters).containsExactly("a", "b", "c", "d").inOrder();
  }

  public void testFixedLengthSplitZeroChunkLen() {
    try {
      Splitter.fixedLength(0);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testFixedLengthSplitNegativeChunkLen() {
    try {
      Splitter.fixedLength(-1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testLimitLarge() {
    String simple = "abcd";
    Iterable<String> letters = Splitter.fixedLength(1).limit(100).split(simple);
    assertThat(letters).containsExactly("a", "b", "c", "d").inOrder();
  }

  public void testLimitOne() {
    String simple = "abcd";
    Iterable<String> letters = Splitter.fixedLength(1).limit(1).split(simple);
    assertThat(letters).containsExactly("abcd").inOrder();
  }

  public void testLimitFixedLength() {
    String simple = "abcd";
    Iterable<String> letters = Splitter.fixedLength(1).limit(2).split(simple);
    assertThat(letters).containsExactly("a", "bcd").inOrder();
  }

  public void testLimit1Separator() {
    String simple = "a,b,c,d";
    Iterable<String> items = COMMA_SPLITTER.limit(1).split(simple);
    assertThat(items).containsExactly("a,b,c,d").inOrder();
  }

  public void testLimitSeparator() {
    String simple = "a,b,c,d";
    Iterable<String> items = COMMA_SPLITTER.limit(2).split(simple);
    assertThat(items).containsExactly("a", "b,c,d").inOrder();
  }

  public void testLimitExtraSeparators() {
    String text = "a,,,b,,c,d";
    Iterable<String> items = COMMA_SPLITTER.limit(2).split(text);
    assertThat(items).containsExactly("a", ",,b,,c,d").inOrder();
  }

  public void testLimitExtraSeparatorsOmitEmpty() {
    String text = "a,,,b,,c,d";
    Iterable<String> items = COMMA_SPLITTER.limit(2).omitEmptyStrings().split(text);
    assertThat(items).containsExactly("a", "b,,c,d").inOrder();
  }

  public void testLimitExtraSeparatorsOmitEmpty3() {
    String text = "a,,,b,,c,d";
    Iterable<String> items = COMMA_SPLITTER.limit(3).omitEmptyStrings().split(text);
    assertThat(items).containsExactly("a", "b", "c,d").inOrder();
  }

  public void testLimitExtraSeparatorsTrim() {
    String text = ",,a,,  , b ,, c,d ";
    Iterable<String> items = COMMA_SPLITTER.limit(2).omitEmptyStrings().trimResults().split(text);
    assertThat(items).containsExactly("a", "b ,, c,d").inOrder();
  }

  public void testLimitExtraSeparatorsTrim3() {
    String text = ",,a,,  , b ,, c,d ";
    Iterable<String> items = COMMA_SPLITTER.limit(3).omitEmptyStrings().trimResults().split(text);
    assertThat(items).containsExactly("a", "b", "c,d").inOrder();
  }

  public void testLimitExtraSeparatorsTrim1() {
    String text = ",,a,,  , b ,, c,d ";
    Iterable<String> items = COMMA_SPLITTER.limit(1).omitEmptyStrings().trimResults().split(text);
    assertThat(items).containsExactly("a,,  , b ,, c,d").inOrder();
  }

  public void testLimitExtraSeparatorsTrim1NoOmit() {
    String text = ",,a,,  , b ,, c,d ";
    Iterable<String> items = COMMA_SPLITTER.limit(1).trimResults().split(text);
    assertThat(items).containsExactly(",,a,,  , b ,, c,d").inOrder();
  }

  public void testLimitExtraSeparatorsTrim1Empty() {
    String text = "";
    Iterable<String> items = COMMA_SPLITTER.limit(1).split(text);
    assertThat(items).containsExactly("").inOrder();
  }

  public void testLimitExtraSeparatorsTrim1EmptyOmit() {
    String text = "";
    Iterable<String> items = COMMA_SPLITTER.omitEmptyStrings().limit(1).split(text);
    assertThat(items).isEmpty();
  }

  public void testInvalidZeroLimit() {
    try {
      COMMA_SPLITTER.limit(0);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @GwtIncompatible // NullPointerTester
  public void testNullPointers() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicStaticMethods(Splitter.class);
    tester.testAllPublicInstanceMethods(COMMA_SPLITTER);
    tester.testAllPublicInstanceMethods(COMMA_SPLITTER.trimResults());
  }

  public void testMapSplitter_trimmedBoth() {
    Map<String, String> m =
        COMMA_SPLITTER
            .trimResults()
            .withKeyValueSeparator(Splitter.on(':').trimResults())
            .split("boy  : tom , girl: tina , cat  : kitty , dog: tommy ");
    ImmutableMap<String, String> expected =
        ImmutableMap.of("boy", "tom", "girl", "tina", "cat", "kitty", "dog", "tommy");
    assertThat(m).isEqualTo(expected);
    assertThat(m.entrySet()).containsExactlyElementsIn(expected.entrySet()).inOrder();
  }

  public void testMapSplitter_trimmedEntries() {
    Map<String, String> m =
        COMMA_SPLITTER
            .trimResults()
            .withKeyValueSeparator(":")
            .split("boy  : tom , girl: tina , cat  : kitty , dog: tommy ");
    ImmutableMap<String, String> expected =
        ImmutableMap.of("boy  ", " tom", "girl", " tina", "cat  ", " kitty", "dog", " tommy");

    assertThat(m).isEqualTo(expected);
    assertThat(m.entrySet()).containsExactlyElementsIn(expected.entrySet()).inOrder();
  }

  public void testMapSplitter_trimmedKeyValue() {
    Map<String, String> m =
        COMMA_SPLITTER
            .withKeyValueSeparator(Splitter.on(':').trimResults())
            .split("boy  : tom , girl: tina , cat  : kitty , dog: tommy ");
    ImmutableMap<String, String> expected =
        ImmutableMap.of("boy", "tom", "girl", "tina", "cat", "kitty", "dog", "tommy");
    assertThat(m).isEqualTo(expected);
    assertThat(m.entrySet()).containsExactlyElementsIn(expected.entrySet()).inOrder();
  }

  public void testMapSplitter_notTrimmed() {
    Map<String, String> m =
        COMMA_SPLITTER
            .withKeyValueSeparator(":")
            .split(" boy:tom , girl: tina , cat :kitty , dog:  tommy ");
    ImmutableMap<String, String> expected =
        ImmutableMap.of(" boy", "tom ", " girl", " tina ", " cat ", "kitty ", " dog", "  tommy ");
    assertThat(m).isEqualTo(expected);
    assertThat(m.entrySet()).containsExactlyElementsIn(expected.entrySet()).inOrder();
  }

  public void testMapSplitter_CharacterSeparator() {
    // try different delimiters.
    Map<String, String> m =
        Splitter.on(",").withKeyValueSeparator(':').split("boy:tom,girl:tina,cat:kitty,dog:tommy");
    ImmutableMap<String, String> expected =
        ImmutableMap.of("boy", "tom", "girl", "tina", "cat", "kitty", "dog", "tommy");

    assertThat(m).isEqualTo(expected);
    assertThat(m.entrySet()).containsExactlyElementsIn(expected.entrySet()).inOrder();
  }

  public void testMapSplitter_multiCharacterSeparator() {
    // try different delimiters.
    Map<String, String> m =
        Splitter.on(",")
            .withKeyValueSeparator(":^&")
            .split("boy:^&tom,girl:^&tina,cat:^&kitty,dog:^&tommy");
    ImmutableMap<String, String> expected =
        ImmutableMap.of("boy", "tom", "girl", "tina", "cat", "kitty", "dog", "tommy");

    assertThat(m).isEqualTo(expected);
    assertThat(m.entrySet()).containsExactlyElementsIn(expected.entrySet()).inOrder();
  }

  public void testMapSplitter_emptySeparator() {
    try {
      COMMA_SPLITTER.withKeyValueSeparator("");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMapSplitter_malformedEntry() {
    try {
      COMMA_SPLITTER.withKeyValueSeparator("=").split("a=1,b,c=2");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  /**
   * Testing the behavior in https://github.com/google/guava/issues/1900 - this behavior may want to
   * be changed?
   */
  public void testMapSplitter_extraValueDelimiter() {
    try {
      COMMA_SPLITTER.withKeyValueSeparator("=").split("a=1,c=2=");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMapSplitter_orderedResults() {
    Map<String, String> m =
        COMMA_SPLITTER.withKeyValueSeparator(":").split("boy:tom,girl:tina,cat:kitty,dog:tommy");

    assertThat(m.keySet()).containsExactly("boy", "girl", "cat", "dog").inOrder();
    assertThat(m)
        .isEqualTo(ImmutableMap.of("boy", "tom", "girl", "tina", "cat", "kitty", "dog", "tommy"));

    // try in a different order
    m = COMMA_SPLITTER.withKeyValueSeparator(":").split("girl:tina,boy:tom,dog:tommy,cat:kitty");

    assertThat(m.keySet()).containsExactly("girl", "boy", "dog", "cat").inOrder();
    assertThat(m)
        .isEqualTo(ImmutableMap.of("boy", "tom", "girl", "tina", "cat", "kitty", "dog", "tommy"));
  }

  public void testMapSplitter_duplicateKeys() {
    try {
      COMMA_SPLITTER.withKeyValueSeparator(":").split("a:1,b:2,a:3");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMapSplitter_varyingTrimLevels() {
    MapSplitter splitter = COMMA_SPLITTER.trimResults().withKeyValueSeparator(Splitter.on("->"));
    Map<String, String> split = splitter.split(" x -> y, z-> a ");
    assertThat(split).containsEntry("x ", " y");
    assertThat(split).containsEntry("z", " a");
  }
}

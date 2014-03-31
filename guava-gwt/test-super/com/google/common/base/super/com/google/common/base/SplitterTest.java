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

import static org.truth0.Truth.ASSERT;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Julien Silland
 */
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
    ASSERT.that(letters).iteratesOverSequence("a", "b", "c");
  }

  /**
   * All of the infrastructure of split and splitToString is identical, so we
   * do one test of splitToString. All other cases should be covered by testing
   * of split.
   *
   * <p>TODO(user): It would be good to make all the relevant tests run on
   * both split and splitToString automatically.
   */
  public void testCharacterSimpleSplitToList() {
    String simple = "a,b,c";
    List<String> letters = COMMA_SPLITTER.splitToList(simple);
    ASSERT.that(letters).iteratesOverSequence("a", "b", "c");
  }

  public void testToString() {
    assertEquals("[]", Splitter.on(',').split("").toString());
    assertEquals("[a, b, c]", Splitter.on(',').split("a,b,c").toString());
    assertEquals("[yam, bam, jam, ham]", Splitter.on(", ").split("yam, bam, jam, ham").toString());
  }

  public void testCharacterSimpleSplitWithNoDelimiter() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.on('.').split(simple);
    ASSERT.that(letters).iteratesOverSequence("a,b,c");
  }

  public void testCharacterSplitWithDoubleDelimiter() {
    String doubled = "a,,b,c";
    Iterable<String> letters = COMMA_SPLITTER.split(doubled);
    ASSERT.that(letters).iteratesOverSequence("a", "", "b", "c");
  }

  public void testCharacterSplitWithDoubleDelimiterAndSpace() {
    String doubled = "a,, b,c";
    Iterable<String> letters = COMMA_SPLITTER.split(doubled);
    ASSERT.that(letters).iteratesOverSequence("a", "", " b", "c");
  }

  public void testCharacterSplitWithTrailingDelimiter() {
    String trailing = "a,b,c,";
    Iterable<String> letters = COMMA_SPLITTER.split(trailing);
    ASSERT.that(letters).iteratesOverSequence("a", "b", "c", "");
  }

  public void testCharacterSplitWithLeadingDelimiter() {
    String leading = ",a,b,c";
    Iterable<String> letters = COMMA_SPLITTER.split(leading);
    ASSERT.that(letters).iteratesOverSequence("", "a", "b", "c");
  }

  public void testCharacterSplitWithMulitpleLetters() {
    Iterable<String> testCharacteringMotto = Splitter.on('-').split(
        "Testing-rocks-Debugging-sucks");
    ASSERT.that(testCharacteringMotto).iteratesOverSequence(
        "Testing", "rocks", "Debugging", "sucks");
  }

  public void testCharacterSplitWithMatcherDelimiter() {
    Iterable<String> testCharacteringMotto = Splitter
        .on(CharMatcher.WHITESPACE)
        .split("Testing\nrocks\tDebugging sucks");
    ASSERT.that(testCharacteringMotto).iteratesOverSequence(
        "Testing", "rocks", "Debugging", "sucks");
  }

  public void testCharacterSplitWithDoubleDelimiterOmitEmptyStrings() {
    String doubled = "a..b.c";
    Iterable<String> letters = Splitter.on('.')
        .omitEmptyStrings().split(doubled);
    ASSERT.that(letters).iteratesOverSequence("a", "b", "c");
  }

  public void testCharacterSplitEmptyToken() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on('.').trimResults()
        .split(emptyToken);
    ASSERT.that(letters).iteratesOverSequence("a", "", "c");
  }

  public void testCharacterSplitEmptyTokenOmitEmptyStrings() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on('.')
        .omitEmptyStrings().trimResults().split(emptyToken);
    ASSERT.that(letters).iteratesOverSequence("a", "c");
  }

  public void testCharacterSplitOnEmptyString() {
    Iterable<String> nothing = Splitter.on('.').split("");
    ASSERT.that(nothing).iteratesOverSequence("");
  }

  public void testCharacterSplitOnEmptyStringOmitEmptyStrings() {
    ASSERT.that(Splitter.on('.').omitEmptyStrings().split("")).isEmpty();
  }

  public void testCharacterSplitOnOnlyDelimiter() {
    Iterable<String> blankblank = Splitter.on('.').split(".");
    ASSERT.that(blankblank).iteratesOverSequence("", "");
  }

  public void testCharacterSplitOnOnlyDelimitersOmitEmptyStrings() {
    Iterable<String> empty = Splitter.on('.').omitEmptyStrings().split("...");
    ASSERT.that(empty).isEmpty();
  }

  public void testCharacterSplitWithTrim() {
    String jacksons = "arfo(Marlon)aorf, (Michael)orfa, afro(Jackie)orfa, "
        + "ofar(Jemaine), aff(Tito)";
    Iterable<String> family = COMMA_SPLITTER
        .trimResults(CharMatcher.anyOf("afro").or(CharMatcher.WHITESPACE))
        .split(jacksons);
    ASSERT.that(family).iteratesOverSequence(
        "(Marlon)", "(Michael)", "(Jackie)", "(Jemaine)", "(Tito)");
  }

  public void testStringSimpleSplit() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.on(',').split(simple);
    ASSERT.that(letters).iteratesOverSequence("a", "b", "c");
  }

  public void testStringSimpleSplitWithNoDelimiter() {
    String simple = "a,b,c";
    Iterable<String> letters = Splitter.on('.').split(simple);
    ASSERT.that(letters).iteratesOverSequence("a,b,c");
  }

  public void testStringSplitWithDoubleDelimiter() {
    String doubled = "a,,b,c";
    Iterable<String> letters = Splitter.on(',').split(doubled);
    ASSERT.that(letters).iteratesOverSequence("a", "", "b", "c");
  }

  public void testStringSplitWithDoubleDelimiterAndSpace() {
    String doubled = "a,, b,c";
    Iterable<String> letters = Splitter.on(',').split(doubled);
    ASSERT.that(letters).iteratesOverSequence("a", "", " b", "c");
  }

  public void testStringSplitWithTrailingDelimiter() {
    String trailing = "a,b,c,";
    Iterable<String> letters = Splitter.on(',').split(trailing);
    ASSERT.that(letters).iteratesOverSequence("a", "b", "c", "");
  }

  public void testStringSplitWithLeadingDelimiter() {
    String leading = ",a,b,c";
    Iterable<String> letters = Splitter.on(',').split(leading);
    ASSERT.that(letters).iteratesOverSequence("", "a", "b", "c");
  }

  public void testStringSplitWithMultipleLetters() {
    Iterable<String> testStringingMotto = Splitter.on('-').split(
        "Testing-rocks-Debugging-sucks");
    ASSERT.that(testStringingMotto).iteratesOverSequence(
        "Testing", "rocks", "Debugging", "sucks");
  }

  public void testStringSplitWithDoubleDelimiterOmitEmptyStrings() {
    String doubled = "a..b.c";
    Iterable<String> letters = Splitter.on('.')
        .omitEmptyStrings().split(doubled);
    ASSERT.that(letters).iteratesOverSequence("a", "b", "c");
  }

  public void testStringSplitEmptyToken() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on('.').trimResults()
        .split(emptyToken);
    ASSERT.that(letters).iteratesOverSequence("a", "", "c");
  }

  public void testStringSplitEmptyTokenOmitEmptyStrings() {
    String emptyToken = "a. .c";
    Iterable<String> letters = Splitter.on('.')
        .omitEmptyStrings().trimResults().split(emptyToken);
    ASSERT.that(letters).iteratesOverSequence("a", "c");
  }

  public void testStringSplitWithLongDelimiter() {
    String longDelimiter = "a, b, c";
    Iterable<String> letters = Splitter.on(", ").split(longDelimiter);
    ASSERT.that(letters).iteratesOverSequence("a", "b", "c");
  }

  public void testStringSplitWithLongLeadingDelimiter() {
    String longDelimiter = ", a, b, c";
    Iterable<String> letters = Splitter.on(", ").split(longDelimiter);
    ASSERT.that(letters).iteratesOverSequence("", "a", "b", "c");
  }

  public void testStringSplitWithLongTrailingDelimiter() {
    String longDelimiter = "a, b, c, ";
    Iterable<String> letters = Splitter.on(", ").split(longDelimiter);
    ASSERT.that(letters).iteratesOverSequence("a", "b", "c", "");
  }

  public void testStringSplitWithDelimiterSubstringInValue() {
    String fourCommasAndFourSpaces = ",,,,    ";
    Iterable<String> threeCommasThenThreeSpaces = Splitter.on(", ").split(
        fourCommasAndFourSpaces);
    ASSERT.that(threeCommasThenThreeSpaces).iteratesOverSequence(",,,", "   ");
  }

  public void testStringSplitWithEmptyString() {
    try {
      Splitter.on("");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testStringSplitOnEmptyString() {
    Iterable<String> notMuch = Splitter.on('.').split("");
    ASSERT.that(notMuch).iteratesOverSequence("");
  }

  public void testStringSplitOnEmptyStringOmitEmptyString() {
    ASSERT.that(Splitter.on('.').omitEmptyStrings().split("")).isEmpty();
  }

  public void testStringSplitOnOnlyDelimiter() {
    Iterable<String> blankblank = Splitter.on('.').split(".");
    ASSERT.that(blankblank).iteratesOverSequence("", "");
  }

  public void testStringSplitOnOnlyDelimitersOmitEmptyStrings() {
    Iterable<String> empty = Splitter.on('.').omitEmptyStrings().split("...");
    ASSERT.that(empty).isEmpty();
  }

  public void testStringSplitWithTrim() {
    String jacksons = "arfo(Marlon)aorf, (Michael)orfa, afro(Jackie)orfa, "
        + "ofar(Jemaine), aff(Tito)";
    Iterable<String> family = Splitter.on(',')
        .trimResults(CharMatcher.anyOf("afro").or(CharMatcher.WHITESPACE))
        .split(jacksons);
    ASSERT.that(family).iteratesOverSequence(
        "(Marlon)", "(Michael)", "(Jackie)", "(Jemaine)", "(Tito)");
  }

  // TODO(kevinb): the name of this method suggests it might not actually be testing what it
  // intends to be testing?

  public void testSplitterIterableIsUnmodifiable_char() {
    assertIteratorIsUnmodifiable(COMMA_SPLITTER.split("a,b").iterator());
  }

  public void testSplitterIterableIsUnmodifiable_string() {
    assertIteratorIsUnmodifiable(Splitter.on(',').split("a,b").iterator());
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
    assertSplitterIterableIsLazy(Splitter.on(','));
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

  public void testFixedLengthSimpleSplit() {
    String simple = "abcde";
    Iterable<String> letters = Splitter.fixedLength(2).split(simple);
    ASSERT.that(letters).iteratesOverSequence("ab", "cd", "e");
  }

  public void testFixedLengthSplitEqualChunkLength() {
    String simple = "abcdef";
    Iterable<String> letters = Splitter.fixedLength(2).split(simple);
    ASSERT.that(letters).iteratesOverSequence("ab", "cd", "ef");
  }

  public void testFixedLengthSplitOnlyOneChunk() {
    String simple = "abc";
    Iterable<String> letters = Splitter.fixedLength(3).split(simple);
    ASSERT.that(letters).iteratesOverSequence("abc");
  }

  public void testFixedLengthSplitSmallerString() {
    String simple = "ab";
    Iterable<String> letters = Splitter.fixedLength(3).split(simple);
    ASSERT.that(letters).iteratesOverSequence("ab");
  }

  public void testFixedLengthSplitEmptyString() {
    String simple = "";
    Iterable<String> letters = Splitter.fixedLength(3).split(simple);
    ASSERT.that(letters).iteratesOverSequence("");
  }

  public void testFixedLengthSplitEmptyStringWithOmitEmptyStrings() {
    ASSERT.that(Splitter.fixedLength(3).omitEmptyStrings().split("")).isEmpty();
  }

  public void testFixedLengthSplitIntoChars() {
    String simple = "abcd";
    Iterable<String> letters = Splitter.fixedLength(1).split(simple);
    ASSERT.that(letters).iteratesOverSequence("a", "b", "c", "d");
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
    ASSERT.that(letters).iteratesOverSequence("a", "b", "c", "d");
  }

  public void testLimitOne() {
    String simple = "abcd";
    Iterable<String> letters = Splitter.fixedLength(1).limit(1).split(simple);
    ASSERT.that(letters).iteratesOverSequence("abcd");
  }

  public void testLimitFixedLength() {
    String simple = "abcd";
    Iterable<String> letters = Splitter.fixedLength(1).limit(2).split(simple);
    ASSERT.that(letters).iteratesOverSequence("a", "bcd");
  }

  public void testLimitSeparator() {
    String simple = "a,b,c,d";
    Iterable<String> items = COMMA_SPLITTER.limit(2).split(simple);
    ASSERT.that(items).iteratesOverSequence("a", "b,c,d");
  }

  public void testLimitExtraSeparators() {
    String text = "a,,,b,,c,d";
    Iterable<String> items = COMMA_SPLITTER.limit(2).split(text);
    ASSERT.that(items).iteratesOverSequence("a", ",,b,,c,d");
  }

  public void testLimitExtraSeparatorsOmitEmpty() {
    String text = "a,,,b,,c,d";
    Iterable<String> items = COMMA_SPLITTER.limit(2).omitEmptyStrings().split(text);
    ASSERT.that(items).iteratesOverSequence("a", "b,,c,d");
  }

  public void testLimitExtraSeparatorsOmitEmpty3() {
    String text = "a,,,b,,c,d";
    Iterable<String> items = COMMA_SPLITTER.limit(3).omitEmptyStrings().split(text);
    ASSERT.that(items).iteratesOverSequence("a", "b", "c,d");
  }

  public void testLimitExtraSeparatorsTrim() {
    String text = ",,a,,  , b ,, c,d ";
    Iterable<String> items = COMMA_SPLITTER.limit(2).omitEmptyStrings().trimResults().split(text);
    ASSERT.that(items).iteratesOverSequence("a", "b ,, c,d");
  }

  public void testLimitExtraSeparatorsTrim3() {
    String text = ",,a,,  , b ,, c,d ";
    Iterable<String> items = COMMA_SPLITTER.limit(3).omitEmptyStrings().trimResults().split(text);
    ASSERT.that(items).iteratesOverSequence("a", "b", "c,d");
  }

  public void testLimitExtraSeparatorsTrim1() {
    String text = ",,a,,  , b ,, c,d ";
    Iterable<String> items = COMMA_SPLITTER.limit(1).omitEmptyStrings().trimResults().split(text);
    ASSERT.that(items).iteratesOverSequence("a,,  , b ,, c,d");
  }

  public void testLimitExtraSeparatorsTrim1NoOmit() {
    String text = ",,a,,  , b ,, c,d ";
    Iterable<String> items = COMMA_SPLITTER.limit(1).trimResults().split(text);
    ASSERT.that(items).iteratesOverSequence(",,a,,  , b ,, c,d");
  }

  public void testLimitExtraSeparatorsTrim1Empty() {
    String text = "";
    Iterable<String> items = COMMA_SPLITTER.limit(1).split(text);
    ASSERT.that(items).iteratesOverSequence("");
  }

  public void testLimitExtraSeparatorsTrim1EmptyOmit() {
    String text = "";
    Iterable<String> items = COMMA_SPLITTER.omitEmptyStrings().limit(1).split(text);
    ASSERT.that(items).isEmpty();
  }

  @SuppressWarnings("ReturnValueIgnored") // testing for exception
  public void testInvalidZeroLimit() {
    try {
      COMMA_SPLITTER.limit(0);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  private static <E> List<E> asList(Collection<E> collection) {
    return ImmutableList.copyOf(collection);
  }

  public void testMapSplitter_trimmedBoth() {
    Map<String, String> m = COMMA_SPLITTER
        .trimResults()
        .withKeyValueSeparator(Splitter.on(':').trimResults())
        .split("boy  : tom , girl: tina , cat  : kitty , dog: tommy ");
    ImmutableMap<String, String> expected =
          ImmutableMap.of("boy", "tom", "girl", "tina", "cat", "kitty", "dog", "tommy");
    ASSERT.that(m).isEqualTo(expected);
    ASSERT.that(asList(m.entrySet())).is(asList(expected.entrySet()));
  }

  public void testMapSplitter_trimmedEntries() {
    Map<String, String> m = COMMA_SPLITTER
        .trimResults()
        .withKeyValueSeparator(":")
        .split("boy  : tom , girl: tina , cat  : kitty , dog: tommy ");
    ImmutableMap<String, String> expected =
        ImmutableMap.of("boy  ", " tom", "girl", " tina", "cat  ", " kitty", "dog", " tommy");

    ASSERT.that(m).isEqualTo(expected);
    ASSERT.that(asList(m.entrySet())).is(asList(expected.entrySet()));
  }

  public void testMapSplitter_trimmedKeyValue() {
    Map<String, String> m =
        COMMA_SPLITTER.withKeyValueSeparator(Splitter.on(':').trimResults()).split(
            "boy  : tom , girl: tina , cat  : kitty , dog: tommy ");
    ImmutableMap<String, String> expected =
        ImmutableMap.of("boy", "tom", "girl", "tina", "cat", "kitty", "dog", "tommy");
    ASSERT.that(m).isEqualTo(expected);
    ASSERT.that(asList(m.entrySet())).is(asList(expected.entrySet()));
  }

  public void testMapSplitter_notTrimmed() {
    Map<String, String> m = COMMA_SPLITTER.withKeyValueSeparator(":").split(
        " boy:tom , girl: tina , cat :kitty , dog:  tommy ");
    ImmutableMap<String, String> expected =
        ImmutableMap.of(" boy", "tom ", " girl", " tina ", " cat ", "kitty ", " dog", "  tommy ");
    ASSERT.that(m).isEqualTo(expected);
    ASSERT.that(asList(m.entrySet())).is(asList(expected.entrySet()));
  }

  public void testMapSplitter_CharacterSeparator() {
    // try different delimiters.
    Map<String, String> m = Splitter
        .on(",")
        .withKeyValueSeparator(':')
        .split("boy:tom,girl:tina,cat:kitty,dog:tommy");
    ImmutableMap<String, String> expected =
        ImmutableMap.of("boy", "tom", "girl", "tina", "cat", "kitty", "dog", "tommy");

    ASSERT.that(m).isEqualTo(expected);
    ASSERT.that(asList(m.entrySet())).is(asList(expected.entrySet()));
  }

  public void testMapSplitter_multiCharacterSeparator() {
    // try different delimiters.
    Map<String, String> m = Splitter
        .on(",")
        .withKeyValueSeparator(":^&")
        .split("boy:^&tom,girl:^&tina,cat:^&kitty,dog:^&tommy");
    ImmutableMap<String, String> expected =
        ImmutableMap.of("boy", "tom", "girl", "tina", "cat", "kitty", "dog", "tommy");

    ASSERT.that(m).isEqualTo(expected);
    ASSERT.that(asList(m.entrySet())).is(asList(expected.entrySet()));
  }

  @SuppressWarnings("ReturnValueIgnored") // testing for exception
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

  public void testMapSplitter_orderedResults() {
    Map<String, String> m = Splitter.on(',')
        .withKeyValueSeparator(":")
        .split("boy:tom,girl:tina,cat:kitty,dog:tommy");

    ASSERT.that(m.keySet()).iteratesOverSequence("boy", "girl", "cat", "dog");
    ASSERT.that(m).isEqualTo(
        ImmutableMap.of("boy", "tom", "girl", "tina", "cat", "kitty", "dog", "tommy"));

    // try in a different order
    m = Splitter.on(',')
        .withKeyValueSeparator(":")
        .split("girl:tina,boy:tom,dog:tommy,cat:kitty");

    ASSERT.that(m.keySet()).iteratesOverSequence("girl", "boy", "dog", "cat");
    ASSERT.that(m).isEqualTo(
        ImmutableMap.of("boy", "tom", "girl", "tina", "cat", "kitty", "dog", "tommy"));
  }

  public void testMapSplitter_duplicateKeys() {
    try {
      Splitter.on(',').withKeyValueSeparator(":").split("a:1,b:2,a:3");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}

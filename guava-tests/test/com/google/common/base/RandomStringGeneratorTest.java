/*
 * Copyright 2017 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.common.base;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author Olivier Grégoire
 */
public class RandomStringGeneratorTest extends TestCase {

  private Random random = new Random(0L);
  private static final CharMatcher DIGITS = CharMatcher.inRange('0', '9');
  private static final CharMatcher LOWER_ALPHA = CharMatcher.inRange('a', 'z');
  private static final CharMatcher UPPER_ALPHA = CharMatcher.inRange('A', 'Z');
  private static final CharMatcher ALPHA = LOWER_ALPHA.or(UPPER_ALPHA);
  private static final CharMatcher ALPHANUMERIC = ALPHA.or(DIGITS);
  private static final CharMatcher SYMBOLS = CharMatcher.ascii()
      .and(ALPHANUMERIC.negate())
      .and(CharMatcher.inRange('\0', ' ').negate());

  public void testGeneratedLength() {
    RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withCharacters("a")
        .build(10, random);

    for (int loop = 0; loop < 100; loop++) {
      assertThat(generator.next().length(), is(10));
    }
  }

  public void testRandom() {
    RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withLowercaseAsciiLetters()
        .build(10, random);

    Set<String> generated = new HashSet<>();
    for (int loop = 0; loop < 100; loop++) {
      generated.add(generator.next());
    }
    // 26^10 = 1.411671e+14 . 100 chances on 26^10 of duplicates. So it's very, very low. But still...
    assertTrue(generated.size() >= 99); // Allow one duplicate, even though there are none with Random(0)
  }

  public void testLowerAlpha() {
    RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withLowercaseAsciiLetters()
        .build(10, random);

    doTestCharacters(generator, LOWER_ALPHA);
  }

  public void testUpperAlpha() {
    RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withUppercaseAsciiLetters()
        .build(10, random);

    doTestCharacters(generator, UPPER_ALPHA);
  }

  public void testDigits() {
    RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withAsciiDigits()
        .build(10, random);

    doTestCharacters(generator, DIGITS);
  }

  public void testSymbols() {
    RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withVisibleAsciiSymbols()
        .build(10, random);

    doTestCharacters(generator, SYMBOLS);
  }

  public void testAlphanumeric() {
    RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withUppercaseAsciiLetters()
        .withLowercaseAsciiLetters()
        .withAsciiDigits()
        .build(10, random);

    doTestCharacters(generator, ALPHANUMERIC);
  }

  public void testRange() {
    RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withRange('0', '9')
        .build(10, random);

    doTestCharacters(generator, DIGITS);
  }

  public void testMinimumQuantities() {
    RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withCharacters("-", 5)
        .withLowercaseAsciiLetters()
        .build(10, random);

    CharMatcher isDash = CharMatcher.is('-');
    CharMatcher isDashOrLowerAlpha = isDash.or(LOWER_ALPHA);

    doTestCharacters(generator, isDashOrLowerAlpha);
    for (int loop = 0; loop < 100; loop++) {
      String generated = generator.next();
      String dashes = isDash.retainFrom(generated);
      assertTrue(dashes.length() >= 5);
    }
  }

  public void testMinimumQuantitiesParametersFail() {
    try {
      new RandomStringGenerator.Builder()
          .withAsciiDigits(-1);
      fail("Expected IllegalArgumentException, but suceeded");
    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalArgumentException, got " + e.getClass().getSimpleName());
    }
    try {
      new RandomStringGenerator.Builder()
          .withCharacters("abc", -1);
      fail("Expected IllegalArgumentException, but suceeded");
    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalArgumentException, got " + e.getClass().getSimpleName());
    }
    try {
      new RandomStringGenerator.Builder()
          .withLowercaseAsciiLetters(-1);
      fail("Expected IllegalArgumentException, but suceeded");
    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalArgumentException, got " + e.getClass().getSimpleName());
    }
    try {
      new RandomStringGenerator.Builder()
          .withUppercaseAsciiLetters(-1);
      fail("Expected IllegalArgumentException, but suceeded");
    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalArgumentException, got " + e.getClass().getSimpleName());
    }
    try {
      new RandomStringGenerator.Builder()
          .withRange('a', 'c', -1);
      fail("Expected IllegalArgumentException, but suceeded");
    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalArgumentException, got " + e.getClass().getSimpleName());
    }
    try {
      new RandomStringGenerator.Builder()
          .withVisibleAsciiSymbols(-1);
      fail("Expected IllegalArgumentException, but suceeded");
    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalArgumentException, got " + e.getClass().getSimpleName());
    }
  }

  public void testRangeParametersFail() {
    try {
      new RandomStringGenerator.Builder()
          .withRange('z', 'a');
      fail("Expected IllegalArgumentException, but suceeded");
    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalArgumentException, got " + e.getClass().getSimpleName());
    }
    try {
      new RandomStringGenerator.Builder()
          .withRange('z', 'a', 5);
      fail("Expected IllegalArgumentException, but suceeded");
    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalArgumentException, got " + e.getClass().getSimpleName());
    }
  }

  public void testCharactersEmpty() {
    try {
      new RandomStringGenerator.Builder()
          .withCharacters("");
      fail("Expected IllegalArgumentException, but suceeded");
    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalArgumentException, got " + e.getClass().getSimpleName());
    }
  }

  public void testWeighted() {
    RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withCharacters("aaaaaaaaab") // 9 'a', 1 'b'
        .build(10, random);
    testWeight(generator, 0.9, 0.1); // about 9 'a' for 1 'b'
  }

  public void testUnique() {
    RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withCharacters("aaaaaaaaab") // 9 'a', 1 'b'
        .unique()
        .build(10, random);
    testWeight(generator, 0.5, 0.1); // about as much 'a' as 'b'
  }

  public void testAvoidSimilarChars() {
    RandomStringGenerator generator = new RandomStringGenerator.Builder()
        .withAsciiDigits()
        .avoidSimilarAsciiCharacters()
        .build(10, random);
    CharMatcher digitsNoZeroNoOne = DIGITS.and(CharMatcher.anyOf("01").negate());
    doTestCharacters(generator, digitsNoZeroNoOne);

    generator = new RandomStringGenerator.Builder()
        .withAsciiDigits()
        .withLowercaseAsciiLetters()
        .withUppercaseAsciiLetters()
        .avoidSimilarAsciiCharacters()
        .build(1000, random); // Bigger strings than usual to make sure they should be present, but aren't
    CharMatcher noSimilarCharacter = ALPHANUMERIC.and(CharMatcher.anyOf("01IOl").negate());
    doTestCharacters(generator, noSimilarCharacter);
  }

  public void testAvoidSimilarCharsWithSimilarChars() {
    try {
      new RandomStringGenerator.Builder()
          .withCharacters("IO10") // Letter i uppercase, letter o uppercase, digit one, digit zero
          .avoidSimilarAsciiCharacters()
          .build(10);

      fail("Expected IllegalStateException, but suceeded");
    } catch (IllegalStateException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalStateException, got " + e.getClass().getSimpleName());
    }
  }

  public void testBuildParameters() {
    try {
      new RandomStringGenerator.Builder()
          .withCharacters("abc")
          .build(-1);
      fail("Expected IllegalArgumentException, but suceeded");
    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalArgumentException, got " + e.getClass().getSimpleName());
    }
    try {
      new RandomStringGenerator.Builder()
          .withCharacters("abc")
          .build(1, null);
      fail("Expected NullPointerException, but suceeded");
    } catch (NullPointerException e) {
      // success
    } catch (Exception e) {
      fail("Expected NullPointerException, got " + e.getClass().getSimpleName());
    }
  }

  public void testBuildNoCallOfWithMethods() {
    try {
      new RandomStringGenerator.Builder()
          .build(10);
      fail("Expected IllegalStateException, but suceeded");
    } catch (IllegalStateException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalStateException, got " + e.getClass().getSimpleName());
    }
  }

  public void testSizeLowerThanMinimumQuantities() {
    try {
      new RandomStringGenerator.Builder()
          .withCharacters("a", 4)
          .build(2);
      fail("Expected IllegalArgumentException, but suceeded");
    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalArgumentException, got " + e.getClass().getSimpleName());
    }
    try {
      new RandomStringGenerator.Builder()
          .withCharacters("a", 3)
          .withCharacters("b", 3)
          .build(5); // bigger than each, lower than sum
      fail("Expected IllegalArgumentException, but suceeded");
    } catch (IllegalArgumentException e) {
      // success
    } catch (Exception e) {
      fail("Expected IllegalArgumentException, got " + e.getClass().getSimpleName());
    }
  }

  private void testWeight(RandomStringGenerator generator, double expectedARatio, double delta) {
    int as = 0;
    int bs = 0;
    for (int loop = 0; loop < 100; loop++) {
      String generated = generator.next();
      for (int index = 0; index < generated.length(); index++) {
        char character = generated.charAt(index);
        if (character == 'a') {
          as++;
        } else if (character == 'b') {
          bs++;
        } else {
          fail("Not 'a' or 'b'");
        }
      }
    }
    assertThat(as + bs, is(1000));
    double aRatio = as / 1000.0d;
    assertEquals(expectedARatio, aRatio, delta);
  }

  private void doTestCharacters(RandomStringGenerator generator, CharMatcher matcher) {
    for (int loop = 0; loop < 100; loop++) {
      String generated = generator.next();
      for (int index = 0; index < generated.length(); index++) {
        char character = generated.charAt(index);
        assertTrue(matcher.matches(character));
      }
    }
  }
}

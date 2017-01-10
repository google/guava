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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.primitives.Chars;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;

/**
 * This class allows the generation of random {@code String}s within flexible parameters.
 *
 * <pre>
 * {@code
 *   RandomStringGenerator generator = new RandomStringGenerator()
 *       .withLowercaseAsciiLetters()
 *       .withAsciiDigits()
 *       .build();
 *   for (int i = 0; i &lt; 10; i++) {
 *     System.out.println(generator.next());
 *   }
 * }
 * </pre>
 *
 * <p>
 * By default, no check is done on the quantity a character is present, so if a generator has to
 * look through {@code "aaab"}, {@code 'a'} has three times the chances of {@code 'b'} to appear. If
 * this behavior is undesired, {@link Builder#unique()} must be called on the builder.
 *
 * <p>
 * A possibility exists to avoid ascii characters that looks similar to each other. Calling {@link Builder#avoidSimilarAsciiCharacters()
 * } on the builder will ensure that those characters do not appear. The list of those characters is
 * the following:
 *
 * <ul>
 * <li>{@code 0} (Digit zero, ASCII 0x30}</li>
 * <li>{@code 1} (Digit one, ASCII 0x31}</li>
 * <li>{@code I} (Uppercase letter i, ASCII 0x49}</li>
 * <li>{@code O} (Uppercase letter o, ASCII 0x4F}</li>
 * <li>{@code l} (Lowercase letter L, ASCII 0x6C}</li>
 * </ul>
 *
 * <p>
 * Note regarding performance: this class internally uses {@code Random}. So for a multithreaded
 * environment, please note that the performances can be impacted, as mentioned in {@link Random}.
 *
 * @author Olivier Grégoire
 */
@ThreadSafe
public class RandomStringGenerator implements Supplier<String> {

  private static final CharMatcher SIMILAR_ASCII_CHARS = CharMatcher.anyOf("0O1Il");

  private final int size;
  private final Random random;
  private final char[] allChars;
  private final ImmutableMultiset<String> requiredChars;

  private RandomStringGenerator(Builder builder, int size, Random random) {
    this.size = size;
    this.random = random;
    StringBuilder chars = new StringBuilder();
    ImmutableMultiset.Builder<String> requiredCharsBuilder = ImmutableMultiset.builder();
    Set<Character> uniqueChars = new LinkedHashSet<>();
    for (Map.Entry<String, Integer> rule : builder.chars.entrySet()) {
      String characters = rule.getKey();
      Integer amount = rule.getValue();
      if (builder.avoidSimilarAsciiCharacters) {
        characters = SIMILAR_ASCII_CHARS.removeFrom(characters);
      }
      if (builder.unique) {
        Set<Character> uniques = new LinkedHashSet<>(Chars.asList(characters.toCharArray()));
        uniqueChars.addAll(uniques);
        characters = new String(Chars.toArray(uniques));
      } else {
        chars.append(characters);
      }
      if (amount != null) {
        requiredCharsBuilder.addCopies(characters, amount);
      }
    }
    if (builder.unique) {
      this.allChars = Chars.toArray(uniqueChars);
    } else {
      this.allChars = chars.toString().toCharArray();
    }
    this.requiredChars = requiredCharsBuilder.build();
  }

  /**
   * Provided only to satisfy the {@link Supplier} interface; use {@link #next() } instead.
   *
   * @return a new random {@code String} within the limits imposed to this object.
   * @deprecated
   */
  @Override
  @Deprecated
  public String get() {
    return next();
  }

  /**
   * Generates a new random {@code String} within the limits imposed to this object by the builder.
   *
   * @return a new random {@code String}.
   */
  public String next() {
    char[] chars = new char[size];
    int index = 0;

    // Add all required rules.
    for (Multiset.Entry<String> required : requiredChars.entrySet()) {
      for (int i = 0; i < required.getCount(); i++) {
        String pool = required.getElement();
        chars[index] = pool.charAt(random.nextInt(pool.length()));
        index++;
      }
    }

    for (; index < size; index++) {
      chars[index] = allChars[random.nextInt(allChars.length)];
    }

    // If there are minimum letters requirements, they were added as first char. Shuffle is needed.
    if (!requiredChars.isEmpty()) {
      for (int i = chars.length - 1; i > 0; i--) {
        int pos = random.nextInt(i + 1);
        char swap = chars[pos];
        chars[pos] = chars[i];
        chars[i] = swap;
      }
    }
    return new String(chars);
  }

  /**
   * Builds new generators based on the rules it has.
   */
  public static class Builder {

    private static final String ASCII_ALPHA_LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String ASCII_ALPHA_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ASCII_DIGITS = "0123456789";
    private static final String ASCII_SYMBOLS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    private final Map<String, Integer> chars = new HashMap<>();
    private boolean unique = false;
    private boolean avoidSimilarAsciiCharacters = false;
    private int countRequiredCharacters = 0;

    /**
     * Creates a new {@code Builder}.
     */
    public Builder() {
    }

    /**
     * Add the letters {@code abcdefghijklmnopqrstuvwxyz} to the pool of characters to use in the
     * generator.
     *
     * @return this {@code Builder}
     */
    public Builder withLowercaseAsciiLetters() {
      return addCharacters(ASCII_ALPHA_LOWERCASE, null);
    }

    /**
     * Adds the letters {@code abcdefghijklmnopqrstuvwxyz} to the pool of characters to use in the
     * generator, and indicates that at least {@code minimumQuantity} characters of the generated
     * {@code String} must be from this pool.
     *
     * @param minimumQuantity the number of letters from this pool that must appear in the generated
     * {@code String}.
     * @return this {@code Builder}
     */
    public Builder withLowercaseAsciiLetters(int minimumQuantity) {
      return addCharacters(ASCII_ALPHA_LOWERCASE, minimumQuantity);
    }

    /**
     * Adds the letters {@code ABCDEFGHIJKLMNOPQRSTUVWXYZ} to the pool of characters to use in the
     * generator.
     *
     * @return this {@code Builder}
     */
    public Builder withUppercaseAsciiLetters() {
      return addCharacters(ASCII_ALPHA_UPPERCASE, null);
    }

    /**
     * Adds the letters {@code ABCDEFGHIJKLMNOPQRSTUVWXYZ} to the pool of characters to use in the
     * generator, and indicates that at least {@code minimumQuantity} characters of the generated
     * {@code String} must be from this pool.
     *
     * @param minimumQuantity the number of letters from this pool that must appear in the generated
     * {@code String}.
     * @return this {@code Builder}
     */
    public Builder withUppercaseAsciiLetters(int minimumQuantity) {
      return addCharacters(ASCII_ALPHA_UPPERCASE, minimumQuantity);
    }

    /**
     * Adds the digits {@code 0123456789} to the pool of characters to use in the generator.
     *
     * @return this {@code Builder}
     */
    public Builder withAsciiDigits() {
      return addCharacters(ASCII_DIGITS, null);
    }

    /**
     * Adds the digits {@code 0123456789} to the pool of characters to use in the generator, and
     * indicates that at least {@code minimumQuantity} characters of the generated {@code String}
     * must be from this pool.
     *
     * @param minimumQuantity the number of letters from this pool that must appear in the generated
     * {@code String}.
     * @return this {@code Builder}
     */
    public Builder withAsciiDigits(int minimumQuantity) {
      return addCharacters(ASCII_DIGITS, minimumQuantity);
    }

    /**
     * Adds the symbols {@code !\"#$%&'()*+,-./:;&lt;=&gt;?@[\\]^_`{|}~} to the pool of characters
     * to use in the generator.
     *
     * @return this {@code Builder}
     */
    public Builder withVisibleAsciiSymbols() {
      return addCharacters(ASCII_SYMBOLS, null);
    }

    /**
     * Adds the symbols {@code !\"#$%&'()*+,-./:;&lt;=&gt;?@[\\]^_`{|}~} to the pool of characters
     * to use in the generator, and indicates that at least {@code minimumQuantity} characters of
     * the generated {@code String} must be from this pool.
     *
     * @param minimumQuantity the number of letters from this pool that must appear in the generated
     * {@code String}.
     * @return this {@code Builder}
     */
    public Builder withVisibleAsciiSymbols(int minimumQuantity) {
      return addCharacters(ASCII_SYMBOLS, minimumQuantity);
    }

    /**
     * Adds the custom {@code characters} to the pool of characters to use in the generator.
     *
     * @param characters the characters to be added to the character pool of the generated
     * {@code String}s.
     * @return this {@code Builder}
     * @throws IllegalArgumentException if {@code charaters} is empty ({@code ""}).
     */
    public Builder withCharacters(String characters) {
      checkNotNull(characters);
      checkArgument(!characters.isEmpty());
      return addCharacters(checkNotNull(characters), null);
    }

    /**
     * Adds the custom {@code characters} to the pool of characters to use in the generator, and
     * indicates that at least {@code minimumQuantity} characters of the generated {@code String}
     * must be from this pool.
     *
     * @param characters the characters to be added to the character pool of the generated
     * {@code String}s.
     * @param minimumQuantity the number of letters from this pool that must appear in the generated
     * {@code String}.
     * @return this {@code Builder}
     * @throws IllegalArgumentException if {@code charaters} is empty ({@code ""}).
     */
    public Builder withCharacters(String characters, int minimumQuantity) {
      checkNotNull(characters);
      checkArgument(!characters.isEmpty());
      return addCharacters(characters, minimumQuantity);
    }

    /**
     * Adds the characters in the range {@code [ from , to ]} (both inclusive) to the pool of
     * characters to use in the generator.
     *
     * @param from the starting character, inclusive
     * @param to the ending character, inclusive
     * @return this {@code Builder}
     * @throws IllegalArgumentException if {@code to &lt; from}
     */
    public Builder withRange(char from, char to) {
      return addCharacters(rangeAsString(from, to), null);
    }

    /**
     * Adds the characters in the range {@code [ from , to ]} (both inclusive) to the pool of
     * characters to use in the generator, and indicates that at least {@code minimumQuantity}
     * characters of the generated {@code String} must be from this pool.
     *
     * @param from the starting character, inclusive
     * @param to the ending character, inclusive
     * @param minimumQuantity the number of letters from this pool that must appear in the generated
     * {@code String}.
     * @return this {@code Builder}
     * @throws IllegalArgumentException if {@code to &lt; from}
     */
    public Builder withRange(char from, char to, int minimumQuantity) {
      return addCharacters(rangeAsString(from, to), minimumQuantity);
    }

    private String rangeAsString(char from, char to) {
      checkArgument(from <= to);
      int size = to - from + 1;
      char[] range = new char[size];
      for (int i = 0; i < range.length; i++, from++) {
        range[i] = from;
      }
      return new String(range);
    }

    private Builder addCharacters(String characters, Integer minimumQuantity) {
      checkArgument(minimumQuantity == null || minimumQuantity > 0,
          "minimumQuantity (%s) must be strictly positive", minimumQuantity);
      chars.put(characters, minimumQuantity);
      if (minimumQuantity != null) {
        countRequiredCharacters += minimumQuantity;
      }
      return this;
    }

    /**
     * Makes sure that the number of times a character is provided has no influence on its weight:
     * each unique character has an equal chance to appear in the generated {@code String}
     *
     * @return this {@code Builder}
     */
    public Builder unique() {
      unique = true;
      return this;
    }

    /**
     * <p>
     * Tells the builder that the following characters will be removed from the generated Strings:
     *
     * <ul>
     * <li>{@code 0} (Digit zero, ASCII 0x30}</li>
     * <li>{@code 1} (Digit one, ASCII 0x31}</li>
     * <li>{@code I} (Uppercase letter i, ASCII 0x49}</li>
     * <li>{@code O} (Uppercase letter o, ASCII 0x4F}</li>
     * <li>{@code l} (Lowercase letter L, ASCII 0x6C}</li>
     * </ul>
     *
     * @return this {@code Builder}
     */
    public Builder avoidSimilarAsciiCharacters() {
      avoidSimilarAsciiCharacters = true;
      return this;
    }

    /**
     * Builds a new {@code RandomStringGenerator} with the defined size.
     *
     * @param size the size of the {@code String}s to generate
     * @return a new {@code RandomStringGenerator} with the defined size.
     * @throws IllegalArgumentException if the size is lower than or equal to {@code 0} or if size
     * is lower than the sum of all {@code minimumQuantity} parameters added when building the
     * generator.
     * @throws IllegalStateException if no {@code with*} method was called or if a
     * {@code withCharacters(String)} and {@code avoidSimilarAsciiCharacters()} cancel each other
     */
    public RandomStringGenerator build(int size) {
      checkBuild(size);
      return new RandomStringGenerator(this, size, new Random());
    }

    /**
     * Builds a new {@code RandomStringGenerator} with the defined size and {@code Random}.
     *
     * @param size the size of the {@code String}s to generate
     * @param random the random to use when generating {@code String}s.
     * @return a new {@code RandomStringGenerator} with the defined size.
     * @throws NullPointerException if {@code random} is {@code null}.
     * @throws IllegalArgumentException if the size is lower than or equal to {@code 0} or if size
     * is lower than the sum of all {@code minimumQuantity} parameters added when building the
     * generator.
     * @throws IllegalStateException if no {@code with*} method was called or if a
     * {@code withCharacters(String [, int])} and {@code avoidSimilarAsciiCharacters()} cancel each
     * other
     */
    public RandomStringGenerator build(int size, Random random) {
      checkNotNull(random, "random must not be null");
      checkBuild(size);
      return new RandomStringGenerator(this, size, random);
    }

    private void checkBuild(int size) {
      checkArgument(size >= countRequiredCharacters,
          "size (%s) is too low compared to the minimum number of characters (%s)",
          size, countRequiredCharacters);
      checkState(!chars.isEmpty(), "No character definition was added to this builder");

      if (avoidSimilarAsciiCharacters) {
        for (String key : chars.keySet()) {
          checkState(!SIMILAR_ASCII_CHARS.removeFrom(key).isEmpty(),
              "withCharacters(%s) and avoidSimilarAsciiCharacters() cancel each other and may not be called together");
        }
      }
    }
  }
}

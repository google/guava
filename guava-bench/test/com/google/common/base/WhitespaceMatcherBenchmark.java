/*
 * Copyright (C) 2010 The Guava Authors
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

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import java.util.BitSet;
import java.util.Random;

/** Benchmark for the {@link CharMatcher#whitespace} implementation. */
public class WhitespaceMatcherBenchmark {
  private static final int STRING_LENGTH = 10000;

  private static final String OLD_WHITESPACE_TABLE =
      "\u0001\u0000\u00a0\u0000\u0000\u0000\u0000\u0000"
          + "\u0000\u0009\n\u000b\u000c\r\u0000\u0000\u2028\u2029\u0000\u0000\u0000\u0000\u0000"
          + "\u202f\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0020\u0000\u0000\u0000\u0000"
          + "\u0000\u0000\u0000\u0000\u0000\u0000\u3000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
          + "\u0000\u0000\u0000\u0085\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009"
          + "\u200a\u0000\u0000\u0000\u0000\u0000\u205f\u1680\u0000\u0000\u180e\u0000\u0000\u0000";

  public static final CharMatcher OLD_WHITESPACE =
      new CharMatcher() {
        @Override
        public boolean matches(char c) {
          return OLD_WHITESPACE_TABLE.charAt(c % 79) == c;
        }
      };

  @Param private boolean useNew;

  @Param({"20", "50", "80"})
  private int percentMatching;

  private String teststring;
  private CharMatcher matcher;

  @BeforeExperiment
  protected void setUp() {
    BitSet bitSet = new BitSet();
    for (int i = 0; i < OLD_WHITESPACE_TABLE.length(); i++) {
      bitSet.set(OLD_WHITESPACE_TABLE.charAt(i));
    }
    bitSet.clear(0);
    bitSet.clear(1);
    matcher = useNew ? CharMatcher.whitespace() : OLD_WHITESPACE;
    teststring = newTestString(new Random(1), bitSet, percentMatching);
  }

  @Benchmark
  public int countIn(int reps) {
    int result = 0;
    CharMatcher matcher = this.matcher;
    String teststring = this.teststring;
    for (int i = 0; i < reps; i++) {
      result += matcher.countIn(teststring);
    }
    return result;
  }

  @Benchmark
  public int collapseFrom(int reps) {
    int result = 0;
    CharMatcher matcher = this.matcher;
    String teststring = this.teststring;
    for (int i = 0; i < reps; i++) {
      result += System.identityHashCode(matcher.collapseFrom(teststring, ' '));
    }
    return result;
  }

  private static String allMatchingChars(BitSet bitSet) {
    final char[] result = new char[bitSet.cardinality()];
    for (int j = 0, c = bitSet.nextSetBit(0); j < result.length; ++j) {
      result[j] = (char) c;
      c = bitSet.nextSetBit(c + 1);
    }
    return new String(result);
  }

  private static String newTestString(Random random, BitSet bitSet, int percentMatching) {
    final String allMatchingChars = allMatchingChars(bitSet);
    final char[] result = new char[STRING_LENGTH];
    // Fill with matching chars.
    for (int i = 0; i < result.length; i++) {
      result[i] = allMatchingChars.charAt(random.nextInt(allMatchingChars.length()));
    }
    // Replace some of chars by non-matching.
    int remaining = (int) ((100 - percentMatching) * result.length / 100.0 + 0.5);
    while (remaining > 0) {
      final char c = (char) random.nextInt();
      if (bitSet.get(c)) {
        final int pos = random.nextInt(result.length);
        if (bitSet.get(result[pos])) {
          result[pos] = c;
          remaining--;
        }
      }
    }
    return new String(result);
  }
}

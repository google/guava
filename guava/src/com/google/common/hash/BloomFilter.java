/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.hash;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.hash.BloomFilterStrategies.BitArray;

import java.io.Serializable;

/**
 * A Bloom filter for instances of {@code T}. A Bloom filter offers an approximate containment test
 * with one-sided error: if it claims that an element is contained in it, this might be in error,
 * but if it claims that an element is <i>not</i> contained in it, then this is definitely true.
 *
 * <p>If you are unfamiliar with Bloom filters, this nice
 * <a href="http://llimllib.github.com/bloomfilter-tutorial/">tutorial</a> may help you understand
 * how they work.
 *
 *
 * @param <T> the type of instances that the {@code BloomFilter} accepts
 * @author Dimitris Andreou
 * @author Kevin Bourrillion
 * @since 11.0
 */
@Beta
public final class BloomFilter<T> implements Serializable {
  /**
   * A strategy to translate T instances, to {@code numHashFunctions} bit indexes.
   *
   * <p>Implementations should be collections of pure functions (i.e. stateless).
   */
  interface Strategy extends java.io.Serializable {

    /**
     * Sets {@code numHashFunctions} bits of the given bit array, by hashing a user element.
     *
     * <p>Returns whether any bits changed as a result of this operation.
     */
    <T> boolean put(T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bits);

    /**
     * Queries {@code numHashFunctions} bits of the given bit array, by hashing a user element;
     * returns {@code true} if and only if all selected bits are set.
     */
    <T> boolean mightContain(
        T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bits);

    /**
     * Identifier used to encode this strategy, when marshalled as part of a BloomFilter.
     * Only values in the [-128, 127] range are valid for the compact serial form.
     * Non-negative values are reserved for enums defined in BloomFilterStrategies;
     * negative values are reserved for any custom, stateful strategy we may define
     * (e.g. any kind of strategy that would depend on user input).
     */
    int ordinal();
  }

  /** The bit set of the BloomFilter (not necessarily power of 2!)*/
  private final BitArray bits;

  /** Number of hashes per element */
  private final int numHashFunctions;

  /** The funnel to translate Ts to bytes */
  private final Funnel<T> funnel;

  /**
   * The strategy we employ to map an element T to {@code numHashFunctions} bit indexes.
   */
  private final Strategy strategy;

  /**
   * Creates a BloomFilter.
   */
  private BloomFilter(BitArray bits, int numHashFunctions, Funnel<T> funnel,
      Strategy strategy) {
    Preconditions.checkArgument(numHashFunctions > 0, "numHashFunctions zero or negative");
    this.bits = checkNotNull(bits);
    this.numHashFunctions = numHashFunctions;
    this.funnel = checkNotNull(funnel);
    this.strategy = strategy;

    /*
     * This only exists to forbid BFs that cannot use the compact persistent representation.
     * If it ever throws, at a user who was not intending to use that representation, we should
     * reconsider
     */
    if (numHashFunctions > 255) {
      throw new AssertionError("Currently we don't allow BloomFilters that would use more than" +
          "255 hash functions, please contact the guava team");
    }
  }

  /**
   * Creates a new {@code BloomFilter} that's a copy of this instance. The new instance is equal to
   * this instance but shares no mutable state.
   *
   * @since 12.0
   */
  public BloomFilter<T> copy() {
    return new BloomFilter<T>(bits.copy(), numHashFunctions, funnel, strategy);
  }

  /**
   * Returns {@code true} if the element <i>might</i> have been put in this Bloom filter,
   * {@code false} if this is <i>definitely</i> not the case.
   */
  public boolean mightContain(T object) {
    return strategy.mightContain(object, funnel, numHashFunctions, bits);
  }

  /**
   * Puts an element into this {@code BloomFilter}. Ensures that subsequent invocations of
   * {@link #mightContain(Object)} with the same element will always return {@code true}.
   *
   * @return true if the bloom filter's bits changed as a result of this operation. If the bits
   *         changed, this is <i>definitely</i> the first time {@code object} has been added to the
   *         filter. If the bits haven't changed, this <i>might</i> be the first time {@code object}
   *         has been added to the filter. Note that {@code put(t)} always returns the
   *         <i>opposite</i> result to what {@code mightContain(t)} would have returned at the time
   *         it is called."
   * @since 12.0 (present in 11.0 with {@code void} return type})
   */
  public boolean put(T object) {
    return strategy.put(object, funnel, numHashFunctions, bits);
  }

  /**
   * Returns the probability that {@linkplain #mightContain(Object)} will erroneously return
   * {@code true} for an object that has not actually been put in the {@code BloomFilter}.
   *
   * <p>Ideally, this number should be close to the {@code falsePositiveProbability} parameter
   * passed in {@linkplain #create(Funnel, int, double)}, or smaller. If it is
   * significantly higher, it is usually the case that too many elements (more than
   * expected) have been put in the {@code BloomFilter}, degenerating it.
   */
  public double expectedFalsePositiveProbability() {
    return Math.pow((double) bits.bitCount() / bits.size(), numHashFunctions);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation uses reference equality to compare funnels.
   */
  @Override public boolean equals(Object o) {
    if (o instanceof BloomFilter) {
      BloomFilter<?> that = (BloomFilter<?>) o;
      return this.numHashFunctions == that.numHashFunctions
          && this.bits.equals(that.bits)
          && this.funnel == that.funnel
          && this.strategy == that.strategy;
    }
    return false;
  }

  @Override public int hashCode() {
    return bits.hashCode();
  }

  @VisibleForTesting int getHashCount() {
    return numHashFunctions;
  }

  /**
   * Creates a {@code Builder} of a {@link BloomFilter BloomFilter<T>}, with the expected number
   * of insertions and expected false positive probability.
   *
   * <p>Note that overflowing a {@code BloomFilter} with significantly more elements
   * than specified, will result in its saturation, and a sharp deterioration of its
   * false positive probability.
   *
   * <p>The constructed {@code BloomFilter<T>} will be serializable if the provided
   * {@code Funnel<T>} is.
   *
   * <p>It is recommended the funnel is implemented as a Java enum. This has the benefit of ensuring
   * proper serialization and deserialization, which is important since {@link #equals} also relies
   * on object identity of funnels.
   *
   * @param funnel the funnel of T's that the constructed {@code BloomFilter<T>} will use
   * @param expectedInsertions the number of expected insertions to the constructed
   *        {@code BloomFilter<T>}; must be positive
   * @param falsePositiveProbability the desired false positive probability (must be positive and
   *        less than 1.0)
   * @return a {@code BloomFilter}
   */
  public static <T> BloomFilter<T> create(Funnel<T> funnel, int expectedInsertions /* n */,
      double falsePositiveProbability) {
    checkNotNull(funnel);
    checkArgument(expectedInsertions >= 0, "Expected insertions cannot be negative");
    checkArgument(falsePositiveProbability > 0.0 & falsePositiveProbability < 1.0,
        "False positive probability in (0.0, 1.0)");
    if (expectedInsertions == 0) {
      expectedInsertions = 1;
    }
    /*
     * andreou: I wanted to put a warning in the javadoc about tiny fpp values,
     * since the resulting size is proportional to -log(p), but there is not
     * much of a point after all, e.g. optimalM(1000, 0.0000000000000001) = 76680
     * which is less that 10kb. Who cares!
     */
    int numBits = optimalNumOfBits(expectedInsertions, falsePositiveProbability);
    int numHashFunctions = optimalNumOfHashFunctions(expectedInsertions, numBits);
    return new BloomFilter<T>(new BitArray(numBits), numHashFunctions, funnel,
        BloomFilterStrategies.MURMUR128_MITZ_32);
  }

  /**
   * Creates a {@code Builder} of a {@link BloomFilter BloomFilter<T>}, with the expected number
   * of insertions, and a default expected false positive probability of 3%.
   *
   * <p>Note that overflowing a {@code BloomFilter} with significantly more elements
   * than specified, will result in its saturation, and a sharp deterioration of its
   * false positive probability.
   *
   * <p>The constructed {@code BloomFilter<T>} will be serializable if the provided
   * {@code Funnel<T>} is.
   *
   * @param funnel the funnel of T's that the constructed {@code BloomFilter<T>} will use
   * @param expectedInsertions the number of expected insertions to the constructed
   *        {@code BloomFilter<T>}; must be positive
   * @return a {@code BloomFilter}
   */
  public static <T> BloomFilter<T> create(Funnel<T> funnel, int expectedInsertions /* n */) {
    return create(funnel, expectedInsertions, 0.03); // FYI, for 3%, we always get 5 hash functions
  }

  /*
   * Cheat sheet:
   *
   * m: total bits
   * n: expected insertions
   * b: m/n, bits per insertion

   * p: expected false positive probability
   *
   * 1) Optimal k = b * ln2
   * 2) p = (1 - e ^ (-kn/m))^k
   * 3) For optimal k: p = 2 ^ (-k) ~= 0.6185^b
   * 4) For optimal k: m = -nlnp / ((ln2) ^ 2)
   */

  private static final double LN2 = Math.log(2);
  private static final double LN2_SQUARED = LN2 * LN2;

  /**
   * Computes the optimal k (number of hashes per element inserted in Bloom filter), given the
   * expected insertions and total number of bits in the Bloom filter.
   *
   * See http://en.wikipedia.org/wiki/File:Bloom_filter_fp_probability.svg for the formula.
   *
   * @param n expected insertions (must be positive)
   * @param m total number of bits in Bloom filter (must be positive)
   */
  @VisibleForTesting static int optimalNumOfHashFunctions(int n, int m) {
    return Math.max(1, (int) Math.round(m / n * LN2));
  }

  /**
   * Computes m (total bits of Bloom filter) which is expected to achieve, for the specified
   * expected insertions, the required false positive probability.
   *
   * See http://en.wikipedia.org/wiki/Bloom_filter#Probability_of_false_positives for the formula.
   *
   * @param n expected insertions (must be positive)
   * @param p false positive rate (must be 0 < p < 1)
   */
  @VisibleForTesting static int optimalNumOfBits(int n, double p) {
    return (int) (-n * Math.log(p) / LN2_SQUARED);
  }

  private Object writeReplace() {
    return new SerialForm<T>(this);
  }

  private static class SerialForm<T> implements Serializable {
    final long[] data;
    final int numHashFunctions;
    final Funnel<T> funnel;
    final Strategy strategy;

    SerialForm(BloomFilter<T> bf) {
      this.data = bf.bits.data;
      this.numHashFunctions = bf.numHashFunctions;
      this.funnel = bf.funnel;
      this.strategy = bf.strategy;
    }
    Object readResolve() {
      return new BloomFilter<T>(new BitArray(data), numHashFunctions, funnel, strategy);
    }
    private static final long serialVersionUID = 1;
  }
}

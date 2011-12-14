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
import static com.google.common.math.IntMath.log2;
import static java.lang.Math.max;
import static java.math.RoundingMode.CEILING;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.hash.HashCodes.HashCodeSlicer;

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
 * @param <T> the type of instances that the {@code BloomFilter} accepts
 * @author Kevin Bourrillion
 * @author Dimitris Andreou
 * @since 11.0
 */
@Beta
public final class BloomFilter<T> implements Serializable {
  /** A power of two sized bit set */
  private final BitArray bits;
  
  /** Number of bits required to index a bit out of {@code bits} */
  private final int hashBitsPerSlice;
  
  /** Number of hashes per element */ 
  private final int numHashFunctions;
  
  /** The funnel to translate T's to bytes */
  private final Funnel<T> funnel;
  
  /** The HashFunction that generates as many bits as this BloomFilter needs */
  private final HashFunction hashFunction;

  /**
   * Creates a BloomFilter. 
   */
  private BloomFilter(BitArray bits, int numHashFunctions, Funnel<T> funnel,
      HashFunction hashFunction) {
    Preconditions.checkArgument(numHashFunctions > 0, "numHashFunctions zero or negative");
    this.bits = checkNotNull(bits);
    this.numHashFunctions = numHashFunctions;
    this.funnel = checkNotNull(funnel);
    this.hashFunction = checkNotNull(hashFunction);
    this.hashBitsPerSlice = log2(max(bits.size(), Long.SIZE /* minimum capacity */ ), CEILING);
  }
  
  /**
   * Returns {@code true} if the element <i>might</i> have been put in this Bloom filter, 
   * {@code false} if this is <i>definitely</i> not the case. 
   */
  public boolean mightContain(T instance) {
    HashCodeSlicer slicer = HashCodes.slice(
        hashFunction.newHasher().putObject(instance, funnel).hash(), hashBitsPerSlice);
    for (int i = 0; i < numHashFunctions; i++) {
      if (!bits.get(slicer.nextSlice())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Puts an element into this {@code BloomFilter}. Ensures that subsequent invocations of 
   * {@link #mightContain(Object)} with the same element will always return {@code true}.
   */
  public void put(T instance) {
    HashCodeSlicer slicer = HashCodes.slice(
        hashFunction.newHasher().putObject(instance, funnel).hash(), hashBitsPerSlice);
    for (int i = 0; i < numHashFunctions; i++) {
      int nextSlice = slicer.nextSlice(); 
      bits.set(nextSlice);
    }
  }
  
  @VisibleForTesting int getHashCount() {
    return numHashFunctions;
  }
  
  @VisibleForTesting double computeExpectedFalsePositiveRate(int insertions) {
    return Math.pow(
        1 - Math.exp(-numHashFunctions * ((double) insertions / (bits.size()))),
        numHashFunctions);
  }
  
  // This little gem is kindly offered by kevinb
  private static class BitArray {
    final long[] data;
    
    BitArray(int bits) {
      this(new long[bits >> 6]);
    }
    
    // Used by serialization
    BitArray(long[] data) {
      checkArgument(data.length > 0, "data length is zero!");
      this.data = data;
    }
    
    void set(int index) {
      data[index >> 6] |= (1L << index);
    }
    
    boolean get(int index) {
      return (data[index >> 6] & (1L << index)) != 0;
    }
    
    /** Number of bits */
    int size() {
      return data.length * Long.SIZE;
    }
  }
  
  /*
   * Cheat sheet for the factories:
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
   * 
   * I expect the user to provide "n", and then one of {m,b,p} is needed.
   * Providing both (n, m) and (n, b) would be confusing, so I go for the 2nd.
   */
  
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
   * @param funnel the funnel of T's that the constructed {@code BloomFilter<T>} will use
   * @param expectedInsertions the number of expected insertions to the constructed 
   *        {@code BloomFilter<T>}; must be positive
   * @param falsePositiveProbability the desired false positive probability (must be positive and 
   *        less than 1.0)
   * @return a {@code Builder}
   */
  public static <T> BloomFilter<T> create(Funnel<T> funnel, int expectedInsertions /* n */,
      double falsePositiveProbability) {
    checkNotNull(funnel);
    checkArgument(expectedInsertions > 0, "Expected insertions must be positive");
    checkArgument(falsePositiveProbability > 0.0 & falsePositiveProbability < 1.0,
        "False positive probability in (0.0, 1.0)");
    /* 
     * andreou: I wanted to put a warning in the javadoc about tiny fpp values,
     * since the resulting size is proportional to -log(p), but there is not
     * much of a point after all, e.g. optimalM(1000, 0.0000000000000001) = 76680
     * which is less that 10kb. Who cares!
     */
    int m = optimalM(expectedInsertions, falsePositiveProbability);
    int k = optimalK(expectedInsertions, m);
    
    BitArray bits = new BitArray(1 << log2(max(m, Long.SIZE /* minimum capacity */ ), CEILING));
    HashFunction hashFunction = BloomFilterStrategies.From128ToN.withBits(
        bits.size() * k, Hashing.murmur3_128());
    return new BloomFilter<T>(bits, k, funnel, hashFunction);
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
   * @return a {@code Builder}
   */
  public static <T> BloomFilter<T> create(Funnel<T> funnel, int expectedInsertions /* n */) {
    return create(funnel, expectedInsertions, 0.03); // FYI, for 3%, we always get 5 hash functions 
  }
  
  
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
  @VisibleForTesting static int optimalK(int n, int m) {
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
  @VisibleForTesting static int optimalM(int n, double p) {
    return (int) (-n * Math.log(p) / LN2_SQUARED);
  }
  
  private Object writeReplace() {
    return new SerialForm<T>(this);
  }
  
  private static class SerialForm<T> implements Serializable {
    final long[] data;
    final int numHashFunctions;
    final Funnel<T> funnel;
    final HashFunction hashFunction;
    
    SerialForm(BloomFilter<T> bf) {
      this.data = bf.bits.data;
      this.numHashFunctions = bf.numHashFunctions;
      this.funnel = bf.funnel;
      this.hashFunction = bf.hashFunction;
    }
    Object readResolve() {
      return new BloomFilter<T>(new BitArray(data), numHashFunctions, funnel, hashFunction);
    }
    private static final long serialVersionUID = 0;
  }
}

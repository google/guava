// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.common.hash;

import com.google.common.primitives.Ints;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

import java.util.Random;

/**
 * Tests for SimpleGenericBloomFilter and derived BloomFilter views.
 * 
 * @author andreou@google.com (Dimitris Andreou)
 */
public class BloomFilterTest extends TestCase {
  /**
   * Sanity checking with many combinations of false positive rates and expected insertions
   */
  public void testBasic() {
    for (double fpr = 0.0000001; fpr < 0.1; fpr *= 10) {
      for (int expectedInsertions = 1; expectedInsertions <= 10000; expectedInsertions *= 10) {
        checkSanity(BloomFilter.create(HashTestUtils.BAD_FUNNEL, expectedInsertions, fpr));
      }
    }
  }
  
  /**
   * Tests that we never get an optimal hashes number of zero. 
   */
  public void testOptimalHashes() {
    for (int n = 1; n < 1000; n++) {
      for (int m = 0; m < 1000; m++) {
        assertTrue(BloomFilter.optimalNumOfHashFunctions(n, m) > 0);
      }
    }
  }
  
  /**
   * Tests that we always get a non-negative optimal size.  
   */
  public void testOptimalSize() {
    for (int n = 1; n < 1000; n++) {
      for (double fpp = Double.MIN_VALUE; fpp < 1.0; fpp += 0.001) {
        assertTrue(BloomFilter.optimalNumOfBits(n, fpp) >= 0);
      }
    }
    
    // some random values
    Random random = new Random(0); 
    for (int repeats = 0; repeats < 10000; repeats++) {
      assertTrue(BloomFilter.optimalNumOfBits(random.nextInt(1 << 16), random.nextDouble()) >= 0);
    }
    
    // and some crazy values
    assertEquals(Integer.MAX_VALUE, BloomFilter.optimalNumOfBits(
        Integer.MAX_VALUE, Double.MIN_VALUE));
  }
  
  private void checkSanity(BloomFilter<Object> bf) {
    assertFalse(bf.mightContain(new Object()));
    for (int i = 0; i < 100; i++) {
      Object o = new Object();
      bf.put(o);
      assertTrue(bf.mightContain(o));
    }
  }
  
  public void testSerialization() {
    BloomFilter<byte[]> bf = BloomFilter.create(Funnels.byteArrayFunnel(), 100);
    for (int i = 0; i < 10; i++) {
      bf.put(Ints.toByteArray(i));
    }
    
    bf = SerializableTester.reserialize(bf);
    for (int i = 0; i < 10; i++) {
      assertTrue(bf.mightContain(Ints.toByteArray(i)));
    }
  }
}

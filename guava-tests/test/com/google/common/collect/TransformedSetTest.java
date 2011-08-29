/*
 * Copyright (C) 2011 The Guava Authors
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

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.SetFeature;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link Sets#transform(Set, Sets.InvertibleFunction)}.
 * 
 * @author Dimitris Andreou
 */
@GwtCompatible(emulated = true)
public class TransformedSetTest extends TestCase {
  // Negates each integer. This is a true bijection, even considering MIN_VALUE
  private static final Sets.InvertibleFunction<Integer, Integer> integerBijection = 
      new Sets.InvertibleFunction<Integer, Integer>() {
        @Override public Integer apply(Integer integer) {
          return integer != null ? -integer : null;
        }

        @Override
        public Integer invert(Integer integer) {
          return integer != null ? -integer : null;
        }
      };

  @GwtIncompatible("suite")
  public static TestSuite suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(SetTestSuiteBuilder
        .using(new TransformedIntegerSetGenerator())
        .named("TransformedSet")
        .withFeatures(
        SetFeature.GENERAL_PURPOSE,
        CollectionFeature.ALLOWS_NULL_VALUES,
        CollectionSize.SEVERAL)
      .createTestSuite());
    return suite;
  }
  
  public void testSimpleCases() {
    Set<Integer> original = Sets.newHashSet(0, 1, 2, 3);
    Set<Integer> transformed = Sets.transform(original, integerBijection);
    
    assertEquals(ImmutableSet.of(0, -1, -2, -3), transformed);
    
    // adding/removing to the original, see if transformed is affected
    assertTrue(original.remove(2));
    assertTrue(original.add(4));
    assertEquals(ImmutableSet.of(0,  1,  3,  4), original);
    assertEquals(ImmutableSet.of(0, -1, -3, -4), transformed);
    
    // adding/removing to the transformed, see if original is affected
    assertTrue(transformed.remove(-1));
    assertTrue(transformed.add(-5));
    assertEquals(ImmutableSet.of(0, -3, -4, -5), transformed);
    assertEquals(ImmutableSet.of(0,  3,  4,  5), original);
    
    // redoing the same actions as above; no effect
    assertFalse(transformed.remove(-1));
    assertFalse(transformed.add(-5));
    
    // they should always have the same size
    assertEquals(original.size(), transformed.size());
    
    transformed.clear();
    assertTrue(original.isEmpty());
    assertTrue(transformed.isEmpty());
  }

  public static class TransformedIntegerSetGenerator implements TestSetGenerator<Integer> {
    @Override public Set<Integer> create(Object... elements) {
      // Would use Collections#checkedCollection, but I get:
      // [ERROR] The method checkedCollection(Collection, Class<Integer>)
      // is undefined for the type Collections
      @SuppressWarnings("unchecked")  
      Iterable<Integer> integers = (Iterable) Arrays.asList(elements);
      
      // I invert these before adding, so that the transformed set will have
      // the expected elements themselves, not their image under the bijection
      Set<Integer> invertedIntegers = Sets.newHashSet(Iterables.transform(integers,
          integerBijection.inverse()));
      return Sets.transform(invertedIntegers, integerBijection);
    }

    @Override public Integer[] createArray(int length) {
      return new Integer[length];
    }

    @Override public SampleElements<Integer> samples() {
      return new SampleElements<Integer>(-1, 0, 1, 2, 3);
    }
    
    @Override public Iterable<Integer> order(List<Integer> insertionOrder) {
      throw new AssertionError();
    }
  }
}

/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.google.MultisetTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringMultisetGenerator;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/**
 * Unit test for {@link AbstractMultiset}.
 *
 * @author Kevin Bourrillion
 * @author Louis Wasserman
 */
@SuppressWarnings("serial") // No serialization is used in this test
@GwtCompatible(emulated = true)
public class SimpleAbstractMultisetTest extends TestCase {
  @GwtIncompatible // suite
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(SimpleAbstractMultisetTest.class);
    suite.addTest(
        MultisetTestSuiteBuilder.using(
                new TestStringMultisetGenerator() {
                  @Override
                  protected Multiset<String> create(String[] elements) {
                    Multiset<String> ms = new NoRemoveMultiset<>();
                    Collections.addAll(ms, elements);
                    return ms;
                  }
                })
            .named("NoRemoveMultiset")
            .withFeatures(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.SUPPORTS_ADD)
            .createTestSuite());
    return suite;
  }

  public void testFastAddAllMultiset() {
    final AtomicInteger addCalls = new AtomicInteger();
    Multiset<String> multiset =
        new NoRemoveMultiset<String>() {
          @Override
          public int add(String element, int occurrences) {
            addCalls.incrementAndGet();
            return super.add(element, occurrences);
          }
        };
    ImmutableMultiset<String> adds =
        new ImmutableMultiset.Builder<String>().addCopies("x", 10).build();
    multiset.addAll(adds);
    assertEquals(1, addCalls.get());
  }

  public void testRemoveUnsupported() {
    Multiset<String> multiset = new NoRemoveMultiset<>();
    multiset.add("a");
    try {
      multiset.remove("a");
      fail();
    } catch (UnsupportedOperationException expected) {
    }
    assertTrue(multiset.contains("a"));
  }

  private static class NoRemoveMultiset<E> extends AbstractMultiset<E> implements Serializable {
    final Map<E, Integer> backingMap = Maps.newHashMap();

    @Override
    public int size() {
      return Multisets.linearTimeSizeImpl(this);
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int count(@NullableDecl Object element) {
      for (Entry<E> entry : entrySet()) {
        if (Objects.equal(entry.getElement(), element)) {
          return entry.getCount();
        }
      }
      return 0;
    }

    @Override
    public int add(@NullableDecl E element, int occurrences) {
      checkArgument(occurrences >= 0);
      Integer frequency = backingMap.get(element);
      if (frequency == null) {
        frequency = 0;
      }
      if (occurrences == 0) {
        return frequency;
      }
      checkArgument(occurrences <= Integer.MAX_VALUE - frequency);
      backingMap.put(element, frequency + occurrences);
      return frequency;
    }

    @Override
    Iterator<E> elementIterator() {
      return Multisets.elementIterator(entryIterator());
    }

    @Override
    Iterator<Entry<E>> entryIterator() {
      final Iterator<Map.Entry<E, Integer>> backingEntries = backingMap.entrySet().iterator();
      return new UnmodifiableIterator<Multiset.Entry<E>>() {
        @Override
        public boolean hasNext() {
          return backingEntries.hasNext();
        }

        @Override
        public Multiset.Entry<E> next() {
          final Map.Entry<E, Integer> mapEntry = backingEntries.next();
          return new Multisets.AbstractEntry<E>() {
            @Override
            public E getElement() {
              return mapEntry.getKey();
            }

            @Override
            public int getCount() {
              Integer frequency = backingMap.get(getElement());
              return (frequency == null) ? 0 : frequency;
            }
          };
        }
      };
    }

    @Override
    public Iterator<E> iterator() {
      return Multisets.iteratorImpl(this);
    }

    @Override
    int distinctElements() {
      return backingMap.size();
    }
  }
}

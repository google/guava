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

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.primitives.Ints;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Package up sample data for common collections benchmarking.
 *
 * @author Nicholaus Shupe
 */
class CollectionBenchmarkSampleData {
  private final boolean isUserTypeFast;
  private final SpecialRandom random;
  private final double hitRate;
  private final int size;

  private final Set<Element> valuesInSet;
  private final Element[] queries;

  CollectionBenchmarkSampleData(int size) {
    this(true, new SpecialRandom(), 1.0, size);
  }

  CollectionBenchmarkSampleData(
      boolean isUserTypeFast, SpecialRandom random, double hitRate, int size) {
    this.isUserTypeFast = isUserTypeFast;
    this.random = checkNotNull(random);
    this.hitRate = hitRate;
    this.size = size;

    this.valuesInSet = createData();
    this.queries = createQueries(valuesInSet, 1024);
  }

  Set<Element> getValuesInSet() {
    return valuesInSet;
  }

  Element[] getQueries() {
    return queries;
  }

  private Element[] createQueries(Set<Element> elementsInSet, int numQueries) {
    List<Element> queryList = Lists.newArrayListWithCapacity(numQueries);

    int numGoodQueries = (int) (numQueries * hitRate + 0.5);

    // add good queries
    int size = elementsInSet.size();
    if (size > 0) {
      int minCopiesOfEachGoodQuery = numGoodQueries / size;
      int extras = numGoodQueries % size;

      for (int i = 0; i < minCopiesOfEachGoodQuery; i++) {
        queryList.addAll(elementsInSet);
      }
      List<Element> tmp = Lists.newArrayList(elementsInSet);
      Collections.shuffle(tmp, random);
      queryList.addAll(tmp.subList(0, extras));
    }

    // now add bad queries
    while (queryList.size() < numQueries) {
      Element candidate = newElement();
      if (!elementsInSet.contains(candidate)) {
        queryList.add(candidate);
      }
    }
    Collections.shuffle(queryList, random);
    return queryList.toArray(new Element[0]);
  }

  private Set<Element> createData() {
    Set<Element> set = Sets.newHashSetWithExpectedSize(size);
    while (set.size() < size) {
      set.add(newElement());
    }
    return set;
  }

  private Element newElement() {
    int value = random.nextInt();
    return isUserTypeFast ? new Element(value) : new SlowElement(value);
  }

  static class Element implements Comparable<Element> {
    final int hash;

    Element(int hash) {
      this.hash = hash;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return this == obj || (obj instanceof Element && ((Element) obj).hash == hash);
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public int compareTo(Element that) {
      return Ints.compare(hash, that.hash);
    }

    @Override
    public String toString() {
      return String.valueOf(hash);
    }
  }

  static class SlowElement extends Element {
    SlowElement(int hash) {
      super(hash);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      return slowItDown() != 1 && super.equals(obj);
    }

    @Override
    public int hashCode() {
      return slowItDown() + hash;
    }

    @Override
    public int compareTo(Element e) {
      int x = slowItDown();
      return x + super.compareTo(e) - x; // silly attempt to prevent opt
    }

    static int slowItDown() {
      int result = 0;
      for (int i = 1; i <= 1000; i++) {
        result += i;
      }
      return result;
    }
  }
}

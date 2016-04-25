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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.MapMaker.ComputingMapAdapter;
import com.google.common.collect.MapMakerInternalMap.ReferenceEntry;
import com.google.common.collect.MapMakerInternalMap.Segment;
import com.google.common.collect.MapMakerInternalMapTest.DummyEntry;
import com.google.common.collect.MapMakerInternalMapTest.DummyValueReference;
import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * @author Charles Fry
 */
public class ComputingConcurrentHashMapTest extends TestCase {

  private static <K, V> ComputingConcurrentHashMap<K, V> makeComputingMap(
      MapMaker maker, Function<? super K, ? extends V> computingFunction) {
    return new ComputingConcurrentHashMap<K, V>(
        maker, computingFunction);
  }

  private static <K, V> ComputingMapAdapter<K, V> makeAdaptedMap(
      MapMaker maker, Function<? super K, ? extends V> computingFunction) {
    return new ComputingMapAdapter<K, V>(
        maker, computingFunction);
  }

  private static MapMaker createMapMaker() {
    MapMaker maker = new MapMaker();
    maker.useCustomMap = true;
    return maker;
  }

  // constructor tests

  public void testComputingFunction() {
    Function<Object, Object> computingFunction = Functions.identity();
    ComputingConcurrentHashMap<Object, Object> map =
        makeComputingMap(createMapMaker(), computingFunction);
    assertSame(computingFunction, map.computingFunction);
  }

  // computation tests

  public void testCompute() throws ExecutionException {
    CountingFunction computingFunction = new CountingFunction();
    ComputingConcurrentHashMap<Object, Object> map =
        makeComputingMap(createMapMaker(), computingFunction);
    assertEquals(0, computingFunction.getCount());

    Object key = new Object();
    Object value = map.getOrCompute(key);
    assertEquals(1, computingFunction.getCount());
    assertEquals(value, map.getOrCompute(key));
    assertEquals(1, computingFunction.getCount());
  }

  public void testComputeNull() {
    Function<Object, Object> computingFunction = new ConstantLoader<Object, Object>(null);
    ComputingMapAdapter<Object, Object> map = makeAdaptedMap(createMapMaker(), computingFunction);
    try {
      map.get(new Object());
      fail();
    } catch (NullPointerException expected) {}
  }

  public void testComputeExistingEntry() throws ExecutionException {
    CountingFunction computingFunction = new CountingFunction();
    ComputingConcurrentHashMap<Object, Object> map =
        makeComputingMap(createMapMaker(), computingFunction);
    assertEquals(0, computingFunction.getCount());

    Object key = new Object();
    Object value = new Object();
    map.put(key, value);

    assertEquals(value, map.getOrCompute(key));
    assertEquals(0, computingFunction.getCount());
  }

  public void testComputePartiallyCollectedKey() throws ExecutionException {
    MapMaker maker = createMapMaker().concurrencyLevel(1);
    CountingFunction computingFunction = new CountingFunction();
    ComputingConcurrentHashMap<Object, Object> map = makeComputingMap(maker, computingFunction);
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(0, computingFunction.getCount());

    Object key = new Object();
    int hash = map.hash(key);
    Object value = new Object();
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> valueRef = DummyValueReference.create(value, entry);
    entry.setValueReference(valueRef);
    table.set(index, entry);
    segment.count++;

    assertSame(value, map.getOrCompute(key));
    assertEquals(0, computingFunction.getCount());
    assertEquals(1, segment.count);

    entry.clearKey();
    assertNotSame(value, map.getOrCompute(key));
    assertEquals(1, computingFunction.getCount());
    assertEquals(2, segment.count);
  }

  public void testComputePartiallyCollectedValue() throws ExecutionException {
    MapMaker maker = createMapMaker().concurrencyLevel(1);
    CountingFunction computingFunction = new CountingFunction();
    ComputingConcurrentHashMap<Object, Object> map = makeComputingMap(maker, computingFunction);
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(0, computingFunction.getCount());

    Object key = new Object();
    int hash = map.hash(key);
    Object value = new Object();
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> valueRef = DummyValueReference.create(value, entry);
    entry.setValueReference(valueRef);
    table.set(index, entry);
    segment.count++;

    assertSame(value, map.getOrCompute(key));
    assertEquals(0, computingFunction.getCount());
    assertEquals(1, segment.count);

    valueRef.clear(null);
    assertNotSame(value, map.getOrCompute(key));
    assertEquals(1, computingFunction.getCount());
    assertEquals(1, segment.count);
  }

  public void testRemovalListener_replaced() {
    // TODO(user): May be a good candidate to play with the MultithreadedTestCase
    final CountDownLatch startSignal = new CountDownLatch(1);
    final CountDownLatch computingSignal = new CountDownLatch(1);
    final CountDownLatch doneSignal = new CountDownLatch(1);
    final Object computedObject = new Object();

    Function<Object, Object> computingFunction = new Function<Object, Object>() {
      @Override
      public Object apply(Object key) {
        computingSignal.countDown();
        try {
          startSignal.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        return computedObject;
      }
    };

    MapMaker maker = (MapMaker) createMapMaker();
    final ComputingConcurrentHashMap<Object, Object> map =
        makeComputingMap(maker, computingFunction);

    final Object one = new Object();
    final Object two = new Object();
    final Object three = new Object();

    new Thread() {
      @Override
      public void run() {
        try {
          map.getOrCompute(one);
        } catch (ExecutionException e) {
          throw new RuntimeException(e);
        }
        doneSignal.countDown();
      }
    }.start();

    try {
      computingSignal.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    map.put(one, two);
    startSignal.countDown();

    try {
      doneSignal.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    assertNotNull(map.putIfAbsent(one, three)); // force notifications
  }

  // computing functions

  private static class CountingFunction implements Function<Object, Object> {
    private final AtomicInteger count = new AtomicInteger();

    @Override
    public Object apply(Object from) {
      count.incrementAndGet();
      return new Object();
    }

    public int getCount() {
      return count.get();
    }
  }

  public void testNullParameters() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    Function<Object, Object> computingFunction = new IdentityLoader<Object>();
    tester.testAllPublicInstanceMethods(makeComputingMap(createMapMaker(), computingFunction));
  }

  static final class ConstantLoader<K, V> implements Function<K, V> {
    private final V constant;

    public ConstantLoader(V constant) {
      this.constant = constant;
    }

    @Override
    public V apply(K key) {
      return constant;
    }
  }

  static final class IdentityLoader<T> implements Function<T, T> {
    @Override
    public T apply(T key) {
      return key;
    }
  }

}

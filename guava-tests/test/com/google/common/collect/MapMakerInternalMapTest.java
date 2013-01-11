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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.MapMakerInternalMap.DISCARDING_QUEUE;
import static com.google.common.collect.MapMakerInternalMap.DRAIN_THRESHOLD;
import static com.google.common.collect.MapMakerInternalMap.nullEntry;
import static com.google.common.collect.MapMakerInternalMap.unset;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Equivalence;
import com.google.common.base.Ticker;
import com.google.common.collect.MapMaker.RemovalCause;
import com.google.common.collect.MapMaker.RemovalListener;
import com.google.common.collect.MapMaker.RemovalNotification;
import com.google.common.collect.MapMakerInternalMap.EntryFactory;
import com.google.common.collect.MapMakerInternalMap.ReferenceEntry;
import com.google.common.collect.MapMakerInternalMap.Segment;
import com.google.common.collect.MapMakerInternalMap.Strength;
import com.google.common.collect.MapMakerInternalMap.ValueReference;
import com.google.common.testing.NullPointerTester;

import junit.framework.TestCase;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * @author Charles Fry
 */
@SuppressWarnings("deprecation") // many tests of deprecated methods
public class MapMakerInternalMapTest extends TestCase {

  static final int SMALL_MAX_SIZE = DRAIN_THRESHOLD * 5;

  private static <K, V> MapMakerInternalMap<K, V> makeMap(GenericMapMaker<K, V> maker) {
    return new MapMakerInternalMap<K, V>((MapMaker) maker);
  }

  private static <K, V> MapMakerInternalMap<K, V> makeMap(MapMaker maker) {
    return new MapMakerInternalMap<K, V>(maker);
  }

  private static MapMaker createMapMaker() {
    MapMaker maker = new MapMaker();
    maker.useCustomMap = true;
    return maker;
  }

  // constructor tests

  public void testDefaults() {
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker());

    assertSame(Strength.STRONG, map.keyStrength);
    assertSame(Strength.STRONG, map.valueStrength);
    assertSame(map.keyStrength.defaultEquivalence(), map.keyEquivalence);
    assertSame(map.valueStrength.defaultEquivalence(), map.valueEquivalence);

    assertEquals(0, map.expireAfterAccessNanos);
    assertEquals(0, map.expireAfterWriteNanos);
    assertEquals(MapMaker.UNSET_INT, map.maximumSize);

    assertSame(EntryFactory.STRONG, map.entryFactory);
    assertSame(MapMaker.NullListener.INSTANCE, map.removalListener);
    assertSame(DISCARDING_QUEUE, map.removalNotificationQueue);
    assertSame(Ticker.systemTicker(), map.ticker);

    assertEquals(4, map.concurrencyLevel);

    // concurrency level
    assertEquals(4, map.segments.length);
    // initial capacity / concurrency level
    assertEquals(16 / map.segments.length, map.segments[0].table.length());

    assertFalse(map.evictsBySize());
    assertFalse(map.expires());
    assertFalse(map.expiresAfterWrite());
    assertFalse(map.expiresAfterAccess());
  }

  public void testSetKeyEquivalence() {
    Equivalence<Object> testEquivalence = new Equivalence<Object>() {
      @Override
      protected boolean doEquivalent(Object a, Object b) {
        return false;
      }

      @Override
      protected int doHash(Object t) {
        return 0;
      }
    };

    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().keyEquivalence(testEquivalence));
    assertSame(testEquivalence, map.keyEquivalence);
    assertSame(map.valueStrength.defaultEquivalence(), map.valueEquivalence);
  }

  public void testSetConcurrencyLevel() {
    // round up to nearest power of two

    checkConcurrencyLevel(1, 1);
    checkConcurrencyLevel(2, 2);
    checkConcurrencyLevel(3, 4);
    checkConcurrencyLevel(4, 4);
    checkConcurrencyLevel(5, 8);
    checkConcurrencyLevel(6, 8);
    checkConcurrencyLevel(7, 8);
    checkConcurrencyLevel(8, 8);
  }

  private static void checkConcurrencyLevel(int concurrencyLevel, int segmentCount) {
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().concurrencyLevel(concurrencyLevel));
    assertEquals(segmentCount, map.segments.length);
  }

  public void testSetInitialCapacity() {
    // share capacity over each segment, then round up to nearest power of two

    checkInitialCapacity(1, 0, 1);
    checkInitialCapacity(1, 1, 1);
    checkInitialCapacity(1, 2, 2);
    checkInitialCapacity(1, 3, 4);
    checkInitialCapacity(1, 4, 4);
    checkInitialCapacity(1, 5, 8);
    checkInitialCapacity(1, 6, 8);
    checkInitialCapacity(1, 7, 8);
    checkInitialCapacity(1, 8, 8);

    checkInitialCapacity(2, 0, 1);
    checkInitialCapacity(2, 1, 1);
    checkInitialCapacity(2, 2, 1);
    checkInitialCapacity(2, 3, 2);
    checkInitialCapacity(2, 4, 2);
    checkInitialCapacity(2, 5, 4);
    checkInitialCapacity(2, 6, 4);
    checkInitialCapacity(2, 7, 4);
    checkInitialCapacity(2, 8, 4);

    checkInitialCapacity(4, 0, 1);
    checkInitialCapacity(4, 1, 1);
    checkInitialCapacity(4, 2, 1);
    checkInitialCapacity(4, 3, 1);
    checkInitialCapacity(4, 4, 1);
    checkInitialCapacity(4, 5, 2);
    checkInitialCapacity(4, 6, 2);
    checkInitialCapacity(4, 7, 2);
    checkInitialCapacity(4, 8, 2);
  }

  private static void checkInitialCapacity(
      int concurrencyLevel, int initialCapacity, int segmentSize) {
    MapMakerInternalMap<Object, Object> map = makeMap(
        createMapMaker().concurrencyLevel(concurrencyLevel).initialCapacity(initialCapacity));
    for (int i = 0; i < map.segments.length; i++) {
      assertEquals(segmentSize, map.segments[i].table.length());
    }
  }

  public void testSetMaximumSize() {
    // vary maximumSize wrt concurrencyLevel

    for (int maxSize = 1; maxSize < 8; maxSize++) {
      checkMaximumSize(1, 8, maxSize);
      checkMaximumSize(2, 8, maxSize);
      checkMaximumSize(4, 8, maxSize);
      checkMaximumSize(8, 8, maxSize);
    }

    checkMaximumSize(1, 8, Integer.MAX_VALUE);
    checkMaximumSize(2, 8, Integer.MAX_VALUE);
    checkMaximumSize(4, 8, Integer.MAX_VALUE);
    checkMaximumSize(8, 8, Integer.MAX_VALUE);

    // vary initial capacity wrt maximumSize

    for (int capacity = 0; capacity < 8; capacity++) {
      checkMaximumSize(1, capacity, 4);
      checkMaximumSize(2, capacity, 4);
      checkMaximumSize(4, capacity, 4);
      checkMaximumSize(8, capacity, 4);
    }
  }

  private static void checkMaximumSize(int concurrencyLevel, int initialCapacity, int maxSize) {
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker()
        .concurrencyLevel(concurrencyLevel)
        .initialCapacity(initialCapacity)
        .maximumSize(maxSize));
    int totalCapacity = 0;
    for (int i = 0; i < map.segments.length; i++) {
      totalCapacity += map.segments[i].maxSegmentSize;
    }
    assertTrue("totalCapcity=" + totalCapacity + ", maxSize=" + maxSize, totalCapacity <= maxSize);
  }

  public void testSetWeakKeys() {
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker().weakKeys());
    checkStrength(map, Strength.WEAK, Strength.STRONG);
    assertSame(EntryFactory.WEAK, map.entryFactory);
  }

  public void testSetWeakValues() {
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker().weakValues());
    checkStrength(map, Strength.STRONG, Strength.WEAK);
    assertSame(EntryFactory.STRONG, map.entryFactory);
  }

  public void testSetSoftValues() {
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker().softValues());
    checkStrength(map, Strength.STRONG, Strength.SOFT);
    assertSame(EntryFactory.STRONG, map.entryFactory);
  }

  private static void checkStrength(
      MapMakerInternalMap<Object, Object> map, Strength keyStrength, Strength valueStrength) {
    assertSame(keyStrength, map.keyStrength);
    assertSame(valueStrength, map.valueStrength);
    assertSame(keyStrength.defaultEquivalence(), map.keyEquivalence);
    assertSame(valueStrength.defaultEquivalence(), map.valueEquivalence);
  }

  public void testSetExpireAfterWrite() {
    long duration = 42;
    TimeUnit unit = SECONDS;
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().expireAfterWrite(duration, unit));
    assertEquals(unit.toNanos(duration), map.expireAfterWriteNanos);
  }

  public void testSetExpireAfterAccess() {
    long duration = 42;
    TimeUnit unit = SECONDS;
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().expireAfterAccess(duration, unit));
    assertEquals(unit.toNanos(duration), map.expireAfterAccessNanos);
  }

  public void testSetRemovalListener() {
    RemovalListener<Object, Object> testListener = new RemovalListener<Object, Object>() {
      @Override
      public void onRemoval(RemovalNotification<Object, Object> notification) {}
    };
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().removalListener(testListener));
    assertSame(testListener, map.removalListener);
  }

  // Removal listener tests

  public void testRemovalListener_explicit() {
    QueuingRemovalListener<Object, Object> listener =
        new QueuingRemovalListener<Object, Object>();
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker()
        .removalListener(listener));
    assertTrue(listener.isEmpty());

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();
    Object four = new Object();
    Object five = new Object();
    Object six = new Object();

    map.put(one, two);
    map.remove(one);
    assertNotified(listener, one, two, RemovalCause.EXPLICIT);

    map.put(two, three);
    map.remove(two, three);
    assertNotified(listener, two, three, RemovalCause.EXPLICIT);

    map.put(three, four);
    Iterator<?> i = map.entrySet().iterator();
    i.next();
    i.remove();
    assertNotified(listener, three, four, RemovalCause.EXPLICIT);

    map.put(four, five);
    i = map.keySet().iterator();
    i.next();
    i.remove();
    assertNotified(listener, four, five, RemovalCause.EXPLICIT);

    map.put(five, six);
    i = map.values().iterator();
    i.next();
    i.remove();
    assertNotified(listener, five, six, RemovalCause.EXPLICIT);

    assertTrue(listener.isEmpty());
  }

  public void testRemovalListener_replaced() {
    QueuingRemovalListener<Object, Object> listener =
        new QueuingRemovalListener<Object, Object>();
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker()
        .removalListener(listener));
    assertTrue(listener.isEmpty());

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();
    Object four = new Object();
    Object five = new Object();
    Object six = new Object();

    map.put(one, two);
    map.put(one, three);
    assertNotified(listener, one, two, RemovalCause.REPLACED);

    Map<Object, Object> newMap = ImmutableMap.of(one, four);
    map.putAll(newMap);
    assertNotified(listener, one, three, RemovalCause.REPLACED);

    map.replace(one, five);
    assertNotified(listener, one, four, RemovalCause.REPLACED);

    map.replace(one, five, six);
    assertNotified(listener, one, five, RemovalCause.REPLACED);
  }

  public void testRemovalListener_collected() {
    QueuingRemovalListener<Object, Object> listener =
        new QueuingRemovalListener<Object, Object>();
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker()
        .concurrencyLevel(1)
        .softValues()
        .removalListener(listener));
    Segment<Object, Object> segment = map.segments[0];
    assertTrue(listener.isEmpty());

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();

    map.put(one, two);
    map.put(two, three);
    assertTrue(listener.isEmpty());

    int hash = map.hash(one);
    ReferenceEntry<Object, Object> entry = segment.getEntry(one, hash);
    map.reclaimValue(entry.getValueReference());
    assertNotified(listener, one, two, RemovalCause.COLLECTED);

    assertTrue(listener.isEmpty());
  }

  public void testRemovalListener_size() {
    QueuingRemovalListener<Object, Object> listener =
        new QueuingRemovalListener<Object, Object>();
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker()
        .concurrencyLevel(1)
        .maximumSize(2)
        .removalListener(listener));
    assertTrue(listener.isEmpty());

    Object one = new Object();
    Object two = new Object();
    Object three = new Object();
    Object four = new Object();

    map.put(one, two);
    map.put(two, three);
    assertTrue(listener.isEmpty());
    map.put(three, four);
    assertNotified(listener, one, two, RemovalCause.SIZE);

    assertTrue(listener.isEmpty());
  }

  static <K, V> void assertNotified(
      QueuingRemovalListener<K, V> listener, K key, V value, RemovalCause cause) {
    RemovalNotification<K, V> notification = listener.remove();
    assertSame(key, notification.getKey());
    assertSame(value, notification.getValue());
    assertSame(cause, notification.getCause());
  }

  // Segment core tests

  public void testNewEntry() {
    for (MapMaker maker : allEntryTypeMakers()) {
      MapMakerInternalMap<Object, Object> map = makeMap(maker);

      Object keyOne = new Object();
      Object valueOne = new Object();
      int hashOne = map.hash(keyOne);
      ReferenceEntry<Object, Object> entryOne = map.newEntry(keyOne, hashOne, null);
      ValueReference<Object, Object> valueRefOne = map.newValueReference(entryOne, valueOne);
      assertSame(valueOne, valueRefOne.get());
      entryOne.setValueReference(valueRefOne);

      assertSame(keyOne, entryOne.getKey());
      assertEquals(hashOne, entryOne.getHash());
      assertNull(entryOne.getNext());
      assertSame(valueRefOne, entryOne.getValueReference());

      Object keyTwo = new Object();
      Object valueTwo = new Object();
      int hashTwo = map.hash(keyTwo);
      ReferenceEntry<Object, Object> entryTwo = map.newEntry(keyTwo, hashTwo, entryOne);
      ValueReference<Object, Object> valueRefTwo = map.newValueReference(entryTwo, valueTwo);
      assertSame(valueTwo, valueRefTwo.get());
      entryTwo.setValueReference(valueRefTwo);

      assertSame(keyTwo, entryTwo.getKey());
      assertEquals(hashTwo, entryTwo.getHash());
      assertSame(entryOne, entryTwo.getNext());
      assertSame(valueRefTwo, entryTwo.getValueReference());
    }
  }

  public void testCopyEntry() {
    for (MapMaker maker : allEntryTypeMakers()) {
      MapMakerInternalMap<Object, Object> map = makeMap(maker);

      Object keyOne = new Object();
      Object valueOne = new Object();
      int hashOne = map.hash(keyOne);
      ReferenceEntry<Object, Object> entryOne = map.newEntry(keyOne, hashOne, null);
      entryOne.setValueReference(map.newValueReference(entryOne, valueOne));

      Object keyTwo = new Object();
      Object valueTwo = new Object();
      int hashTwo = map.hash(keyTwo);
      ReferenceEntry<Object, Object> entryTwo = map.newEntry(keyTwo, hashTwo, entryOne);
      entryTwo.setValueReference(map.newValueReference(entryTwo, valueTwo));
      if (map.evictsBySize()) {
        MapMakerInternalMap.connectEvictables(entryOne, entryTwo);
      }
      if (map.expires()) {
        MapMakerInternalMap.connectExpirables(entryOne, entryTwo);
      }
      assertConnected(map, entryOne, entryTwo);

      ReferenceEntry<Object, Object> copyOne = map.copyEntry(entryOne, null);
      assertSame(keyOne, entryOne.getKey());
      assertEquals(hashOne, entryOne.getHash());
      assertNull(entryOne.getNext());
      assertSame(valueOne, copyOne.getValueReference().get());
      assertConnected(map, copyOne, entryTwo);

      ReferenceEntry<Object, Object> copyTwo = map.copyEntry(entryTwo, copyOne);
      assertSame(keyTwo, copyTwo.getKey());
      assertEquals(hashTwo, copyTwo.getHash());
      assertSame(copyOne, copyTwo.getNext());
      assertSame(valueTwo, copyTwo.getValueReference().get());
      assertConnected(map, copyOne, copyTwo);
    }
  }

  private static <K, V> void assertConnected(
      MapMakerInternalMap<K, V> map, ReferenceEntry<K, V> one, ReferenceEntry<K, V> two) {
    if (map.evictsBySize()) {
      assertSame(two, one.getNextEvictable());
    }
    if (map.expires()) {
      assertSame(two, one.getNextExpirable());
    }
  }

  public void testSegmentGetAndContains() {
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().concurrencyLevel(1).expireAfterAccess(99999, SECONDS));
    Segment<Object, Object> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object value = new Object();
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    int index = hash & (table.length() - 1);

    ReferenceEntry<Object, Object> entry = map.newEntry(key, hash, null);
    ValueReference<Object, Object> valueRef = map.newValueReference(entry, value);
    entry.setValueReference(valueRef);

    assertNull(segment.get(key, hash));

    // count == 0
    table.set(index, entry);
    assertNull(segment.get(key, hash));
    assertFalse(segment.containsKey(key, hash));
    assertFalse(segment.containsValue(value));

    // count == 1
    segment.count++;
    assertSame(value, segment.get(key, hash));
    assertTrue(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    // don't see absent values now that count > 0
    assertNull(segment.get(new Object(), hash));

    // null key
    DummyEntry<Object, Object> nullEntry = DummyEntry.create(null, hash, entry);
    Object nullValue = new Object();
    ValueReference<Object, Object> nullValueRef = map.newValueReference(nullEntry, nullValue);
    nullEntry.setValueReference(nullValueRef);
    table.set(index, nullEntry);
    // skip the null key
    assertSame(value, segment.get(key, hash));
    assertTrue(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    assertFalse(segment.containsValue(nullValue));

    // hash collision
    DummyEntry<Object, Object> dummy = DummyEntry.create(new Object(), hash, entry);
    Object dummyValue = new Object();
    ValueReference<Object, Object> dummyValueRef = map.newValueReference(dummy, dummyValue);
    dummy.setValueReference(dummyValueRef);
    table.set(index, dummy);
    assertSame(value, segment.get(key, hash));
    assertTrue(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    assertTrue(segment.containsValue(dummyValue));

    // key collision
    dummy = DummyEntry.create(key, hash, entry);
    dummyValue = new Object();
    dummyValueRef = map.newValueReference(dummy, dummyValue);
    dummy.setValueReference(dummyValueRef);
    table.set(index, dummy);
    // returns the most recent entry
    assertSame(dummyValue, segment.get(key, hash));
    assertTrue(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    assertTrue(segment.containsValue(dummyValue));

    // expired
    dummy.setExpirationTime(0);
    assertNull(segment.get(key, hash));
    assertFalse(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    assertFalse(segment.containsValue(dummyValue));
  }

  public void testSegmentReplaceValue() {
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().concurrencyLevel(1).expireAfterAccess(99999, SECONDS));
    Segment<Object, Object> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue, entry);
    entry.setValueReference(oldValueRef);

    // no entry
    assertFalse(segment.replace(key, hash, oldValue, newValue));
    assertEquals(0, segment.count);

    // same value
    table.set(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertTrue(segment.replace(key, hash, oldValue, newValue));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));

    // different value
    assertFalse(segment.replace(key, hash, oldValue, newValue));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));

    // cleared
    entry.setValueReference(oldValueRef);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear(null);
    assertFalse(segment.replace(key, hash, oldValue, newValue));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
  }

  public void testSegmentReplace() {
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().concurrencyLevel(1).expireAfterAccess(99999, SECONDS));
    Segment<Object, Object> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue, entry);
    entry.setValueReference(oldValueRef);

    // no entry
    assertNull(segment.replace(key, hash, newValue));
    assertEquals(0, segment.count);

    // same key
    table.set(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertSame(oldValue, segment.replace(key, hash, newValue));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));

    // cleared
    entry.setValueReference(oldValueRef);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear(null);
    assertNull(segment.replace(key, hash, newValue));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
  }

  public void testSegmentPut() {
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().concurrencyLevel(1).expireAfterAccess(99999, SECONDS));
    Segment<Object, Object> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();

    // no entry
    assertEquals(0, segment.count);
    assertNull(segment.put(key, hash, oldValue, false));
    assertEquals(1, segment.count);

    // same key
    assertSame(oldValue, segment.put(key, hash, newValue, false));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));

    // cleared
    ReferenceEntry<Object, Object> entry = segment.getEntry(key, hash);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue, entry);
    entry.setValueReference(oldValueRef);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear(null);
    assertNull(segment.put(key, hash, newValue, false));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));
  }

  public void testSegmentPutIfAbsent() {
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().concurrencyLevel(1).expireAfterAccess(99999, SECONDS));
    Segment<Object, Object> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();

    // no entry
    assertEquals(0, segment.count);
    assertNull(segment.put(key, hash, oldValue, true));
    assertEquals(1, segment.count);

    // same key
    assertSame(oldValue, segment.put(key, hash, newValue, true));
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));

    // cleared
    ReferenceEntry<Object, Object> entry = segment.getEntry(key, hash);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue, entry);
    entry.setValueReference(oldValueRef);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear(null);
    assertNull(segment.put(key, hash, newValue, true));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));
  }

  public void testSegmentPut_expand() {
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object> segment = map.segments[0];
    assertEquals(1, segment.table.length());

    int count = 1024;
    for (int i = 0; i < count; i++) {
      Object key = new Object();
      Object value = new Object();
      int hash = map.hash(key);
      assertNull(segment.put(key, hash, value, false));
      assertTrue(segment.table.length() > i);
    }
  }

  public void testSegmentPut_evict() {
    int maxSize = 10;
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().concurrencyLevel(1).maximumSize(maxSize));

    // manually add elements to avoid eviction
    int originalCount = 1024;
    LinkedHashMap<Object, Object> originalMap = Maps.newLinkedHashMap();
    for (int i = 0; i < originalCount; i++) {
      Object key = new Object();
      Object value = new Object();
      map.put(key, value);
      originalMap.put(key, value);
      if (i >= maxSize) {
        Iterator<Object> it = originalMap.keySet().iterator();
        it.next();
        it.remove();
      }
      assertEquals(originalMap, map);
    }
  }

  public void testSegmentRemove() {
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker().concurrencyLevel(1));
    Segment<Object, Object> segment = map.segments[0];

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue, entry);
    entry.setValueReference(oldValueRef);

    // no entry
    assertEquals(0, segment.count);
    assertNull(segment.remove(key, hash));
    assertEquals(0, segment.count);

    // same key
    table.set(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertSame(oldValue, segment.remove(key, hash));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));

    // cleared
    table.set(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear(null);
    assertNull(segment.remove(key, hash));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
  }

  public void testSegmentRemoveValue() {
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker().concurrencyLevel(1));
    Segment<Object, Object> segment = map.segments[0];

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    int index = hash & (table.length() - 1);

    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> oldValueRef = DummyValueReference.create(oldValue, entry);
    entry.setValueReference(oldValueRef);

    // no entry
    assertEquals(0, segment.count);
    assertNull(segment.remove(key, hash));
    assertEquals(0, segment.count);

    // same value
    table.set(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertTrue(segment.remove(key, hash, oldValue));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));

    // different value
    table.set(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertFalse(segment.remove(key, hash, newValue));
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));

    // cleared
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear(null);
    assertFalse(segment.remove(key, hash, oldValue));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
  }

  public void testExpand() {
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object> segment = map.segments[0];
    assertEquals(1, segment.table.length());

    // manually add elements to avoid expansion
    int originalCount = 1024;
    ReferenceEntry<Object, Object> entry = null;
    for (int i = 0; i < originalCount; i++) {
      Object key = new Object();
      Object value = new Object();
      int hash = map.hash(key);
      // chain all entries together as we only have a single bucket
      entry = map.newEntry(key, hash, entry);
      ValueReference<Object, Object> valueRef = map.newValueReference(entry, value);
      entry.setValueReference(valueRef);
    }
    segment.table.set(0, entry);
    segment.count = originalCount;
    ImmutableMap<Object, Object> originalMap = ImmutableMap.copyOf(map);
    assertEquals(originalCount, originalMap.size());
    assertEquals(originalMap, map);

    for (int i = 1; i <= originalCount * 2; i *= 2) {
      if (i > 1) {
        segment.expand();
      }
      assertEquals(i, segment.table.length());
      assertEquals(originalCount, countLiveEntries(map));
      assertEquals(originalCount, segment.count);
      assertEquals(originalMap, map);
    }
  }

  public void testReclaimKey() {
    CountingRemovalListener<Object, Object> listener =
        new CountingRemovalListener<Object, Object>();
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker()
        .concurrencyLevel(1)
        .initialCapacity(1)
        .maximumSize(SMALL_MAX_SIZE)
        .expireAfterWrite(99999, SECONDS)
        .removalListener(listener));
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(1, table.length());

    // create 3 objects and chain them together
    Object keyOne = new Object();
    Object valueOne = new Object();
    int hashOne = map.hash(keyOne);
    DummyEntry<Object, Object> entryOne = createDummyEntry(keyOne, hashOne, valueOne, null);
    Object keyTwo = new Object();
    Object valueTwo = new Object();
    int hashTwo = map.hash(keyTwo);
    DummyEntry<Object, Object> entryTwo = createDummyEntry(keyTwo, hashTwo, valueTwo, entryOne);
    Object keyThree = new Object();
    Object valueThree = new Object();
    int hashThree = map.hash(keyThree);
    DummyEntry<Object, Object> entryThree =
        createDummyEntry(keyThree, hashThree, valueThree, entryTwo);

    // absent
    assertEquals(0, listener.getCount());
    assertFalse(segment.reclaimKey(entryOne, hashOne));
    assertEquals(0, listener.getCount());
    table.set(0, entryOne);
    assertFalse(segment.reclaimKey(entryTwo, hashTwo));
    assertEquals(0, listener.getCount());
    table.set(0, entryTwo);
    assertFalse(segment.reclaimKey(entryThree, hashThree));
    assertEquals(0, listener.getCount());

    // present
    table.set(0, entryOne);
    segment.count = 1;
    assertTrue(segment.reclaimKey(entryOne, hashOne));
    assertEquals(1, listener.getCount());
    assertSame(keyOne, listener.getLastEvictedKey());
    assertSame(valueOne, listener.getLastEvictedValue());
    assertTrue(map.removalNotificationQueue.isEmpty());
    assertFalse(segment.evictionQueue.contains(entryOne));
    assertFalse(segment.expirationQueue.contains(entryOne));
    assertEquals(0, segment.count);
    assertNull(table.get(0));
  }

  public void testRemoveFromChain() {
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker().concurrencyLevel(1));
    Segment<Object, Object> segment = map.segments[0];

    // create 3 objects and chain them together
    Object keyOne = new Object();
    Object valueOne = new Object();
    int hashOne = map.hash(keyOne);
    DummyEntry<Object, Object> entryOne = createDummyEntry(keyOne, hashOne, valueOne, null);
    Object keyTwo = new Object();
    Object valueTwo = new Object();
    int hashTwo = map.hash(keyTwo);
    DummyEntry<Object, Object> entryTwo = createDummyEntry(keyTwo, hashTwo, valueTwo, entryOne);
    Object keyThree = new Object();
    Object valueThree = new Object();
    int hashThree = map.hash(keyThree);
    DummyEntry<Object, Object> entryThree =
        createDummyEntry(keyThree, hashThree, valueThree, entryTwo);

    // alone
    assertNull(segment.removeFromChain(entryOne, entryOne));

    // head
    assertSame(entryOne, segment.removeFromChain(entryTwo, entryTwo));

    // middle
    ReferenceEntry<Object, Object> newFirst = segment.removeFromChain(entryThree, entryTwo);
    assertSame(keyThree, newFirst.getKey());
    assertSame(valueThree, newFirst.getValueReference().get());
    assertEquals(hashThree, newFirst.getHash());
    assertSame(entryOne, newFirst.getNext());

    // tail (remaining entries are copied in reverse order)
    newFirst = segment.removeFromChain(entryThree, entryOne);
    assertSame(keyTwo, newFirst.getKey());
    assertSame(valueTwo, newFirst.getValueReference().get());
    assertEquals(hashTwo, newFirst.getHash());
    newFirst = newFirst.getNext();
    assertSame(keyThree, newFirst.getKey());
    assertSame(valueThree, newFirst.getValueReference().get());
    assertEquals(hashThree, newFirst.getHash());
    assertNull(newFirst.getNext());
  }

  public void testExpand_cleanup() {
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object> segment = map.segments[0];
    assertEquals(1, segment.table.length());

    // manually add elements to avoid expansion
    // 1/3 null keys, 1/3 null values
    int originalCount = 1024;
    ReferenceEntry<Object, Object> entry = null;
    for (int i = 0; i < originalCount; i++) {
      Object key = new Object();
      Object value = (i % 3 == 0) ? null : new Object();
      int hash = map.hash(key);
      if (i % 3 == 1) {
        key = null;
      }
      // chain all entries together as we only have a single bucket
      entry = DummyEntry.create(key, hash, entry);
      ValueReference<Object, Object> valueRef = DummyValueReference.create(value, entry);
      entry.setValueReference(valueRef);
    }
    segment.table.set(0, entry);
    segment.count = originalCount;
    int liveCount = originalCount / 3;
    assertEquals(1, segment.table.length());
    assertEquals(liveCount, countLiveEntries(map));
    ImmutableMap<Object, Object> originalMap = ImmutableMap.copyOf(map);
    assertEquals(liveCount, originalMap.size());
    // can't compare map contents until cleanup occurs

    for (int i = 1; i <= originalCount * 2; i *= 2) {
      if (i > 1) {
        segment.expand();
      }
      assertEquals(i, segment.table.length());
      assertEquals(liveCount, countLiveEntries(map));
      // expansion cleanup is sloppy, with a goal of avoiding unnecessary copies
      assertTrue(segment.count >= liveCount);
      assertTrue(segment.count <= originalCount);
      assertEquals(originalMap, ImmutableMap.copyOf(map));
    }
  }

  private static <K, V> int countLiveEntries(MapMakerInternalMap<K, V> map) {
    int result = 0;
    for (Segment<K, V> segment : map.segments) {
      AtomicReferenceArray<ReferenceEntry<K, V>> table = segment.table;
      for (int i = 0; i < table.length(); i++) {
        for (ReferenceEntry<K, V> e = table.get(i); e != null; e = e.getNext()) {
          if (map.isLive(e)) {
            result++;
          }
        }
      }
    }
    return result;
  }

  public void testClear() {
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker()
        .concurrencyLevel(1)
        .initialCapacity(1)
        .maximumSize(SMALL_MAX_SIZE)
        .expireAfterWrite(99999, SECONDS));
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(1, table.length());

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = createDummyEntry(key, hash, value, null);
    segment.recordWrite(entry);
    segment.table.set(0, entry);
    segment.readCount.incrementAndGet();
    segment.count = 1;

    assertSame(entry, table.get(0));
    assertSame(entry, segment.evictionQueue.peek());
    assertSame(entry, segment.expirationQueue.peek());

    segment.clear();
    assertNull(table.get(0));
    assertTrue(segment.evictionQueue.isEmpty());
    assertTrue(segment.expirationQueue.isEmpty());
    assertEquals(0, segment.readCount.get());
    assertEquals(0, segment.count);
  }

  public void testRemoveEntry() {
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker()
        .concurrencyLevel(1)
        .initialCapacity(1)
        .maximumSize(SMALL_MAX_SIZE)
        .expireAfterWrite(99999, SECONDS)
        .removalListener(new CountingRemovalListener<Object, Object>()));
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(1, table.length());

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = createDummyEntry(key, hash, value, null);

    // remove absent
    assertFalse(segment.removeEntry(entry, hash, RemovalCause.COLLECTED));

    // remove live
    segment.recordWrite(entry);
    table.set(0, entry);
    segment.count = 1;
    assertTrue(segment.removeEntry(entry, hash, RemovalCause.COLLECTED));
    assertNotificationEnqueued(map, key, value);
    assertTrue(map.removalNotificationQueue.isEmpty());
    assertFalse(segment.evictionQueue.contains(entry));
    assertFalse(segment.expirationQueue.contains(entry));
    assertEquals(0, segment.count);
    assertNull(table.get(0));
  }

  public void testReclaimValue() {
    CountingRemovalListener<Object, Object> listener =
        new CountingRemovalListener<Object, Object>();
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker()
        .concurrencyLevel(1)
        .initialCapacity(1)
        .maximumSize(SMALL_MAX_SIZE)
        .expireAfterWrite(99999, SECONDS)
        .removalListener(listener));
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(1, table.length());

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> valueRef = DummyValueReference.create(value, entry);
    entry.setValueReference(valueRef);

    // reclaim absent
    assertFalse(segment.reclaimValue(key, hash, valueRef));

    // reclaim live
    segment.recordWrite(entry);
    table.set(0, entry);
    segment.count = 1;
    assertTrue(segment.reclaimValue(key, hash, valueRef));
    assertEquals(1, listener.getCount());
    assertSame(key, listener.getLastEvictedKey());
    assertSame(value, listener.getLastEvictedValue());
    assertTrue(map.removalNotificationQueue.isEmpty());
    assertFalse(segment.evictionQueue.contains(entry));
    assertFalse(segment.expirationQueue.contains(entry));
    assertEquals(0, segment.count);
    assertNull(table.get(0));

    // reclaim wrong value reference
    table.set(0, entry);
    DummyValueReference<Object, Object> otherValueRef = DummyValueReference.create(value, entry);
    entry.setValueReference(otherValueRef);
    assertFalse(segment.reclaimValue(key, hash, valueRef));
    assertEquals(1, listener.getCount());
    assertTrue(segment.reclaimValue(key, hash, otherValueRef));
    assertEquals(2, listener.getCount());
    assertSame(key, listener.getLastEvictedKey());
    assertSame(value, listener.getLastEvictedValue());
  }

  public void testClearValue() {
    MapMakerInternalMap<Object, Object> map = makeMap(createMapMaker()
        .concurrencyLevel(1)
        .initialCapacity(1)
        .maximumSize(SMALL_MAX_SIZE)
        .expireAfterWrite(99999, SECONDS)
        .removalListener(new CountingRemovalListener<Object, Object>()));
    Segment<Object, Object> segment = map.segments[0];
    AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
    assertEquals(1, table.length());

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    DummyEntry<Object, Object> entry = DummyEntry.create(key, hash, null);
    DummyValueReference<Object, Object> valueRef = DummyValueReference.create(value, entry);
    entry.setValueReference(valueRef);

    // clear absent
    assertFalse(segment.clearValue(key, hash, valueRef));

    // clear live
    segment.recordWrite(entry);
    table.set(0, entry);
    // don't increment count; this is used during computation
    assertTrue(segment.clearValue(key, hash, valueRef));
    // no notification sent with clearValue
    assertTrue(map.removalNotificationQueue.isEmpty());
    assertFalse(segment.evictionQueue.contains(entry));
    assertFalse(segment.expirationQueue.contains(entry));
    assertEquals(0, segment.count);
    assertNull(table.get(0));

    // clear wrong value reference
    table.set(0, entry);
    DummyValueReference<Object, Object> otherValueRef = DummyValueReference.create(value, entry);
    entry.setValueReference(otherValueRef);
    assertFalse(segment.clearValue(key, hash, valueRef));
    entry.setValueReference(valueRef);
    assertTrue(segment.clearValue(key, hash, valueRef));
  }

  private static <K, V> void assertNotificationEnqueued(
      MapMakerInternalMap<K, V> map, K key, V value) {
    RemovalNotification<K, V> notification = map.removalNotificationQueue.poll();
    assertSame(key, notification.getKey());
    assertSame(value, notification.getValue());
  }

  // Segment eviction tests

  public void testDrainRecencyQueueOnWrite() {
    for (MapMaker maker : allEvictingMakers()) {
      MapMakerInternalMap<Object, Object> map = makeMap(maker.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];

      if (segment.recencyQueue != DISCARDING_QUEUE) {
        Object keyOne = new Object();
        Object valueOne = new Object();
        Object keyTwo = new Object();
        Object valueTwo = new Object();

        map.put(keyOne, valueOne);
        assertTrue(segment.recencyQueue.isEmpty());

        for (int i = 0; i < DRAIN_THRESHOLD / 2; i++) {
          map.get(keyOne);
        }
        assertFalse(segment.recencyQueue.isEmpty());

        map.put(keyTwo, valueTwo);
        assertTrue(segment.recencyQueue.isEmpty());
      }
    }
  }

  public void testDrainRecencyQueueOnRead() {
    for (MapMaker maker : allEvictingMakers()) {
      MapMakerInternalMap<Object, Object> map = makeMap(maker.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];

      if (segment.recencyQueue != DISCARDING_QUEUE) {
        Object keyOne = new Object();
        Object valueOne = new Object();

        // repeated get of the same key

        map.put(keyOne, valueOne);
        assertTrue(segment.recencyQueue.isEmpty());

        for (int i = 0; i < DRAIN_THRESHOLD / 2; i++) {
          map.get(keyOne);
        }
        assertFalse(segment.recencyQueue.isEmpty());

        for (int i = 0; i < DRAIN_THRESHOLD * 2; i++) {
          map.get(keyOne);
          assertTrue(segment.recencyQueue.size() <= DRAIN_THRESHOLD);
        }

        // get over many different keys

        for (int i = 0; i < DRAIN_THRESHOLD * 2; i++) {
          map.put(new Object(), new Object());
        }
        assertTrue(segment.recencyQueue.isEmpty());

        for (int i = 0; i < DRAIN_THRESHOLD / 2; i++) {
          map.get(keyOne);
        }
        assertFalse(segment.recencyQueue.isEmpty());

        for (Object key : map.keySet()) {
          map.get(key);
          assertTrue(segment.recencyQueue.size() <= DRAIN_THRESHOLD);
        }
      }
    }
  }

  public void testRecordRead() {
    for (MapMaker maker : allEvictingMakers()) {
      MapMakerInternalMap<Object, Object> map = makeMap(maker.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];
      List<ReferenceEntry<Object, Object>> writeOrder = Lists.newLinkedList();
      List<ReferenceEntry<Object, Object>> readOrder = Lists.newLinkedList();
      for (int i = 0; i < DRAIN_THRESHOLD * 2; i++) {
        Object key = new Object();
        int hash = map.hash(key);
        Object value = new Object();

        ReferenceEntry<Object, Object> entry = createDummyEntry(key, hash, value, null);
        // must recordRead for drainRecencyQueue to believe this entry is live
        segment.recordWrite(entry);
        writeOrder.add(entry);
        readOrder.add(entry);
      }

      checkEvictionQueues(map, segment, readOrder, writeOrder);
      checkExpirationTimes(map);

      // access some of the elements
      Random random = new Random();
      List<ReferenceEntry<Object, Object>> reads = Lists.newArrayList();
      Iterator<ReferenceEntry<Object, Object>> i = readOrder.iterator();
      while (i.hasNext()) {
        ReferenceEntry<Object, Object> entry = i.next();
        if (random.nextBoolean()) {
          segment.recordRead(entry);
          reads.add(entry);
          i.remove();
        }
      }
      checkAndDrainRecencyQueue(map, segment, reads);
      readOrder.addAll(reads);

      checkEvictionQueues(map, segment, readOrder, writeOrder);
      checkExpirationTimes(map);
    }
  }

  public void testRecordReadOnGet() {
    for (MapMaker maker : allEvictingMakers()) {
      MapMakerInternalMap<Object, Object> map = makeMap(maker.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];
      List<ReferenceEntry<Object, Object>> writeOrder = Lists.newLinkedList();
      List<ReferenceEntry<Object, Object>> readOrder = Lists.newLinkedList();
      for (int i = 0; i < DRAIN_THRESHOLD * 2; i++) {
        Object key = new Object();
        int hash = map.hash(key);
        Object value = new Object();

        map.put(key, value);
        ReferenceEntry<Object, Object> entry = segment.getEntry(key, hash);
        writeOrder.add(entry);
        readOrder.add(entry);
      }

      checkEvictionQueues(map, segment, readOrder, writeOrder);
      checkExpirationTimes(map);
      assertTrue(segment.recencyQueue.isEmpty());

      // access some of the elements
      Random random = new Random();
      List<ReferenceEntry<Object, Object>> reads = Lists.newArrayList();
      Iterator<ReferenceEntry<Object, Object>> i = readOrder.iterator();
      while (i.hasNext()) {
        ReferenceEntry<Object, Object> entry = i.next();
        if (random.nextBoolean()) {
          map.get(entry.getKey());
          reads.add(entry);
          i.remove();
          assertTrue(segment.recencyQueue.size() <= DRAIN_THRESHOLD);
        }
      }
      int undrainedIndex = reads.size() - segment.recencyQueue.size();
      checkAndDrainRecencyQueue(map, segment, reads.subList(undrainedIndex, reads.size()));
      readOrder.addAll(reads);

      checkEvictionQueues(map, segment, readOrder, writeOrder);
      checkExpirationTimes(map);
    }
  }

  public void testRecordWrite() {
    for (MapMaker maker : allEvictingMakers()) {
      MapMakerInternalMap<Object, Object> map = makeMap(maker.concurrencyLevel(1));
      Segment<Object, Object> segment = map.segments[0];
      List<ReferenceEntry<Object, Object>> writeOrder = Lists.newLinkedList();
      for (int i = 0; i < DRAIN_THRESHOLD * 2; i++) {
        Object key = new Object();
        int hash = map.hash(key);
        Object value = new Object();

        ReferenceEntry<Object, Object> entry = createDummyEntry(key, hash, value, null);
        // must recordRead for drainRecencyQueue to believe this entry is live
        segment.recordWrite(entry);
        writeOrder.add(entry);
      }

      checkEvictionQueues(map, segment, writeOrder, writeOrder);
      checkExpirationTimes(map);

      // access some of the elements
      Random random = new Random();
      List<ReferenceEntry<Object, Object>> writes = Lists.newArrayList();
      Iterator<ReferenceEntry<Object, Object>> i = writeOrder.iterator();
      while (i.hasNext()) {
        ReferenceEntry<Object, Object> entry = i.next();
        if (random.nextBoolean()) {
          segment.recordWrite(entry);
          writes.add(entry);
          i.remove();
        }
      }
      writeOrder.addAll(writes);

      checkEvictionQueues(map, segment, writeOrder, writeOrder);
      checkExpirationTimes(map);
    }
  }

  static <K, V> void checkAndDrainRecencyQueue(MapMakerInternalMap<K, V> map,
      Segment<K, V> segment, List<ReferenceEntry<K, V>> reads) {
    if (map.evictsBySize() || map.expiresAfterAccess()) {
      assertSameEntries(reads, ImmutableList.copyOf(segment.recencyQueue));
    }
    segment.drainRecencyQueue();
  }

  static <K, V> void checkEvictionQueues(MapMakerInternalMap<K, V> map,
      Segment<K, V> segment, List<ReferenceEntry<K, V>> readOrder,
      List<ReferenceEntry<K, V>> writeOrder) {
    if (map.evictsBySize()) {
      assertSameEntries(readOrder, ImmutableList.copyOf(segment.evictionQueue));
    }
    if (map.expiresAfterAccess()) {
      assertSameEntries(readOrder, ImmutableList.copyOf(segment.expirationQueue));
    }
    if (map.expiresAfterWrite()) {
      assertSameEntries(writeOrder, ImmutableList.copyOf(segment.expirationQueue));
    }
  }

  private static <K, V> void assertSameEntries(List<ReferenceEntry<K, V>> expectedEntries,
      List<ReferenceEntry<K, V>> actualEntries) {
    int size = expectedEntries.size();
    assertEquals(size, actualEntries.size());
    for (int i = 0; i < size; i++) {
      ReferenceEntry<K, V> expectedEntry = expectedEntries.get(0);
      ReferenceEntry<K, V> actualEntry = actualEntries.get(0);
      assertSame(expectedEntry.getKey(), actualEntry.getKey());
      assertSame(expectedEntry.getValueReference().get(), actualEntry.getValueReference().get());
    }
  }

  static <K, V> void checkExpirationTimes(MapMakerInternalMap<K, V> map) {
    if (!map.expires()) {
      return;
    }

    for (Segment<K, V> segment : map.segments) {
      long lastExpirationTime = 0;
      for (ReferenceEntry<K, V> e : segment.recencyQueue) {
        long expirationTime = e.getExpirationTime();
        assertTrue(expirationTime >= lastExpirationTime);
        lastExpirationTime = expirationTime;
      }

      lastExpirationTime = 0;
      for (ReferenceEntry<K, V> e : segment.expirationQueue) {
        long expirationTime = e.getExpirationTime();
        assertTrue(expirationTime >= lastExpirationTime);
        lastExpirationTime = expirationTime;
      }
    }
  }

  public void testEvictEntries() {
    int maxSize = 10;
    MapMakerInternalMap<Object, Object> map =
        makeMap(createMapMaker().concurrencyLevel(1).maximumSize(maxSize));
    Segment<Object, Object> segment = map.segments[0];

    // manually add elements to avoid eviction
    int originalCount = 1024;
    ReferenceEntry<Object, Object> entry = null;
    LinkedHashMap<Object, Object> originalMap = Maps.newLinkedHashMap();
    for (int i = 0; i < originalCount; i++) {
      Object key = new Object();
      Object value = new Object();
      AtomicReferenceArray<ReferenceEntry<Object, Object>> table = segment.table;
      int hash = map.hash(key);
      int index = hash & (table.length() - 1);
      ReferenceEntry<Object, Object> first = table.get(index);
      entry = map.newEntry(key, hash, first);
      ValueReference<Object, Object> valueRef = map.newValueReference(entry, value);
      entry.setValueReference(valueRef);
      segment.recordWrite(entry);
      table.set(index, entry);
      originalMap.put(key, value);
    }
    segment.count = originalCount;
    assertEquals(originalCount, originalMap.size());
    assertEquals(originalMap, map);

    for (int i = maxSize - 1; i < originalCount; i++) {
      assertTrue(segment.evictEntries());
      Iterator<Object> it = originalMap.keySet().iterator();
      it.next();
      it.remove();
      assertEquals(originalMap, map);
    }
    assertFalse(segment.evictEntries());
  }

  // reference queues

  public void testDrainKeyReferenceQueueOnWrite() {
    for (MapMaker maker : allKeyValueStrengthMakers()) {
      MapMakerInternalMap<Object, Object> map =
          makeMap(maker.concurrencyLevel(1));
      if (map.usesKeyReferences()) {
        Segment<Object, Object> segment = map.segments[0];

        Object keyOne = new Object();
        int hashOne = map.hash(keyOne);
        Object valueOne = new Object();
        Object keyTwo = new Object();
        Object valueTwo = new Object();

        map.put(keyOne, valueOne);
        ReferenceEntry<Object, Object> entry = segment.getEntry(keyOne, hashOne);

        @SuppressWarnings("unchecked")
        Reference<Object> reference = (Reference) entry;
        reference.enqueue();

        map.put(keyTwo, valueTwo);
        assertFalse(map.containsKey(keyOne));
        assertFalse(map.containsValue(valueOne));
        assertNull(map.get(keyOne));
        assertEquals(1, map.size());
        assertNull(segment.keyReferenceQueue.poll());
      }
    }
  }

  public void testDrainValueReferenceQueueOnWrite() {
    for (MapMaker maker : allKeyValueStrengthMakers()) {
      MapMakerInternalMap<Object, Object> map =
          makeMap(maker.concurrencyLevel(1));
      if (map.usesValueReferences()) {
        Segment<Object, Object> segment = map.segments[0];

        Object keyOne = new Object();
        int hashOne = map.hash(keyOne);
        Object valueOne = new Object();
        Object keyTwo = new Object();
        Object valueTwo = new Object();

        map.put(keyOne, valueOne);
        ReferenceEntry<Object, Object> entry = segment.getEntry(keyOne, hashOne);
        ValueReference<Object, Object> valueReference = entry.getValueReference();

        @SuppressWarnings("unchecked")
        Reference<Object> reference = (Reference) valueReference;
        reference.enqueue();

        map.put(keyTwo, valueTwo);
        assertFalse(map.containsKey(keyOne));
        assertFalse(map.containsValue(valueOne));
        assertNull(map.get(keyOne));
        assertEquals(1, map.size());
        assertNull(segment.valueReferenceQueue.poll());
      }
    }
  }

  public void testDrainKeyReferenceQueueOnRead() {
    for (MapMaker maker : allKeyValueStrengthMakers()) {
      MapMakerInternalMap<Object, Object> map =
          makeMap(maker.concurrencyLevel(1));
      if (map.usesKeyReferences()) {
        Segment<Object, Object> segment = map.segments[0];

        Object keyOne = new Object();
        int hashOne = map.hash(keyOne);
        Object valueOne = new Object();
        Object keyTwo = new Object();

        map.put(keyOne, valueOne);
        ReferenceEntry<Object, Object> entry = segment.getEntry(keyOne, hashOne);

        @SuppressWarnings("unchecked")
        Reference<Object> reference = (Reference) entry;
        reference.enqueue();

        for (int i = 0; i < SMALL_MAX_SIZE; i++) {
          map.get(keyTwo);
        }
        assertFalse(map.containsKey(keyOne));
        assertFalse(map.containsValue(valueOne));
        assertNull(map.get(keyOne));
        assertEquals(0, map.size());
        assertNull(segment.keyReferenceQueue.poll());
      }
    }
  }

  public void testDrainValueReferenceQueueOnRead() {
    for (MapMaker maker : allKeyValueStrengthMakers()) {
      MapMakerInternalMap<Object, Object> map =
          makeMap(maker.concurrencyLevel(1));
      if (map.usesValueReferences()) {
        Segment<Object, Object> segment = map.segments[0];

        Object keyOne = new Object();
        int hashOne = map.hash(keyOne);
        Object valueOne = new Object();
        Object keyTwo = new Object();

        map.put(keyOne, valueOne);
        ReferenceEntry<Object, Object> entry = segment.getEntry(keyOne, hashOne);
        ValueReference<Object, Object> valueReference = entry.getValueReference();

        @SuppressWarnings("unchecked")
        Reference<Object> reference = (Reference) valueReference;
        reference.enqueue();

        for (int i = 0; i < SMALL_MAX_SIZE; i++) {
          map.get(keyTwo);
        }
        assertFalse(map.containsKey(keyOne));
        assertFalse(map.containsValue(valueOne));
        assertNull(map.get(keyOne));
        assertEquals(0, map.size());
        assertNull(segment.valueReferenceQueue.poll());
      }
    }
  }

  // utility methods

  /**
   * Returns an iterable containing all combinations of maximumSize, expireAfterAccess/Write,
   * weakKeys and weak/softValues.
   */
  private static Iterable<MapMaker> allEntryTypeMakers() {
    List<MapMaker> result = newArrayList(allKeyValueStrengthMakers());
    for (MapMaker maker : allKeyValueStrengthMakers()) {
      result.add(maker.maximumSize(SMALL_MAX_SIZE));
    }
    for (MapMaker maker : allKeyValueStrengthMakers()) {
      result.add(maker.expireAfterAccess(99999, SECONDS));
    }
    for (MapMaker maker : allKeyValueStrengthMakers()) {
      result.add(maker.expireAfterWrite(99999, SECONDS));
    }
    for (MapMaker maker : allKeyValueStrengthMakers()) {
      result.add(maker.maximumSize(SMALL_MAX_SIZE).expireAfterAccess(99999, SECONDS));
    }
    for (MapMaker maker : allKeyValueStrengthMakers()) {
      result.add(maker.maximumSize(SMALL_MAX_SIZE).expireAfterWrite(99999, SECONDS));
    }
    return result;
  }

  /**
   * Returns an iterable containing all combinations of maximumSize and expireAfterAccess/Write.
   */
  static Iterable<MapMaker> allEvictingMakers() {
    return ImmutableList.of(createMapMaker().maximumSize(SMALL_MAX_SIZE),
        createMapMaker().expireAfterAccess(99999, SECONDS),
        createMapMaker().expireAfterWrite(99999, SECONDS),
        createMapMaker()
            .maximumSize(SMALL_MAX_SIZE)
            .expireAfterAccess(SMALL_MAX_SIZE, TimeUnit.SECONDS),
        createMapMaker()
            .maximumSize(SMALL_MAX_SIZE)
            .expireAfterWrite(SMALL_MAX_SIZE, TimeUnit.SECONDS));
  }

  /**
   * Returns an iterable containing all combinations weakKeys and weak/softValues.
   */
  private static Iterable<MapMaker> allKeyValueStrengthMakers() {
    return ImmutableList.of(createMapMaker(),
        createMapMaker().weakValues(),
        createMapMaker().softValues(),
        createMapMaker().weakKeys(),
        createMapMaker().weakKeys().weakValues(),
        createMapMaker().weakKeys().softValues());
  }

  // listeners

  private static class CountingRemovalListener<K, V> implements RemovalListener<K, V> {
    private final AtomicInteger count = new AtomicInteger();
    private K lastKey;
    private V lastValue;

    @Override
    public void onRemoval(RemovalNotification<K, V> notification) {
      count.incrementAndGet();
      lastKey = notification.getKey();
      lastValue = notification.getValue();
    }

    public int getCount() {
      return count.get();
    }

    public K getLastEvictedKey() {
      return lastKey;
    }

    public V getLastEvictedValue() {
      return lastValue;
    }
  }

  static class QueuingRemovalListener<K, V>
      extends ConcurrentLinkedQueue<RemovalNotification<K, V>> implements RemovalListener<K, V> {
    @Override
    public void onRemoval(RemovalNotification<K, V> notification) {
      add(notification);
    }
  }

  // entries and values

  private static <K, V> DummyEntry<K, V> createDummyEntry(
      K key, int hash, V value, ReferenceEntry<K, V> next) {
    DummyEntry<K, V> entry = DummyEntry.create(key, hash, next);
    DummyValueReference<K, V> valueRef = DummyValueReference.create(value, entry);
    entry.setValueReference(valueRef);
    return entry;
  }

  static class DummyEntry<K, V> implements ReferenceEntry<K, V> {
    private K key;
    private final int hash;
    private final ReferenceEntry<K, V> next;

    public DummyEntry(K key, int hash, ReferenceEntry<K, V> next) {
      this.key = key;
      this.hash = hash;
      this.next = next;
    }

    public static <K, V> DummyEntry<K, V> create(K key, int hash, ReferenceEntry<K, V> next) {
      return new DummyEntry<K, V>(key, hash, next);
    }

    public void clearKey() {
      this.key = null;
    }

    private ValueReference<K, V> valueReference = unset();

    @Override
    public ValueReference<K, V> getValueReference() {
      return valueReference;
    }

    @Override
    public void setValueReference(ValueReference<K, V> valueReference) {
      this.valueReference = valueReference;
    }

    @Override
    public ReferenceEntry<K, V> getNext() {
      return next;
    }

    @Override
    public int getHash() {
      return hash;
    }

    @Override
    public K getKey() {
      return key;
    }

    private long expirationTime = Long.MAX_VALUE;

    @Override
    public long getExpirationTime() {
      return expirationTime;
    }

    @Override
    public void setExpirationTime(long time) {
      this.expirationTime = time;
    }

    private ReferenceEntry<K, V> nextExpirable = nullEntry();

    @Override
    public ReferenceEntry<K, V> getNextExpirable() {
      return nextExpirable;
    }

    @Override
    public void setNextExpirable(ReferenceEntry<K, V> next) {
      this.nextExpirable = next;
    }

    private ReferenceEntry<K, V> previousExpirable = nullEntry();

    @Override
    public ReferenceEntry<K, V> getPreviousExpirable() {
      return previousExpirable;
    }

    @Override
    public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
      this.previousExpirable = previous;
    }

    private ReferenceEntry<K, V> nextEvictable = nullEntry();

    @Override
    public ReferenceEntry<K, V> getNextEvictable() {
      return nextEvictable;
    }

    @Override
    public void setNextEvictable(ReferenceEntry<K, V> next) {
      this.nextEvictable = next;
    }

    private ReferenceEntry<K, V> previousEvictable = nullEntry();

    @Override
    public ReferenceEntry<K, V> getPreviousEvictable() {
      return previousEvictable;
    }

    @Override
    public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
      this.previousEvictable = previous;
    }
  }

  static class DummyValueReference<K, V> implements ValueReference<K, V> {
    final ReferenceEntry<K, V> entry;
    private V value;

    public DummyValueReference(V value, ReferenceEntry<K, V> entry) {
      this.value = value;
      this.entry = entry;
    }

    public static <K, V> DummyValueReference<K, V> create(V value, ReferenceEntry<K, V> entry) {
      return new DummyValueReference<K, V>(value, entry);
    }

    @Override
    public V get() {
      return value;
    }

    @Override
    public ReferenceEntry<K, V> getEntry() {
      return entry;
    }

    @Override
    public ValueReference<K, V> copyFor(
        ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
      return new DummyValueReference<K, V>(value, entry);
    }

    boolean computing = false;

    public void setComputing(boolean computing) {
      this.computing = computing;
    }

    @Override
    public boolean isComputingReference() {
      return computing;
    }

    @Override
    public V waitForValue() {
      return get();
    }

    @Override
    public void clear(ValueReference<K, V> newValue) {
      value = null;
    }
  }

  public void testNullParameters() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(makeMap(createMapMaker()));
  }

}

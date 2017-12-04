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

import static com.google.common.collect.MapMakerInternalMap.DRAIN_THRESHOLD;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Equivalence;
import com.google.common.collect.MapMakerInternalMap.InternalEntry;
import com.google.common.collect.MapMakerInternalMap.Segment;
import com.google.common.collect.MapMakerInternalMap.Strength;
import com.google.common.collect.MapMakerInternalMap.WeakValueEntry;
import com.google.common.collect.MapMakerInternalMap.WeakValueReference;
import com.google.common.testing.NullPointerTester;
import java.lang.ref.Reference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import junit.framework.TestCase;

/** @author Charles Fry */
@SuppressWarnings("deprecation") // many tests of deprecated methods
public class MapMakerInternalMapTest extends TestCase {

  static final int SMALL_MAX_SIZE = DRAIN_THRESHOLD * 5;

  private static <K, V>
      MapMakerInternalMap<K, V, ? extends InternalEntry<K, V, ?>, ? extends Segment<K, V, ?, ?>>
          makeMap(MapMaker maker) {
    return MapMakerInternalMap.create(maker);
  }

  private static MapMaker createMapMaker() {
    MapMaker maker = new MapMaker();
    maker.useCustomMap = true;
    return maker;
  }

  // constructor tests

  public void testDefaults() {
    MapMakerInternalMap<Object, Object, ?, ?> map = makeMap(createMapMaker());

    assertSame(Strength.STRONG, map.keyStrength());
    assertSame(Strength.STRONG, map.valueStrength());
    assertSame(map.keyStrength().defaultEquivalence(), map.keyEquivalence);
    assertSame(map.valueStrength().defaultEquivalence(), map.valueEquivalence());

    assertThat(map.entryHelper)
        .isInstanceOf(MapMakerInternalMap.StrongKeyStrongValueEntry.Helper.class);

    assertEquals(4, map.concurrencyLevel);

    // concurrency level
    assertThat(map.segments).hasLength(4);
    // initial capacity / concurrency level
    assertEquals(16 / map.segments.length, map.segments[0].table.length());
  }

  public void testSetKeyEquivalence() {
    Equivalence<Object> testEquivalence =
        new Equivalence<Object>() {
          @Override
          protected boolean doEquivalent(Object a, Object b) {
            return false;
          }

          @Override
          protected int doHash(Object t) {
            return 0;
          }
        };

    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().keyEquivalence(testEquivalence));
    assertSame(testEquivalence, map.keyEquivalence);
    assertSame(map.valueStrength().defaultEquivalence(), map.valueEquivalence());
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
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(concurrencyLevel));
    assertThat(map.segments).hasLength(segmentCount);
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
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(
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
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(
            createMapMaker().concurrencyLevel(concurrencyLevel).initialCapacity(initialCapacity));
    int totalCapacity = 0;
    for (int i = 0; i < map.segments.length; i++) {
      totalCapacity += map.segments[i].maxSegmentSize;
    }
    assertTrue("totalCapcity=" + totalCapacity + ", maxSize=" + maxSize, totalCapacity <= maxSize);
  }

  public void testSetWeakKeys() {
    MapMakerInternalMap<Object, Object, ?, ?> map = makeMap(createMapMaker().weakKeys());
    checkStrength(map, Strength.WEAK, Strength.STRONG);
    assertThat(map.entryHelper)
        .isInstanceOf(MapMakerInternalMap.WeakKeyStrongValueEntry.Helper.class);
  }

  public void testSetWeakValues() {
    MapMakerInternalMap<Object, Object, ?, ?> map = makeMap(createMapMaker().weakValues());
    checkStrength(map, Strength.STRONG, Strength.WEAK);
    assertThat(map.entryHelper)
        .isInstanceOf(MapMakerInternalMap.StrongKeyWeakValueEntry.Helper.class);
  }

  private static void checkStrength(
      MapMakerInternalMap<Object, Object, ?, ?> map, Strength keyStrength, Strength valueStrength) {
    assertSame(keyStrength, map.keyStrength());
    assertSame(valueStrength, map.valueStrength());
    assertSame(keyStrength.defaultEquivalence(), map.keyEquivalence);
    assertSame(valueStrength.defaultEquivalence(), map.valueEquivalence());
  }

  // Segment core tests

  public void testNewEntry() {
    for (MapMaker maker : allWeakValueStrengthMakers()) {
      MapMakerInternalMap<Object, Object, ?, ?> map = makeMap(maker);
      Segment<Object, Object, ?, ?> segment = map.segments[0];

      Object keyOne = new Object();
      Object valueOne = new Object();
      int hashOne = map.hash(keyOne);
      InternalEntry<Object, Object, ?> entryOne = segment.newEntryForTesting(keyOne, hashOne, null);
      WeakValueReference<Object, Object, ?> valueRefOne =
          segment.newWeakValueReferenceForTesting(entryOne, valueOne);
      assertSame(valueOne, valueRefOne.get());
      segment.setWeakValueReferenceForTesting(entryOne, valueRefOne);

      assertSame(keyOne, entryOne.getKey());
      assertEquals(hashOne, entryOne.getHash());
      assertNull(entryOne.getNext());
      assertSame(valueRefOne, segment.getWeakValueReferenceForTesting(entryOne));

      Object keyTwo = new Object();
      Object valueTwo = new Object();
      int hashTwo = map.hash(keyTwo);

      InternalEntry<Object, Object, ?> entryTwo =
          segment.newEntryForTesting(keyTwo, hashTwo, entryOne);
      WeakValueReference<Object, Object, ?> valueRefTwo =
          segment.newWeakValueReferenceForTesting(entryTwo, valueTwo);
      assertSame(valueTwo, valueRefTwo.get());
      segment.setWeakValueReferenceForTesting(entryTwo, valueRefTwo);

      assertSame(keyTwo, entryTwo.getKey());
      assertEquals(hashTwo, entryTwo.getHash());
      assertSame(entryOne, entryTwo.getNext());
      assertSame(valueRefTwo, segment.getWeakValueReferenceForTesting(entryTwo));
    }
  }

  public void testCopyEntry() {
    for (MapMaker maker : allWeakValueStrengthMakers()) {
      MapMakerInternalMap<Object, Object, ?, ?> map = makeMap(maker);
      Segment<Object, Object, ?, ?> segment = map.segments[0];

      Object keyOne = new Object();
      Object valueOne = new Object();
      int hashOne = map.hash(keyOne);
      InternalEntry<Object, Object, ?> entryOne = segment.newEntryForTesting(keyOne, hashOne, null);
      segment.setValueForTesting(entryOne, valueOne);

      Object keyTwo = new Object();
      Object valueTwo = new Object();
      int hashTwo = map.hash(keyTwo);
      InternalEntry<Object, Object, ?> entryTwo = segment.newEntryForTesting(keyTwo, hashTwo, null);
      segment.setValueForTesting(entryTwo, valueTwo);

      InternalEntry<Object, Object, ?> copyOne = segment.copyForTesting(entryOne, null);
      assertSame(keyOne, entryOne.getKey());
      assertEquals(hashOne, entryOne.getHash());
      assertNull(entryOne.getNext());
      assertSame(valueOne, copyOne.getValue());

      InternalEntry<Object, Object, ?> copyTwo = segment.copyForTesting(entryTwo, copyOne);
      assertSame(keyTwo, copyTwo.getKey());
      assertEquals(hashTwo, copyTwo.getHash());
      assertSame(copyOne, copyTwo.getNext());
      assertSame(valueTwo, copyTwo.getValue());
    }
  }

  public void testSegmentGetAndContains() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).weakValues());
    Segment<Object, Object, ?, ?> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object value = new Object();
    AtomicReferenceArray<? extends InternalEntry<Object, Object, ?>> table = segment.table;
    int index = hash & (table.length() - 1);

    InternalEntry<Object, Object, ?> entry = segment.newEntryForTesting(key, hash, null);
    segment.setValueForTesting(entry, value);

    assertNull(segment.get(key, hash));

    // count == 0
    segment.setTableEntryForTesting(index, entry);
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
    InternalEntry<Object, Object, ?> nullEntry = segment.newEntryForTesting(null, hash, entry);
    Object nullValue = new Object();
    WeakValueReference<Object, Object, ?> nullValueRef =
        segment.newWeakValueReferenceForTesting(nullEntry, nullValue);
    segment.setWeakValueReferenceForTesting(nullEntry, nullValueRef);
    segment.setTableEntryForTesting(index, nullEntry);
    // skip the null key
    assertSame(value, segment.get(key, hash));
    assertTrue(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    assertFalse(segment.containsValue(nullValue));

    // hash collision
    InternalEntry<Object, Object, ?> dummyEntry =
        segment.newEntryForTesting(new Object(), hash, entry);
    Object dummyValue = new Object();
    WeakValueReference<Object, Object, ?> dummyValueRef =
        segment.newWeakValueReferenceForTesting(dummyEntry, dummyValue);
    segment.setWeakValueReferenceForTesting(dummyEntry, dummyValueRef);
    segment.setTableEntryForTesting(index, dummyEntry);
    assertSame(value, segment.get(key, hash));
    assertTrue(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    assertTrue(segment.containsValue(dummyValue));

    // key collision
    dummyEntry = segment.newEntryForTesting(key, hash, entry);
    dummyValue = new Object();
    dummyValueRef = segment.newWeakValueReferenceForTesting(dummyEntry, dummyValue);
    segment.setWeakValueReferenceForTesting(dummyEntry, dummyValueRef);
    segment.setTableEntryForTesting(index, dummyEntry);
    // returns the most recent entry
    assertSame(dummyValue, segment.get(key, hash));
    assertTrue(segment.containsKey(key, hash));
    assertTrue(segment.containsValue(value));
    assertTrue(segment.containsValue(dummyValue));
  }

  public void testSegmentReplaceValue() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).weakValues());
    Segment<Object, Object, ?, ?> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();
    AtomicReferenceArray<? extends InternalEntry<Object, Object, ?>> table = segment.table;
    int index = hash & (table.length() - 1);

    InternalEntry<Object, Object, ?> entry = segment.newEntryForTesting(key, hash, null);
    WeakValueReference<Object, Object, ?> oldValueRef =
        segment.newWeakValueReferenceForTesting(entry, oldValue);
    segment.setWeakValueReferenceForTesting(entry, oldValueRef);

    // no entry
    assertFalse(segment.replace(key, hash, oldValue, newValue));
    assertEquals(0, segment.count);

    // same value
    segment.setTableEntryForTesting(index, entry);
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
    segment.setWeakValueReferenceForTesting(entry, oldValueRef);
    oldValueRef.clear();
    assertFalse(segment.replace(key, hash, oldValue, newValue));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
  }

  public void testSegmentReplace() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).weakValues());
    Segment<Object, Object, ?, ?> segment = map.segments[0];
    // TODO(fry): check recency ordering

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();
    AtomicReferenceArray<? extends InternalEntry<Object, Object, ?>> table = segment.table;
    int index = hash & (table.length() - 1);

    InternalEntry<Object, Object, ?> entry = segment.newEntryForTesting(key, hash, null);
    WeakValueReference<Object, Object, ?> oldValueRef =
        segment.newWeakValueReferenceForTesting(entry, oldValue);
    segment.setWeakValueReferenceForTesting(entry, oldValueRef);

    // no entry
    assertNull(segment.replace(key, hash, newValue));
    assertEquals(0, segment.count);

    // same key
    segment.setTableEntryForTesting(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertSame(oldValue, segment.replace(key, hash, newValue));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));

    // cleared
    segment.setWeakValueReferenceForTesting(entry, oldValueRef);
    oldValueRef.clear();
    assertNull(segment.replace(key, hash, newValue));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
  }

  public void testSegmentPut() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).weakValues());
    Segment<Object, Object, ?, ?> segment = map.segments[0];
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
    InternalEntry<Object, Object, ?> entry = segment.getEntry(key, hash);
    WeakValueReference<Object, Object, ?> oldValueRef =
        segment.newWeakValueReferenceForTesting(entry, oldValue);
    segment.setWeakValueReferenceForTesting(entry, oldValueRef);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear();
    assertNull(segment.put(key, hash, newValue, false));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));
  }

  public void testSegmentPutIfAbsent() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).weakValues());
    Segment<Object, Object, ?, ?> segment = map.segments[0];
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
    InternalEntry<Object, Object, ?> entry = segment.getEntry(key, hash);
    WeakValueReference<Object, Object, ?> oldValueRef =
        segment.newWeakValueReferenceForTesting(entry, oldValue);
    segment.setWeakValueReferenceForTesting(entry, oldValueRef);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear();
    assertNull(segment.put(key, hash, newValue, true));
    assertEquals(1, segment.count);
    assertSame(newValue, segment.get(key, hash));
  }

  public void testSegmentPut_expand() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object, ?, ?> segment = map.segments[0];
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

  public void testSegmentRemove() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).weakValues());
    Segment<Object, Object, ?, ?> segment = map.segments[0];

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    AtomicReferenceArray<? extends InternalEntry<Object, Object, ?>> table = segment.table;
    int index = hash & (table.length() - 1);

    InternalEntry<Object, Object, ?> entry = segment.newEntryForTesting(key, hash, null);
    WeakValueReference<Object, Object, ?> oldValueRef =
        segment.newWeakValueReferenceForTesting(entry, oldValue);
    segment.setWeakValueReferenceForTesting(entry, oldValueRef);

    // no entry
    assertEquals(0, segment.count);
    assertNull(segment.remove(key, hash));
    assertEquals(0, segment.count);

    // same key
    segment.setTableEntryForTesting(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertSame(oldValue, segment.remove(key, hash));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));

    // cleared
    segment.setTableEntryForTesting(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear();
    assertNull(segment.remove(key, hash));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
  }

  public void testSegmentRemoveValue() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).weakValues());
    Segment<Object, Object, ?, ?> segment = map.segments[0];

    Object key = new Object();
    int hash = map.hash(key);
    Object oldValue = new Object();
    Object newValue = new Object();
    AtomicReferenceArray<? extends InternalEntry<Object, Object, ?>> table = segment.table;
    int index = hash & (table.length() - 1);

    InternalEntry<Object, Object, ?> entry = segment.newEntryForTesting(key, hash, null);
    WeakValueReference<Object, Object, ?> oldValueRef =
        segment.newWeakValueReferenceForTesting(entry, oldValue);
    segment.setWeakValueReferenceForTesting(entry, oldValueRef);

    // no entry
    assertEquals(0, segment.count);
    assertNull(segment.remove(key, hash));
    assertEquals(0, segment.count);

    // same value
    segment.setTableEntryForTesting(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertTrue(segment.remove(key, hash, oldValue));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));

    // different value
    segment.setTableEntryForTesting(index, entry);
    segment.count++;
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));
    assertFalse(segment.remove(key, hash, newValue));
    assertEquals(1, segment.count);
    assertSame(oldValue, segment.get(key, hash));

    // cleared
    assertSame(oldValue, segment.get(key, hash));
    oldValueRef.clear();
    assertFalse(segment.remove(key, hash, oldValue));
    assertEquals(0, segment.count);
    assertNull(segment.get(key, hash));
  }

  public void testExpand() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object, ?, ?> segment = map.segments[0];
    assertEquals(1, segment.table.length());

    // manually add elements to avoid expansion
    int originalCount = 1024;
    InternalEntry<Object, Object, ?> entry = null;
    for (int i = 0; i < originalCount; i++) {
      Object key = new Object();
      Object value = new Object();
      int hash = map.hash(key);
      // chain all entries together as we only have a single bucket
      entry = segment.newEntryForTesting(key, hash, entry);
      segment.setValueForTesting(entry, value);
    }
    segment.setTableEntryForTesting(0, entry);
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

  public void testRemoveFromChain() {
    MapMakerInternalMap<Object, Object, ?, ?> map = makeMap(createMapMaker().concurrencyLevel(1));
    Segment<Object, Object, ?, ?> segment = map.segments[0];

    // create 3 objects and chain them together
    Object keyOne = new Object();
    Object valueOne = new Object();
    int hashOne = map.hash(keyOne);
    InternalEntry<Object, Object, ?> entryOne = segment.newEntryForTesting(keyOne, hashOne, null);
    segment.setValueForTesting(entryOne, valueOne);
    Object keyTwo = new Object();
    Object valueTwo = new Object();
    int hashTwo = map.hash(keyTwo);
    InternalEntry<Object, Object, ?> entryTwo =
        segment.newEntryForTesting(keyTwo, hashTwo, entryOne);
    segment.setValueForTesting(entryTwo, valueTwo);
    Object keyThree = new Object();
    Object valueThree = new Object();
    int hashThree = map.hash(keyThree);
    InternalEntry<Object, Object, ?> entryThree =
        segment.newEntryForTesting(keyThree, hashThree, entryTwo);
    segment.setValueForTesting(entryThree, valueThree);

    // alone
    assertNull(segment.removeFromChainForTesting(entryOne, entryOne));

    // head
    assertSame(entryOne, segment.removeFromChainForTesting(entryTwo, entryTwo));

    // middle
    InternalEntry<Object, Object, ?> newFirst =
        segment.removeFromChainForTesting(entryThree, entryTwo);
    assertSame(keyThree, newFirst.getKey());
    assertSame(valueThree, newFirst.getValue());
    assertEquals(hashThree, newFirst.getHash());
    assertSame(entryOne, newFirst.getNext());

    // tail (remaining entries are copied in reverse order)
    newFirst = segment.removeFromChainForTesting(entryThree, entryOne);
    assertSame(keyTwo, newFirst.getKey());
    assertSame(valueTwo, newFirst.getValue());
    assertEquals(hashTwo, newFirst.getHash());
    newFirst = newFirst.getNext();
    assertSame(keyThree, newFirst.getKey());
    assertSame(valueThree, newFirst.getValue());
    assertEquals(hashThree, newFirst.getHash());
    assertNull(newFirst.getNext());
  }

  public void testExpand_cleanup() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object, ?, ?> segment = map.segments[0];
    assertEquals(1, segment.table.length());

    // manually add elements to avoid expansion
    // 1/3 null keys, 1/3 null values
    int originalCount = 1024;
    InternalEntry<Object, Object, ?> entry = null;
    for (int i = 0; i < originalCount; i++) {
      Object key = new Object();
      Object value = (i % 3 == 0) ? null : new Object();
      int hash = map.hash(key);
      if (i % 3 == 1) {
        key = null;
      }
      // chain all entries together as we only have a single bucket
      entry = segment.newEntryForTesting(key, hash, entry);
      segment.setValueForTesting(entry, value);
    }
    segment.setTableEntryForTesting(0, entry);
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

  private static <K, V> int countLiveEntries(MapMakerInternalMap<K, V, ?, ?> map) {
    int result = 0;
    for (Segment<K, V, ?, ?> segment : map.segments) {
      AtomicReferenceArray<? extends InternalEntry<K, V, ?>> table = segment.table;
      for (int i = 0; i < table.length(); i++) {
        for (InternalEntry<K, V, ?> e = table.get(i); e != null; e = e.getNext()) {
          if (map.isLiveForTesting(e)) {
            result++;
          }
        }
      }
    }
    return result;
  }

  public void testClear() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object, ?, ?> segment = map.segments[0];
    AtomicReferenceArray<? extends InternalEntry<Object, Object, ?>> table = segment.table;
    assertEquals(1, table.length());

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    InternalEntry<Object, Object, ?> entry = segment.newEntryForTesting(key, hash, null);
    segment.setValueForTesting(entry, value);

    segment.setTableEntryForTesting(0, entry);
    segment.readCount.incrementAndGet();
    segment.count = 1;

    assertSame(entry, table.get(0));

    segment.clear();
    assertNull(table.get(0));
    assertEquals(0, segment.readCount.get());
    assertEquals(0, segment.count);
  }

  public void testRemoveEntry() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).initialCapacity(1));
    Segment<Object, Object, ?, ?> segment = map.segments[0];
    AtomicReferenceArray<? extends InternalEntry<Object, Object, ?>> table = segment.table;
    assertEquals(1, table.length());

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    InternalEntry<Object, Object, ?> entry = segment.newEntryForTesting(key, hash, null);
    segment.setValueForTesting(entry, value);

    // remove absent
    assertFalse(segment.removeTableEntryForTesting(entry));

    segment.setTableEntryForTesting(0, entry);
    segment.count = 1;
    assertTrue(segment.removeTableEntryForTesting(entry));
    assertEquals(0, segment.count);
    assertNull(table.get(0));
  }

  public void testClearValue() {
    MapMakerInternalMap<Object, Object, ?, ?> map =
        makeMap(createMapMaker().concurrencyLevel(1).initialCapacity(1).weakValues());
    Segment<Object, Object, ?, ?> segment = map.segments[0];
    AtomicReferenceArray<? extends InternalEntry<Object, Object, ?>> table = segment.table;
    assertEquals(1, table.length());

    Object key = new Object();
    Object value = new Object();
    int hash = map.hash(key);
    InternalEntry<Object, Object, ?> entry = segment.newEntryForTesting(key, hash, null);
    segment.setValueForTesting(entry, value);
    WeakValueReference<Object, Object, ?> valueRef = segment.getWeakValueReferenceForTesting(entry);

    // clear absent
    assertFalse(segment.clearValueForTesting(key, hash, valueRef));

    segment.setTableEntryForTesting(0, entry);
    // don't increment count; this is used during computation
    assertTrue(segment.clearValueForTesting(key, hash, valueRef));
    // no notification sent with clearValue
    assertEquals(0, segment.count);
    assertNull(table.get(0));

    // clear wrong value reference
    segment.setTableEntryForTesting(0, entry);
    WeakValueReference<Object, Object, ?> otherValueRef =
        segment.newWeakValueReferenceForTesting(entry, value);
    segment.setWeakValueReferenceForTesting(entry, otherValueRef);
    assertFalse(segment.clearValueForTesting(key, hash, valueRef));
    segment.setWeakValueReferenceForTesting(entry, valueRef);
    assertTrue(segment.clearValueForTesting(key, hash, valueRef));
  }

  // reference queues

  public void testDrainKeyReferenceQueueOnWrite() {
    for (MapMaker maker : allWeakKeyStrengthMakers()) {
      MapMakerInternalMap<Object, Object, ?, ?> map = makeMap(maker.concurrencyLevel(1));
      if (maker.getKeyStrength() == Strength.WEAK) {
        Segment<Object, Object, ?, ?> segment = map.segments[0];

        Object keyOne = new Object();
        int hashOne = map.hash(keyOne);
        Object valueOne = new Object();
        Object keyTwo = new Object();
        Object valueTwo = new Object();

        map.put(keyOne, valueOne);
        InternalEntry<Object, Object, ?> entry = segment.getEntry(keyOne, hashOne);

        @SuppressWarnings("unchecked")
        Reference<Object> reference = (Reference) entry;
        reference.enqueue();

        map.put(keyTwo, valueTwo);
        assertFalse(map.containsKey(keyOne));
        assertFalse(map.containsValue(valueOne));
        assertNull(map.get(keyOne));
        assertEquals(1, map.size());
        assertNull(segment.getKeyReferenceQueueForTesting().poll());
      }
    }
  }

  public void testDrainValueReferenceQueueOnWrite() {
    for (MapMaker maker : allWeakValueStrengthMakers()) {
      MapMakerInternalMap<Object, Object, ?, ?> map = makeMap(maker.concurrencyLevel(1));
      if (maker.getValueStrength() == Strength.WEAK) {
        Segment<Object, Object, ?, ?> segment = map.segments[0];

        Object keyOne = new Object();
        int hashOne = map.hash(keyOne);
        Object valueOne = new Object();
        Object keyTwo = new Object();
        Object valueTwo = new Object();

        map.put(keyOne, valueOne);
        @SuppressWarnings("unchecked")
        WeakValueEntry<Object, Object, ?> entry =
            (WeakValueEntry<Object, Object, ?>) segment.getEntry(keyOne, hashOne);
        WeakValueReference<Object, Object, ?> valueReference = entry.getValueReference();

        @SuppressWarnings("unchecked")
        Reference<Object> reference = (Reference) valueReference;
        reference.enqueue();

        map.put(keyTwo, valueTwo);
        assertFalse(map.containsKey(keyOne));
        assertFalse(map.containsValue(valueOne));
        assertNull(map.get(keyOne));
        assertEquals(1, map.size());
        assertNull(segment.getValueReferenceQueueForTesting().poll());
      }
    }
  }

  public void testDrainKeyReferenceQueueOnRead() {
    for (MapMaker maker : allWeakKeyStrengthMakers()) {
      MapMakerInternalMap<Object, Object, ?, ?> map = makeMap(maker.concurrencyLevel(1));
      if (maker.getKeyStrength() == Strength.WEAK) {
        Segment<Object, Object, ?, ?> segment = map.segments[0];

        Object keyOne = new Object();
        int hashOne = map.hash(keyOne);
        Object valueOne = new Object();
        Object keyTwo = new Object();

        map.put(keyOne, valueOne);
        InternalEntry<Object, Object, ?> entry = segment.getEntry(keyOne, hashOne);

        @SuppressWarnings("unchecked")
        Reference<Object> reference = (Reference) entry;
        reference.enqueue();

        for (int i = 0; i < SMALL_MAX_SIZE; i++) {
          Object unused = map.get(keyTwo);
        }
        assertFalse(map.containsKey(keyOne));
        assertFalse(map.containsValue(valueOne));
        assertNull(map.get(keyOne));
        assertEquals(0, map.size());
        assertNull(segment.getKeyReferenceQueueForTesting().poll());
      }
    }
  }

  public void testDrainValueReferenceQueueOnRead() {
    for (MapMaker maker : allWeakValueStrengthMakers()) {
      MapMakerInternalMap<Object, Object, ?, ?> map = makeMap(maker.concurrencyLevel(1));
      if (maker.getValueStrength() == Strength.WEAK) {
        Segment<Object, Object, ?, ?> segment = map.segments[0];

        Object keyOne = new Object();
        int hashOne = map.hash(keyOne);
        Object valueOne = new Object();
        Object keyTwo = new Object();

        map.put(keyOne, valueOne);
        @SuppressWarnings("unchecked")
        WeakValueEntry<Object, Object, ?> entry =
            (WeakValueEntry<Object, Object, ?>) segment.getEntry(keyOne, hashOne);
        WeakValueReference<Object, Object, ?> valueReference = entry.getValueReference();

        @SuppressWarnings("unchecked")
        Reference<Object> reference = (Reference) valueReference;
        reference.enqueue();

        for (int i = 0; i < SMALL_MAX_SIZE; i++) {
          Object unused = map.get(keyTwo);
        }
        assertFalse(map.containsKey(keyOne));
        assertFalse(map.containsValue(valueOne));
        assertNull(map.get(keyOne));
        assertEquals(0, map.size());
        assertNull(segment.getValueReferenceQueueForTesting().poll());
      }
    }
  }

  // utility methods

  private static Iterable<MapMaker> allWeakKeyStrengthMakers() {
    return ImmutableList.of(createMapMaker().weakKeys(), createMapMaker().weakKeys().weakValues());
  }

  private static Iterable<MapMaker> allWeakValueStrengthMakers() {
    return ImmutableList.of(
        createMapMaker().weakValues(), createMapMaker().weakKeys().weakValues());
  }

  public void testNullParameters() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(makeMap(createMapMaker()));
  }
}

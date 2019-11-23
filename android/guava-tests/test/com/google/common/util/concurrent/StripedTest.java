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

package com.google.common.util.concurrent;

import static com.google.common.collect.Iterables.concat;

import com.google.common.base.Functions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.testing.GcFinalization;
import com.google.common.testing.NullPointerTester;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import junit.framework.TestCase;

/**
 * Tests for Striped.
 *
 * @author Dimitris Andreou
 */
public class StripedTest extends TestCase {
  private static List<Striped<?>> strongImplementations() {
    return ImmutableList.of(
        Striped.readWriteLock(100),
        Striped.readWriteLock(256),
        Striped.lock(100),
        Striped.lock(256),
        Striped.custom(
            100,
            new Supplier<Lock>() {
              @Override
              public Lock get() {
                return new ReentrantLock(true);
              }
            }),
        Striped.custom(
            256,
            new Supplier<Lock>() {
              @Override
              public Lock get() {
                return new ReentrantLock(true);
              }
            }),
        Striped.semaphore(100, 1),
        Striped.semaphore(256, 1));
  }

  private static final Supplier<ReadWriteLock> READ_WRITE_LOCK_SUPPLIER =
      new Supplier<ReadWriteLock>() {
        @Override
        public ReadWriteLock get() {
          return new ReentrantReadWriteLock();
        }
      };

  private static final Supplier<Lock> LOCK_SUPPLER =
      new Supplier<Lock>() {
        @Override
        public Lock get() {
          return new ReentrantLock();
        }
      };

  private static final Supplier<Semaphore> SEMAPHORE_SUPPLER =
      new Supplier<Semaphore>() {
        @Override
        public Semaphore get() {
          return new Semaphore(1, false);
        }
      };

  private static List<Striped<?>> weakImplementations() {
    return ImmutableList.<Striped<?>>builder()
        .add(new Striped.SmallLazyStriped<ReadWriteLock>(50, READ_WRITE_LOCK_SUPPLIER))
        .add(new Striped.SmallLazyStriped<ReadWriteLock>(64, READ_WRITE_LOCK_SUPPLIER))
        .add(new Striped.LargeLazyStriped<ReadWriteLock>(50, READ_WRITE_LOCK_SUPPLIER))
        .add(new Striped.LargeLazyStriped<ReadWriteLock>(64, READ_WRITE_LOCK_SUPPLIER))
        .add(new Striped.SmallLazyStriped<Lock>(50, LOCK_SUPPLER))
        .add(new Striped.SmallLazyStriped<Lock>(64, LOCK_SUPPLER))
        .add(new Striped.LargeLazyStriped<Lock>(50, LOCK_SUPPLER))
        .add(new Striped.LargeLazyStriped<Lock>(64, LOCK_SUPPLER))
        .add(new Striped.SmallLazyStriped<Semaphore>(50, SEMAPHORE_SUPPLER))
        .add(new Striped.SmallLazyStriped<Semaphore>(64, SEMAPHORE_SUPPLER))
        .add(new Striped.LargeLazyStriped<Semaphore>(50, SEMAPHORE_SUPPLER))
        .add(new Striped.LargeLazyStriped<Semaphore>(64, SEMAPHORE_SUPPLER))
        .build();
  }

  private static Iterable<Striped<?>> allImplementations() {
    return concat(strongImplementations(), weakImplementations());
  }

  public void testNull() throws Exception {
    for (Striped<?> striped : allImplementations()) {
      new NullPointerTester().testAllPublicInstanceMethods(striped);
    }
  }

  public void testSizes() {
    // not bothering testing all variations, since we know they share implementations
    assertTrue(Striped.lock(100).size() >= 100);
    assertTrue(Striped.lock(256).size() == 256);
    assertTrue(Striped.lazyWeakLock(100).size() >= 100);
    assertTrue(Striped.lazyWeakLock(256).size() == 256);
  }

  public void testWeakImplementations() {
    for (Striped<?> striped : weakImplementations()) {
      WeakReference<Object> weakRef = new WeakReference<>(striped.get(new Object()));
      GcFinalization.awaitClear(weakRef);
    }
  }

  public void testWeakReadWrite() {
    Striped<ReadWriteLock> striped = Striped.lazyWeakReadWriteLock(1000);
    Object key = new Object();
    Lock readLock = striped.get(key).readLock();
    WeakReference<Object> garbage = new WeakReference<>(new Object());
    GcFinalization.awaitClear(garbage);
    Lock writeLock = striped.get(key).writeLock();
    readLock.lock();
    assertFalse(writeLock.tryLock());
    readLock.unlock();
  }

  public void testStrongImplementations() {
    for (Striped<?> striped : strongImplementations()) {
      WeakReference<Object> weakRef = new WeakReference<>(striped.get(new Object()));
      WeakReference<Object> garbage = new WeakReference<>(new Object());
      GcFinalization.awaitClear(garbage);
      assertNotNull(weakRef.get());
    }
  }

  public void testMaximalWeakStripedLock() {
    Striped<Lock> stripedLock = Striped.lazyWeakLock(Integer.MAX_VALUE);
    for (int i = 0; i < 10000; i++) {
      stripedLock.get(new Object()).lock();
      // nothing special (e.g. an exception) happens
    }
  }

  public void testBulkGetReturnsSorted() {
    for (Striped<?> striped : allImplementations()) {
      Map<Object, Integer> indexByLock = Maps.newHashMap();
      for (int i = 0; i < striped.size(); i++) {
        indexByLock.put(striped.getAt(i), i);
      }

      // ensure that bulkGet returns locks in monotonically increasing order
      for (int objectsNum = 1; objectsNum <= striped.size() * 2; objectsNum++) {
        Set<Object> objects = Sets.newHashSetWithExpectedSize(objectsNum);
        for (int i = 0; i < objectsNum; i++) {
          objects.add(new Object());
        }

        Iterable<?> locks = striped.bulkGet(objects);
        assertTrue(Ordering.natural().onResultOf(Functions.forMap(indexByLock)).isOrdered(locks));

        // check idempotency
        Iterable<?> locks2 = striped.bulkGet(objects);
        assertEquals(Lists.newArrayList(locks), Lists.newArrayList(locks2));
      }
    }
  }

  /** Checks idempotency, and that we observe the promised number of stripes. */
  public void testBasicInvariants() {
    for (Striped<?> striped : allImplementations()) {
      assertBasicInvariants(striped);
    }
  }

  private static void assertBasicInvariants(Striped<?> striped) {
    Set<Object> observed = Sets.newIdentityHashSet(); // for the sake of weakly referenced locks.
    // this gets the stripes with #getAt(index)
    for (int i = 0; i < striped.size(); i++) {
      Object object = striped.getAt(i);
      assertNotNull(object);
      assertSame(object, striped.getAt(i)); // idempotent
      observed.add(object);
    }
    assertTrue("All stripes observed", observed.size() == striped.size());

    // this uses #get(key), makes sure an already observed stripe is returned
    for (int i = 0; i < striped.size() * 100; i++) {
      assertTrue(observed.contains(striped.get(new Object())));
    }

    try {
      striped.getAt(-1);
      fail();
    } catch (RuntimeException expected) {
    }

    try {
      striped.getAt(striped.size());
      fail();
    } catch (RuntimeException expected) {
    }
  }

  public void testMaxSize() {
    for (Striped<?> striped :
        ImmutableList.of(
            Striped.lazyWeakLock(Integer.MAX_VALUE),
            Striped.lazyWeakSemaphore(Integer.MAX_VALUE, Integer.MAX_VALUE),
            Striped.lazyWeakReadWriteLock(Integer.MAX_VALUE))) {
      for (int i = 0; i < 3; i++) {
        // doesn't throw exception
        Object unused = striped.getAt(Integer.MAX_VALUE - i);
      }
    }
  }
}

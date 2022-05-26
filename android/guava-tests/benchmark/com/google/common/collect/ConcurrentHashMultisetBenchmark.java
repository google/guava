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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.CheckForNull;

/**
 * Benchmarks for {@link ConcurrentHashMultiset}.
 *
 * @author mike nonemacher
 */
public class ConcurrentHashMultisetBenchmark {
  @Param({"1", "2", "4", "8"})
  int threads;

  @Param({"3", "30", "300"})
  int size;

  @Param MultisetSupplier implSupplier;

  private Multiset<Integer> multiset;
  private ImmutableList<Integer> keys;
  private ExecutorService threadPool;

  @BeforeExperiment
  void setUp() throws Exception {
    multiset = implSupplier.get();
    ImmutableList.Builder<Integer> builder = ImmutableList.builder();
    for (int i = 0; i < size; i++) {
      builder.add(i);
    }
    keys = builder.build();
    threadPool =
        Executors.newFixedThreadPool(threads, new ThreadFactoryBuilder().setDaemon(true).build());
  }

  @Benchmark
  long add(final int reps) throws ExecutionException, InterruptedException {
    return doMultithreadedLoop(
        new Callable<Long>() {
          @Override
          public Long call() {
            return runAddSingleThread(reps);
          }
        });
  }

  @Benchmark
  long addRemove(final int reps) throws ExecutionException, InterruptedException {
    return doMultithreadedLoop(
        new Callable<Long>() {
          @Override
          public Long call() {
            return runAddRemoveSingleThread(reps);
          }
        });
  }

  private long doMultithreadedLoop(Callable<Long> task)
      throws InterruptedException, ExecutionException {

    List<Future<Long>> futures = Lists.newArrayListWithCapacity(threads);
    for (int i = 0; i < threads; i++) {
      futures.add(threadPool.submit(task));
    }
    long total = 0;
    for (Future<Long> future : futures) {
      total += future.get();
    }
    return total;
  }

  private long runAddSingleThread(int reps) {
    Random random = new Random();
    int nKeys = keys.size();
    long blah = 0;
    for (int i = 0; i < reps; i++) {
      Integer key = keys.get(random.nextInt(nKeys));
      int delta = random.nextInt(5);
      blah += delta;
      multiset.add(key, delta);
    }
    return blah;
  }

  private long runAddRemoveSingleThread(int reps) {
    Random random = new Random();
    int nKeys = keys.size();
    long blah = 0;
    for (int i = 0; i < reps; i++) {
      Integer key = keys.get(random.nextInt(nKeys));
      // This range is [-5, 4] - slight negative bias so we often hit zero, which brings the
      // auto-removal of zeroes into play.
      int delta = random.nextInt(10) - 5;
      blah += delta;
      if (delta >= 0) {
        multiset.add(key, delta);
      } else {
        multiset.remove(key, -delta);
      }
    }
    return blah;
  }

  private enum MultisetSupplier {
    CONCURRENT_HASH_MULTISET() {
      @Override
      Multiset<Integer> get() {
        return ConcurrentHashMultiset.create();
      }
    },
    BOXED_ATOMIC_REPLACE() {
      @Override
      Multiset<Integer> get() {
        return OldConcurrentHashMultiset.create();
      }
    },
    SYNCHRONIZED_MULTISET() {
      @Override
      Multiset<Integer> get() {
        return Synchronized.multiset(HashMultiset.<Integer>create(), null);
      }
    },
    ;

    abstract Multiset<Integer> get();
  }

  /**
   * Duplication of the old version of ConcurrentHashMultiset (with some unused stuff removed, like
   * serialization code) which used a map with boxed integers for the values.
   */
  private static final class OldConcurrentHashMultiset<E> extends AbstractMultiset<E> {
    /** The number of occurrences of each element. */
    private final transient ConcurrentMap<E, Integer> countMap;

    /**
     * Creates a new, empty {@code OldConcurrentHashMultiset} using the default initial capacity,
     * load factor, and concurrency settings.
     */
    public static <E> OldConcurrentHashMultiset<E> create() {
      return new OldConcurrentHashMultiset<E>(new ConcurrentHashMap<E, Integer>());
    }

    @VisibleForTesting
    OldConcurrentHashMultiset(ConcurrentMap<E, Integer> countMap) {
      checkArgument(countMap.isEmpty());
      this.countMap = countMap;
    }

    // Query Operations

    /**
     * Returns the number of occurrences of {@code element} in this multiset.
     *
     * @param element the element to look for
     * @return the nonnegative number of occurrences of the element
     */
    @Override
    public int count(@CheckForNull Object element) {
      try {
        return unbox(countMap.get(element));
      } catch (NullPointerException | ClassCastException e) {
        return 0;
      }
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the data in the multiset is modified by any other threads during this method, it is
     * undefined which (if any) of these modifications will be reflected in the result.
     */
    @Override
    public int size() {
      long sum = 0L;
      for (Integer value : countMap.values()) {
        sum += value;
      }
      return Ints.saturatedCast(sum);
    }

    /*
     * Note: the superclass toArray() methods assume that size() gives a correct
     * answer, which ours does not.
     */

    @Override
    public Object[] toArray() {
      return snapshot().toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      return snapshot().toArray(array);
    }

    /*
     * We'd love to use 'new ArrayList(this)' or 'list.addAll(this)', but
     * either of these would recurse back to us again!
     */
    private List<E> snapshot() {
      List<E> list = Lists.newArrayListWithExpectedSize(size());
      for (Multiset.Entry<E> entry : entrySet()) {
        E element = entry.getElement();
        for (int i = entry.getCount(); i > 0; i--) {
          list.add(element);
        }
      }
      return list;
    }

    // Modification Operations

    /**
     * Adds a number of occurrences of the specified element to this multiset.
     *
     * @param element the element to add
     * @param occurrences the number of occurrences to add
     * @return the previous count of the element before the operation; possibly zero
     * @throws IllegalArgumentException if {@code occurrences} is negative, or if the resulting
     *     amount would exceed {@link Integer#MAX_VALUE}
     */
    @Override
    public int add(E element, int occurrences) {
      if (occurrences == 0) {
        return count(element);
      }
      checkArgument(occurrences > 0, "Invalid occurrences: %s", occurrences);

      while (true) {
        int current = count(element);
        if (current == 0) {
          if (countMap.putIfAbsent(element, occurrences) == null) {
            return 0;
          }
        } else {
          checkArgument(
              occurrences <= Integer.MAX_VALUE - current,
              "Overflow adding %s occurrences to a count of %s",
              occurrences,
              current);
          int next = current + occurrences;
          if (countMap.replace(element, current, next)) {
            return current;
          }
        }
        // If we're still here, there was a race, so just try again.
      }
    }

    /**
     * Removes a number of occurrences of the specified element from this multiset. If the multiset
     * contains fewer than this number of occurrences to begin with, all occurrences will be
     * removed.
     *
     * @param element the element whose occurrences should be removed
     * @param occurrences the number of occurrences of the element to remove
     * @return the count of the element before the operation; possibly zero
     * @throws IllegalArgumentException if {@code occurrences} is negative
     */
    @Override
    public int remove(@CheckForNull Object element, int occurrences) {
      if (occurrences == 0) {
        return count(element);
      }
      checkArgument(occurrences > 0, "Invalid occurrences: %s", occurrences);

      while (true) {
        int current = count(element);
        if (current == 0) {
          return 0;
        }
        if (occurrences >= current) {
          if (countMap.remove(element, current)) {
            return current;
          }
        } else {
          // We know it's an "E" because it already exists in the map.
          @SuppressWarnings("unchecked")
          E casted = (E) element;

          if (countMap.replace(casted, current, current - occurrences)) {
            return current;
          }
        }
        // If we're still here, there was a race, so just try again.
      }
    }

    /**
     * Removes <b>all</b> occurrences of the specified element from this multiset. This method
     * complements {@link Multiset#remove(Object)}, which removes only one occurrence at a time.
     *
     * @param element the element whose occurrences should all be removed
     * @return the number of occurrences successfully removed, possibly zero
     */
    private int removeAllOccurrences(@CheckForNull Object element) {
      try {
        return unbox(countMap.remove(element));
      } catch (NullPointerException | ClassCastException e) {
        return 0;
      }
    }

    /**
     * Removes exactly the specified number of occurrences of {@code element}, or makes no change if
     * this is not possible.
     *
     * <p>This method, in contrast to {@link #remove(Object, int)}, has no effect when the element
     * count is smaller than {@code occurrences}.
     *
     * @param element the element to remove
     * @param occurrences the number of occurrences of {@code element} to remove
     * @return {@code true} if the removal was possible (including if {@code occurrences} is zero)
     */
    public boolean removeExactly(@CheckForNull Object element, int occurrences) {
      if (occurrences == 0) {
        return true;
      }
      checkArgument(occurrences > 0, "Invalid occurrences: %s", occurrences);

      while (true) {
        int current = count(element);
        if (occurrences > current) {
          return false;
        }
        if (occurrences == current) {
          if (countMap.remove(element, occurrences)) {
            return true;
          }
        } else {
          @SuppressWarnings("unchecked") // it's in the map, must be an "E"
          E casted = (E) element;
          if (countMap.replace(casted, current, current - occurrences)) {
            return true;
          }
        }
        // If we're still here, there was a race, so just try again.
      }
    }

    /**
     * Adds or removes occurrences of {@code element} such that the {@link #count} of the element
     * becomes {@code count}.
     *
     * @return the count of {@code element} in the multiset before this call
     * @throws IllegalArgumentException if {@code count} is negative
     */
    @Override
    public int setCount(E element, int count) {
      checkNonnegative(count, "count");
      return (count == 0) ? removeAllOccurrences(element) : unbox(countMap.put(element, count));
    }

    /**
     * Sets the number of occurrences of {@code element} to {@code newCount}, but only if the count
     * is currently {@code oldCount}. If {@code element} does not appear in the multiset exactly
     * {@code oldCount} times, no changes will be made.
     *
     * @return {@code true} if the change was successful. This usually indicates that the multiset
     *     has been modified, but not always: in the case that {@code oldCount == newCount}, the
     *     method will return {@code true} if the condition was met.
     * @throws IllegalArgumentException if {@code oldCount} or {@code newCount} is negative
     */
    @Override
    public boolean setCount(E element, int oldCount, int newCount) {
      checkNonnegative(oldCount, "oldCount");
      checkNonnegative(newCount, "newCount");
      if (newCount == 0) {
        if (oldCount == 0) {
          // No change to make, but must return true if the element is not present
          return !countMap.containsKey(element);
        } else {
          return countMap.remove(element, oldCount);
        }
      }
      if (oldCount == 0) {
        return countMap.putIfAbsent(element, newCount) == null;
      }
      return countMap.replace(element, oldCount, newCount);
    }

    // Views

    @Override
    Set<E> createElementSet() {
      final Set<E> delegate = countMap.keySet();
      return new ForwardingSet<E>() {
        @Override
        protected Set<E> delegate() {
          return delegate;
        }

        @Override
        public boolean remove(Object object) {
          try {
            return delegate.remove(object);
          } catch (NullPointerException | ClassCastException e) {
            return false;
          }
        }
      };
    }

    @Override
    Iterator<E> elementIterator() {
      throw new AssertionError("should never be called");
    }

    private transient EntrySet entrySet;

    @Override
    public Set<Multiset.Entry<E>> entrySet() {
      EntrySet result = entrySet;
      if (result == null) {
        entrySet = result = new EntrySet();
      }
      return result;
    }

    @Override
    int distinctElements() {
      return countMap.size();
    }

    @Override
    public boolean isEmpty() {
      return countMap.isEmpty();
    }

    @Override
    Iterator<Entry<E>> entryIterator() {
      final Iterator<Map.Entry<E, Integer>> backingIterator = countMap.entrySet().iterator();
      return new Iterator<Entry<E>>() {
        @Override
        public boolean hasNext() {
          return backingIterator.hasNext();
        }

        @Override
        public Multiset.Entry<E> next() {
          Map.Entry<E, Integer> backingEntry = backingIterator.next();
          return Multisets.immutableEntry(backingEntry.getKey(), backingEntry.getValue());
        }

        @Override
        public void remove() {
          backingIterator.remove();
        }
      };
    }

    @Override
    public Iterator<E> iterator() {
      return Multisets.iteratorImpl(this);
    }

    @Override
    public void clear() {
      countMap.clear();
    }

    private class EntrySet extends AbstractMultiset<E>.EntrySet {
      @Override
      Multiset<E> multiset() {
        return OldConcurrentHashMultiset.this;
      }

      /*
       * Note: the superclass toArray() methods assume that size() gives a correct
       * answer, which ours does not.
       */

      @Override
      public Object[] toArray() {
        return snapshot().toArray();
      }

      @Override
      public <T> T[] toArray(T[] array) {
        return snapshot().toArray(array);
      }

      private List<Multiset.Entry<E>> snapshot() {
        List<Multiset.Entry<E>> list = Lists.newArrayListWithExpectedSize(size());
        // not Iterables.addAll(list, this), because that'll forward back here
        Iterators.addAll(list, iterator());
        return list;
      }

      @Override
      public boolean remove(Object object) {
        if (object instanceof Multiset.Entry) {
          Multiset.Entry<?> entry = (Multiset.Entry<?>) object;
          Object element = entry.getElement();
          int entryCount = entry.getCount();
          return countMap.remove(element, entryCount);
        }
        return false;
      }

      /** The hash code is the same as countMap's, though the objects aren't equal. */
      @Override
      public int hashCode() {
        return countMap.hashCode();
      }
    }

    /** We use a special form of unboxing that treats null as zero. */
    private static int unbox(@CheckForNull Integer i) {
      return (i == null) ? 0 : i;
    }
  }
}
